/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.benchmarks;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.hisp.dhis.util.DateUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(2)
@State(Scope.Thread)
public class ToLongGmtDateBenchmark {

  // 256 dates spread across 2015–2023, with varied millis-of-day.
  private static final Date[] DATES = new Date[256];

  static {
    long base = 1_420_070_400_000L; // 2015-01-01T00:00:00Z
    long step = 1_100_000_000L; // ~12.7 days apart
    for (int i = 0; i < DATES.length; i++) DATES[i] = new Date(base + i * step);
  }

  private static final DateTimeFormatter JODA =
      DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").withZoneUTC();

  private int idx;

  @Setup(Level.Iteration)
  public void resetIdx() {
    idx = 0;
  }

  private Date next() {
    Date d = DATES[idx];
    idx = (idx + 1) & 0xFF;
    return d;
  }

  // ---- Baseline: current Joda path ----
  @Benchmark
  public String joda_string() {
    return JODA.print(new org.joda.time.DateTime(next()));
  }

  // ---- Hand-rolled renderer + String ----
  @Benchmark
  public String handwritten_string() {
    return DateUtils.toLongGmtDate(next());
  }

  // ---- Hand-rolled renderer + CharSequence wrapper ----
  @Benchmark
  public CharSequence handwritten_cs() {
    return DateUtils.toLongGmtDateText(next());
  }

  // ---- Trap: what if the consumer calls toString()? ----
  @Benchmark
  public String handwritten_cs_toString() {
    return DateUtils.toLongGmtDateText(next()).toString();
  }

  public static void main(String[] args) throws RunnerException {
    new Runner(new OptionsBuilder().include(ToLongGmtDateBenchmark.class.getSimpleName()).build())
        .run();
  }
}
