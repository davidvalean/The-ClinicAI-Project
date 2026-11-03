package com.clinic.backend.service;

import com.clinic.backend.dto.SpecialtyDto;
import com.clinic.backend.dto.SpecialtyRequest;
import com.clinic.backend.entity.Specialty;
import com.clinic.backend.exception.DuplicateResourceException;
import com.clinic.backend.exception.ResourceNotFoundException;
import com.clinic.backend.repository.SpecialtyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpecialtyService {

    private final SpecialtyRepository specialtyRepository;

    @Transactional(readOnly = true)
    public List<SpecialtyDto> findAll() {
        return specialtyRepository.findAll(Sort.by("name")).stream()
                .map(SpecialtyDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SpecialtyDto findById(Long id) {
        return SpecialtyDto.from(getOrThrow(id));
    }

    @Transactional
    public SpecialtyDto create(SpecialtyRequest request) {
        String name = request.name().trim();
        if (specialtyRepository.existsByNameIgnoreCase(name)) {
            throw duplicate(name);
        }
        Specialty specialty = new Specialty(null, name, request.description());
        return SpecialtyDto.from(specialtyRepository.save(specialty));
    }

    @Transactional
    public SpecialtyDto update(Long id, SpecialtyRequest request) {
        Specialty specialty = getOrThrow(id);
        String name = request.name().trim();

        // Check uniqueness only if the name actually changed (case-insensitive);
        // otherwise existsByNameIgnoreCase would match the current record itself.
        if (!specialty.getName().equalsIgnoreCase(name)
                && specialtyRepository.existsByNameIgnoreCase(name)) {
            throw duplicate(name);
        }

        specialty.setName(name);
        specialty.setDescription(request.description());
        return SpecialtyDto.from(specialtyRepository.save(specialty));
    }

    private Specialty getOrThrow(Long id) {
        return specialtyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Specialty with id " + id + " not found"));
    }

    private DuplicateResourceException duplicate(String name) {
        return new DuplicateResourceException("Specialty \"" + name + "\" already exists");
    }
}
