package com.Job.Posting.dto.refresh;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefreshRequestDto {

    @NotBlank(message = "Refresh token cannot be blank")
    private String refreshToken;
}
