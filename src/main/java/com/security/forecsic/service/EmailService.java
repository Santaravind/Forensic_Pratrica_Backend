package com.security.forecsic.service;

import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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

  public void sendOtpEmail(String toEmail, String otp, int expirationMinutes) {
    if (resendApiKey == null || resendApiKey.isBlank()) {
      log.warn("RESEND_API_KEY is not configured! OTP for {} is: {}", toEmail, otp);
      log.warn("Please set RESEND_API_KEY in your .env or application.properties to send real emails.");
      return;
    }

    try {
      String htmlContent = buildOtpEmailHtml(otp, expirationMinutes);

      Map<String, Object> payload = new HashMap<>();
      payload.put("from", fromEmail);
      payload.put("to", List.of(toEmail));
      payload.put("subject", "Your Verification Code: " + otp);
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
        log.info("OTP email sent successfully to {} via Resend. Response: {}", toEmail, response.body());
      } else {
        log.error("Failed to send OTP email via Resend to {}. Status: {}, Response: {}", toEmail, response.statusCode(),
            response.body());
        throw new IllegalStateException("Failed to send OTP email via Resend: " + response.body());
      }
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error sending OTP email to {}", toEmail, e);
      throw new RuntimeException("Error occurred while sending verification email: " + e.getMessage(), e);
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
            .header { background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%); padding: 30px 20px; text-align: center; color: #ffffff; }
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
              <p>Welcome! Thank you for signing up. Please use the following One-Time Password (OTP) to complete your account registration:</p>
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
              &copy; {{YEAR}} Forecsic Platform. All rights reserved.
            </div>
          </div>
        </body>
        </html>
        """
        .replace("{{OTP}}", otp)
        .replace("{{EXPIRATION}}", String.valueOf(expirationMinutes))
        .replace("{{YEAR}}", String.valueOf(java.time.Year.now().getValue()));
  }
}
