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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Simple JMH benchmark example to demonstrate working JMH setup in DHIS2.
 *
 * <p>This example shows how to: 1. Set up JMH benchmarks in the DHIS2 project structure 2. Use
 * parameterized benchmarks 3. Create synthetic data for testing 4. Run benchmarks both in IDE and
 * via Maven CLI
 *
 * <p>To run this benchmark: - In IDE: Ensure JMH profile is active, then run main() method - Maven
 * CLI: mvn clean compile test-compile -Pjmh && java -cp "$(mvn dependency:build-classpath -q
 * -Dmdep.outputFile=/dev/stdout -Pjmh):target/test-classes"
 * org.hisp.dhis.webapi.controller.tracker.SimpleBenchmarkExample
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class SimpleBenchmarkExample {

  @Param({"100", "1000", "10000"})
  private int listSize;

  private List<String> testData;

  @Setup(Level.Trial)
  public void setup() {
    testData = new ArrayList<>(listSize);
    for (int i = 0; i < listSize; i++) {
      testData.add("item-" + i);
    }
  }

  @Benchmark
  public int benchmarkArrayListIteration() {
    int sum = 0;
    for (String item : testData) {
      sum += item.length();
    }
    return sum;
  }

  @Benchmark
  public int benchmarkArrayListIndexAccess() {
    int sum = 0;
    for (int i = 0; i < testData.size(); i++) {
      sum += testData.get(i).length();
    }
    return sum;
  }

  @Benchmark
  public long benchmarkStreamOperations() {
    return testData.stream().mapToInt(String::length).sum();
  }

  public static void main(String[] args) throws RunnerException {
    Options opt =
        new OptionsBuilder()
            .include(SimpleBenchmarkExample.class.getSimpleName())
            .jvmArgs("-Xmx1g")
            .build();

    new Runner(opt).run();
  }
}
