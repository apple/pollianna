# Polliana Metrics Choices

These types of metric sets are available:
- Aggregated essential metrics for [best practices standard JVM monitoring](#JVM-Essentials), or a select subset thereof.
- Comprehensive or select sampled [GC](#GC-Sampling), [NMT](#NMT-Sampling) metrics, or other [miscellaneous runtime](#RT-Sampling) metrics.
- Comprehensive or select aggregated [GC](#GC-Aggregating), [NMT](#NMT-Aggregating), or other [miscellaneous runtime](#RT-Aggregating) metrics.

NMT metrics are only available if the JDK in use is enhanced to support this.
If it is not, then NMT-related JMX beans and attributes will automatically be inactive.

## JVM Essentials
The "Jvm" bean provides a small selection of JVM metrics
that are regarded as most probably essential for continuous monitoring.

| JMX Attribute               |  Type  |     Unit     | Description                              |
|:----------------------------|:------:|:------------:|:-----------------------------------------|
| JvmGcWorkloadMax            | double |      %       | Maximum Java heap occupancy              |
| JvmGcAllocationRateMax.     | double |  MiB/second  | Maximum Java object allocation rate      |
| JvmGcPauseMax               |  long  | milliseconds | Maximum GC pause duration                |
| JvmGcPausePortion           | double |      %       | Sum of GC pause durations % of runtime   |
| JvmDirectMemoryUsageMax     | double |      %       | Maximum direct memory used % of limit    |
| JvmCodeCacheSegmentUsageMax | double |      %       | Maximum % used in any code cache segment |

See for [GC Aggregating](#GC-Aggregating) for purposes and details of the GC metrics in this bean

The metric `JvmCodeCacheSegmentUsageMax` indicates to what percentage the fullest of all code cache segments is full, or,
if the code cache is not segmented, to what percentage the entire "legacy" code cache is full.
At 100% it is probable that dynamic (JIT) compilation will stop working and
then increasing amounts of bytecode will be executed by the JVM's interpreter instead of by compiled code,
which can lead to significant application slowdown.

## GC Sampling
The "GcSample" bean provides raw data samples taken from to the respective most recent GC event.
In other words, these attributes transport the "last known value" of any given metric.

| JMX Attribute             |  Type  |     Unit     | Description                              |
|:--------------------------|:------:|:------------:|:-----------------------------------------|
| GcSampleOccupancy         | double |      %       | Java heap occupancy                      |
| GcSampleWorkload          | double |      %       | Java heap workload                       |
| GcSampleAllocationRate    | double |  MiB/second  | Java object allocation rate              |
| GcSamplePause             |  long  | milliseconds | GC pause duration                        |
| GcSampleCycle             |  long  | milliseconds | GC cycle duration                        |
| GcSampleDirectMemoryLimit |  long  |    bytes     | Constant: available direct buffer memory |
| GcSampleDirectMemory      |  long  |    bytes     | Direct buffer memory in use              |
| GcSampleDirectMemoryUsage | double |      %       | % available direct buffer memory in use  |
| GcSampleMetaspace         |  long  |    bytes     | Metaspace memory in use                  |
| GcSampleMetaspaceUsage    | double |      %       | % available metaspace memory in use      |

The only "aggregations" that this bean performs are:
- internally storing one set of values for the above JMX attributes, which is overwritten by each new GC event;
- internally remembering the Java heap usage of one preceding GC event to calculate the allocation rate between two GC events.

All other aggregations are left to downstream users of the interface.
So, for example, if you are interested in never missing the maximum workload value in a certain time span,
you need to poll the bean more often than GCs occur in that time span.
As a rule of thumb, such JMX polling may have to occur every few seconds, at most tens of seconds,
to observe sufficient GC event resolution for actionable observations.

The above metaspace _usage percentage_ is only accurate if a metaspace limit has been configured
by the JVM command line option `-XX:MaxMetaspaceSize`.

## GC Aggregating
The "GcAggregate" bean provides up to five different temporal aggregations 
of the same underlying raw metrics over a polling interval.
Such a polling interval is automatically defined by two subsequent invocations of the same getter function,
typically driven by periodic JMX scraping.

When polling occurs is completely independent of when any targeted JVM events occur.
Pollianna internally takes note of each and every GC event by subscribing to GC event notifications.
It then repeatedly stores and aggregates observed data values,
and only reports composed results when the bean is queried.

Example: 25 garbage collection pauses happen between two subsequent calls about GC pause durations. 
During this interval, Pollianna remembers the shortest and the longest pause duration it comes across, 
calculates the average of all observed pause durations,
counts 25 pauses, and adds up all 25 pauses times together to a grand total.
It then reports these five outcomes through the getter call that demarcates the end of the polling interval.

### GC Pauses

A (stop-the-world) GC pause is the time span for which garbage collection operations
suspend application code from running.

| JMX Attribute           |  Type  |     Unit     | Description                       |
|:------------------------|:------:|:------------:|:----------------------------------|
| GcAggregatePauseCount   |  long  |    number    | Number of GC pauses               |
| GcAggregatePausePortion | double |      %       | Runtime % of GC pause durations   |
| GcAggregatePauseMin     |  long  | milliseconds | Duration of the shortest GC pause |
| GcAggregatePauseAvg     |  long  | milliseconds | Average GC pause duration         |
| GcAggregatePauseMax     |  long  | milliseconds | Duration of the longest GC pause  |

The GC pause total can be computed if the polling interval duration is known. 
<p><code>total = portion * (double) interval / 100.0</code></p>

### GC Cycles

A GC cycle is the entire time span that a garbage collection takes,
including the above pauses AND algorithm parts that run concurrently with the application code.
The latter part will occasionally show up in partially concurrent collectors such as CMS and G1.

Mostly concurrent collectors such as Shenandoah and ZGC almost always
have orders of magnitude longer cycle durations than pause durations.

For non-concurrent (aka stop-the-world) garbage collectors such as "Serial" and "Parallel",
cycle durations are equal to pause durations, as all GC activity suspends application execution.

| JMX Attribute           |  Type  |     Unit     | Description                       |
|:------------------------|:------:|:------------:|:----------------------------------|
| GcAggregateCycleCount   |  long  |    number    | Number of GC cycles               |
| GcAggregateCyclePortion | double |      %       | Runtime % of GC cycle durations   |
| GcAggregateCycleMin     |  long  | milliseconds | Duration of the shortest GC cycle |
| GcAggregateCycleAvg     |  long  | milliseconds | Average GC cycle duration         |
| GcAggregateCycleMax     |  long  | milliseconds | Duration of the longest GC cycle  |

The GC cycle total can be computed if the polling interval duration is known:
<p><code>total = portion * (double) interval / 100.0</code></p>

As GC cycles can overlap with polling intervals, their calculated runtime portion can exceed 100%.

### Java Heap Allocation Rate

An allocation rate sample is formed by the difference between 
how full the Java heap is at the beginning of a GC
and how full it was at the end of the preceding GC, 
divided by the time between those two meaasurements.

| JMX Attribute                |  Type  |    Unit    | Description                                    |
|:-----------------------------|:------:|:----------:|:-----------------------------------------------|
| GcAggregateAllocationRateMin | double | MiB/second | Lowest allocation rate between subsequent GCs  |
| GcAggregateAllocationRateAvg | double | MiB/second | Average allocation rate over the interval      |
| GcAggregateAllocationRateMax | double | MiB/second | Highest allocation rate between subsequent GCs |

### Java Heap Occupancy

Java heap occupancy is the percentage of the entire Java heap that is occupied by live objects,
as most recently determined by a GC that tallied all heap regions.
Any Java application runs out of memory near or at 100% occupancy.

In a generational GC system such as Serial, Parallel, G1, or CMS, purely young generation collections
do not contribute any occupancy samples.
Occupancy is only updated as new samples become available. Otherwise, the previous value is perpetuated.

| JMX Attribute           |  Type  | Unit | Description                               |
|:------------------------|:------:|:----:|:------------------------------------------|
| GcAggregateOccupancyMin | double |  %   | Lowest detected heap occupancy percentage |
| GcAggregateOccupancyAvg | double |  %   | Average heap occupancy percentage         |
| GcAggregateOccupancyMax | double |  %   | Higest detected heap occupancy percentage |

### Java Heap Workload

For single-generational collectors such as Shenandoah and ZGC, 
as well as Generational ZGC and C4, workload is the always same as occupancy.

For not fully concurrent generational collectors such as Serial, Parallel, CMS, and G1,
the workload calculation involves subtracting
the assumed minimum size of the young generation from the total available object space.
This results in higher reported percentages compared to occupancy,
which provides an earlier warning of running into increased GC activity,
even without detailed knowledge about the given GC configuration.
Near or at 100% workload, garbage collection intensity will increase, 
up to back-to-back collections (GC thrashing).

The assumed minimum young generation size for the latter collectors is the same its initial size,
until a young generation size below this is detected.
Then the assumed minimum is set to zero, which makes workload the same as occupancy, as a dynamic fallback position.
So far, this variability below the initial size has only been observed with G1.

| JMX Attribute          | Type   | Unit | Description                              |
|:-----------------------|:------:|:----:|:-----------------------------------------|
| GcAggregateWorkloadMin | double |  %   | Lowest detected heap workload percentage |
| GcAggregateWorkloadAvg | double |  %   | Average heap workload percentage         |
| GcAggregateWorkloadMax | double |  %   | Higest detected heap workload percentage |

### Direct Buffer Memory

The default direct memory limit equals the configured maximum Java heap size.
This can be changed on the `java` command line with the option `-XX:MaxDirectMemorySize`.
When an additional allocation of direct buffer memory would exceed 100% of the configured limit,
the JVm will throw an `OutOfMemoryException`.
Direct buffer memory can be released explicitly or by global garbage collection.
To capture both, we only record it after such collections.

| JMX Attribute                   |  Type  | Unit  | Description                                              |
|:--------------------------------|:------:|:-----:|:---------------------------------------------------------|
| GcAggregateDirectMemoryLimit    |  long  | bytes | Constant: available direct buffer memory                 |
| GcAggregateDirectMemoryMin      |  long  | bytes | Minimum detected direct buffer memory in use             |
| GcAggregateDirectMemoryAvg      |  long  | bytes | Average detected direct buffer memory in use             |
| GcAggregateDirectMemoryMax      |  long  | bytes | Maximum detected direct buffer memory in use             |
| GcAggregateDirectMemoryUsageMin | double |   %   | Minimum detected % available direct buffer memory in use |
| GcAggregateDirectMemoryUsageAvg | double |   %   | Average detected % available direct buffer memory in use |
| GcAggregateDirectMemoryUsageMax | double |   %   | Maximum detected % available direct buffer memory in use |

### Metaspace

Metaspace size is by default unlimited, but can be limited by JVM command line option `-XX:MaxMetaspaceSize`.
If metaspace is limited, then any attempts to allocate metaspace memory beyond 100% of the limit
will cause the JVM to throw an `OutOfMemoryException`.
Metaspace memory is primarily allocated by class loading and released by class unloading,
which involves global garbage collection.

| JMX Attribute                |  Type  | Unit  | Description                                             |
|:-----------------------------|:------:|:-----:|:--------------------------------------------------------|
| GcAggregateMetaspaceMin      |  long  | bytes | Minimum detected metaspace memory in use                |
| GcAggregateMetaspaceAvg      |  long  | bytes | Average detected metaspace memory in use                |
| GcAggregateMetaspaceMax      |  long  | bytes | Maximum detected metaspace memory in use                |
| GcAggregateMetaspaceUsageMin | double |   %   | Minimum detected % of available metaspace memory in use |
| GcAggregateMetaspaceUsageAvg | double |   %   | Average detected % of available metaspace memory in use |
| GcAggregateMetaspaceUsageMax | double |   %   | Maximum detected % of available metaspace memory in use |

The above metaspace _usage percentages_ are only accurate if a metaspace limit has been configured

## JIT Compilation and Code Cache

The "Compilation" Beans provide metrics of JIT compilation time and Code Cache information.

The JVM Code Cache is a memory area where the JVM stores native code that it compiles from bytecode. It has a fixed size which 
is limited by the JVM command line option `-XX:ReservedCodeCacheSize`. Before JDK 9, the Code Cache is a single
contiguous space, while after JDK 9 it has been split into 3 distinct segments, each of which contains 
compiled code of a particular type. This "segmented" Code Cache configuration can be disabled manually with the JVM command 
line option `-XX:-SegmentedCodeCache`. Either Code Cache configurations are supported, but only the metrics of the one that  
is enabled are available.

| JMX Attribute                                        |  Type  |     Unit     |  Description                                                  |
|:-----------------------------------------------------|:------:|:------------:|:--------------------------------------------------------------|
| CompilerAggregateCompilationTimeMin                  | long   | milliseconds | Minimum JIT compilation time                                  |
| CompilerAggregateCompilationTimeAvg                  | long   | milliseconds | Average JIT compilation time                                  |
| CompilerAggregateCompilationTimeMax                  | long   | milliseconds | Maximum JIT compilation time                                  |
| CompilerAggregateProfiledNMethodsCodeHeapMin         | long   |    bytes     | Minimum Profiled NMethods Code Heap in use                    |    
| CompilerAggregateProfiledNMethodsCodeHeapAvg         | long   |    bytes     | Average Profiled NMethods Code Heap in use                    |    
| CompilerAggregateProfiledNMethodsCodeHeapMax         | long   |    bytes     | Maximum Profiled NMethods Code Heap in use                    |    
| CompilerAggregateProfiledNMethodsCodeHeapUsageMin    | double |      %       | Minimum available % of Profiled NMethods Code Heap in use     |    
| CompilerAggregateProfiledNMethodsCodeHeapUsageAvg    | double |      %       | Average available % of Profiled NMethods Code Heap in use     |    
| CompilerAggregateProfiledNMethodsCodeHeapUsageMax    | double |      %       | Maximum available % of Profiled NMethods Code Heap in use     |    
| CompilerAggregateProfiledNMethodsCodeHeapLimit       | long   |    bytes     | The limit of Profiled NMethods Code Heap                      | 
| CompilerAggregateNonProfiledNMethodsCodeHeapMin      | long   |    bytes     | Minimum Non-Profiled NMethods Code Heap in use                |    
| CompilerAggregateNonProfiledNMethodsCodeHeapAvg      | long   |    bytes     | Average Non-Profiled NMethods Code Heap in use                |    
| CompilerAggregateNonProfiledNMethodsCodeHeapMax      | long   |    bytes     | Maximum Non-Profiled NMethods Code Heap in use                |
| CompilerAggregateNonProfiledNMethodsCodeHeapUsageMin | double |      %       | Minimum available % of Non-Profiled NMethods Code Heap in use |
| CompilerAggregateNonProfiledNMethodsCodeHeapUsageAvg | double |      %       | Average available % of Non-Profiled NMethods Code Heap in use |
| CompilerAggregateNonProfiledNMethodsCodeHeapUsageMax | double |      %       | Maximum available % of Non-Profiled NMethods Code Heap in use |
| CompilerAggregateNonProfiledNMethodsCodeHeapLimit    | long   |    bytes     | The limit of Non-Profiled NMethods Code Heap                  |
| CompilerAggregateNonNMethodsCodeHeapMin              | long   |    bytes     | Minimum Non-NMethods Code Heap in use                         |
| CompilerAggregateNonNMethodsCodeHeapAvg              | long   |    bytes     | Average Non-NMethods Code Heap in use                         |
| CompilerAggregateNonNMethodsCodeHeapMax              | long   |    bytes     | Maximum Non-NMethods Code Heap in use                         |
| CompilerAggregateNonNMethodsCodeHeapUsageMin         | double |      %       | Minimum available % of Non-NMethods Code Heap in use          |
| CompilerAggregateNonNMethodsCodeHeapUsageAvg         | double |      %       | Average available % of Non-NMethods Code Heap in use          |
| CompilerAggregateNonNMethodsCodeHeapUsageMax         | double |      %       | Maximum available % of Non-NMethods Code Heap in use          |
| CompilerAggregateNonNMethodsCodeHeapLimit            | long   |    bytes     | The limit of Non-NMethods Code Heap                           |
| CompilerAggregateCodeCacheMin                        | long   |    bytes     | Minimum Code Cache in use                                     |
| CompilerAggregateCodeCacheAvg                        | long   |    bytes     | Average Code Cache in use                                     |
| CompilerAggregateCodeCacheMax                        | long   |    bytes     | Maximum Code Cache in use                                     |
| CompilerAggregateCodeCacheUsageMin                   | double |      %       | Minimum available % of Code Cache in use                      |
| CompilerAggregateCodeCacheUsageAvg                   | double |      %       | Average available % of Code Cache in use                      |
| CompilerAggregateCodeCacheUsageMax                   | double |      %       | Maximum available % of Code Cache in use                      |
| CompilerAggregateCodeCacheLimit                      | long   |    bytes     | The limit of Code Cache                                       |

While the above metrics deliver a more detailed view on the code cache,
there is also a summary metric that tells to what percentage the fullest of all code cache segments is full, or,
if the code cache is not segmented, to what percentage the entire "legacy" code cache is full.
This metric can be found in the Jvm bean, [see above](JVM-Essentials): "JvmCodeCacheSegmentUsageMax".  

## Miscellaneous Runtime Data Sampling
The "RtSample" bean provides raw data samples taken from the Java runtime system 
that are neither related to GC nor retrieved from the JVM's native memory tracking system.

| JMX Attribute        | Type  | Unit   | Description                                                 |
|:---------------------|:-----:|:------:|:------------------------------------------------------------|
| RtSampleMappedMemory | long  | bytes  | Memory used by file-mapped buffers                          |
| RtSampleMappedMemory | long  | bytes  | Memory used by file-mapped buffers                          |
| RtSampleWarningCount | int   | number | Number of warnings reported by the JVM since it has started |
| RtSampleErrorCount   | int   | number | Number of errors reported by the JVM since it has started   |

## Miscellaneous Runtime Data Aggregating

The "RtAggregate" bean covers the same Java runtime data categories as the "RtSample" bean,
but it provides aggregates in the following form.

| JMX Attribute              | Type |  Unit  | Description                                         |
|:---------------------------|:----:|:------:|:----------------------------------------------------|
| RtAggregateMappedMemoryMin | long | bytes  | Minimun memory used by file-mapped buffers          |
| RtAggregateMappedMemoryAvg | long | bytes  | Average memory used by file-mapped buffers          |
| RtAggregateMappedMemoryMax | long | bytes  | Maximum memory used by file-mapped buffers          |
| RtStartedThreadCount       | long | number | Number of platform threads created and then started |
| RtPeakThreadCount          | long | number | Maximum number of live platform threads             |

Attention: Only use `RtPeakThreadCount` if your application code never calls `java.lang.management.ThreadMXBean.resetPeakThreadCount()` by itself,
because the implementation of the metric also relies on making such a call and there would be mutual interference.

## NMT Sampling

The "NmtSample" bean provides raw samples in this form: 

| JMX Attribute                  | Type |  Unit |
|:-------------------------------|:----:|:-----:|
| NmtSample\<Category\>Reserved  | long | bytes | 
| NmtSample\<Category\>Committed | long | bytes |

These are the NMT category names that you can use to replace `<Category>`:
<p><code>
Total, ArenaChunk, Arguments, Classes, Compiler, Gc, Internal, JavaHeap, Jvmci, Metaspace,
Modules, Nmt, ObjectMonitors, Other, Safepoint, Serviceability, SharedClassSpace, Statistics,
StringDeduplication, Symbol, Synchronization, Thread, ThreadStack, Tracing, Unknown
</code><p>

## NMT Aggregating
The "NmtAggregate" bean covers the same NMT data categories as the "NmtSample" bean,
but it provides aggregates in the following form.

| JMX Attribute                        |  Type  | Unit  |
|:-------------------------------------|:------:|:-----:|
| NmtAggregate\<Category\>ReservedMin  |  long  | bytes | 
| NmtAggregate\<Category\>ReservedAvg  |  long  | bytes | 
| NmtAggregate\<Category\>ReservedMax  |  long  | bytes | 
| NmtAggregate\<Category\>CommittedMin |  long  | bytes | 
| NmtAggregate\<Category\>CommittedAvg |  long  | bytes | 
| NmtAggregate\<Category\>CommittedMax |  long  | bytes | 
| NmtAggregate\<Category\>PercentMin   | double |   %   | 
| NmtAggregate\<Category\>PercentAvg   | double |   %   | 
| NmtAggregate\<Category\>PercentMax   | double |   %   | 

The "Percent" values express the percentage of committed vs reserved memory in the given category.
A typical use case is monitoring the maximum of such a percentage
in order to avoid premature memory exhaustion in bounded JVM memory pools.
