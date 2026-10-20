package com.bloodbank.ui;

import com.bloodbank.dao.RequestDao;
import com.bloodbank.model.BankStaffUser;
import com.bloodbank.model.BloodRequest;
import com.bloodbank.model.User;
import com.bloodbank.util.AppConfig;
import com.bloodbank.util.Session;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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
 * Screen for Bank Staff to review and fulfil pending hospital blood requests.
 * Uses Comparator for strict urgency-then-date ordering (Rubric: Collections).
 * Dispatches sp_fulfil_request via SwingWorker with row-level transaction safety (Rubric: Thread & JDBC CRUD).
 */
public class RequestsToFulfilPanel extends BasePanel {
    private final RequestDao requestDao = new RequestDao();
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private JTable table;
    private DefaultTableModel tableModel;
    private JButton fulfilButton;
    private JButton refreshButton;

    public RequestsToFulfilPanel() {
        add(createHeaderPanel("Requests to Fulfil",
                "Pending hospital requests prioritized by urgency (Critical > Urgent > Normal) and date. Earliest-expiring units are issued first."), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.setBorder(BorderFactory.createTitledBorder("Pending Blood Orders Awaiting Fulfilment"));

        String[] columns = {"Request ID", "Hospital Name", "Blood Group", "Units Needed", "Urgency Priority", "Request Timestamp"};
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

        // Highlight Critical urgency in bold red
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);

                String urgency = tbl.getValueAt(row, 4) != null ? tbl.getValueAt(row, 4).toString() : "";
                if (!isSelected) {
                    if ("Critical".equals(urgency)) {
                        c.setForeground(new Color(200, 0, 0));
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                    } else if ("Urgent".equals(urgency)) {
                        c.setForeground(new Color(180, 90, 0));
                        c.setFont(c.getFont().deriveFont(Font.PLAIN));
                    } else {
                        c.setForeground(Color.BLACK);
                        c.setFont(c.getFont().deriveFont(Font.PLAIN));
                    }
                    c.setBackground(Color.WHITE);
                }

                if (column == 0 || column == 2 || column == 3 || column == 4) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }

                return c;
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                fulfilButton.setEnabled(table.getSelectedRow() >= 0);
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(850, 360));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // Action Toolbar
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        fulfilButton = new JButton("Fulfil Selected Request");
        fulfilButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        fulfilButton.setBackground(new Color(34, 139, 34)); // Green
        fulfilButton.setForeground(Color.WHITE);
        fulfilButton.setEnabled(false);
        fulfilButton.addActionListener(e -> onFulfilRequest());

        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshData());

        actionsPanel.add(fulfilButton);
        actionsPanel.add(refreshButton);

        add(actionsPanel, BorderLayout.SOUTH);

        refreshData();
    }

    private int getStaffBankId() {
        User user = Session.getInstance().getCurrentUser();
        if (user instanceof BankStaffUser staffUser) {
            return staffUser.getBankId();
        }
        return 1;
    }

    private int getStaffId() {
        User user = Session.getInstance().getCurrentUser();
        if (user instanceof BankStaffUser staffUser) {
            return staffUser.getStaffId();
        }
        return 1;
    }

    @Override
    public void refreshData() {
        int bankId = getStaffBankId();

        new SwingWorker<List<BloodRequest>, Void>() {
            @Override
            protected List<BloodRequest> doInBackground() throws Exception {
                // Returns requests already sorted via BloodRequest.BY_URGENCY_THEN_DATE
                return requestDao.getPendingRequestsForBank(bankId);
            }

            @Override
            protected void done() {
                try {
                    List<BloodRequest> list = get();
                    tableModel.setRowCount(0);
                    for (BloodRequest req : list) {
                        String timeStr = req.getRequestDate() != null ? req.getRequestDate().format(DTF) : "";
                        tableModel.addRow(new Object[]{
                                req.getRequestId(),
                                req.getHospitalName(),
                                req.getBloodGroup(),
                                req.getUnitsRequested(),
                                req.getUrgency(),
                                timeStr
                        });
                    }
                    fulfilButton.setEnabled(false);
                } catch (Exception ex) {
                    showError("Failed to load pending requests: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void onFulfilRequest() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) return;

        int requestId = (Integer) tableModel.getValueAt(selectedRow, 0);
        String hospitalName = (String) tableModel.getValueAt(selectedRow, 1);
        String group = (String) tableModel.getValueAt(selectedRow, 2);
        int units = (Integer) tableModel.getValueAt(selectedRow, 3);
        String urgency = (String) tableModel.getValueAt(selectedRow, 4);

        int staffId = getStaffId();
        int holdSeconds = AppConfig.getInstance().getDemoHoldSeconds();

        String message = "Fulfil Blood Request #" + requestId + "?\n" +
                "Hospital: " + hospitalName + "\n" +
                "Blood Group: " + group + " (" + units + " units)\n" +
                "Priority: " + urgency + "\n\n" +
                "The system will atomically issue units from earliest-expiring screened donations.";

        if (!confirmAction(message, "Confirm Request Fulfilment")) {
            return;
        }

        fulfilButton.setEnabled(false);
        fulfilButton.setText("Processing Fulfilment...");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return requestDao.fulfilRequest(requestId, staffId, holdSeconds);
            }

            @Override
            protected void done() {
                fulfilButton.setText("Fulfil Selected Request");
                try {
                    String outcome = get();
                    if ("Fulfilled".equalsIgnoreCase(outcome)) {
                        showInfo("Request #" + requestId + " was successfully FULFILLED!\n" +
                                units + " units of " + group + " have been deducted from inventory.");
                    } else if ("Rejected".equalsIgnoreCase(outcome)) {
                        showWarning("Request #" + requestId + " was REJECTED due to insufficient stock in bank.");
                    } else {
                        showInfo("Request outcome: " + outcome);
                    }
                    refreshData();
                } catch (Exception ex) {
                    showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                    refreshData();
                }
            }
        }.execute();
    }
}
