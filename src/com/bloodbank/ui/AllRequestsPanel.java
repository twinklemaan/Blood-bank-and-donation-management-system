package com.bloodbank.ui;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;

/**
 * Tab panel for Admin viewing all requests across hospitals.
 */
public class AllRequestsPanel extends BasePanel {
    public AllRequestsPanel() {
        add(createHeaderPanel("All Requests Audit", "Comprehensive log of all blood requests, urgency, and fulfilment status"), BorderLayout.NORTH);
        JLabel placeholder = new JLabel("All requests audit module initializing...", SwingConstants.CENTER);
        placeholder.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        add(placeholder, BorderLayout.CENTER);
    }
}
