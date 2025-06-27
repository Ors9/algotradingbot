package com.algotradingbot;

/**
 * Simple logger for writing bot actions and events to console or file.
 * Helps with monitoring and debugging.
 */
public class Logger {
    public static void log(String message) {
        System.out.println("[LOG] " + message);
        // TODO: Optionally save to file
    }
}