package com.clinic.backend.dto;

import com.clinic.backend.entity.Appointment;
import com.clinic.backend.entity.AppointmentStatus;
import com.clinic.backend.entity.Doctor;

import java.time.LocalDateTime;

public record AppointmentDto(
        Long id,
        Long doctorId,
        String doctorFirstName,
        String doctorLastName,
        String specialtyName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        AppointmentStatus status,
        String reason,
        LocalDateTime createdAt
) {

    public static AppointmentDto from(Appointment appointment) {
        Doctor doctor = appointment.getDoctor();
        return new AppointmentDto(
                appointment.getId(),
                doctor.getId(),
                doctor.getUser().getFirstName(),
                doctor.getUser().getLastName(),
                doctor.getSpecialty().getName(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus(),
                appointment.getReason(),
                appointment.getCreatedAt()
        );
    }
}
