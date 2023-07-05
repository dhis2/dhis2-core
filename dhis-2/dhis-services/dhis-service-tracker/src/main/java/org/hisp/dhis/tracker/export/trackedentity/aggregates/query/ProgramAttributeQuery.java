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
package org.hisp.dhis.tracker.export.trackedentity.aggregates.query;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

public class ProgramAttributeQuery {
  public enum COLUMNS {
    PI_UID,
    PI_ID,
    CREATED,
    UPDATED,
    VALUE,
    STOREDBY,
    ATTR_UID,
    ATTR_NAME,
    ATTR_VALUE_TYPE,
    ATTR_CODE,
    ATTR_SKIP_SYNC
  }

  public static final Map<COLUMNS, TableColumn> columnMap =
      ImmutableMap.<COLUMNS, TableColumn>builder()
          .put(COLUMNS.PI_UID, new TableColumn("pi", "uid", "pi_uid"))
          .put(COLUMNS.PI_ID, new TableColumn("pi", "programinstanceid", "id"))
          .put(COLUMNS.CREATED, new TableColumn("teav", "created"))
          .put(COLUMNS.UPDATED, new TableColumn("teav", "lastupdated"))
          .put(COLUMNS.STOREDBY, new TableColumn("teav", "storedby"))
          .put(COLUMNS.VALUE, new TableColumn("teav", "value"))
          .put(COLUMNS.ATTR_UID, new TableColumn("t", "uid", "att_uid"))
          .put(COLUMNS.ATTR_VALUE_TYPE, new TableColumn("t", "valuetype", "att_val_type"))
          .put(COLUMNS.ATTR_CODE, new TableColumn("t", "code", "att_code"))
          .put(COLUMNS.ATTR_NAME, new TableColumn("t", "name", "att_name"))
          .put(COLUMNS.ATTR_SKIP_SYNC, new TableColumn("t", "skipsynchronization", "att_skip_sync"))
          .build();

  public static String getQuery() {
    return getSelect()
        + "from trackedentityattributevalue teav "
        + "join program_attributes pa on teav.trackedentityattributeid  = pa.trackedentityattributeid "
        + "join trackedentityattribute t on t.trackedentityattributeid = pa.trackedentityattributeid "
        + "join trackedentityinstance tei on tei.trackedentityinstanceid = teav.trackedentityinstanceid "
        + "join programinstance pi on pi.programid = pa.programid and pi.trackedentityinstanceid = tei.trackedentityinstanceid "
        + "where pi.programinstanceid IN (:ids)";
  }

  private static String getSelect() {
    return QueryUtils.getSelect(columnMap.values());
  }

  public static String getColumnName(COLUMNS columns) {
    return columnMap.get(columns).getResultsetValue();
  }
}
