package com.Job.Posting.user.controller;

import com.Job.Posting.dto.skills.SkillsDto;
import com.Job.Posting.dto.user.AddUserRequestDto;
import com.Job.Posting.dto.user.UserDto;
import com.Job.Posting.user.repository.UserRepository;
import com.Job.Posting.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Validated
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<Page<UserDto>> getAllUsers(@RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(userService.getAllUsers(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id)
    {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    public ResponseEntity<UserDto> addNewUser(@RequestBody @Valid AddUserRequestDto addUserRequestDto)
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.addNewUser(addUserRequestDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@RequestBody @Valid AddUserRequestDto addUserRequestDto, @PathVariable Long id)
    {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(userService.updateUser(addUserRequestDto,id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserDto> updateUserValue(@RequestBody Map<String,Object> updates, @PathVariable Long id)
    {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(userService.updateUserValue(updates,id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id)
    {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/skills")
    public ResponseEntity<List<SkillsDto>> getUserSkills(@PathVariable Long id)
    {
        return ResponseEntity.ok(userService.getUserSkills(id));
    }

    @PostMapping("/{userId}/skills/{skillId}")
    public ResponseEntity<UserDto> addSkillToUser(@PathVariable Long userId, @PathVariable Long skillId)
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.addSkillToUser(userId,skillId));
    }

    @DeleteMapping("/{userId}/skills/{skillId}")
    public ResponseEntity<Void> deleteSkillFromUser(@PathVariable Long userId, @PathVariable Long skillId)
    {
        userService.deleteSkillFromUser(userId,skillId);
        return ResponseEntity.noContent().build();
    }

}