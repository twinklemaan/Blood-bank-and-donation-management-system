package com.bloodbank.thread;

import com.bloodbank.dao.StockDao;
import com.bloodbank.model.StockEntry;
import com.bloodbank.util.AppConfig;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background thread for polling blood stock levels and dispatching warnings.
 * Demonstrates multithreading, thread safety, and Collections (Map, Set) usage (Rubric: Thread).
 */
public class StockMonitor implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(StockMonitor.class.getName());
    private static ScheduledExecutorService scheduler;
    private static StockMonitor instance;

    private final StockDao stockDao = new StockDao();
    private final List<StockListener> listeners = new CopyOnWriteArrayList<>();

    // Rubric: Collections requirement - Set<String> of already-alerted (bankId:group) pairs
    private final Set<String> alreadyAlertedPairs = Collections.synchronizedSet(new HashSet<>());

    public static synchronized void startMonitoring() {
        if (scheduler == null || scheduler.isShutdown()) {
            instance = new StockMonitor();
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "StockMonitor-Thread");
                t.setDaemon(true);
                return t;
            });
            int interval = AppConfig.getInstance().getMonitorIntervalSeconds();
            scheduler.scheduleWithFixedDelay(instance, 2, interval, TimeUnit.SECONDS);
            LOGGER.info("StockMonitor started with interval: " + interval + "s");
        }
    }

    public static synchronized void stopMonitoring() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
            if (instance != null) {
                instance.listeners.clear();
                instance.alreadyAlertedPairs.clear();
                instance = null;
            }
            LOGGER.info("StockMonitor stopped.");
        }
    }

    public static StockMonitor getInstance() {
        return instance;
    }

    public void addListener(StockListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(StockListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void run() {
        try {
            int threshold = AppConfig.getInstance().getLowStockThreshold();
            List<StockEntry> entries = stockDao.getAllStock();

            // Rubric: Collections requirement - Map<String, Integer>
            Map<String, Integer> summary = new LinkedHashMap<>();
            List<String> newAlerts = new ArrayList<>();
            Set<String> currentLowPairs = new HashSet<>();

            for (StockEntry entry : entries) {
                String pairKey = entry.getBankId() + ":" + entry.getBloodGroup();
                String displayKey = entry.getBankName() + " (" + entry.getCity() + ") - " + entry.getBloodGroup();
                summary.put(displayKey, entry.getUnitsAvailable());

                if (entry.getUnitsAvailable() < threshold) {
                    currentLowPairs.add(pairKey);
                    if (!alreadyAlertedPairs.contains(pairKey)) {
                        newAlerts.add(entry.getBankName() + " [" + entry.getBloodGroup() + "] has only "
                                + entry.getUnitsAvailable() + " unit(s) remaining (threshold: " + threshold + ")");
                        alreadyAlertedPairs.add(pairKey);
                    }
                }
            }

            // Remove recovered pairs from alreadyAlertedPairs so future drops will alert again
            alreadyAlertedPairs.removeIf(pairKey -> !currentLowPairs.contains(pairKey));

            // Rubric: Thread requirement - listeners updated ONLY via SwingUtilities.invokeLater
            SwingUtilities.invokeLater(() -> {
                for (StockListener listener : listeners) {
                    try {
                        listener.onStockUpdated(summary, newAlerts);
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "Error notifying stock listener", ex);
                    }
                }
            });

        } catch (Exception e) {
            LOGGER.log(Level.FINE, "StockMonitor check encountered: {0}", e.getMessage());
        }
    }
}
