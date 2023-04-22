package com.cmymesh.event.assistant;

/**
 * Define the app running mode .
 * - VALIDATE: Runs a Guest check from the configured guest data source.
 * - SEND_NOTIFICATIONS: Contacts the guest data source, sends notifications and add them to tracking database.
 * - TRACKING_FAILED_REPORT: List all tracked guests that have failed to send any notification.
 * - DUMP_TRACKING: Dump the tracking data base to an xml file.
 */
public enum AppRunningMode {
    VALIDATE,
    SEND_NOTIFICATIONS,
    TRACKING_FAILED_REPORT,
    DUMP_TRACKING
}
