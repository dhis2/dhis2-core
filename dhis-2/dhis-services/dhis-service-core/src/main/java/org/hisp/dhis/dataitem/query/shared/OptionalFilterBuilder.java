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
package org.hisp.dhis.dataitem.query.shared;

import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.rootJunction;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * This class is responsible for encapsulating the filtering logic and conditions for optional
 * filters.
 *
 * @author maikel arabori
 */
public class OptionalFilterBuilder {
  private final StringBuilder sb = new StringBuilder();

  private final MapSqlParameterSource paramsMap;

  private boolean first = true;

  private String rootJunction = " and ";

  public OptionalFilterBuilder(MapSqlParameterSource paramsMap) {
    this.paramsMap = paramsMap;
  }

  /**
   * Appends a new filter to the be current builder. It will add an additional AND string before the
   * first string appended, and will close it once toString() is invoked. It will automatically
   * handle rootJunctions (AND | OR).
   *
   * @param filter the filter to be appended
   * @return the current instance of this class
   */
  public OptionalFilterBuilder append(String filter) {
    rootJunction = rootJunction(paramsMap);

    if (isNotBlank(filter) && paramsMap != null) {
      if (first) {
        // Opening AND for optional filters.
        sb.append(" and (");
        first = false;
      }

      sb.append(filter);
      sb.append(rootJunction(paramsMap));
    }

    return this;
  }

  /**
   * Prefix the filter with AND or OR condition.
   *
   * @param filter the filter to be appended
   * @return the current instance of this class
   */
  public OptionalFilterBuilder append(String filter, String andOr) {
    if (isNotBlank(andOr)) {
      rootJunction = andOr;
    }

    if (isNotBlank(filter) && paramsMap != null) {
      if (first) {
        // Opening AND | OR for optional filters.
        sb.append(andOr + " (");
        first = false;
      }

      sb.append(SPACE + filter + SPACE);
      sb.append(andOr);
    }

    return this;
  }

  /**
   * Will close the list of appended filters and return the final statement for the optional filters
   * .
   *
   * @return the final filter statement
   */
  public String toString() {
    String fullFilterStatement = sb.toString();

    if (isNotBlank(fullFilterStatement)) {
      // Closing AND
      fullFilterStatement = fullFilterStatement + ")";

      // Removing the last unnecessary root junction from optional
      // filters and returning.
      fullFilterStatement = fullFilterStatement.replace(rootJunction + ")", ") ");
    }

    return fullFilterStatement;
  }
}
