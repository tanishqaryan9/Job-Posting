package com.Job.Posting.user.service.impl;

import com.Job.Posting.auth.repository.AppUserRepository;
import com.Job.Posting.dto.skills.SkillsDto;
import com.Job.Posting.dto.user.AddUserRequestDto;
import com.Job.Posting.dto.user.OAuthProfileRequestDto;
import com.Job.Posting.dto.user.UserDto;
import com.Job.Posting.entity.AppUser;
import com.Job.Posting.entity.Skills;
import com.Job.Posting.entity.User;
import com.Job.Posting.exception.AccessDeniedException;
import com.Job.Posting.exception.ResourceNotFoundException;
import com.Job.Posting.application.repository.JobApplicationRepository;
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
    private final AppUserRepository appUserRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final ModelMapper modelMapper;
    private final com.Job.Posting.job.repository.JobRepository jobRepository;
    private final com.Job.Posting.refresh.repository.RefreshTokenRepository refreshTokenRepository;

    @Override @Transactional
    public Page<UserDto> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAll(pageable).map(u -> {
            UserDto dto = modelMapper.map(u, UserDto.class);
            appUserRepository.findByUserProfile(u).ifPresent(appUser -> dto.setRole(appUser.getRole()));
            return dto;
        });
    }

    @Override @Transactional
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        UserDto dto = modelMapper.map(user, UserDto.class);
        appUserRepository.findByUserProfile(user).ifPresent(appUser -> dto.setRole(appUser.getRole()));
        return dto;
    }

    @Override @Transactional
    public UserDto addNewUser(AddUserRequestDto dto) {
        User user = modelMapper.map(dto, User.class);
        return modelMapper.map(userRepository.save(user), UserDto.class);
    }

    @Override @Transactional
    public UserDto updateUser(AddUserRequestDto dto, Long id) {
        requireOwnership(id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        modelMapper.map(dto, user);
        return modelMapper.map(userRepository.save(user), UserDto.class);
    }

    @Override @Transactional
    public UserDto updateUserValue(Map<String, Object> updates, Long id) {
        requireOwnership(id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        applyUpdates(user, updates);
        return modelMapper.map(userRepository.save(user), UserDto.class);
    }

    @Override @Transactional
    public void deleteUser(Long id) {
        requireOwnership(id);
        deleteUserCascade(id);
    }

    @Override @Transactional
    public void deleteUserAsAdmin(Long id) {
        deleteUserCascade(id);
    }

    private void deleteUserCascade(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        userRepository.deleteUserSkillsByUserId(id);
        jobApplicationRepository.deleteByUserId(id);
        jobApplicationRepository.deleteByJobCreatorId(id);
        jobRepository.deleteByCreatedById(id);

        appUserRepository.findByUserProfile(user).ifPresent(appUser -> {
            refreshTokenRepository.deleteAllByAppUser(appUser);
            appUserRepository.delete(appUser);
        });
    }

    @Override @Transactional
    public List<SkillsDto> getUserSkills(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return user.getSkills().stream()
                .map(s -> modelMapper.map(s, SkillsDto.class))
                .toList();
    }

    @Override @Transactional
    public UserDto addSkillToUser(Long userId, Long skillId) {
        requireOwnership(userId);
        Skills skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + skillId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.getSkills().add(skill);
        return modelMapper.map(user, UserDto.class);
    }

    @Override @Transactional
    public void deleteSkillFromUser(Long userId, Long skillId) {
        requireOwnership(userId);
        Skills skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + skillId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.getSkills().remove(skill);
    }

    // ── OAuth profile creation ─────────────────────────────────────────────────

    /**
     * POST /users/oauth-profile/{appUserId}
     *
     * New OAuth users have an AppUser row but no User (profile) row yet.
     * requireOwnership() would throw 403 because getUserProfile() == null.
     * This method verifies the caller IS the AppUser in the JWT instead.
     */
    @Override @Transactional
    public UserDto createOAuthProfile(Long appUserId, OAuthProfileRequestDto dto) {
        AppUser currentAppUser = getCurrentUser();

        // Security: caller's AppUser.id must match the path variable
        if (!currentAppUser.getId().equals(appUserId)) {
            throw new AccessDeniedException("You are not allowed to create a profile for another user");
        }

        // Idempotency: if they already have a profile, return it
        if (currentAppUser.getUserProfile() != null) {
            return modelMapper.map(currentAppUser.getUserProfile(), UserDto.class);
        }

        // Fetch managed entity
        AppUser appUser = appUserRepository.findById(appUserId)
                .orElseThrow(() -> new ResourceNotFoundException("AppUser not found: " + appUserId));

        // Create the User (profile) row
        User profile = new User();
        profile.setName(dto.getName());
        profile.setNumber(dto.getNumber());
        profile.setLocation(dto.getLocation());
        profile.setExperience(dto.getExperience());
        if (dto.getLatitude()  != null) profile.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != null) profile.setLongitude(dto.getLongitude());
        profile.setIsVerified(true); // OAuth emails are trusted
        User saved = userRepository.save(profile);

        // Link to AppUser
        appUser.setUserProfile(saved);
        appUserRepository.save(appUser);

        return modelMapper.map(saved, UserDto.class);
    }

    @Override @Transactional
    public UserDto getCurrentUserProfile() {
        AppUser currentAppUser = getCurrentUser();
        // Fetch managed entity to ensure we get the latest profile status from DB
        AppUser appUser = appUserRepository.findById(currentAppUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found in database"));
        
        if (appUser.getUserProfile() == null) {
            throw new ResourceNotFoundException("User profile not found for current user");
        }
        
        UserDto dto = modelMapper.map(appUser.getUserProfile(), UserDto.class);
        dto.setRole(appUser.getRole());
        return dto;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void requireOwnership(Long profileId) {
        AppUser cur = getCurrentUser();
        if ("ROLE_ADMIN".equals(cur.getRole())) {
            return;
        }
        Long curProfileId = cur.getUserProfile() != null
                ? cur.getUserProfile().getId() : null;
        if (curProfileId == null || !curProfileId.equals(profileId)) {
            throw new AccessDeniedException("You are not allowed to modify this user's data");
        }
    }

    private void applyUpdates(User user, Map<String, Object> updates) {
        updates.forEach((key, value) -> {
            switch (key) {
                case "name"          -> user.setName((String) value);
                case "number"        -> user.setNumber((String) value);
                case "location"      -> user.setLocation((String) value);
                case "experience"    -> user.setExperience(((Number) value).intValue());
                case "profile_photo" -> user.setProfile_photo((String) value);
                case "latitude"      -> user.setLatitude(((Number) value).doubleValue());
                case "longitude"     -> user.setLongitude(((Number) value).doubleValue());
                case "fcmToken"      -> user.setFcmToken((String) value);
                default -> throw new ResourceNotFoundException("Invalid field: " + key);
            }
        });
    }

    private AppUser getCurrentUser() {
        return (AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
