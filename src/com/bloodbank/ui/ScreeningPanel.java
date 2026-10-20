package com.bloodbank.ui;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;

/**
 * Tab panel for laboratory screening of blood units (Bank Staff).
 */
public class ScreeningPanel extends BasePanel {
    public ScreeningPanel() {
        add(createHeaderPanel("Laboratory Screening", "Review pending donations and assign definitive Pass or Fail results"), BorderLayout.NORTH);
        JLabel placeholder = new JLabel("Screening module initializing...", SwingConstants.CENTER);
        placeholder.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        add(placeholder, BorderLayout.CENTER);
    }
}
