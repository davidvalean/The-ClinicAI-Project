package com.clinic.backend.service;

import com.clinic.backend.dto.AppointmentDto;
import com.clinic.backend.dto.CreateAppointmentRequest;
import com.clinic.backend.dto.TimeSlotDto;
import com.clinic.backend.entity.Appointment;
import com.clinic.backend.entity.AppointmentStatus;
import com.clinic.backend.entity.Doctor;
import com.clinic.backend.entity.User;
import com.clinic.backend.exception.ConflictException;
import com.clinic.backend.exception.InvalidRequestException;
import com.clinic.backend.exception.ResourceNotFoundException;
import com.clinic.backend.repository.AppointmentRepository;
import com.clinic.backend.repository.DoctorRepository;
import com.clinic.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static com.clinic.backend.entity.AppointmentStatus.CANCELLED;
import static com.clinic.backend.entity.AppointmentStatus.SCHEDULED;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final Duration MIN_CANCEL_NOTICE = Duration.ofHours(24);

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    // Injected instead of calling LocalDateTime.now() directly, so tests can fix "now".
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<TimeSlotDto> findFreeSlots(Long doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findWithDetailsById(doctorId)
                .orElseThrow(() -> doctorNotFound(doctorId));

        LocalDateTime now = LocalDateTime.now(clock);
        if (date.isBefore(now.toLocalDate())) {
            throw new InvalidRequestException("Date " + date + " is in the past");
        }

        List<LocalDateTime> slotStarts = scheduleSlotStarts(doctor, date);
        if (slotStarts.isEmpty()) {
            return List.of();
        }

        List<Appointment> booked = appointmentRepository.findOverlapping(
                doctorId, SCHEDULED, date.atStartOfDay(), date.plusDays(1).atStartOfDay());

        return slotStarts.stream()
                .filter(start -> start.isAfter(now))
                .filter(start -> booked.stream().noneMatch(a -> overlaps(a, start, start.plus(Appointment.DURATION))))
                .map(start -> new TimeSlotDto(start, start.plus(Appointment.DURATION)))
                .toList();
    }

    @Transactional
    public AppointmentDto create(CreateAppointmentRequest request, User currentUser) {
        // Locks the doctor row until the transaction ends, so two bookings for the same
        // doctor are checked one after the other instead of at the same time.
        Doctor doctor = doctorRepository.findByIdForUpdate(request.doctorId())
                .orElseThrow(() -> doctorNotFound(request.doctorId()));

        LocalDateTime start = request.startTime();
        LocalDateTime end = start.plus(Appointment.DURATION);
        LocalDate day = start.toLocalDate();

        if (!start.isAfter(LocalDateTime.now(clock))) {
            throw new InvalidRequestException("Appointment cannot be in the past");
        }
        if (!scheduleSlotStarts(doctor, day).contains(start)) {
            throw new InvalidRequestException("Start time " + start + " is not a slot in the doctor's schedule");
        }
        if (appointmentRepository.existsOverlapping(doctor.getId(), SCHEDULED, start, end)) {
            throw new ConflictException("This slot is already booked");
        }
        if (appointmentRepository.existsForPatientOnDay(currentUser.getId(), doctor.getId(), SCHEDULED,
                day.atStartOfDay(), day.plusDays(1).atStartOfDay())) {
            throw new ConflictException("You already have an appointment with this doctor on " + day);
        }

        Appointment appointment = new Appointment();
        appointment.setPatient(userRepository.getReferenceById(currentUser.getId()));
        appointment.setDoctor(doctor);
        appointment.setStartTime(start);
        appointment.setEndTime(end);
        appointment.setStatus(SCHEDULED);
        appointment.setReason(request.reason());
        appointment.setCreatedAt(LocalDateTime.now(clock));

        try {
            // saveAndFlush sends the INSERT now, so a unique constraint violation is caught here.
            return AppointmentDto.from(appointmentRepository.saveAndFlush(appointment));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("This slot is already booked");
        }
    }

    @Transactional(readOnly = true)
    public List<AppointmentDto> findForPatient(Long patientId, AppointmentStatus status) {
        List<Appointment> appointments = status == null
                ? appointmentRepository.findByPatientId(patientId)
                : appointmentRepository.findByPatientIdAndStatus(patientId, status);

        // Upcoming appointments first (soonest first), then past ones (most recent first).
        LocalDateTime now = LocalDateTime.now(clock);
        Comparator<Appointment> byStart = Comparator.comparing(Appointment::getStartTime);
        Stream<Appointment> upcoming = appointments.stream()
                .filter(a -> !a.getStartTime().isBefore(now))
                .sorted(byStart);
        Stream<Appointment> past = appointments.stream()
                .filter(a -> a.getStartTime().isBefore(now))
                .sorted(byStart.reversed());

        return Stream.concat(upcoming, past).map(AppointmentDto::from).toList();
    }

    @Transactional
    public AppointmentDto cancel(Long id, User currentUser) {
        Appointment appointment = appointmentRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment with id " + id + " not found"));

        if (!appointment.getPatient().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only cancel your own appointments");
        }
        if (appointment.getStatus() != SCHEDULED) {
            throw new ConflictException("Only scheduled appointments can be cancelled");
        }
        if (appointment.getStartTime().isBefore(LocalDateTime.now(clock).plus(MIN_CANCEL_NOTICE))) {
            throw new ConflictException("Appointments can only be cancelled at least 24 hours in advance");
        }

        appointment.setStatus(CANCELLED);
        return AppointmentDto.from(appointment);
    }

    // All 30-minute slot start times from the doctor's schedule on that day of the week.
    private List<LocalDateTime> scheduleSlotStarts(Doctor doctor, LocalDate date) {
        List<LocalDateTime> starts = new ArrayList<>();
        doctor.getSchedules().stream()
                .filter(schedule -> schedule.getDayOfWeek() == date.getDayOfWeek())
                .findFirst()
                .ifPresent(schedule -> {
                    LocalDateTime slot = date.atTime(schedule.getStartTime());
                    LocalDateTime scheduleEnd = date.atTime(schedule.getEndTime());
                    while (!slot.plus(Appointment.DURATION).isAfter(scheduleEnd)) {
                        starts.add(slot);
                        slot = slot.plus(Appointment.DURATION);
                    }
                });
        return starts;
    }

    private static boolean overlaps(Appointment appointment, LocalDateTime start, LocalDateTime end) {
        return appointment.getStartTime().isBefore(end) && appointment.getEndTime().isAfter(start);
    }

    private static ResourceNotFoundException doctorNotFound(Long id) {
        return new ResourceNotFoundException("Doctor with id " + id + " not found");
    }
}
