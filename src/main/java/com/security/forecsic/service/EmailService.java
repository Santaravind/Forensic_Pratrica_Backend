package com.security.forecsic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
     * Send OTP Verification Email
     */
    public void sendOtpEmail(String toEmail, String otp, int expirationMinutes) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("RESEND_API_KEY is not configured! OTP for {} is: {}", toEmail, otp);
            return;
        }

        try {
            String htmlContent = buildOtpEmailHtml(otp, expirationMinutes);
            sendEmailViaResend(toEmail, "Your Verification Code: " + otp, htmlContent);
        } catch (Exception e) {
            log.error("Error sending OTP email to {}", toEmail, e);
            throw new RuntimeException("Error occurred while sending verification email: " + e.getMessage(), e);
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

    private void sendEmailViaResend(String toEmail, String subject, String htmlContent) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("from", fromEmail);
        payload.put("to", List.of(toEmail));
        payload.put("subject", subject);
        payload.put("html", htmlContent);

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

    private String buildOtpEmailHtml(String otp, int expirationMinutes) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Email Verification</title>
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
                  <h1>Verification Code</h1>
                </div>
                <div class="content">
                  <p>Welcome to Forensic Patrika! Please use the following One-Time Password (OTP) to complete your verification:</p>
                  <div class="otp-box">
                    <span class="otp-code">{{OTP}}</span>
                  </div>
                  <div>
                    <span class="badge">&#9201; Expires in {{EXPIRATION}} minutes</span>
                  </div>
                  <p style="font-size: 13px; color: #6b7280; margin-top: 10px;">
                    If you did not request this registration, you can safely ignore this email.
                  </p>
                </div>
                <div class="footer">
                  &copy; {{YEAR}} Forensic Patrika Platform. All rights reserved.
                </div>
              </div>
            </body>
            </html>
            """
                .replace("{{OTP}}", otp)
                .replace("{{EXPIRATION}}", String.valueOf(expirationMinutes))
                .replace("{{YEAR}}", String.valueOf(Year.now().getValue()));
    }

    private String buildSubmissionConfirmationHtml(
            String authorName,
            String submissionId,
            String paperTitle,
            String researchArea
    ) {
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
                .replace("{{AUTHOR_NAME}}", authorName != null ? authorName : "Author")
                .replace("{{SUBMISSION_ID}}", submissionId)
                .replace("{{PAPER_TITLE}}", paperTitle)
                .replace("{{RESEARCH_AREA}}", researchArea != null ? researchArea : "Forensic Science")
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
                .replace("{{AUTHOR_NAME}}", authorName != null ? authorName : "Author")
                .replace("{{PAPER_TITLE}}", paperTitle)
                .replace("{{JOURNAL_TITLE}}", journalTitle != null ? journalTitle : "Forensic Patrika Journal")
                .replace("{{DOI}}", doi)
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
                .replace("{{SUBJECT}}", subject)
                .replace("{{AUTHOR_NAME}}", authorName != null ? authorName : "Author")
                .replace("{{PAPER_INFO}}", (paperTitle != null && !paperTitle.isBlank()) ?
                        "<div class=\"paper-info\"><strong>Regarding Manuscript:</strong> " + paperTitle + "</div>" : "")
                .replace("{{MESSAGE_CONTENT}}", messageContent)
                .replace("{{SENDER_NAME}}", senderName != null ? senderName : "Publishing Team")
                .replace("{{YEAR}}", String.valueOf(Year.now().getValue()));
    }
}
