/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DimensionConstants {
  public static final String DATA_X_DIM_ID = "dx"; // in, de, ds, do

  public static final String DATA_COLLAPSED_DIM_ID = "dy"; // Collapsed event data dimensions

  public static final String CATEGORYOPTIONCOMBO_DIM_ID = "co";

  public static final String ATTRIBUTEOPTIONCOMBO_DIM_ID = "ao";

  public static final String PERIOD_DIM_ID = "pe";

  public static final String ORGUNIT_DIM_ID = "ou";

  public static final String ORGUNIT_GROUP_DIM_ID = "oug"; // Used for org unit target

  public static final String ITEM_DIM_ID = "item";

  public static final String DIMENSION_SEP = "-";

  public static final String LONGITUDE_DIM_ID = "longitude";

  public static final String LATITUDE_DIM_ID = "latitude";

  public static final String DIMENSION_NAME_SEP = ":";

  public static final String PERIOD_FREE_RANGE_SEPARATOR = "_";

  public static final String QUERY_MODS_ID_SEPARATOR = "_";

  public static final String OPTION_SEP = ";";

  public static final String MULTI_CHOICES_OPTION_SEP = ",";

  public static final String ITEM_SEP = "-";

  public static final String DIMENSION_IDENTIFIER_SEP = ".";

  public static final String VALUE_COLUMN_NAME = "value";

  public static final String TEXTVALUE_COLUMN_NAME = "textvalue";
}
