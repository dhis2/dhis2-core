/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.export;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.test.config.QueryCountDataSourceProxy;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentOperationParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.singleevent.SingleEventOperationParams;
import org.hisp.dhis.tracker.export.singleevent.SingleEventService;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventOperationParams;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * The tracker exports must not join a subquery that groups every category option combo to check the
 * attribute option combo access (DHIS2-22400). PostgreSQL has no statistics for such a grouped
 * subquery; once it is estimated at more than 200 rows, the join to it inflates the row estimate of
 * the whole query, which can flip the plan to a backward walk of the event primary key. The
 * readable attribute option combos are resolved up front instead.
 *
 * <p>The export query must not join {@code categoryoptioncombo} either: together with the filter on
 * the readable attribute option combos, PostgreSQL then estimates the matches from the number of
 * all category option combos, an underestimate that grows with it.
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(classes = {QueryCountDataSourceProxy.class})
class TrackerExportAocAccessQueryTest extends PostgresIntegrationTestBase {
  /** The grouped subquery aggregated the category options of every option combo. */
  private static final String GROUPED_COC_SUBQUERY = "group by coc.categoryoptioncomboid";

  /** A join of the option combos, which the planner merges with the readable combo filter. */
  private static final String COC_JOIN = "join categoryoptioncombo ";

  /**
   * The option combo UID looked up per event row, only needed to order by it. Events get their
   * option combos loaded once per distinct combo after the export query.
   */
  private static final String COC_UID_PER_EVENT =
      "where coc.categoryoptioncomboid = ev.attributeoptioncomboid";

  /** A user without the ALL authority, so the attribute option combo access check applies. */
  private static final String NON_SUPERUSER = "FIgVWzUCkpw";

  @Autowired private TestSetup testSetup;
  @Autowired private TrackerEventService trackerEventService;
  @Autowired private SingleEventService singleEventService;
  @Autowired private EnrollmentService enrollmentService;
  @Autowired private IdentifiableObjectManager manager;

  private OrganisationUnit orgUnit;
  private Program trackerProgram;
  private Program eventProgram;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();
    injectSecurityContextUser(userService.getUser("tTgjgobT1oS"));
    testSetup.importTrackerData();
    orgUnit = manager.get(OrganisationUnit.class, "h4w96yEMlzO");
    trackerProgram = manager.get(ProgramStage.class, "NpsdDv6kKSO").getProgram();
    eventProgram = manager.get(Program.class, "iS7eutanDry");
    manager.flush();
  }

  @Test
  void trackerEventsAsSuperuser() throws Exception {
    injectAdminIntoSecurityContext();
    assertAocAccessResolvedUpFront("from trackerevent ev", this::findTrackerEvents);
  }

  @Test
  void trackerEventsAsNonSuperuser() throws Exception {
    injectSecurityContextUser(userService.getUser(NON_SUPERUSER));
    assertAocAccessResolvedUpFront("from trackerevent ev", this::findTrackerEvents);
  }

  @Test
  void singleEventsAsSuperuser() throws Exception {
    injectAdminIntoSecurityContext();
    assertAocAccessResolvedUpFront("from singleevent ev", this::findSingleEvents);
  }

  @Test
  void singleEventsAsNonSuperuser() throws Exception {
    injectSecurityContextUser(userService.getUser(NON_SUPERUSER));
    assertAocAccessResolvedUpFront("from singleevent ev", this::findSingleEvents);
  }

  @Test
  void enrollmentsAsNonSuperuser() throws Exception {
    injectSecurityContextUser(userService.getUser(NON_SUPERUSER));
    assertAocAccessResolvedUpFront("from enrollment e", this::findEnrollments);
  }

  private void findTrackerEvents() throws Exception {
    trackerEventService.findEvents(
        isSuperuser()
            ? TrackerEventOperationParams.builderForProgram(UID.of(trackerProgram))
                .orgUnitMode(ALL)
                .build()
            : TrackerEventOperationParams.builderForProgram(UID.of(trackerProgram))
                .orgUnit(orgUnit)
                .orgUnitMode(DESCENDANTS)
                .build());
  }

  private void findSingleEvents() throws Exception {
    singleEventService.findEvents(
        isSuperuser()
            ? SingleEventOperationParams.builderForProgram(UID.of(eventProgram))
                .orgUnitMode(ALL)
                .build()
            : SingleEventOperationParams.builderForProgram(UID.of(eventProgram))
                .orgUnit(orgUnit)
                .orgUnitMode(DESCENDANTS)
                .build());
  }

  private void findEnrollments() throws Exception {
    enrollmentService.findEnrollments(
        EnrollmentOperationParams.builder()
            .program(trackerProgram)
            .orgUnitMode(ACCESSIBLE)
            .build());
  }

  private static boolean isSuperuser() {
    return CurrentUserUtil.getCurrentUserDetails().isSuper();
  }

  private void assertAocAccessResolvedUpFront(String exportQuery, Export export) throws Exception {
    QueryCountDataSourceProxy.clearCapturedSql();

    export.run();

    assertTrue(
        QueryCountDataSourceProxy.countCapturedSqlMatching(exportQuery) > 0,
        "expected the export query to run: " + exportQuery);
    assertEquals(0, QueryCountDataSourceProxy.countCapturedSqlMatching(GROUPED_COC_SUBQUERY));
    assertEquals(0, QueryCountDataSourceProxy.countCapturedSqlMatching(COC_JOIN));
    assertEquals(0, QueryCountDataSourceProxy.countCapturedSqlMatching(COC_UID_PER_EVENT));
  }

  @FunctionalInterface
  private interface Export {
    void run() throws Exception;
  }
}
