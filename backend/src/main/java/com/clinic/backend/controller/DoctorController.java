package com.clinic.backend.controller;

import com.clinic.backend.dto.DoctorCreateRequest;
import com.clinic.backend.dto.DoctorDto;
import com.clinic.backend.dto.DoctorProfileDto;
import com.clinic.backend.dto.DoctorUpdateRequest;
import com.clinic.backend.service.DoctorService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping
    public List<DoctorDto> findAll(@RequestParam(required = false) Long specialtyId) {
        return doctorService.findAll(specialtyId);
    }

    @GetMapping("/{id}")
    public DoctorProfileDto findById(@PathVariable Long id) {
        return doctorService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public DoctorProfileDto create(@Valid @RequestBody DoctorCreateRequest request) {
        return doctorService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public DoctorProfileDto update(@PathVariable Long id, @Valid @RequestBody DoctorUpdateRequest request) {
        return doctorService.update(id, request);
    }
}
