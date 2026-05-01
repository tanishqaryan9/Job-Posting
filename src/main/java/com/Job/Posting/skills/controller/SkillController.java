package com.Job.Posting.skills.controller;

import com.Job.Posting.dto.skills.AddSkillDto;
import com.Job.Posting.dto.skills.SkillsDto;
import com.Job.Posting.skills.service.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @GetMapping
    public ResponseEntity<List<SkillsDto>> getAllSkills() {
        return ResponseEntity.ok(skillService.getAllSkills());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SkillsDto> getSkillsById(@PathVariable Long id) {
        return ResponseEntity.ok(skillService.getSkillsById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> deleteSkillAsAdmin(@PathVariable Long id) {
        skillService.deleteSkillAsAdmin(id);
        return ResponseEntity.noContent().build();
    }

    //Write endpoints — ADMIN only

    @PostMapping
    public ResponseEntity<SkillsDto> addSkill(@RequestBody @Valid AddSkillDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(skillService.addSkill(dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteSkill(@PathVariable Long id) {
        skillService.deleteSkill(id);
        return ResponseEntity.ok("Skill deleted successfully");
    }
}