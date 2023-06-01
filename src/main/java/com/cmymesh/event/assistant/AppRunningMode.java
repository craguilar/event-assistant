package com.cmymesh.event.assistant;

/**
 * Define the app running mode .
 * - GUEST_VALIDATE: Runs a Guest check from the configured guest data source.
 * - SEND_NOTIFICATIONS: Contacts the guest data source, sends notifications and add them to tracking database.
 * - TRACKING_FAILED_REPORT: List all tracked guests that have failed to send any notification.
 * - TRACKING_GUEST_RECONCILIATION: Extracts GUESTS - TRACKING (with error) = {PENDING TRACKING} and
 *  TRACKING - GUESTS = {NOT IN SOURCE}.
 * - DUMP_TRACKING: Dump the tracking database to an xml file.
 */
public enum AppRunningMode {
    GUEST_VALIDATE,
    SEND_NOTIFICATIONS,
    TRACKING_FAILED_REPORT,
    TRACKING_GUEST_RECONCILIATION,
    TRACKING_DUMP
}
