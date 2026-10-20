package com.bloodbank.ui;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;

/**
 * Tab panel for managing Bank Staff members (Admin).
 */
public class StaffPanel extends BasePanel {
    public StaffPanel() {
        add(createHeaderPanel("Staff Management", "View, register, and update bank personnel and login credentials"), BorderLayout.NORTH);
        JLabel placeholder = new JLabel("Staff module initializing...", SwingConstants.CENTER);
        placeholder.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        add(placeholder, BorderLayout.CENTER);
    }
}
