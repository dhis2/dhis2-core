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
package org.hisp.dhis.dataitem.query;

import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.hisp.dhis.common.DimensionItemType.PROGRAM_ATTRIBUTE_OPTION;
import static org.hisp.dhis.common.DimensionItemType.PROGRAM_DATA_ELEMENT_OPTION;
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

  private static final String ITEM_CODE = "item_code";

  private static final String EXPRESSION = "expression";

  private static final String OPTION_SET_UID = "optionset_uid";
  public static final String ITEM_NAME = "item_name";
  public static final String OPTION_VALUE_NAME = "optionvalue_name";
  public static final String ITEM_TYPE = "item_type";
  public static final String ITEM_SHORTNAME = "item_shortname";
  public static final String PROGRAM_SHORTNAME = "program_shortname";
  public static final String ITEM_UID = "item_uid";
  public static final String ITEM_VALUE_TYPE = "item_valuetype";
  public static final String OPTION_VALUE_UID = "optionvalue_uid";
  public static final String I18N_THIRD_NAME = "i18n_third_name";
  public static final String I18N_SECOND_NAME = "i18n_second_name";
  public static final String I18N_FIRST_NAME = "i18n_first_name";
  public static final String I18N_FIRST_SHORTNAME = "i18n_first_shortname";
  public static final String I18N_SECOND_SHORTNAME = "i18n_second_shortname";

  private ResultProcessor() {}

  /**
   * It will process the given query results, convert and populate response values into a final list
   * of DataItems.
   *
   * @param rowSet the query results
   * @return the populated list of items
   */
  static List<DataItem> process(SqlRowSet rowSet) {
    List<DataItem> dataItems = new ArrayList<>();

    while (rowSet.next()) {
      dataItems.add(
          builder()
              .name(getName(rowSet))
              .displayName(getDisplayName(rowSet))
              .id(getUid(rowSet))
              .shortName(getShortName(rowSet))
              .displayShortName(getDisplayShortName(rowSet))
              .code(rowSet.getString(ITEM_CODE))
              .dimensionItemType(getItemType(rowSet))
              .programId(rowSet.getString(PROGRAM_UID))
              .programDataElementId(getProgramDataElementId(rowSet))
              .programAttributeId(getProgramAttributeId(rowSet))
              .valueType(getValueType(rowSet))
              .expression(rowSet.getString(EXPRESSION))
              .optionSetId(rowSet.getString(OPTION_SET_UID))
              .build());
    }

    return dataItems;
  }

  private static String getProgramDataElementId(SqlRowSet rowSet) {
    if (isProgramDataElement(rowSet)) {
      return getProgramItemId(rowSet);
    }

    return null;
  }

  private static String getProgramItemId(SqlRowSet rowSet) {
    String uid = rowSet.getString(ITEM_UID);

    if (isNotBlank(rowSet.getString(PROGRAM_UID))) {
      uid = rowSet.getString(PROGRAM_UID) + "." + uid;
    }

    return uid;
  }

  private static String getProgramAttributeId(SqlRowSet rowSet) {
    if (isProgramAttribute(rowSet)) {
      return getProgramItemId(rowSet);
    }

    return null;
  }

  private static DimensionItemType getItemType(SqlRowSet rowSet) {
    if (isNotBlank(rowSet.getString(ITEM_TYPE))) {
      return valueOf(rowSet.getString(ITEM_TYPE));
    }

    return null;
  }

  private static ValueType getValueType(SqlRowSet rowSet) {
    if (isNotBlank(rowSet.getString(ITEM_VALUE_TYPE))) {
      return fromString(rowSet.getString(ITEM_VALUE_TYPE));
    }

    return null;
  }

  private static String getUid(SqlRowSet rowSet) {
    String uid = rowSet.getString(ITEM_UID);
    String itemType = rowSet.getString(ITEM_TYPE);

    boolean ignoreProgramUid =
        PROGRAM_INDICATOR.name().equalsIgnoreCase(rowSet.getString(ITEM_TYPE));

    if (isNotBlank(rowSet.getString(PROGRAM_UID)) && !ignoreProgramUid) {
      uid = rowSet.getString(PROGRAM_UID) + "." + uid;
    }

    if (hasOptionUid(itemType)) {
      uid += "." + rowSet.getString(OPTION_VALUE_UID);
    }

    return uid;
  }

  private static String getDisplayShortName(SqlRowSet rowSet) {
    String itemType = rowSet.getString(ITEM_TYPE);

    if (hasOptionUid(itemType)) {
      return String.format(
          "%s (%s, %s)",
          trimToEmpty(rowSet.getString(I18N_THIRD_NAME)),
          trimToEmpty(rowSet.getString(I18N_SECOND_NAME)),
          trimToEmpty(rowSet.getString(I18N_FIRST_SHORTNAME)));
    } else if (isNotBlank(rowSet.getString(PROGRAM_NAME))) {
      return trimToEmpty(rowSet.getString(I18N_FIRST_SHORTNAME))
          + SPACE
          + trimToEmpty(rowSet.getString(I18N_SECOND_SHORTNAME));
    } else {
      return trimToEmpty(rowSet.getString(I18N_FIRST_SHORTNAME));
    }
  }

  private static String getShortName(SqlRowSet rowSet) {
    String itemType = rowSet.getString(ITEM_TYPE);

    if (hasOptionUid(itemType)) {
      return String.format(
          "%s (%s, %s)",
          trimToEmpty(rowSet.getString(OPTION_VALUE_NAME)),
          trimToEmpty(rowSet.getString(ITEM_SHORTNAME)),
          trimToEmpty(rowSet.getString(PROGRAM_SHORTNAME)));
    } else if (isNotBlank(rowSet.getString(PROGRAM_SHORTNAME))) {
      return trimToEmpty(rowSet.getString(PROGRAM_SHORTNAME))
          + SPACE
          + trimToEmpty(rowSet.getString(ITEM_SHORTNAME));
    } else {
      return trimToEmpty(rowSet.getString(ITEM_SHORTNAME));
    }
  }

  private static String getDisplayName(SqlRowSet rowSet) {
    String itemType = rowSet.getString(ITEM_TYPE);

    if (hasOptionUid(itemType)) {
      return String.format(
          "%s (%s, %s)",
          trimToEmpty(rowSet.getString(I18N_THIRD_NAME)),
          trimToEmpty(rowSet.getString(I18N_SECOND_NAME)),
          trimToEmpty(rowSet.getString(I18N_FIRST_NAME)));
    } else if (isNotBlank(rowSet.getString(PROGRAM_NAME))) {
      return trimToEmpty(rowSet.getString(I18N_FIRST_NAME))
          + SPACE
          + trimToEmpty(rowSet.getString(I18N_SECOND_NAME));
    } else {
      return trimToEmpty(rowSet.getString(I18N_FIRST_NAME));
    }
  }

  private static boolean hasOptionUid(String itemType) {
    return PROGRAM_ATTRIBUTE_OPTION.name().equalsIgnoreCase(itemType)
        || PROGRAM_DATA_ELEMENT_OPTION.name().equalsIgnoreCase(itemType);
  }

  private static boolean isProgramAttribute(SqlRowSet rowSet) {
    DimensionItemType itemType = getItemType(rowSet);
    return PROGRAM_ATTRIBUTE_OPTION == itemType;
  }

  private static boolean isProgramDataElement(SqlRowSet rowSet) {
    return PROGRAM_DATA_ELEMENT_OPTION == getItemType(rowSet);
  }

  private static String getName(SqlRowSet rowSet) {
    String itemType = rowSet.getString(ITEM_TYPE);

    if (hasOptionUid(itemType)) {
      return String.format(
          "%s (%s, %s)",
          trimToEmpty(rowSet.getString(OPTION_VALUE_NAME)),
          trimToEmpty(rowSet.getString(ITEM_NAME)),
          trimToEmpty(rowSet.getString(PROGRAM_NAME)));
    } else if (isNotBlank(rowSet.getString(PROGRAM_NAME))) {
      return trimToEmpty(rowSet.getString(PROGRAM_NAME))
          + SPACE
          + trimToEmpty(rowSet.getString(ITEM_NAME));
    } else {
      return trimToEmpty(rowSet.getString(ITEM_NAME));
    }
  }
}
