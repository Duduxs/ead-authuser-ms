package com.ead.authuser.configurations.security;

import com.ead.authuser.exceptions.NotFoundHttpException;
import com.ead.authuser.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        final var user = repository.findByUsername(username)
                .orElseThrow(() -> new NotFoundHttpException("Username not found by name: " + username));

        return UserDetailsImpl.build(user);
    }

    public UserDetails loadUserById(UUID id) throws AuthenticationCredentialsNotFoundException {
        final var user = repository.findById(id)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Username not found by id: " + id));

        return UserDetailsImpl.build(user);
    }

}
