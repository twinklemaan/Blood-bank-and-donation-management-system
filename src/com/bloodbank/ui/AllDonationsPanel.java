package com.bloodbank.ui;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;

/**
 * Tab panel for Admin viewing all donations across banks.
 */
public class AllDonationsPanel extends BasePanel {
    public AllDonationsPanel() {
        add(createHeaderPanel("All Donations Audit", "Comprehensive log of all blood donations and screening states"), BorderLayout.NORTH);
        JLabel placeholder = new JLabel("All donations audit module initializing...", SwingConstants.CENTER);
        placeholder.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        add(placeholder, BorderLayout.CENTER);
    }
}
