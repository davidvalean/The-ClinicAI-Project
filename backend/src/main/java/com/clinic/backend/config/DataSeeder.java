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
                new Specialty(null, "Cardiologie", "Diagnosticul și tratamentul afecțiunilor inimii și vaselor de sânge."),
                new Specialty(null, "Dermatologie", "Afecțiuni ale pielii, părului și unghiilor."),
                new Specialty(null, "Neurologie", "Afecțiuni ale sistemului nervos central și periferic."),
                new Specialty(null, "Pediatrie", "Îngrijirea medicală a copiilor și adolescenților."),
                new Specialty(null, "Ortopedie", "Afecțiuni ale oaselor, articulațiilor și mușchilor."),
                new Specialty(null, "Oftalmologie", "Diagnosticul și tratamentul bolilor ochilor."),
                new Specialty(null, "Medicină internă", "Diagnosticul și tratamentul bolilor organelor interne la adulți."),
                new Specialty(null, "ORL", "Afecțiuni ale urechii, nasului și gâtului.")
        ));
    }
}
