package com.bloodbank.ui;

import com.bloodbank.dao.DonorDao;
import com.bloodbank.exception.AppException;
import com.bloodbank.model.Donor;
import com.bloodbank.util.Validator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
import java.time.LocalDate;
import java.util.List;

/**
 * Screen for Bank Staff to register donors and search existing donors with eligibility status.
 * Demonstrates PreparedStatement, CallableStatement, and Custom Exceptions (Rubric: JDBC CRUD).
 */
public class DonorsPanel extends BasePanel {
    private final DonorDao donorDao = new DonorDao();

    private JTable table;
    private DefaultTableModel tableModel;

    // Search filters
    private JComboBox<String> filterGroupCombo;
    private JTextField filterCityField;

    // Registration Form Fields
    private JTextField nameField;
    private JComboBox<String> genderCombo;
    private JTextField dobField;
    private JComboBox<String> bloodGroupCombo;
    private JTextField cityField;
    private JTextField stateField;
    private JTextField pincodeField;
    private JTextField phoneField;

    public DonorsPanel() {
        add(createHeaderPanel("Donor Registration & Eligibility Directory",
                "Register new donors and review donation eligibility dates and age constraints"), BorderLayout.NORTH);

        JPanel mainContent = new JPanel(new BorderLayout(8, 8));

        // Filter / Search Toolbar
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchBar.setBorder(BorderFactory.createTitledBorder("Search & Filter Donors"));

        searchBar.add(new JLabel("Blood Group:"));
        filterGroupCombo = new JComboBox<>(new String[]{"All", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"});
        searchBar.add(filterGroupCombo);

        searchBar.add(new JLabel("City:"));
        filterCityField = new JTextField(12);
        searchBar.add(filterCityField);

        JButton searchBtn = new JButton("Filter");
        searchBtn.addActionListener(e -> refreshData());
        searchBar.add(searchBtn);

        JButton resetBtn = new JButton("Reset");
        resetBtn.addActionListener(e -> {
            filterGroupCombo.setSelectedIndex(0);
            filterCityField.setText("");
            refreshData();
        });
        searchBar.add(resetBtn);

        mainContent.add(searchBar, BorderLayout.NORTH);

        // Center: Donors Table
        String[] columns = {"ID", "Name", "Gender", "DOB", "Blood Group", "Age", "City", "State", "Pincode", "Phones", "Next Eligible Date"};
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

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(850, 240));
        mainContent.add(scrollPane, BorderLayout.CENTER);

        add(mainContent, BorderLayout.CENTER);

        // South: Registration Form
        JPanel southPanel = new JPanel(new BorderLayout(8, 8));
        southPanel.setBorder(BorderFactory.createTitledBorder("Register New Donor"));

        JPanel formGrid = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0
        gbc.gridx = 0; gbc.gridy = 0;
        formGrid.add(new JLabel("Full Name:*"), gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        nameField = new JTextField(15);
        formGrid.add(nameField, gbc);

        gbc.gridx = 2; gbc.gridy = 0;
        formGrid.add(new JLabel("Gender:*"), gbc);
        gbc.gridx = 3; gbc.gridy = 0;
        genderCombo = new JComboBox<>(new String[]{"M", "F", "O"});
        formGrid.add(genderCombo, gbc);

        gbc.gridx = 4; gbc.gridy = 0;
        formGrid.add(new JLabel("DOB (YYYY-MM-DD):*"), gbc);
        gbc.gridx = 5; gbc.gridy = 0;
        dobField = new JTextField(10);
        formGrid.add(dobField, gbc);

        // Row 1
        gbc.gridx = 0; gbc.gridy = 1;
        formGrid.add(new JLabel("Blood Group:*"), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        bloodGroupCombo = new JComboBox<>(new String[]{"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"});
        formGrid.add(bloodGroupCombo, gbc);

        gbc.gridx = 2; gbc.gridy = 1;
        formGrid.add(new JLabel("Phone:*"), gbc);
        gbc.gridx = 3; gbc.gridy = 1;
        phoneField = new JTextField(12);
        formGrid.add(phoneField, gbc);

        gbc.gridx = 4; gbc.gridy = 1;
        formGrid.add(new JLabel("City:*"), gbc);
        gbc.gridx = 5; gbc.gridy = 1;
        cityField = new JTextField(12);
        formGrid.add(cityField, gbc);

        // Row 2
        gbc.gridx = 0; gbc.gridy = 2;
        formGrid.add(new JLabel("State:*"), gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        stateField = new JTextField(12);
        formGrid.add(stateField, gbc);

        gbc.gridx = 2; gbc.gridy = 2;
        formGrid.add(new JLabel("Pincode (6 digits):*"), gbc);
        gbc.gridx = 3; gbc.gridy = 2;
        pincodeField = new JTextField(8);
        formGrid.add(pincodeField, gbc);

        southPanel.add(formGrid, BorderLayout.CENTER);

        // Actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton registerBtn = new JButton("Register Donor");
        registerBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        registerBtn.addActionListener(e -> onRegisterDonor());

        JButton clearBtn = new JButton("Clear Form");
        clearBtn.addActionListener(e -> clearForm());

        actions.add(registerBtn);
        actions.add(clearBtn);
        southPanel.add(actions, BorderLayout.SOUTH);

        add(southPanel, BorderLayout.SOUTH);

        refreshData();
    }

    @Override
    public void refreshData() {
        String group = (String) filterGroupCombo.getSelectedItem();
        String city = filterCityField.getText().trim();

        new SwingWorker<List<Donor>, Void>() {
            @Override
            protected List<Donor> doInBackground() throws Exception {
                return donorDao.searchDonors(group, city);
            }

            @Override
            protected void done() {
                try {
                    List<Donor> donors = get();
                    tableModel.setRowCount(0);
                    for (Donor d : donors) {
                        tableModel.addRow(new Object[]{
                                d.getId(),
                                d.getName(),
                                d.getGender(),
                                d.getDob(),
                                d.getBloodGroup(),
                                d.getAge(),
                                d.getCity(),
                                d.getState(),
                                d.getPincode(),
                                String.join(", ", d.getPhones()),
                                d.getNextEligibleDate() != null ? d.getNextEligibleDate() : "Eligible Now"
                        });
                    }
                } catch (Exception ex) {
                    showError("Failed to load donors: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void clearForm() {
        nameField.setText("");
        genderCombo.setSelectedIndex(0);
        dobField.setText("");
        bloodGroupCombo.setSelectedIndex(0);
        cityField.setText("");
        stateField.setText("");
        pincodeField.setText("");
        phoneField.setText("");
    }

    private void onRegisterDonor() {
        try {
            String name = nameField.getText().trim();
            String gender = (String) genderCombo.getSelectedItem();
            String dobStr = dobField.getText().trim();
            String group = (String) bloodGroupCombo.getSelectedItem();
            String city = cityField.getText().trim();
            String state = stateField.getText().trim();
            String pincode = pincodeField.getText().trim();
            String phone = phoneField.getText().trim();

            Validator.requireNotEmpty(name, "Full Name");
            Validator.requireNotEmpty(gender, "Gender");
            LocalDate dob = Validator.validateDate(dobStr, "Date of Birth");
            Validator.requireNotEmpty(group, "Blood Group");
            Validator.requireNotEmpty(city, "City");
            Validator.requireNotEmpty(state, "State");
            Validator.validatePincode(pincode);
            Validator.requireNotEmpty(phone, "Phone");

            new SwingWorker<Integer, Void>() {
                @Override
                protected Integer doInBackground() throws Exception {
                    return donorDao.registerDonor(name, gender, dob, group, city, state, pincode, phone);
                }

                @Override
                protected void done() {
                    try {
                        int id = get();
                        showInfo("Donor successfully registered with ID: " + id);
                        clearForm();
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
}
