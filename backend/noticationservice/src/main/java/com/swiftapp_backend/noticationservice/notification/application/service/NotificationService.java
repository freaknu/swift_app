package com.swiftapp_backend.noticationservice.notification.application.service;

import com.swiftapp_backend.noticationservice.notification.application.port.EmailSenderPort;
import com.swiftapp_backend.noticationservice.notification.application.port.NotificationPort;
import com.swiftapp_backend.noticationservice.notification.domain.model.EmailNotification;
import com.swiftapp_backend.noticationservice.notification.domain.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService implements NotificationPort {
    private final EmailSenderPort emailSender;
    private final PushNotificationService pushNotifications;

    public void sendOtpEmail(EmailNotification notification) {
        emailSender.send(notification);
    }

    public void sendRideRequestedNotification(String driverEmail, String driverName, String pickupLocation) {
        String subject = "New Ride Request";
        String htmlContent = buildRideRequestTemplate(driverName, pickupLocation);
        emailSender.send(new EmailNotification(driverEmail, subject, htmlContent));
        log.info("Ride requested email sent");
    }

    public void sendRideAcceptedNotification(String riderEmail, String riderName, String driverName, String vehicleInfo) {
        String subject = "Ride Accepted";
        String htmlContent = buildRideAcceptedTemplate(riderName, driverName, vehicleInfo);
        emailSender.send(new EmailNotification(riderEmail, subject, htmlContent));
        log.info("Ride accepted email sent");
    }

    public void sendDriverArrivedNotification(String riderEmail, String riderName, String driverName) {
        String subject = "Driver Has Arrived";
        String htmlContent = buildDriverArrivedTemplate(riderName, driverName);
        emailSender.send(new EmailNotification(riderEmail, subject, htmlContent));
        log.info("Driver arrived email sent");
    }

    public void sendRideStartedNotification(String riderEmail, String riderName, String destination) {
        String subject = "Ride Started";
        String htmlContent = buildRideStartedTemplate(riderName, destination);
        emailSender.send(new EmailNotification(riderEmail, subject, htmlContent));
        log.info("Ride started email sent");
    }

    public void sendRideCompletedNotification(String riderEmail, String riderName, Double fare) {
        String subject = "Ride Completed";
        String htmlContent = buildRideCompletedTemplate(riderName, fare);
        emailSender.send(new EmailNotification(riderEmail, subject, htmlContent));
        log.info("Ride completed email sent");
    }

    public void sendRideCancelledNotification(String recipientEmail, String recipientName, String cancelledBy, String reason) {
        String subject = "Ride Cancelled";
        String htmlContent = buildRideCancelledTemplate(recipientName, cancelledBy, reason);
        emailSender.send(new EmailNotification(recipientEmail, subject, htmlContent));
        log.info("Ride cancelled email sent");
    }

    public void sendPaymentSuccessNotification(String riderEmail, String riderName, Double amount, String paymentMethod) {
        String subject = "Payment Successful";
        String htmlContent = buildPaymentSuccessTemplate(riderName, amount, paymentMethod);
        emailSender.send(new EmailNotification(riderEmail, subject, htmlContent));
        log.info("Payment success email sent");
    }

    public void sendPaymentFailedNotification(String riderEmail, String riderName, Double amount, String reason) {
        String subject = "Payment Failed";
        String htmlContent = buildPaymentFailedTemplate(riderName, amount, reason);
        emailSender.send(new EmailNotification(riderEmail, subject, htmlContent));
        log.info("Payment failed email sent");
    }

    public void sendRatingReminderNotification(String riderEmail, String riderName, String driverName) {
        String subject = "Rate Your Recent Ride";
        String htmlContent = buildRatingReminderTemplate(riderName, driverName);
        emailSender.send(new EmailNotification(riderEmail, subject, htmlContent));
        log.info("Rating reminder email sent");
    }

    private String buildRideRequestTemplate(String driverName, String pickupLocation) {
        return String.format("""
            <html>
            <body>
                <h2>New Ride Request</h2>
                <p>Hello %s,</p>
                <p>You have a new ride request from <strong>%s</strong>.</p>
                <p>Please review and accept the ride in your app.</p>
                <br/>
                <p>Best regards,<br/>SwiftApp Team</p>
            </body>
            </html>
            """, driverName, pickupLocation);
    }

    private String buildRideAcceptedTemplate(String riderName, String driverName, String vehicleInfo) {
        return String.format("""
            <html>
            <body>
                <h2>Ride Accepted!</h2>
                <p>Hello %s,</p>
                <p>Your ride has been accepted by <strong>%s</strong>.</p>
                <p>Vehicle: <strong>%s</strong></p>
                <p>Your driver is on the way!</p>
                <br/>
                <p>Best regards,<br/>SwiftApp Team</p>
            </body>
            </html>
            """, riderName, driverName, vehicleInfo);
    }

    private String buildDriverArrivedTemplate(String riderName, String driverName) {
        return String.format("""
            <html>
            <body>
                <h2>Driver Has Arrived</h2>
                <p>Hello %s,</p>
                <p>Your driver <strong>%s</strong> has arrived at the pickup location.</p>
                <p>Please meet your driver now.</p>
                <br/>
                <p>Best regards,<br/>SwiftApp Team</p>
            </body>
            </html>
            """, riderName, driverName);
    }

    private String buildRideStartedTemplate(String riderName, String destination) {
        return String.format("""
            <html>
            <body>
                <h2>Ride Started</h2>
                <p>Hello %s,</p>
                <p>Your ride has started. Destination: <strong>%s</strong></p>
                <p>Have a safe journey!</p>
                <br/>
                <p>Best regards,<br/>SwiftApp Team</p>
            </body>
            </html>
            """, riderName, destination);
    }

    private String buildRideCompletedTemplate(String riderName, Double fare) {
        String amount = fare == null ? "available in the app" : String.format("₹%.2f", fare);
        return String.format("""
            <html>
            <body>
                <h2>Ride Completed</h2>
                <p>Hello %s,</p>
                <p>Your ride has been completed successfully.</p>
                <p>Total Fare: <strong>%s</strong></p>
                <p>Thank you for riding with us!</p>
                <br/>
                <p>Best regards,<br/>SwiftApp Team</p>
            </body>
            </html>
            """, riderName, amount);
    }

    private String buildRideCancelledTemplate(String recipientName, String cancelledBy, String reason) {
        return String.format("""
            <html>
            <body>
                <h2>Ride Cancelled</h2>
                <p>Hello %s,</p>
                <p>Your ride has been cancelled by <strong>%s</strong>.</p>
                <p>Reason: <strong>%s</strong></p>
                <p>We apologize for the inconvenience.</p>
                <br/>
                <p>Best regards,<br/>SwiftApp Team</p>
            </body>
            </html>
            """, recipientName, cancelledBy, reason);
    }

    private String buildPaymentSuccessTemplate(String riderName, Double amount, String paymentMethod) {
        return String.format("""
            <html>
            <body>
                <h2>Payment Successful</h2>
                <p>Hello %s,</p>
                <p>Your payment of <strong>$%.2f</strong> has been processed successfully.</p>
                <p>Payment Method: <strong>%s</strong></p>
                <p>Thank you!</p>
                <br/>
                <p>Best regards,<br/>SwiftApp Team</p>
            </body>
            </html>
            """, riderName, amount, paymentMethod);
    }

    private String buildPaymentFailedTemplate(String riderName, Double amount, String reason) {
        return String.format("""
            <html>
            <body>
                <h2>Payment Failed</h2>
                <p>Hello %s,</p>
                <p>Your payment of <strong>$%.2f</strong> has failed.</p>
                <p>Reason: <strong>%s</strong></p>
                <p>Please update your payment method and try again.</p>
                <br/>
                <p>Best regards,<br/>SwiftApp Team</p>
            </body>
            </html>
            """, riderName, amount, reason);
    }

    private String buildRatingReminderTemplate(String riderName, String driverName) {
        return String.format("""
            <html>
            <body>
                <h2>Rate Your Recent Ride</h2>
                <p>Hello %s,</p>
                <p>We hope you enjoyed your ride with <strong>%s</strong>!</p>
                <p>Please take a moment to rate your experience in the app.</p>
                <p>Your feedback helps us improve our service.</p>
                <br/>
                <p>Best regards,<br/>SwiftApp Team</p>
            </body>
            </html>
            """, riderName, driverName);
    }

    @Override
    public void sendNotification(Notification notification) {
        pushNotifications.send(notification.getReceiverId(), notification.getReceiverRole(),
                notification.getTitle(), notification.getBody(), Map.of(), 300);
    }
}
