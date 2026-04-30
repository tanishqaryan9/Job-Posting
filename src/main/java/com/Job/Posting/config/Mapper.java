package com.Job.Posting.config;

import com.Job.Posting.auth.repository.AppUserRepository;
import com.Job.Posting.dto.application.ApplicationDto;
import com.Job.Posting.dto.job.CreatedByDto;
import com.Job.Posting.dto.job.JobDto;
import com.Job.Posting.dto.skills.SkillsDto;
import com.Job.Posting.dto.user.UserDto;
import com.Job.Posting.entity.AppUser;
import com.Job.Posting.entity.Job;
import com.Job.Posting.entity.JobApplication;
import com.Job.Posting.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.stream.Collectors;

@Configuration
public class Mapper {

    @Autowired
    private AppUserRepository appUserRepository;

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();

        mapper.createTypeMap(JobApplication.class, ApplicationDto.class)
                .setConverter(ctx -> {
                    JobApplication src = ctx.getSource();
                    ApplicationDto dto = new ApplicationDto();

                    dto.setId(src.getId());
                    dto.setStatus(src.getStatus());
                    dto.setApplied_at(src.getApplied_at());
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

                        // Resolve email from AppUser.username (the login credential).
                        appUserRepository.findByUserProfile(user)
                                .map(AppUser::getUsername)
                                .ifPresent(dto::setApplicantEmail);
                    }

                    // Map job — already eagerly loaded by EntityGraph.
                    Job job = src.getJob();
                    if (job != null) {
                        try {
                            JobDto jobDto = new JobDto();
                            jobDto.setId(job.getId());
                            jobDto.setTitle(job.getTitle());
                            jobDto.setDescription(job.getDescription());
                            jobDto.setSalary(job.getSalary());
                            jobDto.setSalaryPeriod(job.getSalaryPeriod());
                            jobDto.setLocation(job.getLocation());
                            jobDto.setJob_type(job.getJob_type());
                            jobDto.setExperience_required(job.getExperience_required());
                            jobDto.setLatitude(job.getLatitude());
                            jobDto.setLongitude(job.getLongitude());
                            jobDto.setCreated_at(job.getCreated_at());
                            if (job.getRequiredSkills() != null) {
                                jobDto.setRequiredSkills(job.getRequiredSkills().stream()
                                        .map(s -> new SkillsDto(s.getId(), s.getName()))
                                        .collect(Collectors.toSet()));
                            }
                            if (job.getCreatedBy() != null) {
                                User creator = job.getCreatedBy();
                                jobDto.setCreatedBy(new CreatedByDto(
                                        creator.getId(),
                                        creator.getName(),
                                        creator.getLocation()));
                            }
                            dto.setJob(jobDto);
                        } catch (jakarta.persistence.EntityNotFoundException e) {
                            dto.setJob(null);
                        }
                    }

                    return dto;
                });

        return mapper;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
