package com.clinic.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "appointments",
        // A doctor cannot have two active appointments with the same start time.
        // Cancelled appointments have active_start_time = NULL, and NULLs never collide
        // in a unique constraint, so a cancelled slot can be booked again.
        uniqueConstraints = @UniqueConstraint(
                name = "uk_appointments_doctor_active_slot",
                columnNames = {"doctor_id", "active_start_time"}
        ),
        indexes = @Index(name = "idx_appointments_patient", columnList = "patient_id")
)
@Getter
@Setter
@NoArgsConstructor
public class Appointment {

    public static final Duration DURATION = Duration.ofMinutes(30);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Copy of startTime while the appointment is not cancelled. It exists only for the
    // unique constraint above and is kept in sync automatically before every insert/update.
    @Setter(AccessLevel.NONE)
    @Column(name = "active_start_time")
    private LocalDateTime activeStartTime;

    @PrePersist
    @PreUpdate
    void syncActiveStartTime() {
        activeStartTime = status == AppointmentStatus.CANCELLED ? null : startTime;
    }
}
