package com.clinic.backend.dto;

// Number of appointments of one specialty, in total and split by status.
// Built directly by the JPQL query in AppointmentRepository.countBySpecialty.
public record SpecialtyStatsDto(
        Long specialtyId,
        String specialtyName,
        Long total,
        Long scheduled,
        Long completed,
        Long cancelled
) {
}
