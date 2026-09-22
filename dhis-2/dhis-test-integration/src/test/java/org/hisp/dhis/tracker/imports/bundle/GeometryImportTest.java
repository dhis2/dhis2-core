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
package org.hisp.dhis.tracker.imports.bundle;

import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.SoftDeletableEntity;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.model.Enrollment;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackerEvent;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies that the {@code geometry} column is correctly persisted on both insert and update by the
 * JDBC writers (DHIS2-21378). The writers build the geometry with {@code ST_GeomFromText(...,
 * 4326)} so we assert both the coordinates and that the SRID round-trips.
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GeometryImportTest extends PostgresIntegrationTestBase {

  private static final int SRID = 4326;

  /** Standalone tracked entity (no dependents) so its geometry import exercises the insert path. */
  private static final String STANDALONE_TE_FILE = "tracker/another_single_te.json";

  private static final String STANDALONE_TE_UID = "Fr9h5ps74jF";
  private static final String ENROLLMENT_UID = "TvctPPhpD8u";
  private static final String EVENT_UID = "D9PbzJY8bJO";

  @Autowired private TestSetup testSetup;
  @Autowired private TrackerImportService trackerImportService;
  @Autowired private IdentifiableObjectManager manager;

  private User importUser;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    // Enrollment geometry is validated against the program feature type, which is NONE in the base
    // metadata. Allow points so the enrollment geometry can be imported.
    Program program = manager.get(Program.class, "BFcipDERJnf");
    program.setFeatureType(FeatureType.POINT);
    manager.update(program);

    // Tracked entity parent required by the enrollment and event imports below.
    testSetup.importTrackerData("tracker/one_te.json");
  }

  @BeforeEach
  void setUpUser() {
    injectSecurityContextUser(importUser);
  }

  @Test
  void shouldPersistGeometryWhenTrackedEntityIsCreatedAndUpdated() throws IOException {
    Point created = point(11.1, 22.2);
    importTrackedEntity(created, TrackerImportStrategy.CREATE);
    assertGeometry(
        created, getEntity(TrackedEntity.class, STANDALONE_TE_UID).getGeometry(), "tracked entity");

    Point updated = point(33.3, 44.4);
    importTrackedEntity(updated, TrackerImportStrategy.UPDATE);
    assertGeometry(
        updated, getEntity(TrackedEntity.class, STANDALONE_TE_UID).getGeometry(), "tracked entity");
  }

  @Test
  void shouldPersistGeometryWhenEnrollmentIsCreatedAndUpdated() throws IOException {
    Point created = point(11.1, 22.2);
    importEnrollment(created, TrackerImportStrategy.CREATE);
    assertGeometry(
        created, getEntity(Enrollment.class, ENROLLMENT_UID).getGeometry(), "enrollment");

    Point updated = point(33.3, 44.4);
    importEnrollment(updated, TrackerImportStrategy.UPDATE);
    assertGeometry(
        updated, getEntity(Enrollment.class, ENROLLMENT_UID).getGeometry(), "enrollment");
  }

  @Test
  void shouldPersistGeometryWhenEventIsCreatedAndUpdated() throws IOException {
    // The event references the enrollment, which is not part of the shared setup.
    testSetup.importTrackerData("tracker/one_enrollment.json");
    clearSession();

    Point created = point(11.1, 22.2);
    importEvent(created, TrackerImportStrategy.CREATE);
    assertGeometry(created, getEntity(TrackerEvent.class, EVENT_UID).getGeometry(), "event");

    Point updated = point(33.3, 44.4);
    importEvent(updated, TrackerImportStrategy.UPDATE);
    assertGeometry(updated, getEntity(TrackerEvent.class, EVENT_UID).getGeometry(), "event");
  }

  private void importTrackedEntity(Geometry geometry, TrackerImportStrategy strategy)
      throws IOException {
    TrackerObjects objects = testSetup.fromJson(STANDALONE_TE_FILE);
    objects.getTrackedEntities().get(0).setGeometry(geometry);
    importAndClear(objects, strategy);
  }

  private void importEnrollment(Geometry geometry, TrackerImportStrategy strategy)
      throws IOException {
    TrackerObjects objects = testSetup.fromJson("tracker/one_enrollment.json");
    objects.getEnrollments().get(0).setGeometry(geometry);
    importAndClear(objects, strategy);
  }

  private void importEvent(Geometry geometry, TrackerImportStrategy strategy) throws IOException {
    TrackerObjects objects = testSetup.fromJson("tracker/one_tracker_event.json");
    objects.getEvents().get(0).setGeometry(geometry);
    importAndClear(objects, strategy);
  }

  private void importAndClear(TrackerObjects objects, TrackerImportStrategy strategy) {
    TrackerImportParams params = TrackerImportParams.builder().importStrategy(strategy).build();
    assertNoErrors(trackerImportService.importTracker(params, objects));
    clearSession();
  }

  private void assertGeometry(Point expected, Geometry actual, String entity) {
    assertAll(
        "geometry not persisted correctly for " + entity,
        () -> assertNotNull(actual, "geometry was not persisted"),
        () -> assertEquals("Point", actual.getGeometryType()),
        () -> assertEquals(expected.getX(), actual.getCoordinate().x, 0.0001),
        () -> assertEquals(expected.getY(), actual.getCoordinate().y, 0.0001),
        () -> assertEquals(SRID, actual.getSRID(), "geometry SRID was not preserved"));
  }

  private Point point(double x, double y) {
    Point point = new GeometryFactory().createPoint(new Coordinate(x, y));
    point.setSRID(SRID);
    return point;
  }

  /**
   * Read with the entity manager so deleted entities and store-level filtering do not interfere; we
   * only want to assert what the JDBC writer persisted.
   */
  @SuppressWarnings("unchecked")
  private <T extends SoftDeletableEntity> T getEntity(Class<T> type, String uid) {
    return (T)
        entityManager
            .createQuery("SELECT e FROM " + type.getSimpleName() + " e WHERE e.uid = :uid")
            .setParameter("uid", uid)
            .getSingleResult();
  }
}
