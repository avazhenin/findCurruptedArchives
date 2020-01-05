/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package findCurruptedArchives;

import java.io.File;
import java.util.Date;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 *
 * @author vazhenin
 */
public class sendMailSMTP {

    private String SmtpUsername;
    private String SmtpPassword;
    private boolean authentication;

    public sendMailSMTP() {
        this.authentication = false;
    }

    public sendMailSMTP(String SmtpUsername, String SmtpPassword) {
        this.authentication = true;
        this.SmtpUsername = SmtpUsername;
        this.SmtpPassword = SmtpPassword;
    }

    public String getSmtpUsername() {
        return SmtpUsername;
    }

    public String getSmtpPassword() {
        return SmtpPassword;
    }

    void sendSMTPMessage(String text, String i_from, String i_to, String i_cc, String i_bcc, String Mailhost, File file, String i_subject) {
        String mailer = "msgsend";
        String mailhost = Mailhost;
        String bodyText = text;
        String subject = i_subject;
        String from = i_from;
        String to = i_to;
        String cc = i_cc;
        String bcc = i_bcc;
        Authenticator auth = null;

        try {
            Properties props = System.getProperties();
            props.put("mail.smtp.host", mailhost);
            if (this.authentication) {
                props.put("mail.smtp.auth", "true");
                auth = new SMTPAuthenticator();
            }

            // Get a Session object            
            Session session = Session.getInstance(props, auth);
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from));
            msg.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(to, false));
            if (cc != null) {
                msg.setRecipients(Message.RecipientType.CC,
                        InternetAddress.parse(cc, false));
            }
            if (bcc != null) {
                msg.setRecipients(Message.RecipientType.BCC,
                        InternetAddress.parse(bcc, false));
            }

            msg.setSubject(subject);
            if (file != null) {
                // Attach the specified file.
                // We need a multipart message to hold the attachment.
                MimeBodyPart mbp1 = new MimeBodyPart();
                mbp1.setText(bodyText);
                MimeBodyPart mbp2 = new MimeBodyPart();
                mbp2.attachFile(file);
                MimeMultipart mp = new MimeMultipart();
                mp.addBodyPart(mbp1);
                mp.addBodyPart(mbp2);
                msg.setContent(mp);
            } else {
                // If the desired charset is known, you can use
                // setText(text, charset)
                msg.setText(text);
            }
            msg.setHeader("X-Mailer", mailer);
            msg.setSentDate(new Date());

            // send the thing off
            Transport.send(msg);

            System.out.println("\nMail was sent successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private class SMTPAuthenticator extends javax.mail.Authenticator {

        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(getSmtpUsername(), getSmtpPassword());
        }
    }
}
