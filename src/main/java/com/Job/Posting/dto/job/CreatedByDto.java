package com.Job.Posting.dto.job;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatedByDto {
    private Long id;
    private String name;
    private String location;
}
