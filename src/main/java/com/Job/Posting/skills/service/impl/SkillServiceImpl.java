package com.Job.Posting.skills.service.impl;

import com.Job.Posting.dto.skills.AddSkillDto;
import com.Job.Posting.dto.skills.SkillsDto;
import com.Job.Posting.entity.Skills;
import com.Job.Posting.exception.DuplicateResourceException;
import com.Job.Posting.exception.ResourceNotFoundException;
import com.Job.Posting.skills.repository.SkillRepository;
import com.Job.Posting.skills.service.SkillService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {

    private final SkillRepository skillRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public List<SkillsDto> getAllSkills() {
        List<Skills> skills = skillRepository.findAll();
        return skills.stream()
                .map(element -> modelMapper.map(element, SkillsDto.class))
                .toList();
    }

    @Override
    @Transactional
    public SkillsDto getSkillsById(Long id) {
        Skills skills = skillRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Skills not found with id: "+id));
        return modelMapper.map(skills,SkillsDto.class);
    }

    @Override
    @Transactional
    public void deleteSkill(Long id) {
        if(!skillRepository.existsById(id))
        {
            throw new ResourceNotFoundException("Skills not found with id: "+id);
        }
        skillRepository.deleteById(id);
    }

    @Override
    @Transactional
    public SkillsDto addSkill(AddSkillDto dto) {
        if(skillRepository.existsByNameIgnoreCase(dto.getName()))
        {
            throw new DuplicateResourceException("Skill already Exists "+dto.getName());
        }
        Skills skills=modelMapper.map(dto,Skills.class);
        Skills savedSkills=skillRepository.save(skills);
        return modelMapper.map(savedSkills,SkillsDto.class);
    }
}
