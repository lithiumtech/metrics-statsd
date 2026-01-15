package com.bealetech.metrics.reporting;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Sampling;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

public class StatsdReporter extends ScheduledReporter {

    private static final Logger LOG = LoggerFactory.getLogger(StatsdReporter.class);

    public static enum StatType { COUNTER, TIMER, GAUGE }

    protected static final int MAX_UDPDATAGRAM_LENGTH = 512; // In reality, usually closer to 1500

    /**
     * Returns a new {@link Builder} for {@link StatsdReporter}.
     *
     * @param registry the registry to report
     * @param hostname The destination hostname for UDP packets
     * @param port The destination port for UDP packets
     * @return a {@link Builder} instance for a {@link StatsdReporter}
     */
    public static Builder forRegistry(MetricRegistry registry, String hostname, int port) {
        return new Builder(registry, new DefaultSocketProvider(hostname, port));
    }

    //For testing
    public static Builder forRegistry(MetricRegistry registry, UDPSocketProvider udpSocketProvider) {
        return new Builder(registry, udpSocketProvider);
    }

    /**
     * A builder for {@link StatsdReporter} instances. Defaults to using the default locale, converting
     * rates to events/second, converting durations to milliseconds, and not filtering metrics.
     */
    public static class Builder {
        private final MetricRegistry registry;
        private final UDPSocketProvider udpSocketProvider;
        private Locale locale;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;
        private Clock clock;
        private MetricFilter filter;
        private String prefix;
        private String appendTag;
        private boolean minimizeMetrics;
        private boolean send98thPercentile;

        private Builder(MetricRegistry registry, UDPSocketProvider udpSocketProvider) {
            this.registry = registry;
            this.udpSocketProvider = udpSocketProvider;
            this.locale = Locale.getDefault();
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.clock = Clock.defaultClock();
            this.filter = MetricFilter.ALL;
            this.minimizeMetrics = true;
            this.send98thPercentile = false;
        }

        /**
         * Format numbers for the given {@link Locale}.
         *
         * @param locale a {@link Locale}
         * @return {@code this}
         */
        public Builder formatFor(Locale locale) {
            this.locale = locale;
            return this;
        }

        /**
         * Convert rates to the given time unit.
         *
         * @param rateUnit a unit of time
         * @return {@code this}
         */
        public Builder convertRatesTo(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        /**
         * Convert durations to the given time unit.
         *
         * @param durationUnit a unit of time
         * @return {@code this}
         */
        public Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        /**
         * Use the given {@link Clock} instance for the time.
         *
         * @param clock a {@link Clock} instance
         * @return {@code this}
         */
        public Builder withClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * Only report metrics which match the given filter.
         *
         * @param filter a {@link MetricFilter}
         * @return {@code this}
         */
        public Builder withFilter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Add a prefix to all reported metrics
         *
         * @param prefix a string prefix to add to all metrics
         * @return {@code this}
         */
        public Builder withPrefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Append a datadog tag to each metric
         *
         * @param appendTag a string tag to append to all metrics for sending to dogstatsd
         * @return {@code this}
         */
        public Builder withAppendTag(String appendTag) {
            this.appendTag = appendTag;
            return this;
        }

        /**
         * Minimize the metrics being sent for histograms and timers
         *
         * @param minimizeMetrics boolean whether or not to minimize metrics being sent.
         * @return {@code this}
         */
        public Builder withMinimizeMetrics(boolean minimizeMetrics) {
            this.minimizeMetrics = minimizeMetrics;
            return this;
        }

        /**
         * Should the 98th percentile metric be sent?
         *
         * @param send98thPercentile boolean whether or not to send the 98th percentile metric.
         * @return {@code this}
         */
        public Builder withSend98thPercentile(boolean send98thPercentile) {
            this.send98thPercentile = send98thPercentile;
            return this;
        }

        /**
         * Builds a {@link StatsdReporter} with the given properties
         *
         * @return a {@link StatsdReporter}
         */
        public StatsdReporter build() {
            return new StatsdReporter(registry,
                                   udpSocketProvider,
                                   prefix,
                                   appendTag,
                                   locale,
                                   rateUnit,
                                   durationUnit,
                                   clock,
                                   filter,
                                   minimizeMetrics,
                                   send98thPercentile);
        }
    }


    protected final String prefix;
    private final String appendTag;
    protected final MetricFilter filter;
    protected final Locale locale;
    protected final Clock clock;
    protected final UDPSocketProvider socketProvider;
    private final boolean minimizeMetrics;
    private final boolean send98thPercentile;

    protected DatagramSocket currentSocket = null;
    protected Writer writer;
    protected ByteArrayOutputStream outputData;

    private boolean prependNewline = false;

    private StatsdReporter(MetricRegistry registry,
                           UDPSocketProvider socketProvider,
                           String prefix,
                           String appendTag,
                           Locale locale,
                           TimeUnit rateUnit,
                           TimeUnit durationUnit,
                           Clock clock,
                           MetricFilter filter,
                           boolean minimizeMetrics,
                           boolean send98thPercentile) {
        super(registry, "statsd-reporter", filter, rateUnit, durationUnit);

        this.socketProvider = socketProvider;
        if (prefix != null) {
            // Pre-append the "." so that we don't need to make anything conditional later.
            this.prefix = prefix + ".";
        } else {
            this.prefix = "";
        }
        this.appendTag = appendTag;
        this.locale = locale;
        this.clock = clock;
        this.filter = filter;
        this.minimizeMetrics = minimizeMetrics;
        this.send98thPercentile = send98thPercentile;

        outputData = new ByteArrayOutputStream();
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges,
                       SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms,
                       SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {

        try {
            currentSocket = this.socketProvider.get();
            resetWriterState();

            for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
                processGauge(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Counter> entry : counters.entrySet()) {
                processCounter(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
                processHistogram(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Meter> entry : meters.entrySet()) {
                processMeter(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Timer> entry : timers.entrySet()) {
                processTimer(entry.getKey(), entry.getValue());
            }

            // Send UDP data
            sendDatagram();
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error writing to statsd", e);
            } else {
                LOG.warn("Error writing to statsd: {}", e.getMessage());
            }
            if (writer != null) {
                try {
                    writer.flush();
                } catch (IOException e1) {
                    LOG.error("Error while flushing writer:", e1);
                }
            }
        } finally {
            if (currentSocket != null) {
                currentSocket.close();
            }
            writer = null;
        }
    }

    private void resetWriterState() {
        outputData.reset();
        prependNewline = false;
        writer = new BufferedWriter(new OutputStreamWriter(this.outputData));
    }

    private void sendDatagram() throws IOException {
        writer.flush();
        if (outputData.size() > 0) { // Don't send an empty datagram
            //LOG.info("Sending datagram: {}", outputData.toString());
            DatagramPacket packet = socketProvider.newPacket(outputData);
            packet.setData(outputData.toByteArray());
            currentSocket.send(packet);
        }
        resetWriterState();
    }

    public void processMeter(String name, Metered meter) throws Exception {
        sendInt(name + ".count", StatType.GAUGE, meter.getCount());
        if (!minimizeMetrics) {
            sendFloat(name + ".meanRate", StatType.TIMER, meter.getMeanRate());
        }
        sendFloat(name + ".1MinuteRate", StatType.TIMER, meter.getOneMinuteRate());
        if (!minimizeMetrics) {
            sendFloat(name + ".5MinuteRate", StatType.TIMER, meter.getFiveMinuteRate());
            sendFloat(name + ".15MinuteRate", StatType.TIMER, meter.getFifteenMinuteRate());
        }
    }

    public void processCounter(String name, Counter counter) throws Exception {
        sendInt(name + ".count", StatType.GAUGE, counter.getCount());
    }

    public void processHistogram(String name, Histogram histogram) throws Exception {
        sendSummarizable(name, histogram);
        sendSampling(name, histogram);
    }

    public void processTimer(String name, Timer timer) throws Exception {
        processMeter(name, timer);
        sendSummarizable(name, timer);
        sendSampling(name, timer);
    }

    public void processGauge(String name, Gauge<?> gauge) throws Exception {
        //unfortunately the weakly typed Gauge can send some values that will break our statsd impl
        if (gauge.getValue() instanceof Number) {
            sendObj(name + ".count", StatType.GAUGE, gauge.getValue());
        } else {
            LOG.debug("Ignoring non numeric gauge " + name);
        }
    }

    protected void sendSummarizable(String sanitizedName, Sampling metric) throws IOException {
        final Snapshot snapshot = metric.getSnapshot();

        if (!minimizeMetrics) {
            sendFloat(sanitizedName + ".min", StatType.TIMER, snapshot.getMin());
            sendFloat(sanitizedName + ".max", StatType.TIMER, snapshot.getMax());
        }
        sendFloat(sanitizedName + ".mean", StatType.TIMER, snapshot.getMean());
        if (!minimizeMetrics) {
            sendFloat(sanitizedName + ".stddev", StatType.TIMER, snapshot.getStdDev());
        }
    }

    protected void sendSampling(String sanitizedName, Sampling metric) throws IOException {
        if (minimizeMetrics && !send98thPercentile) {
            return;
        }

        final Snapshot snapshot = metric.getSnapshot();
        if (minimizeMetrics) {
            sendFloat(sanitizedName + ".98percentile", StatType.TIMER, snapshot.get98thPercentile());
        } else {
            sendFloat(sanitizedName + ".median", StatType.TIMER, snapshot.getMedian());
            sendFloat(sanitizedName + ".75percentile", StatType.TIMER, snapshot.get75thPercentile());
            sendFloat(sanitizedName + ".95percentile", StatType.TIMER, snapshot.get95thPercentile());
            sendFloat(sanitizedName + ".98percentile", StatType.TIMER, snapshot.get98thPercentile());
            sendFloat(sanitizedName + ".99percentile", StatType.TIMER, snapshot.get99thPercentile());
            sendFloat(sanitizedName + ".999percentile", StatType.TIMER, snapshot.get999thPercentile());
        }

    }


    protected void sendInt(String name, StatType statType, long value) {
        sendData(name, String.format(locale, "%d", value), statType);
    }

    protected void sendFloat(String name, StatType statType, double value) {
        sendData(name, String.format(locale, "%2.2f", value), statType);
    }

    protected void sendObj(String name, StatType statType, Object value) {
        sendData(name, String.format(locale, "%s", value), statType);
    }

    protected String sanitizeString(String s) {
        return s.replace(' ', '-');
    }

    private boolean isNumeric(String str){
        try {
            double ignore = Double.parseDouble(str);
        } catch(NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    protected void sendData(String name, String value, StatType statType) {
        if (!isNumeric(value)) {
            return;
        }

        String statTypeStr = "";
        switch (statType) {
            case COUNTER:
                statTypeStr = "c";
                break;
            case GAUGE:
                statTypeStr = "g";
                break;
            case TIMER:
                statTypeStr = minimizeMetrics ? "g" : "ms";
                break;
        }

        try {
            if (prependNewline) {
                writer.write("\n");
            }
            if (!prefix.isEmpty()) {
                writer.write(prefix);
            }
            writer.write(sanitizeString(name));
            writer.write(":");
            writer.write(value);
            writer.write("|");
            writer.write(statTypeStr);
            if (appendTag != null) {
                writer.write("|#");
                writer.write(appendTag);
            }
            prependNewline = true;
            writer.flush();

            if (outputData.size() > MAX_UDPDATAGRAM_LENGTH) {
                // Need to send our UDP packet now before it gets too big.
                sendDatagram();
            }
        } catch (IOException e) {
            LOG.error("Error sending to Graphite:", e);
        }
    }

}