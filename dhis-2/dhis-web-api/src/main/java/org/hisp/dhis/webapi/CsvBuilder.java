/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.webapi;

import static java.util.stream.Collectors.joining;

import com.csvreader.CsvWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import org.hisp.dhis.system.util.CsvUtils;

/**
 * CSV writer specifically tailored to the Gist API needs.
 *
 * @author Jan Bernitt
 */
public final class CsvBuilder {
  public enum Preference {
    SKIP_HEADERS,
    EXPLICIT_NULLS
  }

  private final CsvWriter out;

  private final EnumSet<Preference> preferences = EnumSet.noneOf(Preference.class);

  private DateTimeFormatter dateTimeFormatter;

  public CsvBuilder(PrintWriter out) {
    this(out, CsvUtils.DELIMITER);
  }

  public CsvBuilder(PrintWriter out, char delimiter) {
    this.out = new CsvWriter(out, delimiter);
    withLocale(Locale.getDefault());
  }

  public CsvBuilder with(Preference preference) {
    preferences.add(preference);
    return this;
  }

  public CsvBuilder without(Preference preference) {
    preferences.remove(preference);
    return this;
  }

  public CsvBuilder withLocale(Locale locale) {
    this.dateTimeFormatter =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withLocale(locale == null ? Locale.getDefault() : locale);
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

  public void toRows(List<String> fields, List<?> values) {
    try {
      if (!preferences.contains(Preference.SKIP_HEADERS)) {
        for (String header : fields) {
          out.write(header);
        }
        out.endRecord();
      }
      final int columns = fields.size();
      for (Object value : values) {
        if (value instanceof Object[]) {
          Object[] row = (Object[]) value;
          for (int c = 0; c < columns; c++) {
            out.write(toCsvValue(row[c]));
          }
        } else {
          out.write(toCsvValue(value));
        }
        out.endRecord();
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public boolean isNullEmpty() {
    return !preferences.contains(Preference.EXPLICIT_NULLS);
  }

  private String toCsvValue(Object value) {
    if (value == null) {
      return isNullEmpty() ? "" : "null";
    }
    if (value instanceof Object[]) {
      return toCsvValue((Object[]) value);
    }
    if (value instanceof Collection<?>) {
      return toCsvValue((Collection<?>) value);
    }
    if (value instanceof Date) {
      return dateTimeFormatter.format(
          LocalDateTime.ofInstant(Instant.ofEpochMilli(((Date) value).getTime()), ZoneOffset.UTC));
    }
    return value.toString();
  }

  private String toCsvValue(Object[] items) {
    if (items.length == 0) {
      return toCsvValue((Object) null);
    }
    if (items.length == 1) {
      return toCsvValue(items[0]);
    }
    if (items.length < 10 && items[0] instanceof Number) {
      return Arrays.stream(items).map(String::valueOf).collect(joining(" "));
    }
    return "(" + items.length + " elements)";
  }

  private String toCsvValue(Collection<?> items) {
    if (items.isEmpty()) {
      return toCsvValue((Object) null);
    }
    if (items.size() == 1) {
      return toCsvValue(items.iterator().next());
    }
    if (items.size() < 10 && items.iterator().next() instanceof Number) {
      return items.stream().map(String::valueOf).collect(joining(" "));
    }
    return "(" + items.size() + " elements)";
  }
}
