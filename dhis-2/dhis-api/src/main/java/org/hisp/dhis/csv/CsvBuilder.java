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

import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import org.hisp.dhis.jsontree.Appender;
import org.hisp.dhis.util.DateUtils;

/**
 * CSV writer specifically tailored to the Gist API needs.
 *
 * <p>Output is written through the given {@link Appender}, which the caller is responsible for
 * closing — typically via try-with-resources around {@link Appender#ofUTF8(java.io.OutputStream)}.
 * Output is UTF-8 regardless of platform default charset.
 *
 * <p>Format: CRLF record separators, {@code "} text qualifier, embedded quotes doubled. A field is
 * quoted when it contains the delimiter, a quote, CR or LF, or when it starts with a space or tab
 * (leading whitespace in an unquoted field would be trimmed by the reader).
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
    return new CsvBuilder(out, delimiter, preferences);
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
        writeField(header);
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
        for (int c = 0; c < columns; c++) {
          if (i > 0) out.appendAscii(delimiter);
          writeField(toCsvValue(row[c]));
          i++;
        }
      } else {
        writeField(toCsvValue(value));
      }
      endRecord();
    }
  }

  public boolean isNullEmpty() {
    return !preferences.contains(Preference.EXPLICIT_NULLS);
  }

  private void writeField(String value) {
    if (value == null || value.isEmpty()) return;
    int len = value.length();
    if (!needsQuoting(value)) {
      out.appendEncoded(value);
      return;
    }
    out.appendAscii('"');
    int start = 0;
    for (int i = 0; i < len; i++) {
      if (value.charAt(i) == '"') {
        if (i > start) out.appendEncoded(value, start, i);
        out.appendAscii('"');
        out.appendAscii('"');
        start = i + 1;
      }
    }
    if (start < len) out.appendEncoded(value, start, len);
    out.appendAscii('"');
  }

  private boolean needsQuoting(String value) {
    int len = value.length();
    char first = value.charAt(0);
    boolean needsQuoting = first == ' ' || first == '\t';
    if (!needsQuoting) {
      for (int i = 0; i < len; i++) {
        char c = value.charAt(i);
        if (c == delimiter || c == '"' || c == '\r' || c == '\n') {
          needsQuoting = true;
          break;
        }
      }
    }
    return needsQuoting;
  }

  private void endRecord() {
    out.appendAscii('\n');
  }

  private String toCsvValue(Object value) {
    if (value == null) return isNullEmpty() ? "" : "null";
    if (value instanceof Object[] arr) return toCsvValue(arr);
    if (value instanceof Collection<?> c) return toCsvValue(c);
    if (value instanceof Date date) return DateUtils.toIso8601(date);
    return value.toString();
  }

  private String toCsvValue(Object[] items) {
    if (items.length == 0) return toCsvValue((Object) null);
    if (items.length == 1) return toCsvValue(items[0]);
    if (items.length < 10 && items[0] instanceof Number)
      return Arrays.stream(items).map(String::valueOf).collect(joining(" "));
    return "(" + items.length + " elements)";
  }

  private String toCsvValue(Collection<?> items) {
    if (items.isEmpty()) return toCsvValue((Object) null);
    if (items.size() == 1) return toCsvValue(items.iterator().next());
    if (items.size() < 10 && items.iterator().next() instanceof Number)
      return items.stream().map(String::valueOf).collect(joining(" "));
    return "(" + items.size() + " elements)";
  }
}
