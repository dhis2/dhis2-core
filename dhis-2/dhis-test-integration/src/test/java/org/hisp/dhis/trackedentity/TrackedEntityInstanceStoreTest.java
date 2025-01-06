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
import java.util.Set;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
class TrackedEntityInstanceStoreTest extends TransactionalIntegrationTest {

  @Autowired private TrackedEntityInstanceStore teiStore;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private TrackedEntityAttributeValueService attributeValueService;

  @Autowired private TrackedEntityTypeService trackedEntityTypeService;

  @Autowired private ProgramInstanceService programInstanceService;

  @Autowired private TrackedEntityAttributeService trackedEntityAttributeService;

  @Autowired private ProgramService programService;

  private TrackedEntityInstance teiA;

  private TrackedEntityInstance teiB;

  private TrackedEntityInstance teiC;

  private TrackedEntityInstance teiD;

  private TrackedEntityInstance teiE;

  private TrackedEntityInstance teiF;

  private TrackedEntityAttribute atA;

  private TrackedEntityAttribute atB;

  private TrackedEntityAttribute atC;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  private OrganisationUnit ouC;

  private Program prA;

  private Program prB;

  private TrackedEntityType trackedEntityType;

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
    trackedEntityType = createTrackedEntityType('A');
    trackedEntityType.setMaxTeiCountToReturn(1000);
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType);
    prA = createProgram('A', null, null);
    prA.setTrackedEntityType(trackedEntityType);
    prB = createProgram('B', null, null);
    prB.setTrackedEntityType(trackedEntityType);
    programService.addProgram(prA);
    programService.addProgram(prB);
    teiA = createTrackedEntityInstance(ouA);
    teiB = createTrackedEntityInstance(ouB);
    teiC = createTrackedEntityInstance(ouB);
    teiD = createTrackedEntityInstance(ouC);
    teiE = createTrackedEntityInstance(ouC);
    teiF = createTrackedEntityInstance(ouC);
    teiA.setTrackedEntityType(trackedEntityType);
    teiB.setTrackedEntityType(trackedEntityType);
    teiC.setTrackedEntityType(trackedEntityType);
    teiD.setTrackedEntityType(trackedEntityType);
    teiE.setTrackedEntityType(trackedEntityType);
    teiF.setTrackedEntityType(trackedEntityType);
  }

  @Test
  void testTrackedEntityInstanceExists() {
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
    programInstanceService.enrollTrackedEntityInstance(teiB, prA, new Date(), new Date(), ouB);
    programInstanceService.enrollTrackedEntityInstance(teiE, prA, new Date(), new Date(), ouB);
    // Get all
    TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
    params.setPrograms(List.of(prA));
    params.setTrackedEntityType(trackedEntityType);
    List<TrackedEntityInstance> teis = teiStore.getTrackedEntityInstances(params);
    assertEquals(6, teis.size());
    // Filter by attribute with EQ
    params =
        new TrackedEntityInstanceQueryParams()
            .setPrograms(List.of(prA, prB))
            .setTrackedEntityType(trackedEntityType)
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EQ, "Male", ValueType.TEXT, AggregationType.NONE, null));
    teis = teiStore.getTrackedEntityInstances(params);
    assertEquals(2, teis.size());
    assertTrue(teis.contains(teiD));
    assertTrue(teis.contains(teiE));
    // Filter by attribute with EQ
    params =
        new TrackedEntityInstanceQueryParams()
            .setPrograms(List.of(prA, prB))
            .setTrackedEntityType(trackedEntityType)
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EQ, "Female", ValueType.TEXT, AggregationType.NONE, null));
    teis = teiStore.getTrackedEntityInstances(params);
    assertEquals(1, teis.size());
    assertTrue(teis.contains(teiF));

    // Filter by attribute with STARTS
    params =
        new TrackedEntityInstanceQueryParams()
            .setPrograms(List.of(prA, prB))
            .setTrackedEntityType(trackedEntityType)
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.SW, "ma", ValueType.TEXT, AggregationType.NONE, null));
    teis = teiStore.getTrackedEntityInstances(params);
    assertEquals(2, teis.size());
    assertTrue(teis.contains(teiD));
    assertTrue(teis.contains(teiE));

    params =
        new TrackedEntityInstanceQueryParams()
            .setPrograms(List.of(prA, prB))
            .setTrackedEntityType(trackedEntityType)
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.SW, "al", ValueType.TEXT, AggregationType.NONE, null));
    teis = teiStore.getTrackedEntityInstances(params);
    assertEquals(0, teis.size());

    params =
        new TrackedEntityInstanceQueryParams()
            .setPrograms(List.of(prA, prB))
            .setTrackedEntityType(trackedEntityType)
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.SW, "ale", ValueType.TEXT, AggregationType.NONE, null));
    teis = teiStore.getTrackedEntityInstances(params);
    assertEquals(0, teis.size());

    // Filter by attribute with ENDS
    params =
        new TrackedEntityInstanceQueryParams()
            .setPrograms(List.of(prA, prB))
            .setTrackedEntityType(trackedEntityType)
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EW, "emale", ValueType.TEXT, AggregationType.NONE, null));
    teis = teiStore.getTrackedEntityInstances(params);
    assertEquals(1, teis.size());
    assertTrue(teis.contains(teiF));

    params =
        new TrackedEntityInstanceQueryParams()
            .setPrograms(List.of(prA, prB))
            .setTrackedEntityType(trackedEntityType)
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EW, "male", ValueType.TEXT, AggregationType.NONE, null));
    teis = teiStore.getTrackedEntityInstances(params);
    assertEquals(3, teis.size());
    assertTrue(teis.contains(teiD));
    assertTrue(teis.contains(teiE));
    assertTrue(teis.contains(teiF));

    params =
        new TrackedEntityInstanceQueryParams()
            .setPrograms(List.of(prA, prB))
            .setTrackedEntityType(trackedEntityType)
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EW, "fem", ValueType.TEXT, AggregationType.NONE, null));
    teis = teiStore.getTrackedEntityInstances(params);
    assertEquals(0, teis.size());

    params =
        new TrackedEntityInstanceQueryParams()
            .setPrograms(List.of(prA, prB))
            .setTrackedEntityType(trackedEntityType)
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EW, "em", ValueType.TEXT, AggregationType.NONE, null));
    teis = teiStore.getTrackedEntityInstances(params);
    assertEquals(0, teis.size());

    // Filter by selected org units
    params =
        new TrackedEntityInstanceQueryParams()
            .setPrograms(List.of(prA, prB))
            .setTrackedEntityType(trackedEntityType)
            .addOrganisationUnit(ouB)
            .setOrganisationUnitMode(OrganisationUnitSelectionMode.SELECTED);
    teis = teiStore.getTrackedEntityInstances(params);
    assertEquals(3, teis.size());
    assertTrue(teis.contains(teiB));
    assertTrue(teis.contains(teiC));
    assertTrue(teis.contains(teiE));
    // Filter by descendants org units
    params =
        new TrackedEntityInstanceQueryParams()
            .setPrograms(List.of(prA, prB))
            .setTrackedEntityType(trackedEntityType)
            .addOrganisationUnit(ouB)
            .setOrganisationUnitMode(OrganisationUnitSelectionMode.DESCENDANTS);
    teis = teiStore.getTrackedEntityInstances(params);
    assertEquals(5, teis.size());
    assertTrue(teis.contains(teiB));
    assertTrue(teis.contains(teiC));
    assertTrue(teis.contains(teiD));
    assertTrue(teis.contains(teiE));
    assertTrue(teis.contains(teiF));
    // Filter by program enrollment
    params =
        new TrackedEntityInstanceQueryParams()
            .setProgram(prA)
            .setTrackedEntityType(trackedEntityType);
    teis = teiStore.getTrackedEntityInstances(params);
    assertEquals(2, teis.size());
    assertTrue(teis.contains(teiB));
    assertTrue(teis.contains(teiE));
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
    programInstanceService.enrollTrackedEntityInstance(teiB, prA, new Date(), new Date(), ouB);
    programInstanceService.enrollTrackedEntityInstance(teiE, prA, new Date(), new Date(), ouB);
    TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
    params.setPrograms(List.of(prA, prB));
    params.setTrackedEntityType(trackedEntityType);
    List<TrackedEntityInstance> teis = teiStore.getTrackedEntityInstances(params);
    assertEquals(6, teis.size());

    // Filter by attribute with STARTS
    params =
        new TrackedEntityInstanceQueryParams()
            .setPrograms(List.of(prA, prB))
            .setTrackedEntityType(trackedEntityType)
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.SW, "ma", ValueType.TEXT, AggregationType.NONE, null));
    teis = teiStore.getTrackedEntityInstances(params);
    assertEquals(2, teis.size());
    assertTrue(teis.contains(teiD));
    assertTrue(teis.contains(teiE));

    params =
        new TrackedEntityInstanceQueryParams()
            .setPrograms(List.of(prA, prB))
            .setTrackedEntityType(trackedEntityType)
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.SW, "al", ValueType.TEXT, AggregationType.NONE, null));
    teis = teiStore.getTrackedEntityInstances(params);
    assertEquals(0, teis.size());

    params =
        new TrackedEntityInstanceQueryParams()
            .setPrograms(List.of(prA, prB))
            .setTrackedEntityType(trackedEntityType)
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.SW, "ale", ValueType.TEXT, AggregationType.NONE, null));
    teis = teiStore.getTrackedEntityInstances(params);
    assertEquals(0, teis.size());
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
    programInstanceService.enrollTrackedEntityInstance(teiB, prA, new Date(), new Date(), ouB);
    programInstanceService.enrollTrackedEntityInstance(teiE, prA, new Date(), new Date(), ouB);
    TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
    params.setPrograms(List.of(prA));
    params.setTrackedEntityType(trackedEntityType);
    List<TrackedEntityInstance> teis = teiStore.getTrackedEntityInstances(params);
    assertEquals(6, teis.size());

    // Filter by attribute with ENDS
    params =
        new TrackedEntityInstanceQueryParams()
            .setPrograms(List.of(prA, prB))
            .setTrackedEntityType(trackedEntityType)
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EW, "emale", ValueType.TEXT, AggregationType.NONE, null));
    teis = teiStore.getTrackedEntityInstances(params);
    assertEquals(1, teis.size());
    assertTrue(teis.contains(teiF));

    params =
        new TrackedEntityInstanceQueryParams()
            .setPrograms(List.of(prA, prB))
            .setTrackedEntityType(trackedEntityType)
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EW, "male", ValueType.TEXT, AggregationType.NONE, null));
    teis = teiStore.getTrackedEntityInstances(params);
    assertEquals(3, teis.size());
    assertTrue(teis.contains(teiD));
    assertTrue(teis.contains(teiE));
    assertTrue(teis.contains(teiF));

    params =
        new TrackedEntityInstanceQueryParams()
            .setPrograms(List.of(prA, prB))
            .setTrackedEntityType(trackedEntityType)
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EW, "fem", ValueType.TEXT, AggregationType.NONE, null));
    teis = teiStore.getTrackedEntityInstances(params);
    assertEquals(0, teis.size());

    params =
        new TrackedEntityInstanceQueryParams()
            .setPrograms(List.of(prA, prB))
            .setTrackedEntityType(trackedEntityType)
            .addFilter(
                new QueryItem(
                    atA, QueryOperator.EW, "em", ValueType.TEXT, AggregationType.NONE, null));
    teis = teiStore.getTrackedEntityInstances(params);
    assertEquals(0, teis.size());
  }

  @Test
  void testPotentialDuplicateInGridQuery() {
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType);
    teiA.setTrackedEntityType(trackedEntityType);
    teiA.setPotentialDuplicate(true);
    teiStore.save(teiA);
    teiB.setTrackedEntityType(trackedEntityType);
    teiB.setPotentialDuplicate(true);
    teiStore.save(teiB);
    teiC.setTrackedEntityType(trackedEntityType);
    teiStore.save(teiC);
    teiD.setTrackedEntityType(trackedEntityType);
    teiStore.save(teiD);
    dbmsManager.flushSession();
    // Get all
    TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
    params.setPrograms(List.of(prA, prB));
    params.setTrackedEntityType(trackedEntityType);
    List<Map<String, String>> teis = teiStore.getTrackedEntityInstancesGrid(params);
    assertEquals(4, teis.size());
    teis.forEach(
        teiMap -> {
          if (teiMap
                  .get(TrackedEntityInstanceQueryParams.TRACKED_ENTITY_INSTANCE_ID)
                  .equals(teiA.getUid())
              || teiMap
                  .get(TrackedEntityInstanceQueryParams.TRACKED_ENTITY_INSTANCE_ID)
                  .equals(teiB.getUid())) {
            assertTrue(
                Boolean.parseBoolean(
                    teiMap.get(TrackedEntityInstanceQueryParams.POTENTIAL_DUPLICATE)));
          } else {
            assertFalse(
                Boolean.parseBoolean(
                    teiMap.get(TrackedEntityInstanceQueryParams.POTENTIAL_DUPLICATE)));
          }
        });
  }

  @Test
  void shouldHandleNullProgramWhenCheckingMaxTeiLimitInGridQuery() {
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType);
    teiA.setTrackedEntityType(trackedEntityType);
    teiA.setPotentialDuplicate(true);
    teiStore.save(teiA);
    teiB.setTrackedEntityType(trackedEntityType);
    teiB.setPotentialDuplicate(true);
    teiStore.save(teiB);
    teiC.setTrackedEntityType(trackedEntityType);
    teiStore.save(teiC);
    teiD.setTrackedEntityType(trackedEntityType);
    teiStore.save(teiD);
    dbmsManager.flushSession();

    TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
    params.setOrganisationUnitMode(OrganisationUnitSelectionMode.ACCESSIBLE);
    params.setPrograms(List.of(prA, prB));
    params.setTrackedEntityType(trackedEntityType);
    params.setTrackedEntityInstanceUids(
        Set.of(teiA.getUid(), teiB.getUid(), teiC.getUid(), teiD.getUid()));

    int count = teiStore.getTrackedEntityInstanceCountForGridWithMaxTeiLimit(params);
    assertEquals(4, count);
  }

  @Test
  void testProgramAttributeOfTypeOrgUnitIsResolvedToOrgUnitName() {
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType);
    teiA.setTrackedEntityType(trackedEntityType);
    teiStore.save(teiA);
    prA.setTrackedEntityType(trackedEntityType);
    programService.updateProgram(prA);
    attributeValueService.addTrackedEntityAttributeValue(
        new TrackedEntityAttributeValue(atC, teiA, ouC.getUid()));
    programInstanceService.enrollTrackedEntityInstance(teiA, prA, new Date(), new Date(), ouA);
    TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
    params.setTrackedEntityType(trackedEntityType);
    params.setOrganisationUnitMode(OrganisationUnitSelectionMode.ALL);
    params.setPrograms(List.of(prA, prB));
    params.setTrackedEntityType(trackedEntityType);
    QueryItem queryItem = new QueryItem(atC);
    queryItem.setValueType(atC.getValueType());
    params.setAttributes(Collections.singletonList(queryItem));
    List<Map<String, String>> grid = teiStore.getTrackedEntityInstancesGrid(params);
    assertThat(grid, hasSize(1));
    assertThat(grid.get(0).keySet(), hasSize(9));
    assertThat(grid.get(0).get(atC.getUid()), is("OrganisationUnitC"));
  }
}
