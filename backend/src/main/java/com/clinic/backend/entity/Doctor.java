package com.clinic.backend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The login account of the doctor (name, email, password, role DOCTOR).
    @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "specialty_id", nullable = false)
    private Specialty specialty;

    @Column(length = 1000)
    private String bio;

    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DoctorSchedule> schedules = new ArrayList<>();

    // Keeps both sides of the relationship in sync.
    public void addSchedule(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        schedules.add(new DoctorSchedule(null, this, dayOfWeek, startTime, endTime));
    }
}
