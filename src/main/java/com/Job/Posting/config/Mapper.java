package com.Job.Posting.config;

import com.Job.Posting.dto.application.ApplicationDto;
import com.Job.Posting.dto.job.CreatedByDto;
import com.Job.Posting.dto.job.JobDto;
import com.Job.Posting.dto.skills.SkillsDto;
import com.Job.Posting.dto.user.UserDto;
import com.Job.Posting.entity.Job;
import com.Job.Posting.entity.JobApplication;
import com.Job.Posting.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.stream.Collectors;

@Configuration
public class Mapper {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();

        // Job entity → JobDto: explicit mapping required because entity uses snake_case
        // field names (job_type, created_at, etc.) while DTO uses camelCase (jobType, createdAt).
        mapper.createTypeMap(Job.class, JobDto.class)
                .setConverter(ctx -> mapJobToDto(ctx.getSource()));

        mapper.createTypeMap(JobApplication.class, ApplicationDto.class)
                .setConverter(ctx -> {
                    JobApplication src = ctx.getSource();
                    ApplicationDto dto = new ApplicationDto();

                    dto.setId(src.getId());
                    dto.setStatus(src.getStatus());
                    dto.setAppliedAt(src.getApplied_at());
                    dto.setCoverLetter(src.getCoverLetter());

                    // Map user — already eagerly loaded by EntityGraph.
                    User user = src.getUser();
                    if (user != null) {
                        UserDto userDto = new UserDto();
                        userDto.setId(user.getId());
                        userDto.setName(user.getName());
                        userDto.setNumber(user.getNumber());
                        userDto.setLocation(user.getLocation());
                        userDto.setExperience(user.getExperience());
                        userDto.setProfile_photo(user.getProfile_photo());
                        userDto.setLatitude(user.getLatitude());
                        userDto.setLongitude(user.getLongitude());
                        userDto.setCreated_at(user.getCreated_at());
                        if (user.getSkills() != null) {
                            userDto.setSkills(user.getSkills().stream()
                                    .map(s -> new SkillsDto(s.getId(), s.getName()))
                                    .collect(Collectors.toList()));
                        }
                        dto.setUser(userDto);
                    }

                    // Map job — already eagerly loaded by EntityGraph.
                    if (src.getJob() != null) {
                        dto.setJob(mapJobToDto(src.getJob()));
                    }

                    return dto;
                });

        return mapper;
    }

    private static JobDto mapJobToDto(Job src) {
        JobDto dto = new JobDto();
        dto.setId(src.getId());
        dto.setTitle(src.getTitle());
        dto.setDescription(src.getDescription());
        dto.setSalary(src.getSalary());
        dto.setLocation(src.getLocation());
        dto.setJobType(src.getJob_type());
        dto.setExperienceRequired(src.getExperience_required());
        dto.setLatitude(src.getLatitude());
        dto.setLongitude(src.getLongitude());
        dto.setCreatedAt(src.getCreated_at());
        if (src.getRequiredSkills() != null) {
            dto.setSkills(src.getRequiredSkills().stream()
                    .map(s -> new SkillsDto(s.getId(), s.getName()))
                    .collect(Collectors.toSet()));
        }
        if (src.getCreatedBy() != null) {
            User creator = src.getCreatedBy();
            dto.setCreatedBy(new CreatedByDto(
                    creator.getId(),
                    creator.getName(),
                    creator.getLocation()));
        }
        return dto;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}