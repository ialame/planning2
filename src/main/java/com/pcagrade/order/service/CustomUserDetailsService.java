package com.pcagrade.order.service;

import com.pcagrade.order.entity.Employee;
import com.pcagrade.order.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Custom UserDetailsService implementation for loading employee authentication data
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Employee not found with email: " + email));

        // Check if employee is active using getActive() method
        if (!employee.getActive()) {
            throw new UsernameNotFoundException("Employee account is deactivated: " + email);
        }

        return User.builder()
                .username(employee.getEmail())
                .password(employee.getPassword())
                .authorities(getAuthorities(employee))
                .accountExpired(false)
                .accountLocked(!employee.getActive())
                .credentialsExpired(false)
                .disabled(!employee.getActive())
                .build();
    }

    /**
     * Convert employee teams to Spring Security authorities
     */
    private Collection<? extends GrantedAuthority> getAuthorities(Employee employee) {
        return employee.getTeams().stream()
                .map(team -> new SimpleGrantedAuthority(team.getName()))
                .collect(Collectors.toList());
    }
}