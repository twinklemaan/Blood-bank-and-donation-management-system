package com.bloodbank;

import com.bloodbank.ui.LoginFrame;
import com.bloodbank.util.AppConfig;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application Entry Point for the Blood Bank & Donation Management System.
 */
public class Main {
    public static void main(String[] args) {
        // Apply System Look and Feel for native OS appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fall back to default Swing look and feel
        }

        // Initialize application configuration
        AppConfig config = AppConfig.getInstance();
        if (!config.isFileLoaded()) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                        null,
                        "Notice: 'config/db.properties' was not found or could not be loaded.\n" +
                                "The application will use default connection settings.\n" +
                                "Please ensure MySQL is running and seed scripts are executed.",
                        "Configuration Notice",
                        JOptionPane.INFORMATION_MESSAGE
                );
            });
        }

        // Launch UI on Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
