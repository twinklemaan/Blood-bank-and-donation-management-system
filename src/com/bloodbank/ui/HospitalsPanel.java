package com.bloodbank.ui;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;

/**
 * Tab panel for managing Hospitals (Admin).
 */
public class HospitalsPanel extends BasePanel {
    public HospitalsPanel() {
        add(createHeaderPanel("Hospitals Management", "View, register, and update affiliated hospitals"), BorderLayout.NORTH);
        JLabel placeholder = new JLabel("Hospitals module initializing...", SwingConstants.CENTER);
        placeholder.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        add(placeholder, BorderLayout.CENTER);
    }
}
