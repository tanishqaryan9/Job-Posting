package com.Job.Posting.entity;

import com.Job.Posting.entity.type.JobType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@SQLRestriction("deleted_at IS NULL")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 300)
    private String description;

    private Double salary;

    @Column(length = 100)
    private String location;

    @Enumerated(EnumType.STRING)
    private JobType job_type;

    private Integer experience_required;

    private Double latitude;
    private Double longitude;

    @ManyToMany
    @JoinTable(name = "job_skills",
            joinColumns = @JoinColumn(name = "job_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"))
    private Set<Skills> requiredSkills = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime created_at;

    // Soft delete column
    private LocalDateTime deleted_at;
}
