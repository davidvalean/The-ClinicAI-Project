package com.clinic.backend.controller;

import com.clinic.backend.entity.Appointment;
import com.clinic.backend.entity.AppointmentStatus;
import com.clinic.backend.entity.Doctor;
import com.clinic.backend.entity.User;
import com.clinic.backend.repository.AppointmentRepository;
import com.clinic.backend.repository.DoctorRepository;
import com.clinic.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Integration test over the whole stack (security -> controller -> service -> H2 database),
// using the accounts created by DataSeeder.
@SpringBootTest
@AutoConfigureMockMvc
class DoctorAdminApiTest {

    private static final String DOCTOR_EMAIL = "andrei.popescu@clinic.ro";

    private static final RequestPostProcessor ADMIN =
            SecurityMockMvcRequestPostProcessors.httpBasic("admin@clinic.ro", "Admin123!");
    private static final RequestPostProcessor PATIENT =
            SecurityMockMvcRequestPostProcessors.httpBasic("pacient@clinic.ro", "Pacient123!");
    private static final RequestPostProcessor DOCTOR =
            SecurityMockMvcRequestPostProcessors.httpBasic(DOCTOR_EMAIL, "Doctor123!");
    private static final RequestPostProcessor OTHER_DOCTOR =
            SecurityMockMvcRequestPostProcessors.httpBasic("elena.ionescu@clinic.ro", "Doctor123!");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private UserRepository userRepository;

    // ---------- access by role ----------

    @Test
    void anonymousGets401() throws Exception {
        mockMvc.perform(get("/api/appointments/doctor")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/appointments/1/complete")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/stats/specialties")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/stats/monthly")).andExpect(status().isUnauthorized());
    }

    @Test
    void patientGets403OnDoctorAndAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/appointments/doctor").with(PATIENT)).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/appointments/1/complete").with(PATIENT)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/stats/specialties").with(PATIENT)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/stats/monthly").with(PATIENT)).andExpect(status().isForbidden());
    }

    @Test
    void doctorGets403OnAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/stats/specialties").with(DOCTOR)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/stats/monthly").with(DOCTOR)).andExpect(status().isForbidden());
    }

    @Test
    void adminGets403OnDoctorEndpoints() throws Exception {
        mockMvc.perform(get("/api/appointments/doctor").with(ADMIN)).andExpect(status().isForbidden());
    }

    // ---------- doctor endpoints ----------

    @Test
    void doctorSeesOwnAppointments() throws Exception {
        mockMvc.perform(get("/api/appointments/doctor").with(DOCTOR)
                        .param("from", "2000-01-01").param("to", "2100-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(4))))
                .andExpect(jsonPath("$[0].patientLastName").value("Test"));
    }

    @Test
    void doctorCannotCombineDateWithInterval() throws Exception {
        mockMvc.perform(get("/api/appointments/doctor").with(DOCTOR)
                        .param("date", "2026-03-10").param("from", "2026-03-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void completeFollowsTheRules() throws Exception {
        Long id = pastScheduledAppointmentOfDoctor().getId();

        // Another doctor cannot complete it.
        mockMvc.perform(patch("/api/appointments/" + id + "/complete").with(OTHER_DOCTOR))
                .andExpect(status().isForbidden());

        // The owner can.
        mockMvc.perform(patch("/api/appointments/" + id + "/complete").with(DOCTOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Not twice.
        mockMvc.perform(patch("/api/appointments/" + id + "/complete").with(DOCTOR))
                .andExpect(status().isConflict());
    }

    @Test
    void completeMissingAppointmentGets404() throws Exception {
        mockMvc.perform(patch("/api/appointments/999999/complete").with(DOCTOR))
                .andExpect(status().isNotFound());
    }

    // ---------- admin statistics ----------

    @Test
    void adminGetsStatsPerSpecialty() throws Exception {
        mockMvc.perform(get("/api/admin/stats/specialties").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(8))))
                .andExpect(jsonPath("$[0].total").isNumber())
                .andExpect(jsonPath("$[0].completed").isNumber());

        mockMvc.perform(get("/api/admin/stats/specialties").with(ADMIN)
                        .param("from", "2026-12-31").param("to", "2026-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminGetsTwelveMonths() throws Exception {
        mockMvc.perform(get("/api/admin/stats/monthly").with(ADMIN).param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(12)))
                .andExpect(jsonPath("$[0].month").value(1))
                .andExpect(jsonPath("$[11].month").value(12));

        // A year with no appointments still returns 12 months, all with 0.
        mockMvc.perform(get("/api/admin/stats/monthly").with(ADMIN).param("year", "1990"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(12)))
                .andExpect(jsonPath("$[5].total").value(0));
    }

    // A SCHEDULED appointment of Dr. Popescu that started in the past, created only for this test.
    private Appointment pastScheduledAppointmentOfDoctor() {
        User doctorUser = userRepository.findByEmail(DOCTOR_EMAIL).orElseThrow();
        Doctor doctor = doctorRepository.findByUserId(doctorUser.getId()).orElseThrow();
        // 07:00 is outside every seeded slot, so it cannot collide with the seeded appointments.
        LocalDateTime start = LocalDateTime.now().minusDays(20).truncatedTo(ChronoUnit.DAYS).withHour(7);

        Appointment appointment = new Appointment();
        appointment.setPatient(userRepository.findByEmail("pacient@clinic.ro").orElseThrow());
        appointment.setDoctor(doctor);
        appointment.setStartTime(start);
        appointment.setEndTime(start.plus(Appointment.DURATION));
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setCreatedAt(LocalDateTime.now());
        return appointmentRepository.save(appointment);
    }
}
