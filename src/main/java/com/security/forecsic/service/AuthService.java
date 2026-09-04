package com.security.forecsic.service;

import com.security.forecsic.dto.*;
import com.security.forecsic.model.Register;
import com.security.forecsic.repositery.jpa.RegisterRepository;
import com.security.forecsic.utilits.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final RegisterRepository registerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;

    public ApiResponse register(RegisterRequest request) {
        Optional<Register> existingUserOpt = registerRepository.findByEmail(request.getEmail());

        Register user;
        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            if (user.isVerified()) {
                throw new IllegalArgumentException("Email already registered and verified. Please log in.");
            }
            // Update details for unverified user re-attempting registration
            user.setFullName(request.getFullName());
            user.setOrganization(request.getOrganization());
            user.setDomain(request.getDomain());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setMobileNo(Long.parseLong(request.getMobileNo()));
            user.setRole(request.getRole());
        } else {
            if (registerRepository.existsByMobileNo(Long.parseLong(request.getMobileNo()))) {
                throw new IllegalArgumentException("Mobile number already registered");
            }

            user = new Register();
            user.setFullName(request.getFullName());
            user.setOrganization(request.getOrganization());
            user.setDomain(request.getDomain());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setMobileNo(Long.parseLong(request.getMobileNo()));
            user.setEmail(request.getEmail());
            user.setRole(request.getRole());
            user.setVerified(false);
        }

        registerRepository.save(user);

        // Send OTP via Resend
        otpService.sendRegistrationOtp(user.getEmail());

        return new ApiResponse(true, "OTP sent to your email. Please verify your OTP to complete registration.");
    }

    public ApiResponse verifyOtp(VerifyOtpRequest request) {
        Register user = registerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("No account found with email: " + request.getEmail()));

        if (user.isVerified()) {
            return new ApiResponse(true, "Account is already verified. You can log in.");
        }

        otpService.verifyRegistrationOtp(request.getEmail(), request.getOtp());

        user.setVerified(true);
        registerRepository.save(user);

        return new ApiResponse(true, "Email verified successfully. Registration is complete! You can now log in.");
    }

    public ApiResponse resendOtp(ResendOtpRequest request) {
        Register user = registerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("No account found with email: " + request.getEmail()));

        if (user.isVerified()) {
            throw new IllegalArgumentException("Account is already verified. You can log in directly.");
        }

        otpService.sendRegistrationOtp(user.getEmail());

        return new ApiResponse(true, "A new OTP has been sent to your email.");
    }

    public TokenResponse login(LoginRequest request) {
        Register user = registerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!user.isVerified()) {
            throw new IllegalArgumentException("Account is not verified. Please verify the OTP sent to your email.");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        if (!user.getRole().equalsIgnoreCase(request.getRole())) {
            throw new IllegalArgumentException("Role mismatch for this account");
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        java.util.Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("role", user.getRole() != null ? user.getRole().toUpperCase() : "USER");
        claims.put("email", user.getEmail());
        claims.put("fullName", user.getFullName());
        claims.put("id", user.getId());
        String token = jwtUtil.generateToken(userDetails, claims);

        return new TokenResponse(token);
    }

    public ApiResponse resetPassword(ResetPasswordRequest request) {
        Register user = registerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("No account found with this email"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        registerRepository.save(user);

        return new ApiResponse(true, "Password reset successfully");
    }
}
