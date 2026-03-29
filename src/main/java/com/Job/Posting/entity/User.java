package com.Job.Posting.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
@SQLRestriction("deleted_at IS NULL")  // Hibernate auto-filters soft-deleted rows
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 10, unique = true)
    private String number;

    @Column(nullable = false, length = 100)
    private String location;

    @Column(nullable = false)
    private Integer experience;

    private String profile_photo;

    private Double latitude;
    private Double longitude;

    private String fcmToken;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime created_at;

    // Soft delete column — null means active, set means deleted
    private LocalDateTime deleted_at;

    @JsonIgnore
    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Job> job = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<JobApplication> jobApplications = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "user_skills",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"))
    private Set<Skills> skills = new HashSet<>();
}
