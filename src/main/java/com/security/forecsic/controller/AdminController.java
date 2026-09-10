package com.security.forecsic.controller;

import com.security.forecsic.dto.ApiResponse;
import com.security.forecsic.dto.SendAuthorEmailRequest;
import com.security.forecsic.model.Register;
import com.security.forecsic.service.CustomUserDetails;
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

    /**
     * Admin/Publisher direct email to specific author via Resend
     */
    @PostMapping({"/mail-author", "/send-author-email", "/send-email"})
    public ResponseEntity<ApiResponse> sendAuthorEmail(
            @Valid @RequestBody SendAuthorEmailRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Register sender = (userDetails != null) ? userDetails.getUser() : null;
        publisherService.sendAuthorDirectEmail(request, sender);
        return ResponseEntity.ok(new ApiResponse(true, "Email successfully dispatched to author: " + request.getRecipientEmail()));
    }
}
