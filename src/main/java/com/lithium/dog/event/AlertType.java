package com.lithium.dog.event;

public enum AlertType {
    // enum values are lower case to comply with ddog expectations
    // alert_type (String, None) — default: ‘info’ — Can be ‘error’, ‘warning’, ‘info’ or ‘success’.
    error, warning, info, success;
}
