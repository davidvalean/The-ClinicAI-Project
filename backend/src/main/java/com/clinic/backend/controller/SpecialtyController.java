package com.clinic.backend.controller;

import com.clinic.backend.dto.SpecialtyDto;
import com.clinic.backend.dto.SpecialtyRequest;
import com.clinic.backend.service.SpecialtyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/specialties")
@RequiredArgsConstructor
public class SpecialtyController {

    private final SpecialtyService specialtyService;

    @GetMapping
    public List<SpecialtyDto> findAll() {
        return specialtyService.findAll();
    }

    @GetMapping("/{id}")
    public SpecialtyDto findById(@PathVariable Long id) {
        return specialtyService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public SpecialtyDto create(@Valid @RequestBody SpecialtyRequest request) {
        return specialtyService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SpecialtyDto update(@PathVariable Long id, @Valid @RequestBody SpecialtyRequest request) {
        return specialtyService.update(id, request);
    }
}
