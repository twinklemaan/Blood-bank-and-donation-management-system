package com.bloodbank.ui;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;

/**
 * Tab panel for Hospitals to raise new blood supply requests.
 */
public class RaiseRequestPanel extends BasePanel {
    public RaiseRequestPanel() {
        add(createHeaderPanel("Raise Blood Request", "Submit blood supply requirements to affiliated blood banks"), BorderLayout.NORTH);
        JLabel placeholder = new JLabel("Raise request module initializing...", SwingConstants.CENTER);
        placeholder.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        add(placeholder, BorderLayout.CENTER);
    }
}
