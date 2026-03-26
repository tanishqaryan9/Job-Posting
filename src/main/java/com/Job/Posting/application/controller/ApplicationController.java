package com.Job.Posting.application.controller;

import com.Job.Posting.application.service.ApplicationService;
import com.Job.Posting.dto.application.AddApplicationDto;
import com.Job.Posting.dto.application.ApplicationDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/application")
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping
    public ResponseEntity<List<ApplicationDto>> getAllApplications()
    {
        return ResponseEntity.ok(applicationService.getAllApplications());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationDto> getApplicationByID(@PathVariable Long id)
    {
        return ResponseEntity.ok(applicationService.getApplicationByID(id));
    }

    @PostMapping
    public ResponseEntity<ApplicationDto> createApplication(@RequestBody @Valid AddApplicationDto addApplicationDto)
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationService.createApplication(addApplicationDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApplicationDto> updateApplication(@RequestBody @Valid AddApplicationDto addApplicationDto, @PathVariable Long id)
    {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(applicationService.updateApplication(addApplicationDto,id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApplicationDto> updateApplicationValue(@RequestBody Map<String,Object> update, @PathVariable Long id)
    {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(applicationService.updateApplicationValue(update,id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable Long id)
    {
        applicationService.deleteApplication(id);
        return ResponseEntity.noContent().build();
    }

}
