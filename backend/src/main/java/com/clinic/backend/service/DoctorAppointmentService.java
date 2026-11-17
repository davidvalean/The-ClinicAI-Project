package com.clinic.backend.service;

import com.clinic.backend.dto.DoctorAppointmentDto;
import com.clinic.backend.entity.Appointment;
import com.clinic.backend.entity.Doctor;
import com.clinic.backend.entity.User;
import com.clinic.backend.exception.ConflictException;
import com.clinic.backend.exception.InvalidRequestException;
import com.clinic.backend.exception.ResourceNotFoundException;
import com.clinic.backend.repository.AppointmentRepository;
import com.clinic.backend.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.clinic.backend.entity.AppointmentStatus.COMPLETED;
import static com.clinic.backend.entity.AppointmentStatus.SCHEDULED;

// What a logged-in doctor can do with their own appointments.
@Service
@RequiredArgsConstructor
public class DoctorAppointmentService {

    // Upper bound used when no end date is given, so the repository needs a single query.
    private static final LocalDateTime FAR_FUTURE = LocalDate.of(9999, 12, 31).atStartOfDay();

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    // Injected instead of calling LocalDateTime.now() directly, so tests can fix "now".
    private final Clock clock;

    // date        -> only that day
    // from / to   -> that interval, both days included (each one is optional)
    // no filters  -> from today on
    @Transactional(readOnly = true)
    public List<DoctorAppointmentDto> findForDoctor(User currentUser, LocalDate date, LocalDate from, LocalDate to) {
        if (date != null && (from != null || to != null)) {
            throw new InvalidRequestException("Use either 'date' or 'from'/'to', not both");
        }

        LocalDate firstDay = date != null ? date : (from != null ? from : LocalDate.now(clock));
        LocalDate lastDay = date != null ? date : to;
        if (lastDay != null && lastDay.isBefore(firstDay)) {
            throw new InvalidRequestException("'to' (" + lastDay + ") must not be before 'from' (" + firstDay + ")");
        }

        Doctor doctor = currentDoctor(currentUser);
        LocalDateTime start = firstDay.atStartOfDay();
        // The end is exclusive, so we stop at the beginning of the day after lastDay.
        LocalDateTime end = lastDay != null ? lastDay.plusDays(1).atStartOfDay() : FAR_FUTURE;

        return appointmentRepository.findForDoctorBetween(doctor.getId(), start, end).stream()
                .map(DoctorAppointmentDto::from)
                .toList();
    }

    @Transactional
    public DoctorAppointmentDto complete(Long id, User currentUser) {
        Appointment appointment = appointmentRepository.findWithPatientById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment with id " + id + " not found"));

        if (!appointment.getDoctor().getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only complete your own appointments");
        }
        if (appointment.getStatus() != SCHEDULED) {
            throw new ConflictException("Only scheduled appointments can be completed");
        }
        if (!appointment.getStartTime().isBefore(LocalDateTime.now(clock))) {
            throw new ConflictException("An appointment can only be completed after it has started");
        }

        // No save() needed: the entity is managed, so the change is written when the transaction commits.
        appointment.setStatus(COMPLETED);
        return DoctorAppointmentDto.from(appointment);
    }

    private Doctor currentDoctor(User currentUser) {
        return doctorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("No doctor profile is linked to this account"));
    }
}
