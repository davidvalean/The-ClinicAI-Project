package com.clinic.backend.dto;

import com.clinic.backend.entity.Appointment;
import com.clinic.backend.entity.AppointmentStatus;
import com.clinic.backend.entity.User;

import java.time.LocalDateTime;

// An appointment as the doctor sees it: who the patient is, not which doctor it is.
public record DoctorAppointmentDto(
        Long id,
        LocalDateTime startTime,
        LocalDateTime endTime,
        AppointmentStatus status,
        String reason,
        Long patientId,
        String patientFirstName,
        String patientLastName
) {

    public static DoctorAppointmentDto from(Appointment appointment) {
        User patient = appointment.getPatient();
        return new DoctorAppointmentDto(
                appointment.getId(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus(),
                appointment.getReason(),
                patient.getId(),
                patient.getFirstName(),
                patient.getLastName()
        );
    }
}
