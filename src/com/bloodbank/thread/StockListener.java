package com.bloodbank.thread;

import java.util.List;
import java.util.Map;

/**
 * Listener interface for background stock monitoring events.
 */
public interface StockListener {
    /**
     * Fired when the background monitor loads fresh stock data.
     * Guaranteed to be dispatched on the Swing Event Dispatch Thread (EDT).
     *
     * @param stockSummary Map of "BankName - BloodGroup" to units
     * @param lowStockAlerts List of descriptions for groups below low threshold
     */
    void onStockUpdated(Map<String, Integer> stockSummary, List<String> lowStockAlerts);
}
