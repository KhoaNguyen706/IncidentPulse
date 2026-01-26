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

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;
    private final String defaultRecipient;
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    public EmailService(JavaMailSender mailSender, @Value("${spring.mail.username:}") String from,
                        @Value("${app.email.default-recipient:anhkhoa17092006@gmail.com}") String defaultRecipient){
        this.mailSender = mailSender;
        this.from = from;
        this.defaultRecipient = defaultRecipient;
    }

    public void sendAssignmentEmail(User assignedUser, Incident incident){
        String to = (assignedUser != null && assignedUser.getEmail() != null && !assignedUser.getEmail().isBlank())
                ? assignedUser.getEmail()
                : this.defaultRecipient;

        if(to == null || to.isBlank()){
            log.info("No recipient available, skipping notification");
            return;
        }

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        if(from != null && !from.isBlank()){
            msg.setFrom(from);
        }
        msg.setSubject("[IncidentPulse] New Incident Assigned: " + incident.getTitle());
        msg.setText(buildBody(incident, assignedUser));
        mailSender.send(msg);
        log.info("Sent assignment email to {}", to);
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
        msg.setText(buildBody(incident, null));
        mailSender.send(msg);
        log.info("Sent incident email to default recipient {}", this.defaultRecipient);
    }

    private String buildBody(Incident incident, User user){
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
}
