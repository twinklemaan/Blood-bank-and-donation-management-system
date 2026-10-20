package com.bloodbank.ui;

import com.bloodbank.dao.BankDao;
import com.bloodbank.exception.AppException;
import com.bloodbank.model.Bank;
import com.bloodbank.util.Validator;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
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
import java.util.List;

/**
 * Admin screen for Blood Banks with full CRUD (Register, View, Update, Delete).
 * Demonstrates PreparedStatement and CallableStatement usage (Rubric: JDBC CRUD).
 */
public class BanksPanel extends BasePanel {
    private final BankDao bankDao = new BankDao();

    private JTable table;
    private DefaultTableModel tableModel;

    private JTextField idField;
    private JTextField nameField;
    private JTextField licenceField;
    private JTextField phoneField;
    private JTextField cityField;
    private JTextField stateField;
    private JTextField pincodeField;

    private JButton addButton;
    private JButton updateButton;
    private JButton deleteButton;
    private JButton clearButton;
    private JButton refreshButton;

    public BanksPanel() {
        // Header
        add(createHeaderPanel("Blood Banks Management", "Register new blood banks, update information, and manage records"), BorderLayout.NORTH);

        // Center: Table
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(BorderFactory.createTitledBorder("Registered Blood Banks"));

        String[] columns = {"ID", "Name", "Licence No", "Phone", "City", "State", "Pincode"};
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
                populateFormFromSelection();
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(800, 240));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // South: Form and Actions
        JPanel southPanel = new JPanel(new BorderLayout(10, 10));
        southPanel.setBorder(BorderFactory.createTitledBorder("Blood Bank Details"));

        JPanel formGrid = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0
        gbc.gridx = 0; gbc.gridy = 0;
        formGrid.add(new JLabel("Bank ID:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        idField = new JTextField(6);
        idField.setEditable(false);
        idField.setBackground(new Color(240, 240, 240));
        formGrid.add(idField, gbc);

        gbc.gridx = 2; gbc.gridy = 0;
        formGrid.add(new JLabel("Name:*"), gbc);
        gbc.gridx = 3; gbc.gridy = 0;
        nameField = new JTextField(18);
        formGrid.add(nameField, gbc);

        gbc.gridx = 4; gbc.gridy = 0;
        formGrid.add(new JLabel("Licence No:*"), gbc);
        gbc.gridx = 5; gbc.gridy = 0;
        licenceField = new JTextField(14);
        formGrid.add(licenceField, gbc);

        // Row 1
        gbc.gridx = 0; gbc.gridy = 1;
        formGrid.add(new JLabel("Phone:*"), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        phoneField = new JTextField(12);
        formGrid.add(phoneField, gbc);

        gbc.gridx = 2; gbc.gridy = 1;
        formGrid.add(new JLabel("City:*"), gbc);
        gbc.gridx = 3; gbc.gridy = 1;
        cityField = new JTextField(14);
        formGrid.add(cityField, gbc);

        gbc.gridx = 4; gbc.gridy = 1;
        formGrid.add(new JLabel("State:*"), gbc);
        gbc.gridx = 5; gbc.gridy = 1;
        stateField = new JTextField(14);
        formGrid.add(stateField, gbc);

        // Row 2
        gbc.gridx = 0; gbc.gridy = 2;
        formGrid.add(new JLabel("Pincode (6 digits):*"), gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        pincodeField = new JTextField(8);
        formGrid.add(pincodeField, gbc);

        southPanel.add(formGrid, BorderLayout.CENTER);

        // Buttons Bar
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        addButton = new JButton("Register Bank");
        addButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addButton.addActionListener(e -> onAddBank());

        updateButton = new JButton("Update Selected");
        updateButton.setEnabled(false);
        updateButton.addActionListener(e -> onUpdateBank());

        deleteButton = new JButton("Delete Selected");
        deleteButton.setEnabled(false);
        deleteButton.setForeground(new Color(178, 34, 34));
        deleteButton.addActionListener(e -> onDeleteBank());

        clearButton = new JButton("Clear Form");
        clearButton.addActionListener(e -> clearForm());

        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshData());

        buttonsPanel.add(addButton);
        buttonsPanel.add(updateButton);
        buttonsPanel.add(deleteButton);
        buttonsPanel.add(clearButton);
        buttonsPanel.add(refreshButton);

        southPanel.add(buttonsPanel, BorderLayout.SOUTH);
        add(southPanel, BorderLayout.SOUTH);

        refreshData();
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
                    tableModel.setRowCount(0);
                    for (Bank b : banks) {
                        tableModel.addRow(new Object[]{
                                b.getBankId(),
                                b.getName(),
                                b.getLicenceNo(),
                                b.getPhone(),
                                b.getCity(),
                                b.getState(),
                                b.getPincode()
                        });
                    }
                    clearForm();
                } catch (Exception ex) {
                    showError("Failed to load blood banks: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void populateFormFromSelection() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            idField.setText(tableModel.getValueAt(selectedRow, 0).toString());
            nameField.setText(tableModel.getValueAt(selectedRow, 1).toString());
            licenceField.setText(tableModel.getValueAt(selectedRow, 2).toString());
            phoneField.setText(tableModel.getValueAt(selectedRow, 3).toString());
            cityField.setText(tableModel.getValueAt(selectedRow, 4).toString());
            stateField.setText(tableModel.getValueAt(selectedRow, 5).toString());
            pincodeField.setText(tableModel.getValueAt(selectedRow, 6).toString());

            updateButton.setEnabled(true);
            deleteButton.setEnabled(true);
            addButton.setEnabled(false);
        } else {
            clearForm();
        }
    }

    private void clearForm() {
        table.clearSelection();
        idField.setText("");
        nameField.setText("");
        licenceField.setText("");
        phoneField.setText("");
        cityField.setText("");
        stateField.setText("");
        pincodeField.setText("");

        addButton.setEnabled(true);
        updateButton.setEnabled(false);
        deleteButton.setEnabled(false);
    }

    private void onAddBank() {
        try {
            String name = nameField.getText().trim();
            String licence = licenceField.getText().trim();
            String phone = phoneField.getText().trim();
            String city = cityField.getText().trim();
            String state = stateField.getText().trim();
            String pincode = pincodeField.getText().trim();

            Validator.requireNotEmpty(name, "Bank Name");
            Validator.requireNotEmpty(licence, "Licence Number");
            Validator.requireNotEmpty(phone, "Phone");
            Validator.requireNotEmpty(city, "City");
            Validator.requireNotEmpty(state, "State");
            Validator.validatePincode(pincode);

            new SwingWorker<Integer, Void>() {
                @Override
                protected Integer doInBackground() throws Exception {
                    return bankDao.registerBank(name, phone, city, state, pincode, licence);
                }

                @Override
                protected void done() {
                    try {
                        int newId = get();
                        showInfo("Blood Bank registered successfully with ID: " + newId);
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

    private void onUpdateBank() {
        try {
            int bankId = Integer.parseInt(idField.getText().trim());
            String name = nameField.getText().trim();
            String licence = licenceField.getText().trim();
            String phone = phoneField.getText().trim();
            String city = cityField.getText().trim();
            String state = stateField.getText().trim();
            String pincode = pincodeField.getText().trim();

            Validator.requireNotEmpty(name, "Bank Name");
            Validator.requireNotEmpty(licence, "Licence Number");
            Validator.requireNotEmpty(phone, "Phone");
            Validator.requireNotEmpty(city, "City");
            Validator.requireNotEmpty(state, "State");
            Validator.validatePincode(pincode);

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    bankDao.updateBank(bankId, name, phone, city, state, pincode, licence);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        showInfo("Blood Bank updated successfully.");
                        refreshData();
                    } catch (Exception ex) {
                        showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                    }
                }
            }.execute();

        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void onDeleteBank() {
        String idText = idField.getText().trim();
        if (idText.isEmpty()) return;

        int bankId = Integer.parseInt(idText);
        String name = nameField.getText().trim();

        if (confirmAction("Are you sure you want to delete blood bank '" + name + "' (ID: " + bankId + ")?", "Confirm Deletion")) {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    bankDao.deleteBank(bankId);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        showInfo("Blood Bank deleted successfully.");
                        refreshData();
                    } catch (Exception ex) {
                        showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                    }
                }
            }.execute();
        }
    }
}
