package com.vitorcamprubi.sgtc.security;

import com.vitorcamprubi.sgtc.repo.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class DbUserDetailsService implements UserDetailsService {
    private final UserRepository repo;
    public DbUserDetailsService(UserRepository repo) { this.repo = repo; }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var u = repo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));
        return org.springframework.security.core.userdetails.User
                .withUsername(u.getEmail())
                .password(u.getSenhaHash())
                .roles(u.getRole().name()) // vira ROLE_ADMIN etc.
                .build();
    }
}
