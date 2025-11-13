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
package org.hisp.dhis.analytics.table;

import static java.time.LocalDate.now;
import static org.hisp.dhis.analytics.AnalyticsTableType.TRACKED_ENTITY_INSTANCE_EVENTS;
import static org.hisp.dhis.analytics.util.SqlComparisonUtils.normalizeSql;
import static org.hisp.dhis.period.PeriodDataProvider.PeriodSource.DATABASE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.db.sql.PostgreSqlAnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class JdbcTrackedEntityEventsAnalyticsTableManagerTest {

  @Mock private JdbcTemplate jdbcTemplate;

  @Mock private AnalyticsTableSettings analyticsTableSettings;

  @Mock private PeriodDataProvider periodDataProvider;

  @Spy private PostgreSqlBuilder sqlBuilder;

  @Spy private PostgreSqlAnalyticsSqlBuilder analyticsSqlBuilder;

  @Mock private PartitionManager partitionManager;

  @Mock private SystemSettingsProvider systemSettingsProvider;

  @Mock private IdentifiableObjectManager identifiableObjectManager;

  @Mock private TrackedEntityTypeService trackedEntityTypeService;

  @Mock private TrackedEntityAttributeService trackedEntityAttributeService;

  @Mock private CategoryService categoryService;

  @Mock private DataApprovalLevelService dataApprovalLevelService;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private ResourceTableService resourceTableService;

  @Mock private AnalyticsTableHookService analyticsTableHookService;

  @InjectMocks private JdbcTrackedEntityEventsAnalyticsTableManager tableManager;

  private static final Date START_TIME = new DateTime(2019, 8, 1, 0, 0).toDate();

  @Test
  void testTableName() {
    assertEquals(TRACKED_ENTITY_INSTANCE_EVENTS.getTableName(), tableManager.getTableName());
  }

  @Test
  void testPopulateTable() {
    TrackedEntityType tet = mock(TrackedEntityType.class);
    when(tet.getUid()).thenReturn("tetUid");
    when(trackedEntityTypeService.getAllTrackedEntityType()).thenReturn(List.of(tet));

    when(periodDataProvider.getAvailableYears(DATABASE))
        .thenReturn(List.of(2018, 2019, now().getYear()));

    when(jdbcTemplate.queryForList(startsWith("select temp.supportedyear from"), any(Class.class)))
        .thenReturn(new ArrayList<>(List.of(2018, 2019)));

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder().lastYears(2).startTime(START_TIME).build();
    List<AnalyticsTable> analyticsTables = tableManager.getAnalyticsTables(params);
    assertFalse(analyticsTables.isEmpty());
    AnalyticsTablePartition partition = new AnalyticsTablePartition(analyticsTables.get(0));

    tableManager.populateTable(params, partition);

    String subQuery =
        """
        (select json_object_agg(l2.keys, l2.datavalue) as value
            from (
                select l1.uid,
                l1.keys,
                json_strip_nulls(json_build_object(
                'value', l1.eventdatavalues -> l1.keys ->> 'value',
                'created', l1.eventdatavalues -> l1.keys ->> 'created',
                'storedBy', l1.eventdatavalues -> l1.keys ->> 'storedBy',
                'lastUpdated', l1.eventdatavalues -> l1.keys ->> 'lastUpdated',
                'providedElsewhere', l1.eventdatavalues -> l1.keys -> 'providedElsewhere',
                'value_name', (select ou.name
                    from organisationunit ou
                    where ou.uid = l1.eventdatavalues -> l1.keys ->> 'value'),
                'value_code', (select ou.code
                    from organisationunit ou
                    where ou.uid = l1.eventdatavalues -> l1.keys ->> 'value'))) as datavalue
                from (select inner_evt.*, jsonb_object_keys(inner_evt.eventdatavalues) keys
                from trackerevent inner_evt) as l1) as l2
            where l2.uid = ev.uid
            group by l2.uid)::jsonb
            """;

    String expectedSql =
        """
        insert into analytics_te_event_tetuid_temp ("trackedentity","program","enrollment","programstage","event","occurreddate","lastupdated","created",
        "scheduleddate","status","ou","ouname","oucode","oulevel","eventdatavalues","eventgeometry",
        "evlongitude","evlatitude","ounamehierarchy") select distinct te.uid,p.uid,en.uid,ps.uid,ev.uid,ev.occurreddate,ev.lastupdated,
        ev.created,ev.scheduleddate,ev.status,ous.organisationunituid,ous.name,ous.code,ous.level,
        %s,
        ev.geometry,case when 'POINT' = GeometryType(ev.geometry) then ST_X(ev.geometry) end,case when 'POINT' = GeometryType(ev.geometry) then ST_Y(ev.geometry) end,concat_ws(' / ',) as ounamehierarchy
        from "trackerevent" ev inner join "enrollment" en on en.enrollmentid=ev.enrollmentid
        and en."deleted" = false inner join "trackedentity" te on te.trackedentityid=en.trackedentityid
        and te."deleted" = false and te.trackedentitytypeid = 0
        and te.lastupdated < '2019-08-01T00:00:00' left join "programstage" ps on ev.programstageid=ps.programstageid
        left join "program" p on ps.programid=p.programid
        left join analytics_rs_orgunitstructure ous on ev.organisationunitid=ous.organisationunitid
        where ev.status in ('COMPLETED','ACTIVE','SCHEDULE')
        and (CASE WHEN 'SCHEDULE' = ev.status THEN ev.scheduleddate ELSE ev.occurreddate END) >= 'null'
        and (CASE WHEN 'SCHEDULE' = ev.status THEN ev.scheduleddate ELSE ev.occurreddate END) < 'null'
        and ev."deleted" = false"""
            .formatted(subQuery);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(jdbcTemplate, times(1)).execute(sqlCaptor.capture()); // verify it was called twice
    assertEquals(normalizeSql(expectedSql), normalizeSql(sqlCaptor.getAllValues().get(0))); // che
  }
}
