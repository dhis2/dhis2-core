/*
 * Copyright (c) 2004-2024, University of Oslo
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

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.tracker.export.event.EventChangeLog.Change;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository("org.hisp.dhis.tracker.export.event.ChangeLogStore")
@RequiredArgsConstructor
class JdbcEventChangeLogStore {

  private final JdbcTemplate jdbcTemplate;

  private static final RowMapper<EventChangeLog> customEventChangeLogRowMapper =
      (rs, rowNum) -> {
        UserInfoSnapshot createdBy = new UserInfoSnapshot();
        createdBy.setUsername(rs.getString("userName"));
        createdBy.setFirstName(rs.getString("firstname"));
        createdBy.setSurname(rs.getString("surname"));
        createdBy.setUid(rs.getString("useruid"));

        return new EventChangeLog(
            createdBy,
            rs.getTimestamp("updatedAt"),
            new Change(
                new EventChangeLog.DataValueChange(
                    rs.getString("dataElementUid"),
                    rs.getString("previousValue"),
                    rs.getString("currentValue"))));
      };

  public List<EventChangeLog> getEventChangeLog(String eventUid) {
    final String sql =
        """
            select
              case
                when cl.audittype = 'CREATE' then cl.previouschangelogvalue
                when cl.audittype = 'UPDATE' and cl.currentchangelogvalue is null then cl.currentValue
                when cl.audittype = 'UPDATE' and cl.currentchangelogvalue is not null then cl.currentchangelogvalue
              end as currentValue,
              case
                when cl.audittype = 'DELETE' then cl.previouschangelogvalue
                when cl.audittype = 'UPDATE' and cl.currentchangelogvalue is null then cl.previouschangelogvalue
                when cl.audittype = 'UPDATE' and cl.currentchangelogvalue is not null then cl.previouschangelogvalue
              end as previousValue, cl.created as updatedAt, cl.modifiedby as userName, cl.dataElementUid as dataElementUid, cl.firstname, cl.surname, cl.username, cl.useruid
            from
              (select t.created, d.uid as dataElementUid, t.modifiedby, t.audittype, u.firstname, u.surname, u.username, u.uid as useruid,
                  LAG (t.value) OVER (PARTITION BY t.eventid, t.dataelementid ORDER BY t.created DESC) AS currentchangelogvalue,
                  t.value as previouschangelogvalue,
                  e.eventdatavalues -> d.uid  ->> 'value' as currentValue
              from trackedentitydatavalueaudit t
              join event e using (eventid)
              join dataelement d using (dataelementid)
              join userinfo u on u.username = t.modifiedby
              where t.audittype in ('CREATE', 'UPDATE', 'DELETE')
              and e.uid = ?
              order by t.created desc) cl
            """;

    return jdbcTemplate.query(sql, customEventChangeLogRowMapper, eventUid);
  }
}
