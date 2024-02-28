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
package org.hisp.dhis.tracker.export.trackedentity;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.tracker.export.Page;
import org.hisp.dhis.tracker.export.PageParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityChangeLog.Change;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository("org.hisp.dhis.tracker.export.trackedentity.JdbcTrackedEntityChangeLogStore")
@RequiredArgsConstructor
public class JdbcTrackedEntityChangeLogStore {

  private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

  private static final RowMapper<TrackedEntityChangeLog> customTrackedEntityChangeLogRowMapper =
      (rs, rowNum) -> {
        UserInfoSnapshot createdBy = new UserInfoSnapshot();
        createdBy.setUsername(rs.getString("username"));
        createdBy.setFirstName(rs.getString("firstname"));
        createdBy.setSurname(rs.getString("surname"));
        createdBy.setUid(rs.getString("useruid"));

        return new TrackedEntityChangeLog(
            createdBy,
            rs.getTimestamp("createdat"),
            rs.getString("type"),
            new Change(
                new TrackedEntityChangeLog.TrackedEntityAttributeChange(
                    rs.getString("trackedentityattributeuid"),
                    rs.getString("previousvalue"),
                    rs.getString("currentvalue"))));
      };

  public Page<TrackedEntityChangeLog> getTrackedEntityChangeLog(
      UID trackedEntity, Set<String> attributes, PageParams pageParams) {
    String sql =
        """
          select cl.type,
              case
          when cl.type = 'CREATE' then cl.currentchangelogvalue
          when cl.type = 'UPDATE' then cl.currentchangelogvalue
          end as currentvalue,
              case
          when cl.type = 'DELETE' then cl.currentchangelogvalue
          when cl.type = 'UPDATE' then cl.previouschangelogvalue
          end as previousvalue, cl.createdat , cl.trackedentityattributeuid, cl.firstname, cl.surname, cl.username, cl.useruid, cl.type
          from
              (
                  select audit.created as createdat, tea.uid as trackedentityattributeuid, audit.audittype as type, u.firstname, u.surname, u.username, u.uid as useruid,
                  lead (audit.value) over (partition by audit.trackedentityid, audit.trackedentityattributeid order by audit.created DESC) as previouschangelogvalue,
                  audit.value as currentchangelogvalue
                  from trackedEntityAttributeValueAudit audit
                  join trackedentity t using(trackedentityid)
                  join trackedentityattribute tea using(trackedentityattributeid)
                  join userinfo u on u.username = audit.modifiedby
                  where audit.audittype in ('CREATE', 'UPDATE', 'DELETE')
                  and t.uid = :trackedEntity
       """;

    List<TrackedEntityChangeLog> changeLogs;
    if (attributes.isEmpty()) {
      sql += """
              order by audit.created desc) cl
          """;
      SqlParameterSource parameters =
          new MapSqlParameterSource("trackedEntity", trackedEntity.getValue());
      changeLogs =
          namedParameterJdbcTemplate.query(sql, parameters, customTrackedEntityChangeLogRowMapper);
    } else {
      sql +=
          """
              and tea.uid in (:attributes)
              order by audit.created desc) cl
          """;
      SqlParameterSource parameters =
          new MapSqlParameterSource("attributes", attributes)
              .addValue("trackedEntity", trackedEntity.getValue());
      changeLogs =
          namedParameterJdbcTemplate.query(sql, parameters, customTrackedEntityChangeLogRowMapper);
    }

    Integer prevPage = pageParams.getPage() > 1 ? pageParams.getPage() - 1 : null;
    if (changeLogs.size() > pageParams.getPageSize()) {
      return Page.withPrevAndNext(
          changeLogs.subList(0, pageParams.getPageSize()),
          pageParams.getPage(),
          pageParams.getPageSize(),
          prevPage,
          pageParams.getPage() + 1);
    }

    return Page.withPrevAndNext(
        changeLogs, pageParams.getPage(), pageParams.getPageSize(), prevPage, null);
  }
}
