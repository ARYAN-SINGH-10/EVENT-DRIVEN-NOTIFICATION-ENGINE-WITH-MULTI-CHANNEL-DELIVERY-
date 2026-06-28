package com.notificationengine.service.impl;

import com.notificationengine.dto.taxonomy.FinancialEventTypeDto;
import com.notificationengine.service.FinancialEventTaxonomyService;
import com.notificationengine.taxonomy.FinancialEventCategory;
import com.notificationengine.taxonomy.FinancialEventType;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FinancialEventTaxonomyServiceImpl implements FinancialEventTaxonomyService {

    @Override
    public List<FinancialEventTypeDto> getAllEvents() {
        return Arrays.stream(FinancialEventType.values())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<FinancialEventTypeDto> getEventsByCategory(FinancialEventCategory category) {
        return Arrays.stream(FinancialEventType.values())
                .filter(e -> e.getCategory() == category)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public FinancialEventTypeDto getEventByCode(String eventCode) {
        return Arrays.stream(FinancialEventType.values())
                .filter(e -> e.getEventCode().equalsIgnoreCase(eventCode))
                .findFirst()
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Invalid event code: " + eventCode));
    }

    private FinancialEventTypeDto toDto(FinancialEventType entity) {
        return FinancialEventTypeDto.builder()
                .eventCode(entity.getEventCode())
                .eventName(entity.getEventName())
                .category(entity.getCategory())
                .priority(entity.getPriority())
                .regulatoryFlag(entity.isRegulatoryFlag())
                .defaultChannels(entity.getDefaultChannels())
                .maxLatency(entity.getMaxLatency())
                .build();
    }
}
