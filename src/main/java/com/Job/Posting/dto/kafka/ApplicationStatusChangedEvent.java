package com.Job.Posting.dto.kafka;

import com.Job.Posting.entity.type.StatusType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationStatusChangedEvent {

    private Long applicationId;
    private Long userId;
    private String jobTitle;
    private StatusType newStatus;
    private String userFcmToken;
}
