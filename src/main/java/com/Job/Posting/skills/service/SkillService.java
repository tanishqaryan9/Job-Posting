package com.Job.Posting.skills.service;

import com.Job.Posting.dto.skills.AddSkillDto;
import com.Job.Posting.dto.skills.SkillsDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Service
public interface SkillService {

    List<SkillsDto> getAllSkills();

    SkillsDto getSkillsById(Long id);

    void deleteSkill(Long id);

    SkillsDto addSkill(@Valid AddSkillDto dto);
}
