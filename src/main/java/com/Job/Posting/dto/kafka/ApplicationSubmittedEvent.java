package com.Job.Posting.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationSubmittedEvent {
    private Long applicationId;
    private Long userId;
    private Long jobId;
    private String jobTitle;
    private String userName;
    private String userFcmToken;
    private Long jobCreatorId;
    private String jobCreatorFcmToken;
}
