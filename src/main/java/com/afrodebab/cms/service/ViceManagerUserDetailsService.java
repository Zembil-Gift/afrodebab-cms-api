package com.afrodebab.cms.service;

import com.afrodebab.cms.jpa.entity.Manager;
import com.afrodebab.cms.jpa.repository.ManagerRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ViceManagerUserDetailsService implements UserDetailsService {

    private final ManagerRepository repo;

    public ViceManagerUserDetailsService(ManagerRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Manager manager = repo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Vice Manager not found"));

        if (!manager.isActive()) {
            throw new UsernameNotFoundException("Vice Manager account is inactive");
        }

        if (manager.getRole() != Manager.ManagerRole.VICE_MANAGER) {
            throw new UsernameNotFoundException("User is not a Vice Manager");
        }

        return new User(
                manager.getEmail(),
                manager.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_VICE_MANAGER"))
        );
    }
}
