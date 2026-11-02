package com.revendo.security;

import com.revendo.model.User;
import com.revendo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Loads a {@link User} from the database and wraps it as Spring Security
 * {@link UserDetails}. Used by {@link JwtAuthenticationFilter} to populate
 * the {@code SecurityContext} after JWT validation.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load a user by their ID (stored as the JWT {@code sub} claim).
     *
     * @param userId the user's numeric ID as a string
     * @throws UsernameNotFoundException if no user with that ID exists
     */
    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        Long id = Long.parseLong(userId);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with id: " + userId));

        return org.springframework.security.core.userdetails.User.builder()
                .username(String.valueOf(user.getId()))
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority(user.getRole().name())))
                .accountLocked(user.getLockedUntil() != null
                        && user.getLockedUntil().isAfter(java.time.LocalDateTime.now()))
                .build();
    }
}
