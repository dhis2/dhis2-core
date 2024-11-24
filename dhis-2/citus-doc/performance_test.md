# DHIS2 Performance Benchmarking Report

Before diving into the results and analysis of the benchmark, the detailed data and original tables can be accessed in the following Google Sheets document: [Citus Benchmark](https://docs.google.com/spreadsheets/d/1TZjEJEoQ432UETBILDbbqVrYWCnE4OqThVk6p8yHulk/edit?gid=0#gid=0). This document contains the raw values for all scenarios and metrics tested during the benchmarking process, providing a comprehensive view of the performance measurements for both the baseline and Citus-enabled setups.

Additionally, the HTML results generated from the Gatling performance tests are available in the `citus-doc/gatling` folder. These results provide a detailed view of the test executions, including response times, throughput, and other relevant metrics, which can further support the analysis of the performance improvements observed with Citus.

## Introduction

This document presents the results of a performance benchmarking study conducted on the DHIS2 platform. The aim of the study was to evaluate potential performance improvements of using **Citus**, a PostgreSQL extension for distributed databases, compared to a standard PostgreSQL setup.

### Environment Description

The benchmarks were performed in the following environment:

- **Virtual Machines Configuration**:
    - 8GB RAM each
    - 4 CPU cores
- **Host Machine**:
    - Processor: Ryzen 9 8845HS
    - Memory: 96GB RAM
    - Operating System: Linux

### Benchmark Scope

Benchmarks were executed on two database configurations:

1. **HMIS Database**:
    - Export time of the HMIS database.
    - Focused on two particularly slow queries.
      - [query 1](http://localhost:8080/api/41/analytics/events/query/Xh88p1nyefp?dimension=ou:USER_ORGUNIT,gWTETHreVph,NI0QRzJvQ0k,Ewi7FUfcHAD:ILIKE:1,Jt68iauILtD:IN:FEMALE;MALE;OTHER,ZjibVPdslI0,WIMu5Pxg3Ty,I9WqSbljVpk,nB8yUjIepnt,BjuC5jWB8Iu:ZcbWJfYaX5n;U53tdte60Ku,DJGoXYJGidL,xjLyANK1j2T:NE:NV,THKJuSqbSAc,uPyzO7grF0C,UWuWIrUr1bW:NE:1,QN0R5zmqII3,VJFhXqOOMWO,ang4CLldbIu.ApNE5GlYotZ:IN:CLINICAL_VISIT;ISSUES,LcLuqY1lC1b:GT:1,Itnxj2T3jEr,mdbVnIgl9la,YkDsYAlmaAc,K8vtCfwzbnT,ltSJMb4WtF1:NE:NV,ve5QMylVnRa:NE:NV,Fs6H5tYrHtM:NE:NV,Xhdn49gUd52:!LIKE:1&filter=ENRjVGxVL6l:ILIKE:a&filter=sB1IHYu2xQT:!ILIKE:w&filter=fctSQp5nAYl:NE:NV&filter=sWn0CERcUYj:!ILIKE:z&headers=ouname,gWTETHreVph,NI0QRzJvQ0k,Ewi7FUfcHAD,Jt68iauILtD,ZjibVPdslI0,WIMu5Pxg3Ty,I9WqSbljVpk,lastupdated,nB8yUjIepnt,BjuC5jWB8Iu,DJGoXYJGidL,xjLyANK1j2T,THKJuSqbSAc,uPyzO7grF0C,UWuWIrUr1bW,QN0R5zmqII3,VJFhXqOOMWO,eventstatus,programstatus,scheduleddate,ang4CLldbIu.ApNE5GlYotZ,LcLuqY1lC1b,Itnxj2T3jEr,mdbVnIgl9la,YkDsYAlmaAc,K8vtCfwzbnT,ltSJMb4WtF1,ve5QMylVnRa,Fs6H5tYrHtM,Xhdn49gUd52&totalPages=false&lastUpdated=LAST_10_YEARS&eventStatus=ACTIVE,COMPLETED,SCHEDULE&programStatus=ACTIVE,COMPLETED,CANCELLED&scheduledDate=LAST_10_YEARS&displayProperty=NAME&pageSize=100&page=1&includeMetadataDetails=true&outputType=EVENT&stage=ang4CLldbIu)
      - [query 2](http://localhost:8080/api/41/analytics?dimension=dx:wPCJ11WI2ql;fQvyH9wHUEc;rCtEivd6xAv;zbPQe7kabob;P0cCSjPQoq7;oxSmWuzlaYT;wvZHYMuLtr7;m8Mk8kpT7Iw;pL17czCoSHJ;ezfpMUo7il8;bszLtmPkhVZ;nGL9E56yMU7;RLZtrCeBVvb;ppsRwp0Ta62.ACTUAL_REPORTS;jTiAV0IwOE0.REPORTING_RATE_ON_TIME;fuB7mSxsFj1;kRqIdDK9mDH;dolK91TIEj7;u4J1vq11Zuk;g1uY1rwUof8;UjVeDXPTBKZ;qRspyHVAwbi;O3n7RLXXNeJ;qRgLaKrhVgX;xb8jj9OATIl;InypjydhrCu;TXbd0RVVyBH,pe:LAST_10_YEARS&filter=ou:XKGgynPS1WZ;rO2RVJWHpCe;FRmrFTE63D0;hdeC7uX9Cko;RdNV4tTRNEo;VWGSudnonm5;MBZYTqkEgwf;YvLOmtTQD6b;W6sNfkJcXGC;quFXhkOJGB4;vBWtCmNNnCG;c4HrGRJoarj;pFCZqWnXtoU;TOgZ99Jv0bN;dOhqCNenSjS;sv6c7CpPcrc;hRQsZhmvqgS;K27JzTKmBKh;LEVEL-vFr4zVw6Avn&displayProperty=NAME&includeNumDen=false&skipMeta=true&skipData=false)
    - Included the **GetRawSpeed** scenario from the "performance-tests-gatling" suite.

2. **Sierra Leone Database**:
    - Export time of the Sierra Leone database.
    - End-to-end (E2E) tests.
    - Slow queries from E2E tests identified by the `@Tag(slowQuery)` annotation in the codebase.
    - Tracked entity (TE) only E2E tests.

### Benchmark Scenarios

The following scenarios were executed:

- **Baseline**: Standard PostgreSQL setup without Citus.
- **Scenario 1**: Citus enabled with 8 nodes (1 master, 7 workers).
- **Scenario 2**: Citus enabled with 4 nodes (1 master, 3 workers).
- **Scenario 3**: Citus enabled with 3 nodes (1 master, 2 workers).
- **Scenario 4**: Citus enabled with 2 nodes (1 master, 1 worker).

The subsequent sections detail the results of the benchmarks, providing a comparative analysis of the execution times and performance improvements across the scenarios.

### Benchmark Results

#### BaseLine

The baseline scenario was executed on a standard PostgreSQL setup without Citus. The results are as follows:


| **Metrics**           | Elapsed Time  | Seconds      |
|-----------------------|---------------|--------------|
| **HMIS Export Time**  | 30:07.832     | 1807.832     |
| **HMIS Query 1**      | 4.60 seconds  | 4.6          |
| **HMIS Query 2**      | 36.43 seconds | 35.43        |
| **HMIS Gatling Perf** | 51 Seconds    | 51           |
| **SL Export Time**    | 04:25.930     | 265.93       |
| **SL Slow Queries**   | 48.204        | 48.204       |
| **SL Analytics E2E**  | 02:31.000     | 151          |
| **SL TE E2E**         | 45.678        | 45.678       |


#### Scenario 1

The scenario with Citus enabled on 8 nodes (1 master, 7 workers) was executed. The results are as follows:

| **Metrics**           | Elapsed Time | Seconds    | Improvement (times faster) |
|-----------------------|--------------|------------|-----------------------------|
| **HMIS Export Time**  | 13:49.762    | 829.762    | ✅ 2.18 X                   |
| **HMIS Query 1**      | 0.78 seconds | 0.78       | ✅ 5.90 X                   |
| **HMIS Query 2**      | 37.98 seconds| 37.98      | ❌ 0.93 X                   |
| **HMIS Gatling Perf** | 15 seconds   | 15         | ✅ 3.40 X                   |
| **SL Export Time**    | 04:40.813    | 280.813    | ❌ 0.95 X                   |
| **SL Slow Queries**   | 32.492       | 32.492     | ✅ 1.48 X                   |
| **SL Analytics E2E**  | 02:03.000    | 123        | ✅ 1.23 X                   |
| **SL TE E2E**         | 22.628       | 22.628     | ✅ 2.02 X                   |

#### Scenario 2

The scenario with Citus enabled on 4 nodes (1 master, 3 workers) was executed. The results are as follows:

| **Metrics**          | Elapsed Time | Seconds    | Improvement (times faster) |
|----------------------|--------------|------------|-----------------------------|
| **HMIS Export Time** | 14:24.410    | 864.41     | ✅ 2.09 X                   |
| **HMIS Query 1**     | 1.07 seconds | 1.07       | ✅ 4.30 X                   |
| **HMIS Query 2**     | 41.64 seconds| 41.64      | ❌ 0.85 X                   |
| **Gatling Perf**     | 16 seconds   | 16         | ✅ 3.19 X                   |
| **SL Export Time**   | 05:32.169    | 332.169    | ❌ 0.80 X                   |
| **SL Slow Queries**  | 36.124       | 36.124     | ✅ 1.33 X                   |
| **SL Analytics E2E** | 01:58.000    | 118        | ✅ 1.28 X                   |
| **SL TE E2E**        | 25.658       | 25.658     | ✅ 1.78 X                   |

#### Scenario 3

The scenario with Citus enabled on 3 nodes (1 master, 2 workers) was executed. The results are as follows:

| **Metrics**           | Elapsed Time | Seconds    | Improvement (times faster) |
|-----------------------|--------------|------------|-----------------------------|
| **HMIS Export Time**  | 19:11.863    | 1151.863   | ✅ 1.57 X                   |
| **HMIS Query 1**      | 1.61 seconds | 1.61       | ✅ 2.86 X                   |
| **HMIS Query 2**      | 52.78 seconds| 52.78      | ❌ 0.67 X                   |
| **HMIS Gatling Perf** | SKIPPED      | -          | -                           |
| **SL Export Time**    | 06:13.373    | 373.373    | ❌ 0.71 X                   |
| **SL Slow Queries**   | 37.22        | 37.22      | ✅ 1.30 X                   |
| **SL Analytics E2E**  | 02:04.000    | 124        | ✅ 1.22 X                   |
| **SL TE E2E**         | 27.866       | 27.866     | ✅ 1.64 X                   |

#### Scenario 4

The scenario with Citus enabled on 2 nodes (1 master, 1 worker) was executed. The results are as follows:

| **Metrics**           | Elapsed Time | Seconds    | Improvement (times faster) |
|-----------------------|--------------|------------|-----------------------------|
| **HMIS Export Time**  | 32:38.873    | 1958.873   | ❌ 0.92 X                   |
| **HMIS Query 1**      | 2.41 seconds | 2.41       | ✅ 1.91 X                   |
| **HMIS Query 2**      | 74.35 seconds| 74.35      | ❌ 0.48 X                   |
| **HMIS Gatling Perf** | 25 seconds   | 25         | ✅ 2.04 X                   |
| **SL Export Time**    | 07:59.062    | 479.062    | ❌ 0.56 X                   |
| **SL Slow Queries**   | 42.701       | 42.701     | ✅ 1.13 X                   |
| **SL Analytics E2E**  | 02:10.000    | 130        | ✅ 1.16 X                   |
| **SL TE E2E**         | 41.81        | 41.81      | ✅ 1.09 X                   |

### Conclusions

Based on the benchmark results, we can draw several objective conclusions about the performance improvements achieved with Citus in comparison to a standard PostgreSQL setup.

#### 1. **Performance Improvement with Citus**

Across all scenarios with Citus enabled, there is a noticeable improvement in performance for certain metrics, especially in queries that are more computationally intensive. For example, **HMIS Query 1** consistently shows significant improvements, achieving up to **4.30 X faster** times in Scenario 2 with 4 nodes. Similarly, **Gatling Performance** also benefited from Citus, with improvements reaching up to **3.40 X faster** in Scenario 1.

#### 2. **Varying Impact Across Scenarios**

The results indicate that the degree of improvement is heavily dependent on the number of nodes in the Citus cluster. For instance, in scenarios with fewer nodes (e.g., Scenario 4 with only 2 nodes), the performance improvements are less pronounced, with **HMIS Query 2** and **SL Export Time** showing minimal or negative improvements in some cases, such as in Scenario 4 where **SL Export Time** showed a **0.56 X slower** performance. Conversely, as the number of nodes increases (e.g., Scenario 1 with 8 nodes), the improvements are more significant, especially in **HMIS Query 1** and **Gatling Performance**.

#### 3. **Limitations on Some Metrics and Citus Overhead**

Not all metrics showed improvements with Citus. Some, like **HMIS Query 2** and **SL Export Time**, did not show significant performance gains or even displayed slower times in certain scenarios (e.g., Scenario 2, Scenario 3, and Scenario 4). This suggests that Citus' ability to speed up certain types of queries may be limited, particularly for those that are more complex or less parallelizable. Additionally, Citus introduces some overhead due to the communication between nodes, which can lead to slightly degraded performance in specific cases. This overhead can affect queries that do not fully benefit from the distributed architecture, as seen with **SL Export Time** and **HMIS Query 2**, where the performance degradation might be explained by the added complexity of node coordination and data distribution across the cluster.

#### 4. **Potential for Scalability**
The data demonstrates that Citus can significantly boost performance for workloads that benefit from distributed processing, particularly with a higher number of nodes. However, its impact is less clear-cut for workloads that do not scale well across multiple nodes or for queries that are heavily dependent on single-node processing.

In conclusion, while Citus provides substantial performance benefits for specific queries, especially in the case of simple and highly parallelizable workloads, its effectiveness depends on the query structure and the number of nodes in the cluster. It is clear that Citus can offer meaningful improvements, but its performance gains may not be universal across all types of queries and scenarios.