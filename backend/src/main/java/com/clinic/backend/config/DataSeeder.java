package com.clinic.backend.config;

import com.clinic.backend.entity.Appointment;
import com.clinic.backend.entity.AppointmentStatus;
import com.clinic.backend.entity.Doctor;
import com.clinic.backend.entity.Role;
import com.clinic.backend.entity.Specialty;
import com.clinic.backend.entity.User;
import com.clinic.backend.repository.AppointmentRepository;
import com.clinic.backend.repository.DoctorRepository;
import com.clinic.backend.repository.SpecialtyRepository;
import com.clinic.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.clinic.backend.entity.AppointmentStatus.CANCELLED;
import static com.clinic.backend.entity.AppointmentStatus.COMPLETED;
import static com.clinic.backend.entity.AppointmentStatus.SCHEDULED;
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
    private final AppointmentRepository appointmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Override
    @Transactional
    public void run(String... args) {
        seedSpecialties();
        seedUser("Admin", "Clinic", "admin@clinic.ro", "Admin123!", Role.ADMIN);
        seedUser("Pacient", "Test", "pacient@clinic.ro", "Pacient123!", Role.PATIENT);
        seedDoctors();
        seedAppointments();
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

    // Past and future appointments of the test patient, with different statuses, spread over
    // several months so the admin statistics have something to show. Dates are relative to today
    // and moved off weekends.
    // It uses the doctors that already exist in the database (first ones by id) instead of fixed
    // emails, because an older database may contain doctors created by an earlier version of this seeder.
    // Every entry has a different time of day, so even if the same doctor is reused for several
    // entries, two of them can never fall in the same slot.
    private void seedAppointments() {
        if (appointmentRepository.count() > 0) {
            return;
        }
        List<Doctor> doctors = doctorRepository.findAll(Sort.by("id"));
        User patient = userRepository.findByEmail("pacient@clinic.ro").orElse(null);
        if (doctors.isEmpty() || patient == null) {
            return;
        }

        // The first doctor (lowest id) gets the most entries; it is the one to log in with for the demo.
        appointmentRepository.saveAll(List.of(
                appointment(patient, doctors, 0, -14, "09:00", COMPLETED, "Annual check-up"),
                appointment(patient, doctors, 0, -3, "09:30", SCHEDULED, "Chest pain follow-up"),
                appointment(patient, doctors, 0, 2, "10:00", SCHEDULED, "ECG results"),
                appointment(patient, doctors, 0, 5, "10:30", CANCELLED, "Blood pressure check"),
                appointment(patient, doctors, 1, -30, "11:00", COMPLETED, "Skin rash"),
                appointment(patient, doctors, 1, 3, "11:30", SCHEDULED, "Mole check"),
                appointment(patient, doctors, 2, -60, "12:00", COMPLETED, "Vaccination"),
                appointment(patient, doctors, 2, -10, "12:30", CANCELLED, "Fever"),
                appointment(patient, doctors, 3, 7, "13:00", SCHEDULED, "Eye exam"),
                appointment(patient, doctors, 4, -45, "13:30", COMPLETED, "Diabetes control"),
                appointment(patient, doctors, 4, -2, "14:00", SCHEDULED, "Blood test review")
        ));
    }

    private Appointment appointment(User patient, List<Doctor> doctors, int doctorIndex, int daysFromToday,
                                    String time, AppointmentStatus status, String reason) {
        LocalDateTime start = weekday(LocalDate.now(clock).plusDays(daysFromToday), daysFromToday < 0 ? -1 : 1)
                .atTime(LocalTime.parse(time));

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        // With fewer doctors than indexes, the list is reused from the beginning.
        appointment.setDoctor(doctors.get(doctorIndex % doctors.size()));
        appointment.setStartTime(start);
        appointment.setEndTime(start.plus(Appointment.DURATION));
        appointment.setStatus(status);
        appointment.setReason(reason);
        appointment.setCreatedAt(LocalDateTime.now(clock));
        return appointment;
    }

    // Moves a weekend date to the nearest weekday in the given direction (-1 = back, +1 = forward).
    private static LocalDate weekday(LocalDate date, int step) {
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(step);
        }
        return date;
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
