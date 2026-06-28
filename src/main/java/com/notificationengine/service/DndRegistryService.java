package com.notificationengine.service;

public interface DndRegistryService {

    /**
     * Checks if a phone number is registered in the national DND database.
     * Optionally checks if a specific promotional category is allowed.
     *
     * @param phoneNumber the E.164 formatted phone number
     * @param category    the promotional category (e.g. BANKING, RETAIL)
     * @return true if the number is blocked for this category, false otherwise
     */
    boolean isNumberBlockedForCategory(String phoneNumber, String category);

}
