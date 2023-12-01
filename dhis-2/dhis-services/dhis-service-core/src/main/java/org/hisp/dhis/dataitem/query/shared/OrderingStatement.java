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

import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.hisp.dhis.dataitem.query.shared.ParamPresenceChecker.hasNonBlankStringPresence;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.DISPLAY_NAME_ORDER;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.DISPLAY_SHORT_NAME_ORDER;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.NAME_ORDER;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.SHORT_NAME_ORDER;

import lombok.NoArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * This class held common ordering SQL statements for data items.
 *
 * @author maikel arabori
 */
@NoArgsConstructor(access = PRIVATE)
public class OrderingStatement {
  private static final String ORDER_BY = " order by ";

  public static String ordering(
      String displayNameOrderingColumns,
      String nameOrderingColumns,
      String displayShortNameOrderingColumns,
      String shortNameOrderingColumns,
      MapSqlParameterSource paramsMap) {
    if (hasNonBlankStringPresence(paramsMap, DISPLAY_NAME_ORDER)
        && isNotBlank(displayNameOrderingColumns)) {
      return buildOrderByStatement(
          displayNameOrderingColumns, (String) paramsMap.getValue(DISPLAY_NAME_ORDER));
    } else if (hasNonBlankStringPresence(paramsMap, NAME_ORDER)
        && isNotBlank(nameOrderingColumns)) {
      return buildOrderByStatement(nameOrderingColumns, (String) paramsMap.getValue(NAME_ORDER));
    } else if (hasNonBlankStringPresence(paramsMap, SHORT_NAME_ORDER)
        && isNotBlank(nameOrderingColumns)) {
      return buildOrderByStatement(
          shortNameOrderingColumns, (String) paramsMap.getValue(SHORT_NAME_ORDER));
    } else if (hasNonBlankStringPresence(paramsMap, DISPLAY_SHORT_NAME_ORDER)
        && isNotBlank(displayShortNameOrderingColumns)) {
      return buildOrderByStatement(
          displayShortNameOrderingColumns, (String) paramsMap.getValue(DISPLAY_SHORT_NAME_ORDER));
    }

    return EMPTY;
  }

  private static String buildOrderByStatement(String displayOrderingColumns, String ascOrDesc) {
    StringBuilder orderBy = new StringBuilder();
    String[] columns = trimToEmpty(displayOrderingColumns).split(",");
    boolean hasElement = false;

    if (columns != null && columns.length > 0) {
      for (String column : columns) {
        if (isNotBlank(column)) {
          if (!hasElement) {
            orderBy.append(ORDER_BY);
          }

          orderBy.append(column + SPACE + ascOrDesc + ",");
          hasElement = true;
        }
      }

      if (hasElement) {
        // Delete last extra comma.
        orderBy.deleteCharAt(orderBy.length() - 1);
      }
    }

    return orderBy.toString();
  }
}
