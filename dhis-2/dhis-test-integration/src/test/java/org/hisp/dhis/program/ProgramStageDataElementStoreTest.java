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
package org.hisp.dhis.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CustomType;
import org.hibernate.type.ManyToOneType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.hibernate.jsonb.type.JsonDeviceRenderTypeMap;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.render.DeviceRenderTypeMap;
import org.hisp.dhis.render.RenderDevice;
import org.hisp.dhis.render.type.ValueTypeRenderingObject;
import org.hisp.dhis.render.type.ValueTypeRenderingType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Chau Thu Tran
 */
@Transactional
class ProgramStageDataElementStoreTest extends PostgresIntegrationTestBase {

  @Autowired private ProgramStageDataElementStore programStageDataElementStore;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private DataElementService dataElementService;

  @Autowired private ProgramService programService;

  @Autowired private ProgramStageService programStageService;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private EntityManagerFactory entityManagerFactory;

  private OrganisationUnit organisationUnit;

  private ProgramStage stageA;

  private ProgramStage stageB;

  private DataElement dataElementA;

  private DataElement dataElementB;

  private ProgramStageDataElement stageDataElementA;

  private ProgramStageDataElement stageDataElementB;

  @BeforeEach
  void setUp() {
    organisationUnit = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(organisationUnit);
    Program program = createProgram('A', new HashSet<>(), organisationUnit);
    programService.addProgram(program);
    stageA = new ProgramStage("A", program);
    stageA.setUid("StageA");
    stageA.setSortOrder(1);
    programStageService.saveProgramStage(stageA);
    stageB = new ProgramStage("B", program);
    stageB.setSortOrder(2);
    programStageService.saveProgramStage(stageB);
    Set<ProgramStage> programStages = new HashSet<>();
    programStages.add(stageA);
    programStages.add(stageB);
    program.setProgramStages(programStages);
    programService.updateProgram(program);
    dataElementA = createDataElement('A');
    dataElementB = createDataElement('B');
    dataElementService.addDataElement(dataElementA);
    dataElementService.addDataElement(dataElementB);
    stageDataElementA = new ProgramStageDataElement(stageA, dataElementA, false, 1);
    stageDataElementB = new ProgramStageDataElement(stageA, dataElementB, false, 2);
  }

  @Test
  void testAddProgramStageDataElement() {
    programStageDataElementStore.save(stageDataElementA);
    programStageDataElementStore.save(stageDataElementB);
    assertNotNull(programStageDataElementStore.get(stageA, dataElementA));
    assertNotNull(programStageDataElementStore.get(stageA, dataElementB));
  }

  @Test
  void testUpdateProgramStageDataElement() {
    programStageDataElementStore.save(stageDataElementA);
    assertNotNull(programStageDataElementStore.get(stageA, dataElementA));
    stageDataElementA.setCompulsory(true);
    programStageDataElementStore.update(stageDataElementA);
    assertEquals(true, programStageDataElementStore.get(stageA, dataElementA).isCompulsory());
  }

  @Test
  void testDeleteProgramStageDataElement() {
    programStageDataElementStore.save(stageDataElementA);
    programStageDataElementStore.save(stageDataElementB);
    assertNotNull(programStageDataElementStore.get(stageA, dataElementA));
    assertNotNull(programStageDataElementStore.get(stageA, dataElementB));
    programStageDataElementStore.delete(stageDataElementA);
    assertNull(programStageDataElementStore.get(stageA, dataElementA));
    assertNotNull(programStageDataElementStore.get(stageA, dataElementB));
    programStageDataElementStore.delete(stageDataElementB);
    assertNull(programStageDataElementStore.get(stageA, dataElementA));
    assertNull(programStageDataElementStore.get(stageA, dataElementB));
  }

  @Test
  void testGetByStageElement() {
    programStageDataElementStore.save(stageDataElementA);
    programStageDataElementStore.save(stageDataElementB);
    assertNotNull(programStageDataElementStore.get(stageA, dataElementA));
    assertNotNull(programStageDataElementStore.get(stageA, dataElementB));
  }

  @Test
  void testGetAllProgramStageDataElements() {
    programStageDataElementStore.save(stageDataElementA);
    programStageDataElementStore.save(stageDataElementB);
    assertTrue(equals(programStageDataElementStore.getAll(), stageDataElementA, stageDataElementB));
  }

  @Test
  @DisplayName("retrieving Program Stage DataElements by data element returns expected entries")
  void retrievingPSDEByDataElement() {
    // given
    programStageDataElementStore.save(stageDataElementA);
    programStageDataElementStore.save(stageDataElementB);

    // when
    List<ProgramStageDataElement> psdes =
        programStageDataElementStore.getAllByDataElement(List.of(dataElementA, dataElementB));

    // then
    assertEquals(2, psdes.size());
    assertTrue(psdes.containsAll(List.of(stageDataElementA, stageDataElementB)));
    assertTrue(
        psdes.stream()
            .map(psde -> psde.getDataElement().getUid())
            .toList()
            .containsAll(List.of(dataElementA.getUid(), dataElementB.getUid())));
  }

  @Test
  @DisplayName("the id is generated by a sequence generator on hibernate_sequence")
  void testIdGeneratorUsesHibernateSequence() {
    SequenceStyleGenerator generator =
        assertInstanceOf(SequenceStyleGenerator.class, persister().getIdentifierGenerator());

    assertEquals(
        "hibernate_sequence",
        generator.getDatabaseStructure().getPhysicalName().getObjectName().getText());
  }

  @Test
  @DisplayName("every mapped property is written to its column and read back")
  void testColumnsRoundTrip() {
    User user = getAdminUser();
    Date created = new Date(1_600_000_000_000L);
    Date lastUpdated = new Date(1_700_000_000_000L);
    ProgramStageDataElement psde = new ProgramStageDataElement(stageA, dataElementA, true, 7);
    psde.setUid("psdeRoundT1");
    psde.setCode("PSDE_ROUND_TRIP");
    psde.setCreated(created);
    psde.setLastUpdated(lastUpdated);
    psde.setLastUpdatedBy(user);
    psde.setAllowProvidedElsewhere(true);
    psde.setDisplayInReports(true);
    psde.setAllowFutureDate(true);
    psde.setRenderOptionsAsRadio(true);
    psde.setRenderType(renderType(ValueTypeRenderingType.VERTICAL_RADIOBUTTONS));
    psde.setSkipSynchronization(true);
    psde.setSkipAnalytics(true);
    entityManager.persist(psde);
    entityManager.flush();

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select uid, code, created, lastupdated, lastupdatedby, programstageid, dataelementid,"
                + " compulsory, allowprovidedelsewhere, sort_order, displayinreports,"
                + " allowfuturedate, renderoptionsasradio, skipsynchronization, skipanalytics,"
                + " rendertype -> 'MOBILE' ->> 'type' as rendertype"
                + " from programstagedataelement where programstagedataelementid = ?",
            psde.getId());
    assertEquals(
        Map.ofEntries(
            Map.entry("uid", "psdeRoundT1"),
            Map.entry("code", "PSDE_ROUND_TRIP"),
            Map.entry("created", new Timestamp(created.getTime())),
            Map.entry("lastupdated", new Timestamp(lastUpdated.getTime())),
            Map.entry("lastupdatedby", user.getId()),
            Map.entry("programstageid", stageA.getId()),
            Map.entry("dataelementid", dataElementA.getId()),
            Map.entry("compulsory", true),
            Map.entry("allowprovidedelsewhere", true),
            Map.entry("sort_order", 7),
            Map.entry("displayinreports", true),
            Map.entry("allowfuturedate", true),
            Map.entry("renderoptionsasradio", true),
            Map.entry("skipsynchronization", true),
            Map.entry("skipanalytics", true),
            Map.entry("rendertype", "VERTICAL_RADIOBUTTONS")),
        row);

    clearSessionAndSecondLevelCache();

    ProgramStageDataElement reloaded =
        entityManager.find(ProgramStageDataElement.class, psde.getId());
    assertEquals("psdeRoundT1", reloaded.getUid());
    assertEquals("PSDE_ROUND_TRIP", reloaded.getCode());
    assertEquals(created.getTime(), reloaded.getCreated().getTime());
    assertEquals(lastUpdated.getTime(), reloaded.getLastUpdated().getTime());
    assertEquals(user.getUid(), reloaded.getLastUpdatedBy().getUid());
    assertEquals(stageA.getUid(), reloaded.getProgramStage().getUid());
    assertEquals(dataElementA.getUid(), reloaded.getDataElement().getUid());
    assertTrue(reloaded.isCompulsory());
    assertTrue(reloaded.getAllowProvidedElsewhere());
    assertEquals(7, reloaded.getSortOrder());
    assertTrue(reloaded.getDisplayInReports());
    assertTrue(reloaded.getAllowFutureDate());
    assertTrue(reloaded.getRenderOptionsAsRadio());
    assertTrue(reloaded.getSkipSynchronization());
    assertTrue(reloaded.getSkipAnalytics());
    ValueTypeRenderingObject mobile = reloaded.getRenderType().get(RenderDevice.MOBILE);
    assertEquals(ValueTypeRenderingType.VERTICAL_RADIOBUTTONS, mobile.getType());
  }

  @Test
  @DisplayName(
      "uid, created, lastUpdated, dataElement, compulsory and skipAnalytics are mapped not-null,"
          + " the other properties not")
  void testNullability() {
    EntityPersister persister = persister();
    boolean[] nullable = persister.getPropertyNullability();

    for (String property :
        List.of("uid", "created", "lastUpdated", "dataElement", "compulsory", "skipAnalytics")) {
      assertFalse(nullable[propertyIndex(persister, property)], property);
    }
    for (String property :
        List.of(
            "code",
            "lastUpdatedBy",
            "programStage",
            "allowProvidedElsewhere",
            "sortOrder",
            "displayInReports",
            "allowFutureDate",
            "renderOptionsAsRadio",
            "renderType",
            "skipSynchronization")) {
      assertTrue(nullable[propertyIndex(persister, property)], property);
    }
  }

  @Test
  @DisplayName("created is not updatable, lastUpdated is")
  void testCreatedIsNotUpdatable() {
    EntityPersister persister = persister();
    boolean[] updatable = persister.getPropertyUpdateability();

    assertFalse(updatable[propertyIndex(persister, "created")]);
    assertTrue(updatable[propertyIndex(persister, "lastUpdated")]);
  }

  @Test
  @DisplayName("renderType is bound as a JSON device render type map of value type renderings")
  void testRenderTypeIsBoundAsJsonDeviceRenderTypeMap() {
    CustomType type = assertInstanceOf(CustomType.class, persister().getPropertyType("renderType"));

    JsonDeviceRenderTypeMap userType =
        assertInstanceOf(JsonDeviceRenderTypeMap.class, type.getUserType());
    assertEquals(DeviceRenderTypeMap.class, userType.returnedClass());
  }

  @Test
  @DisplayName("programStage, dataElement and lastUpdatedBy are loaded lazily")
  void testManyToOnesAreLazy() {
    EntityPersister persister = persister();

    for (String property : List.of("programStage", "dataElement", "lastUpdatedBy")) {
      ManyToOneType type =
          assertInstanceOf(ManyToOneType.class, persister.getPropertyType(property));
      assertFalse(type.isEager(null), property);
    }
  }

  @Test
  @DisplayName("the entity is not second-level cached")
  void testEntityIsNotCached() {
    EntityPersister persister = persister();

    assertFalse(persister.canReadFromCache());
    assertNull(persister.getCacheAccessStrategy());
  }

  @Test
  @DisplayName(
      "a program stage saves its data elements by cascade, loads them by sort order and deletes"
          + " a removed one")
  void testProgramStageOwnsDataElementsInSortOrder() {
    ProgramStageDataElement second = new ProgramStageDataElement(null, dataElementA, false, 2);
    ProgramStageDataElement first = new ProgramStageDataElement(null, dataElementB, false, 1);
    second.setAutoFields();
    first.setAutoFields();
    stageB.getProgramStageDataElements().add(second);
    stageB.getProgramStageDataElements().add(first);
    programStageService.updateProgramStage(stageB);
    entityManager.flush();

    assertEquals(
        List.of(dataElementB.getId(), dataElementA.getId()),
        jdbcTemplate.queryForList(
            "select dataelementid from programstagedataelement where programstageid = ?"
                + " order by sort_order",
            Long.class,
            stageB.getId()));

    clearSessionAndSecondLevelCache();

    ProgramStage loaded = programStageService.getProgramStage(stageB.getId());
    assertEquals(
        List.of(dataElementB.getUid(), dataElementA.getUid()),
        loaded.getProgramStageDataElements().stream()
            .map(psde -> psde.getDataElement().getUid())
            .toList());
    loaded.getProgramStageDataElements().removeIf(psde -> psde.getSortOrder() == 1);
    programStageService.updateProgramStage(loaded);
    entityManager.flush();

    assertEquals(
        List.of(dataElementA.getId()),
        jdbcTemplate.queryForList(
            "select dataelementid from programstagedataelement where programstageid = ?",
            Long.class,
            stageB.getId()));
    assertEquals(
        0,
        jdbcTemplate.queryForObject(
            "select count(*) from programstagedataelement where programstagedataelementid = ?",
            Integer.class,
            first.getId()));
  }

  /** Reloads must hit the database, not the second-level cache. */
  private void clearSessionAndSecondLevelCache() {
    clearSession();
    entityManagerFactory.getCache().evictAll();
  }

  private EntityPersister persister() {
    return entityManagerFactory
        .unwrap(SessionFactoryImplementor.class)
        .getMetamodel()
        .entityPersister(ProgramStageDataElement.class);
  }

  private static int propertyIndex(EntityPersister persister, String property) {
    return persister.getEntityMetamodel().getPropertyIndex(property);
  }

  private static DeviceRenderTypeMap<ValueTypeRenderingObject> renderType(
      ValueTypeRenderingType type) {
    DeviceRenderTypeMap<ValueTypeRenderingObject> renderType = new DeviceRenderTypeMap<>();
    renderType.put(RenderDevice.MOBILE, new ValueTypeRenderingObject(type));
    return renderType;
  }
}
