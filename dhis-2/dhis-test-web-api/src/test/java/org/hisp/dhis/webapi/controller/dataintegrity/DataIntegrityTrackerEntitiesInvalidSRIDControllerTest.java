/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.controller.dataintegrity;

import java.util.Set;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

/**
 * @author Zubair Asghar
 */
class DataIntegrityTrackerEntitiesInvalidSRIDControllerTest
    extends AbstractDataIntegrityIntegrationTest {
  private TrackedEntityInstance trackedEntityWithInvalidSRID;

  @Test
  void testTrackedEntitiesWithInvalidSRID() {
    setUp();
    assertHasDataIntegrityIssues(
        "tracker",
        "tracker_geometry_invalid_srid.yaml",
        50,
        trackedEntityWithInvalidSRID.getUid(),
        "trackedentity",
        "Expected SRID 4326 but found 0",
        true);
  }

  @Test
  void testTrackedEntitiesWithNoData() {
    assertHasNoDataIntegrityIssues("tracker", "tracker_geometry_invalid_srid.yaml", false);
  }

  @Test
  void testTrackedEntitiesWithValidSRID() {
    setUpValidData();
    assertHasNoDataIntegrityIssues("tracker", "tracker_geometry_invalid_srid.yaml", true);
  }

  private void setUp() {
    OrganisationUnit organisationUnit = createOrganisationUnit('O');
    manager.save(organisationUnit);

    TrackedEntityType trackedEntityType = createTrackedEntityType('P');
    manager.save(trackedEntityType);

    Program program = createProgram('P', Set.of(), organisationUnit);
    manager.save(program);

    TrackedEntityInstance trackedEntityWithValidSRID =
        createTrackedEntityInstance(organisationUnit);
    trackedEntityWithValidSRID.setTrackedEntityType(trackedEntityType);
    Point point = new GeometryFactory().createPoint(new Coordinate(60.0, 30.0));
    point.setSRID(4326);
    trackedEntityWithValidSRID.setGeometry(point);

    trackedEntityWithInvalidSRID = createTrackedEntityInstance(organisationUnit);
    trackedEntityWithInvalidSRID.setTrackedEntityType(trackedEntityType);
    point = new GeometryFactory().createPoint(new Coordinate(60.0, 30.0));
    point.setSRID(0); // invalid SRID
    trackedEntityWithInvalidSRID.setGeometry(point);

    manager.save(trackedEntityWithValidSRID);
    manager.save(trackedEntityWithInvalidSRID);
  }

  private void setUpValidData() {
    OrganisationUnit organisationUnit = createOrganisationUnit('P');
    manager.save(organisationUnit);

    TrackedEntityType trackedEntityType = createTrackedEntityType('Q');
    manager.save(trackedEntityType);

    Program program = createProgram('R', Set.of(), organisationUnit);
    manager.save(program);

    TrackedEntityInstance trackedEntityWithValidSRID =
        createTrackedEntityInstance(organisationUnit);
    trackedEntityWithValidSRID.setTrackedEntityType(trackedEntityType);
    Point point = new GeometryFactory().createPoint(new Coordinate(60.0, 30.0));
    point.setSRID(4326);
    trackedEntityWithValidSRID.setGeometry(point);

    manager.save(trackedEntityWithValidSRID);
  }
}
