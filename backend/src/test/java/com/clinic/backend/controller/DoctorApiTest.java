package com.clinic.backend.controller;

import com.clinic.backend.entity.Doctor;
import com.clinic.backend.entity.Specialty;
import com.clinic.backend.repository.DoctorRepository;
import com.clinic.backend.repository.SpecialtyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DoctorApiTest {

    private static final RequestPostProcessor ADMIN =
            SecurityMockMvcRequestPostProcessors.httpBasic("admin@clinic.ro", "Admin123!");
    private static final RequestPostProcessor PATIENT =
            SecurityMockMvcRequestPostProcessors.httpBasic("pacient@clinic.ro", "Pacient123!");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private SpecialtyRepository specialtyRepository;

    private Long specialtyId(String name) {
        return specialtyRepository.findAll().stream()
                .filter(specialty -> specialty.getName().equals(name))
                .map(Specialty::getId)
                .findFirst()
                .orElseThrow();
    }

    private Doctor seededDoctor() {
        return doctorRepository.findAll().get(0);
    }

    private String doctorJson(String email, Long specialtyId, String schedule) {
        return """
                {"firstName":"Test","lastName":"Doctor","email":"%s","password":"Parola123!",
                 "specialtyId":%d,"schedule":%s}
                """.formatted(email, specialtyId, schedule);
    }

    @Test
    void seederCreatesDoctorsWithSchedules() throws Exception {
        mockMvc.perform(get("/api/specialties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(8))));

        mockMvc.perform(get("/api/doctors/" + seededDoctor().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedule").isNotEmpty());
    }

    @Test
    void listIsPublicAndCanBeFilteredBySpecialty() throws Exception {
        mockMvc.perform(get("/api/doctors").param("specialtyId", specialtyId("Pediatrics").toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].specialtyName").value("Pediatrics"));
    }

    @Test
    void missingDoctorReturns404() throws Exception {
        mockMvc.perform(get("/api/doctors/999999")).andExpect(status().isNotFound());
    }

    @Test
    void adminCanCreateSpecialty() throws Exception {
        mockMvc.perform(post("/api/specialties").with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Radiology\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void adminCanCreateDoctorWithSchedule() throws Exception {
        String body = doctorJson("nou@clinic.ro", specialtyId("Neurology"),
                "[{\"dayOfWeek\":\"TUESDAY\",\"startTime\":\"09:00\",\"endTime\":\"12:00\"}]");

        mockMvc.perform(post("/api/doctors").with(ADMIN).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.specialtyName").value("Neurology"))
                .andExpect(jsonPath("$.schedule", hasSize(1)));

        // The same email cannot be used twice.
        mockMvc.perform(post("/api/doctors").with(ADMIN).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void adminCanReplaceSchedule() throws Exception {
        // The replacement reuses a day that already exists, which exercises the unique constraint.
        Long id = seededDoctor().getId();
        String body = """
                {"firstName":"Nume","lastName":"Nou","specialtyId":%d,
                 "schedule":[{"dayOfWeek":"MONDAY","startTime":"07:00","endTime":"11:00"}]}
                """.formatted(specialtyId("Cardiology"));

        mockMvc.perform(put("/api/doctors/" + id).with(ADMIN).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/doctors/" + id))
                .andExpect(jsonPath("$.lastName").value("Nou"))
                .andExpect(jsonPath("$.schedule", hasSize(1)))
                .andExpect(jsonPath("$.schedule[0].startTime").value("07:00:00"));
    }

    @Test
    void invalidScheduleIsRejected() throws Exception {
        String reversedHours = doctorJson("a@clinic.ro", specialtyId("Neurology"),
                "[{\"dayOfWeek\":\"MONDAY\",\"startTime\":\"12:00\",\"endTime\":\"09:00\"}]");
        mockMvc.perform(post("/api/doctors").with(ADMIN).contentType(MediaType.APPLICATION_JSON).content(reversedHours))
                .andExpect(status().isBadRequest());

        String duplicatedDay = doctorJson("b@clinic.ro", specialtyId("Neurology"), """
                [{"dayOfWeek":"MONDAY","startTime":"08:00","endTime":"09:00"},
                 {"dayOfWeek":"MONDAY","startTime":"10:00","endTime":"11:00"}]""");
        mockMvc.perform(post("/api/doctors").with(ADMIN).contentType(MediaType.APPLICATION_JSON).content(duplicatedDay))
                .andExpect(status().isConflict());
    }

    @Test
    void onlyAdminCanWrite() throws Exception {
        String body = doctorJson("c@clinic.ro", specialtyId("Neurology"), "[]");

        mockMvc.perform(post("/api/doctors").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/doctors").with(PATIENT).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }
}
