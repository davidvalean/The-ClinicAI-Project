package com.clinic.backend.controller;

import com.clinic.backend.dto.DoctorAppointmentDto;
import com.clinic.backend.entity.User;
import com.clinic.backend.service.DoctorAppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

// Doctor-side appointment endpoints. Same base path as AppointmentController (patient side),
// kept in a separate class so each role's endpoints stay together.
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorAppointmentController {

    private final DoctorAppointmentService doctorAppointmentService;

    // The doctor is taken from the logged-in user, never from the request.
    @GetMapping("/doctor")
    public List<DoctorAppointmentDto> findMine(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal User currentUser) {
        return doctorAppointmentService.findForDoctor(currentUser, date, from, to);
    }

    @PatchMapping("/{id}/complete")
    public DoctorAppointmentDto complete(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        return doctorAppointmentService.complete(id, currentUser);
    }
}
