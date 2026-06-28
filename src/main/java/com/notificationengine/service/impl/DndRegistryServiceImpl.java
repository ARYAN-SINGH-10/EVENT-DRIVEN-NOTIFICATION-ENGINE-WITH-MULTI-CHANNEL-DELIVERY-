package com.notificationengine.service.impl;

import com.notificationengine.entity.DndRegistry;
import com.notificationengine.repository.DndRegistryRepository;
import com.notificationengine.service.DndRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DndRegistryServiceImpl implements DndRegistryService {

    private final DndRegistryRepository dndRegistryRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isNumberBlockedForCategory(String phoneNumber, String category) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return false; // Can't block without a phone number
        }

        Optional<DndRegistry> registryOpt = dndRegistryRepository.findById(phoneNumber);
        
        if (registryOpt.isEmpty()) {
            return false; // Number is not in the DND registry at all
        }

        DndRegistry registry = registryOpt.get();

        if (registry.isFullyBlocked()) {
            return true;
        }

        if (registry.getAllowedCategories() != null && !registry.getAllowedCategories().isBlank() && category != null) {
            boolean isAllowed = Arrays.stream(registry.getAllowedCategories().split(","))
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .anyMatch(c -> c.equals(category.toUpperCase()));
            return !isAllowed;
        }

        return true; // Partially blocked, but category not in allowed list
    }
}
