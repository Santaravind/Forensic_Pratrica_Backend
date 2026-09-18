package com.security.forecsic.controller;

import com.security.forecsic.dto.AdminSendEmailRequest;
import com.security.forecsic.dto.ApiResponse;
import com.security.forecsic.dto.SendAuthorEmailRequest;
import com.security.forecsic.model.Register;
import com.security.forecsic.service.CustomUserDetails;
import com.security.forecsic.service.EmailService;
import com.security.forecsic.service.PublisherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'PUBLISHER', 'EDITOR')")
public class AdminController {

    private final PublisherService publisherService;
    private final EmailService emailService;

    /**
     * Admin direct custom email to ANY recipient (Author, Reviewer, Board Member, External Contact)
     * Allows Admin to compose custom subject, message body (plain text or rich HTML), sender title, action button, etc.
     */
    @PostMapping({"/send-custom-email", "/send-email", "/mail"})
    @PreAuthorize("hasAnyRole('ADMIN', 'PUBLISHER')")
    public ResponseEntity<ApiResponse> sendCustomEmail(
            @Valid @RequestBody AdminSendEmailRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Register sender = (userDetails != null) ? userDetails.getUser() : null;
        emailService.sendAdminCustomEmail(request, sender);
        return ResponseEntity.ok(new ApiResponse(true, "Email successfully dispatched to: " + request.getTo()));
    }

    /**
     * Admin/Publisher direct email to specific author regarding a manuscript submission
     */
    @PostMapping({"/mail-author", "/send-author-email"})
    public ResponseEntity<ApiResponse> sendAuthorEmail(
            @Valid @RequestBody SendAuthorEmailRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Register sender = (userDetails != null) ? userDetails.getUser() : null;
        publisherService.sendAuthorDirectEmail(request, sender);
        return ResponseEntity.ok(new ApiResponse(true, "Email successfully dispatched to author: " + request.getRecipientEmail()));
    }
}

