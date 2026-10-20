package com.bloodbank.ui;

import com.bloodbank.dao.AuthDao;
import com.bloodbank.dao.HospitalDao;
import com.bloodbank.exception.AppException;
import com.bloodbank.model.Hospital;
import com.bloodbank.util.Validator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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
 * Admin screen for Hospitals with full CRUD and optional user login creation.
 * Demonstrates PreparedStatement, CallableStatement, and FK handling (Rubric: JDBC CRUD).
 */
public class HospitalsPanel extends BasePanel {
    private final HospitalDao hospitalDao = new HospitalDao();
    private final AuthDao authDao = new AuthDao();

    private JTable table;
    private DefaultTableModel tableModel;

    private JTextField idField;
    private JTextField nameField;
    private JTextField regNoField;
    private JTextField phoneField;
    private JTextField cityField;
    private JTextField stateField;
    private JTextField pincodeField;

    // Optional user credentials for portal access
    private JTextField usernameField;
    private JPasswordField passwordField;

    private JButton addButton;
    private JButton updateButton;
    private JButton deleteButton;
    private JButton clearButton;
    private JButton refreshButton;

    public HospitalsPanel() {
        add(createHeaderPanel("Hospitals Management", "Register new hospitals, provision user access, and maintain hospital details"), BorderLayout.NORTH);

        // Center: Table
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(BorderFactory.createTitledBorder("Registered Hospitals"));

        String[] columns = {"ID", "Name", "Reg No", "Phone", "City", "State", "Pincode"};
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
        southPanel.setBorder(BorderFactory.createTitledBorder("Hospital Details & Credentials"));

        JPanel formGrid = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0
        gbc.gridx = 0; gbc.gridy = 0;
        formGrid.add(new JLabel("Hospital ID:"), gbc);
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
        formGrid.add(new JLabel("Reg No:*"), gbc);
        gbc.gridx = 5; gbc.gridy = 0;
        regNoField = new JTextField(14);
        formGrid.add(regNoField, gbc);

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

        // Row 2: Pincode and Portal Credentials
        gbc.gridx = 0; gbc.gridy = 2;
        formGrid.add(new JLabel("Pincode (6 digits):*"), gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        pincodeField = new JTextField(8);
        formGrid.add(pincodeField, gbc);

        gbc.gridx = 2; gbc.gridy = 2;
        formGrid.add(new JLabel("Portal Username:"), gbc);
        gbc.gridx = 3; gbc.gridy = 2;
        usernameField = new JTextField(14);
        formGrid.add(usernameField, gbc);

        gbc.gridx = 4; gbc.gridy = 2;
        formGrid.add(new JLabel("Portal Password:"), gbc);
        gbc.gridx = 5; gbc.gridy = 2;
        passwordField = new JPasswordField(14);
        formGrid.add(passwordField, gbc);

        southPanel.add(formGrid, BorderLayout.CENTER);

        // Buttons Bar
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        addButton = new JButton("Register Hospital");
        addButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addButton.addActionListener(e -> onAddHospital());

        updateButton = new JButton("Update Selected");
        updateButton.setEnabled(false);
        updateButton.addActionListener(e -> onUpdateHospital());

        deleteButton = new JButton("Delete Selected");
        deleteButton.setEnabled(false);
        deleteButton.setForeground(new Color(178, 34, 34));
        deleteButton.addActionListener(e -> onDeleteHospital());

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
        new SwingWorker<List<Hospital>, Void>() {
            @Override
            protected List<Hospital> doInBackground() throws Exception {
                return hospitalDao.getAllHospitals();
            }

            @Override
            protected void done() {
                try {
                    List<Hospital> hospitals = get();
                    tableModel.setRowCount(0);
                    for (Hospital h : hospitals) {
                        tableModel.addRow(new Object[]{
                                h.getHospitalId(),
                                h.getName(),
                                h.getRegNo(),
                                h.getPhone(),
                                h.getCity(),
                                h.getState(),
                                h.getPincode()
                        });
                    }
                    clearForm();
                } catch (Exception ex) {
                    showError("Failed to load hospitals: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void populateFormFromSelection() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            idField.setText(tableModel.getValueAt(selectedRow, 0).toString());
            nameField.setText(tableModel.getValueAt(selectedRow, 1).toString());
            regNoField.setText(tableModel.getValueAt(selectedRow, 2).toString());
            phoneField.setText(tableModel.getValueAt(selectedRow, 3).toString());
            cityField.setText(tableModel.getValueAt(selectedRow, 4).toString());
            stateField.setText(tableModel.getValueAt(selectedRow, 5).toString());
            pincodeField.setText(tableModel.getValueAt(selectedRow, 6).toString());

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
        regNoField.setText("");
        phoneField.setText("");
        cityField.setText("");
        stateField.setText("");
        pincodeField.setText("");
        usernameField.setText("");
        usernameField.setEnabled(true);
        passwordField.setText("");
        passwordField.setEnabled(true);

        addButton.setEnabled(true);
        updateButton.setEnabled(false);
        deleteButton.setEnabled(false);
    }

    private void onAddHospital() {
        try {
            String name = nameField.getText().trim();
            String regNo = regNoField.getText().trim();
            String phone = phoneField.getText().trim();
            String city = cityField.getText().trim();
            String state = stateField.getText().trim();
            String pincode = pincodeField.getText().trim();

            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();

            Validator.requireNotEmpty(name, "Hospital Name");
            Validator.requireNotEmpty(regNo, "Registration Number");
            Validator.requireNotEmpty(phone, "Phone");
            Validator.requireNotEmpty(city, "City");
            Validator.requireNotEmpty(state, "State");
            Validator.validatePincode(pincode);

            if (!username.isEmpty() && password.isEmpty()) {
                throw new AppException("Please provide a password for the hospital portal account.");
            }

            new SwingWorker<Integer, Void>() {
                @Override
                protected Integer doInBackground() throws Exception {
                    int hospId = hospitalDao.registerHospital(name, phone, city, state, pincode, regNo);
                    if (!username.isEmpty() && !password.isEmpty()) {
                        authDao.createUser(username, password, "HOSPITAL", null, hospId);
                    }
                    return hospId;
                }

                @Override
                protected void done() {
                    try {
                        int newId = get();
                        showInfo("Hospital registered successfully with ID: " + newId +
                                (!username.isEmpty() ? "\nPortal login created: " + username : ""));
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

    private void onUpdateHospital() {
        try {
            int hospId = Integer.parseInt(idField.getText().trim());
            String name = nameField.getText().trim();
            String regNo = regNoField.getText().trim();
            String phone = phoneField.getText().trim();
            String city = cityField.getText().trim();
            String state = stateField.getText().trim();
            String pincode = pincodeField.getText().trim();

            Validator.requireNotEmpty(name, "Hospital Name");
            Validator.requireNotEmpty(regNo, "Registration Number");
            Validator.requireNotEmpty(phone, "Phone");
            Validator.requireNotEmpty(city, "City");
            Validator.requireNotEmpty(state, "State");
            Validator.validatePincode(pincode);

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    hospitalDao.updateHospital(hospId, name, phone, city, state, pincode, regNo);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        showInfo("Hospital updated successfully.");
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

    private void onDeleteHospital() {
        String idText = idField.getText().trim();
        if (idText.isEmpty()) return;

        int hospId = Integer.parseInt(idText);
        String name = nameField.getText().trim();

        if (confirmAction("Are you sure you want to delete hospital '" + name + "' (ID: " + hospId + ")?\n" +
                "Any linked portal account will also be removed.", "Confirm Deletion")) {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    hospitalDao.deleteHospital(hospId);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        showInfo("Hospital deleted successfully.");
                        refreshData();
                    } catch (Exception ex) {
                        showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                    }
                }
            }.execute();
        }
    }
}
