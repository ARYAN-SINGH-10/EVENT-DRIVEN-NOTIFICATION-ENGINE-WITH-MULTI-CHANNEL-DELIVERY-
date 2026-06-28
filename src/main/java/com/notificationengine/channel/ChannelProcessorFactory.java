package com.notificationengine.channel;

import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.exception.ChannelUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory that resolves the appropriate {@link NotificationChannelProcessor} for a given channel type.
 *
 * <p>All processors are auto-discovered via Spring's dependency injection.</p>
 */
@Slf4j
@Component
public class ChannelProcessorFactory {

    private final Map<ChannelType, NotificationChannelProcessor> processors;

    public ChannelProcessorFactory(List<NotificationChannelProcessor> processorList) {
        this.processors = processorList.stream()
                .collect(Collectors.toMap(
                        NotificationChannelProcessor::getChannelType,
                        Function.identity()
                ));
        log.info("Registered channel processors: {}", processors.keySet());
    }

    /**
     * Returns the processor for the given channel type.
     *
     * @throws ChannelUnavailableException if no processor is registered or the channel is unavailable
     */
    public NotificationChannelProcessor getProcessor(ChannelType channelType) {
        NotificationChannelProcessor processor = processors.get(channelType);
        if (processor == null) {
            throw new ChannelUnavailableException(channelType.name());
        }
        if (!processor.isAvailable()) {
            throw new ChannelUnavailableException(channelType.name());
        }
        return processor;
    }

    /**
     * Returns true if a processor exists for the given channel type and is available.
     */
    public boolean isChannelAvailable(ChannelType channelType) {
        NotificationChannelProcessor processor = processors.get(channelType);
        return processor != null && processor.isAvailable();
    }

    /**
     * Returns all registered channel types.
     */
    public java.util.Set<ChannelType> getAvailableChannels() {
        return processors.entrySet().stream()
                .filter(e -> e.getValue().isAvailable())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
