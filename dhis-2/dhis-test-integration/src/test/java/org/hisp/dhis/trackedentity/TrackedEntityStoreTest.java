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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
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

  @Autowired private TrackedEntityStore teiStore;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private TrackedEntityAttributeValueService attributeValueService;

  @Autowired private TrackedEntityTypeService trackedEntityTypeService;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private TrackedEntityAttributeService trackedEntityAttributeService;

  @Autowired private ProgramService programService;

  private TrackedEntity teiA;

  private TrackedEntity teiB;

  private TrackedEntity teiC;

  private TrackedEntity teiD;

  private TrackedEntity teiE;

  private TrackedEntity teiF;

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
    teiA = createTrackedEntity(ouA);
    teiB = createTrackedEntity(ouB);
    teiC = createTrackedEntity(ouB);
    teiD = createTrackedEntity(ouC);
    teiE = createTrackedEntity(ouC);
    teiF = createTrackedEntity(ouC);
  }

  @Test
  void testTrackedEntityExists() {
    teiStore.save(teiA);
    teiStore.save(teiB);
    dbmsManager.flushSession();
    assertTrue(teiStore.exists(teiA.getUid()));
    assertTrue(teiStore.exists(teiB.getUid()));
    assertFalse(teiStore.exists("aaaabbbbccc"));
  }

  @Test
  void testAddGet() {
    teiStore.save(teiA);
    long idA = teiA.getId();
    teiStore.save(teiB);
    long idB = teiB.getId();
    assertNotNull(teiStore.get(idA));
    assertNotNull(teiStore.get(idB));
  }

  @Test
  void testAddGetbyOu() {
    teiStore.save(teiA);
    long idA = teiA.getId();
    teiStore.save(teiB);
    long idB = teiB.getId();
    assertEquals(teiA.getName(), teiStore.get(idA).getName());
    assertEquals(teiB.getName(), teiStore.get(idB).getName());
  }

  @Test
  void testDelete() {
    teiStore.save(teiA);
    long idA = teiA.getId();
    teiStore.save(teiB);
    long idB = teiB.getId();
    assertNotNull(teiStore.get(idA));
    assertNotNull(teiStore.get(idB));
    teiStore.delete(teiA);
    assertNull(teiStore.get(idA));
    assertNotNull(teiStore.get(idB));
    teiStore.delete(teiB);
    assertNull(teiStore.get(idA));
    assertNull(teiStore.get(idB));
  }

  @Test
  void testGetAll() {
    teiStore.save(teiA);
    teiStore.save(teiB);
    assertTrue(equals(teiStore.getAll(), teiA, teiB));
  }

  @Test
  void testQuery() {
    teiStore.save(teiA);
    teiStore.save(teiB);
    teiStore.save(teiC);
    teiStore.save(teiD);
    teiStore.save(teiE);
    teiStore.save(teiF);
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(atA, teiD, "Male"));
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(atA, teiE, "Male"));
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(atA, teiF, "Female"));
    enrollmentService.enrollTrackedEntity(teiB, prA, new Date(), new Date(), ouB);
    enrollmentService.enrollTrackedEntity(teiE, prA, new Date(), new Date(), ouB);
    // Get all
    TrackedEntityQueryParams params = new TrackedEntityQueryParams();
    List<TrackedEntity> trackedEntitites = teiStore.getTrackedEntities(params);
    assertEquals(6, trackedEntitites.size());
    // Filter by attribute with EQ
    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EQ, "Male", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = teiStore.getTrackedEntities(params);
    assertEquals(2, trackedEntitites.size());
    assertTrue(trackedEntitites.contains(teiD));
    assertTrue(trackedEntitites.contains(teiE));
    // Filter by attribute with EQ
    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EQ, "Female", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = teiStore.getTrackedEntities(params);
    assertEquals(1, trackedEntitites.size());
    assertTrue(trackedEntitites.contains(teiF));

    // Filter by attribute with STARTS
    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.SW, "ma", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = teiStore.getTrackedEntities(params);
    assertEquals(2, trackedEntitites.size());
    assertTrue(trackedEntitites.contains(teiD));
    assertTrue(trackedEntitites.contains(teiE));

    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.SW, "al", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = teiStore.getTrackedEntities(params);
    assertEquals(0, trackedEntitites.size());

    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.SW, "ale", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = teiStore.getTrackedEntities(params);
    assertEquals(0, trackedEntitites.size());

    // Filter by attribute with ENDS
    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EW, "emale", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = teiStore.getTrackedEntities(params);
    assertEquals(1, trackedEntitites.size());
    assertTrue(trackedEntitites.contains(teiF));

    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EW, "male", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = teiStore.getTrackedEntities(params);
    assertEquals(3, trackedEntitites.size());
    assertTrue(trackedEntitites.contains(teiD));
    assertTrue(trackedEntitites.contains(teiE));
    assertTrue(trackedEntitites.contains(teiF));

    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EW, "fem", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = teiStore.getTrackedEntities(params);
    assertEquals(0, trackedEntitites.size());

    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EW, "em", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = teiStore.getTrackedEntities(params);
    assertEquals(0, trackedEntitites.size());

    // Filter by selected org units
    params =
        new TrackedEntityQueryParams()
            .addOrganisationUnit(ouB)
            .setOrgUnitMode(OrganisationUnitSelectionMode.SELECTED);
    trackedEntitites = teiStore.getTrackedEntities(params);
    assertEquals(2, trackedEntitites.size());
    assertTrue(trackedEntitites.contains(teiB));
    assertTrue(trackedEntitites.contains(teiC));
    // Filter by descendants org units
    params =
        new TrackedEntityQueryParams()
            .addOrganisationUnit(ouB)
            .setOrgUnitMode(OrganisationUnitSelectionMode.DESCENDANTS);
    trackedEntitites = teiStore.getTrackedEntities(params);
    assertEquals(5, trackedEntitites.size());
    assertTrue(trackedEntitites.contains(teiB));
    assertTrue(trackedEntitites.contains(teiC));
    assertTrue(trackedEntitites.contains(teiD));
    assertTrue(trackedEntitites.contains(teiE));
    assertTrue(trackedEntitites.contains(teiF));
    // Filter by program enrollment
    params = new TrackedEntityQueryParams().setProgram(prA);
    trackedEntitites = teiStore.getTrackedEntities(params);
    assertEquals(2, trackedEntitites.size());
    assertTrue(trackedEntitites.contains(teiB));
    assertTrue(trackedEntitites.contains(teiE));
  }

  @Test
  void testStartsWithQueryOperator() {
    teiStore.save(teiA);
    teiStore.save(teiB);
    teiStore.save(teiC);
    teiStore.save(teiD);
    teiStore.save(teiE);
    teiStore.save(teiF);
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(atA, teiD, "Male"));
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(atA, teiE, "Male"));
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(atA, teiF, "Female"));
    enrollmentService.enrollTrackedEntity(teiB, prA, new Date(), new Date(), ouB);
    enrollmentService.enrollTrackedEntity(teiE, prA, new Date(), new Date(), ouB);
    TrackedEntityQueryParams params = new TrackedEntityQueryParams();
    List<TrackedEntity> trackedEntitites = teiStore.getTrackedEntities(params);
    assertEquals(6, trackedEntitites.size());

    // Filter by attribute with STARTS
    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.SW, "ma", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = teiStore.getTrackedEntities(params);
    assertEquals(2, trackedEntitites.size());
    assertTrue(trackedEntitites.contains(teiD));
    assertTrue(trackedEntitites.contains(teiE));

    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.SW, "al", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = teiStore.getTrackedEntities(params);
    assertEquals(0, trackedEntitites.size());

    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.SW, "ale", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = teiStore.getTrackedEntities(params);
    assertEquals(0, trackedEntitites.size());
  }

  @Test
  void testEndsWithQueryOperator() {
    teiStore.save(teiA);
    teiStore.save(teiB);
    teiStore.save(teiC);
    teiStore.save(teiD);
    teiStore.save(teiE);
    teiStore.save(teiF);
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(atA, teiD, "Male"));
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(atA, teiE, "Male"));
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(atA, teiF, "Female"));
    enrollmentService.enrollTrackedEntity(teiB, prA, new Date(), new Date(), ouB);
    enrollmentService.enrollTrackedEntity(teiE, prA, new Date(), new Date(), ouB);
    TrackedEntityQueryParams params = new TrackedEntityQueryParams();
    List<TrackedEntity> trackedEntitites = teiStore.getTrackedEntities(params);
    assertEquals(6, trackedEntitites.size());

    // Filter by attribute with ENDS
    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EW, "emale", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = teiStore.getTrackedEntities(params);
    assertEquals(1, trackedEntitites.size());
    assertTrue(trackedEntitites.contains(teiF));

    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EW, "male", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = teiStore.getTrackedEntities(params);
    assertEquals(3, trackedEntitites.size());
    assertTrue(trackedEntitites.contains(teiD));
    assertTrue(trackedEntitites.contains(teiE));
    assertTrue(trackedEntitites.contains(teiF));

    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EW, "fem", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = teiStore.getTrackedEntities(params);
    assertEquals(0, trackedEntitites.size());

    params =
        new TrackedEntityQueryParams()
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EW, "em", ValueType.TEXT, AggregationType.NONE, null));
    trackedEntitites = teiStore.getTrackedEntities(params);
    assertEquals(0, trackedEntitites.size());
  }

  @Test
  void testPotentialDuplicateInGridQuery() {
    TrackedEntityType trackedEntityTypeA = createTrackedEntityType('A');
    trackedEntityTypeService.addTrackedEntityType(trackedEntityTypeA);
    teiA.setTrackedEntityType(trackedEntityTypeA);
    teiA.setPotentialDuplicate(true);
    teiStore.save(teiA);
    teiB.setTrackedEntityType(trackedEntityTypeA);
    teiB.setPotentialDuplicate(true);
    teiStore.save(teiB);
    teiC.setTrackedEntityType(trackedEntityTypeA);
    teiStore.save(teiC);
    teiD.setTrackedEntityType(trackedEntityTypeA);
    teiStore.save(teiD);
    dbmsManager.flushSession();
    // Get all
    TrackedEntityQueryParams params = new TrackedEntityQueryParams();
    params.setTrackedEntityType(trackedEntityTypeA);
    List<Map<String, String>> trackedEntitites = teiStore.getTrackedEntitiesGrid(params);
    assertEquals(4, trackedEntitites.size());
    trackedEntitites.forEach(
        teiMap -> {
          if (teiMap.get(TrackedEntityQueryParams.TRACKED_ENTITY_ID).equals(teiA.getUid())
              || teiMap.get(TrackedEntityQueryParams.TRACKED_ENTITY_ID).equals(teiB.getUid())) {
            assertTrue(
                Boolean.parseBoolean(teiMap.get(TrackedEntityQueryParams.POTENTIAL_DUPLICATE)));
          } else {
            assertFalse(
                Boolean.parseBoolean(teiMap.get(TrackedEntityQueryParams.POTENTIAL_DUPLICATE)));
          }
        });
  }

  @Test
  void testProgramAttributeOfTypeOrgUnitIsResolvedToOrgUnitName() {
    TrackedEntityType trackedEntityTypeA = createTrackedEntityType('A');
    trackedEntityTypeService.addTrackedEntityType(trackedEntityTypeA);
    teiA.setTrackedEntityType(trackedEntityTypeA);
    teiStore.save(teiA);
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(atC, teiA, ouC.getUid()));
    enrollmentService.enrollTrackedEntity(teiA, prA, new Date(), new Date(), ouA);
    TrackedEntityQueryParams params = new TrackedEntityQueryParams();
    params.setTrackedEntityType(trackedEntityTypeA);
    params.setOrgUnitMode(OrganisationUnitSelectionMode.ALL);
    QueryItem queryItem = new QueryItem(atC);
    queryItem.setValueType(atC.getValueType());
    params.setAttributes(Collections.singletonList(queryItem));
    List<Map<String, String>> grid = teiStore.getTrackedEntitiesGrid(params);
    assertThat(grid, hasSize(1));
    assertThat(grid.get(0).keySet(), hasSize(9));
    assertThat(grid.get(0).get(atC.getUid()), is("OrganisationUnitC"));
  }
}
