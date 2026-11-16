package com.clinic.backend.dto;

import com.clinic.backend.entity.Doctor;
import com.clinic.backend.entity.DoctorSchedule;

import java.util.Comparator;
import java.util.List;

public record DoctorProfileDto(
        Long id,
        String firstName,
        String lastName,
        String bio,
        Long specialtyId,
        String specialtyName,
        List<DoctorScheduleDto> schedule
) {

    public static DoctorProfileDto from(Doctor doctor) {
        List<DoctorScheduleDto> schedule = doctor.getSchedules().stream()
                .sorted(Comparator.comparing(DoctorSchedule::getDayOfWeek))
                .map(DoctorScheduleDto::from)
                .toList();

        return new DoctorProfileDto(
                doctor.getId(),
                doctor.getUser().getFirstName(),
                doctor.getUser().getLastName(),
                doctor.getBio(),
                doctor.getSpecialty().getId(),
                doctor.getSpecialty().getName(),
                schedule
        );
    }
}
