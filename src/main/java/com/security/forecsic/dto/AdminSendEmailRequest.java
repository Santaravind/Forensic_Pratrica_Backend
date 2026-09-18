package com.security.forecsic.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSendEmailRequest {

    /**
     * Target recipient email address (supports 'to', 'recipientEmail', 'toEmail' JSON keys)
     */
    @NotBlank(message = "Recipient email is required")
    @Email(message = "Valid recipient email address is required")
    @JsonAlias({"to", "recipientEmail", "toEmail", "email"})
    private String to;

    /**
     * Optional recipient display name (e.g. 'Dr. Jane Doe')
     */
    @JsonAlias({"recipientName", "toName", "name"})
    private String recipientName;

    /**
     * Subject line of the email
     */
    @NotBlank(message = "Email subject is required")
    private String subject;

    /**
     * The custom email message content written by the admin
     * (supports 'message', 'body', 'content', 'messageContent' JSON keys)
     */
    @NotBlank(message = "Email message content is required")
    @JsonAlias({"message", "body", "content", "messageContent"})
    private String message;

    /**
     * Set to true if the message content contains custom HTML tags
     */
    @Builder.Default
    private boolean isHtml = false;

    /**
     * Optional custom sender name (defaults to authenticated Admin's name)
     */
    @JsonAlias({"senderName", "fromName"})
    private String senderName;

    /**
     * Optional sender designation/title (e.g. 'Editor-in-Chief', 'Administration Desk')
     */
    @JsonAlias({"senderTitle", "senderRole", "designation"})
    private String senderTitle;

    /**
     * Optional Call to Action Button text (e.g. 'Open Dashboard', 'Review Paper')
     */
    private String buttonText;

    /**
     * Optional Call to Action Button URL
     */
    private String buttonUrl;

    /**
     * Optional CC email addresses
     */
    private List<String> cc;

    /**
     * Optional BCC email addresses
     */
    private List<String> bcc;

    /**
     * Optional custom reply-to address
     */
    private String replyTo;

    /**
     * Optional reference ID or topic note for administration records
     */
    private String referenceNote;
}
