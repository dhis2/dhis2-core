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
package org.hisp.dhis.analytics.data.sql;

import java.util.List;
import lombok.experimental.UtilityClass;

/**
 * Constants used in analytics SQL queries. This class contains column names and common prefixes
 * used across the analytics module.
 */
@UtilityClass
public final class AnalyticsColumns {

  /** Time-related columns */
  public static final String YEAR = "year";

  public static final String PESTARTDATE = "pestartdate";
  public static final String PEENDDATE = "peenddate";
  public static final String DAYSNO = "daysno";

  /** Value columns */
  public static final String VALUE = "value";

  public static final String TEXTVALUE = "textvalue";
  public static final String DAYSXVALUE = "daysxvalue";

  /** Organization unit related columns */
  public static final String OULEVEL = "oulevel";

  public static final String APPROVALLEVEL = "approvallevel";

  /** Dimension identifiers */
  public static final String DX = "dx";

  public static final String OU = "ou";
  public static final String CO = "co";
  public static final String AO = "ao";

  /** Common constants */
  public static final int LAST_VALUE_YEARS_OFFSET = -10;

  /** Returns all value-related columns */
  public static List<String> getValueColumns() {
    return List.of(VALUE, TEXTVALUE, DAYSXVALUE, DAYSNO);
  }

  /** Returns all time-related columns */
  public static List<String> getTimeColumns() {
    return List.of(YEAR, PESTARTDATE, PEENDDATE);
  }

  /** Returns all dimension columns */
  public static List<String> getDimensionColumns() {
    return List.of(DX, OU, CO, AO);
  }

  /** Returns all columns required for first/last value queries */
  public static List<String> getFirstLastValueColumns() {
    return List.of(YEAR, PESTARTDATE, PEENDDATE, OULEVEL, DAYSXVALUE, DAYSNO, VALUE, TEXTVALUE);
  }
}
