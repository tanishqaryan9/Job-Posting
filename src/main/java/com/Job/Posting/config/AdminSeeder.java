package com.Job.Posting.config;

import com.Job.Posting.auth.repository.AppUserRepository;
import com.Job.Posting.entity.AppUser;
import com.Job.Posting.entity.User;
import com.Job.Posting.entity.type.AuthProviderType;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        String adminUsername = "Trigno001";
        AppUser existingAdmin = appUserRepository.findByUsername(adminUsername);
        
        if (existingAdmin == null) {
            User adminProfile = new User();
            adminProfile.setName("Admin Trigno");
            adminProfile.setNumber("0000000000");
            adminProfile.setLocation("System");
            adminProfile.setExperience(99);
            adminProfile.setIsVerified(true);
            
            AppUser admin = new AppUser();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode("Trigno@95"));
            admin.setRole("ROLE_ADMIN");
            admin.setProviderType(AuthProviderType.LOCAL);
            admin.setUserProfile(adminProfile);
            
            appUserRepository.save(admin);
            System.out.println("Admin user " + adminUsername + " seeded successfully.");
        } else {
            // Ensure the role is set correctly in case it was created as a normal user manually
            if (!"ROLE_ADMIN".equals(existingAdmin.getRole())) {
                existingAdmin.setRole("ROLE_ADMIN");
                existingAdmin.setPassword(passwordEncoder.encode("Trigno@95"));
                appUserRepository.save(existingAdmin);
                System.out.println("Admin user updated to ROLE_ADMIN role.");
            }
        }
    }
}
