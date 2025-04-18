/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.test.matchers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * @author Luciano Fiandesio
 */
public class DateTimeFormatMatcher extends TypeSafeMatcher<String> {
  private final String format;

  public DateTimeFormatMatcher(String format) {
    this.format = format;
  }

  public boolean isValidFormat(String format, String value, Locale locale) {
    LocalDateTime ldt;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format, locale);

    try {
      ldt = LocalDateTime.parse(value, formatter);
      String result = ldt.format(formatter);
      return result.equals(value);
    } catch (DateTimeParseException e) {
      try {
        LocalDate ld = LocalDate.parse(value, formatter);
        String result = ld.format(formatter);
        return result.equals(value);
      } catch (DateTimeParseException exp) {
        try {
          LocalTime lt = LocalTime.parse(value, formatter);
          String result = lt.format(formatter);
          return result.equals(value);
        } catch (DateTimeParseException e2) {
          // Debugging purposes
          // e2.printStackTrace();
        }
      }
    }

    return false;
  }

  @Override
  protected boolean matchesSafely(String value) {
    return isValidFormat(this.format, value, Locale.getDefault());
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("Invalid date format. Expected [" + format + "]");
  }

  public static Matcher<String> hasDateTimeFormat(String format) {
    return new DateTimeFormatMatcher(format);
  }
}
