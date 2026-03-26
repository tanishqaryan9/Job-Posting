package com.Job.Posting.auth.repository;

import com.Job.Posting.entity.AppUser;
import com.Job.Posting.entity.type.AuthProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

public interface AppUserRepository extends JpaRepository<AppUser,Long> {
    AppUser findByUsername(String username);

    AppUser findByProviderIdAndProviderType(String providerId, AuthProviderType providerType);
}
