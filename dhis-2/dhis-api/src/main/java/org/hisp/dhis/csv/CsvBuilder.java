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

import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import org.hisp.dhis.jsontree.Appender;
import org.hisp.dhis.util.DateUtils;

/**
 * A generic CSV writer.
 *
 * <p>Output is written through the given {@link Appender}, which the caller is responsible for
 * closing — typically via try-with-resources around {@link Appender#ofUTF8(java.io.OutputStream)}.
 * Output is UTF-8 regardless of platform default charset.
 *
 * <p>Format: LF record separators, {@code "} as quotes, embedded quotes doubled. Every field is
 * trimmed of leading and trailing whitespace before being written, matching the behaviour of the
 * previous {@code com.csvreader.CsvWriter} that this class replaces. A field is quoted when it
 * contains the delimiter, a quote, CR or LF. A {@code null} or empty first field is written as
 * {@code ""} rather than as an empty cell, so a row whose first field is empty is not misread as
 * the end of input by line-based CSV readers (including DHIS2's own {@link CSV}).
 *
 * @author Jan Bernitt
 */
public final class CsvBuilder {
  public enum Preference {
    SKIP_HEADERS,
    EXPLICIT_NULLS
  }

  private final Appender out;
  private final char delimiter;
  private final EnumSet<Preference> preferences;

  public CsvBuilder(Appender out) {
    this(out, ',');
  }

  public CsvBuilder(Appender out, char delimiter) {
    this(out, delimiter, EnumSet.noneOf(Preference.class));
  }

  private CsvBuilder(Appender out, char delimiter, EnumSet<Preference> preferences) {
    this.out = out;
    this.delimiter = delimiter;
    this.preferences = preferences;
  }

  public CsvBuilder withDelimiter(char delimiter) {
    return new CsvBuilder(out, delimiter, preferences.clone());
  }

  public CsvBuilder with(Preference preference) {
    preferences.add(preference);
    return this;
  }

  public CsvBuilder without(Preference preference) {
    preferences.remove(preference);
    return this;
  }

  public CsvBuilder nullAsEmpty() {
    return without(Preference.EXPLICIT_NULLS);
  }

  public CsvBuilder nullAsNull() {
    return with(Preference.EXPLICIT_NULLS);
  }

  public CsvBuilder skipHeaders(boolean skipHeaders) {
    return skipHeaders ? with(Preference.SKIP_HEADERS) : without(Preference.SKIP_HEADERS);
  }

  public void toRows(List<String> fields, Stream<?> values) {
    if (!preferences.contains(Preference.SKIP_HEADERS)) {
      int i = 0;
      for (String header : fields) {
        if (i > 0) out.appendAscii(delimiter);
        writeField(header, i == 0);
        i++;
      }
      endRecord();
    }
    final int columns = fields.size();
    Iterator<?> iter = values.iterator();
    while (iter.hasNext()) {
      int i = 0;
      Object value = iter.next();
      if (value instanceof Object[] row) {
        if (row.length == 0) continue;
        for (int c = 0; c < columns; c++) {
          if (i > 0) out.appendAscii(delimiter);
          writeField(row[c], i == 0);
          i++;
        }
      } else {
        writeField(value, true);
      }
      endRecord();
    }
  }

  public boolean isNullEmpty() {
    return !preferences.contains(Preference.EXPLICIT_NULLS);
  }

  private void writeField(Object value, boolean first) {
    if (value instanceof Object[] arr) {
      writeArrayValue(arr, first);
    } else if (value instanceof Collection<?> c) {
      writeCollectionValue(c, first);
    } else writeSimpleValue(value, first);
  }

  private void writeSimpleValue(Object value, boolean first) {
    if (value == null) {
      writeNullValue(first);
    } else if (value instanceof Boolean b) {
      out.appendAscii(b ? "true" : "false");
    } else if (value instanceof Number n) {
      writeNumber(n, first);
    } else if (value instanceof Date date) {
      writeCharSequence(DateUtils.toIso8601(date), first);
    } else if (value instanceof CharSequence cs) {
      writeCharSequence(cs, first);
    } else writeCharSequence(value.toString(), first);
  }

  private void writeNumber(Number value, boolean first) {
    if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
      out.appendInt(value.intValue());
    } else if (value instanceof Long l) {
      out.appendLong(l);
    } else if (value instanceof Double d) {
      out.appendDouble(d);
    } else if (value instanceof Float f) {
      out.appendDouble(f.doubleValue());
    } else
      // BigInteger, BigDecimal, or custom Number — their toString may differ from
      // the Appender primitive writers, so route through the
      // general CharSequence path to preserve the exact text and quote check.
      writeCharSequence(value.toString(), first);
  }

  private void writeNullValue(boolean first) {
    if (isNullEmpty()) {
      if (first) out.appendAscii("\"\"");
    } else {
      out.appendAscii("null");
    }
  }

  /**
   * Writes a {@link CharSequence} value with trim and quote handling. Trimming is done by computing
   * {@code start}/{@code end} offsets on the original sequence rather than materialising a
   * substring — the whole path works on views and never allocates a {@code String} for the trimmed
   * value.
   */
  private void writeCharSequence(CharSequence value, boolean first) {
    int start = 0;
    int end = value.length();
    // trim leading and trailing whitespace (matches String.trim: chars <= ' ')
    while (start < end && value.charAt(start) <= ' ') start++;
    while (end > start && value.charAt(end - 1) <= ' ') end--;

    if (start == end) {
      if (first) out.appendAscii("\"\"");
      return;
    }
    if (!needsQuoting(value, start, end)) {
      out.appendEncoded(value, start, end);
      return;
    }
    out.appendAscii('"');
    int runStart = start;
    for (int i = start; i < end; i++) {
      if (value.charAt(i) == '"') {
        if (i > runStart) out.appendEncoded(value, runStart, i);
        out.appendAscii('"');
        out.appendAscii('"');
        runStart = i + 1;
      }
    }
    if (runStart < end) out.appendEncoded(value, runStart, end);
    out.appendAscii('"');
  }

  private void writeArrayValue(Object[] items, boolean first) {
    if (items.length == 0) {
      writeNullValue(first);
      return;
    }
    if (items.length == 1) {
      writeField(items[0], first);
      return;
    }
    if (items.length < 10 && items[0] instanceof Number) {
      // few numbers — space-separated, no quoting needed (numbers have no special chars)
      for (int i = 0; i < items.length; i++) {
        if (i > 0) out.appendAscii(' ');
        writeNumElement(items[i]);
      }
      return;
    }
    writeNumElements(items.length);
  }

  private void writeNumElement(Object e) {
    if (e instanceof Number n) {
      writeNumber(n, false);
    } else {
      out.appendAscii('?');
    }
  }

  private void writeCollectionValue(Collection<?> items, boolean first) {
    int size = items.size();
    if (size == 0) {
      writeNullValue(first);
      return;
    }
    if (size == 1) {
      writeField(items.iterator().next(), first);
      return;
    }
    if (size < 10) {
      Iterator<?> it = items.iterator();
      Object firstItem = it.next();
      if (firstItem instanceof Number n) {
        writeNumElement(n);
        while (it.hasNext()) {
          out.appendAscii(' ');
          writeNumElement(it.next());
        }
        return;
      }
    }
    writeNumElements(size);
  }

  private void writeNumElements(int n) {
    out.appendAscii('(');
    out.appendInt(n);
    out.appendAscii(" elements)");
  }

  private boolean needsQuoting(CharSequence value, int start, int end) {
    for (int i = start; i < end; i++) {
      char c = value.charAt(i);
      if (c == delimiter || c == '"' || c == '\r' || c == '\n') return true;
    }
    return false;
  }

  private void endRecord() {
    out.appendAscii('\n');
  }
}
