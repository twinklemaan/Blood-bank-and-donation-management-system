package com.bloodbank.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton configuration manager reading from config/db.properties.
 */
public class AppConfig {
    private static final Logger LOGGER = Logger.getLogger(AppConfig.class.getName());
    private static final String CONFIG_FILE_PATH = "config/db.properties";
    private static AppConfig instance;

    private final Properties properties = new Properties();
    private boolean fileLoaded = false;

    private AppConfig() {
        loadProperties();
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    public synchronized void reload() {
        loadProperties();
    }

    private void loadProperties() {
        properties.clear();
        File file = new File(CONFIG_FILE_PATH);
        if (!file.exists()) {
            // Also check relative to working directory or user directory
            file = new File(System.getProperty("user.dir"), CONFIG_FILE_PATH);
        }

        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                properties.load(in);
                fileLoaded = true;
                LOGGER.info("Loaded configuration from " + file.getAbsolutePath());
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to load " + CONFIG_FILE_PATH, e);
            }
        } else {
            LOGGER.warning("Configuration file not found at " + CONFIG_FILE_PATH);
            fileLoaded = false;
        }
    }

    public boolean isFileLoaded() {
        return fileLoaded;
    }

    public String getDbUrl() {
        return properties.getProperty("db.url",
                "jdbc:mysql://localhost:3306/blood_bank_db_er?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata");
    }

    public String getDbUser() {
        return properties.getProperty("db.user", "root");
    }

    public String getDbPassword() {
        return properties.getProperty("db.password", "");
    }

    public int getLowStockThreshold() {
        try {
            return Integer.parseInt(properties.getProperty("stock.low.threshold", "5").trim());
        } catch (NumberFormatException e) {
            return 5;
        }
    }

    public int getMonitorIntervalSeconds() {
        try {
            return Integer.parseInt(properties.getProperty("monitor.interval.seconds", "10").trim());
        } catch (NumberFormatException e) {
            return 10;
        }
    }

    public int getDemoHoldSeconds() {
        try {
            return Integer.parseInt(properties.getProperty("demo.hold.seconds", "0").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
