# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Statsd reporter library for Codahale/Dropwizard Metrics (metrics-core). It allows Java applications to send metrics to Statsd/DogStatsD servers via UDP. This is a Lithium Technologies fork that adds per-metric tagging support for DogStatsD.

- **GroupId**: `com.bealetech`
- **ArtifactId**: `metrics-statsd`
- **Current Version**: 3.0.1-li-11-SNAPSHOT
- **Java Version**: 1.8
- **Metrics Version**: 3.1.2

## Build and Development Commands

### Building the Project
```bash
# Clean and compile
mvn clean compile

# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=StatsdReporterTest

# Run a specific test method
mvn test -Dtest=StatsdReporterTest#testMethodName

# Package (creates JAR)
mvn package

# Install to local Maven repository
mvn install
```

### Deployment Commands
```bash
# Deploy snapshot to Lithium's Nexus
mvn clean deploy

# Create a release (Maven release plugin)
mvn release:clean
mvn release:prepare
mvn release:perform
```

### Code Quality
```bash
# Generate Javadoc
mvn javadoc:javadoc

# Generate sources JAR
mvn source:jar
```

## Architecture Overview

### Core Components

The codebase consists of two main functional areas:

1. **Metrics Reporting** (`com.bealetech.metrics.reporting` package)
   - `StatsdReporter`: Main reporter that extends Dropwizard's `ScheduledReporter`. Handles periodic metric collection and transmission.
   - `UDPSocketProvider`: Interface for providing UDP sockets (enables testing with mock sockets)
   - `DefaultSocketProvider`: Production implementation that creates real UDP sockets

2. **DataDog Event Client** (`com.lithium.dog.event` package)
   - `DataDogEventClient`: Sends event messages to DogStatsD event API
   - `EventMessage`: Builder for DogStatsD event format
   - `AlertType`, `Priority`: Enums for event metadata
   - `ErrorHandler`: Interface for handling UDP send errors

### StatsdReporter Architecture

The `StatsdReporter` is the heart of the library:

- Uses **Builder pattern** for configuration: prefix, tags, minimized metrics, percentiles, etc.
- Implements UDP batching: Metrics are buffered in memory and sent when the buffer reaches 512 bytes (MAX_UDPDATAGRAM_LENGTH) or at the end of a reporting cycle.
- **Metric types supported**: Gauges, Counters, Histograms, Meters, Timers
- **Tag support**:
  - Global tags via `withAppendTag()` - applied to all metrics
  - Per-metric tags via `sendDataWithTags()` - allows dynamic tagging
  - Format: `metric.name:value|type|#tag1:val1,tag2:val2`

### Key Design Patterns

1. **Builder Pattern**: `StatsdReporter.Builder` provides fluent API for configuration
2. **Strategy Pattern**: `UDPSocketProvider` interface allows swapping socket implementations
3. **Template Method**: `ScheduledReporter` (from metrics-core) defines reporting lifecycle; `StatsdReporter` implements metric-specific logic

### UDP Packet Management

The reporter carefully manages UDP packet size:
- Writes metrics to a `ByteArrayOutputStream` via `BufferedWriter`
- Checks packet size after each metric (limit: 512 bytes)
- Automatically sends and resets buffer when threshold is exceeded
- Prevents UDP packet fragmentation

### Metric Minimization

When `minimizeMetrics=true` (default):
- Only essential metrics are sent (count, 1-minute rate, mean)
- Timers use gauge type (`g`) instead of milliseconds (`ms`)
- Reduces Statsd/DataDog metric cardinality and costs
- Optional 98th percentile via `withSend98thPercentile()`

## Important Implementation Notes

### Package History
- Original package: `com.studyblue` (v2.2.x and earlier)
- Current package: `com.bealetech` (v2.3.0+)
- Lithium fork adds per-metric tagging (v3.0.x-li branches)

### Per-Metric Tagging Feature
The Lithium fork adds the ability to send tags with individual metrics (not just global tags):
- `sendMetricWithTags(String name, long value, String tags)` - for counters
- `sendTimerWithTags(String name, long durationMs, String tags)` - for timers
- Tags format: comma-separated key:value pairs (e.g., "company:nike,env:qa")
- Used by `ic-backend` project's `TaggedMetrics` wrapper

### Testing
- Tests use Mockito to mock `UDPSocketProvider`
- Tests verify UDP packet format and content
- Socket lifecycle (open/close) is tested
- Test classes mirror source structure: `src/test/java` matches `src/main/java`

### Maven Configuration Details
- Uses Lithium's internal Nexus: `http://nexus.dev.lithium.com/nexus/content/repositories/releases`
- Release plugin configured with tag format: `v@{project.version}` (e.g., v3.0.1-li-10)
- GPG signing available via `-DgpgSign=true` profile
- Javadoc plugin uses `-Xdoclint:none` to suppress strict checks

## Common Development Patterns

### Adding New Metric Types
1. Add processing method in `StatsdReporter` (e.g., `processNewMetric()`)
2. Call it from `report()` method
3. Use `sendInt()`, `sendFloat()`, or `sendObj()` to emit metric
4. Add test coverage in `StatsdReporterTest`

### Modifying Metric Format
- All metric formatting happens in `sendDataWithTags()` method (src/main/java/com/bealetech/metrics/reporting/StatsdReporter.java:423)
- Metric format: `prefix.name:value|type|#tags`
- String sanitization: spaces replaced with hyphens via `sanitizeString()`

### Socket Provider Pattern
To add custom socket behavior:
1. Implement `UDPSocketProvider` interface
2. Provide implementation to `StatsdReporter.forRegistry(registry, customProvider)`
3. Used for testing and custom networking scenarios
