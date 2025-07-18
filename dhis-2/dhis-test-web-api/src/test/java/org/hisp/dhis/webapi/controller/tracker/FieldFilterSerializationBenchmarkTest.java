/*
 * Copyright (c) 2004-2023, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.webapi.controller.tracker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import org.hisp.dhis.fieldfiltering.FieldFilterParser;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.tracker.export.fieldfiltering.Fields;
import org.hisp.dhis.tracker.export.fieldfiltering.FieldsParser;
import org.hisp.dhis.tracker.export.fieldfiltering.FieldsPropertyFilter;
import org.hisp.dhis.webapi.controller.tracker.view.Event;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * JMH benchmark integrated with DHIS2 Spring testing infrastructure.
 *
 * <p>This approach combines JUnit test execution with JMH benchmarking, allowing full access to
 * Spring services (based on: <a
 * href="https://gist.github.com/msievers/ce80d343fc15c44bea6cbb741dde7e45">this gist</a>).
 *
 * <p><strong>Fork Configuration:</strong> This benchmark uses {@code @Fork(0)} to run in the same
 * JVM as the test runner. While JMH typically recommends forking for isolation, this is necessary
 * to preserve the Spring application context and dependency injection. The trade-off is acceptable
 * because:
 *
 * <ul>
 *   <li>All benchmarks run under the same conditions, so relative performance differences remain
 *       valid
 *   <li>Spring services are essential for realistic performance testing
 *   <li>The magnitude of performance differences makes minor interference negligible
 * </ul>
 */
@Tag("benchmark")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class FieldFilterSerializationBenchmarkTest extends H2ControllerIntegrationTestBase {

  @State(Scope.Benchmark)
  public static class BenchmarkState {
    @Param({"25", "50", "100", "200", "400", "800"})
    public int eventCount;

    @Param({
      "*", // serialize all so we can compare field filtering to pure Jackson serialization
      "event", // serialize only a single String field so we reduce the actual data Jackson has to
      // write and so our field filtering is what should dominate
      "*,!relationships", // default fields of /tracker/events
      "*,event~rename(foo)", // transformation with all fields
      "event~rename(foo)" // transformation with single field
    })
    public String fields;

    public List<Event> events;

    // Spring services are injected from test instance
    public static FieldFilterService fieldFilterService;
    public static ObjectMapper objectMapper;
    public static ObjectMapper filterMapper;
    public static Authentication savedAuth;

    @Setup(Level.Trial)
    public void setup() {
      events = FieldFilterSerializationTest.createEvents(eventCount);

      // JMH runs in separate threads, need to propagate SecurityContext
      if (savedAuth != null) {
        SecurityContextHolder.getContext().setAuthentication(savedAuth);
      }
    }
  }

  public static class FieldFilterBenchmarks {
    @Benchmark
    public void noFieldFiltering(BenchmarkState state, Blackhole bh)
        throws JsonProcessingException {
      bh.consume(BenchmarkState.objectMapper.writeValueAsString(state.events));
    }

    @Benchmark
    public void currentFieldFiltering(BenchmarkState state, Blackhole bh)
        throws JsonProcessingException {
      List<FieldPath> filter = FieldFilterParser.parse(state.fields);
      List<ObjectNode> objectNodes =
          BenchmarkState.fieldFilterService.toObjectNodes(state.events, filter);
      bh.consume(BenchmarkState.objectMapper.writeValueAsString(objectNodes));
    }

    @Benchmark
    public void trackerFieldFiltering(BenchmarkState state, Blackhole bh)
        throws JsonProcessingException {
      Fields fields = FieldsParser.parse(state.fields);
      bh.consume(
          BenchmarkState.filterMapper
              .writer()
              .withAttribute(FieldsPropertyFilter.FIELDS_ATTRIBUTE, fields)
              .writeValueAsString(state.events));
    }
  }

  // Spring dependencies - accessed through test instance
  @Autowired private FieldFilterService fieldFilterService;
  @Autowired private ObjectMapper objectMapper;

  @Qualifier("jsonFilterMapper")
  @Autowired
  private ObjectMapper filterMapper;

  @Test
  @Timeout(unit = TimeUnit.MINUTES, value = 200)
  void executeJmhRunner() throws Exception {
    // Inject Spring services into static benchmark state
    BenchmarkState.fieldFilterService = this.fieldFilterService;
    BenchmarkState.objectMapper = this.objectMapper;
    BenchmarkState.filterMapper = this.filterMapper;
    // Capture current authentication for propagation to benchmark threads
    BenchmarkState.savedAuth = SecurityContextHolder.getContext().getAuthentication();

    Options opt =
        new OptionsBuilder()
            .include(
                "org.hisp.dhis.webapi.controller.tracker.FieldFilterSerializationBenchmarkTest.FieldFilterBenchmarks.*")
            .shouldFailOnError(true)
            .forks(0) // Run in same JVM to preserve Spring context
            .mode(Mode.Throughput)
            .timeUnit(TimeUnit.SECONDS)
            .warmupIterations(3)
            .warmupTime(TimeValue.seconds(5))
            .measurementIterations(5)
            .measurementTime(TimeValue.seconds(5))
            .resultFormat(ResultFormatType.CSV)
            .result("jmh-result.csv")
            //            .addProfiler(AsyncProfiler.class,
            //
            // "libPath=linux-x64/libasyncProfiler.so;event=cpu;output=jfr,flamegraph;dir=target/profiler-output") // to profile: download and fix .so path https://github.com/async-profiler/async-profiler/
            .build();

    // add the events/s to the benchmark results to see the effect of the eventCount on the event
    // throughput of each implementation
    Collection<RunResult> results = new Runner(opt).run();
    List<RunResult> sortedResults = sortResults(results);
    addEventThroughputToCsv("jmh-result-enhanced.csv", sortedResults);
  }

  /**
   * Sort results by fields parameter, eventCount and then benchmark method name. This makes
   * comparison of the different implementations given the same benchmark parameters easier.
   */
  private List<RunResult> sortResults(Collection<RunResult> results) {
    List<RunResult> sortedResults = new ArrayList<>(results);
    sortedResults.sort(
        Comparator.comparing((RunResult r) -> r.getParams().getParam("fields"))
            .thenComparing(
                (RunResult r) -> {
                  String eventCountStr = r.getParams().getParam("eventCount");
                  return eventCountStr != null ? Integer.parseInt(eventCountStr) : 0;
                })
            .thenComparing(
                (RunResult r) -> {
                  String benchmark = r.getParams().getBenchmark();
                  return benchmark.substring(benchmark.lastIndexOf('.') + 1);
                }));
    return sortedResults;
  }

  private void addEventThroughputToCsv(String csvFilePath, Collection<RunResult> results) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFilePath))) {
      writeHeader(writer, results);

      for (RunResult rr : results) {
        writeDataRow(writer, rr);
      }

      System.out.println("Generated benchmark results: " + csvFilePath);
    } catch (IOException e) {
      System.err.println("Failed to write benchmark results CSV: " + e.getMessage());
    }
  }

  private void writeHeader(BufferedWriter writer, Collection<RunResult> results)
      throws IOException {
    // Get parameter keys from first result
    SortedSet<String> params =
        results.isEmpty()
            ? new TreeSet<>()
            : new TreeSet<>(results.iterator().next().getParams().getParamsKeys());

    writer.write(
        "\"Benchmark\",\"Mode\",\"Threads\",\"Samples\",\"Score\",\"Score Error (99.9% CI)\",\"Unit\"");

    for (String param : params) {
      writer.write(",\"Param: " + param + "\"");
    }

    writer.write(",\"Events/s\",\"Events/s Error (99.9% CI)\"");
    writer.newLine();
  }

  private void writeDataRow(BufferedWriter writer, RunResult rr) throws IOException {
    BenchmarkParams params = rr.getParams();
    Result result = rr.getPrimaryResult();

    // Calculate events/s and events/s error
    String eventCountStr = params.getParam("eventCount");
    double eventsPerSec = 0;
    double eventsPerSecError = 0;
    if (eventCountStr != null) {
      int eventCount = Integer.parseInt(eventCountStr);
      eventsPerSec = result.getScore() * eventCount;
      eventsPerSecError = result.getScoreError() * eventCount;
    }

    // Write CSV row
    // method name instead of full package
    String methodName = params.getBenchmark().substring(params.getBenchmark().lastIndexOf('.') + 1);
    writer.write(csvQuote(methodName));
    writer.write(",");
    writer.write(csvQuote(params.getMode().shortLabel()));
    writer.write(",");
    writer.write(String.valueOf(params.getThreads()));
    writer.write(",");
    writer.write(String.valueOf(result.getSampleCount()));
    writer.write(",");
    writer.write(String.format("%f", result.getScore()));
    writer.write(",");
    writer.write(String.format("%f", result.getScoreError()));
    writer.write(",");
    writer.write(csvQuote(result.getScoreUnit()));

    // Write parameter values (quote if contains comma)
    for (String paramKey : params.getParamsKeys()) {
      writer.write(",");
      String paramValue = params.getParam(paramKey);
      if (paramValue != null) {
        // Quote if contains comma (for values like "*,!relationships")
        if (paramValue.contains(",")) {
          writer.write("\"" + paramValue + "\"");
        } else {
          writer.write(paramValue);
        }
      }
    }

    // Write events/s and events/s error
    writer.write(",");
    writer.write(String.format("%.2f", eventsPerSec));
    writer.write(",");
    writer.write(String.format("%.2f", eventsPerSecError));
    writer.newLine();
  }

  private String csvQuote(String value) {
    if (value.contains(",")
        || value.contains(" ")
        || value.contains("\n")
        || value.contains("\r")
        || value.contains("\"")) {
      return "\"" + value.replaceAll("\"", "\"\"") + "\"";
    } else {
      return "\"" + value + "\"";
    }
  }
}
