package com.clinic.backend.config;

import com.clinic.backend.entity.Doctor;
import com.clinic.backend.entity.Role;
import com.clinic.backend.entity.Specialty;
import com.clinic.backend.entity.User;
import com.clinic.backend.repository.DoctorRepository;
import com.clinic.backend.repository.SpecialtyRepository;
import com.clinic.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;

// Test data for local development. Each group is checked separately,
// so a database that already has specialties still receives the doctors.
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final String DOCTOR_PASSWORD = "Doctor123!";

    private static final List<DayOfWeek> ALL_WEEK = List.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY);
    private static final List<DayOfWeek> MON_WED_FRI = List.of(MONDAY, WEDNESDAY, FRIDAY);
    private static final List<DayOfWeek> TUE_THU = List.of(TUESDAY, THURSDAY);

    private final SpecialtyRepository specialtyRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedSpecialties();
        seedUser("Admin", "Clinic", "admin@clinic.ro", "Admin123!", Role.ADMIN);
        seedUser("Pacient", "Test", "pacient@clinic.ro", "Pacient123!", Role.PATIENT);
        seedDoctors();
    }

    private void seedSpecialties() {
        if (specialtyRepository.count() > 0) {
            return;
        }
        specialtyRepository.saveAll(List.of(
                new Specialty(null, "Cardiology", "Diagnosis and treatment of heart and blood vessel disorders."),
                new Specialty(null, "Dermatology", "Conditions of the skin, hair and nails."),
                new Specialty(null, "Neurology", "Disorders of the central and peripheral nervous system."),
                new Specialty(null, "Pediatrics", "Medical care for infants, children and adolescents."),
                new Specialty(null, "Orthopedics", "Conditions of the bones, joints and muscles."),
                new Specialty(null, "Ophthalmology", "Diagnosis and treatment of eye diseases."),
                new Specialty(null, "Internal Medicine", "Diagnosis and treatment of internal organ diseases in adults."),
                new Specialty(null, "Otolaryngology (ENT)", "Conditions of the ear, nose and throat.")
        ));
    }

    private void seedUser(String firstName, String lastName, String email, String password, Role role) {
        if (userRepository.findByEmail(email).isEmpty()) {
            userRepository.save(buildUser(firstName, lastName, email, password, role));
        }
    }

    private void seedDoctors() {
        if (doctorRepository.count() > 0) {
            return;
        }
        Map<String, Specialty> specialties = specialtyRepository.findAll().stream()
                .collect(Collectors.toMap(Specialty::getName, Function.identity()));

        doctorRepository.saveAll(List.of(
                doctor("Andrei", "Popescu", specialties.get("Cardiology"), "Interventional cardiologist.", ALL_WEEK, 9, 17),
                doctor("Elena", "Ionescu", specialties.get("Cardiology"), "Specialist in heart failure.", MON_WED_FRI, 8, 14),
                doctor("Maria", "Dumitrescu", specialties.get("Dermatology"), "Medical and aesthetic dermatology.", ALL_WEEK, 10, 18),
                doctor("Radu", "Stan", specialties.get("Neurology"), "Focused on migraines and epilepsy.", TUE_THU, 9, 15),
                doctor("Ioana", "Marin", specialties.get("Neurology"), "Stroke recovery and rehabilitation.", MON_WED_FRI, 12, 18),
                doctor("Mihai", "Georgescu", specialties.get("Pediatrics"), "General pediatrics.", ALL_WEEK, 8, 16),
                doctor("Ana", "Stoica", specialties.get("Pediatrics"), "Pediatric allergies and asthma.", TUE_THU, 10, 16),
                doctor("Vlad", "Constantin", specialties.get("Orthopedics"), "Sports injuries.", MON_WED_FRI, 9, 17),
                doctor("Cristina", "Munteanu", specialties.get("Ophthalmology"), "Cataract and refractive surgery.", ALL_WEEK, 9, 15),
                doctor("Bogdan", "Florea", specialties.get("Internal Medicine"), "Chronic disease management.", ALL_WEEK, 8, 14),
                doctor("Laura", "Nistor", specialties.get("Internal Medicine"), "Preventive medicine.", TUE_THU, 12, 18),
                doctor("Paul", "Rusu", specialties.get("Otolaryngology (ENT)"), "Sinus and hearing disorders.", MON_WED_FRI, 10, 16)
        ));
    }

    private Doctor doctor(String firstName, String lastName, Specialty specialty, String bio,
                          List<DayOfWeek> days, int startHour, int endHour) {
        String email = (firstName + "." + lastName).toLowerCase(Locale.ROOT) + "@clinic.ro";

        Doctor doctor = new Doctor();
        doctor.setUser(buildUser(firstName, lastName, email, DOCTOR_PASSWORD, Role.DOCTOR));
        doctor.setSpecialty(specialty);
        doctor.setBio(bio);
        days.forEach(day -> doctor.addSchedule(day, LocalTime.of(startHour, 0), LocalTime.of(endHour, 0)));
        return doctor;
    }

    private User buildUser(String firstName, String lastName, String email, String password, Role role) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}
