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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
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
  private final byte[] sinkBuf = new byte[28];

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
    byte[] b = new byte[28];
    Render.toLongGmtDate(next(), b);
    return new String(b, 0, 28, StandardCharsets.US_ASCII);
  }

  // ---- Hand-rolled renderer + CharSequence wrapper ----
  @Benchmark
  public AsciiTimestamp handwritten_cs() {
    byte[] b = new byte[28];
    Render.toLongGmtDate(next(), b);
    return new AsciiTimestamp(b, 28);
  }

  // ---- Trap: what if the consumer calls toString()? ----
  @Benchmark
  public String handwritten_cs_toString() {
    byte[] b = new byte[28];
    Render.toLongGmtDate(next(), b);
    return new AsciiTimestamp(b, 28).toString();
  }

  // ---- Sink: true zero alloc (the floor to compare against) ----
  @Benchmark
  public void sink_zero_alloc(Blackhole bh) {
    Render.toLongGmtDate(next(), sinkBuf);
    bh.consume(Arrays.hashCode(sinkBuf)); // forces all 28 bytes to be materialized
  }

  // ---- The renderer (same as previous answer, inlined for self-containment) ----
  static final class Render {
    private static final byte[] DIGITS = "0123456789".getBytes(StandardCharsets.US_ASCII);

    static void toLongGmtDate(Date date, byte[] b) {
      long ms = date.getTime();
      long epochDay = Math.floorDiv(ms, 86_400_000L);
      long msOfDay = ms - epochDay * 86_400_000L;

      long z = epochDay + 719468L;
      long era = (z >= 0 ? z : z - 146096) / 146097;
      long doe = z - era * 146097;
      long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
      long y = yoe + era * 400;
      long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
      long mp = (5 * doy + 2) / 153;
      long d = doy - (153 * mp + 2) / 5 + 1;
      long m = mp < 10 ? mp + 3 : mp - 9;
      if (m <= 2) y++;

      int hour = (int) (msOfDay / 3_600_000L);
      int min = (int) (msOfDay / 60_000L % 60);
      int sec = (int) (msOfDay / 1_000L % 60);
      int mil = (int) (msOfDay % 1_000L);

      int p = 0;
      p = w4(b, p, (int) y);
      b[p++] = '-';
      p = w2(b, p, (int) m);
      b[p++] = '-';
      p = w2(b, p, (int) d);
      b[p++] = 'T';
      p = w2(b, p, hour);
      b[p++] = ':';
      p = w2(b, p, min);
      b[p++] = ':';
      p = w2(b, p, sec);
      b[p++] = '.';
      p = w3(b, p, mil);
      b[p++] = '+';
      b[p++] = '0';
      b[p++] = '0';
      b[p++] = '0';
      b[p++] = '0';
    }

    private static int w2(byte[] b, int p, int v) {
      b[p] = DIGITS[v / 10];
      b[p + 1] = DIGITS[v % 10];
      return p + 2;
    }

    private static int w3(byte[] b, int p, int v) {
      b[p] = DIGITS[v / 100];
      b[p + 1] = DIGITS[v / 10 % 10];
      b[p + 2] = DIGITS[v % 10];
      return p + 3;
    }

    private static int w4(byte[] b, int p, int v) {
      b[p] = DIGITS[v / 1000];
      b[p + 1] = DIGITS[v / 100 % 10];
      b[p + 2] = DIGITS[v / 10 % 10];
      b[p + 3] = DIGITS[v % 10];
      return p + 4;
    }
  }

  // ---- The CharSequence wrapper (final class, not record — see prior discussion) ----
  public static final class AsciiTimestamp implements CharSequence {
    private final byte[] buf;
    private final int len;

    AsciiTimestamp(byte[] buf, int len) {
      this.buf = buf;
      this.len = len;
    }

    @Override
    public int length() {
      return len;
    }

    @Override
    public char charAt(int i) {
      if (i < 0 || i >= len) throw new IndexOutOfBoundsException(i);
      return (char) (buf[i] & 0xFF);
    }

    @Override
    public CharSequence subSequence(int s, int e) {
      throw new UnsupportedOperationException(); // not on the hot path
    }

    @Override
    public String toString() {
      return new String(buf, 0, len, StandardCharsets.US_ASCII);
    }
  }

  public static void main(String[] args) throws RunnerException {
    new Runner(new OptionsBuilder().include(ToLongGmtDateBenchmark.class.getSimpleName()).build())
        .run();
  }
}
