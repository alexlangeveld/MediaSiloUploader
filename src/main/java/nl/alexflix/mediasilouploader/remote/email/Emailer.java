package nl.alexflix.mediasilouploader.remote.email;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import nl.alexflix.mediasilouploader.local.types.Exit;
import nl.alexflix.mediasilouploader.local.types.Export;
import nl.alexflix.mediasilouploader.Main;
import nl.alexflix.mediasilouploader.Util;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;

public class Emailer implements Runnable {
    private boolean running = true;

    private final LinkedBlockingQueue<Export> emailQueue;
    private final LinkedBlockingQueue<Export> doneQueue;
    private JSONObject emailTemplate;

    private String host;
    private String port;
    private String username;
    private String password;
    public Emailer(LinkedBlockingQueue<Export> emailQueue, String emailTemplate, LinkedBlockingQueue<Export> doneQueue) {
        this.emailQueue = emailQueue;
        this.doneQueue = doneQueue;

        try {
            File template = new File(emailTemplate);
            if (!template.exists()) throw new FileNotFoundException("Email template bestand niet gevonden.");
            Scanner scanner = new Scanner(template);
            StringBuilder sb = new StringBuilder();
            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine());
            }
            this.emailTemplate = new JSONObject(sb.toString());
            this.host = this.emailTemplate.getString("host");
            this.port = this.emailTemplate.getString("port");
            this.username = this.emailTemplate.getString("user");
            this.password = this.emailTemplate.getString("password");
            scanner.close();
        } catch (FileNotFoundException e) {
            Util.err("Email template niet gevonden: " + e.getMessage());
            Main.exit();
        }
    }



    @Override
    public void run() {
        Util.success("Emailer gestart");
        while (running) {
            try {
                Export export = emailQueue.take();
                if (export instanceof Exit) {
                    if (Main.queuesEmpty()) {
                        running = false;
                        doneQueue.put(export);
                        break; }
                    else {
                        emailQueue.put(export);
                        continue;
                    }
                }
                if (export.sendEmail()) {
                    Email email = new Email(export, emailTemplate, username);
                    boolean sent = send(email, false);
                    export.setEmailSent(sent);
                    doneQueue.put(export);
                } else {
                    export.setEmailSent(false);
                    doneQueue.put(export);
                }
            } catch (InterruptedException e) {
                Util.err("E-mail wachtrij reageert niet: " + e.getMessage());
            } catch (Exception e) {
                Util.err("Kan geen e-mails verzenden: " + e.getMessage());
                Main.exit();
                running = false;
            }

        }
        Util.success("Emailer gestopt");
    }

    private boolean send(Email email) {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", port);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));


            for (String toAddress : email.getToAddresses()) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(toAddress));
            }


            if (email.getCcAddresses() != null) {
                for (String ccAddress : email.getCcAddresses()) {
                    message.addRecipient(Message.RecipientType.CC, new InternetAddress(ccAddress));
                }
            }

            message.setSubject(email.getOnderwerp());
            message.setText(email.getEmailBody());

            Transport.send(message);
            Util.success(email.getOnderwerp() + " is verstuurd");
        } catch (MessagingException e) {
            Util.err("Kon " + email.getOnderwerp() + " niet sturen: " + e.getMessage());
            return false;
        }


        return true;
    }


    private boolean send(Email email, boolean dryRun) {
        if (dryRun) {
            System.out.println();
            System.out.println();
            System.out.println("From: " + email.getFromAdress());
            System.out.println("To: " + email.getToAddresses());
            for (String ccAddress : email.getCcAddresses()) {
                System.out.println("Cc: " + ccAddress);
            }
            System.out.println();
            System.out.println("Subject: " + email.getOnderwerp());
            System.out.println();
            System.out.println(email.getEmailBody());
            System.out.println();
            return true;
        }
        return send(email);
    }




}
