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
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.hisp.dhis.fieldfiltering.FieldFilterParser;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.fieldfiltering.better.Fields;
import org.hisp.dhis.fieldfiltering.better.FieldsParser;
import org.hisp.dhis.fieldfiltering.better.FieldsPropertyFilter;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.webapi.controller.tracker.view.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * JMH benchmark integrated with DHIS2 Spring testing infrastructure.
 *
 * <p>This approach combines JUnit test execution with JMH benchmarking, allowing full access to
 * Spring services while maintaining proper benchmark execution semantics.
 *
 * <p>Based on: https://gist.github.com/msievers/ce80d343fc15c44bea6cbb741dde7e45
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class FieldFilterSerializationBenchmarkTest extends H2ControllerIntegrationTestBase {

  @State(Scope.Benchmark)
  public static class BenchmarkState {
    //        @Param({"25","50", "100", "200", "400", "800"})
    @Param({"25"})
    public int eventCount;

    @Param({
      //            "*", // Full serialization - baseline
      "event,dataValues", // Moderate filtering - common case
      //            "dataValues[dataElement,value]", // Deep field selection
      //            "relationships[from[trackedEntity[*]]]", // Complex nested filtering
      //            "*,!relationships", // Large exclusion - performance test
      //            "event,dataValues[*,!storedBy]" // Mixed include/exclude
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
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
    @Fork(0) // Run in same JVM to preserve Spring context
    public String benchmarkOnlyJackson(BenchmarkState state) throws JsonProcessingException {
      return BenchmarkState.objectMapper.writeValueAsString(state.events);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
    @Fork(0) // Run in same JVM to preserve Spring context
    public String benchmarkCurrentFilter(BenchmarkState state) throws JsonProcessingException {
      List<FieldPath> filter = FieldFilterParser.parse(state.fields);
      List<ObjectNode> objectNodes =
          BenchmarkState.fieldFilterService.toObjectNodes(state.events, filter);
      return BenchmarkState.objectMapper.writeValueAsString(objectNodes);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
    @Fork(0) // Run in same JVM to preserve Spring context
    public String benchmarkBetterFilter(BenchmarkState state) throws JsonProcessingException {
      Fields fields = FieldsParser.parse(state.fields);
      return BenchmarkState.filterMapper
          .writer()
          .withAttribute(FieldsPropertyFilter.FIELDS_ATTRIBUTE, fields)
          .writeValueAsString(state.events);
    }
  }

  // Spring dependencies - accessed through test instance
  @Autowired private FieldFilterService fieldFilterService;
  @Autowired private ObjectMapper objectMapper;

  @Qualifier("jsonFilterMapper")
  @Autowired
  private ObjectMapper filterMapper;

  @Test
  public void executeJmhRunner() throws Exception {
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
            .shouldDoGC(true)
            .build();

    new Runner(opt).run();
  }
}
