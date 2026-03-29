package com.Job.Posting.user;

import com.Job.Posting.dto.skills.SkillsDto;
import com.Job.Posting.dto.user.AddUserRequestDto;
import com.Job.Posting.dto.user.UserDto;
import com.Job.Posting.entity.Skills;
import com.Job.Posting.entity.User;
import com.Job.Posting.exception.ResourceNotFoundException;
import com.Job.Posting.skills.repository.SkillRepository;
import com.Job.Posting.user.repository.UserRepository;
import com.Job.Posting.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private ModelMapper modelMapper;

    @InjectMocks private UserServiceImpl userService;

    private User user;
    private UserDto userDto;
    private Skills skill;
    private SkillsDto skillsDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("John Doe");
        user.setNumber("1234567890");
        user.setLocation("Delhi");
        user.setExperience(3);
        user.setLatitude(28.6);
        user.setLongitude(77.2);
        user.setSkills(new HashSet<>());

        userDto = new UserDto();
        userDto.setId(1L);
        userDto.setName("John Doe");

        skill = new Skills();
        skill.setId(5L);
        skill.setName("Java");

        skillsDto = new SkillsDto();
        skillsDto.setId(5L);
        skillsDto.setName("Java");
    }

    // ── getAllUsers ───────────────────────────────────────────────────────────

    @Test
    void getAllUsers_shouldReturnPagedResults() {
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(modelMapper.map(user, UserDto.class)).thenReturn(userDto);

        Page<UserDto> result = userService.getAllUsers(0, 10);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("John Doe");
    }

    // ── getUserById ───────────────────────────────────────────────────────────

    @Test
    void getUserById_shouldReturnDto_whenExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(modelMapper.map(user, UserDto.class)).thenReturn(userDto);

        UserDto result = userService.getUserById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getUserById_shouldThrow_whenNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ── addNewUser ────────────────────────────────────────────────────────────

    @Test
    void addNewUser_shouldSaveAndReturnDto() {
        AddUserRequestDto requestDto = new AddUserRequestDto();
        requestDto.setName("John Doe");
        requestDto.setNumber("1234567890");
        requestDto.setLocation("Delhi");
        requestDto.setExperience(3);

        when(modelMapper.map(requestDto, User.class)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(modelMapper.map(user, UserDto.class)).thenReturn(userDto);

        UserDto result = userService.addNewUser(requestDto);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("John Doe");
        verify(userRepository).save(user);
    }

    // ── updateUser ────────────────────────────────────────────────────────────

    @Test
    void updateUser_shouldUpdateAndReturn_whenExists() {
        AddUserRequestDto requestDto = new AddUserRequestDto();
        requestDto.setName("Updated Name");
        requestDto.setNumber("9999999999");
        requestDto.setLocation("Mumbai");
        requestDto.setExperience(5);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        // Stub the map(requestDto -> user) call that copies fields
        doNothing().when(modelMapper).map(any(AddUserRequestDto.class), any(User.class));
        when(modelMapper.map(user, UserDto.class)).thenReturn(userDto);

        UserDto result = userService.updateUser(requestDto, 1L);

        assertThat(result).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void updateUser_shouldThrow_whenNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(new AddUserRequestDto(), 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ── updateUserValue ───────────────────────────────────────────────────────

    @Test
    void updateUserValue_shouldUpdateName() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(modelMapper.map(user, UserDto.class)).thenReturn(userDto);

        userService.updateUserValue(Map.of("name", "New Name"), 1L);

        assertThat(user.getName()).isEqualTo("New Name");
    }

    @Test
    void updateUserValue_shouldUpdateLocation() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(modelMapper.map(user, UserDto.class)).thenReturn(userDto);

        userService.updateUserValue(Map.of("location", "Bangalore"), 1L);

        assertThat(user.getLocation()).isEqualTo("Bangalore");
    }

    @Test
    void updateUserValue_shouldUpdateLatLong() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(modelMapper.map(user, UserDto.class)).thenReturn(userDto);

        userService.updateUserValue(Map.of("latitude", 12.9, "longitude", 77.5), 1L);

        assertThat(user.getLatitude()).isEqualTo(12.9);
        assertThat(user.getLongitude()).isEqualTo(77.5);
    }

    @Test
    void updateUserValue_shouldThrow_whenInvalidField() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateUserValue(Map.of("invalidField", "value"), 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Invalid field");
    }

    @Test
    void updateUserValue_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserValue(Map.of("name", "x"), 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ── deleteUser ────────────────────────────────────────────────────────────

    @Test
    void deleteUser_shouldSoftDelete_whenExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L);

        assertThat(user.getDeleted_at()).isNotNull();
        verify(userRepository).save(user);
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void deleteUser_shouldThrow_whenNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ── getUserSkills ─────────────────────────────────────────────────────────

    @Test
    void getUserSkills_shouldReturnSkillList() {
        user.getSkills().add(skill);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(modelMapper.map(skill, SkillsDto.class)).thenReturn(skillsDto);

        List<SkillsDto> result = userService.getUserSkills(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Java");
    }

    @Test
    void getUserSkills_shouldReturnEmptyList_whenNoSkills() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        List<SkillsDto> result = userService.getUserSkills(1L);

        assertThat(result).isEmpty();
    }

    // ── addSkillToUser ────────────────────────────────────────────────────────

    @Test
    void addSkillToUser_shouldAddSkill_whenBothExist() {
        when(skillRepository.findById(5L)).thenReturn(Optional.of(skill));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(modelMapper.map(user, UserDto.class)).thenReturn(userDto);

        userService.addSkillToUser(1L, 5L);

        assertThat(user.getSkills()).contains(skill);
    }

    @Test
    void addSkillToUser_shouldThrow_whenSkillNotFound() {
        when(skillRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.addSkillToUser(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void addSkillToUser_shouldThrow_whenUserNotFound() {
        when(skillRepository.findById(5L)).thenReturn(Optional.of(skill));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.addSkillToUser(999L, 5L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ── deleteSkillFromUser ───────────────────────────────────────────────────

    @Test
    void deleteSkillFromUser_shouldRemoveSkill_whenBothExist() {
        user.getSkills().add(skill);
        when(skillRepository.findById(5L)).thenReturn(Optional.of(skill));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteSkillFromUser(1L, 5L);

        assertThat(user.getSkills()).doesNotContain(skill);
    }

    @Test
    void deleteSkillFromUser_shouldThrow_whenSkillNotFound() {
        when(skillRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteSkillFromUser(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }
}
