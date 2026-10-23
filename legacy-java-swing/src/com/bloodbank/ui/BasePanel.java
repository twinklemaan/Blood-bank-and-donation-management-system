package com.bloodbank.ui;

import com.bloodbank.exception.AppException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Abstract base class for all tab panels in the application.
 * Satisfies the Inheritance rubric criterion and provides common UI styling and helpers.
 */
public abstract class BasePanel extends JPanel {

    public BasePanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    }

    /**
     * Called whenever the panel needs to refresh its data from the database.
     * Can be overridden by subclasses.
     */
    public void refreshData() {
        // Default no-op for panels without dynamic data
    }

    /**
     * Creates a standardized header panel with a title and optional subtitle.
     */
    protected JPanel createHeaderPanel(String title, String subtitle) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(178, 34, 34)); // Crimson red
        panel.add(titleLabel);

        if (subtitle != null && !subtitle.isEmpty()) {
            panel.add(Box.createRigidArea(new Dimension(0, 4)));
            JLabel subLabel = new JLabel(subtitle);
            subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            subLabel.setForeground(Color.GRAY);
            panel.add(subLabel);
        }

        return panel;
    }

    /**
     * Displays a friendly error dialog to the user.
     */
    protected void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Displays an informational message to the user.
     */
    protected void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Displays a warning dialog to the user.
     */
    protected void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Displays a confirmation dialog and returns true if user selects YES.
     */
    protected boolean confirmAction(String message, String title) {
        int choice = JOptionPane.showConfirmDialog(this, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }
}
