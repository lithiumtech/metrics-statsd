package com.lithium.dog.event;

import com.bealetech.metrics.reporting.DefaultSocketProvider;
import com.bealetech.metrics.reporting.UDPSocketProvider;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A blocking client for sending event messages to dogstatsd (http://docs.datadoghq.com/guides/dogstatsd/#events).
 */
public class DataDogEventClient {

    private final UDPSocketProvider socketProvider;
    private final Set<String> constantTags;
    private final ErrorHandler errorHandler;

    public DataDogEventClient(String host,
                              int port,
                              Set<String> constantTags,
                              ErrorHandler errorHandler) {
        this.socketProvider = new DefaultSocketProvider(host, port);
        this.constantTags = constantTags;
        this.errorHandler = errorHandler;
    }

    public EventMessage sendEvent(String title, String message, String... tags) {
        return sendEvent(title, message, null, null, null, tags);
    }

    /**
     * Emit an event to be delivered to DogStatsD over UDP.
     */
    public EventMessage sendEvent(String title,
                          String message,
                          String aggregationKey,
                          Priority priority,
                          AlertType alterType,
                          String... tags) {

        final Set<String> combinedTags = (tags == null) ? new LinkedHashSet<String>() : new LinkedHashSet<>(Arrays.asList(tags));
        if (constantTags != null) {
            combinedTags.addAll(constantTags);
        }

        final EventMessage msg = new EventMessage(title, message, aggregationKey, priority, alterType, combinedTags);
        final byte[] sendData = msg.toString().getBytes();

        try (DatagramSocket socket = socketProvider.get()) {
            final DatagramPacket sendPacket = socketProvider.newPacket(sendData);
            socket.send(sendPacket);
        } catch (Exception e) {
            errorHandler.handle(e);
            return null;
        }

        return msg;
    }

}
