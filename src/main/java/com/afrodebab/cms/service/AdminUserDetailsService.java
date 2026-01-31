package com.afrodebab.cms.service;


import com.afrodebab.cms.jpa.entity.Admin;
import com.afrodebab.cms.jpa.repository.AdminRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminRepository repo;

    public AdminUserDetailsService(AdminRepository repo) { this.repo = repo; }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Admin admin = repo.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Admin not found"));
        if (!admin.isActive()) throw new UsernameNotFoundException("Admin is inactive");

        return new User(admin.getEmail(), admin.getPasswordHash(), List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}

