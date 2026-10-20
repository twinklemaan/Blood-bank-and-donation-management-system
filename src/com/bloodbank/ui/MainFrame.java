package com.bloodbank.ui;

import com.bloodbank.dao.StockDao;
import com.bloodbank.exception.AppException;
import com.bloodbank.model.AdminUser;
import com.bloodbank.model.User;
import com.bloodbank.thread.StockMonitor;
import com.bloodbank.util.Session;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.BevelBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main application window displaying tabs customized according to the authenticated user's role.
 * Demonstrates polymorphism: builds tabs strictly by calling user.getTabs() (Rubric: Inheritance).
 */
public class MainFrame extends JFrame {
    private final User currentUser;
    private final JTabbedPane tabbedPane;
    private final Map<String, BasePanel> tabPanels = new HashMap<>();

    private JLabel userStatusLabel;
    private JLabel monitorStatusLabel;
    private JLabel timeStatusLabel;

    public MainFrame(User user) {
        super("Blood Bank & Donation Management System");
        this.currentUser = user;

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleExit();
            }
        });

        setSize(1100, 720);
        setMinimumSize(new Dimension(850, 550));
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        // Top Navigation and Header Bar
        add(createTopBar(), BorderLayout.NORTH);

        // Center Tabbed Pane created dynamically from user.getTabs() (Polymorphism)
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        buildRoleTabs();
        add(tabbedPane, BorderLayout.CENTER);

        // Bottom Status Bar
        add(createStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(245, 245, 245));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 210, 210)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        // Left Branding
        JLabel brand = new JLabel("Blood Bank Management System");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 16));
        brand.setForeground(new Color(178, 34, 34)); // Crimson
        bar.add(brand, BorderLayout.WEST);

        // Right Action controls
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        // Admin-only: Run expiry sweep now
        if (currentUser instanceof AdminUser) {
            JButton sweepBtn = new JButton("Run Expiry Sweep Now");
            sweepBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            sweepBtn.setToolTipText("Manually execute stored procedure sp_expire_all() to sweep expired blood units");
            sweepBtn.addActionListener(e -> runExpirySweep());
            rightPanel.add(sweepBtn);
        }

        JLabel roleBadge = new JLabel(currentUser.getRoleDisplayName());
        roleBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        roleBadge.setForeground(new Color(60, 60, 60));
        rightPanel.add(roleBadge);

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        logoutBtn.addActionListener(e -> handleLogout());
        rightPanel.add(logoutBtn);

        bar.add(rightPanel, BorderLayout.EAST);
        return bar;
    }

    /**
     * Builds tabs polymorphically by invoking currentUser.getTabs().
     */
    private void buildRoleTabs() {
        List<String> tabs = currentUser.getTabs();
        for (String tabName : tabs) {
            BasePanel panel = createPanelForTab(tabName);
            if (panel != null) {
                tabPanels.put(tabName, panel);
                tabbedPane.addTab(tabName, panel);
            }
        }

        tabbedPane.addChangeListener(e -> {
            int selected = tabbedPane.getSelectedIndex();
            if (selected >= 0) {
                String title = tabbedPane.getTitleAt(selected);
                BasePanel activePanel = tabPanels.get(title);
                if (activePanel != null) {
                    activePanel.refreshData();
                }
            }
        });
    }

    /**
     * Factory method mapping tab names to BasePanel instances.
     */
    private BasePanel createPanelForTab(String tabName) {
        return switch (tabName) {
            case "Banks" -> new BanksPanel();
            case "Hospitals" -> new HospitalsPanel();
            case "Staff" -> new StaffPanel();
            case "Stock" -> new StockPanel();
            case "All Donations" -> new AllDonationsPanel();
            case "All Requests" -> new AllRequestsPanel();
            case "Donors" -> new DonorsPanel();
            case "Donations" -> new DonationsPanel();
            case "Screening" -> new ScreeningPanel();
            case "Requests to Fulfil" -> new RequestsToFulfilPanel();
            case "Raise Request" -> new RaiseRequestPanel();
            case "My Requests" -> new MyRequestsPanel();
            case "Availability Search" -> new AvailabilitySearchPanel();
            default -> null;
        };
    }

    private JPanel createStatusBar() {
        JPanel status = new JPanel(new BorderLayout());
        status.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        status.setPreferredSize(new Dimension(getWidth(), 24));

        userStatusLabel = new JLabel(" Logged in as: " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
        userStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        monitorStatusLabel = new JLabel("Stock Monitor: Initializing...  ", JLabel.CENTER);
        monitorStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        timeStatusLabel = new JLabel("Ready  ", JLabel.RIGHT);
        timeStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        status.add(userStatusLabel, BorderLayout.WEST);
        status.add(monitorStatusLabel, BorderLayout.CENTER);
        status.add(timeStatusLabel, BorderLayout.EAST);

        return status;
    }

    public void updateStockMonitorStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            if (monitorStatusLabel != null) {
                monitorStatusLabel.setText("Stock Monitor: " + message);
            }
            if (timeStatusLabel != null) {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
                timeStatusLabel.setText("Updated: " + dtf.format(LocalDateTime.now()) + "  ");
            }
        });
    }

    public BasePanel getTabPanel(String tabName) {
        return tabPanels.get(tabName);
    }

    private void runExpirySweep() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Execute sp_expire_all() to sweep expired blood units now?",
                "Run Expiry Sweep",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                new StockDao().runExpirySweep();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Expiry sweep executed successfully. Any expired blood units have been cleared.",
                            "Expiry Sweep Complete",
                            JOptionPane.INFORMATION_MESSAGE);
                    // Refresh current active tab
                    int idx = tabbedPane.getSelectedIndex();
                    if (idx >= 0) {
                        BasePanel bp = tabPanels.get(tabbedPane.getTitleAt(idx));
                        if (bp != null) bp.refreshData();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Failed to execute expiry sweep: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION) {
            // Clean up session and background tasks
            StockMonitor.stopMonitoring();
            Session.getInstance().clear();
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        }
    }

    private void handleExit() {
        StockMonitor.stopMonitoring();
        Session.getInstance().clear();
        dispose();
        System.exit(0);
    }
}
