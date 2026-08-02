package com.security.forecsic.service;



import com.security.forecsic.dto.*;


import com.security.forecsic.model.Register;
import com.security.forecsic.repositery.RegisterRepository;
import com.security.forecsic.utilits.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
//import  java.lang.Throwable;
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RegisterRepository registerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

//    public AuthResponse register(RegisterRequest request) {
//
//        if (registerRepository.existsByEmail(request.getEmail())) {
//            throw new IllegalArgumentException("Email already registered");
//        }
//        if (registerRepository.existsByMobileNo(Long.parseLong(request.getMobileNo()))) {
//            throw new IllegalArgumentException("Mobile number already registered");
//        }
//
//        Register user = new Register();
//        user.setFullName(request.getFullName());
//        user.setOrganization(request.getOrganization());
//        user.setDomain(request.getDomain());
//        user.setPassword(passwordEncoder.encode(request.getPassword()));
//        user.setMobileNo(Long.parseLong(request.getMobileNo()));
//        user.setEmail(request.getEmail());
//        user.setRole("USER");
//
//        registerRepository.save(user);
//
//        CustomUserDetails userDetails = new CustomUserDetails(user);
//        String token = jwtUtil.generateToken(userDetails);
//
//        return new AuthResponse(token, user.getEmail(), user.getFullName());
//    }

    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        Register user = registerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!user.getRole().equalsIgnoreCase(request.getRole())) {
            throw new IllegalArgumentException("Role mismatch for this account");
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtUtil.generateToken(userDetails);

        return new TokenResponse(token);
    }

    public ApiResponse register(RegisterRequest request) {
        if (registerRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (registerRepository.existsByMobileNo(Long.parseLong(request.getMobileNo()))) {
            throw new IllegalArgumentException("Mobile number already registered");
        }

        Register user = new Register();
        user.setFullName(request.getFullName());
        user.setOrganization(request.getOrganization());
        user.setDomain(request.getDomain());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setMobileNo(Long.parseLong(request.getMobileNo()));
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());

        registerRepository.save(user);

        return new ApiResponse(true, "User registered successfully");
    }

    public ApiResponse resetPassword(ResetPasswordRequest request) {
        Register user = registerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("No account found with this email"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        registerRepository.save(user);

        return new ApiResponse(true, "Password reset successfully");
    }
}
