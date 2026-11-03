package com.clinic.backend.config;

import com.clinic.backend.entity.Specialty;
import com.clinic.backend.repository.SpecialtyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final SpecialtyRepository specialtyRepository;

    @Override
    public void run(String... args) {
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
}
