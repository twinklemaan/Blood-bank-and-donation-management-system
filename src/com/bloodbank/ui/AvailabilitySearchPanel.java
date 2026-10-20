package com.bloodbank.ui;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;

/**
 * Tab panel for Hospitals to search blood availability across banks.
 */
public class AvailabilitySearchPanel extends BasePanel {
    public AvailabilitySearchPanel() {
        add(createHeaderPanel("Blood Availability Search", "Search blood units available across blood banks by group and city"), BorderLayout.NORTH);
        JLabel placeholder = new JLabel("Availability search module initializing...", SwingConstants.CENTER);
        placeholder.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        add(placeholder, BorderLayout.CENTER);
    }
}
