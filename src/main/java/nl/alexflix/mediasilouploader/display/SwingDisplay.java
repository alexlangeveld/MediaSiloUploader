package nl.alexflix.mediasilouploader.display;

import nl.alexflix.mediasilouploader.Main;
import nl.alexflix.mediasilouploader.Util;
import nl.alexflix.mediasilouploader.local.types.Export;
import nl.alexflix.mediasilouploader.local.types.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;


public class SwingDisplay extends SimpleDisplay implements Display {


    JFrame frame;
    JLabel statusDisplay;
    JLabel logDisplay;
    volatile boolean running = true;

    public SwingDisplay(List<Export> exports) {
        super(exports);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            Util.err("SwingDisplay: " + e.getMessage());
            Util.err(e);
        }

        this.frame = new JFrame("MediaSilo Uploader");
        this.frame.setUndecorated(false);
        this.frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.frame.setSize(1280, 720);
        this.frame.setResizable(false);
        this.frame.setBackground(new java.awt.Color(0, 0, 0)); // Set the background color of the JFrame
        this.frame.setVisible(true);
        this.frame.getContentPane().setBackground(Color.BLACK);
        this.frame.setIconImage(new ImageIcon(getClass().getResource("/favicon-4.png")).getImage());
        this.frame.setLocationRelativeTo(null);
        // Add window listener to handle custom close operation
        this.frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                Main.exit();
            }
        });

        this.statusDisplay = initStatusDisplay();
        this.frame.add(statusDisplay, BorderLayout.CENTER);

        this.logDisplay = initLogDisplay();
        this.frame.add(logDisplay, BorderLayout.SOUTH);

        // Load and add logo to BorderLayout.NORTH
        JLabel logoLabel = initLogoLabel(); // Method to initialize logo
        logoLabel.setHorizontalAlignment(SwingConstants.LEFT);
        if (logoLabel != null) {
            this.frame.add(logoLabel, BorderLayout.NORTH); // Add the logo to the top
        }
    }

    private JLabel initStatusDisplay() {
        JLabel label = new JLabel();
        label.setBounds(0, 0, 1280, 720);
        label.setForeground(new java.awt.Color(255, 255, 255));
        label.setText("MediaSilo Uploader");

//        Font consolasFont = new Font("Consolas", Font.PLAIN, 20);
//        if (consolasFont.getFamily().equals("Dialog")) {
//            // If Consolas is not available, fallback to Monospaced
//            consolasFont = new Font(Font.MONOSPACED, Font.PLAIN, 20);
//        }
//        label.setFont(consolasFont);

        // Load the custom Consolas font from the resources folder
        try {
            InputStream is = getClass().getResourceAsStream("/whitrabt.ttf");
            Font consolasFont = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(Font.PLAIN, 20);

            // Register the font with the system to ensure it's used properly
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(consolasFont);

            label.setFont(consolasFont);
        } catch (FontFormatException | IOException e) {
            Util.err(e);
            // Fallback to Monospaced if the custom font fails to load
            label.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 20));
        }


        label.setHorizontalAlignment(SwingConstants.LEFT);
        label.setVerticalAlignment(SwingConstants.TOP);
        return label;
    }

    private JLabel initLogDisplay() {
        JLabel label = new JLabel();
        label.setBounds(0, 0, 1280, 24);
        label.setForeground(new java.awt.Color(255, 255, 255));
        label.setText("MediaSilo Uploader");
        label.setFont(new java.awt.Font(Font.MONOSPACED, Font.PLAIN, 10));
        label.setHorizontalAlignment(SwingConstants.LEFT);
        label.setVerticalAlignment(SwingConstants.BOTTOM);
        return label;
    }

    private JLabel initLogoLabel() {
        try {
            // Load the logo from the resources folder
            ImageIcon logoIcon = new ImageIcon(getClass().getResource("/MediaSiloUploaderLogoV1.png")); // Adjust the path as needed

            // Scale the image if necessary
            Image logoImage = logoIcon.getImage().getScaledInstance(1240, 100, Image.SCALE_SMOOTH); // Adjust size as needed
            logoIcon = new ImageIcon(logoImage);

            // Create a JLabel with the logo icon
            JLabel logoLabel = new JLabel(logoIcon);
            logoLabel.setHorizontalAlignment(SwingConstants.CENTER); // Center the logo in the north

            return logoLabel;
        } catch (Exception e) {
            System.out.println("Error loading logo: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        this.frame.setVisible(true);
        while (running) {
            String redered = render(false);
            statusDisplay.setText(redered);
            logDisplay.setText(Util.getLogsHTML());

            //TODO MAKE BETER
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Util.err(e);
            }

        }

    }

    @Override
    String render(boolean show_logo) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<u>");
        sb.append(
                "  EXPORT                                 | Transcoden | Uploaden | Transcoden | Verzonden | Deleten                "
                        .replace(" ","&nbsp;")
        );
        sb.append("</u>");
        sb.append("<br>");

        for (Export export : super.exports) {
            String timeToLive;
            try {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime timeOfDeath = export.getTimeOfDeath();
                Duration duration = Duration.between(now, timeOfDeath);
                long hours = duration.toHours();
                long minutes = duration.toMinutes() % 60;
                long seconds = duration.getSeconds() % 60;
                timeToLive = String.format(" | %02d:%02d:%02d", hours, minutes, seconds);
            } catch (Exception e) {
                timeToLive = " | --:--:--";
            }

            String htmlGREEN = "<font color='#00FF00'>";
            String htmlEndFont = "</font>";

            // Replace spaces with &nbsp;
            String exportString = String.format(
                    "%s |       %3d%% |     %3d%% |       %3d%% |",
                    export.toSubString(40),
                    export.getLocalTranscodeProgress(),
                    export.getUploadProgress(),
                    export.getRemoteTranscodeProgress()
            ).replace(" ", "&nbsp;");

            sb.append(
                    exportString
                            + (export.isEmailSent() ? (htmlGREEN + "    Ja    " + htmlEndFont) : "    Nee   ")
                            .replace(" ", "&nbsp;")
                            + timeToLive.replace(" ", "&nbsp;")
            );

            sb.append("<br>");
        }

        Incoming[] incomings = Main.getAllIncoming();

        for (Incoming incoming : incomings) {
            if (incoming.isHidden()) continue;

            // Replace spaces with &nbsp;
            String incomingString = String.format("%40s |                                  ",
                    incoming.toSubString(40)
            ).replace(" ", "&nbsp;");

            sb.append(incomingString);
            sb.append("<br>");
        }
        sb.append(tempStatus.replace(" ", "&nbsp;"));
        sb.append("<br>");
        sb.append("</html>");
        return sb.toString();
    }

    // Method to convert ANSI escape codes in a string to HTML color codes
    public static String convertANSItoHTML(String ansiString) {
        // Define the ANSI color escape codes and their corresponding HTML color codes
        String[][] ansiToHtml = {
                {"\u001B[31m", "#FF0000"}, // Red
                {"\u001B[32m", "#00FF00"}, // Green
                {"\u001B[27m", "#FFFF00"}, // Yellow
                {"\u001B[34m", "#0000FF"}, // Blue
                {"\u001B[37;41m", "#FFFFFF; background-color:#FF0000"}, // White text on Red background
                {"\u001B[0;37m", "#FFFFFF"}, // White
                {"\u001B[40m", "#000000"}, // Background Black
                {"\u001B[1;92m", "#00FF00"}, // Bold High Intensity Green
                {"\u001B[1;93m", "#FFFF00"}, // Bold High Intensity Yellow
                {"\u001B[1;94m", "#0000FF"}, // Bold High Intensity Blue
                {"\u001B[1;95m", "#FF00FF"}, // Bold High Intensity Purple
                {"\u001B[1;96m", "#00FFFF"}, // Bold High Intensity Cyan
                {"\u001B[0m", "#FFFFFF"}     // Reset (set to white + black background)
        };

        // Start building the HTML string
        StringBuilder htmlString = new StringBuilder("<html>");

        // Add the base color as white (to handle strings that do not start with ANSI codes)
        String currentColor = "#FFFFFF";
        htmlString.append("<font color='").append(currentColor).append("'>");

        // Iterate over the ANSI string and replace escape codes with HTML font tags
        int i = 0;
        while (i < ansiString.length()) {
            // Check if the current substring contains an ANSI escape code
            boolean matched = false;
            for (String[] pair : ansiToHtml) {
                String ansiCode = pair[0];
                String htmlColor = pair[1];

                // If an ANSI escape code is found, change the HTML font color
                if (ansiString.startsWith(ansiCode, i)) {
                    // Close the previous <font> tag
                    htmlString.append("</font>");

                    // Set the new color and open a new <font> tag
                    currentColor = htmlColor;
                    htmlString.append("<font color='").append(currentColor).append("'>");

                    // Move the index past the ANSI escape code
                    i += ansiCode.length();
                    matched = true;
                    break;
                }
            }

            // If no ANSI escape code was matched, append the character to the HTML string
            if (!matched) {
                htmlString.append(ansiString.charAt(i));
                i++;
            }
        }

        // Close the final open <font> tag
        htmlString.append("</font></html>");

        return htmlString.toString();
    }
}

class customBox extends JComponent {
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Draw a horizontal line (box top)
        g2.drawLine(10, 10, 400, 10);

        // Draw the text
        g2.drawString("EXPORT | Transcoden | Uploaden | Transcoden | Verzonden | Deleten", 10, 30);

        // Draw another horizontal line (box bottom)
        g2.drawLine(10, 40, 400, 40);
    }
}