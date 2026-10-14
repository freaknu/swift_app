package com.swiftapp_backend.authservice.authentication.infrastructure.security;

import com.swiftapp_backend.authservice.authentication.domain.model.AppUser;
import com.swiftapp_backend.authservice.authentication.domain.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService{
    private final AppUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser existingUser = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not fount with email" + username));

        return User
                .builder()
                .authorities("ROLE_"+existingUser.getRole().name())
                .username(username)
                .password("")
                .accountLocked(false)
                .build();
    }
}