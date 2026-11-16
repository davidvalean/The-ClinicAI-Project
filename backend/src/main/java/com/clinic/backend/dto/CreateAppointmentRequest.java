package com.clinic.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

// There is no patientId here on purpose: the patient is always the logged-in user.
public record CreateAppointmentRequest(
        @NotNull(message = "Doctor is required")
        Long doctorId,

        @NotNull(message = "Start time is required")
        LocalDateTime startTime,

        @Size(max = 500, message = "Reason must be at most 500 characters")
        String reason
) {}
