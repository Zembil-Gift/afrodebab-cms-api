package com.afrodebab.cms.service;


import com.afrodebab.cms.jpa.entity.Manager;
import com.afrodebab.cms.jpa.repository.ManagerRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ManagerUserDetailsService implements UserDetailsService {

    private final ManagerRepository repo;

    public ManagerUserDetailsService(ManagerRepository repo) { this.repo = repo; }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Manager manager = repo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Manager not found"));
        if (!manager.isActive()) throw new UsernameNotFoundException("Manager is inactive");

        return new User(manager.getEmail(), manager.getPasswordHash(), List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));
    }
}
