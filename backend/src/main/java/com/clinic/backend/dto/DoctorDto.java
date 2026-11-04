package com.clinic.backend.dto;

import com.clinic.backend.entity.Doctor;

public record DoctorDto(
        Long id,
        String firstName,
        String lastName,
        String bio,
        Long specialtyId,
        String specialtyName
) {

    public static DoctorDto from(Doctor doctor) {
        return new DoctorDto(
                doctor.getId(),
                doctor.getUser().getFirstName(),
                doctor.getUser().getLastName(),
                doctor.getBio(),
                doctor.getSpecialty().getId(),
                doctor.getSpecialty().getName()
        );
    }
}
