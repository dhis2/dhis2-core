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
package org.hisp.dhis.dataitem.query;

import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.hisp.dhis.common.DimensionItemType.PROGRAM_INDICATOR;
import static org.hisp.dhis.common.DimensionItemType.valueOf;
import static org.hisp.dhis.common.ValueType.fromString;
import static org.hisp.dhis.dataitem.DataItem.builder;

import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataitem.DataItem;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * This class is responsible for providing auxiliary methods used to process query results.
 *
 * @author maikel arabori
 */
class ResultProcessor {
  private static final String PROGRAM_NAME = "program_name";

  private static final String PROGRAM_UID = "program_uid";

  private static final String EXPRESSION = "expression";

  private ResultProcessor() {}

  /**
   * It will process the given query results, convert and populate response values into a final list
   * of DataItems.
   *
   * @param rowSet the query results
   * @return the populated list of items
   */
  static List<DataItem> process(final SqlRowSet rowSet) {
    final List<DataItem> dataItems = new ArrayList<>();

    while (rowSet.next()) {
      dataItems.add(
          builder()
              .name(getName(rowSet))
              .displayName(getDisplayName(rowSet))
              .id(getUid(rowSet))
              .shortName(getShortName(rowSet))
              .displayShortName(getDisplayShortName(rowSet))
              .code(rowSet.getString("item_code"))
              .dimensionItemType(getItemType(rowSet))
              .programId(rowSet.getString(PROGRAM_UID))
              .valueType(getValueType(rowSet))
              .expression(rowSet.getString(EXPRESSION))
              .build());
    }

    return dataItems;
  }

  private static DimensionItemType getItemType(final SqlRowSet rowSet) {
    if (isNotBlank(rowSet.getString("item_type"))) {
      return valueOf(rowSet.getString("item_type"));
    }

    return null;
  }

  private static ValueType getValueType(final SqlRowSet rowSet) {
    if (isNotBlank(rowSet.getString("item_valuetype"))) {
      return fromString(rowSet.getString("item_valuetype"));
    }

    return null;
  }

  private static String getUid(final SqlRowSet rowSet) {
    final boolean ignoreProgramUid =
        PROGRAM_INDICATOR.name().equalsIgnoreCase(rowSet.getString("item_type"));

    if (isNotBlank(rowSet.getString(PROGRAM_UID)) && !ignoreProgramUid) {
      return rowSet.getString(PROGRAM_UID) + "." + rowSet.getString("item_uid");
    } else {
      return rowSet.getString("item_uid");
    }
  }

  private static String getDisplayShortName(final SqlRowSet rowSet) {
    if (isNotBlank(rowSet.getString(PROGRAM_NAME))) {
      return trimToEmpty(rowSet.getString("i18n_first_shortname"))
          + SPACE
          + trimToEmpty(rowSet.getString("i18n_second_shortname"));
    } else {
      return trimToEmpty(rowSet.getString("i18n_first_shortname"));
    }
  }

  private static String getShortName(final SqlRowSet rowSet) {
    if (isNotBlank(rowSet.getString("program_shortname"))) {
      return trimToEmpty(rowSet.getString("program_shortname"))
          + SPACE
          + trimToEmpty(rowSet.getString("item_shortname"));
    } else {
      return trimToEmpty(rowSet.getString("item_shortname"));
    }
  }

  private static String getDisplayName(final SqlRowSet rowSet) {
    if (isNotBlank(rowSet.getString(PROGRAM_NAME))) {
      return trimToEmpty(rowSet.getString("i18n_first_name"))
          + SPACE
          + trimToEmpty(rowSet.getString("i18n_second_name"));
    } else {
      return trimToEmpty(rowSet.getString("i18n_first_name"));
    }
  }

  private static String getName(final SqlRowSet rowSet) {
    if (isNotBlank(rowSet.getString(PROGRAM_NAME))) {
      return trimToEmpty(rowSet.getString(PROGRAM_NAME))
          + SPACE
          + trimToEmpty(rowSet.getString("item_name"));
    } else {
      return trimToEmpty(rowSet.getString("item_name"));
    }
  }
}
