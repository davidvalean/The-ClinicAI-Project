package com.clinic.backend.dto;

import com.clinic.backend.entity.Specialty;

public record SpecialtyDto(Long id, String name, String description) {

    public static SpecialtyDto from(Specialty specialty) {
        return new SpecialtyDto(specialty.getId(), specialty.getName(), specialty.getDescription());
    }
}
