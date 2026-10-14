package com.zuriagro.service;

import com.zuriagro.domain.entity.Role;
import com.zuriagro.domain.entity.RoleType;
import com.zuriagro.domain.entity.User;
import com.zuriagro.dto.auth.JwtResponse;
import com.zuriagro.dto.auth.LoginRequest;
import com.zuriagro.dto.auth.RegisterRequest;
import com.zuriagro.dto.auth.UserResponse;
import com.zuriagro.exception.DuplicateResourceException;
import com.zuriagro.exception.ResourceNotFoundException;
import com.zuriagro.repository.RoleRepository;
import com.zuriagro.repository.UserRepository;
import com.zuriagro.security.JwtTokenProvider;
import com.zuriagro.security.UserDetailsImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public JwtResponse authenticate(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return new JwtResponse(jwt, user.getId(), user.getUsername(), user.getEmail(), user.getFullName(), roles);
    }

    @Transactional
    public UserResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.username())) {
            throw new DuplicateResourceException("Username '" + registerRequest.username() + "' is already taken");
        }

        if (userRepository.existsByEmail(registerRequest.email())) {
            throw new DuplicateResourceException("Email '" + registerRequest.email() + "' is already registered");
        }

        User user = new User(
                registerRequest.username(),
                registerRequest.email(),
                passwordEncoder.encode(registerRequest.password()),
                registerRequest.fullName(),
                registerRequest.phoneNumber()
        );

        Set<Role> roles = new HashSet<>();
        // Public registration assigns ROLE_FARMER; privileged roles (ADMIN, DEPOT_MANAGER) require administrative assignment
        Role defaultRole = roleRepository.findByName(RoleType.ROLE_FARMER)
                .orElseThrow(() -> new ResourceNotFoundException("Default role ROLE_FARMER not configured in database"));
        roles.add(defaultRole);

        user.setRoles(roles);
        User savedUser = userRepository.save(user);

        return UserResponse.fromEntity(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return UserResponse.fromEntity(user);
    }
}
