# Pollianna
Pollianna is a Java library and Java command line agent
that accumulates JVM metrics over user-determined polling intervals and exposes them via JMX,
so that they can be consumed by both Java and non-Java telemetry data sinks.

Pollianna also has an API which exposes the same JVM metrics through method calls.
Additionally, it allows specifying an OpenTelemetry compatible endpoint to publish metrics to.

The offered metrics are intended to facilitate Java service deployment configuration and operation
and to support best-practice continuous monitoring.

* [List of Metrics](docs/metrics-list.md)
* [Specific Garbage Collector Considerations](docs/gc-specifics.md)

## Metrics Delivery

There are three distinct but not mutually exclusive ways to use Pollianna.
1. Register JMX beans that serve JVM metrics data via bean attributes.
   You can then apply a JMX scraper such as Prometheus to transport metrics from these JXM Beans.
2. Configure an OpenTelemetry endpoint.
   Pollianna will then directly send your selected metrics there.
3. Retrieve JVM metric data by local API calls. 
   No JMX involved. 
   How you further disseminate the metric data is then up to you.

### 1. Starting JMX Beans

All Pollianna JMX beans must be instated in one of these three ways:
- by a call to a Java method, typically very early in the `main()` program,
- by adding Pollianna as a Java command line agent to a JVM command line,
- by attaching Pollianna as a Java agent to a running JVM.

#### Starting JMX Beans by a Static Method Call
Pollianna can be started by:
```
Pollianna.start();
```
Without any arguments, this call installs and starts the "Jvm" bean,
so is equivalent:
```
Pollianna.start("Jvm");
```

If arguments are given, they configure, install, and start
one or several of Pollianna's JVM observation beans.
All arguments must be of type `String` and they are evaluated left-to-right.

If an argument begins with the keyword `file` followed by a colon (':'),
then it specifies a file path and all arguments in that file,
separated by semicolons (';'), are evaluated.
Examples:
```
Pollianna.start("file:relative-path/pollianna-arguments.txt");
```
```
Pollianna.start("file:/absolute-path/pollianna-arguments.txt");
```

If an argument begins with the keyword `interval` followed by a colon (':'),
then the rest of the argument specifies the interval time in seconds
to be used for periodic sampling (of RT and NMT metrics). The default is 10.
Example:
```
Pollianna.start("interval:5");
```

Otherwise, an argument specifies a bean name.
If that name is followed by a pipe character ('|'),
then only the bean attributes listed after the colon will be exposed to JMX.
If an attribute has a non-primitive return type then its name has
the base name of a getter method in that type as a suffix.
Example: if the bean named before the colon has an attribute "Pause"
stemming from its getter method `getPause()`,
and the return type of `getPause()` has a getter method 'getMax()',
then the complete attribute name is "PauseMax".
Example bean argument with select attributes:
```
"GcAggregate|PauseMax,CycleAvg,AllocationRateMax"
```
If the same bean name is specified multiple times, only the right-most argument applies.

The available beans are: `Jvm`, `RtAggregate`, `RtSample`, `GcAggregate`, `GcSample`, `CompilerAggregate`, and `CompilerSample`.
If the JDK in use supports NMT data discovery by a dedicated JMX bean (see below),
then these additional beans are available: `NmtAggregate` and `NmtSample`.

Example with multiple arguments:
```
Pollianna.start("interval:20", "RtSample", "GcAggregate|PauseMax,CycleAvg,AllocationRateMax", "file:morePolliannaArguments.txt");
```

#### Pollianna as Java Command Line Agent
Adding this to your JVM command line invokes Pollianna without touching your application's source code.
```
-javaagent:pollianna-1.16.1.jar
```
You can provide the same arguments as for a Pollianna invokation by method call,
except that they have to be combined into one single string in which they are separated by semicolons.
Full example:
```
java -Xms4G -Xmx4G \
     -javaagent:pollianna-1.16.1.jar="interval:20;NmtSample;GcAggregate|PauseMax,CycleAvg,AllocationRateMax;file:morePolliannaArguments.txt" \
     MyApplication
```
This sets the sampling interval for NMT data to every 20 seconds, 
starts the `NmtSample` bean, starts the `GcAggregate` bean with a few select attributes,
and then reads and applies additional arguments from local file `"morePolliannaArguments.txt"`.

#### Attaching the Pollianna Agent to a Running JVM

Instead of specifying the agent on the command line,
operators can also dynamically attach it to a running Java program.
This leaves the original deployment code intact,
but requires additional code for the agent's deployment,
its activation, and local service PID discovery.

#### Enabling NMT Data
Pollianna beans for Native Memory Tracking (NMT) data will only function if:
1. The observed JDK provides all the classes and methods which are reflectively referenced in "NmtAccess.java".
   (Vanilla OpenJDK does not have these.)
2. The command line option `-XX:NativeMemoryTracking=summary` or `-XX:NativeMemoryTracking=detail` is used. 
   Only `summary` is needed for Pollianna, but you can also choose `detail` if needed for other purposes.

### 2. Publishing Metrics with OpenTelemetry

Collected metrics can be published to an OpenTelemetry-compatible target,
configured by the following parameters:

| Argument name                   | Required | Description                                                                |
|:--------------------------------|:--------:|:---------------------------------------------------------------------------|
| `otel_endpoint`                 |  `yes`   | OpenTelemetry gRPC endpoint.                                               |
| `otel_service_name`             |  `yes`   | Service name to attach to all metrics                                      |
| `otel_trusted_root`             |  `yes`   | Path to trusted CA file (PEM).                                             |
| `otel_client_keystore`          |  `yes`   | Path to keystore with mTLS credentials.                                    |
| `otel_client_keystore_password` |   `no`   | Path to file containing keystore password, defaults to empty password.     |
| `otel_headers`                  |   `no`   | Headers to set and send with requests. Supports env vars for values.       |
| `otel_labels`                   |   `no`   | Additional labels to set on metrics. Supports env vars for values.         |
| `otel_interval`                 |   `no`   | The metrics polling and OTel publishing interval, in seconds. Default: 60. |

Example command line:

```shell
java -Xms4G -Xmx4G \
     -javaagent:/somewhere/pollianna-1.16.1-iso.jar="Jvm;otel_endpoint:https://example-ingestion-gateway.telemetry.example.com:2345;otel_service_name:my_service;otel_client_keystore:/somewhere/application.p12;otel_trusted_root:/somewhere/trusted-root.pem;otel_headers:WORKSPACE=my_workspace,MY_EXTRAS=my_extras;otel_labels:cluster=a_custer,namespace=a_namespace,pod=$HOSTNAME;otel_interval:360" MyApplication
```

This will publish the default metrics, see `JVM Essentials` in [metrics-list.md](./docs/metrics-list.md#jvm-essentials).
Alternative metric sets can be hand-picked with the same syntax as described above.

Note: It is necessary to use the "fat" or the "iso" JAR variant,
either of which includes the necessary dependencies to use OpenTelemetry,
or to include the [dependencies](#releases) on the class path.

### 3. Pollianna API

If you have other means of transporting and consuming metrics than through JMX or OpenTelemetry,
you can query JVM metrics by local calls to Pollianna's API.
It is then up to you how to further disseminate the gathered data.

The names of the involved classes correspond directly to the names of the above JMX beans, plus the suffix "Seed".
Example: there is a JMX bean name "GcAggregate" and an API class `GcAggregateSeed`.
Background: a "seed" is what is inside a "bean", without the shell, the JMX wrapper.
Example:
```
final JvmSeed jvm = new JvmSeed();
jvm.startRecording();
...
final double maxJavaHeapWorkloadPercentage = jvm.getGcWorkloadMax()
...
```

See [Pollianna API Examples](src/test/java/com/apple/pollianna/PolliannaApiExamples.java) for more details on
how to create seed objects and how to activate and query them. 
There is an example for each available seed class.
Both aggregating and sampling style are supported.

#### GC Aggregating and Sampling
All GC metrics are produced by listening to _**asynchronous**_ GC events that occur inside the JVM.
They are captured whenever GC activities occur,
which is decoupled from when users query Pollianna for GC data.
This is the same for aggregate and sampled GC data, since a GC "sample" is a remembered value,
like any aggregate, albeit with no computation involved.
In either case you need to call `startRecording()` to begin processing GC events,
which makes GC metrics available.

#### NMT Aggregating

NMT data are based on _**synchronous**_ polling from outside the JVM.
NMT aggregating is always performed periodically, at a fixed interval,
This is started by calling `startRecording()`.
This interval, with default value 10 seconds, can only be changed globally,
and only before any sampling seed or JMX bean has started recording.
For example, this call changes the NMT recording interval from the default to 5 seconds:
```
PeridodicAggregator.setIntervalSeconds(5);
```

#### NMT Sampling

For NMT sampling you have a choice between two strategies:
1. Call `startRecording()` to have samples recorded periodically in the background.
   When you query results, they will maximally be as old as the length of the global sampling interval.
2. Call `recordNow()` to explicitly invoke one sample recording at any given time. 
   If you query a result immediately after this, it will be current.

You can use both approaches, but whichever recording is the most recent when querying determines the resulting values.

### Sampling and Aggregating Miscellaneous Other Runtime Data

Sampling with `RtSampleSeed` works exactly as with `NmtSampleSeed`,
with the same choice between `startRecording()` and `recordNow()`.
And the same basic calls that apply to `NmtAggregatSeed` also apply to `RtAggregateSeed`.

Example:
```
    final RtAggregateSeed seed = new RtAggregateSeed();
    final long mappedMemoryBytes = seed.getMappedMemory();
```

## Building
This command creates ready-to-use JAR files and places them into `./build/libs`.
```
./gradlew build
```
Excluding testing:
```
./gradlew build -x test
```

These alternative JAR files are created:
- Without any dependencies ("slim JAR"): `pollianna-<version>.jar`. 
  This is sufficient if you do not configure OpenTelemetry or if you have the required dependencies for OpenTelemetry on your classpath.
- With all dependencies included ("fat JAR"): `pollianna-<version>-all.jar`.
- With all dependencies included and isolated ("iso JAR"): `pollianna-<version>-iso.jar`.
  Must be used as command line agent.
  All dependencies will be isolated from the application by a custom class loader with a separate claspath.

## Testing
```
./gradlew test
```
In addition, this command tests Pollianna with every garbage collector available
in the JDK that it is running on.
```
src/test/test.sh
