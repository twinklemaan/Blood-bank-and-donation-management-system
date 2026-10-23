package com.bloodbank.ui;

import com.bloodbank.dao.BankDao;
import com.bloodbank.dao.HospitalDao;
import com.bloodbank.dao.RequestDao;
import com.bloodbank.model.Bank;
import com.bloodbank.model.BloodRequest;
import com.bloodbank.model.Hospital;

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
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Screen for System Administrators to audit all blood requests across all hospitals and blood banks.
 * Read-only table view with filtering (Rubric: GUI & JDBC CRUD).
 */
public class AllRequestsPanel extends BasePanel {
    private final RequestDao requestDao = new RequestDao();
    private final BankDao bankDao = new BankDao();
    private final HospitalDao hospitalDao = new HospitalDao();
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private JComboBox<String> statusCombo;
    private JComboBox<NamedFilterItem> bankCombo;
    private JComboBox<NamedFilterItem> hospCombo;
    private JTable table;
    private DefaultTableModel tableModel;

    private static class NamedFilterItem {
        private final Integer id;
        private final String name;

        public NamedFilterItem(Integer id, String name) {
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

    public AllRequestsPanel() {
        add(createHeaderPanel("All Requests Audit",
                "Administrative audit of blood requisition pipeline, urgency priorities, and fulfilment metrics."), BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));

        // Filters Toolbar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        filterBar.setBorder(BorderFactory.createTitledBorder("Audit Filters"));

        filterBar.add(new JLabel("Status:"));
        statusCombo = new JComboBox<>(new String[]{"All", "Pending", "Fulfilled", "Rejected"});
        filterBar.add(statusCombo);

        filterBar.add(new JLabel("Bank:"));
        bankCombo = new JComboBox<>();
        filterBar.add(bankCombo);

        filterBar.add(new JLabel("Hospital:"));
        hospCombo = new JComboBox<>();
        filterBar.add(hospCombo);

        JButton filterBtn = new JButton("Apply Filters");
        filterBtn.addActionListener(e -> loadRequestsData());
        filterBar.add(filterBtn);

        JButton resetBtn = new JButton("Reset");
        resetBtn.addActionListener(e -> {
            statusCombo.setSelectedIndex(0);
            if (bankCombo.getItemCount() > 0) bankCombo.setSelectedIndex(0);
            if (hospCombo.getItemCount() > 0) hospCombo.setSelectedIndex(0);
            loadRequestsData();
        });
        filterBar.add(resetBtn);

        mainPanel.add(filterBar, BorderLayout.NORTH);

        // Results Table
        String[] columns = {"ID", "Hospital", "Blood Bank", "Group", "Units", "Urgency", "Status", "Request Date", "Fulfilled Date"};
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

        // Color Renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);

                String status = tbl.getValueAt(row, 6) != null ? tbl.getValueAt(row, 6).toString() : "";
                String urgency = tbl.getValueAt(row, 5) != null ? tbl.getValueAt(row, 5).toString() : "";

                if (!isSelected) {
                    if (column == 6) {
                        switch (status) {
                            case "Fulfilled" -> c.setForeground(new Color(0, 130, 0));
                            case "Rejected" -> c.setForeground(new Color(180, 0, 0));
                            case "Pending" -> c.setForeground(new Color(190, 110, 0));
                            default -> c.setForeground(Color.BLACK);
                        }
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                    } else if (column == 5) {
                        if ("Critical".equals(urgency)) {
                            c.setForeground(new Color(200, 0, 0));
                            c.setFont(c.getFont().deriveFont(Font.BOLD));
                        } else {
                            c.setForeground(Color.BLACK);
                        }
                    } else {
                        c.setForeground(Color.BLACK);
                    }
                    c.setBackground(Color.WHITE);
                }

                if (column == 0 || column == 3 || column == 4 || column == 5 || column == 6) {
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
        new SwingWorker<Void, Void>() {
            private List<Bank> banks;
            private List<Hospital> hospitals;

            @Override
            protected Void doInBackground() throws Exception {
                banks = bankDao.getAllBanks();
                hospitals = hospitalDao.getAllHospitals();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    DefaultComboBoxModel<NamedFilterItem> bankModel = new DefaultComboBoxModel<>();
                    bankModel.addElement(new NamedFilterItem(null, "All Banks"));
                    for (Bank b : banks) {
                        bankModel.addElement(new NamedFilterItem(b.getBankId(), b.getName()));
                    }
                    bankCombo.setModel(bankModel);

                    DefaultComboBoxModel<NamedFilterItem> hospModel = new DefaultComboBoxModel<>();
                    hospModel.addElement(new NamedFilterItem(null, "All Hospitals"));
                    for (Hospital h : hospitals) {
                        hospModel.addElement(new NamedFilterItem(h.getHospitalId(), h.getName()));
                    }
                    hospCombo.setModel(hospModel);

                    loadRequestsData();
                } catch (Exception ex) {
                    showError("Failed to initialize filters: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void loadRequestsData() {
        String status = (String) statusCombo.getSelectedItem();
        NamedFilterItem selectedBank = (NamedFilterItem) bankCombo.getSelectedItem();
        NamedFilterItem selectedHosp = (NamedFilterItem) hospCombo.getSelectedItem();

        Integer bankId = selectedBank != null ? selectedBank.getId() : null;
        Integer hospId = selectedHosp != null ? selectedHosp.getId() : null;

        new SwingWorker<List<BloodRequest>, Void>() {
            @Override
            protected List<BloodRequest> doInBackground() throws Exception {
                return requestDao.getAllRequests(status, bankId, hospId);
            }

            @Override
            protected void done() {
                try {
                    List<BloodRequest> list = get();
                    tableModel.setRowCount(0);
                    for (BloodRequest r : list) {
                        String reqDateStr = r.getRequestDate() != null ? r.getRequestDate().format(DTF) : "";
                        String fulDateStr = r.getFulfilledDate() != null ? r.getFulfilledDate().format(DTF) : "—";

                        tableModel.addRow(new Object[]{
                                r.getRequestId(),
                                r.getHospitalName(),
                                r.getBankName(),
                                r.getBloodGroup(),
                                r.getUnitsRequested(),
                                r.getUrgency(),
                                r.getStatus(),
                                reqDateStr,
                                fulDateStr
                        });
                    }
                } catch (Exception ex) {
                    showError("Failed to load requests: " + ex.getMessage());
                }
            }
        }.execute();
    }
}
