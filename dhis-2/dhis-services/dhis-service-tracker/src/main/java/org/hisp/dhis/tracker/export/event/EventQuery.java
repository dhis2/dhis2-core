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
package org.hisp.dhis.tracker.export.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Luciano Fiandesio
 */
public class EventQuery {
  @RequiredArgsConstructor
  public enum COLUMNS {
    ID(new TableColumn("ev", "eventid")),
    UID(new TableColumn("ev", "uid")),
    STATUS(new TableColumn("ev", "status")),
    EXECUTION_DATE(new TableColumn("ev", "executiondate")),
    DUE_DATE(new TableColumn("ev", "duedate")),
    STOREDBY(new TableColumn("ev", "storedby")),
    COMPLETEDBY(new TableColumn("ev", "completedby")),
    COMPLETEDDATE(new TableColumn("ev", "completeddate")),
    CREATED_BY(new TableColumn("ev", "createdbyuserinfo")),
    CREATED(new TableColumn("ev", "created")),
    CREATEDCLIENT(new TableColumn("ev", "createdatclient")),
    UPDATED(new TableColumn("ev", "lastupdated")),
    UPDATEDCLIENT(new TableColumn("ev", "lastupdatedatclient")),
    LAST_UPDATED_BY(new TableColumn("ev", "lastupdatedbyuserinfo")),
    DELETED(new TableColumn("ev", "deleted")),
    GEOMETRY(new Function("ST_AsBinary", "ev", "geometry", "geometry")),
    TEI_UID(new TableColumn("tei", "uid", "tei_uid")),
    ENROLLMENT_UID(new TableColumn("en", "uid", "enruid")),
    ENROLLMENT_FOLLOWUP(new TableColumn("en", "followup", "enrfollowup")),
    ENROLLMENT_STATUS(new TableColumn("en", "status", "enrstatus")),
    PROGRAM_UID(new TableColumn("p", "uid", "prguid")),
    PROGRAM_STAGE_UID(new TableColumn("ps", "uid", "prgstguid")),
    ORGUNIT_UID(new TableColumn("o", "uid", "ou_uid")),
    ORGUNIT_NAME(new TableColumn("o", "name", "ou_name")),
    COC_UID(new TableColumn("coc", "uid", "cocuid")),
    CAT_OPTIONS(
        new Subselect(
            "( "
                + "SELECT string_agg(opt.uid::text, ',') "
                + "FROM dataelementcategoryoption opt "
                + "join categoryoptioncombos_categoryoptions ccc "
                + "on opt.categoryoptionid = ccc.categoryoptionid "
                + "WHERE coc.categoryoptioncomboid = ccc.categoryoptioncomboid )",
            "catoptions")),
    ASSIGNED_USER(new TableColumn("ui", "uid", "userid")),
    ASSIGNED_USER_FIRST_NAME(new TableColumn("ui", "firstname")),
    ASSIGNED_USER_SURNAME(new TableColumn("ui", "surname")),
    ASSIGNED_USER_USERNAME(new TableColumn("ui", "username"));

    @Getter private final QueryElement queryElement;

    public String getColumnName() {
      if (queryElement instanceof TableColumn) {
        return ((TableColumn) queryElement).getColumn();
      }
      if (queryElement instanceof Function) {
        return ((Function) queryElement).getColumn();
      }
      throw new IllegalArgumentException(
          "getColumnName can only be invoked on TableColumn or Function");
    }
  }
}
