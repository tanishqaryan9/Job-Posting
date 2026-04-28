package com.Job.Posting.entity;

import com.Job.Posting.entity.type.StatusType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(
    name = "job_application",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_user_job_application",
        columnNames = {"user_id", "job_id"}
    )
)
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private StatusType status;

    @Column(columnDefinition = "TEXT")
    private String coverLetter;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime applied_at;
}
