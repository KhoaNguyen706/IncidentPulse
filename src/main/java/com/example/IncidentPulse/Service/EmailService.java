package com.example.IncidentPulse.Service;

import com.example.IncidentPulse.Model.Incident;
import com.example.IncidentPulse.Model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private static final DateTimeFormatter SHIFT_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JavaMailSender mailSender;
    private final String from;
    private final String defaultRecipient;
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    public EmailService(JavaMailSender mailSender, @Value("${spring.mail.username:}") String from,
                        @Value("${app.email.default-recipient:}") String defaultRecipient){
        this.mailSender = mailSender;
        this.from = from;
        this.defaultRecipient = defaultRecipient;
    }

    public void sendAssignmentEmail(User assignedUser, Incident incident){
        String to = resolveRecipient(assignedUser);

        if(to == null || to.isBlank()){
            log.info("No recipient available, skipping incident assignment notification");
            return;
        }

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        if(from != null && !from.isBlank()){
            msg.setFrom(from);
        }
        msg.setSubject("[IncidentPulse] New Incident Assigned: " + incident.getTitle());
        msg.setText(buildIncidentBody(incident, assignedUser));
        mailSender.send(msg);
        log.info("Sent assignment email to {}", to);
    }

    public void sendOnCallShiftEmail(User engineer, LocalDateTime startedAt, LocalDateTime endAt) {
        String to = resolveRecipient(engineer);
        if (to == null || to.isBlank()) {
            log.info("No recipient available, skipping on-call shift notification");
            return;
        }

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        if (from != null && !from.isBlank()) {
            msg.setFrom(from);
        }
        msg.setSubject("[IncidentPulse] You are scheduled for on-call");
        msg.setText(buildOnCallBody(engineer, startedAt, endAt));
        mailSender.send(msg);
        log.info("Sent on-call shift email to {}", to);
    }

    public void sendToDefaultRecipient(Incident incident){
        if(this.defaultRecipient == null || this.defaultRecipient.isBlank()){
            log.info("Default recipient not set, skipping email");
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(this.defaultRecipient);
        if(from != null && !from.isBlank()){
            msg.setFrom(from);
        }
        msg.setSubject("[IncidentPulse] New Incident Created: " + incident.getTitle());
        msg.setText(buildIncidentBody(incident, null));
        mailSender.send(msg);
        log.info("Sent incident email to default recipient {}", this.defaultRecipient);
    }

    private String resolveRecipient(User user) {
        if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail();
        }
        return defaultRecipient;
    }

    private String buildIncidentBody(Incident incident, User user){
        StringBuilder text = new StringBuilder();
        text.append("Hello ").append(user == null || user.getName() == null ? "" : user.getName()).append(",\n\n");
        text.append(user == null ? "An incident has been created:\n\n" : "A new incident has been created and assigned to you:\n\n");
        text.append("Title: ").append(incident.getTitle()).append("\n");
        text.append("Severity: ").append(incident.getSeverity()).append("\n");
        text.append("Message: \n").append(incident.getMessage()).append("\n\n");
        text.append("Please check the Incident Pulse application to acknowledge and handle the incident.\n\n");
        text.append("Thanks,\nIncident Pulse");
        return text.toString();
    }

    private String buildOnCallBody(User engineer, LocalDateTime startedAt, LocalDateTime endAt) {
        String name = engineer != null && engineer.getName() != null ? engineer.getName() : "Engineer";
        StringBuilder text = new StringBuilder();
        text.append("Hello ").append(name).append(",\n\n");
        text.append("You have been scheduled for on-call duty in IncidentPulse.\n\n");
        text.append("Start: ").append(startedAt.format(SHIFT_TIME)).append("\n");
        text.append("End:   ").append(endAt.format(SHIFT_TIME)).append("\n\n");
        text.append("During this window, new incidents will be assigned to you automatically.\n");
        text.append("Open the My Incidents tab in IncidentPulse to view and update your assignments.\n\n");
        text.append("Thanks,\nIncident Pulse");
        return text.toString();
    }
}
