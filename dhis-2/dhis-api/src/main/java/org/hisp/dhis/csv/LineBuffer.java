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
package org.hisp.dhis.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.ToString;

/**
 * A helper to efficiently read CSV records from a {@link BufferedReader} and split them into column
 * values.
 *
 * <p>Follows RFC 4180 with some leniency:
 *
 * <ul>
 *   <li>records end with {@code \n}, {@code \r\n} or {@code \r}
 *   <li>blank lines and records with only empty values (e.g. {@code ,,,}) are skipped
 *   <li>quoted values may contain commas, line breaks and escaped quotes ({@code ""})
 *   <li>unquoted values are trimmed, empty unquoted values are {@code null}
 *   <li>indent around a quoted value is ignored
 * </ul>
 *
 * <p>A quoted value that is never closed swallows the rest of the input (as with any RFC 4180
 * reader). To keep this from buffering an entire upload in memory a quoted value is limited to
 * {@link #MAX_QUOTED_VALUE_LENGTH} characters.
 *
 * @author Jan Bernitt
 */
@ToString
final class LineBuffer {

  /**
   * Maximum length of a single quoted value. Far above any legitimate value (data values and
   * comments are limited to 50k characters) but bounds the memory a stray quote can consume.
   */
  static final int MAX_QUOTED_VALUE_LENGTH = 1_000_000;

  private final int columns;
  private final StringBuilder value = new StringBuilder(64);
  private List<String> values = List.of();

  /** number of line breaks consumed so far, the current line is {@code lines + 1} */
  private int lines;

  public static LineBuffer of(int columns) {
    return new LineBuffer(columns);
  }

  private LineBuffer(int columns) {
    this.columns = columns;
  }

  /**
   * Reads the next record that has at least one non-empty value.
   *
   * @return true if a record was read, false if the end of input was reached
   * @throws IllegalArgumentException if a quoted value is not terminated or too long
   */
  boolean readLine(BufferedReader csv) throws IOException {
    while (true) {
      int c = read(csv);
      while (c == '\r' || c == '\n') c = read(csv);
      if (c == -1) return false;
      List<String> res = new ArrayList<>(columns);
      boolean empty = true;
      while (true) {
        while (isIndent(c)) c = read(csv);
        value.setLength(0);
        if (c == '"') {
          c = readQuoted(csv);
          res.add(value.toString());
          empty = false;
          // ignore anything between closing quote and the next comma
          while (c != ',' && !isEOL(c)) c = read(csv);
        } else {
          while (c != ',' && !isEOL(c)) {
            value.append((char) c);
            c = read(csv);
          }
          String str = value.toString().trim();
          res.add(str.isEmpty() ? null : str);
          if (!str.isEmpty()) empty = false;
        }
        if (c != ',') break;
        c = read(csv);
      }
      if (c == '\r') {
        csv.mark(1);
        if (csv.read() != '\n') csv.reset();
      }
      if (!empty) {
        values = res;
        return true;
      }
    }
  }

  /**
   * Reads a quoted value into {@link #value} starting after the opening quote.
   *
   * @return the first character after the closing quote
   */
  private int readQuoted(BufferedReader csv) throws IOException {
    int startLine = lines + 1;
    while (true) {
      int c = read(csv);
      if (c == -1)
        throw new IllegalArgumentException(
            "Quoted value starting in line %d is not terminated".formatted(startLine));
      if (c == '"') {
        c = read(csv);
        if (c != '"') return c;
      }
      if (value.length() >= MAX_QUOTED_VALUE_LENGTH)
        throw new IllegalArgumentException(
            "Quoted value starting in line %d exceeds %d characters"
                .formatted(startLine, MAX_QUOTED_VALUE_LENGTH));
      value.append((char) c);
    }
  }

  /** Reads a character and counts line breaks ({@code \r\n} counts once). */
  private int read(BufferedReader csv) throws IOException {
    int c = csv.read();
    if (c == '\n') {
      lines++;
    } else if (c == '\r') {
      csv.mark(1);
      int next = csv.read();
      csv.reset();
      if (next != '\n') lines++;
    }
    return c;
  }

  List<String> split() {
    return values;
  }

  private static boolean isEOL(int c) {
    return c == -1 || c == '\n' || c == '\r';
  }

  private static boolean isIndent(int c) {
    return c == ' ' || c == '\t';
  }
}
