package com.Job.Posting.user.service.impl;

import com.Job.Posting.dto.skills.SkillsDto;
import com.Job.Posting.dto.user.AddUserRequestDto;
import com.Job.Posting.dto.user.UserDto;
import com.Job.Posting.entity.AppUser;
import com.Job.Posting.entity.Skills;
import com.Job.Posting.entity.User;
import com.Job.Posting.exception.AccessDeniedException;
import com.Job.Posting.exception.ResourceNotFoundException;
import com.Job.Posting.skills.repository.SkillRepository;
import com.Job.Posting.user.repository.UserRepository;
import com.Job.Posting.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public Page<UserDto> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAll(pageable).map(user -> modelMapper.map(user, UserDto.class));
    }

    @Override
    @Transactional
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    @Transactional
    public UserDto addNewUser(AddUserRequestDto addUserRequestDto) {
        User user = modelMapper.map(addUserRequestDto, User.class);
        user = userRepository.save(user);
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    @Transactional
    public UserDto updateUser(AddUserRequestDto addUserRequestDto, Long id) {
        requireOwnership(id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        modelMapper.map(addUserRequestDto, user);
        User newUser = userRepository.save(user);
        return modelMapper.map(newUser, UserDto.class);
    }

    @Override
    @Transactional
    public UserDto updateUserValue(Map<String, Object> updates, Long id) {
        requireOwnership(id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        updates.forEach((key, value) -> {
            switch (key) {
                case "name"          -> user.setName((String) value);
                case "number"        -> user.setNumber((String) value);
                case "location"      -> user.setLocation((String) value);
                case "experience"    -> user.setExperience((Integer) value);
                case "profile_photo" -> user.setProfile_photo((String) value);
                case "latitude"      -> user.setLatitude(((Number) value).doubleValue());
                case "longitude"     -> user.setLongitude(((Number) value).doubleValue());
                case "fcmToken"      -> user.setFcmToken((String) value);
                default -> throw new ResourceNotFoundException("Invalid field: " + key);
            }
        });
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        requireOwnership(id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setDeleted_at(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public List<SkillsDto> getUserSkills(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return user.getSkills().stream()
                .map(skill -> modelMapper.map(skill, SkillsDto.class))
                .toList();
    }

    @Override
    @Transactional
    public UserDto addSkillToUser(Long userId, Long skillId) {
        requireOwnership(userId);
        Skills skills = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + skillId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.getSkills().add(skills);
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    @Transactional
    public void deleteSkillFromUser(Long userId, Long skillId) {
        requireOwnership(userId);
        Skills skills = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + skillId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.getSkills().remove(skills);
    }

    //Helpers
    private void requireOwnership(Long profileId) {
        AppUser currentUser = getCurrentUser();
        Long currentProfileId = currentUser.getUserProfile() != null
                ? currentUser.getUserProfile().getId() : null;

        if (currentProfileId == null || !currentProfileId.equals(profileId)) {
            throw new AccessDeniedException("You are not allowed to modify this user's data");
        }
    }

    private AppUser getCurrentUser() {
        return (AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}