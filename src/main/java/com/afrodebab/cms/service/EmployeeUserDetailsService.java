package com.afrodebab.cms.service;

import com.afrodebab.cms.jpa.entity.Employee;
import com.afrodebab.cms.jpa.repository.EmployeeRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeUserDetailsService {
    private final EmployeeRepository repo;

    public EmployeeUserDetailsService(EmployeeRepository repo) {
        this.repo = repo;
    }

    public UserDetails loadUserByUsername(String email) {
        Employee employee = repo.findByEmailIgnoreCase(email).orElseThrow(() -> new UsernameNotFoundException("Employee not found"));
        if (!employee.isActive()) throw new UsernameNotFoundException("Employee is inactive");

        return new User(employee.getEmail(), employee.getPasswordHash(), List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));
    }
}
