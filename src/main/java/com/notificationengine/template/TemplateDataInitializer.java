package com.notificationengine.template;

import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.entity.NotificationTemplate;
import com.notificationengine.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds the database with pre-built notification templates on startup.
 *
 * <p>Covers all 5 supported channels (SMS, EMAIL, PUSH, WHATSAPP, IN_APP) and
 * all 5 supported locales (en, hi, mr, ta, te) for the most common financial
 * event types. Skips templates that already exist by name to be idempotent.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TemplateDataInitializer {

    private final NotificationTemplateRepository repository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        List<NotificationTemplate> templates = buildAllTemplates();
        int created = 0;
        for (NotificationTemplate t : templates) {
            if (!repository.existsByName(t.getName())) {
                repository.save(t);
                created++;
            }
        }
        log.info("Template seeding complete. Created={} Skipped={}", created, templates.size() - created);
    }

    // =========================================================================
    // Template definitions
    // =========================================================================

    private List<NotificationTemplate> buildAllTemplates() {
        return List.of(

            // ----------------------------------------------------------------
            // SMS Templates — TXNX-001 Buy Order Executed
            // ----------------------------------------------------------------
            sms("sms.txnx001.en", "TXNX-001", "en",
                null,
                "{{appName}}: {{userName}}, your BUY order for {{quantity}} shares of {{ticker}} at ₹{{price}} was executed on {{currentDate}}. Ref: {{orderId}}.",
                null,
                "userName, ticker, quantity, price, orderId"),

            sms("sms.txnx001.hi", "TXNX-001", "hi",
                null,
                "{{appName}}: {{userName}}, {{currentDate}} को {{ticker}} के {{quantity}} शेयर ₹{{price}} पर खरीदने का आदेश पूरा हुआ। संदर्भ: {{orderId}}।",
                null,
                "userName, ticker, quantity, price, orderId"),

            sms("sms.txnx001.mr", "TXNX-001", "mr",
                null,
                "{{appName}}: {{userName}}, {{currentDate}} रोजी {{ticker}} चे {{quantity}} शेअर्स ₹{{price}} ला खरेदीचे आदेश पूर्ण झाले. संदर्भ: {{orderId}}.",
                null,
                "userName, ticker, quantity, price, orderId"),

            sms("sms.txnx001.ta", "TXNX-001", "ta",
                null,
                "{{appName}}: {{userName}}, {{currentDate}} அன்று {{ticker}} பங்குகள் {{quantity}} எண்ணிக்கையில் ₹{{price}} விலையில் வாங்கும் ஆணை நிறைவேற்றப்பட்டது. குறிப்பு: {{orderId}}.",
                null,
                "userName, ticker, quantity, price, orderId"),

            sms("sms.txnx001.te", "TXNX-001", "te",
                null,
                "{{appName}}: {{userName}}, {{currentDate}} న {{ticker}} షేర్లు {{quantity}} సంఖ్యలో ₹{{price}} వద్ద కొనుగోలు ఆర్డర్ నిర్వహించబడింది. రిఫరెన్స్: {{orderId}}.",
                null,
                "userName, ticker, quantity, price, orderId"),

            // ----------------------------------------------------------------
            // SMS Templates — TXNX-003 Sell Order Executed
            // ----------------------------------------------------------------
            sms("sms.txnx003.en", "TXNX-003", "en",
                null,
                "{{appName}}: {{userName}}, your SELL order for {{quantity}} shares of {{ticker}} at ₹{{price}} was executed on {{currentDate}}. Net: ₹{{netAmount}}. Ref: {{orderId}}.",
                null,
                "userName, ticker, quantity, price, netAmount, orderId"),

            sms("sms.txnx003.hi", "TXNX-003", "hi",
                null,
                "{{appName}}: {{userName}}, {{ticker}} के {{quantity}} शेयर ₹{{price}} पर बेचने का आदेश पूरा हुआ। नेट: ₹{{netAmount}}। संदर्भ: {{orderId}}।",
                null,
                "userName, ticker, quantity, price, netAmount, orderId"),

            sms("sms.txnx003.mr", "TXNX-003", "mr",
                null,
                "{{appName}}: {{userName}}, {{ticker}} चे {{quantity}} शेअर्स ₹{{price}} ला विक्रीचे आदेश पूर्ण झाले. निव्वळ: ₹{{netAmount}}. संदर्भ: {{orderId}}.",
                null,
                "userName, ticker, quantity, price, netAmount, orderId"),

            sms("sms.txnx003.ta", "TXNX-003", "ta",
                null,
                "{{appName}}: {{userName}}, {{ticker}} பங்குகள் {{quantity}} எண்ணிக்கையில் ₹{{price}} விலையில் விற்கும் ஆணை நிறைவேற்றப்பட்டது. நிகர: ₹{{netAmount}}. குறிப்பு: {{orderId}}.",
                null,
                "userName, ticker, quantity, price, netAmount, orderId"),

            sms("sms.txnx003.te", "TXNX-003", "te",
                null,
                "{{appName}}: {{userName}}, {{ticker}} {{quantity}} షేర్లు ₹{{price}} వద్ద అమ్మకం ఆర్డర్ పూర్తయింది. నికర: ₹{{netAmount}}. రెఫ్: {{orderId}}.",
                null,
                "userName, ticker, quantity, price, netAmount, orderId"),

            // ----------------------------------------------------------------
            // SMS Templates — RISK-001 Margin Call
            // ----------------------------------------------------------------
            sms("sms.risk001.en", "RISK-001", "en",
                null,
                "URGENT {{appName}}: {{userName}}, your account has triggered a margin call. Required: ₹{{requiredAmount}} by {{deadline}}. Add funds now to avoid position liquidation.",
                null,
                "userName, requiredAmount, deadline"),

            sms("sms.risk001.hi", "RISK-001", "hi",
                null,
                "अत्यावश्यक {{appName}}: {{userName}}, आपके खाते में मार्जिन कॉल आया है। {{deadline}} तक ₹{{requiredAmount}} जमा करें, अन्यथा पोजीशन बंद हो सकती है।",
                null,
                "userName, requiredAmount, deadline"),

            sms("sms.risk001.mr", "RISK-001", "mr",
                null,
                "अत्यंत महत्त्वाचे {{appName}}: {{userName}}, आपल्या खात्यात मार्जिन कॉल आला आहे. {{deadline}} पर्यंत ₹{{requiredAmount}} जमा करा.",
                null,
                "userName, requiredAmount, deadline"),

            sms("sms.risk001.ta", "RISK-001", "ta",
                null,
                "அவசரம் {{appName}}: {{userName}}, உங்கள் கணக்கில் மார்ஜின் கால் உள்ளது. {{deadline}} க்குள் ₹{{requiredAmount}} செலுத்துங்கள்.",
                null,
                "userName, requiredAmount, deadline"),

            sms("sms.risk001.te", "RISK-001", "te",
                null,
                "అత్యవసరం {{appName}}: {{userName}}, మీ ఖాతాలో మార్జిన్ కాల్ వచ్చింది. {{deadline}} లోపల ₹{{requiredAmount}} జమ చేయండి.",
                null,
                "userName, requiredAmount, deadline"),

            // ----------------------------------------------------------------
            // SMS Templates — REGX-001 KYC Expiry Warning
            // ----------------------------------------------------------------
            sms("sms.regx001.en", "REGX-001", "en",
                null,
                "{{appName}}: Dear {{userName}}, your KYC expires on {{expiryDate}} ({{daysLeft}} days left). Update at {{kycUrl}} to avoid trading restrictions.",
                null,
                "userName, expiryDate, daysLeft, kycUrl"),

            sms("sms.regx001.hi", "REGX-001", "hi",
                null,
                "{{appName}}: प्रिय {{userName}}, आपका KYC {{expiryDate}} को समाप्त होगा ({{daysLeft}} दिन शेष)। ट्रेडिंग जारी रखने के लिए {{kycUrl}} पर अपडेट करें।",
                null,
                "userName, expiryDate, daysLeft, kycUrl"),

            sms("sms.regx001.mr", "REGX-001", "mr",
                null,
                "{{appName}}: प्रिय {{userName}}, आपले KYC {{expiryDate}} रोजी संपेल ({{daysLeft}} दिवस शिल्लक). {{kycUrl}} वर अपडेट करा.",
                null,
                "userName, expiryDate, daysLeft, kycUrl"),

            sms("sms.regx001.ta", "REGX-001", "ta",
                null,
                "{{appName}}: அன்பான {{userName}}, உங்கள் KYC {{expiryDate}} அன்று காலாவதியாகும் ({{daysLeft}} நாட்கள் மீதம்). {{kycUrl}} இல் புதுப்பிக்கவும்.",
                null,
                "userName, expiryDate, daysLeft, kycUrl"),

            sms("sms.regx001.te", "REGX-001", "te",
                null,
                "{{appName}}: ప్రియమైన {{userName}}, మీ KYC {{expiryDate}} న ముగుస్తుంది ({{daysLeft}} రోజులు మిగిలాయి). {{kycUrl}} లో అప్‌డేట్ చేయండి.",
                null,
                "userName, expiryDate, daysLeft, kycUrl"),

            // ----------------------------------------------------------------
            // EMAIL Templates — TXNX-001 Buy Order
            // ----------------------------------------------------------------
            email("email.txnx001.en", "TXNX-001", "en",
                "Order Confirmation — {{ticker}} BUY {{quantity}} shares @ ₹{{price}}",
                "Dear {{userName}},\n\nYour BUY order has been successfully executed.\n\nOrder Details:\n  Ticker    : {{ticker}}\n  Quantity  : {{quantity}} shares\n  Price     : ₹{{price}}\n  Total     : ₹{{totalAmount}}\n  Order ID  : {{orderId}}\n  Date      : {{currentDate}}\n\n{{#if charges}}\nApplicable charges:\n  Brokerage : ₹{{brokerage}}\n  STT       : ₹{{stt}}\n  Total     : ₹{{totalCharges}}\n{{/if}}\n\nThank you for trading with {{appName}}.\n\nThis is a system-generated message. Do not reply.",
                "<html><body style='font-family:Arial,sans-serif;color:#222'>" +
                "<h2 style='color:#1a7a4a'>✅ Order Confirmation</h2>" +
                "<p>Dear <strong>{{userName}}</strong>,</p>" +
                "<p>Your <strong>BUY</strong> order has been successfully executed.</p>" +
                "<table style='border-collapse:collapse;width:100%'>" +
                "<tr style='background:#f4f4f4'><td style='padding:8px;border:1px solid #ddd'><b>Ticker</b></td><td style='padding:8px;border:1px solid #ddd'>{{ticker}}</td></tr>" +
                "<tr><td style='padding:8px;border:1px solid #ddd'><b>Quantity</b></td><td style='padding:8px;border:1px solid #ddd'>{{quantity}} shares</td></tr>" +
                "<tr style='background:#f4f4f4'><td style='padding:8px;border:1px solid #ddd'><b>Price</b></td><td style='padding:8px;border:1px solid #ddd'>₹{{price}}</td></tr>" +
                "<tr><td style='padding:8px;border:1px solid #ddd'><b>Total</b></td><td style='padding:8px;border:1px solid #ddd'>₹{{totalAmount}}</td></tr>" +
                "<tr style='background:#f4f4f4'><td style='padding:8px;border:1px solid #ddd'><b>Order ID</b></td><td style='padding:8px;border:1px solid #ddd'>{{orderId}}</td></tr>" +
                "<tr><td style='padding:8px;border:1px solid #ddd'><b>Date</b></td><td style='padding:8px;border:1px solid #ddd'>{{currentDate}}</td></tr>" +
                "</table>" +
                "{{#if charges}}<h3>Charges Breakdown</h3>" +
                "<p>Brokerage: ₹{{brokerage}} | STT: ₹{{stt}} | Total: ₹{{totalCharges}}</p>{{/if}}" +
                "<p style='color:#888;font-size:12px'>This is a system-generated message from {{appName}}. Do not reply.</p>" +
                "</body></html>",
                "userName, ticker, quantity, price, totalAmount, orderId, charges, brokerage, stt, totalCharges"),

            email("email.txnx001.hi", "TXNX-001", "hi",
                "ऑर्डर पुष्टि — {{ticker}} खरीद {{quantity}} शेयर @ ₹{{price}}",
                "प्रिय {{userName}},\n\nआपका खरीद आदेश सफलतापूर्वक निष्पादित हो गया है।\n\nआदेश विवरण:\n  टिकर     : {{ticker}}\n  मात्रा   : {{quantity}} शेयर\n  मूल्य    : ₹{{price}}\n  कुल      : ₹{{totalAmount}}\n  ऑर्डर ID  : {{orderId}}\n  दिनांक   : {{currentDate}}\n\n{{appName}} के साथ ट्रेडिंग के लिए धन्यवाद।",
                null,
                "userName, ticker, quantity, price, totalAmount, orderId"),

            // ----------------------------------------------------------------
            // EMAIL Templates — RISK-001 Margin Call
            // ----------------------------------------------------------------
            email("email.risk001.en", "RISK-001", "en",
                "⚠️ URGENT: Margin Call Notice — Action Required by {{deadline}}",
                "Dear {{userName}},\n\nThis is an urgent notice that your trading account has triggered a MARGIN CALL.\n\nAccount Details:\n  Account ID      : {{accountId}}\n  Current Margin  : ₹{{currentMargin}}\n  Required Margin : ₹{{requiredAmount}}\n  Shortfall       : ₹{{shortfall}}\n  Deadline        : {{deadline}}\n\nPlease add funds immediately to avoid automatic position liquidation.\n\nAt-risk positions:\n{{#each atRiskPositions}}\n  - {{this}}\n{{/each}}\n\nThis is a regulatory notice from {{appName}}.",
                "<html><body style='font-family:Arial,sans-serif'>" +
                "<div style='background:#ff4444;color:white;padding:16px;border-radius:4px'>" +
                "<h2>⚠️ URGENT: Margin Call Notice</h2></div>" +
                "<p>Dear <strong>{{userName}}</strong>,</p>" +
                "<p>Your trading account has triggered a <strong>MARGIN CALL</strong>. Immediate action is required.</p>" +
                "<table style='border-collapse:collapse;width:100%'>" +
                "<tr style='background:#fff3cd'><td style='padding:8px;border:1px solid #ffc107'><b>Shortfall</b></td><td style='padding:8px;border:1px solid #ffc107;color:#dc3545;font-weight:bold'>₹{{shortfall}}</td></tr>" +
                "<tr><td style='padding:8px;border:1px solid #ddd'><b>Deadline</b></td><td style='padding:8px;border:1px solid #ddd;color:#dc3545'>{{deadline}}</td></tr>" +
                "<tr style='background:#f4f4f4'><td style='padding:8px;border:1px solid #ddd'><b>Account ID</b></td><td style='padding:8px;border:1px solid #ddd'>{{accountId}}</td></tr>" +
                "</table>" +
                "<p style='color:#888;font-size:12px'>Regulatory notice — {{appName}} | {{currentDate}}</p>" +
                "</body></html>",
                "userName, accountId, currentMargin, requiredAmount, shortfall, deadline, atRiskPositions"),

            email("email.risk001.hi", "RISK-001", "hi",
                "⚠️ अत्यावश्यक: मार्जिन कॉल सूचना — {{deadline}} तक कार्रवाई आवश्यक",
                "प्रिय {{userName}},\n\nआपके ट्रेडिंग खाते में मार्जिन कॉल आया है।\n\nखाता विवरण:\n  खाता ID        : {{accountId}}\n  वर्तमान मार्जिन : ₹{{currentMargin}}\n  आवश्यक मार्जिन  : ₹{{requiredAmount}}\n  कमी             : ₹{{shortfall}}\n  अंतिम समय       : {{deadline}}\n\nकृपया तुरंत फंड जोड़ें।\n\n{{appName}} से एक नियामक सूचना।",
                null,
                "userName, accountId, currentMargin, requiredAmount, shortfall, deadline"),

            // ----------------------------------------------------------------
            // EMAIL Templates — REGX-003 Contract Note
            // ----------------------------------------------------------------
            email("email.regx003.en", "REGX-003", "en",
                "Contract Note — {{contractDate}} | {{orderId}}",
                "Dear {{userName}},\n\nPlease find attached your contract note for the trades executed on {{contractDate}}.\n\nSummary:\n  Total Trades  : {{totalTrades}}\n  Total Turnover: ₹{{totalTurnover}}\n  Net P&L       : ₹{{netPnl}}\n\nThis is a regulatory document. Please retain for your records.\n\n{{appName}} | SEBI Registered",
                "<html><body style='font-family:Arial,sans-serif'>" +
                "<h2 style='color:#333'>📄 Contract Note</h2>" +
                "<p>Dear <strong>{{userName}}</strong>,</p>" +
                "<p>Your contract note for <strong>{{contractDate}}</strong> is ready.</p>" +
                "<table style='border-collapse:collapse;width:100%'>" +
                "<tr style='background:#e8f4fd'><td style='padding:8px;border:1px solid #bee5eb'><b>Total Trades</b></td><td style='padding:8px;border:1px solid #bee5eb'>{{totalTrades}}</td></tr>" +
                "<tr><td style='padding:8px;border:1px solid #ddd'><b>Total Turnover</b></td><td style='padding:8px;border:1px solid #ddd'>₹{{totalTurnover}}</td></tr>" +
                "<tr style='background:#e8f4fd'><td style='padding:8px;border:1px solid #bee5eb'><b>Net P&amp;L</b></td><td style='padding:8px;border:1px solid #bee5eb'>₹{{netPnl}}</td></tr>" +
                "</table>" +
                "<p style='font-size:12px;color:#666'>This is a SEBI-mandated regulatory document. Retain for your records.</p>" +
                "</body></html>",
                "userName, contractDate, orderId, totalTrades, totalTurnover, netPnl"),

            // ----------------------------------------------------------------
            // PUSH Templates — RISK-001 Margin Call
            // ----------------------------------------------------------------
            push("push.risk001.en", "RISK-001", "en",
                "⚠️ Margin Call — Action Required",
                "Add ₹{{requiredAmount}} to your account by {{deadline}} to avoid position liquidation."),

            push("push.risk001.hi", "RISK-001", "hi",
                "⚠️ मार्जिन कॉल — तुरंत कार्रवाई करें",
                "₹{{requiredAmount}} {{deadline}} तक जोड़ें, अन्यथा पोजीशन बंद होंगी।"),

            push("push.risk001.mr", "RISK-001", "mr",
                "⚠️ मार्जिन कॉल — त्वरित कार्रवाई",
                "₹{{requiredAmount}} {{deadline}} पर्यंत जमा करा."),

            push("push.risk001.ta", "RISK-001", "ta",
                "⚠️ மார்ஜின் கால் — உடனடி நடவடிக்கை",
                "₹{{requiredAmount}} ஐ {{deadline}} க்குள் செலுத்துங்கள்."),

            push("push.risk001.te", "RISK-001", "te",
                "⚠️ మార్జిన్ కాల్ — తక్షణ చర్య",
                "₹{{requiredAmount}} ని {{deadline}} లోపల జమ చేయండి."),

            // ----------------------------------------------------------------
            // PUSH Templates — TXNX-001 Buy Order
            // ----------------------------------------------------------------
            push("push.txnx001.en", "TXNX-001", "en",
                "✅ BUY Order Executed — {{ticker}}",
                "{{quantity}} shares of {{ticker}} bought at ₹{{price}}. Order: {{orderId}}"),

            push("push.txnx001.hi", "TXNX-001", "hi",
                "✅ खरीद आदेश निष्पादित — {{ticker}}",
                "{{quantity}} {{ticker}} शेयर ₹{{price}} पर खरीदे। ऑर्डर: {{orderId}}"),

            push("push.txnx001.mr", "TXNX-001", "mr",
                "✅ खरेदी आदेश पूर्ण — {{ticker}}",
                "{{quantity}} {{ticker}} शेअर्स ₹{{price}} ला खरेदी. ऑर्डर: {{orderId}}"),

            push("push.txnx001.ta", "TXNX-001", "ta",
                "✅ வாங்கும் ஆணை நிறைவேற்றப்பட்டது — {{ticker}}",
                "{{quantity}} {{ticker}} பங்குகள் ₹{{price}} விலையில் வாங்கப்பட்டன. ஆணை: {{orderId}}"),

            push("push.txnx001.te", "TXNX-001", "te",
                "✅ కొనుగోలు ఆర్డర్ పూర్తైంది — {{ticker}}",
                "{{quantity}} {{ticker}} షేర్లు ₹{{price}} వద్ద కొన్నారు. ఆర్డర్: {{orderId}}"),

            // ----------------------------------------------------------------
            // PUSH Templates — SIPX-001 SIP Reminder
            // ----------------------------------------------------------------
            push("push.sipx001.en", "SIPX-001", "en",
                "📅 SIP Reminder — {{sipName}}",
                "Your SIP of ₹{{sipAmount}} in {{fundName}} is scheduled for {{sipDate}}."),

            push("push.sipx001.hi", "SIPX-001", "hi",
                "📅 SIP रिमाइंडर — {{sipName}}",
                "{{fundName}} में ₹{{sipAmount}} का SIP {{sipDate}} को होगा।"),

            push("push.sipx001.mr", "SIPX-001", "mr",
                "📅 SIP आठवण — {{sipName}}",
                "{{fundName}} मध्ये ₹{{sipAmount}} चा SIP {{sipDate}} रोजी होणार आहे."),

            push("push.sipx001.ta", "SIPX-001", "ta",
                "📅 SIP நினைவூட்டல் — {{sipName}}",
                "{{fundName}} இல் ₹{{sipAmount}} SIP {{sipDate}} அன்று திட்டமிடப்பட்டுள்ளது."),

            push("push.sipx001.te", "SIPX-001", "te",
                "📅 SIP రిమైండర్ — {{sipName}}",
                "{{fundName}} లో ₹{{sipAmount}} SIP {{sipDate}} న షెడ్యూల్ చేయబడింది."),

            // ----------------------------------------------------------------
            // WHATSAPP Templates — SIPX-001 SIP Reminder
            // ----------------------------------------------------------------
            whatsapp("wa.sipx001.en", "SIPX-001", "en",
                null,
                "Hello {{userName}} 👋\n\nThis is a reminder for your upcoming SIP:\n\n📌 *Fund:* {{fundName}}\n💰 *Amount:* ₹{{sipAmount}}\n📅 *Date:* {{sipDate}}\n🔖 *SIP ID:* {{sipId}}\n\n{{#if lowBalance}}⚠️ Your linked bank account balance may be insufficient. Please ensure adequate funds are available.{{/if}}\n\nManage your SIPs: {{manageUrl}}\n\n— {{appName}}",
                null,
                "userName, fundName, sipAmount, sipDate, sipId, lowBalance, manageUrl"),

            whatsapp("wa.sipx001.hi", "SIPX-001", "hi",
                null,
                "नमस्ते {{userName}} 👋\n\nआपके आगामी SIP की याद दिलाने के लिए:\n\n📌 *फंड:* {{fundName}}\n💰 *राशि:* ₹{{sipAmount}}\n📅 *तारीख:* {{sipDate}}\n🔖 *SIP ID:* {{sipId}}\n\n{{#if lowBalance}}⚠️ आपके बैंक खाते में पर्याप्त राशि नहीं है। कृपया सुनिश्चित करें।{{/if}}\n\n— {{appName}}",
                null,
                "userName, fundName, sipAmount, sipDate, sipId, lowBalance"),

            whatsapp("wa.sipx001.mr", "SIPX-001", "mr",
                null,
                "नमस्कार {{userName}} 👋\n\nतुमच्या येणाऱ्या SIP ची आठवण:\n\n📌 *फंड:* {{fundName}}\n💰 *रक्कम:* ₹{{sipAmount}}\n📅 *दिनांक:* {{sipDate}}\n🔖 *SIP ID:* {{sipId}}\n\n— {{appName}}",
                null,
                "userName, fundName, sipAmount, sipDate, sipId"),

            whatsapp("wa.sipx001.ta", "SIPX-001", "ta",
                null,
                "வணக்கம் {{userName}} 👋\n\nவரவிருக்கும் SIP நினைவூட்டல்:\n\n📌 *நிதி:* {{fundName}}\n💰 *தொகை:* ₹{{sipAmount}}\n📅 *தேதி:* {{sipDate}}\n🔖 *SIP ID:* {{sipId}}\n\n— {{appName}}",
                null,
                "userName, fundName, sipAmount, sipDate, sipId"),

            whatsapp("wa.sipx001.te", "SIPX-001", "te",
                null,
                "నమస్కారం {{userName}} 👋\n\nమీ రాబోయే SIP గుర్తు చేయడానికి:\n\n📌 *ఫండ్:* {{fundName}}\n💰 *మొత్తం:* ₹{{sipAmount}}\n📅 *తేదీ:* {{sipDate}}\n🔖 *SIP ID:* {{sipId}}\n\n— {{appName}}",
                null,
                "userName, fundName, sipAmount, sipDate, sipId"),

            // ----------------------------------------------------------------
            // WHATSAPP Templates — TXNX-001 Buy Order
            // ----------------------------------------------------------------
            whatsapp("wa.txnx001.en", "TXNX-001", "en",
                null,
                "Hello {{userName}} 👋\n\n✅ *Trade Executed Successfully*\n\n🔖 *Type:* BUY\n📈 *Stock:* {{ticker}}\n📦 *Qty:* {{quantity}} shares\n💹 *Price:* ₹{{price}}\n💰 *Total:* ₹{{totalAmount}}\n🗓 *Date:* {{currentDate}}\n🔢 *Order ID:* {{orderId}}\n\nHappy investing! 🚀\n— {{appName}}",
                null,
                "userName, ticker, quantity, price, totalAmount, orderId"),

            whatsapp("wa.txnx001.hi", "TXNX-001", "hi",
                null,
                "नमस्ते {{userName}} 👋\n\n✅ *ट्रेड सफलतापूर्वक हुआ*\n\n🔖 *प्रकार:* खरीद\n📈 *स्टॉक:* {{ticker}}\n📦 *मात्रा:* {{quantity}} शेयर\n💹 *मूल्य:* ₹{{price}}\n💰 *कुल:* ₹{{totalAmount}}\n🔢 *ऑर्डर ID:* {{orderId}}\n\n— {{appName}}",
                null,
                "userName, ticker, quantity, price, totalAmount, orderId"),

            // ----------------------------------------------------------------
            // IN_APP Templates — TXNX-001 Buy Order
            // ----------------------------------------------------------------
            inApp("inapp.txnx001.en", "TXNX-001", "en",
                "BUY Order Executed — {{ticker}}",
                "Your order to buy {{quantity}} shares of {{ticker}} at ₹{{price}} was executed successfully. Order ID: {{orderId}}.",
                null,
                "ticker, quantity, price, orderId"),

            inApp("inapp.txnx001.hi", "TXNX-001", "hi",
                "खरीद आदेश पूरा — {{ticker}}",
                "{{ticker}} के {{quantity}} शेयर ₹{{price}} पर खरीदने का आदेश पूरा हुआ। ऑर्डर ID: {{orderId}}।",
                null,
                "ticker, quantity, price, orderId"),

            inApp("inapp.txnx001.mr", "TXNX-001", "mr",
                "खरेदी आदेश पूर्ण — {{ticker}}",
                "{{ticker}} चे {{quantity}} शेअर्स ₹{{price}} ला खरेदी पूर्ण झाली. ऑर्डर ID: {{orderId}}.",
                null,
                "ticker, quantity, price, orderId"),

            inApp("inapp.txnx001.ta", "TXNX-001", "ta",
                "வாங்கும் ஆணை நிறைவேற்றப்பட்டது — {{ticker}}",
                "{{ticker}} பங்குகள் {{quantity}} எண்ணிக்கையில் ₹{{price}} விலையில் வாங்கப்பட்டன. ஆணை ID: {{orderId}}.",
                null,
                "ticker, quantity, price, orderId"),

            inApp("inapp.txnx001.te", "TXNX-001", "te",
                "కొనుగోలు ఆర్డర్ పూర్తైంది — {{ticker}}",
                "{{ticker}} {{quantity}} షేర్లు ₹{{price}} వద్ద విజయవంతంగా కొనుగోలు చేయబడ్డాయి. ఆర్డర్ ID: {{orderId}}.",
                null,
                "ticker, quantity, price, orderId"),

            // ----------------------------------------------------------------
            // IN_APP Templates — RISK-001 Margin Call
            // ----------------------------------------------------------------
            inApp("inapp.risk001.en", "RISK-001", "en",
                "⚠️ Margin Call Alert",
                "Your account requires an additional ₹{{requiredAmount}} by {{deadline}}. Failure to add funds may result in automatic liquidation of positions.",
                null,
                "requiredAmount, deadline"),

            inApp("inapp.risk001.hi", "RISK-001", "hi",
                "⚠️ मार्जिन कॉल अलर्ट",
                "{{deadline}} तक ₹{{requiredAmount}} जोड़ें। अन्यथा पोजीशन स्वत: बंद हो सकती है।",
                null,
                "requiredAmount, deadline"),

            // ----------------------------------------------------------------
            // IN_APP Templates — SIPX-001 SIP Reminder
            // ----------------------------------------------------------------
            inApp("inapp.sipx001.en", "SIPX-001", "en",
                "📅 SIP Due: {{fundName}}",
                "Your SIP of ₹{{sipAmount}} in {{fundName}} is due on {{sipDate}}. Ensure your bank account has sufficient balance.",
                null,
                "fundName, sipAmount, sipDate"),

            inApp("inapp.sipx001.hi", "SIPX-001", "hi",
                "📅 SIP देय: {{fundName}}",
                "{{fundName}} में ₹{{sipAmount}} का SIP {{sipDate}} को होगा। अपने खाते में पर्याप्त राशि रखें।",
                null,
                "fundName, sipAmount, sipDate")
        );
    }

    // =========================================================================
    // Builder helpers
    // =========================================================================

    private NotificationTemplate sms(String name, String eventType, String locale,
                                     String subject, String body, String html, String vars) {
        return base(name, eventType, locale, ChannelType.SMS, subject, body, html, vars);
    }

    private NotificationTemplate email(String name, String eventType, String locale,
                                       String subject, String body, String html, String vars) {
        return base(name, eventType, locale, ChannelType.EMAIL, subject, body, html, vars);
    }

    private NotificationTemplate push(String name, String eventType, String locale,
                                      String subject, String body) {
        return base(name, eventType, locale, ChannelType.PUSH, subject, body, null, "");
    }

    private NotificationTemplate whatsapp(String name, String eventType, String locale,
                                          String subject, String body, String html, String vars) {
        return base(name, eventType, locale, ChannelType.WHATSAPP, subject, body, html, vars);
    }

    private NotificationTemplate inApp(String name, String eventType, String locale,
                                       String subject, String body, String html, String vars) {
        return base(name, eventType, locale, ChannelType.IN_APP, subject, body, html, vars);
    }

    private NotificationTemplate base(String name, String eventType, String locale,
                                      ChannelType channel, String subject, String body,
                                      String html, String vars) {
        return NotificationTemplate.builder()
                .name(name)
                .description(channel.name() + " template for " + eventType + " [" + locale.toUpperCase() + "]")
                .channelType(channel)
                .eventType(eventType)
                .locale(locale)
                .subjectTemplate(subject)
                .bodyTemplate(body)
                .htmlBodyTemplate(html)
                .availableVariables(vars)
                .active(true)
                .createdBy("system")
                .build();
    }
}
