package com.security.forecsic.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import com.security.forecsic.model.*;
import com.security.forecsic.repositery.jpa.CertificateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateGeneratorService {

    private final CertificateRepository certificateRepository;
    private final Cloudinary cloudinary;

    @Value("${app.frontend.url:https://forensicpatrika.com}")
    private String frontendUrl;

    /**
     * Generate publication certificates for all authors of a research paper
     */
    @Transactional
    public List<Certificate> generatePublicationCertificates(ResearchPaper paper, Register recipientUser) {
        List<Certificate> createdCertificates = new ArrayList<>();
        List<ResearchPaperAuthor> authors = paper.getAuthors();

        if (authors == null || authors.isEmpty()) {
            // Fallback to submittedBy or single certificate
            String name = (paper.getSubmittedBy() != null) ? paper.getSubmittedBy().getFullName() : "Author";
            String email = (paper.getSubmittedBy() != null) ? paper.getSubmittedBy().getEmail() : "author@forensicpatrika.com";
            Certificate cert = generateSingleCertificate(paper, recipientUser, name, email, CertificateType.AUTHOR_PUBLICATION);
            createdCertificates.add(cert);
        } else {
            for (ResearchPaperAuthor author : authors) {
                Certificate cert = generateSingleCertificate(
                        paper,
                        recipientUser,
                        author.getName(),
                        author.getEmail(),
                        CertificateType.AUTHOR_PUBLICATION
                );
                createdCertificates.add(cert);
            }
        }

        return createdCertificates;
    }

    /**
     * Generate on-demand single certificate
     */
    @Transactional
    public Certificate generateSingleCertificate(
            ResearchPaper paper,
            Register recipientUser,
            String recipientName,
            String recipientEmail,
            CertificateType certificateType
    ) {
        String certTypeStr = (certificateType != null) ? certificateType.name() : CertificateType.AUTHOR_PUBLICATION.name();
        CertificateType certType = (certificateType != null) ? certificateType : CertificateType.AUTHOR_PUBLICATION;

        int randomNum = 1000 + new Random().nextInt(9000);
        String certNo = String.format("CERT-FP-%d-%d", Year.now().getValue(), randomNum);

        String rawHashInput = certNo + "-" + (paper != null ? paper.getId().toString() : "GEN") + "-" + recipientEmail + "-" + System.currentTimeMillis();
        String qrVerificationCode = generateSha256Hash(rawHashInput);

        // 1. Generate PDF in memory
        byte[] pdfBytes = createCertificatePdf(paper, certNo, recipientName, certType, qrVerificationCode);

        // 2. Upload to Cloudinary
        String pdfUrl;
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(pdfBytes, ObjectUtils.asMap(
                    "public_id", "certificates/" + certNo,
                    "resource_type", "raw",
                    "use_filename", true
            ));
            pdfUrl = (String) uploadResult.get("secure_url");
            if (pdfUrl == null) {
                pdfUrl = (String) uploadResult.get("url");
            }
        } catch (Exception e) {
            log.error("Failed to upload certificate PDF to Cloudinary for {}", certNo, e);
            // Fallback placeholder URL if Cloudinary upload fails
            pdfUrl = "https://storage.forensicpatrika.com/certificates/" + certNo + ".pdf";
        }

        // 3. Save Certificate in Database
        Certificate certificate = Certificate.builder()
                .certificateNo(certNo)
                .recipientUser(recipientUser)
                .recipientName(recipientName)
                .recipientEmail(recipientEmail)
                .researchPaper(paper)
                .certificateType(certType)
                .issueDate(LocalDate.now())
                .certificatePdfUrl(pdfUrl)
                .qrVerificationCode(qrVerificationCode)
                .isRevoked(false)
                .build();

        return certificateRepository.save(certificate);
    }

    /**
     * Generate OpenPDF styled landscape document in memory
     */
    private byte[] createCertificatePdf(
            ResearchPaper paper,
            String certNo,
            String recipientName,
            CertificateType certType,
            String qrCode
    ) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            PdfContentByte cb = writer.getDirectContent();

            // Decorative Borders (A4 Landscape: 842 x 595 pt)
            cb.setRGBColorStroke(11, 15, 59); // Dark Navy #0B0F3B
            cb.setLineWidth(4f);
            cb.rectangle(20, 20, 802, 555);
            cb.stroke();

            cb.setRGBColorStroke(217, 119, 6); // Amber Gold #D97706
            cb.setLineWidth(1.5f);
            cb.rectangle(28, 28, 786, 539);
            cb.stroke();

            // Fonts
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, new Color(11, 15, 59));
            Font fontSubHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(99, 102, 241));
            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(217, 119, 6));
            Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 12, new Color(75, 85, 99));
            Font fontPaperTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(17, 24, 39));
            Font fontAuthor = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(17, 24, 39));
            Font fontFooter = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(107, 114, 128));

            // Content
            Paragraph pHeader = new Paragraph("FORENSIC PATRIKA", fontHeader);
            pHeader.setAlignment(Element.ALIGN_CENTER);
            pHeader.setSpacingBefore(10f);
            document.add(pHeader);

            Paragraph pSub = new Paragraph("JOURNAL OF FORENSIC SCIENCE & RESEARCH", fontSubHeader);
            pSub.setAlignment(Element.ALIGN_CENTER);
            pSub.setSpacingAfter(15f);
            document.add(pSub);

            String titleText = switch (certType) {
                case REVIEWER_EXCELLENCE -> "CERTIFICATE OF EXCELLENCE IN PEER REVIEW";
                case EDITORIAL_BOARD -> "CERTIFICATE OF EDITORIAL BOARD APPOINTMENT";
                default -> "CERTIFICATE OF PUBLICATION";
            };

            Paragraph pCertTitle = new Paragraph(titleText, fontTitle);
            pCertTitle.setAlignment(Element.ALIGN_CENTER);
            pCertTitle.setSpacingAfter(12f);
            document.add(pCertTitle);

            Paragraph pLead = new Paragraph("This is to certify that the research article entitled:", fontBody);
            pLead.setAlignment(Element.ALIGN_CENTER);
            pLead.setSpacingAfter(6f);
            document.add(pLead);

            String paperTitle = (paper != null && paper.getTitle() != null) ? paper.getTitle() : "Research Paper";
            Paragraph pPaper = new Paragraph("\"" + paperTitle + "\"", fontPaperTitle);
            pPaper.setAlignment(Element.ALIGN_CENTER);
            pPaper.setSpacingAfter(10f);
            document.add(pPaper);

            Paragraph pAuthor = new Paragraph("Authored by: " + recipientName, fontAuthor);
            pAuthor.setAlignment(Element.ALIGN_CENTER);
            pAuthor.setSpacingAfter(4f);
            document.add(pAuthor);

            String journalName = (paper != null && paper.getJournal() != null) ? paper.getJournal().getTitle() : "Forensic Patrika Journal";
            String issueName = (paper != null && paper.getIssue() != null) ? paper.getIssue().getIssueTitle() : "Current Volume";
            String doiStr = (paper != null && paper.getDoi() != null) ? paper.getDoi() : "N/A";

            Paragraph pPubInfo = new Paragraph("Published in: " + journalName + " (" + issueName + ")", fontBody);
            pPubInfo.setAlignment(Element.ALIGN_CENTER);
            pPubInfo.setSpacingAfter(2f);
            document.add(pPubInfo);

            Paragraph pDoi = new Paragraph("DOI: " + doiStr, fontBody);
            pDoi.setAlignment(Element.ALIGN_CENTER);
            pDoi.setSpacingAfter(15f);
            document.add(pDoi);

            // Generate and Embed QR Code
            String verificationUrl = frontendUrl + "/verify-certificate/" + qrCode;
            byte[] qrImageBytes = generateQrCodePng(verificationUrl, 90, 90);
            if (qrImageBytes != null) {
                Image qrImage = Image.getInstance(qrImageBytes);
                qrImage.setAbsolutePosition(700, 45);
                qrImage.scaleAbsolute(75, 75);
                document.add(qrImage);
            }

            // Footer metadata
            ColumnText ct = new ColumnText(cb);
            ct.setSimpleColumn(50, 35, 600, 85);
            Paragraph pMeta1 = new Paragraph("Certificate No: " + certNo, fontFooter);
            Paragraph pMeta2 = new Paragraph("Issued Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")), fontFooter);
            Paragraph pMeta3 = new Paragraph("Verify Authenticity: " + frontendUrl + "/verify-certificate", fontFooter);
            ct.addElement(pMeta1);
            ct.addElement(pMeta2);
            ct.addElement(pMeta3);
            ct.go();

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate certificate PDF", e);
            throw new RuntimeException("Error rendering certificate PDF: " + e.getMessage(), e);
        }
    }

    private byte[] generateQrCodePng(String text, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error generating QR code image", e);
            return null;
        }
    }

    private String generateSha256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}
