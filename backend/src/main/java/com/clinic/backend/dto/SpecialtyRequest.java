package com.clinic.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SpecialtyRequest(
        @NotBlank(message = "Numele este obligatoriu")
        @Size(max = 100, message = "Numele poate avea maxim 100 de caractere")
        String name,

        @Size(max = 1000, message = "Descrierea poate avea maxim 1000 de caractere")
        String description
) {}
