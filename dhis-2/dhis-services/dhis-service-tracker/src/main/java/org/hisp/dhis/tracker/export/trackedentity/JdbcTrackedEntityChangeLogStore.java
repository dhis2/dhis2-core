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

import static java.util.Map.entry;

import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.tracker.export.Page;
import org.hisp.dhis.tracker.export.PageParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityChangeLog.Change;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository("org.hisp.dhis.tracker.export.trackedentity.JdbcTrackedEntityChangeLogStore")
@RequiredArgsConstructor
public class JdbcTrackedEntityChangeLogStore {
  private static final String COLUMN_CHANGELOG_CREATED = "created";
  private static final String DEFAULT_ORDER =
      COLUMN_CHANGELOG_CREATED + " " + SortDirection.DESC.getValue();

  /**
   * Tracked entities change logs can be ordered by given fields which correspond to fields on
   * {@link org.hisp.dhis.tracker.export.trackedentity.TrackedEntityChangeLog}. Maps fields to DB
   * columns. The order implementation for change logs is different from other tracker exporters
   * {@link org.hisp.dhis.tracker.export.trackedentity.TrackedEntityChangeLog} is the view which is
   * already returned from the service/store. Tracker exporter services return a representation we
   * have to map to a view model. This mapping is not necessary for change logs.
   */
  private static final Map<String, String> ORDERABLE_FIELDS =
      Map.ofEntries(entry("createdAt", COLUMN_CHANGELOG_CREATED));

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
            rs.getTimestamp(COLUMN_CHANGELOG_CREATED),
            rs.getString("type"),
            new Change(
                new TrackedEntityChangeLog.TrackedEntityAttributeChange(
                    rs.getString("trackedentityattributeuid"),
                    rs.getString("previousvalue"),
                    rs.getString("currentvalue"))));
      };

  public Page<TrackedEntityChangeLog> getTrackedEntityChangeLog(
      UID trackedEntity,
      Set<String> attributes,
      UID program,
      List<Order> order,
      PageParams pageParams) {

    MapSqlParameterSource parameters =
        new MapSqlParameterSource("trackedEntity", trackedEntity.getValue())
            .addValue("limit", pageParams.getPageSize() + 1)
            .addValue("offset", (pageParams.getPage() - 1) * pageParams.getPageSize());

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
          end as previousvalue, cl.created, cl.trackedentityattributeuid, cl.firstname, cl.surname, cl.username, cl.useruid, cl.type
          from
              (
                  select audit.created, tea.uid as trackedentityattributeuid, audit.audittype as type, u.firstname, u.surname, u.username, u.uid as useruid,
                  lead (audit.value) over (partition by audit.trackedentityid, audit.trackedentityattributeid order by audit.created DESC) as previouschangelogvalue,
                  audit.value as currentchangelogvalue
                  from trackedEntityAttributeValueAudit audit
                  join trackedentity t using(trackedentityid)
                  join trackedentityattribute tea using(trackedentityattributeid)
                  join userinfo u on u.username = audit.modifiedby
       """;

    if (program != null) {
      sql +=
          """
              join program_attributes pa using (trackedentityattributeid)
              join program p using (programid)
              where audit.audittype in ('CREATE', 'UPDATE', 'DELETE')
              and t.uid = :trackedEntity
              and p.uid = :program
          """;
      parameters.addValue("program", program.getValue());
    } else {
      sql +=
          """
              where audit.audittype in ('CREATE', 'UPDATE', 'DELETE')
              and t.uid = :trackedEntity
          """;
    }

    List<TrackedEntityChangeLog> changeLogs;
    if (!attributes.isEmpty()) {
      sql += """
              and tea.uid in (:attributes)
          """;
    }

    sql +=
        """
              order by %s
              limit :limit offset :offset) cl
          """
            .formatted(sortExpressions(order));
    parameters.addValue("attributes", attributes);
    changeLogs =
        namedParameterJdbcTemplate.query(sql, parameters, customTrackedEntityChangeLogRowMapper);

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

  private static String sortExpressions(List<Order> order) {
    if (order.isEmpty()) {
      return DEFAULT_ORDER;
    }

    return ORDERABLE_FIELDS.get(order.get(0).getField())
        + " "
        + order.get(0).getDirection().getValue();
  }

  public Set<String> getOrderableFields() {
    return ORDERABLE_FIELDS.keySet();
  }
}
