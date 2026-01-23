# Specific Garbage Collector Considerations

Pollianna does not need to be configured. 
It automatically detects which garbage collector is in use and adapts to it.

The following holds for all collectors:
* The allocation rate is measured between any kinds of collections,
  i.e. no matter whether the old generation is involved.
* "Occupancy" is the percentage of the entire heap space
  that is occupied by live objects after a global garbage collection,
  i.e. a collection that assessed the entire heap for liveness.

"Workload" is calculated in the same way as occupancy, except that for Serial, Parallel, CMS, and G1
the assumed minimum young generation size is subtracted from the available heap space.
This results in higher reported percentages compared to occupancy,
which provides an earlier warning of running into increased GC activity,
even without detailed knowledge about the given GC configuration.
Near or at 100% workload, garbage collection intensity will increase,
up to back-to-back collections (GC thrashing).
The assumed minimum young generation size for the latter collectors is the same its initial size,
until a young generation size below this is detected. 
Then the assumed minimum is set to zero, which makes workload the same as occupancy, as a dynamic fallback position.
So far, this variability below the initial size has only been observed with G1.

## Overview

| Collector        | Cycles &ne; Pauses |             Workload &gt; Occupancy             | Workload/Occupancy Over-Reporting | Allocation Rate Under-Reporting |
|:-----------------|:------------------:|:-----------------------------------------------:|:---------------------------------:|:-------------------------------:|
| Serial           |         no         |                       yes                       |                no                 |               no                |
| Parallel         |         no         |                       yes                       |                no                 |               no                |
| CMS              |     sometimes      |                       yes                       |                no                 |               no                |
| G1               |     sometimes      | until young gen size &lt; initial size detected |                yes                |               no                |
| Shenandoah       |       always       |                       no                        |                no                 |               no                |
| ZGC              |       always       |                       no                        |                no                 |               no                |
| Generational ZGC |       always       |                       no                        |                no                 |               yes               |
| C4               |       always       |                       no                        |                no                 |               yes               |

## Serial
The "Serial" collector is selected by the JVM command line option ```-XX:+UseSerialGC```.

It is a "stop-the-world" collector:
application execution is always suspended for the entire time when the GC is active.
Therefore, GC pauses and GC cycles are interchangeable, with identical count and duration metric values.
It is sufficient to monitor either of these and omit the other.

The Serial collector is generational.
Heap occupancy and heap workload are only determined at the end of "full" collections, 
which include the old generation.
Young generation collections are not considered for this purpose,
because they do not establish a complete live set of objects.

Workload is calculated assuming a fixed young generation size.

## Parallel
The Parallel collector is the default in JDK 8.
It is explicitly selected by the JVM command line option ```-XX:+UseParallellGC```.

It is a "stop-the-world" collector:
application execution is always suspended for the entire time when the GC is active.
Therefore, GC pauses and GC cycles are interchangeable, with identical count and duration metric values.
It is sufficient to monitor either of these and omit the other.

The Parallel collector is generational.
Heap occupancy and heap workload are only determined at the end of collections
which include the old generation.
Collections restricted to the young generation are not considered for this purpose,
because they do not establish a complete live set of objects.

## Concurrent Mark-Sweep (CMS)
The Concurrent Mark-Sweep collector is selected by the JVM command line option ```-XX:+UseConcMarkSweepGC```.

It is a partially concurrent collector: 
application execution is only suspended for some of the times when the GC is active.
GC pauses and GC cycles usually differ,
except for young generation collections,
which are still executed in a stop-the-world manner.
It is recommended to monitor both pauses and cycles.

The Parallel collector is generational.
Heap occupancy and heap workload are only determined at the end of collections
which include the old generation.
Collections restricted to the young generation are not considered for this purpose,
because they do not establish a complete live set of objects.

## G1
The G1 collector is the default as of JDK 9.
It is explicitly selected by the JVM command line option ```-XX:+UseG1GC```.

It is a partially concurrent collector:
application execution is only suspended for some of the times when the GC is active.
GC pauses and GC cycles usually differ,
except for pure young generation collections,
which are still executed in a stop-the-world manner.
It is recommended to monitor both pauses and cycles.

The G1 collector is generational. 
However, there is no JDK API to query if a collection involved the old generation or not.
This makes it impossible for Pollianna 
to completely narrow down the extent of the live set at any time,
except after a "full" collection (which is to be avoided).
Pollianna assesses the currently used space after any kind of collection.
Thus both heap occupancy and heap workload may be over-reported most of the time.
But any increase in GC frequency tends to reduce this margin of error.
This means that the reported values tend to approximate reality better when it matters most 
and can deviate substantially when it does not.

## Shenandoah
The Shenandoah collector is selected by the JVM command line option ```-XX:+UseShenandoahGC```.

It is a mostly concurrent collector:
application execution proceeds concurrently for almost all the times when the GC is active.
GC pauses and GC cycles usually differ by orders of magnitude.
It is recommended to monitor both pauses and cycles.

The Shenandoah collector is single-generational.
Therefore, heap occupancy and workload are the same,
and they are determined quite accurately at the end of each GC cycle.

## Generational Shenandoah

The Generational Shenandoah collector is selected by the JVM command line option 
```-XX:+UseShenandoahGC -XX:+UnlockExperimentalVMOptions -XX:ShenandoahGCMode=generational```.

It is an almost completely concurrent collector:
application execution proceeds concurrently for almost all the times when the GC is active.
Additionally, young and old generation collections are concurrent with each other,
with very short synchronization pauses.
GC pauses and GC cycles usually differ by orders of magnitude.
It is recommended to monitor both pauses and cycles.

This collector is generational, as its name indicates.
Heap occupancy and workload are only determined at the end of collections
which include the old generation.
Collections restricted to the young generation are not considered for this purpose,
because they do not establish a complete live set of objects.

For Generational Shenandoah, heap occupancy and workload are assumed to be the same.
By default, generation sizing is dynamically variable, and even when it is explicit configured,
its effects are not detectable via standard JDK monitoring interfaces (GC notification listeners).

Since young and old generation collections can overlap in time,
there can occasionally be phases in which allocation rate measurements are omitted.
This can lead to underreporting, which can increases with GC frequency.

## ZGC
The ZGC collector is selected by the JVM command line option ```-XX:+UseZGC```.
It must not be used in JDK versions below 17.

ZGC is a mostly concurrent collector:
application execution proceeds concurrently for almost all the times when the GC is active.
GC pauses and GC cycles usually differ by orders of magnitude.
It is recommended to monitor both pauses and cycles.

The ZGC collector is single-generational.
Therefore, heap occupancy and workload are the same,
and they are determined quite accurately at the end of each GC cycle.

## Generational ZGC
The ZGC collector is selected by the JVM command line options ```-XX:+UseZGC -XX:+ZGenerational```.
It is only available in JDK versions 21 and later.

It is an almost completely concurrent collector:
application execution proceeds concurrently for almost all the times when the GC is active.
Additionally, young and old generation collections are concurrent with each other,
with very short synchronization pauses.
GC pauses and GC cycles usually differ by orders of magnitude.
It is recommended to monitor both pauses and cycles.

This collector is generational, as its name indicates.
Heap occupancy and workload are only determined at the end of collections
which include the old generation.
Collections restricted to the young generation are not considered for this purpose,
because they do not establish a complete live set of objects.

Heap occupancy and workload are the same,
because the generation sizing is always dynamically variable,
with no minimum size for the young generation.

Since young and old generation collections can overlap in time,
there can occasionally be phases in which allocation rate measurements are omitted.
This can lead to underreporting, which can increases with GC frequency.

## C4
The C4 collector is the default in Azul Systems' proprietary Zing JVM in the Zulu Prime JDK.

It is an almost completely concurrent collector:
application execution proceeds concurrently for almost all the times when the GC is active.
Additionally, young and old generation collections are concurrent with each other, 
with very short synchronization pauses.
GC pauses and GC cycles usually differ by orders of magnitude.
It is recommended to monitor both pauses and cycles.

The C4 collector is generational.
Heap occupancy and workload are only determined at the end of collections
which include the old generation.
Collections restricted to the young generation are not considered for this purpose,
because they do not establish a complete live set of objects.

Heap occupancy and workload are the same,
because the generation sizing is dynamically variable,
with no minimum size for the young generation.

Because young and old generation collections can overlap in time,
there can occasionally be phases in which allocation rate measurements are omitted.
This can lead to underreporting, which can increases with GC frequency.
