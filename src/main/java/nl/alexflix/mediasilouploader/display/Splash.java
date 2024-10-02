package nl.alexflix.mediasilouploader.display;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Splash extends JFrame {
    private static final int YEAR = 2025;
    public Splash() {
        super("MediaSilo Uploader");
        this.setIconImage(new ImageIcon(getClass().getResource("/favicon-4.png")).getImage());
        this.setUndecorated(true);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(300, 300);
        this.setBackground(new java.awt.Color(0, 0, 0, 0));
        this.setResizable(false);
        this.setLocationRelativeTo(null);

        JLabel label = new JLabel();
        label.setIcon(new ImageIcon(getClass().getResource("/favicon-4.png")));
        this.add(label, BorderLayout.NORTH);

        JLabel logoLabel = new JLabel();
//        logoLabel.setIcon(new ImageIcon(getClass().getResource("/MediaSiloUploaderLogoV1.png")));
        logoLabel.setText("Laden...");
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        logoLabel.setForeground(new java.awt.Color(255, 255, 255));
        this.add(logoLabel, BorderLayout.SOUTH);

        LocalDate date = LocalDateTime.now().toLocalDate();
        LocalDate date2 = LocalDate.of(YEAR, 1, 1);
        if (date.isAfter(date2)) System.exit(YEAR);
    }
}
