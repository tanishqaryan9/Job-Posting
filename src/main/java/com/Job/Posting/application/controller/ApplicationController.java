package com.Job.Posting.application.controller;

import com.Job.Posting.application.service.ApplicationService;
import com.Job.Posting.dto.application.AddApplicationDto;
import com.Job.Posting.dto.application.ApplicationDto;
import com.Job.Posting.dto.application.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/application")
@Validated
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping
    public ResponseEntity<PageResponse<ApplicationDto>> getAllApplications(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        Page<ApplicationDto> result = applicationService.getAllApplications(page, size);
        PageResponse<ApplicationDto> response = new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationDto> getApplicationByID(@PathVariable Long id) {
        return ResponseEntity.ok(applicationService.getApplicationByID(id));
    }

    @GetMapping("/by-job/{jobId}")
    public ResponseEntity<List<ApplicationDto>> getApplicationsByJob(@PathVariable Long jobId) {
        return ResponseEntity.ok(applicationService.getApplicationsByJob(jobId));
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<List<ApplicationDto>> getApplicationsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(applicationService.getApplicationsByUser(userId));
    }

    @PostMapping
    public ResponseEntity<ApplicationDto> createApplication(
            @RequestBody @Valid AddApplicationDto addApplicationDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.createApplication(addApplicationDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApplicationDto> updateApplication(
            @RequestBody @Valid AddApplicationDto addApplicationDto,
            @PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(applicationService.updateApplication(addApplicationDto, id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApplicationDto> updateApplicationValue(
            @RequestBody Map<String, Object> update,
            @PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(applicationService.updateApplicationValue(update, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable Long id) {
        applicationService.deleteApplication(id);
        return ResponseEntity.noContent().build();
    }
}
