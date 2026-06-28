package com.notificationengine.channel;

import com.notificationengine.entity.NotificationEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Slack channel processor.
 *
 * <p>In simulation mode, logs the Slack message.
 * Configure Slack Bot token and webhook URL for real delivery.</p>
 */
@Slf4j
@Component
public class SlackChannelProcessor implements NotificationChannelProcessor {

    @Value("${app.channel.slack.simulation:true}")
    private boolean simulationMode;

    @Override
    public NotificationEntity.ChannelType getChannelType() {
        return NotificationEntity.ChannelType.SLACK;
    }

    @Override
    public String send(NotificationEntity notification) {
        if (simulationMode) {
            log.info("[SLACK SIMULATION] Channel/User={} Text='{}'",
                    notification.getRecipientAddress(),
                    notification.getBody());
            return "sim-slack-" + System.currentTimeMillis();
        }
        // TODO: Integrate with Slack Web API (chat.postMessage)
        throw new UnsupportedOperationException("Real Slack provider not configured.");
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
