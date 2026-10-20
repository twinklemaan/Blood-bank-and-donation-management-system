package com.bloodbank.ui;

import com.bloodbank.dao.BankDao;
import com.bloodbank.dao.DonationDao;
import com.bloodbank.model.Bank;
import com.bloodbank.model.Donation;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
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

/**
 * Screen for System Administrators to audit all donation records across banks with filtering.
 * Read-only table view (Rubric: GUI & JDBC CRUD).
 */
public class AllDonationsPanel extends BasePanel {
    private final DonationDao donationDao = new DonationDao();
    private final BankDao bankDao = new BankDao();

    private JComboBox<String> statusCombo;
    private JComboBox<BankFilterItem> bankCombo;
    private JTable table;
    private DefaultTableModel tableModel;

    private static class BankFilterItem {
        private final Integer id;
        private final String name;

        public BankFilterItem(Integer id, String name) {
            this.id = id;
            this.name = name;
        }

        public Integer getId() {
            return id;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public AllDonationsPanel() {
        add(createHeaderPanel("All Donations Audit",
                "Administrative overview of blood donations across all network blood banks with lifecycle tracking."), BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));

        // Filter Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        filterBar.setBorder(BorderFactory.createTitledBorder("Audit Filters"));

        filterBar.add(new JLabel("Screening Status:"));
        statusCombo = new JComboBox<>(new String[]{"All", "Pending", "Pass", "Fail"});
        filterBar.add(statusCombo);

        filterBar.add(new JLabel("Blood Bank:"));
        bankCombo = new JComboBox<>();
        filterBar.add(bankCombo);

        JButton filterBtn = new JButton("Apply Filters");
        filterBtn.addActionListener(e -> loadDonationsData());
        filterBar.add(filterBtn);

        JButton resetBtn = new JButton("Reset");
        resetBtn.addActionListener(e -> {
            statusCombo.setSelectedIndex(0);
            if (bankCombo.getItemCount() > 0) bankCombo.setSelectedIndex(0);
            loadDonationsData();
        });
        filterBar.add(resetBtn);

        mainPanel.add(filterBar, BorderLayout.NORTH);

        // Center Table
        String[] columns = {"ID", "Donor ID", "Donor Name", "Blood Bank", "Group", "Date", "Expiry", "Units", "Remaining", "Hb (g/dL)", "Screening"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);

                String status = tbl.getValueAt(row, 10) != null ? tbl.getValueAt(row, 10).toString() : "";
                if (!isSelected && column == 10) {
                    switch (status) {
                        case "Pass" -> c.setForeground(new Color(0, 130, 0));
                        case "Fail" -> c.setForeground(new Color(180, 0, 0));
                        case "Pending" -> c.setForeground(new Color(180, 100, 0));
                        default -> c.setForeground(Color.BLACK);
                    }
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } else if (!isSelected) {
                    c.setForeground(Color.BLACK);
                }

                if (column == 0 || column == 1 || column == 4 || column == 7 || column == 8 || column == 9 || column == 10) {
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
        // Load bank filters first, then load donations
        new SwingWorker<List<Bank>, Void>() {
            @Override
            protected List<Bank> doInBackground() throws Exception {
                return bankDao.getAllBanks();
            }

            @Override
            protected void done() {
                try {
                    List<Bank> banks = get();
                    DefaultComboBoxModel<BankFilterItem> model = new DefaultComboBoxModel<>();
                    model.addElement(new BankFilterItem(null, "All Banks"));
                    for (Bank b : banks) {
                        model.addElement(new BankFilterItem(b.getBankId(), b.getName()));
                    }
                    bankCombo.setModel(model);
                    loadDonationsData();
                } catch (Exception ex) {
                    showError("Failed to load bank filter: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void loadDonationsData() {
        String status = (String) statusCombo.getSelectedItem();
        BankFilterItem selectedBank = (BankFilterItem) bankCombo.getSelectedItem();
        Integer bankId = selectedBank != null ? selectedBank.getId() : null;

        new SwingWorker<List<Donation>, Void>() {
            @Override
            protected List<Donation> doInBackground() throws Exception {
                return donationDao.getAllDonations(status, bankId);
            }

            @Override
            protected void done() {
                try {
                    List<Donation> list = get();
                    tableModel.setRowCount(0);
                    for (Donation don : list) {
                        tableModel.addRow(new Object[]{
                                don.getDonationId(),
                                don.getDonorId(),
                                don.getDonorName(),
                                don.getBankName(),
                                don.getBloodGroup(),
                                don.getDonationDate(),
                                don.getExpiryDate(),
                                don.getUnits(),
                                don.getUnitsRemaining(),
                                don.getHemoglobinLevel(),
                                don.getScreeningResult()
                        });
                    }
                } catch (Exception ex) {
                    showError("Failed to load donations: " + ex.getMessage());
                }
            }
        }.execute();
    }
}
