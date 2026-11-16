package com.clinic.backend.service;

import com.clinic.backend.dto.DoctorCreateRequest;
import com.clinic.backend.dto.DoctorDto;
import com.clinic.backend.dto.DoctorProfileDto;
import com.clinic.backend.dto.DoctorScheduleRequest;
import com.clinic.backend.dto.DoctorUpdateRequest;
import com.clinic.backend.entity.Doctor;
import com.clinic.backend.entity.Role;
import com.clinic.backend.entity.Specialty;
import com.clinic.backend.entity.User;
import com.clinic.backend.exception.DuplicateResourceException;
import com.clinic.backend.exception.ResourceNotFoundException;
import com.clinic.backend.repository.DoctorRepository;
import com.clinic.backend.repository.SpecialtyRepository;
import com.clinic.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private static final Sort BY_NAME = Sort.by("user.lastName", "user.firstName");

    private final DoctorRepository doctorRepository;
    private final SpecialtyRepository specialtyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<DoctorDto> findAll(Long specialtyId) {
        List<Doctor> doctors = specialtyId == null
                ? doctorRepository.findAll(BY_NAME)
                : doctorRepository.findBySpecialtyId(specialtyId, BY_NAME);
        return doctors.stream().map(DoctorDto::from).toList();
    }

    @Transactional(readOnly = true)
    public DoctorProfileDto findById(Long id) {
        return DoctorProfileDto.from(getOrThrow(id));
    }

    @Transactional
    public DoctorProfileDto create(DoctorCreateRequest request) {
        String email = request.email().trim();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateResourceException("Email \"" + email + "\" is already used");
        }

        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setRole(Role.DOCTOR);
        user.setCreatedAt(LocalDateTime.now());

        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setSpecialty(getSpecialtyOrThrow(request.specialtyId()));
        doctor.setBio(request.bio());
        if (request.schedule() != null) {
            addSchedule(doctor, request.schedule());
        }

        return DoctorProfileDto.from(doctorRepository.save(doctor));
    }

    @Transactional
    public DoctorProfileDto update(Long id, DoctorUpdateRequest request) {
        Doctor doctor = getOrThrow(id);

        User user = doctor.getUser();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(request.phone());
        doctor.setSpecialty(getSpecialtyOrThrow(request.specialtyId()));
        doctor.setBio(request.bio());

        if (request.schedule() != null) {
            doctor.getSchedules().clear();
            // Hibernate runs INSERTs before DELETEs, so without this flush the new rows
            // would collide with the old ones on the (doctor_id, day_of_week) constraint.
            doctorRepository.flush();
            addSchedule(doctor, request.schedule());
        }

        return DoctorProfileDto.from(doctorRepository.save(doctor));
    }

    private void addSchedule(Doctor doctor, List<DoctorScheduleRequest> schedule) {
        Set<DayOfWeek> seenDays = EnumSet.noneOf(DayOfWeek.class);
        for (DoctorScheduleRequest entry : schedule) {
            if (!seenDays.add(entry.dayOfWeek())) {
                throw new DuplicateResourceException("Schedule contains " + entry.dayOfWeek() + " more than once");
            }
            doctor.addSchedule(entry.dayOfWeek(), entry.startTime(), entry.endTime());
        }
    }

    private Doctor getOrThrow(Long id) {
        return doctorRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor with id " + id + " not found"));
    }

    private Specialty getSpecialtyOrThrow(Long id) {
        return specialtyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Specialty with id " + id + " not found"));
    }
}
