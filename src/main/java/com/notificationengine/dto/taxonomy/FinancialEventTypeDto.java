package com.notificationengine.dto.taxonomy;

import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.taxonomy.FinancialEventCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialEventTypeDto {
    private String eventCode;
    private String eventName;
    private FinancialEventCategory category;
    private String priority;
    private boolean regulatoryFlag;
    private List<ChannelType> defaultChannels;
    private String maxLatency;
}
