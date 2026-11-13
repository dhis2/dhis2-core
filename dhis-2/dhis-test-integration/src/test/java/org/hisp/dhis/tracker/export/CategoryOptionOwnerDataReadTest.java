/*
 * Copyright (c) 2004-2026, University of Oslo
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

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.TrackerTestUtils.uids;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentOperationParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.singleevent.SingleEventOperationParams;
import org.hisp.dhis.tracker.export.singleevent.SingleEventService;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventOperationParams;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owning a category option does not grant data read: the tracker exports only return events and
 * enrollments whose attribute option combo has category options the user has data read access to
 * through sharing (DHIS2-22479).
 *
 * <p>The non-superuser {@value #USER} owns category option {@code yMj2MnmNI8L}, whose sharing
 * grants no one data read. The user has data read access to {@code xEunk8LPzkb} (public) and {@code
 * OUUdG3sdOqb} (user access), the options of {@value #READABLE_AOC}. {@value #OWNED_AOC} combines
 * {@code yMj2MnmNI8L} with the publicly readable {@code xwZ2u3WyQR0}.
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CategoryOptionOwnerDataReadTest extends PostgresIntegrationTestBase {
  private static final String USER = "o1HMTIzBGo7";

  private static final String READABLE_AOC = "AeOORUC0ISH";

  private static final String OWNED_AOC = "V2gbpszLQJm";

  private static final String ORG_UNIT = "DiszpKrYNg8";

  private static final String TRACKER_PROGRAM = "TsngICFQjvH";

  private static final String TRACKER_EVENT = "H0PbzJY8bJG";

  private static final String ENROLLMENT = "GYWSSZunTLk";

  private static final String SINGLE_EVENT = "lumVtWwwy0O";

  @Autowired private TestSetup testSetup;
  @Autowired private TrackerEventService trackerEventService;
  @Autowired private SingleEventService singleEventService;
  @Autowired private EnrollmentService enrollmentService;
  @Autowired private IdentifiableObjectManager manager;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();
    injectSecurityContextUser(userService.getUser("tTgjgobT1oS"));
    testSetup.importTrackerData();
    // only the attribute option combo decides access in these tests
    for (IdentifiableObject object :
        List.of(
            manager.get(Program.class, TRACKER_PROGRAM),
            manager.get(TrackedEntityType.class, "Ip8NY4PW7Xm"))) {
      object.getSharing().setPublicAccess(AccessStringHelper.CATEGORY_OPTION_DEFAULT);
      manager.update(object);
    }
    manager.flush();
  }

  @BeforeEach
  void setUpUser() {
    injectSecurityContextUser(userService.getUser(USER));
  }

  @Test
  void shouldReturnTrackerEventWhenUserCanReadDataOfAllCategoryOptions() throws Exception {
    setAoc("trackerevent", "eventid", TRACKER_EVENT, READABLE_AOC);

    assertContainsOnly(List.of(TRACKER_EVENT), findTrackerEvents());
  }

  @Test
  void shouldNotReturnTrackerEventWhenUserOnlyOwnsACategoryOption() throws Exception {
    setAoc("trackerevent", "eventid", TRACKER_EVENT, OWNED_AOC);

    assertIsEmpty(findTrackerEvents());
  }

  @Test
  void shouldReturnEnrollmentWhenUserCanReadDataOfAllCategoryOptions() throws Exception {
    setAoc("enrollment", "enrollmentid", ENROLLMENT, READABLE_AOC);

    assertContainsOnly(List.of(ENROLLMENT), findEnrollments());
  }

  @Test
  void shouldNotReturnEnrollmentWhenUserOnlyOwnsACategoryOption() throws Exception {
    setAoc("enrollment", "enrollmentid", ENROLLMENT, OWNED_AOC);

    assertIsEmpty(findEnrollments());
  }

  @Test
  void shouldReturnSingleEventWhenUserCanReadDataOfAllCategoryOptions() throws Exception {
    setAoc("singleevent", "eventid", SINGLE_EVENT, READABLE_AOC);

    assertContainsOnly(List.of(SINGLE_EVENT), findSingleEvents());
  }

  @Test
  void shouldNotReturnSingleEventWhenUserOnlyOwnsACategoryOption() throws Exception {
    setAoc("singleevent", "eventid", SINGLE_EVENT, OWNED_AOC);

    assertIsEmpty(findSingleEvents());
  }

  /** Moves the row to the given attribute option combo, rolled back after the test. */
  private void setAoc(String table, String idColumn, String uid, String aoc) {
    entityManager.flush();
    int updated =
        entityManager
            .createNativeQuery(
                ("update %s set attributeoptioncomboid ="
                        + " (select categoryoptioncomboid from categoryoptioncombo where uid = :aoc)"
                        + " where uid = :uid")
                    .formatted(table))
            .setParameter("aoc", aoc)
            .setParameter("uid", uid)
            .executeUpdate();
    if (updated != 1)
      throw new IllegalStateException("no " + table + " " + uid + " (" + idColumn + ")");
    entityManager.clear();
  }

  private List<String> findTrackerEvents() throws Exception {
    return uids(
        trackerEventService.findEvents(
            TrackerEventOperationParams.builderForProgram(UID.of(TRACKER_PROGRAM))
                .orgUnit(UID.of(ORG_UNIT))
                .orgUnitMode(SELECTED)
                .events(Set.of(UID.of(TRACKER_EVENT)))
                .build()));
  }

  private List<String> findEnrollments() throws Exception {
    return uids(
        enrollmentService.findEnrollments(
            EnrollmentOperationParams.builder()
                .program(UID.of(TRACKER_PROGRAM))
                .orgUnits(Set.of(UID.of(ORG_UNIT)))
                .orgUnitMode(SELECTED)
                .enrollments(Set.of(UID.of(ENROLLMENT)))
                .build()));
  }

  private List<String> findSingleEvents() throws Exception {
    return uids(
        singleEventService.findEvents(
            SingleEventOperationParams.builderForProgram(UID.of("iS7eutanDry"))
                .orgUnit(UID.of(ORG_UNIT))
                .orgUnitMode(SELECTED)
                .events(Set.of(UID.of(SINGLE_EVENT)))
                .build()));
  }
}
