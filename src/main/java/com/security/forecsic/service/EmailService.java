package com.security.forecsic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.security.forecsic.dto.AdminSendEmailRequest;
import com.security.forecsic.model.Register;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.web.util.HtmlUtils;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${resend.from.email:noreply@forensicpatrika.com}")
    private String fromEmail;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    /**
     * Send OTP Verification Email for Registration
     */
    public void sendOtpEmail(String toEmail, String otp, int expirationMinutes) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("RESEND_API_KEY is not configured! Registration OTP for {} is: {}", toEmail, otp);
            return;
        }

        try {
            String htmlContent = buildOtpEmailHtml(otp, expirationMinutes, "Registration Verification");
            sendEmailViaResend(toEmail, "Your Verification Code: " + otp, htmlContent);
        } catch (Exception e) {
            log.error("Error sending OTP email to {}", toEmail, e);
            throw new RuntimeException("Error occurred while sending verification email: " + e.getMessage(), e);
        }
    }

    /**
     * Send OTP Verification Email for Password Reset
     */
    public void sendPasswordResetOtpEmail(String toEmail, String otp, int expirationMinutes) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("RESEND_API_KEY is not configured! Password Reset OTP for {} is: {}", toEmail, otp);
            return;
        }

        try {
            String htmlContent = buildOtpEmailHtml(otp, expirationMinutes, "Password Reset");
            sendEmailViaResend(toEmail, "Password Reset Code: " + otp, htmlContent);
        } catch (Exception e) {
            log.error("Error sending password reset OTP email to {}", toEmail, e);
            throw new RuntimeException("Error occurred while sending password reset email: " + e.getMessage(), e);
        }
    }

    /**
     * Send Research Paper Submission Confirmation Email
     */
    public void sendPaperSubmissionConfirmationEmail(
            String toEmail,
            String authorName,
            String submissionId,
            String paperTitle,
            String researchArea
    ) {
        if (toEmail == null || toEmail.isBlank()) return;

        log.info("Sending Paper Submission Confirmation to {} for Submission ID: {}", toEmail, submissionId);
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("RESEND_API_KEY is not configured. Skipping live email for submission: {}", submissionId);
            return;
        }

        try {
            String htmlContent = buildSubmissionConfirmationHtml(authorName, submissionId, paperTitle, researchArea);
            sendEmailViaResend(toEmail, "Paper Submission Received: [" + submissionId + "] " + paperTitle, htmlContent);
        } catch (Exception e) {
            log.error("Failed to send paper submission confirmation email to {}", toEmail, e);
        }
    }

    /**
     * Send Publication Success Email with DOI and Certificate
     */
    public void sendPublicationSuccessEmail(
            String toEmail,
            String authorName,
            String paperTitle,
            String journalTitle,
            String doi,
            String certificateUrl,
            String paperUrl
    ) {
        if (toEmail == null || toEmail.isBlank()) return;

        log.info("Sending Publication Success Email to {} for DOI: {}", toEmail, doi);
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("RESEND_API_KEY is not configured. Skipping live publication email for DOI: {}", doi);
            return;
        }

        try {
            String htmlContent = buildPublicationSuccessHtml(authorName, paperTitle, journalTitle, doi, certificateUrl, paperUrl);
            sendEmailViaResend(toEmail, "🎉 Congratulations! Your Research Paper is Published: " + paperTitle, htmlContent);
        } catch (Exception e) {
            log.error("Failed to send publication success email to {}", toEmail, e);
        }
    }

    /**
     * Send Direct / Custom Email from Admin/Publisher to Author
     */
    public void sendCustomAuthorEmail(
            String toEmail,
            String authorName,
            String subject,
            String messageContent,
            String paperTitle,
            String senderName
    ) {
        if (toEmail == null || toEmail.isBlank()) return;

        log.info("Sending Admin/Publisher Direct Email to {} regarding: {}", toEmail, subject);
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("RESEND_API_KEY is not configured. Skipping direct email to: {}", toEmail);
            return;
        }

        try {
            String htmlContent = buildCustomAuthorEmailHtml(authorName, subject, messageContent, paperTitle, senderName);
            sendEmailViaResend(toEmail, subject, htmlContent);
        } catch (Exception e) {
            log.error("Failed to send custom author email to {}", toEmail, e);
            throw new RuntimeException("Failed to send email to author: " + e.getMessage(), e);
        }
    }

    /**
     * Send Custom / Direct Email composed by Administrator from dashboard
     */
    public void sendAdminCustomEmail(AdminSendEmailRequest request, Register sender) {
        if (request == null || request.getTo() == null || request.getTo().isBlank()) {
            throw new IllegalArgumentException("Recipient email cannot be empty");
        }

        String toEmail = request.getTo().trim();
        String subject = (request.getSubject() != null && !request.getSubject().isBlank())
                ? request.getSubject().trim()
                : "Official Communication - Forensic Patrika";

        String senderName = (request.getSenderName() != null && !request.getSenderName().isBlank())
                ? request.getSenderName().trim()
                : ((sender != null && sender.getFullName() != null && !sender.getFullName().isBlank())
                    ? sender.getFullName().trim()
                    : "Forensic Patrika Administration");

        String senderTitle = (request.getSenderTitle() != null && !request.getSenderTitle().isBlank())
                ? request.getSenderTitle().trim()
                : "Editorial & Administrative Board";

        log.info("Admin [{}] sending direct custom email to [{}] with subject [{}]", senderName, toEmail, subject);

        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("RESEND_API_KEY is not configured. Skipping live email dispatch to: {}", toEmail);
            return;
        }

        try {
            String htmlContent = buildAdminCustomEmailHtml(request, senderName, senderTitle);
            sendEmailViaResend(
                    toEmail,
                    subject,
                    htmlContent,
                    request.getCc(),
                    request.getBcc(),
                    request.getReplyTo()
            );
        } catch (Exception e) {
            log.error("Failed to send admin custom email to {}", toEmail, e);
            throw new RuntimeException("Failed to send email to " + toEmail + ": " + e.getMessage(), e);
        }
    }

    private void sendEmailViaResend(String toEmail, String subject, String htmlContent) throws Exception {
        sendEmailViaResend(toEmail, subject, htmlContent, null, null, null);
    }

    private void sendEmailViaResend(
            String toEmail,
            String subject,
            String htmlContent,
            List<String> cc,
            List<String> bcc,
            String replyTo
    ) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("from", fromEmail);
        payload.put("to", List.of(toEmail));
        payload.put("subject", subject);
        payload.put("html", htmlContent);

        if (cc != null && !cc.isEmpty()) {
            payload.put("cc", cc);
        }
        if (bcc != null && !bcc.isEmpty()) {
            payload.put("bcc", bcc);
        }
        if (replyTo != null && !replyTo.isBlank()) {
            payload.put("reply_to", replyTo.trim());
        }

        String requestBody = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RESEND_API_URL))
                .header("Authorization", "Bearer " + resendApiKey.trim())
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            log.info("Email successfully sent to {} via Resend. Status: {}", toEmail, response.statusCode());
        } else {
            log.error("Resend API rejected email to {}. Status: {}, Body: {}", toEmail, response.statusCode(), response.body());
            throw new IllegalStateException("Resend email error: " + response.body());
        }
    }

    private String buildOtpEmailHtml(String otp, int expirationMinutes, String purposeTitle) {
        String safePurpose = HtmlUtils.htmlEscape(purposeTitle != null ? purposeTitle : "Email Verification");
        String safeOtp = HtmlUtils.htmlEscape(otp);

        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>{{PURPOSE}}</title>
              <style>
                body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f6f9; margin: 0; padding: 20px; color: #333333; }
                .container { max-width: 520px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.08); }
                .header { background: linear-gradient(135deg, #0B0F3B 0%, #3B82F6 100%); padding: 30px 20px; text-align: center; color: #ffffff; }
                .header h1 { margin: 0; font-size: 24px; font-weight: 700; letter-spacing: 0.5px; }
                .content { padding: 30px 25px; text-align: center; }
                .content p { font-size: 15px; line-height: 1.6; color: #4b5563; margin-bottom: 24px; }
                .otp-box { display: inline-block; background-color: #f0fdf4; border: 2px dashed #22c55e; border-radius: 10px; padding: 16px 36px; margin: 10px 0 24px 0; }
                .otp-code { font-size: 32px; font-weight: 800; letter-spacing: 8px; color: #15803d; font-family: 'Courier New', Courier, monospace; }
                .badge { display: inline-block; background-color: #fef3c7; color: #92400e; padding: 4px 12px; border-radius: 20px; font-size: 13px; font-weight: 600; margin-bottom: 20px; }
                .footer { background-color: #f9fafb; padding: 20px; text-align: center; font-size: 12px; color: #9ca3af; border-top: 1px solid #f3f4f6; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>{{PURPOSE}}</h1>
                </div>
                <div class="content">
                  <p>Please use the following One-Time Password (OTP) to complete your verification:</p>
                  <div class="otp-box">
                    <span class="otp-code">{{OTP}}</span>
                  </div>
                  <div>
                    <span class="badge">&#9201; Expires in {{EXPIRATION}} minutes</span>
                  </div>
                  <p style="font-size: 13px; color: #6b7280; margin-top: 10px;">
                    If you did not make this request, you can safely ignore this email.
                  </p>
                </div>
                <div class="footer">
                  &copy; {{YEAR}} Forensic Patrika Platform. All rights reserved.
                </div>
              </div>
            </body>
            </html>
            """
                .replace("{{PURPOSE}}", safePurpose)
                .replace("{{OTP}}", safeOtp)
                .replace("{{EXPIRATION}}", String.valueOf(expirationMinutes))
                .replace("{{YEAR}}", String.valueOf(Year.now().getValue()));
    }

    private String buildSubmissionConfirmationHtml(
            String authorName,
            String submissionId,
            String paperTitle,
            String researchArea
    ) {
        String safeAuthor = HtmlUtils.htmlEscape(authorName != null ? authorName : "Author");
        String safeSubId = HtmlUtils.htmlEscape(submissionId);
        String safeTitle = HtmlUtils.htmlEscape(paperTitle);
        String safeArea = HtmlUtils.htmlEscape(researchArea != null ? researchArea : "Forensic Science");

        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Research Paper Submission Confirmation</title>
              <style>
                body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f1f5f9; margin: 0; padding: 24px; color: #1e293b; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 14px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
                .header { background: linear-gradient(135deg, #0b1137 0%, #1e3a8a 100%); padding: 32px 24px; text-align: center; color: #ffffff; }
                .header h1 { margin: 0; font-size: 24px; font-weight: 700; }
                .header p { margin: 8px 0 0 0; font-size: 14px; color: #93c5fd; }
                .content { padding: 32px 28px; }
                .greeting { font-size: 16px; font-weight: 600; color: #0f172a; margin-bottom: 16px; }
                .desc { font-size: 14px; line-height: 1.7; color: #475569; margin-bottom: 24px; }
                .details-card { background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px; padding: 20px; margin-bottom: 24px; }
                .detail-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #f1f5f9; font-size: 14px; }
                .detail-row:last-child { border-bottom: none; }
                .detail-label { color: #64748b; font-weight: 500; }
                .detail-value { color: #0f172a; font-weight: 600; text-align: right; }
                .sub-badge { display: inline-block; background-color: #eff6ff; color: #1d4ed8; padding: 4px 12px; border-radius: 6px; font-family: monospace; font-size: 15px; font-weight: 700; }
                .steps-card { background-color: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 10px; padding: 18px; margin-bottom: 24px; }
                .steps-card h4 { margin: 0 0 10px 0; color: #166534; font-size: 15px; }
                .steps-card ul { margin: 0; padding-left: 20px; color: #15803d; font-size: 13px; line-height: 1.6; }
                .footer { background-color: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>Forensic Patrika</h1>
                  <p>Journal of Forensic Science & Research</p>
                </div>
                <div class="content">
                  <div class="greeting">Dear {{AUTHOR_NAME}},</div>
                  <div class="desc">
                    Thank you for submitting your research manuscript to <strong>Forensic Patrika</strong>. We have successfully received your submission and it has been queued for editorial screening and peer review.
                  </div>
                  <div class="details-card">
                    <div class="detail-row">
                      <span class="detail-label">Submission ID:</span>
                      <span class="detail-value"><span class="sub-badge">{{SUBMISSION_ID}}</span></span>
                    </div>
                    <div class="detail-row">
                      <span class="detail-label">Paper Title:</span>
                      <span class="detail-value" style="max-width: 320px;">{{PAPER_TITLE}}</span>
                    </div>
                    <div class="detail-row">
                      <span class="detail-label">Research Area:</span>
                      <span class="detail-value">{{RESEARCH_AREA}}</span>
                    </div>
                    <div class="detail-row">
                      <span class="detail-label">Status:</span>
                      <span class="detail-value" style="color: #2563eb;">Submitted (Under Screening)</span>
                    </div>
                  </div>
                  <div class="steps-card">
                    <h4>What Happens Next?</h4>
                    <ul>
                      <li><strong>Editorial Initial Screening:</strong> 3 &ndash; 5 business days</li>
                      <li><strong>Peer Review Process:</strong> 2 &ndash; 3 weeks</li>
                      <li><strong>Decision Notification:</strong> You will receive updates via email</li>
                    </ul>
                  </div>
                  <div class="desc" style="font-size: 13px; color: #64748b;">
                    Please quote your <strong>Submission ID ({{SUBMISSION_ID}})</strong> in all future communications regarding this manuscript.
                  </div>
                </div>
                <div class="footer">
                  &copy; {{YEAR}} Forensic Patrika &bull; Editorial & Publishing Office &bull; All Rights Reserved.
                </div>
              </div>
            </body>
            </html>
            """
                .replace("{{AUTHOR_NAME}}", safeAuthor)
                .replace("{{SUBMISSION_ID}}", safeSubId)
                .replace("{{PAPER_TITLE}}", safeTitle)
                .replace("{{RESEARCH_AREA}}", safeArea)
                .replace("{{YEAR}}", String.valueOf(Year.now().getValue()));
    }

    private String buildPublicationSuccessHtml(
            String authorName,
            String paperTitle,
            String journalTitle,
            String doi,
            String certificateUrl,
            String paperUrl
    ) {
        String safeAuthor = HtmlUtils.htmlEscape(authorName != null ? authorName : "Author");
        String safePaper = HtmlUtils.htmlEscape(paperTitle);
        String safeJournal = HtmlUtils.htmlEscape(journalTitle != null ? journalTitle : "Forensic Patrika Journal");
        String safeDoi = HtmlUtils.htmlEscape(doi);

        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Research Paper Published</title>
              <style>
                body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f1f5f9; margin: 0; padding: 24px; color: #1e293b; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 14px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
                .header { background: linear-gradient(135deg, #064e3b 0%, #059669 100%); padding: 36px 24px; text-align: center; color: #ffffff; }
                .header h1 { margin: 0; font-size: 24px; font-weight: 700; }
                .header p { margin: 8px 0 0 0; font-size: 14px; color: #a7f3d0; }
                .content { padding: 32px 28px; }
                .greeting { font-size: 16px; font-weight: 600; color: #0f172a; margin-bottom: 16px; }
                .desc { font-size: 14px; line-height: 1.7; color: #475569; margin-bottom: 24px; }
                .doi-box { background-color: #ecfdf5; border: 1px solid #a7f3d0; border-radius: 10px; padding: 18px; margin-bottom: 24px; text-align: center; }
                .doi-label { font-size: 12px; text-transform: uppercase; color: #047857; font-weight: 700; letter-spacing: 1px; }
                .doi-val { font-size: 16px; font-weight: 700; color: #065f46; margin-top: 4px; font-family: monospace; }
                .btn { display: inline-block; padding: 12px 24px; background: #059669; color: #ffffff; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 14px; margin: 6px 4px; }
                .btn-secondary { background: #4338ca; }
                .btn-container { text-align: center; margin: 24px 0; }
                .footer { background-color: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>Official Publication Notice</h1>
                  <p>{{JOURNAL_TITLE}}</p>
                </div>
                <div class="content">
                  <div class="greeting">Dear {{AUTHOR_NAME}},</div>
                  <div class="desc">
                    We are pleased to inform you that your research paper entitled <strong>"{{PAPER_TITLE}}"</strong> has been officially published and indexed in <strong>{{JOURNAL_TITLE}}</strong>.
                  </div>
                  <div class="doi-box">
                    <div class="doi-label">Assigned Digital Object Identifier (DOI)</div>
                    <div class="doi-val"><a href="https://doi.org/{{DOI}}" style="color: #065f46; text-decoration: none;">https://doi.org/{{DOI}}</a></div>
                  </div>
                  <div class="btn-container">
                    <a href="{{CERTIFICATE_URL}}" class="btn">🎓 Download Certificate (PDF)</a>
                    <a href="{{PAPER_URL}}" class="btn btn-secondary">📄 View Published Paper</a>
                  </div>
                  <div class="desc" style="font-size: 13px; color: #64748b;">
                    Your official Publication Certificate includes a secure cryptographic QR code for online institutional verification.
                  </div>
                </div>
                <div class="footer">
                  &copy; {{YEAR}} Forensic Patrika Platform &bull; All Rights Reserved.
                </div>
              </div>
            </body>
            </html>
            """
                .replace("{{AUTHOR_NAME}}", safeAuthor)
                .replace("{{PAPER_TITLE}}", safePaper)
                .replace("{{JOURNAL_TITLE}}", safeJournal)
                .replace("{{DOI}}", safeDoi)
                .replace("{{CERTIFICATE_URL}}", certificateUrl != null ? certificateUrl : "#")
                .replace("{{PAPER_URL}}", paperUrl != null ? paperUrl : "#")
                .replace("{{YEAR}}", String.valueOf(Year.now().getValue()));
    }

    private String buildCustomAuthorEmailHtml(
            String authorName,
            String subject,
            String messageContent,
            String paperTitle,
            String senderName
    ) {
        String safeSubject = HtmlUtils.htmlEscape(subject);
        String safeAuthor = HtmlUtils.htmlEscape(authorName != null ? authorName : "Author");
        String safeContent = HtmlUtils.htmlEscape(messageContent != null ? messageContent : "").replace("\n", "<br/>");
        String safeTitle = (paperTitle != null && !paperTitle.isBlank()) ? HtmlUtils.htmlEscape(paperTitle) : null;
        String safeSender = HtmlUtils.htmlEscape(senderName != null ? senderName : "Publishing Team");

        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>{{SUBJECT}}</title>
              <style>
                body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f1f5f9; margin: 0; padding: 24px; color: #1e293b; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 14px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
                .header { background: linear-gradient(135deg, #1e1b4b 0%, #312e81 100%); padding: 32px 24px; text-align: center; color: #ffffff; }
                .header h1 { margin: 0; font-size: 22px; font-weight: 700; }
                .header p { margin: 6px 0 0 0; font-size: 13px; color: #c7d2fe; }
                .content { padding: 32px 28px; }
                .greeting { font-size: 16px; font-weight: 600; color: #0f172a; margin-bottom: 16px; }
                .message-body { font-size: 15px; line-height: 1.8; color: #334155; white-space: pre-line; background: #f8fafc; padding: 20px; border-radius: 8px; border-left: 4px solid #4f46e5; margin-bottom: 24px; }
                .paper-info { font-size: 13px; color: #64748b; margin-bottom: 20px; }
                .signoff { font-size: 14px; color: #475569; }
                .footer { background-color: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>Forensic Patrika</h1>
                  <p>Editorial & Publishing Office</p>
                </div>
                <div class="content">
                  <div class="greeting">Dear {{AUTHOR_NAME}},</div>
                  {{PAPER_INFO}}
                  <div class="message-body">
                    {{MESSAGE_CONTENT}}
                  </div>
                  <div class="signoff">
                    Sincerely,<br/>
                    <strong>{{SENDER_NAME}}</strong><br/>
                    <em>Forensic Patrika Editorial Board</em>
                  </div>
                </div>
                <div class="footer">
                  &copy; {{YEAR}} Forensic Patrika &bull; All Rights Reserved.
                </div>
              </div>
            </body>
            </html>
            """
                .replace("{{SUBJECT}}", safeSubject)
                .replace("{{AUTHOR_NAME}}", safeAuthor)
                .replace("{{PAPER_INFO}}", (safeTitle != null) ?
                        "<div class=\"paper-info\"><strong>Regarding Manuscript:</strong> " + safeTitle + "</div>" : "")
                .replace("{{MESSAGE_CONTENT}}", safeContent)
                .replace("{{SENDER_NAME}}", safeSender)
                .replace("{{YEAR}}", String.valueOf(Year.now().getValue()));
    }

    private String buildAdminCustomEmailHtml(
            AdminSendEmailRequest request,
            String senderName,
            String senderTitle
    ) {
        String safeSubject = HtmlUtils.htmlEscape(request.getSubject() != null ? request.getSubject().trim() : "Forensic Patrika");
        String safeRecipientName = (request.getRecipientName() != null && !request.getRecipientName().isBlank())
                ? HtmlUtils.htmlEscape(request.getRecipientName().trim())
                : null;

        String formattedMessage;
        if (request.isHtml()) {
            formattedMessage = request.getMessage() != null ? request.getMessage() : "";
        } else {
            String escaped = HtmlUtils.htmlEscape(request.getMessage() != null ? request.getMessage() : "");
            formattedMessage = escaped.replace("\n", "<br/>");
        }

        String safeSender = HtmlUtils.htmlEscape(senderName);
        String safeTitle = HtmlUtils.htmlEscape(senderTitle);

        String greeting = (safeRecipientName != null)
                ? "Dear " + safeRecipientName + ","
                : "Hello,";

        String buttonHtml = "";
        if (request.getButtonUrl() != null && !request.getButtonUrl().isBlank()) {
            String btnText = (request.getButtonText() != null && !request.getButtonText().isBlank())
                    ? HtmlUtils.htmlEscape(request.getButtonText().trim())
                    : "Access Portal";
            String btnUrl = HtmlUtils.htmlEscape(request.getButtonUrl().trim());
            buttonHtml = """
                <div style="text-align: center; margin: 28px 0;">
                  <a href="%s" style="display: inline-block; padding: 13px 28px; background: linear-gradient(135deg, #1e1b4b 0%%, #4338ca 100%%); color: #ffffff; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 14px; box-shadow: 0 4px 12px rgba(67, 56, 202, 0.25);">
                    %s
                  </a>
                </div>
                """.formatted(btnUrl, btnText);
        }

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>%s</title>
              <style>
                body { font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, Helvetica, Arial, sans-serif; background-color: #f1f5f9; margin: 0; padding: 24px; color: #1e293b; }
                .container { max-width: 620px; margin: 0 auto; background: #ffffff; border-radius: 14px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
                .header { background: linear-gradient(135deg, #0f172a 0%%, #1e293b 50%%, #312e81 100%%); padding: 34px 28px; text-align: center; color: #ffffff; }
                .header h1 { margin: 0; font-size: 23px; font-weight: 700; letter-spacing: 0.5px; }
                .header p { margin: 6px 0 0 0; font-size: 13px; color: #94a3b8; letter-spacing: 0.3px; }
                .content { padding: 32px 30px; }
                .greeting { font-size: 16px; font-weight: 600; color: #0f172a; margin-bottom: 18px; }
                .message-body { font-size: 15px; line-height: 1.75; color: #334155; background: #f8fafc; padding: 22px; border-radius: 10px; border-left: 4px solid #4f46e5; margin-bottom: 24px; }
                .signoff { font-size: 14px; color: #475569; border-top: 1px solid #f1f5f9; padding-top: 18px; line-height: 1.6; }
                .footer { background-color: #f8fafc; padding: 22px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; line-height: 1.5; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>Forensic Patrika</h1>
                  <p>Official Journal & Administrative Communication</p>
                </div>
                <div class="content">
                  <div class="greeting">%s</div>
                  <div class="message-body">
                    %s
                  </div>
                  %s
                  <div class="signoff">
                    Warm regards,<br/>
                    <strong style="color: #0f172a;">%s</strong><br/>
                    <span style="color: #64748b; font-size: 13px;">%s</span><br/>
                    <em style="color: #4f46e5; font-size: 12px;">Forensic Patrika Platform</em>
                  </div>
                </div>
                <div class="footer">
                  This is an official communication dispatched from the Forensic Patrika Portal.<br/>
                  &copy; %d Forensic Patrika &bull; All Rights Reserved.
                </div>
              </div>
            </body>
            </html>
            """
                .formatted(
                        safeSubject,
                        greeting,
                        formattedMessage,
                        buttonHtml,
                        safeSender,
                        safeTitle,
                        Year.now().getValue()
                );
    }
}
