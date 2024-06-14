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
package org.hisp.dhis.trackedentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
class TrackedEntityStoreTest extends TransactionalIntegrationTest {

  @Autowired private TrackedEntityStore trackedEntityStore;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private TrackedEntityAttributeValueService attributeValueService;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private TrackedEntityAttributeService trackedEntityAttributeService;

  @Autowired private ProgramService programService;

  private TrackedEntity trackedEntityA;

  private TrackedEntity trackedEntityB;

  private TrackedEntity trackedEntityC;

  private TrackedEntity trackedEntityD;

  private TrackedEntity trackedEntityE;

  private TrackedEntity trackedEntityF;

  private TrackedEntityAttribute atA;

  private TrackedEntityAttribute atB;

  private TrackedEntityAttribute atC;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  private OrganisationUnit ouC;

  private Program prA;

  private Program prB;

  @Override
  public void setUpTest() {
    atA = createTrackedEntityAttribute('A');
    atB = createTrackedEntityAttribute('B');
    atC = createTrackedEntityAttribute('C', ValueType.ORGANISATION_UNIT);
    atB.setUnique(true);
    trackedEntityAttributeService.addTrackedEntityAttribute(atA);
    trackedEntityAttributeService.addTrackedEntityAttribute(atB);
    trackedEntityAttributeService.addTrackedEntityAttribute(atC);
    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B', ouA);
    ouC = createOrganisationUnit('C', ouB);
    organisationUnitService.addOrganisationUnit(ouA);
    organisationUnitService.addOrganisationUnit(ouB);
    organisationUnitService.addOrganisationUnit(ouC);
    prA = createProgram('A', null, null);
    prB = createProgram('B', null, null);
    programService.addProgram(prA);
    programService.addProgram(prB);
    trackedEntityA = createTrackedEntity(ouA);
    trackedEntityB = createTrackedEntity(ouB);
    trackedEntityC = createTrackedEntity(ouB);
    trackedEntityD = createTrackedEntity(ouC);
    trackedEntityE = createTrackedEntity(ouC);
    trackedEntityF = createTrackedEntity(ouC);
  }

  @Test
  void testTrackedEntityExists() {
    trackedEntityStore.save(trackedEntityA);
    trackedEntityStore.save(trackedEntityB);
    dbmsManager.flushSession();
    assertTrue(trackedEntityStore.exists(trackedEntityA.getUid()));
    assertTrue(trackedEntityStore.exists(trackedEntityB.getUid()));
    assertFalse(trackedEntityStore.exists("aaaabbbbccc"));
  }

  @Test
  void testAddGet() {
    trackedEntityStore.save(trackedEntityA);
    long idA = trackedEntityA.getId();
    trackedEntityStore.save(trackedEntityB);
    long idB = trackedEntityB.getId();
    assertNotNull(trackedEntityStore.get(idA));
    assertNotNull(trackedEntityStore.get(idB));
  }

  @Test
  void testAddGetbyOu() {
    trackedEntityStore.save(trackedEntityA);
    long idA = trackedEntityA.getId();
    trackedEntityStore.save(trackedEntityB);
    long idB = trackedEntityB.getId();
    assertEquals(trackedEntityA.getName(), trackedEntityStore.get(idA).getName());
    assertEquals(trackedEntityB.getName(), trackedEntityStore.get(idB).getName());
  }

  @Test
  void testDelete() {
    trackedEntityStore.save(trackedEntityA);
    long idA = trackedEntityA.getId();
    trackedEntityStore.save(trackedEntityB);
    long idB = trackedEntityB.getId();
    assertNotNull(trackedEntityStore.get(idA));
    assertNotNull(trackedEntityStore.get(idB));
    trackedEntityStore.delete(trackedEntityA);
    assertNull(trackedEntityStore.get(idA));
    assertNotNull(trackedEntityStore.get(idB));
    trackedEntityStore.delete(trackedEntityB);
    assertNull(trackedEntityStore.get(idA));
    assertNull(trackedEntityStore.get(idB));
  }

  @Test
  void testGetAll() {
    trackedEntityStore.save(trackedEntityA);
    trackedEntityStore.save(trackedEntityB);
    assertTrue(equals(trackedEntityStore.getAll(), trackedEntityA, trackedEntityB));
  }

  @Test
  void testQuery() {
    trackedEntityStore.save(trackedEntityA);
    trackedEntityStore.save(trackedEntityB);
    trackedEntityStore.save(trackedEntityC);
    trackedEntityStore.save(trackedEntityD);
    trackedEntityStore.save(trackedEntityE);
    trackedEntityStore.save(trackedEntityF);
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(atA, trackedEntityD, "Male"));
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(atA, trackedEntityE, "Male"));
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(atA, trackedEntityF, "Female"));
    enrollmentService.enrollTrackedEntity(trackedEntityB, prA, new Date(), new Date(), ouB);
    enrollmentService.enrollTrackedEntity(trackedEntityE, prA, new Date(), new Date(), ouB);
    // Get all
    TrackedEntityQueryParams params = new TrackedEntityQueryParams();
    List<TrackedEntity> trackedEntitites = trackedEntityStore.getTrackedEntities(params);
    assertEquals(6, trackedEntitites.size());
    // Filter by attribute with EQ
    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EQ, "Male", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = trackedEntityStore.getTrackedEntities(params);
    assertEquals(2, trackedEntitites.size());
    assertTrue(trackedEntitites.contains(trackedEntityD));
    assertTrue(trackedEntitites.contains(trackedEntityE));
    // Filter by attribute with EQ
    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EQ, "Female", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = trackedEntityStore.getTrackedEntities(params);
    assertEquals(1, trackedEntitites.size());
    assertTrue(trackedEntitites.contains(trackedEntityF));

    // Filter by attribute with STARTS
    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.SW, "ma", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = trackedEntityStore.getTrackedEntities(params);
    assertEquals(2, trackedEntitites.size());
    assertTrue(trackedEntitites.contains(trackedEntityD));
    assertTrue(trackedEntitites.contains(trackedEntityE));

    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.SW, "al", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = trackedEntityStore.getTrackedEntities(params);
    assertEquals(0, trackedEntitites.size());

    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.SW, "ale", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = trackedEntityStore.getTrackedEntities(params);
    assertEquals(0, trackedEntitites.size());

    // Filter by attribute with ENDS
    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EW, "emale", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = trackedEntityStore.getTrackedEntities(params);
    assertEquals(1, trackedEntitites.size());
    assertTrue(trackedEntitites.contains(trackedEntityF));

    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EW, "male", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = trackedEntityStore.getTrackedEntities(params);
    assertEquals(3, trackedEntitites.size());
    assertTrue(trackedEntitites.contains(trackedEntityD));
    assertTrue(trackedEntitites.contains(trackedEntityE));
    assertTrue(trackedEntitites.contains(trackedEntityF));

    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EW, "fem", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = trackedEntityStore.getTrackedEntities(params);
    assertEquals(0, trackedEntitites.size());

    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EW, "em", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = trackedEntityStore.getTrackedEntities(params);
    assertEquals(0, trackedEntitites.size());

    // Filter by selected org units
    params =
        new TrackedEntityQueryParams()
            .addOrganisationUnit(ouB)
            .setOrgUnitMode(OrganisationUnitSelectionMode.SELECTED);
    trackedEntitites = trackedEntityStore.getTrackedEntities(params);
    assertEquals(2, trackedEntitites.size());
    assertTrue(trackedEntitites.contains(trackedEntityB));
    assertTrue(trackedEntitites.contains(trackedEntityC));
    // Filter by descendants org units
    params =
        new TrackedEntityQueryParams()
            .addOrganisationUnit(ouB)
            .setOrgUnitMode(OrganisationUnitSelectionMode.DESCENDANTS);
    trackedEntitites = trackedEntityStore.getTrackedEntities(params);
    assertEquals(5, trackedEntitites.size());
    assertTrue(trackedEntitites.contains(trackedEntityB));
    assertTrue(trackedEntitites.contains(trackedEntityC));
    assertTrue(trackedEntitites.contains(trackedEntityD));
    assertTrue(trackedEntitites.contains(trackedEntityE));
    assertTrue(trackedEntitites.contains(trackedEntityF));
    // Filter by program enrollment
    params = new TrackedEntityQueryParams().setProgram(prA);
    trackedEntitites = trackedEntityStore.getTrackedEntities(params);
    assertEquals(2, trackedEntitites.size());
    assertTrue(trackedEntitites.contains(trackedEntityB));
    assertTrue(trackedEntitites.contains(trackedEntityE));
  }

  @Test
  void testStartsWithQueryOperator() {
    trackedEntityStore.save(trackedEntityA);
    trackedEntityStore.save(trackedEntityB);
    trackedEntityStore.save(trackedEntityC);
    trackedEntityStore.save(trackedEntityD);
    trackedEntityStore.save(trackedEntityE);
    trackedEntityStore.save(trackedEntityF);
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(atA, trackedEntityD, "Male"));
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(atA, trackedEntityE, "Male"));
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(atA, trackedEntityF, "Female"));
    enrollmentService.enrollTrackedEntity(trackedEntityB, prA, new Date(), new Date(), ouB);
    enrollmentService.enrollTrackedEntity(trackedEntityE, prA, new Date(), new Date(), ouB);
    TrackedEntityQueryParams params = new TrackedEntityQueryParams();
    List<TrackedEntity> trackedEntitites = trackedEntityStore.getTrackedEntities(params);
    assertEquals(6, trackedEntitites.size());

    // Filter by attribute with STARTS
    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.SW, "ma", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = trackedEntityStore.getTrackedEntities(params);
    assertEquals(2, trackedEntitites.size());
    assertTrue(trackedEntitites.contains(trackedEntityD));
    assertTrue(trackedEntitites.contains(trackedEntityE));

    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.SW, "al", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = trackedEntityStore.getTrackedEntities(params);
    assertEquals(0, trackedEntitites.size());

    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.SW, "ale", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = trackedEntityStore.getTrackedEntities(params);
    assertEquals(0, trackedEntitites.size());
  }

  @Test
  void testEndsWithQueryOperator() {
    trackedEntityStore.save(trackedEntityA);
    trackedEntityStore.save(trackedEntityB);
    trackedEntityStore.save(trackedEntityC);
    trackedEntityStore.save(trackedEntityD);
    trackedEntityStore.save(trackedEntityE);
    trackedEntityStore.save(trackedEntityF);
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(atA, trackedEntityD, "Male"));
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(atA, trackedEntityE, "Male"));
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(atA, trackedEntityF, "Female"));
    enrollmentService.enrollTrackedEntity(trackedEntityB, prA, new Date(), new Date(), ouB);
    enrollmentService.enrollTrackedEntity(trackedEntityE, prA, new Date(), new Date(), ouB);
    TrackedEntityQueryParams params = new TrackedEntityQueryParams();
    List<TrackedEntity> trackedEntitites = trackedEntityStore.getTrackedEntities(params);
    assertEquals(6, trackedEntitites.size());

    // Filter by attribute with ENDS
    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EW, "emale", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = trackedEntityStore.getTrackedEntities(params);
    assertEquals(1, trackedEntitites.size());
    assertTrue(trackedEntitites.contains(trackedEntityF));

    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EW, "male", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = trackedEntityStore.getTrackedEntities(params);
    assertEquals(3, trackedEntitites.size());
    assertTrue(trackedEntitites.contains(trackedEntityD));
    assertTrue(trackedEntitites.contains(trackedEntityE));
    assertTrue(trackedEntitites.contains(trackedEntityF));

    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EW, "fem", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = trackedEntityStore.getTrackedEntities(params);
    assertEquals(0, trackedEntitites.size());

    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EW, "em", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = trackedEntityStore.getTrackedEntities(params);
    assertEquals(0, trackedEntitites.size());
  }
}
