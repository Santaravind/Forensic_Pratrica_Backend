package com.security.forecsic.service;

import com.security.forecsic.model.OtpVerification;
import com.security.forecsic.repositery.OtpVerificationRepository;
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
        otpVerification.setExpiryTime(expiryTime);
        otpVerification.setCreatedAt(LocalDateTime.now());

        otpVerificationRepository.save(otpVerification);

        emailService.sendOtpEmail(email, otp, expirationMinutes);
    }

    @Transactional
    public boolean verifyRegistrationOtp(String email, String submittedOtp) {
        OtpVerification otpVerification = otpVerificationRepository
                .findTopByEmailAndTypeOrderByCreatedAtDesc(email, "REGISTRATION")
                .orElseThrow(() -> new IllegalArgumentException("No OTP requested for this email. Please request a new OTP."));

        if (LocalDateTime.now().isAfter(otpVerification.getExpiryTime())) {
            otpVerificationRepository.deleteByEmailAndType(email, "REGISTRATION");
            throw new IllegalArgumentException("OTP has expired. Please request a new OTP.");
        }

        if (!otpVerification.getOtp().equals(submittedOtp.trim())) {
            throw new IllegalArgumentException("Invalid OTP. Please check the code and try again.");
        }

        // Successfully verified - clean up OTP
        otpVerificationRepository.deleteByEmailAndType(email, "REGISTRATION");
        return true;
    }
}
