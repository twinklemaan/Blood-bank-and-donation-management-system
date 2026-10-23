package com.bloodbank.ui;

import com.bloodbank.dao.RequestDao;
import com.bloodbank.model.BloodRequest;
import com.bloodbank.model.HospitalUser;
import com.bloodbank.model.User;
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
 * Screen for Hospitals to track the status of their blood requests.
 * Features color-coded status badges (Pending: Amber, Fulfilled: Green, Rejected: Red).
 */
public class MyRequestsPanel extends BasePanel {
    private final RequestDao requestDao = new RequestDao();
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private JTable table;
    private DefaultTableModel tableModel;

    public MyRequestsPanel() {
        add(createHeaderPanel("My Blood Requests",
                "Track hospital blood requisitions and fulfilment status in real time."), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.setBorder(BorderFactory.createTitledBorder("Hospital Blood Supply Orders"));

        String[] columns = {"Request ID", "Target Blood Bank", "Blood Group", "Units", "Urgency", "Status", "Request Date", "Fulfilled Date"};
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

        // Color-code Status and Urgency
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);

                String status = tbl.getValueAt(row, 5) != null ? tbl.getValueAt(row, 5).toString() : "";
                String urgency = tbl.getValueAt(row, 4) != null ? tbl.getValueAt(row, 4).toString() : "";

                if (!isSelected) {
                    if (column == 5) {
                        switch (status) {
                            case "Fulfilled" -> {
                                c.setForeground(new Color(0, 130, 0)); // Green
                                c.setFont(c.getFont().deriveFont(Font.BOLD));
                            }
                            case "Rejected" -> {
                                c.setForeground(new Color(180, 0, 0)); // Red
                                c.setFont(c.getFont().deriveFont(Font.BOLD));
                            }
                            case "Pending" -> {
                                c.setForeground(new Color(190, 110, 0)); // Amber
                                c.setFont(c.getFont().deriveFont(Font.BOLD));
                            }
                            default -> c.setForeground(Color.BLACK);
                        }
                    } else if (column == 4) {
                        if ("Critical".equals(urgency)) {
                            c.setForeground(new Color(200, 0, 0));
                            c.setFont(c.getFont().deriveFont(Font.BOLD));
                        } else if ("Urgent".equals(urgency)) {
                            c.setForeground(new Color(180, 90, 0));
                        } else {
                            c.setForeground(Color.BLACK);
                        }
                    } else {
                        c.setForeground(Color.BLACK);
                    }
                    c.setBackground(Color.WHITE);
                }

                if (column == 0 || column == 2 || column == 3 || column == 4 || column == 5) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }

                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(850, 360));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // Refresh Bar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        JButton refreshBtn = new JButton("Refresh Requests");
        refreshBtn.addActionListener(e -> refreshData());
        bottomBar.add(refreshBtn);
        add(bottomBar, BorderLayout.SOUTH);

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
        int hospitalId = getHospitalId();

        new SwingWorker<List<BloodRequest>, Void>() {
            @Override
            protected List<BloodRequest> doInBackground() throws Exception {
                return requestDao.getRequestsByHospital(hospitalId);
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
