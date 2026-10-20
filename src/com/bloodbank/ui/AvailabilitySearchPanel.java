package com.bloodbank.ui;

import com.bloodbank.dao.StockDao;
import com.bloodbank.model.StockEntry;
import com.bloodbank.util.AppConfig;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
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

/**
 * Screen for Hospitals to search blood availability across banks by blood group and city.
 * Highlights low-stock banks in RED (Rubric: GUI).
 */
public class AvailabilitySearchPanel extends BasePanel {
    private final StockDao stockDao = new StockDao();
    private final int lowThreshold = AppConfig.getInstance().getLowStockThreshold();

    private JComboBox<String> groupCombo;
    private JTextField cityField;

    private JTable table;
    private DefaultTableModel tableModel;

    public AvailabilitySearchPanel() {
        add(createHeaderPanel("Blood Availability Search",
                "Explore available units across all registered blood banks filtered by blood group and city."), BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));

        // Filter Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        filterBar.setBorder(BorderFactory.createTitledBorder("Search Criteria"));

        filterBar.add(new JLabel("Blood Group:"));
        groupCombo = new JComboBox<>(new String[]{"All", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"});
        filterBar.add(groupCombo);

        filterBar.add(new JLabel("City:"));
        cityField = new JTextField(12);
        filterBar.add(cityField);

        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> refreshData());
        filterBar.add(searchBtn);

        JButton resetBtn = new JButton("Reset");
        resetBtn.addActionListener(e -> {
            groupCombo.setSelectedIndex(0);
            cityField.setText("");
            refreshData();
        });
        filterBar.add(resetBtn);

        mainPanel.add(filterBar, BorderLayout.NORTH);

        // Results Table
        String[] columns = {"Bank ID", "Blood Bank Name", "City", "Blood Group", "Units Available", "Stock Status"};
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

        // Color render: red for units < threshold
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
                        c.setBackground(new Color(255, 235, 235));
                        c.setForeground(new Color(178, 34, 34));
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

        refreshData();
    }

    @Override
    public void refreshData() {
        String selectedGroup = (String) groupCombo.getSelectedItem();
        String cityQuery = cityField.getText().trim().toLowerCase();

        new SwingWorker<List<StockEntry>, Void>() {
            @Override
            protected List<StockEntry> doInBackground() throws Exception {
                // Get all banks comprehensive grid
                List<StockEntry> list = stockDao.getComprehensiveStockGrid(null);

                // Apply client-side filters
                return list.stream().filter(entry -> {
                    boolean groupMatch = "All".equalsIgnoreCase(selectedGroup) || entry.getBloodGroup().equalsIgnoreCase(selectedGroup);
                    boolean cityMatch = cityQuery.isEmpty() || entry.getCity().toLowerCase().contains(cityQuery);
                    return groupMatch && cityMatch;
                }).toList();
            }

            @Override
            protected void done() {
                try {
                    List<StockEntry> list = get();
                    tableModel.setRowCount(0);
                    for (StockEntry entry : list) {
                        int units = entry.getUnitsAvailable();
                        String status = units < lowThreshold
                                ? (units == 0 ? "Depleted (0 Units)" : "Low Stock (< " + lowThreshold + ")")
                                : "Available (" + units + " Units)";

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
                    showError("Failed to search blood stock: " + ex.getMessage());
                }
            }
        }.execute();
    }
}
