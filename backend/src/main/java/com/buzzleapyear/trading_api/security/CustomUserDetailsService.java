package com.buzzleapyear.trading_api.security;

import com.buzzleapyear.trading_api.entity.User;
import com.buzzleapyear.trading_api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
//This acts as a user database lookup service
//when spring security needs to authenticate a user by username, it will use this service to load user details
@Service
public class CustomUserDetailsService implements UserDetailsService {

    //dependency injection for the UserRepository to access user dataand so we can query the database
    @Autowired
    private UserRepository userRepository;

    //load user by username for authentication
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = userRepository.findByUsername(username);
        //check if user exists in the database
        //returns 401 Unauthorized if the user is not found
        if (user.isEmpty()) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        User appUser = user.get();
        Collection<GrantedAuthority> authorities = appUser.getUserRoles().stream()
            .map(userRole -> new SimpleGrantedAuthority(
                "ROLE_" + userRole.getRole().getRoleName().name()))
            .collect(Collectors.toList());

        //return a UserDetails object containing the username, hashed password, and authorities
        return org.springframework.security.core.userdetails.User.builder()
                .username(appUser.getUsername())
                .password(appUser.getPasswordHash())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
    //retrieve a user by their unique ID from the database
    //optional: returns null if the user is not found
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}
