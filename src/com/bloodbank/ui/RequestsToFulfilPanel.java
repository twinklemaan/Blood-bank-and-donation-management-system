package com.bloodbank.ui;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;

/**
 * Tab panel for Bank Staff fulfilling pending blood requests.
 */
public class RequestsToFulfilPanel extends BasePanel {
    public RequestsToFulfilPanel() {
        add(createHeaderPanel("Requests to Fulfil", "Review pending requests prioritized by urgency and date, and issue stock"), BorderLayout.NORTH);
        JLabel placeholder = new JLabel("Fulfilment module initializing...", SwingConstants.CENTER);
        placeholder.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        add(placeholder, BorderLayout.CENTER);
    }
}
