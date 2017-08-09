/**
 * Copyright (C) 2012-2013 Sean Laurent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.bealetech.metrics.reporting;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StatsdReporterTest {

    private static final String TAG = "env:prod,container:10";

    protected DatagramSocket socket;

    protected StatsdReporter createReporter(boolean minimizeMetrics, String appendTag) throws Exception {
        final UDPSocketProvider provider = mock(UDPSocketProvider.class);
        //socket = mock(DatagramSocket.class, withSettings().verboseLogging());
        socket = mock(DatagramSocket.class);

        when(provider.get()).thenReturn(socket);
        when(provider.newPacket(any(ByteArrayOutputStream.class))).thenReturn(new DatagramPacket(new byte[65536], 65536), new DatagramPacket(new byte[65536], 65536));

        return StatsdReporter.forRegistry(new MetricRegistry(), provider)
                                                      .withPrefix("prefix")
                                                      .withFilter(MetricFilter.ALL)
                                                      .withMinimizeMetrics(minimizeMetrics)
                                                      .withAppendTag(appendTag)
                                                      .build();
    }

    @Test
    public void reportsGaugeValues() throws Exception {
        final Gauge gauge = mock(Gauge.class);
        when(gauge.getValue()).thenReturn(3);

        createReporter(false, null).report(map("mygaugename", gauge),
                                this.<Counter>map(),
                                this.<Histogram>map(),
                                this.<Meter>map(),
                                this.<Timer>map());

        assertOutput(expectedGaugeResult("3"), 1);
    }

    @Test
    public void ignoreNonNumberGuageValue() throws Exception {
        final Gauge gaugeBad = mock(Gauge.class);
        when(gaugeBad.getValue()).thenReturn(Collections.EMPTY_LIST);
        final Gauge goodGuage = mock(Gauge.class);
        when(goodGuage.getValue()).thenReturn(3);

        final SortedMap<String, Gauge> gaugeMap = new TreeMap<>();
        gaugeMap.put("mygaugename", goodGuage);
        gaugeMap.put("badgaugename", gaugeBad);

        createReporter(false, null).report(gaugeMap,
                                                                     this.<Counter>map(),
                                                                     this.<Histogram>map(),
                                                                     this.<Meter>map(),
                                                                     this.<Timer>map());

        assertOutput(expectedGaugeResult("3"), 1);
    }

    @Test
    public void reportsGaugeValuesMinimized() throws Exception {
        final Gauge gauge = mock(Gauge.class);
        when(gauge.getValue()).thenReturn(3);

        createReporter(true, TAG).report(map("mygaugename", gauge),
                                     this.<Counter>map(),
                                     this.<Histogram>map(),
                                     this.<Meter>map(),
                                     this.<Timer>map());

        assertOutput(expectedGaugeResultMinimizedWithTag("3"), 1);
    }

    @Test
    public void reportsCounterValues() throws Exception {
        final Counter counter = mock(Counter.class);
        when(counter.getCount()).thenReturn(100L);

        createReporter(false, null).report(this.<Gauge>map(),
                                map("test.counter", counter),
                                this.<Histogram>map(),
                                this.<Meter>map(),
                                this.<Timer>map());

        assertOutput(expectedCounterResult(100L), 1);
    }

    @Test
    public void reportsCounterValuesMinized() throws Exception {
        final Counter counter = mock(Counter.class);
        when(counter.getCount()).thenReturn(100L);

        createReporter(true, TAG).report(this.<Gauge>map(),
                                           map("test.counter", counter),
                                           this.<Histogram>map(),
                                           this.<Meter>map(),
                                           this.<Timer>map());

        assertOutput(expectedCounterResultMinimizedWithTag(100L), 1);
    }

    @Test
    public void reportsHistogramValues() throws Exception {
        final Histogram histogram = mock(Histogram.class);
        when(histogram.getCount()).thenReturn(1L);

        final Snapshot snapshot = mock(Snapshot.class);
        when(snapshot.getMax()).thenReturn(2L);
        when(snapshot.getMean()).thenReturn(3.0);
        when(snapshot.getMin()).thenReturn(4L);
        when(snapshot.getStdDev()).thenReturn(5.0);
        when(snapshot.getMedian()).thenReturn(6.0);
        when(snapshot.get75thPercentile()).thenReturn(0.75);
        when(snapshot.get95thPercentile()).thenReturn(0.95);
        when(snapshot.get98thPercentile()).thenReturn(0.98);
        when(snapshot.get99thPercentile()).thenReturn(0.99);
        when(snapshot.get999thPercentile()).thenReturn(1.00);

        when(histogram.getSnapshot()).thenReturn(snapshot);

        createReporter(false, null).report(this.<Gauge>map(),
                                this.<Counter>map(),
                                map("test.histogram", histogram),
                                this.<Meter>map(),
                                this.<Timer>map());

        assertOutput(expectedHistogramResult(), 1);
    }

    @Test
    public void reportsHistogramValuesMinimized() throws Exception {
        final Histogram histogram = mock(Histogram.class);
        when(histogram.getCount()).thenReturn(1L);

        final Snapshot snapshot = mock(Snapshot.class);
        when(snapshot.getMax()).thenReturn(2L);
        when(snapshot.getMean()).thenReturn(3.0);
        when(snapshot.getMin()).thenReturn(4L);
        when(snapshot.getStdDev()).thenReturn(5.0);
        when(snapshot.getMedian()).thenReturn(6.0);
        when(snapshot.get75thPercentile()).thenReturn(0.75);
        when(snapshot.get95thPercentile()).thenReturn(0.95);
        when(snapshot.get98thPercentile()).thenReturn(0.98);
        when(snapshot.get99thPercentile()).thenReturn(0.99);
        when(snapshot.get999thPercentile()).thenReturn(1.00);

        when(histogram.getSnapshot()).thenReturn(snapshot);

        createReporter(true, TAG).report(this.<Gauge>map(),
                                           this.<Counter>map(),
                                           map("test.histogram", histogram),
                                           this.<Meter>map(),
                                           this.<Timer>map());

        assertOutput(expectedHistogramResultMinimizedWithTag(), 1);
    }

    @Test
    public void reportsMeterValues() throws Exception {
        final Meter meter = mock(Meter.class);
        when(meter.getCount()).thenReturn(1L);
        when(meter.getMeanRate()).thenReturn(2.0);
        when(meter.getOneMinuteRate()).thenReturn(3.0);
        when(meter.getFiveMinuteRate()).thenReturn(4.0);
        when(meter.getFifteenMinuteRate()).thenReturn(5.0);

        createReporter(false, null).report(this.<Gauge>map(),
                                this.<Counter>map(),
                                this.<Histogram>map(),
                                map("test.meter", meter),
                                this.<Timer>map());

        assertOutput(expectedMeterResult(), 1);
    }

    @Test
    public void reportsMeterValuesMinimized() throws Exception {
        final Meter meter = mock(Meter.class);
        when(meter.getCount()).thenReturn(1L);
        when(meter.getMeanRate()).thenReturn(2.0);
        when(meter.getOneMinuteRate()).thenReturn(3.0);
        when(meter.getFiveMinuteRate()).thenReturn(4.0);
        when(meter.getFifteenMinuteRate()).thenReturn(5.0);

        createReporter(true, TAG).report(this.<Gauge>map(),
                                           this.<Counter>map(),
                                           this.<Histogram>map(),
                                           map("test.meter", meter),
                                           this.<Timer>map());

        assertOutput(expectedMeterResultMinimizedWithTag(), 1);
    }

    @Test
    public void reportsTimerValues() throws Exception {
        final Timer timer = mock(Timer.class);
        when(timer.getCount()).thenReturn(1L);
        when(timer.getMeanRate()).thenReturn(2.0);
        when(timer.getOneMinuteRate()).thenReturn(3.0);
        when(timer.getFiveMinuteRate()).thenReturn(4.0);
        when(timer.getFifteenMinuteRate()).thenReturn(5.0);

        final Snapshot snapshot = mock(Snapshot.class);
        when(snapshot.getMax()).thenReturn(2L);
        when(snapshot.getMean()).thenReturn(3.0);
        when(snapshot.getMin()).thenReturn(4L);
        when(snapshot.getStdDev()).thenReturn(5.0);
        when(snapshot.getMedian()).thenReturn(6.0);
        when(snapshot.get75thPercentile()).thenReturn(0.75);
        when(snapshot.get95thPercentile()).thenReturn(0.95);
        when(snapshot.get98thPercentile()).thenReturn(0.98);
        when(snapshot.get99thPercentile()).thenReturn(0.99);
        when(snapshot.get999thPercentile()).thenReturn(1.00);

        when(timer.getSnapshot()).thenReturn(snapshot);

        createReporter(false, null).report(this.<Gauge>map(),
                                this.<Counter>map(),
                                this.<Histogram>map(),
                                this.<Meter>map(),
                                map("test.another.timer", timer));

        assertOutput(expectedTimerResult(), 2);
    }

    @Test
    public void reportsTimerValuesMinimized() throws Exception {
        final Timer timer = mock(Timer.class);
        when(timer.getCount()).thenReturn(1L);
        when(timer.getMeanRate()).thenReturn(2.0);
        when(timer.getOneMinuteRate()).thenReturn(3.0);
        when(timer.getFiveMinuteRate()).thenReturn(4.0);
        when(timer.getFifteenMinuteRate()).thenReturn(5.0);

        final Snapshot snapshot = mock(Snapshot.class);
        when(snapshot.getMax()).thenReturn(2L);
        when(snapshot.getMean()).thenReturn(3.0);
        when(snapshot.getMin()).thenReturn(4L);
        when(snapshot.getStdDev()).thenReturn(5.0);
        when(snapshot.getMedian()).thenReturn(6.0);
        when(snapshot.get75thPercentile()).thenReturn(0.75);
        when(snapshot.get95thPercentile()).thenReturn(0.95);
        when(snapshot.get98thPercentile()).thenReturn(0.98);
        when(snapshot.get99thPercentile()).thenReturn(0.99);
        when(snapshot.get999thPercentile()).thenReturn(1.00);

        when(timer.getSnapshot()).thenReturn(snapshot);

        createReporter(true, TAG).report(this.<Gauge>map(),
                                           this.<Counter>map(),
                                           this.<Histogram>map(),
                                           this.<Meter>map(),
                                           map("test.another.timer", timer));

        assertOutput(expectedTimerResultMinimizedWithTag(), 1);
    }

    private void assertOutput(String[] expectedLines, int expectedDatagramPackets) throws IOException {
        ArgumentCaptor<DatagramPacket> packetCaptor = ArgumentCaptor.forClass(DatagramPacket.class);
        verify(socket, atLeast(1)).send(packetCaptor.capture());

        StringBuilder sb = new StringBuilder();
        List<DatagramPacket> capturedPackets = packetCaptor.getAllValues();
        assertEquals("The number of UDP packets actually sent for this reporter run was different than expected.", expectedDatagramPackets, capturedPackets.size());
        for (DatagramPacket capturedPacket : capturedPackets) {
            sb.append(new String(capturedPacket.getData()) + "\n");
        }

        String packetData = sb.toString();
        final String[] actualLines = packetData.split("\r?\n|\r");

        assertEquals("Line count mismatch, was:\n" + Arrays.toString(actualLines) + "\nexpected:\n" + Arrays.toString(expectedLines) + "\n",
                     expectedLines.length,
                     actualLines.length);
        for (int i = 0; i < actualLines.length; i++) {
            if (!expectedLines[i].trim().equals(actualLines[i].trim())) {
                System.err.println("Failure comparing line " + (1 + i));
                System.err.println("Was:      '" + actualLines[i] + "'");
                System.err.println("Expected: '" + expectedLines[i] + "'\n");
            }
            assertEquals(expectedLines[i].trim(), actualLines[i].trim());
        }
    }


    private <T> SortedMap<String, T> map() {
        return new TreeMap<String, T>();
    }

    private <T> SortedMap<String, T> map(String name, T metric) {
        final TreeMap<String, T> map = new TreeMap<String, T>();
        map.put(name, metric);
        return map;
    }

    public String[] expectedGaugeResult(String value) {
        return new String[]{String.format("prefix.mygaugename.count:%s|g", value)};
    }

    public String[] expectedGaugeResultMinimizedWithTag(String value) {
        return new String[]{String.format("prefix.mygaugename.count:%s|g|#%s", value, TAG)};
    }

    public String[] expectedTimerResult() {
        return new String[]{
                "prefix.test.another.timer.count:1|g",
                "prefix.test.another.timer.meanRate:2.00|ms",
                "prefix.test.another.timer.1MinuteRate:3.00|ms",
                "prefix.test.another.timer.5MinuteRate:4.00|ms",
                "prefix.test.another.timer.15MinuteRate:5.00|ms",
                "prefix.test.another.timer.min:4.00|ms",
                "prefix.test.another.timer.max:2.00|ms",
                "prefix.test.another.timer.mean:3.00|ms",
                "prefix.test.another.timer.stddev:5.00|ms",
                "prefix.test.another.timer.median:6.00|ms",
                "prefix.test.another.timer.75percentile:0.75|ms",
                "prefix.test.another.timer.95percentile:0.95|ms",
                "prefix.test.another.timer.98percentile:0.98|ms",
                "prefix.test.another.timer.99percentile:0.99|ms",
                "prefix.test.another.timer.999percentile:1.00|ms"
        };
    }

    public String[] expectedTimerResultMinimizedWithTag() {
        return new String[]{
                "prefix.test.another.timer.count:1|g|#" + TAG,
                "prefix.test.another.timer.1MinuteRate:3.00|g|#" + TAG,
                "prefix.test.another.timer.mean:3.00|g|#" + TAG
        };
    }

    public String[] expectedMeterResult() {
        return new String[]{
                "prefix.test.meter.count:1|g",
                "prefix.test.meter.meanRate:2.00|ms",
                "prefix.test.meter.1MinuteRate:3.00|ms",
                "prefix.test.meter.5MinuteRate:4.00|ms",
                "prefix.test.meter.15MinuteRate:5.00|ms"
        };
    }

    public String[] expectedMeterResultMinimizedWithTag() {
        return new String[]{
                "prefix.test.meter.count:1|g|#" + TAG,
                "prefix.test.meter.1MinuteRate:3.00|g|#" + TAG
        };
    }

    public String[] expectedHistogramResult() {
        return new String[]{
                "prefix.test.histogram.min:4.00|ms",
                "prefix.test.histogram.max:2.00|ms",
                "prefix.test.histogram.mean:3.00|ms",
                "prefix.test.histogram.stddev:5.00|ms",
                "prefix.test.histogram.median:6.00|ms",
                "prefix.test.histogram.75percentile:0.75|ms",
                "prefix.test.histogram.95percentile:0.95|ms",
                "prefix.test.histogram.98percentile:0.98|ms",
                "prefix.test.histogram.99percentile:0.99|ms",
                "prefix.test.histogram.999percentile:1.00|ms"
        };
    }

    public String[] expectedHistogramResultMinimizedWithTag() {
        return new String[]{
                "prefix.test.histogram.mean:3.00|g|#" + TAG
        };
    }

    public String[] expectedCounterResult(long count) {
        return new String[]{
                String.format("prefix.test.counter.count:%d|g", count)
        };
    }

    public String[] expectedCounterResultMinimizedWithTag(long count) {
        return new String[]{
                String.format("prefix.test.counter.count:%d|g|#%s", count, TAG)
        };
    }

}
