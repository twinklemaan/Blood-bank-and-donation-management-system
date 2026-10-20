package com.bloodbank.ui;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;

/**
 * Tab panel for Hospitals to view status of their requests.
 */
public class MyRequestsPanel extends BasePanel {
    public MyRequestsPanel() {
        add(createHeaderPanel("My Blood Requests", "Track status (Pending, Fulfilled, Rejected) of raised requests"), BorderLayout.NORTH);
        JLabel placeholder = new JLabel("My requests module initializing...", SwingConstants.CENTER);
        placeholder.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        add(placeholder, BorderLayout.CENTER);
    }
}
