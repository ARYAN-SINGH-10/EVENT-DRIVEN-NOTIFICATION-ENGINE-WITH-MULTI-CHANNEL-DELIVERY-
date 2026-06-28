package com.notificationengine.taxonomy;

import com.notificationengine.entity.NotificationEntity.ChannelType;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * Complete taxonomy of 25+ financial event types across 5 categories.
 */
@Getter
public enum FinancialEventType {

    // ---------------------------------------------------------
    // Category 1: Transaction Events (TXNX-*)
    // ---------------------------------------------------------
    TXNX_001("TXNX-001", "Buy Order Executed", FinancialEventCategory.TRANSACTION_EVENTS, "HIGH", true, Arrays.asList(ChannelType.SMS, ChannelType.PUSH, ChannelType.EMAIL), "30 seconds"),
    TXNX_002("TXNX-002", "Sell Order Executed", FinancialEventCategory.TRANSACTION_EVENTS, "HIGH", true, Arrays.asList(ChannelType.SMS, ChannelType.PUSH, ChannelType.EMAIL), "30 seconds"),
    TXNX_003("TXNX-003", "Order Rejected", FinancialEventCategory.TRANSACTION_EVENTS, "HIGH", true, Arrays.asList(ChannelType.PUSH, ChannelType.SMS), "15 seconds"),
    TXNX_004("TXNX-004", "Dividend Credited", FinancialEventCategory.TRANSACTION_EVENTS, "MEDIUM", false, Arrays.asList(ChannelType.EMAIL, ChannelType.PUSH), "1 hour"),
    TXNX_005("TXNX-005", "Funds Deposited", FinancialEventCategory.TRANSACTION_EVENTS, "HIGH", true, Arrays.asList(ChannelType.SMS, ChannelType.PUSH), "60 seconds"),

    // ---------------------------------------------------------
    // Category 2: Risk & Margin Events (RISK-*)
    // ---------------------------------------------------------
    RISK_001("RISK-001", "Margin Call Warning", FinancialEventCategory.RISK_EVENTS, "CRITICAL", true, Arrays.asList(ChannelType.SMS, ChannelType.PUSH, ChannelType.CALL), "10 seconds"),
    RISK_002("RISK-002", "Margin Shortfall", FinancialEventCategory.RISK_EVENTS, "CRITICAL", true, Arrays.asList(ChannelType.SMS, ChannelType.PUSH, ChannelType.EMAIL, ChannelType.CALL), "5 seconds"),
    RISK_003("RISK-003", "Position Squared Off", FinancialEventCategory.RISK_EVENTS, "CRITICAL", true, Arrays.asList(ChannelType.SMS, ChannelType.PUSH, ChannelType.EMAIL), "15 seconds"),
    RISK_004("RISK-004", "Portfolio Risk Alert", FinancialEventCategory.RISK_EVENTS, "HIGH", false, Arrays.asList(ChannelType.PUSH, ChannelType.EMAIL), "5 minutes"),
    RISK_005("RISK-005", "Concentration Alert", FinancialEventCategory.RISK_EVENTS, "MEDIUM", false, Arrays.asList(ChannelType.EMAIL, ChannelType.IN_APP), "1 hour"),

    // ---------------------------------------------------------
    // Category 3: SIP & Investment Events (SIPX-*)
    // ---------------------------------------------------------
    SIPX_001("SIPX-001", "SIP Due Reminder", FinancialEventCategory.SIP_EVENTS, "MEDIUM", false, Arrays.asList(ChannelType.PUSH, ChannelType.WHATSAPP), "3 days"),
    SIPX_002("SIPX-002", "SIP Executed", FinancialEventCategory.SIP_EVENTS, "HIGH", true, Arrays.asList(ChannelType.SMS, ChannelType.EMAIL), "2 hours"),
    SIPX_003("SIPX-003", "SIP Failed", FinancialEventCategory.SIP_EVENTS, "HIGH", true, Arrays.asList(ChannelType.SMS, ChannelType.PUSH, ChannelType.EMAIL), "30 minutes"),
    SIPX_004("SIPX-004", "SIP Step-Up Reminder", FinancialEventCategory.SIP_EVENTS, "LOW", false, Arrays.asList(ChannelType.EMAIL, ChannelType.IN_APP), "Annual"),
    SIPX_005("SIPX-005", "Goal Milestone Reached", FinancialEventCategory.SIP_EVENTS, "LOW", false, Arrays.asList(ChannelType.PUSH, ChannelType.IN_APP), "4 hours"),

    // ---------------------------------------------------------
    // Category 4: Market & Price Events (MKTX-*)
    // ---------------------------------------------------------
    MKTX_001("MKTX-001", "Price Alert Triggered", FinancialEventCategory.MARKET_EVENTS, "HIGH", false, Arrays.asList(ChannelType.PUSH, ChannelType.SMS), "15 seconds"),
    MKTX_002("MKTX-002", "Circuit Breaker Hit", FinancialEventCategory.MARKET_EVENTS, "CRITICAL", true, Arrays.asList(ChannelType.PUSH, ChannelType.SMS), "10 seconds"),
    MKTX_003("MKTX-003", "Market Open/Close", FinancialEventCategory.MARKET_EVENTS, "LOW", false, Arrays.asList(ChannelType.PUSH), "At event time"),
    MKTX_004("MKTX-004", "52-Week High/Low", FinancialEventCategory.MARKET_EVENTS, "MEDIUM", false, Arrays.asList(ChannelType.PUSH, ChannelType.EMAIL), "30 minutes"),
    MKTX_005("MKTX-005", "Earnings Announcement", FinancialEventCategory.MARKET_EVENTS, "MEDIUM", false, Arrays.asList(ChannelType.EMAIL, ChannelType.IN_APP), "1 day"),

    // ---------------------------------------------------------
    // Category 5: Regulatory & Compliance Events (REGX-*)
    // ---------------------------------------------------------
    REGX_001("REGX-001", "KYC Expiry Warning", FinancialEventCategory.REGULATORY_EVENTS, "HIGH", true, Arrays.asList(ChannelType.SMS, ChannelType.EMAIL, ChannelType.PUSH), "30 days"),
    REGX_002("REGX-002", "Nominee Update Reminder", FinancialEventCategory.REGULATORY_EVENTS, "MEDIUM", true, Arrays.asList(ChannelType.EMAIL, ChannelType.PUSH), "Quarterly"),
    REGX_003("REGX-003", "Contract Note Generated", FinancialEventCategory.REGULATORY_EVENTS, "HIGH", true, Arrays.asList(ChannelType.EMAIL), "EOD"),
    REGX_004("REGX-004", "Tax Statement Available", FinancialEventCategory.REGULATORY_EVENTS, "MEDIUM", true, Arrays.asList(ChannelType.EMAIL, ChannelType.IN_APP), "Quarterly"),
    REGX_005("REGX-005", "Regulatory Policy Change", FinancialEventCategory.REGULATORY_EVENTS, "LOW", false, Arrays.asList(ChannelType.EMAIL), "24 hours");

    private final String eventCode;
    private final String eventName;
    private final FinancialEventCategory category;
    private final String priority;
    private final boolean regulatoryFlag;
    private final List<ChannelType> defaultChannels;
    private final String maxLatency;

    FinancialEventType(String eventCode, String eventName, FinancialEventCategory category, String priority, boolean regulatoryFlag, List<ChannelType> defaultChannels, String maxLatency) {
        this.eventCode = eventCode;
        this.eventName = eventName;
        this.category = category;
        this.priority = priority;
        this.regulatoryFlag = regulatoryFlag;
        this.defaultChannels = defaultChannels;
        this.maxLatency = maxLatency;
    }
}
