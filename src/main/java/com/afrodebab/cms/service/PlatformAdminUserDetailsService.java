package com.afrodebab.cms.service;


import com.afrodebab.cms.jpa.entity.PlatformAdmin;
import com.afrodebab.cms.jpa.repository.PlatformAdminRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlatformAdminUserDetailsService implements UserDetailsService {

    private final PlatformAdminRepository repo;

    public PlatformAdminUserDetailsService(PlatformAdminRepository repo) { this.repo = repo; }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        PlatformAdmin admin = repo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Platform admin not found"));
        if (!admin.isActive()) throw new UsernameNotFoundException("Platform admin is inactive");

        return new User(admin.getEmail(), admin.getPasswordHash(), List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}
