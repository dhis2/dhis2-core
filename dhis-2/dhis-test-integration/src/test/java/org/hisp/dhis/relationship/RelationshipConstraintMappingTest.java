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
package org.hisp.dhis.relationship;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.sql.Types;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CustomType;
import org.hibernate.type.EnumType;
import org.hibernate.type.ManyToOneType;
import org.hisp.dhis.hibernate.jsonb.type.SafeJsonBinaryType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackerdataview.TrackerDataView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Tests the persistence mapping of {@link RelationshipConstraint}. */
@Transactional
class RelationshipConstraintMappingTest extends PostgresIntegrationTestBase {

  @Autowired private RelationshipTypeStore relationshipTypeStore;

  @Autowired private TrackedEntityTypeService trackedEntityTypeService;

  @Autowired private ProgramService programService;

  @Autowired private ProgramStageService programStageService;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private EntityManagerFactory entityManagerFactory;

  private TrackedEntityType trackedEntityType;

  private Program program;

  private ProgramStage programStage;

  @BeforeEach
  void setUp() {
    trackedEntityType = createTrackedEntityType('A');
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType);
    program = createProgram('A');
    program.setTrackedEntityType(trackedEntityType);
    programService.addProgram(program);
    programStage = createProgramStage('A', program);
    programStageService.saveProgramStage(programStage);
  }

  @Test
  void testIdGeneratorUsesHibernateSequence() {
    SequenceStyleGenerator generator =
        assertInstanceOf(SequenceStyleGenerator.class, persister().getIdentifierGenerator());

    assertEquals(
        "hibernate_sequence",
        generator.getDatabaseStructure().getPhysicalName().getObjectName().getText());
    assertEquals(1, generator.getDatabaseStructure().getIncrementSize());
  }

  @Test
  void testPersisterMapsTypesNullabilityLazinessAndReadWriteCache() {
    EntityPersister persister = persister();

    CustomType entityType =
        assertInstanceOf(CustomType.class, persister.getPropertyType("relationshipEntity"));
    EnumType<?> enumType = assertInstanceOf(EnumType.class, entityType.getUserType());
    assertFalse(enumType.isOrdinal());
    assertSame(RelationshipEntity.class, enumType.returnedClass());
    assertArrayEquals(new int[] {Types.VARCHAR}, enumType.sqlTypes());

    CustomType dataViewType =
        assertInstanceOf(CustomType.class, persister.getPropertyType("trackerDataView"));
    SafeJsonBinaryType jsonType =
        assertInstanceOf(SafeJsonBinaryType.class, dataViewType.getUserType());
    assertSame(TrackerDataView.class, jsonType.returnedClass());

    for (String property : List.of("trackedEntityType", "program", "programStage")) {
      ManyToOneType type =
          assertInstanceOf(ManyToOneType.class, persister.getPropertyType(property));
      assertFalse(type.isEager(null), property);
    }

    for (String property :
        List.of(
            "relationshipEntity",
            "trackedEntityType",
            "program",
            "programStage",
            "trackerDataView")) {
      assertTrue(isNullable(persister, property), property);
    }

    assertTrue(persister.canReadFromCache());
    assertEquals(AccessType.READ_WRITE, persister.getCacheAccessStrategy().getAccessType());
  }

  @Test
  void testColumnsAreWrittenAndReadBack() {
    RelationshipConstraint constraint = new RelationshipConstraint();
    constraint.setRelationshipEntity(RelationshipEntity.PROGRAM_STAGE_INSTANCE);
    constraint.setTrackedEntityType(trackedEntityType);
    constraint.setProgram(program);
    constraint.setProgramStage(programStage);
    constraint.setTrackerDataView(dataView(List.of("attrB", "attrA"), List.of("deA")));
    entityManager.persist(constraint);
    entityManager.flush();

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            """
            select entity, trackedentitytypeid, programid, programstageid,
              dataview->'attributes' #>> '{}' as attributes,
              dataview->'dataElements' #>> '{}' as dataelements
            from relationshipconstraint where relationshipconstraintid = ?""",
            constraint.getId());
    assertEquals("PROGRAM_STAGE_INSTANCE", row.get("entity"));
    assertEquals(trackedEntityType.getId(), row.get("trackedentitytypeid"));
    assertEquals(program.getId(), row.get("programid"));
    assertEquals(programStage.getId(), row.get("programstageid"));
    assertEquals("[\"attrB\", \"attrA\"]", row.get("attributes"));
    assertEquals("[\"deA\"]", row.get("dataelements"));

    jdbcTemplate.update(
        """
        update relationshipconstraint set entity = 'PROGRAM_INSTANCE', programstageid = null,
          dataview = '{"attributes": ["attrC"], "dataElements": []}'
        where relationshipconstraintid = ?""",
        constraint.getId());

    RelationshipConstraint reloaded = reload(constraint);
    assertSame(RelationshipEntity.PROGRAM_INSTANCE, reloaded.getRelationshipEntity());
    assertEquals(trackedEntityType.getUid(), reloaded.getTrackedEntityType().getUid());
    assertEquals(program.getUid(), reloaded.getProgram().getUid());
    assertNull(reloaded.getProgramStage());
    assertEquals(List.of("attrC"), List.copyOf(reloaded.getTrackerDataView().getAttributes()));
    assertEquals(List.of(), List.copyOf(reloaded.getTrackerDataView().getDataElements()));
  }

  @Test
  void testRelationshipTypeCascadesSaveUpdateReplaceAndDeleteToItsConstraints() {
    RelationshipType type = createRelationshipType('A');
    type.getFromConstraint().setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    type.getFromConstraint().setTrackedEntityType(trackedEntityType);
    type.getToConstraint().setRelationshipEntity(RelationshipEntity.PROGRAM_INSTANCE);
    type.getToConstraint().setProgram(program);
    relationshipTypeStore.save(type);
    int fromId = type.getFromConstraint().getId();
    int toId = type.getToConstraint().getId();
    clearSessionAndSecondLevelCache();
    assertEquals(
        Map.of(fromId, "TRACKED_ENTITY_INSTANCE", toId, "PROGRAM_INSTANCE"),
        constraintRows(fromId, toId));

    RelationshipType loaded = relationshipTypeStore.get(type.getId());
    loaded.getFromConstraint().setRelationshipEntity(RelationshipEntity.PROGRAM_STAGE_INSTANCE);
    RelationshipConstraint replacement = new RelationshipConstraint();
    replacement.setRelationshipEntity(RelationshipEntity.PROGRAM_STAGE_INSTANCE);
    replacement.setProgramStage(programStage);
    loaded.setToConstraint(replacement);
    relationshipTypeStore.update(loaded);
    clearSessionAndSecondLevelCache();
    assertEquals(
        Map.of(fromId, "PROGRAM_STAGE_INSTANCE", replacement.getId(), "PROGRAM_STAGE_INSTANCE"),
        constraintRows(fromId, toId, replacement.getId()));
    assertEquals(
        Map.of(
            "from_relationshipconstraintid",
            fromId,
            "to_relationshipconstraintid",
            replacement.getId()),
        jdbcTemplate.queryForMap(
            "select from_relationshipconstraintid, to_relationshipconstraintid"
                + " from relationshiptype where relationshiptypeid = ?",
            type.getId()));

    relationshipTypeStore.delete(relationshipTypeStore.get(type.getId()));
    clearSessionAndSecondLevelCache();
    assertEquals(Map.of(), constraintRows(fromId, toId, replacement.getId()));
  }

  private static TrackerDataView dataView(List<String> attributes, List<String> dataElements) {
    return new TrackerDataView(new LinkedHashSet<>(attributes), new LinkedHashSet<>(dataElements));
  }

  /** Reloads must hit the database, not the session or the read-write second-level cache. */
  private void clearSessionAndSecondLevelCache() {
    clearSession();
    entityManagerFactory.getCache().evictAll();
  }

  private RelationshipConstraint reload(RelationshipConstraint constraint) {
    clearSessionAndSecondLevelCache();
    return entityManager.find(RelationshipConstraint.class, constraint.getId());
  }

  /** The entity column of the existing rows among the given ids, by id. */
  private Map<Integer, String> constraintRows(int... ids) {
    String idList = IntStream.of(ids).mapToObj(Integer::toString).collect(Collectors.joining(","));
    Map<Integer, String> rows = new HashMap<>();
    jdbcTemplate.query(
        "select relationshipconstraintid, entity from relationshipconstraint"
            + " where relationshipconstraintid in ("
            + idList
            + ")",
        rs -> {
          rows.put(rs.getInt("relationshipconstraintid"), rs.getString("entity"));
        });
    return rows;
  }

  private static boolean isNullable(EntityPersister persister, String property) {
    return persister
        .getPropertyNullability()[persister.getEntityMetamodel().getPropertyIndex(property)];
  }

  private EntityPersister persister() {
    return entityManagerFactory
        .unwrap(SessionFactoryImplementor.class)
        .getMetamodel()
        .entityPersister(RelationshipConstraint.class);
  }
}
