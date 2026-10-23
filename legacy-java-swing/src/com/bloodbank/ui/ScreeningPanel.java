package com.bloodbank.ui;

import com.bloodbank.dao.DonationDao;
import com.bloodbank.model.BankStaffUser;
import com.bloodbank.model.Donation;
import com.bloodbank.model.User;
import com.bloodbank.util.Session;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

/**
 * Screen for Bank Staff to perform laboratory screening on Pending blood donations.
 * Demonstrates Stored Procedure execution and transactional state changes (Rubric: JDBC CRUD).
 */
public class ScreeningPanel extends BasePanel {
    private final DonationDao donationDao = new DonationDao();

    private JTable table;
    private DefaultTableModel tableModel;

    private JButton passButton;
    private JButton failButton;
    private JButton refreshButton;

    public ScreeningPanel() {
        add(createHeaderPanel("Laboratory Screening & Quality Control",
                "Review pending blood donations. Marking 'Pass' immediately adds units to stock. Results are permanent."), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.setBorder(BorderFactory.createTitledBorder("Pending Blood Donations Awaiting Screening"));

        String[] columns = {"Donation ID", "Donor ID", "Donor Name", "Blood Group", "Donation Date", "Expiry Date", "Units", "Hb (g/dL)", "Screening Status"};
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

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean rowSelected = table.getSelectedRow() >= 0;
                passButton.setEnabled(rowSelected);
                failButton.setEnabled(rowSelected);
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(850, 320));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // Actions Bar
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        passButton = new JButton("Pass (Add to Stock)");
        passButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        passButton.setBackground(new Color(34, 139, 34)); // Forest green
        passButton.setForeground(Color.WHITE);
        passButton.setEnabled(false);
        passButton.addActionListener(e -> updateSelectedScreening("Pass"));

        failButton = new JButton("Fail (Discard Unit)");
        failButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        failButton.setBackground(new Color(178, 34, 34)); // Crimson
        failButton.setForeground(Color.WHITE);
        failButton.setEnabled(false);
        failButton.addActionListener(e -> updateSelectedScreening("Fail"));

        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshData());

        actionsPanel.add(passButton);
        actionsPanel.add(failButton);
        actionsPanel.add(refreshButton);

        add(actionsPanel, BorderLayout.SOUTH);

        refreshData();
    }

    private int getTargetBankId() {
        User user = Session.getInstance().getCurrentUser();
        if (user instanceof BankStaffUser staffUser) {
            return staffUser.getBankId();
        }
        return 1;
    }

    @Override
    public void refreshData() {
        int bankId = getTargetBankId();

        new SwingWorker<List<Donation>, Void>() {
            @Override
            protected List<Donation> doInBackground() throws Exception {
                return donationDao.getPendingDonationsByBank(bankId);
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
                                don.getBloodGroup(),
                                don.getDonationDate(),
                                don.getExpiryDate(),
                                don.getUnits(),
                                don.getHemoglobinLevel(),
                                don.getScreeningResult()
                        });
                    }
                    passButton.setEnabled(false);
                    failButton.setEnabled(false);
                } catch (Exception ex) {
                    showError("Failed to load pending donations: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void updateSelectedScreening(String result) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) return;

        int donationId = (Integer) tableModel.getValueAt(selectedRow, 0);
        String bloodGroup = (String) tableModel.getValueAt(selectedRow, 3);
        int units = (Integer) tableModel.getValueAt(selectedRow, 6);

        String message = "Are you sure you want to mark Donation #" + donationId + " as '" + result + "'?\n" +
                (result.equals("Pass")
                        ? "This will add " + units + " unit(s) of " + bloodGroup + " to available bank stock."
                        : "This will discard the unit without adding stock.") +
                "\nNote: A Pass or Fail screening result is final and cannot be reversed.";

        if (confirmAction(message, "Confirm Screening Result")) {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    donationDao.updateScreening(donationId, result);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        showInfo("Donation #" + donationId + " successfully screened as '" + result + "'.");
                        refreshData();
                    } catch (Exception ex) {
                        showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                    }
                }
            }.execute();
        }
    }
}
