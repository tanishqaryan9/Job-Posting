package com.Job.Posting.dto.user;

import com.Job.Posting.dto.skills.SkillsDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {

    private Long id;
    private String name;
    private String number;
    private String location;
    private String role;
    private List<SkillsDto> skills;
    private Integer experience;
    private String profile_photo;
    private Double latitude;
    private Double longitude;
    private Boolean isVerified;
    private LocalDateTime created_at;
    private String email;
    private String password;
}
