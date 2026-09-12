package com.security.forecsic.service;

import com.security.forecsic.model.OtpVerification;
import com.security.forecsic.repositery.jpa.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpVerificationRepository otpVerificationRepository;
    private final EmailService emailService;

    @Value("${otp.expiration.minutes:10}")
    private int expirationMinutes;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateNumericOtp(int length) {
        int bound = (int) Math.pow(10, length);
        int min = (int) Math.pow(10, length - 1);
        int otpNumber = min + secureRandom.nextInt(bound - min);
        return String.valueOf(otpNumber);
    }

    private static final int MAX_OTP_ATTEMPTS = 5;

    @Transactional
    public void sendRegistrationOtp(String email) {
        // Invalidate previous registration OTPs for this email
        otpVerificationRepository.deleteByEmailAndType(email, "REGISTRATION");

        String otp = generateNumericOtp(6);
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(expirationMinutes);

        OtpVerification otpVerification = new OtpVerification();
        otpVerification.setEmail(email);
        otpVerification.setOtp(otp);
        otpVerification.setType("REGISTRATION");
        otpVerification.setAttempts(0);
        otpVerification.setExpiryTime(expiryTime);
        otpVerification.setCreatedAt(LocalDateTime.now());

        otpVerificationRepository.save(otpVerification);

        emailService.sendOtpEmail(email, otp, expirationMinutes);
    }

    @Transactional
    public boolean verifyRegistrationOtp(String email, String submittedOtp) {
        return verifyOtpInternal(email, submittedOtp, "REGISTRATION");
    }

    @Transactional
    public void sendPasswordResetOtp(String email) {
        // Invalidate previous password reset OTPs for this email
        otpVerificationRepository.deleteByEmailAndType(email, "PASSWORD_RESET");

        String otp = generateNumericOtp(6);
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(expirationMinutes);

        OtpVerification otpVerification = new OtpVerification();
        otpVerification.setEmail(email);
        otpVerification.setOtp(otp);
        otpVerification.setType("PASSWORD_RESET");
        otpVerification.setAttempts(0);
        otpVerification.setExpiryTime(expiryTime);
        otpVerification.setCreatedAt(LocalDateTime.now());

        otpVerificationRepository.save(otpVerification);

        emailService.sendPasswordResetOtpEmail(email, otp, expirationMinutes);
    }

    @Transactional
    public boolean verifyPasswordResetOtp(String email, String submittedOtp) {
        return verifyOtpInternal(email, submittedOtp, "PASSWORD_RESET");
    }

    private boolean verifyOtpInternal(String email, String submittedOtp, String type) {
        OtpVerification otpVerification = otpVerificationRepository
                .findTopByEmailAndTypeOrderByCreatedAtDesc(email, type)
                .orElseThrow(() -> new IllegalArgumentException("No verification code requested for this email. Please request a new OTP."));

        if (LocalDateTime.now().isAfter(otpVerification.getExpiryTime())) {
            otpVerificationRepository.deleteByEmailAndType(email, type);
            throw new IllegalArgumentException("Verification code has expired. Please request a new OTP.");
        }

        if (!otpVerification.getOtp().equals(submittedOtp.trim())) {
            int newAttempts = otpVerification.getAttempts() + 1;
            otpVerification.setAttempts(newAttempts);

            if (newAttempts >= MAX_OTP_ATTEMPTS) {
                otpVerificationRepository.deleteByEmailAndType(email, type);
                throw new IllegalArgumentException("Too many incorrect attempts. For security, this verification code is invalidated. Please request a new OTP.");
            }

            otpVerificationRepository.save(otpVerification);
            int remaining = MAX_OTP_ATTEMPTS - newAttempts;
            throw new IllegalArgumentException("Invalid verification code. " + remaining + " attempt(s) remaining.");
        }

        // Successfully verified - clean up OTP
        otpVerificationRepository.deleteByEmailAndType(email, type);
        return true;
    }
}
