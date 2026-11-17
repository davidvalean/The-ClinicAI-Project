package com.clinic.backend.service;

import com.clinic.backend.dto.DoctorAppointmentDto;
import com.clinic.backend.entity.Appointment;
import com.clinic.backend.entity.AppointmentStatus;
import com.clinic.backend.entity.Doctor;
import com.clinic.backend.entity.User;
import com.clinic.backend.exception.ConflictException;
import com.clinic.backend.exception.InvalidRequestException;
import com.clinic.backend.exception.ResourceNotFoundException;
import com.clinic.backend.repository.AppointmentRepository;
import com.clinic.backend.repository.DoctorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Pure unit test: the repositories are mocks and "now" is fixed, so no database or Spring context is needed.
@ExtendWith(MockitoExtension.class)
class DoctorAppointmentServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Bucharest");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 10, 12, 0);

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    private DoctorAppointmentService service;

    private User doctorUser;
    private User otherDoctorUser;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
        service = new DoctorAppointmentService(appointmentRepository, doctorRepository, fixedClock);

        doctorUser = user(10L, "Andrei", "Popescu");
        otherDoctorUser = user(20L, "Elena", "Ionescu");
        doctor = new Doctor();
        doctor.setId(1L);
        doctor.setUser(doctorUser);
    }

    // ---------- complete ----------

    @Test
    void completeMissingAppointmentThrowsNotFound() {
        when(appointmentRepository.findWithPatientById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.complete(99L, doctorUser))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void completeAppointmentOfAnotherDoctorThrowsAccessDenied() {
        Appointment appointment = appointment(AppointmentStatus.SCHEDULED, NOW.minusHours(2));
        when(appointmentRepository.findWithPatientById(5L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.complete(5L, otherDoctorUser))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    void completeCancelledAppointmentThrowsConflict() {
        Appointment appointment = appointment(AppointmentStatus.CANCELLED, NOW.minusHours(2));
        when(appointmentRepository.findWithPatientById(5L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.complete(5L, doctorUser))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("scheduled");
    }

    @Test
    void completeAlreadyCompletedAppointmentThrowsConflict() {
        Appointment appointment = appointment(AppointmentStatus.COMPLETED, NOW.minusHours(2));
        when(appointmentRepository.findWithPatientById(5L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.complete(5L, doctorUser))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void completeFutureAppointmentThrowsConflict() {
        Appointment appointment = appointment(AppointmentStatus.SCHEDULED, NOW.plusHours(1));
        when(appointmentRepository.findWithPatientById(5L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.complete(5L, doctorUser))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("started");
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    void completeAppointmentStartingExactlyNowThrowsConflict() {
        Appointment appointment = appointment(AppointmentStatus.SCHEDULED, NOW);
        when(appointmentRepository.findWithPatientById(5L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.complete(5L, doctorUser))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void completePastScheduledAppointmentOfOwnDoctorSucceeds() {
        Appointment appointment = appointment(AppointmentStatus.SCHEDULED, NOW.minusMinutes(30));
        when(appointmentRepository.findWithPatientById(5L)).thenReturn(Optional.of(appointment));

        DoctorAppointmentDto result = service.complete(5L, doctorUser);

        assertThat(result.status()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(result.patientLastName()).isEqualTo("Pacient");
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    // ---------- findForDoctor (filters) ----------

    @Test
    void withoutFiltersListsFromTodayOn() {
        when(doctorRepository.findByUserId(doctorUser.getId())).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findForDoctorBetween(
                1L, NOW.toLocalDate().atStartOfDay(), LocalDate.of(9999, 12, 31).atStartOfDay()))
                .thenReturn(List.of(appointment(AppointmentStatus.SCHEDULED, NOW.plusDays(1))));

        assertThat(service.findForDoctor(doctorUser, null, null, null)).hasSize(1);
    }

    @Test
    void dateFilterCoversExactlyThatDay() {
        LocalDate day = LocalDate.of(2026, 3, 15);
        when(doctorRepository.findByUserId(doctorUser.getId())).thenReturn(Optional.of(doctor));

        service.findForDoctor(doctorUser, day, null, null);

        verify(appointmentRepository).findForDoctorBetween(1L, day.atStartOfDay(), day.plusDays(1).atStartOfDay());
    }

    @Test
    void dateTogetherWithIntervalIsRejected() {
        LocalDate day = LocalDate.of(2026, 3, 15);

        assertThatThrownBy(() -> service.findForDoctor(doctorUser, day, day, null))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void reversedIntervalIsRejected() {
        assertThatThrownBy(() -> service.findForDoctor(doctorUser, null,
                LocalDate.of(2026, 3, 20), LocalDate.of(2026, 3, 10)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void userWithoutDoctorProfileIsDenied() {
        when(doctorRepository.findByUserId(doctorUser.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findForDoctor(doctorUser, null, null, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Appointment appointment(AppointmentStatus status, LocalDateTime start) {
        Appointment appointment = new Appointment();
        appointment.setId(5L);
        appointment.setDoctor(doctor);
        appointment.setPatient(user(30L, "Test", "Pacient"));
        appointment.setStartTime(start);
        appointment.setEndTime(start.plus(Appointment.DURATION));
        appointment.setStatus(status);
        return appointment;
    }

    private static User user(Long id, String firstName, String lastName) {
        User user = new User();
        user.setId(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return user;
    }
}
