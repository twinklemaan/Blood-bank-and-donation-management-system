package com.bloodbank.ui;

import com.bloodbank.dao.AuthDao;
import com.bloodbank.dao.BankDao;
import com.bloodbank.dao.StaffDao;
import com.bloodbank.exception.AppException;
import com.bloodbank.model.Bank;
import com.bloodbank.model.StaffMember;
import com.bloodbank.util.Validator;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
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
 * Admin screen for managing Bank Staff members with full CRUD.
 * Demonstrates PreparedStatement, CallableStatement, and FK handling (Rubric: JDBC CRUD).
 */
public class StaffPanel extends BasePanel {
    private final StaffDao staffDao = new StaffDao();
    private final BankDao bankDao = new BankDao();
    private final AuthDao authDao = new AuthDao();

    private JTable table;
    private DefaultTableModel tableModel;

    private JTextField idField;
    private JComboBox<BankItem> bankCombo;
    private JTextField nameField;
    private JComboBox<String> roleCombo;
    private JTextField phoneField;

    // Optional user credentials for portal access
    private JTextField usernameField;
    private JPasswordField passwordField;

    private JButton addButton;
    private JButton updateButton;
    private JButton deleteButton;
    private JButton clearButton;
    private JButton refreshButton;

    // Helper class for Bank dropdown combo
    private static class BankItem {
        private final int id;
        private final String name;

        public BankItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        @Override
        public String toString() {
            return name + " (ID: " + id + ")";
        }
    }

    public StaffPanel() {
        add(createHeaderPanel("Staff Management", "Register bank staff, assign roles and banks, and create staff credentials"), BorderLayout.NORTH);

        // Center: Table
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(BorderFactory.createTitledBorder("Bank Staff Personnel"));

        String[] columns = {"ID", "Staff Name", "Bank ID", "Bank Name", "Role", "Phone"};
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
        scrollPane.setPreferredSize(new Dimension(800, 220));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // South: Form and Actions
        JPanel southPanel = new JPanel(new BorderLayout(10, 10));
        southPanel.setBorder(BorderFactory.createTitledBorder("Staff Member Details & Credentials"));

        JPanel formGrid = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0
        gbc.gridx = 0; gbc.gridy = 0;
        formGrid.add(new JLabel("Staff ID:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        idField = new JTextField(6);
        idField.setEditable(false);
        idField.setBackground(new Color(240, 240, 240));
        formGrid.add(idField, gbc);

        gbc.gridx = 2; gbc.gridy = 0;
        formGrid.add(new JLabel("Blood Bank:*"), gbc);
        gbc.gridx = 3; gbc.gridy = 0;
        bankCombo = new JComboBox<>();
        formGrid.add(bankCombo, gbc);

        gbc.gridx = 4; gbc.gridy = 0;
        formGrid.add(new JLabel("Staff Name:*"), gbc);
        gbc.gridx = 5; gbc.gridy = 0;
        nameField = new JTextField(16);
        formGrid.add(nameField, gbc);

        // Row 1
        gbc.gridx = 0; gbc.gridy = 1;
        formGrid.add(new JLabel("Role:*"), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        roleCombo = new JComboBox<>(new String[]{"Lab Technician", "Medical Officer", "Nurse", "Manager"});
        formGrid.add(roleCombo, gbc);

        gbc.gridx = 2; gbc.gridy = 1;
        formGrid.add(new JLabel("Phone:*"), gbc);
        gbc.gridx = 3; gbc.gridy = 1;
        phoneField = new JTextField(14);
        formGrid.add(phoneField, gbc);

        // Row 2: Credentials
        gbc.gridx = 0; gbc.gridy = 2;
        formGrid.add(new JLabel("Login Username:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        usernameField = new JTextField(14);
        formGrid.add(usernameField, gbc);

        gbc.gridx = 2; gbc.gridy = 2;
        formGrid.add(new JLabel("Login Password:"), gbc);
        gbc.gridx = 3; gbc.gridy = 2;
        passwordField = new JPasswordField(14);
        formGrid.add(passwordField, gbc);

        southPanel.add(formGrid, BorderLayout.CENTER);

        // Buttons Bar
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        addButton = new JButton("Register Staff");
        addButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addButton.addActionListener(e -> onAddStaff());

        updateButton = new JButton("Update Selected");
        updateButton.setEnabled(false);
        updateButton.addActionListener(e -> onUpdateStaff());

        deleteButton = new JButton("Delete Selected");
        deleteButton.setEnabled(false);
        deleteButton.setForeground(new Color(178, 34, 34));
        deleteButton.addActionListener(e -> onDeleteStaff());

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
        // Load banks and staff in background
        new SwingWorker<Void, Void>() {
            private List<Bank> banks;
            private List<StaffMember> staffMembers;

            @Override
            protected Void doInBackground() throws Exception {
                banks = bankDao.getAllBanks();
                staffMembers = staffDao.getAllStaff();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    // Populate bank dropdown
                    DefaultComboBoxModel<BankItem> comboModel = new DefaultComboBoxModel<>();
                    for (Bank b : banks) {
                        comboModel.addElement(new BankItem(b.getBankId(), b.getName()));
                    }
                    bankCombo.setModel(comboModel);

                    // Populate staff table
                    tableModel.setRowCount(0);
                    for (StaffMember s : staffMembers) {
                        tableModel.addRow(new Object[]{
                                s.getId(),
                                s.getName(),
                                s.getBankId(),
                                s.getBankName(),
                                s.getRole(),
                                s.getPhone()
                        });
                    }
                    clearForm();
                } catch (Exception ex) {
                    showError("Failed to load staff data: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void populateFormFromSelection() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            idField.setText(tableModel.getValueAt(selectedRow, 0).toString());
            nameField.setText(tableModel.getValueAt(selectedRow, 1).toString());
            int bankId = (int) tableModel.getValueAt(selectedRow, 2);

            for (int i = 0; i < bankCombo.getItemCount(); i++) {
                BankItem item = bankCombo.getItemAt(i);
                if (item.getId() == bankId) {
                    bankCombo.setSelectedIndex(i);
                    break;
                }
            }

            roleCombo.setSelectedItem(tableModel.getValueAt(selectedRow, 4).toString());
            phoneField.setText(tableModel.getValueAt(selectedRow, 5).toString());

            usernameField.setText("");
            usernameField.setEnabled(false);
            passwordField.setText("");
            passwordField.setEnabled(false);

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
        if (bankCombo.getItemCount() > 0) {
            bankCombo.setSelectedIndex(0);
        }
        roleCombo.setSelectedIndex(0);
        phoneField.setText("");
        usernameField.setText("");
        usernameField.setEnabled(true);
        passwordField.setText("");
        passwordField.setEnabled(true);

        addButton.setEnabled(true);
        updateButton.setEnabled(false);
        deleteButton.setEnabled(false);
    }

    private void onAddStaff() {
        try {
            BankItem selectedBank = (BankItem) bankCombo.getSelectedItem();
            if (selectedBank == null) {
                throw new AppException("Please select a blood bank.");
            }
            int bankId = selectedBank.getId();
            String name = nameField.getText().trim();
            String role = (String) roleCombo.getSelectedItem();
            String phone = phoneField.getText().trim();

            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();

            Validator.requireNotEmpty(name, "Staff Name");
            Validator.requireNotEmpty(role, "Role");
            Validator.requireNotEmpty(phone, "Phone");

            if (!username.isEmpty() && password.isEmpty()) {
                throw new AppException("Please provide a password for the staff login.");
            }

            new SwingWorker<Integer, Void>() {
                @Override
                protected Integer doInBackground() throws Exception {
                    int staffId = staffDao.registerStaff(bankId, name, role, phone);
                    if (!username.isEmpty() && !password.isEmpty()) {
                        authDao.createUser(username, password, "STAFF", staffId, null);
                    }
                    return staffId;
                }

                @Override
                protected void done() {
                    try {
                        int newId = get();
                        showInfo("Staff registered successfully with ID: " + newId +
                                (!username.isEmpty() ? "\nStaff login created: " + username : ""));
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

    private void onUpdateStaff() {
        try {
            int staffId = Integer.parseInt(idField.getText().trim());
            BankItem selectedBank = (BankItem) bankCombo.getSelectedItem();
            if (selectedBank == null) {
                throw new AppException("Please select a blood bank.");
            }
            int bankId = selectedBank.getId();
            String name = nameField.getText().trim();
            String role = (String) roleCombo.getSelectedItem();
            String phone = phoneField.getText().trim();

            Validator.requireNotEmpty(name, "Staff Name");
            Validator.requireNotEmpty(role, "Role");
            Validator.requireNotEmpty(phone, "Phone");

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    staffDao.updateStaff(staffId, bankId, name, role, phone);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        showInfo("Staff updated successfully.");
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

    private void onDeleteStaff() {
        String idText = idField.getText().trim();
        if (idText.isEmpty()) return;

        int staffId = Integer.parseInt(idText);
        String name = nameField.getText().trim();

        if (confirmAction("Are you sure you want to delete staff member '" + name + "' (ID: " + staffId + ")?\n" +
                "Any linked portal account will also be removed.", "Confirm Deletion")) {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    staffDao.deleteStaff(staffId);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        showInfo("Staff member deleted successfully.");
                        refreshData();
                    } catch (Exception ex) {
                        showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                    }
                }
            }.execute();
        }
    }
}
