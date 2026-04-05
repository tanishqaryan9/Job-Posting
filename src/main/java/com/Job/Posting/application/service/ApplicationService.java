package com.Job.Posting.application.service;

import com.Job.Posting.dto.application.AddApplicationDto;
import com.Job.Posting.dto.application.ApplicationDto;
import com.Job.Posting.dto.application.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public interface ApplicationService {

    Page<ApplicationDto> getAllApplications(int page, int size);

    ApplicationDto getApplicationByID(Long id);

    ApplicationDto createApplication(AddApplicationDto addApplicationDto);

    ApplicationDto updateApplication(AddApplicationDto addApplicationDto, Long id);

    ApplicationDto updateApplicationValue(Map<String, Object> update, Long id);

    void deleteApplication(Long id);
}
