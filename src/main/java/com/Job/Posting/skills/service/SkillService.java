package com.Job.Posting.skills.service;

import com.Job.Posting.dto.skills.AddSkillDto;
import com.Job.Posting.dto.skills.SkillsDto;

import java.util.List;

public interface SkillService {

    List<SkillsDto> getAllSkills();

    SkillsDto getSkillsById(Long id);

    void deleteSkill(Long id);

    SkillsDto addSkill(AddSkillDto dto);
}
