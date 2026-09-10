package com.security.forecsic.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendAuthorEmailRequest {

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Valid recipient email is required")
    private String recipientEmail;

    private String recipientName;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Message content is required")
    private String messageContent;

    private String paperTitle;

    private UUID paperId;
}
