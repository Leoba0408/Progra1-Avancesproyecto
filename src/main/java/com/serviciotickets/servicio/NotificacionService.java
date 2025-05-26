package com.serviciotickets.servicio;

import com.serviciotickets.modelo.Ticket;
import com.serviciotickets.modelo.Tecnico;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Service
public class NotificacionService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    public void notificarNuevoTicket(Ticket ticket) throws MessagingException {
        String template = "email/nuevo-ticket";
        Map<String, Object> variables = new HashMap<>();
        variables.put("ticket", ticket);
        
        enviarCorreo(
            ticket.getDepartamento().getTecnicos(),
            "Nuevo ticket asignado: " + ticket.getTitulo(),
            template,
            variables
        );
    }

    public void notificarCambioEstado(Ticket ticket, String estadoAnterior) throws MessagingException {
        String template = "email/cambio-estado";
        Map<String, Object> variables = new HashMap<>();
        variables.put("ticket", ticket);
        variables.put("estadoAnterior", estadoAnterior);
        
        enviarCorreo(
            ticket.getSolicitante().getEmail(),
            "Actualización de ticket: " + ticket.getTitulo(),
            template,
            variables
        );
    }

    public void notificarNuevaNota(Ticket ticket, String nota) throws MessagingException {
        String template = "email/nueva-nota";
        Map<String, Object> variables = new HashMap<>();
        variables.put("ticket", ticket);
        variables.put("nota", nota);
        
        enviarCorreo(
            ticket.getSolicitante().getEmail(),
            "Nueva nota en ticket: " + ticket.getTitulo(),
            template,
            variables
        );
    }

    private void enviarCorreo(String destinatario, String asunto, String template, Map<String, Object> variables) 
            throws MessagingException {
        Context context = new Context(new Locale("es"));
        context.setVariables(variables);
        
        String contenido = templateEngine.process(template, context);
        
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setTo(destinatario);
        helper.setSubject(asunto);
        helper.setText(contenido, true);
        
        mailSender.send(message);
    }

    private void enviarCorreo(List<Tecnico> destinatarios, String asunto, String template, Map<String, Object> variables) 
            throws MessagingException {
        for (Tecnico destinatario : destinatarios) {
            enviarCorreo(destinatario.getEmail(), asunto, template, variables);
        }
    }
} 