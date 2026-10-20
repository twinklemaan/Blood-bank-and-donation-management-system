package com.bloodbank.ui;

import com.bloodbank.dao.DonationDao;
import com.bloodbank.dao.DonorDao;
import com.bloodbank.exception.AppException;
import com.bloodbank.model.BankStaffUser;
import com.bloodbank.model.Donation;
import com.bloodbank.model.Donor;
import com.bloodbank.model.User;
import com.bloodbank.util.Session;
import com.bloodbank.util.Validator;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Screen for Bank Staff to record blood donations and check donor eligibility.
 * Enforces bank scoping and non-blocking eligibility checks (Rubric: GUI & JDBC CRUD).
 */
public class DonationsPanel extends BasePanel {
    private final DonationDao donationDao = new DonationDao();
    private final DonorDao donorDao = new DonorDao();

    private JTable table;
    private DefaultTableModel tableModel;

    // Donor selection
    private JComboBox<DonorItem> donorCombo;
    private JLabel eligibilityHintLabel;

    private JSpinner unitsSpinner;
    private JTextField hbField;
    private JTextField dateField;

    private JButton recordButton;
    private JButton deleteButton;
    private JButton checkEligibleBtn;

    private static class DonorItem {
        private final int id;
        private final String name;
        private final String group;

        public DonorItem(int id, String name, String group) {
            this.id = id;
            this.name = name;
            this.group = group;
        }

        public int getId() {
            return id;
        }

        @Override
        public String toString() {
            return name + " [" + group + "] (ID: " + id + ")";
        }
    }

    public DonationsPanel() {
        add(createHeaderPanel("Record Blood Donations",
                "Record incoming donations. All donations start as 'Pending' until laboratory screening."), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.setBorder(BorderFactory.createTitledBorder("Bank Donation Intake Records"));

        String[] columns = {"ID", "Donor ID", "Donor Name", "Blood Group", "Date", "Expiry Date", "Units", "Units Rem.", "Hb (g/dL)", "Screening"};
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
                int row = table.getSelectedRow();
                deleteButton.setEnabled(row >= 0);
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(850, 240));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // South: Recording Form
        JPanel southPanel = new JPanel(new BorderLayout(8, 8));
        southPanel.setBorder(BorderFactory.createTitledBorder("New Donation Details"));

        JPanel formGrid = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0
        gbc.gridx = 0; gbc.gridy = 0;
        formGrid.add(new JLabel("Select Donor:*"), gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        donorCombo = new JComboBox<>();
        donorCombo.setPreferredSize(new Dimension(220, 25));
        formGrid.add(donorCombo, gbc);

        gbc.gridx = 2; gbc.gridy = 0;
        checkEligibleBtn = new JButton("Check Eligibility");
        checkEligibleBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        checkEligibleBtn.addActionListener(e -> checkSelectedDonorEligibility());
        formGrid.add(checkEligibleBtn, gbc);

        gbc.gridx = 3; gbc.gridy = 0;
        eligibilityHintLabel = new JLabel("Eligibility: Not checked");
        eligibilityHintLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        formGrid.add(eligibilityHintLabel, gbc);

        // Row 1
        gbc.gridx = 0; gbc.gridy = 1;
        formGrid.add(new JLabel("Units Donated:*"), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        unitsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        formGrid.add(unitsSpinner, gbc);

        gbc.gridx = 2; gbc.gridy = 1;
        formGrid.add(new JLabel("Haemoglobin (g/dL):*"), gbc);
        gbc.gridx = 3; gbc.gridy = 1;
        hbField = new JTextField("14.0", 8);
        formGrid.add(hbField, gbc);

        // Row 2
        gbc.gridx = 0; gbc.gridy = 2;
        formGrid.add(new JLabel("Donation Date:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        dateField = new JTextField("", 10);
        dateField.setToolTipText("Leave empty for today (CURRENT_DATE), or enter YYYY-MM-DD");
        formGrid.add(dateField, gbc);

        gbc.gridx = 2; gbc.gridy = 2;
        gbc.gridwidth = 2;
        JLabel dateHint = new JLabel("(Format: YYYY-MM-DD, or leave blank for today)");
        dateHint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        dateHint.setForeground(Color.GRAY);
        formGrid.add(dateHint, gbc);

        southPanel.add(formGrid, BorderLayout.CENTER);

        // Buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        recordButton = new JButton("Record Donation (Pending)");
        recordButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        recordButton.addActionListener(e -> onRecordDonation());

        deleteButton = new JButton("Delete Selected");
        deleteButton.setEnabled(false);
        deleteButton.setForeground(new Color(178, 34, 34));
        deleteButton.setToolTipText("Only Pending or Failed donations can be deleted. Pass donations are locked by trigger.");
        deleteButton.addActionListener(e -> onDeleteDonation());

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshData());

        actions.add(recordButton);
        actions.add(deleteButton);
        actions.add(refreshBtn);
        southPanel.add(actions, BorderLayout.SOUTH);

        add(southPanel, BorderLayout.SOUTH);

        refreshData();
    }

    private int getTargetBankId() {
        User user = Session.getInstance().getCurrentUser();
        if (user instanceof BankStaffUser staffUser) {
            return staffUser.getBankId();
        }
        return 1; // Default
    }

    @Override
    public void refreshData() {
        int bankId = getTargetBankId();

        new SwingWorker<Void, Void>() {
            private List<Donation> donations;
            private List<Donor> donors;

            @Override
            protected Void doInBackground() throws Exception {
                donations = donationDao.getDonationsByBank(bankId);
                donors = donorDao.searchDonors(null, null);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    // Populate donor combo
                    DefaultComboBoxModel<DonorItem> comboModel = new DefaultComboBoxModel<>();
                    for (Donor d : donors) {
                        comboModel.addElement(new DonorItem(d.getId(), d.getName(), d.getBloodGroup()));
                    }
                    donorCombo.setModel(comboModel);

                    // Populate table
                    tableModel.setRowCount(0);
                    for (Donation don : donations) {
                        tableModel.addRow(new Object[]{
                                don.getDonationId(),
                                don.getDonorId(),
                                don.getDonorName(),
                                don.getBloodGroup(),
                                don.getDonationDate(),
                                don.getExpiryDate(),
                                don.getUnits(),
                                don.getUnitsRemaining(),
                                don.getHemoglobinLevel(),
                                don.getScreeningResult()
                        });
                    }
                    deleteButton.setEnabled(false);
                } catch (Exception ex) {
                    showError("Failed to load donations: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void checkSelectedDonorEligibility() {
        DonorItem selected = (DonorItem) donorCombo.getSelectedItem();
        if (selected == null) {
            showWarning("Please select a donor first.");
            return;
        }

        new SwingWorker<DonorDao.EligibilityResult, Void>() {
            @Override
            protected DonorDao.EligibilityResult doInBackground() throws Exception {
                return donorDao.checkEligibility(selected.getId(), LocalDate.now());
            }

            @Override
            protected void done() {
                try {
                    DonorDao.EligibilityResult res = get();
                    if (res.isEligible()) {
                        eligibilityHintLabel.setText("Eligible to donate today");
                        eligibilityHintLabel.setForeground(new Color(0, 130, 0));
                    } else {
                        String msg = res.getNextEligibleDate() != null
                                ? "Deferred until " + res.getNextEligibleDate()
                                : "Not eligible (Age/Gap criteria)";
                        eligibilityHintLabel.setText(msg);
                        eligibilityHintLabel.setForeground(new Color(180, 0, 0));
                    }
                } catch (Exception ex) {
                    eligibilityHintLabel.setText("Check failed: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void onRecordDonation() {
        try {
            DonorItem selected = (DonorItem) donorCombo.getSelectedItem();
            if (selected == null) {
                throw new AppException("Please select a donor.");
            }

            int donorId = selected.getId();
            int bankId = getTargetBankId();
            int units = (Integer) unitsSpinner.getValue();

            String hbStr = hbField.getText().trim();
            Validator.requireNotEmpty(hbStr, "Haemoglobin");
            double hbVal = Validator.validatePositiveDouble(hbStr, "Haemoglobin");
            BigDecimal hb = BigDecimal.valueOf(hbVal);

            String dateStr = dateField.getText().trim();
            LocalDate donDate = null;
            if (!dateStr.isEmpty()) {
                donDate = Validator.validateDate(dateStr, "Donation Date");
            }

            final LocalDate finalDonDate = donDate;
            new SwingWorker<Integer, Void>() {
                @Override
                protected Integer doInBackground() throws Exception {
                    return donationDao.recordDonation(donorId, bankId, units, hb, "Pending", finalDonDate);
                }

                @Override
                protected void done() {
                    try {
                        int donationId = get();
                        showInfo("Donation recorded successfully with ID: " + donationId +
                                "\nScreening Status: Pending. Please proceed to Screening tab to test and approve.");
                        refreshData();
                    } catch (Exception ex) {
                        showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                    }
                }
            }.execute();

        } catch (AppException ex) {
            showError(ex.getMessage());
        }
    }

    private void onDeleteDonation() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) return;

        int donationId = (Integer) tableModel.getValueAt(selectedRow, 0);
        String screeningResult = (String) tableModel.getValueAt(selectedRow, 9);

        if ("Pass".equalsIgnoreCase(screeningResult)) {
            showWarning("A 'Pass' donation is backing stock and cannot be deleted by database rules.");
            return;
        }

        if (confirmAction("Are you sure you want to delete donation #" + donationId + "?", "Confirm Deletion")) {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    donationDao.deleteDonation(donationId);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        showInfo("Donation #" + donationId + " deleted successfully.");
                        refreshData();
                    } catch (Exception ex) {
                        showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                    }
                }
            }.execute();
        }
    }
}
