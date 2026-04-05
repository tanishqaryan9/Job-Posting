package com.Job.Posting.dto.skills;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddSkillDto {

    private Long id;
    @NotBlank(message = "Skills cannot be Blank")
    @Size(max = 50, message = "Skills name cannot exceed 50 characters")
    private String name;
}
