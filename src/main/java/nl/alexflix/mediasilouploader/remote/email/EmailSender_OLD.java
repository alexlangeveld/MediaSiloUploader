//package nl.alexflix.mediasilouploader.remote.email;
//import jakarta.mail.*;
//import jakarta.mail.internet.InternetAddress;
//import jakarta.mail.internet.MimeMessage;
//import nl.alexflix.mediasilouploader.Main;
//
//import java.util.Properties;
//
//public class EmailSender_OLD {
//
//    public static void sendEmail(String host, String port, String username, String password, String[] toAddresses, String[] ccAddresses, String subject, String messageBody) {
//        Properties properties = new Properties();
//        properties.put("mail.smtp.host", host);
//        properties.put("mail.smtp.port", port);
//        properties.put("mail.smtp.auth", "true");
//        properties.put("mail.smtp.starttls.enable", "true");
//
//        Session session = Session.getInstance(properties, new Authenticator() {
//            @Override
//            protected PasswordAuthentication getPasswordAuthentication() {
//                return new PasswordAuthentication(username, password);
//            }
//        });
//
//        try {
//            Message message = new MimeMessage(session);
//            message.setFrom(new InternetAddress(username));
//
//            // Set recipients
//            for (String toAddress : toAddresses) {
//                message.addRecipient(Message.RecipientType.TO, new InternetAddress(toAddress));
//            }
//
//            // Set CC recipients
//            if (ccAddresses != null) {
//                for (String ccAddress : ccAddresses) {
//                    message.addRecipient(Message.RecipientType.CC, new InternetAddress(ccAddress));
//                }
//            }
//
//            message.setSubject(subject);
//            message.setText(messageBody);
//
//            Transport.send(message);
//            System.out.println("Email sent successfully");
//        } catch (MessagingException e) {
//            e.printStackTrace();
//            System.out.println("Failed to send email");
//        }
//    }
//
//    public static void main(String[] args) {
//        String host = "smtp.gmail.com";
//        String port = "587";
//        String username = "alexander.langeveld@gmail.com";
//        String password = "wjgy xwwa estb cttp";
//        String[] toAddresses = {Main.standaardEmail()};
//        String[] ccAddresses = {};
//        String subject = "Test Email";
//        String messageBody = composeMessageBody("Testbestand.mp4", "http://example.com/blabla/1234567890");
//
//        sendEmail(host, port, username, password, toAddresses, ccAddresses, subject, messageBody);
//    }
//
//    void temp(){
//        String host = "smtp.gmail.com";
//        String port = "587";
//        String username = "alexander.langeveld@gmail.com";
//        String password = "wjgy xwwa estb cttp";
//        String[] toAddresses = {Main.standaardEmail()};
//        String[] ccAddresses = {};
//        String subject = "Test Email";
//        String messageBody = """
//                Hello!
//
//                This is a test email.
//
//                XOXO""";
//    }
//
//    public static String composeMessageBody(String name, String url) {
//        return String.format("""
//                Hoihoi,
//
//                Hier het linkje van %s: %s
//
//                Mvg, Alex""", name, url);
//    }
//}
//
