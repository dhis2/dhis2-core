/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AnalyticsConstants {
  public static final String ANALYTICS_TBL_ALIAS = "ax";

  public static final String ANALYTICS_TBL_TEMP_SUFFIX = "_temp";

  public static final String OWNERSHIP_TBL_ALIAS = "own";

  public static final String DATE_PERIOD_STRUCT_ALIAS = "ps";

  public static final String ORG_UNIT_STRUCT_ALIAS = "ous";

  public static final String ORG_UNIT_GROUPSET_STRUCT_ALIAS = "ougs";

  public static final String NULL = "null";

  public static final String KEY_USER_ORGUNIT = "USER_ORGUNIT";

  public static final String KEY_USER_ORGUNIT_CHILDREN = "USER_ORGUNIT_CHILDREN";

  public static final String KEY_USER_ORGUNIT_GRANDCHILDREN = "USER_ORGUNIT_GRANDCHILDREN";

  public static final String KEY_LEVEL = "LEVEL-";

  public static final String KEY_ORGUNIT_GROUP = "OU_GROUP-";

  public static final String KEY_DATASET = "DS-";

  public static final String KEY_PROGRAM = "PR-";
}
