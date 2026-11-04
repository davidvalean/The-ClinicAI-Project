package com.clinic.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// Email and password are account data, so they are not edited here.
// A null schedule keeps the current one; a list replaces it entirely.
public record DoctorUpdateRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must be at most 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must be at most 100 characters")
        String lastName,

        String phone,

        @NotNull(message = "Specialty is required")
        Long specialtyId,

        @Size(max = 1000, message = "Bio must be at most 1000 characters")
        String bio,

        List<@Valid DoctorScheduleRequest> schedule
) {}
