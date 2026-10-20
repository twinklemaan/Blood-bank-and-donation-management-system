package com.bloodbank.ui;

import com.bloodbank.dao.BankDao;
import com.bloodbank.dao.RequestDao;
import com.bloodbank.exception.AppException;
import com.bloodbank.model.Bank;
import com.bloodbank.model.HospitalUser;
import com.bloodbank.model.User;
import com.bloodbank.util.Session;
import com.bloodbank.util.Validator;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

/**
 * Screen for Hospitals to raise new blood supply requests.
 * Calls stored procedure sp_raise_request via RequestDao in a SwingWorker (Rubric: GUI & Thread).
 */
public class RaiseRequestPanel extends BasePanel {
    private final RequestDao requestDao = new RequestDao();
    private final BankDao bankDao = new BankDao();

    private JComboBox<BankItem> bankCombo;
    private JComboBox<String> groupCombo;
    private JSpinner unitsSpinner;
    private JComboBox<String> urgencyCombo;
    private JButton submitButton;

    private static class BankItem {
        private final int id;
        private final String name;
        private final String city;

        public BankItem(int id, String name, String city) {
            this.id = id;
            this.name = name;
            this.city = city;
        }

        public int getId() {
            return id;
        }

        @Override
        public String toString() {
            return name + " (" + city + ", ID: " + id + ")";
        }
    }

    public RaiseRequestPanel() {
        add(createHeaderPanel("Raise Blood Supply Request",
                "Submit hospital blood requirements to authorized blood banks with urgency classification."), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("Request Details"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 12, 10, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Bank Selection
        gbc.gridx = 0; gbc.gridy = 0;
        centerPanel.add(new JLabel("Target Blood Bank:*"), gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        bankCombo = new JComboBox<>();
        bankCombo.setPreferredSize(new Dimension(280, 28));
        centerPanel.add(bankCombo, gbc);

        // Row 1: Blood Group
        gbc.gridx = 0; gbc.gridy = 1;
        centerPanel.add(new JLabel("Blood Group Required:*"), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        groupCombo = new JComboBox<>(new String[]{"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"});
        centerPanel.add(groupCombo, gbc);

        // Row 2: Units Requested
        gbc.gridx = 0; gbc.gridy = 2;
        centerPanel.add(new JLabel("Units Requested:*"), gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        unitsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 50, 1));
        centerPanel.add(unitsSpinner, gbc);

        // Row 3: Urgency Level
        gbc.gridx = 0; gbc.gridy = 3;
        centerPanel.add(new JLabel("Urgency Priority:*"), gbc);
        gbc.gridx = 1; gbc.gridy = 3;
        urgencyCombo = new JComboBox<>(new String[]{"Normal", "Urgent", "Critical"});
        centerPanel.add(urgencyCombo, gbc);

        // Row 4: Submit Button
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 10));
        submitButton = new JButton("Submit Blood Request");
        submitButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        submitButton.addActionListener(e -> onSubmitRequest());
        btnPanel.add(submitButton);
        centerPanel.add(btnPanel, gbc);

        add(centerPanel, BorderLayout.CENTER);

        refreshData();
    }

    private int getHospitalId() {
        User user = Session.getInstance().getCurrentUser();
        if (user instanceof HospitalUser hospUser) {
            return hospUser.getHospitalId();
        }
        return 1;
    }

    @Override
    public void refreshData() {
        new SwingWorker<List<Bank>, Void>() {
            @Override
            protected List<Bank> doInBackground() throws Exception {
                return bankDao.getAllBanks();
            }

            @Override
            protected void done() {
                try {
                    List<Bank> banks = get();
                    DefaultComboBoxModel<BankItem> model = new DefaultComboBoxModel<>();
                    for (Bank b : banks) {
                        model.addElement(new BankItem(b.getBankId(), b.getName(), b.getCity()));
                    }
                    bankCombo.setModel(model);
                } catch (Exception ex) {
                    showError("Failed to load blood banks: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void onSubmitRequest() {
        BankItem selectedBank = (BankItem) bankCombo.getSelectedItem();
        if (selectedBank == null) {
            showWarning("Please select a target blood bank.");
            return;
        }

        int hospitalId = getHospitalId();
        int bankId = selectedBank.getId();
        String bloodGroup = (String) groupCombo.getSelectedItem();
        int units = (Integer) unitsSpinner.getValue();
        String urgency = (String) urgencyCombo.getSelectedItem();

        submitButton.setEnabled(false);
        submitButton.setText("Submitting...");

        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                return requestDao.raiseRequest(hospitalId, bankId, bloodGroup, units, urgency);
            }

            @Override
            protected void done() {
                submitButton.setEnabled(true);
                submitButton.setText("Submit Blood Request");
                try {
                    int requestId = get();
                    showInfo("Blood request successfully submitted!\nRequest ID: #" + requestId +
                            "\nStatus: Pending. Bank staff will review and fulfil this request.");
                    unitsSpinner.setValue(1);
                    urgencyCombo.setSelectedIndex(0);
                } catch (Exception ex) {
                    showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                }
            }
        }.execute();
    }
}
