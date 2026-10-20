package com.bloodbank.ui;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;

/**
 * Tab panel for viewing blood stock levels and thresholds.
 */
public class StockPanel extends BasePanel {
    public StockPanel() {
        add(createHeaderPanel("Blood Stock Inventory", "Real-time stock levels with low-inventory alerts"), BorderLayout.NORTH);
        JLabel placeholder = new JLabel("Stock inventory module initializing...", SwingConstants.CENTER);
        placeholder.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        add(placeholder, BorderLayout.CENTER);
    }
}
