package com.docuverify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url:https://docuverify-platform.vercel.app}")
    private String frontendUrl;

    @Value("${spring.mail.username:noreply@docuverify.com}")
    private String fromAddress;

    /**
     * Sent when a verifier approves a document.
     * Includes the public verification link.
     */
    @Async
    public void sendApprovalEmail(String toEmail, String uploaderName,
                                   String documentTitle, String verificationToken,
                                   String institutionName) {
        try {
            String verifyUrl = frontendUrl + "/verify/" + verificationToken;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("[DocuVerify] Your document has been approved — " + documentTitle);
            message.setText(
                "Hi " + uploaderName + ",\n\n" +
                "Great news! Your document has been reviewed and approved by " + institutionName + ".\n\n" +
                "Document: " + documentTitle + "\n" +
                "Institution: " + institutionName + "\n\n" +
                "You can now share the verification link below with employers, " +
                "institutions, or anyone who needs to verify this document:\n\n" +
                verifyUrl + "\n\n" +
                "Anyone with this link can verify your document — no account required.\n\n" +
                "— DocuVerify Trust Platform"
            );
            mailSender.send(message);
            log.info("Approval email sent to {}", toEmail);
        } catch (Exception e) {
            // Never fail the main transaction because of email
            log.error("Failed to send approval email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Sent when a verifier rejects a document.
     * Includes the rejection reason so the uploader knows what to fix.
     */
    @Async
    public void sendRejectionEmail(String toEmail, String uploaderName,
                                    String documentTitle, String rejectionReason,
                                    String institutionName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("[DocuVerify] Your document needs attention — " + documentTitle);
            message.setText(
                "Hi " + uploaderName + ",\n\n" +
                "Your document has been reviewed by " + institutionName +
                " and could not be approved at this time.\n\n" +
                "Document: " + documentTitle + "\n" +
                "Institution: " + institutionName + "\n\n" +
                "Reason for rejection:\n" + rejectionReason + "\n\n" +
                "You can delete the document and upload a corrected version, " +
                "then resubmit it for review.\n\n" +
                "Log in to your dashboard:\n" + frontendUrl + "/documents\n\n" +
                "— DocuVerify Trust Platform"
            );
            mailSender.send(message);
            log.info("Rejection email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send rejection email to {}: {}", toEmail, e.getMessage());
        }
    }
}