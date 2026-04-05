package com.Job.Posting.user.service;

import com.Job.Posting.dto.skills.SkillsDto;
import com.Job.Posting.dto.user.AddUserRequestDto;
import com.Job.Posting.dto.user.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface UserService {


    Page<UserDto> getAllUsers(int page, int size);

    UserDto getUserById(Long id);

    UserDto addNewUser(AddUserRequestDto addUserRequestDto);

    UserDto updateUser(AddUserRequestDto addUserRequestDto, Long id);

    UserDto updateUserValue(Map<String, Object> updates, Long id);

    void deleteUser(Long id);

    List<SkillsDto> getUserSkills(Long id);

    UserDto addSkillToUser(Long userId, Long skillId);

    void deleteSkillFromUser(Long userId, Long skillId);
}
