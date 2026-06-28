package com.notificationengine.service;

import com.notificationengine.dto.taxonomy.FinancialEventTypeDto;
import com.notificationengine.taxonomy.FinancialEventCategory;

import java.util.List;

public interface FinancialEventTaxonomyService {
    List<FinancialEventTypeDto> getAllEvents();
    List<FinancialEventTypeDto> getEventsByCategory(FinancialEventCategory category);
    FinancialEventTypeDto getEventByCode(String eventCode);
}
