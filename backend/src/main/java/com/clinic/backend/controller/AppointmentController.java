package com.clinic.backend.controller;

import com.clinic.backend.dto.AppointmentDto;
import com.clinic.backend.dto.CreateAppointmentRequest;
import com.clinic.backend.entity.AppointmentStatus;
import com.clinic.backend.entity.User;
import com.clinic.backend.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    // @AuthenticationPrincipal gives the logged-in User, so the patient never comes from the request body.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PATIENT')")
    public AppointmentDto create(@Valid @RequestBody CreateAppointmentRequest request,
                                 @AuthenticationPrincipal User currentUser) {
        return appointmentService.create(request, currentUser);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public List<AppointmentDto> findMine(@RequestParam(required = false) AppointmentStatus status,
                                         @AuthenticationPrincipal User currentUser) {
        return appointmentService.findForPatient(currentUser.getId(), status);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('PATIENT')")
    public AppointmentDto cancel(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        return appointmentService.cancel(id, currentUser);
    }
}
