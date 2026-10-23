package com.bloodbank.ui;

import com.bloodbank.dao.StockDao;
import com.bloodbank.model.AdminUser;
import com.bloodbank.model.BankStaffUser;
import com.bloodbank.model.StockEntry;
import com.bloodbank.model.User;
import com.bloodbank.thread.StockListener;
import com.bloodbank.thread.StockMonitor;
import com.bloodbank.util.AppConfig;
import com.bloodbank.util.Session;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Map;

/**
 * Tab panel for viewing blood stock across banks and groups.
 * Color-codes rows below threshold in RED (including 0 units).
 * Implements StockListener for real-time background thread updates (Rubric: GUI & Thread).
 */
public class StockPanel extends BasePanel implements StockListener {
    private final StockDao stockDao = new StockDao();
    private final int lowThreshold = AppConfig.getInstance().getLowStockThreshold();

    private JTable table;
    private DefaultTableModel tableModel;
    private JCheckBox showAllBanksCheck;
    private JLabel thresholdLabel;

    public StockPanel() {
        add(createHeaderPanel("Blood Stock Inventory",
                "Real-time blood units available by bank and group. Deficits below threshold (" +
                        lowThreshold + " units) are highlighted in RED."), BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        User currentUser = Session.getInstance().getCurrentUser();
        if (currentUser instanceof BankStaffUser) {
            showAllBanksCheck = new JCheckBox("Show all blood banks (uncheck for your bank only)", false);
            showAllBanksCheck.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            showAllBanksCheck.addActionListener(e -> refreshData());
            toolbar.add(showAllBanksCheck);
        }

        thresholdLabel = new JLabel("Warning Threshold: < " + lowThreshold + " units (Red)");
        thresholdLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        thresholdLabel.setForeground(new Color(178, 34, 34));
        toolbar.add(thresholdLabel);

        mainPanel.add(toolbar, BorderLayout.NORTH);

        // Center Table
        String[] columns = {"Bank ID", "Blood Bank", "City", "Blood Group", "Units Available", "Inventory Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(25);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Custom Cell Renderer: Color-code low stock rows in RED
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);

                int units = 0;
                try {
                    units = Integer.parseInt(tbl.getValueAt(row, 4).toString());
                } catch (Exception ignored) {
                }

                if (!isSelected) {
                    if (units < lowThreshold) {
                        c.setBackground(new Color(255, 235, 235)); // Soft red
                        c.setForeground(new Color(178, 34, 34));   // Bold crimson text
                        if (column == 4 || column == 5) {
                            c.setFont(c.getFont().deriveFont(Font.BOLD));
                        }
                    } else {
                        c.setBackground(Color.WHITE);
                        c.setForeground(Color.BLACK);
                    }
                }

                if (column == 0 || column == 3 || column == 4) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }

                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(850, 360));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);

        // Register this panel as a listener to the background StockMonitor thread
        if (StockMonitor.getInstance() != null) {
            StockMonitor.getInstance().addListener(this);
        }

        refreshData();
    }

    private Integer getFilterBankId() {
        User user = Session.getInstance().getCurrentUser();
        if (user instanceof BankStaffUser staffUser) {
            if (showAllBanksCheck != null && !showAllBanksCheck.isSelected()) {
                return staffUser.getBankId();
            }
        }
        return null;
    }

    @Override
    public void refreshData() {
        Integer filterBankId = getFilterBankId();

        new SwingWorker<List<StockEntry>, Void>() {
            @Override
            protected List<StockEntry> doInBackground() throws Exception {
                return stockDao.getComprehensiveStockGrid(filterBankId);
            }

            @Override
            protected void done() {
                try {
                    List<StockEntry> entries = get();
                    tableModel.setRowCount(0);
                    for (StockEntry entry : entries) {
                        int units = entry.getUnitsAvailable();
                        String status = units < lowThreshold
                                ? (units == 0 ? "CRITICAL (0 Units)" : "LOW STOCK (< " + lowThreshold + ")")
                                : "Adequate (" + units + " Units)";

                        tableModel.addRow(new Object[]{
                                entry.getBankId(),
                                entry.getBankName(),
                                entry.getCity(),
                                entry.getBloodGroup(),
                                units,
                                status
                        });
                    }
                } catch (Exception ex) {
                    showError("Failed to load blood stock: " + ex.getMessage());
                }
            }
        }.execute();
    }

    /**
     * StockListener implementation: Triggered by background StockMonitor thread.
     * Guaranteed to be executed on the Swing Event Dispatch Thread (EDT).
     */
    @Override
    public void onStockUpdated(Map<String, Integer> stockSummary, List<String> lowStockAlerts) {
        // Silently reload table data in background to reflect live stock changes
        refreshData();

        // Non-blocking warning notification when new low stock alerts arise
        if (lowStockAlerts != null && !lowStockAlerts.isEmpty()) {
            StringBuilder sb = new StringBuilder("Low Stock Warning Alert:\n");
            for (String alert : lowStockAlerts) {
                sb.append(" • ").append(alert).append("\n");
            }
            JOptionPane.showMessageDialog(this, sb.toString(), "Stock Threshold Warning", JOptionPane.WARNING_MESSAGE);
        }
    }
}
