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
package org.hisp.dhis.datavalue;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.type.BooleanType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.StringType;
import org.hibernate.type.TimestampType;
import org.hibernate.type.Type;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link DataValue} mapping: the {@code datavalue} table keyed by data element, period,
 * organisation unit, category option combo and attribute option combo.
 */
@Transactional
class DataValueMappingTest extends PostgresIntegrationTestBase {

  private static final String SELECT_ROW =
      "select * from datavalue where dataelementid = ? and periodid = ? and sourceid = ?"
          + " and categoryoptioncomboid = ? and attributeoptioncomboid = ?";

  @Autowired private DataValueStore dataValueStore;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private PeriodService periodService;

  @Autowired private PeriodStore periodStore;

  @Autowired private CategoryService categoryService;

  @Autowired private JdbcTemplate jdbcTemplate;

  private AbstractEntityPersister persister;

  private DataElement dataElement;

  private Period period;

  private OrganisationUnit sourceA;

  private OrganisationUnit sourceB;

  private CategoryOptionCombo optionCombo;

  @BeforeEach
  void setUp() {
    persister =
        (AbstractEntityPersister)
            entityManager
                .getEntityManagerFactory()
                .unwrap(SessionFactoryImplementor.class)
                .getMetamodel()
                .entityPersister(DataValue.class);

    periodStore.invalidateCache();
    dataElement = createDataElement('A');
    manager.save(dataElement);
    sourceA = createOrganisationUnit('A');
    sourceB = createOrganisationUnit('B');
    manager.save(List.of(sourceA, sourceB));
    period = createPeriod(new MonthlyPeriodType(), getDate(2000, 1, 1), getDate(2000, 1, 31));
    periodService.addPeriod(period);
    optionCombo = categoryService.getDefaultCategoryOptionCombo();
    entityManager.flush();
  }

  @Test
  void shouldMapTableAndCompositeIdOfFiveLazyReferences() {
    assertEquals("datavalue", persister.getTableName());
    assertTrue(persister.getEntityMetamodel().getIdentifierProperty().isEmbedded());
    Map<String, String> columns =
        Map.of(
            "dataElement", "dataelementid",
            "period", "periodid",
            "source", "sourceid",
            "categoryOptionCombo", "categoryoptioncomboid",
            "attributeOptionCombo", "attributeoptioncomboid");
    Map<String, Class<?>> targets =
        Map.of(
            "dataElement", DataElement.class,
            "period", Period.class,
            "source", OrganisationUnit.class,
            "categoryOptionCombo", CategoryOptionCombo.class,
            "attributeOptionCombo", CategoryOptionCombo.class);

    ComponentType id = assertInstanceOf(ComponentType.class, persister.getIdentifierType());
    String[] properties = id.getPropertyNames();
    assertEquals(columns.keySet(), Set.of(properties));
    for (int i = 0; i < properties.length; i++) {
      String property = properties[i];
      assertEquals(columns.get(property), persister.getIdentifierColumnNames()[i], property);
      ManyToOneType reference = assertInstanceOf(ManyToOneType.class, id.getSubtypes()[i]);
      assertEquals(targets.get(property).getName(), reference.getAssociatedEntityName());
      assertFalse(reference.isEager(null), property);
    }
  }

  @Test
  void shouldMapPropertiesToColumnsTypesAndNullability() {
    Map<String, Class<? extends Type>> types =
        Map.of(
            "value", StringType.class,
            "storedBy", StringType.class,
            "created", TimestampType.class,
            "lastUpdated", TimestampType.class,
            "comment", StringType.class,
            "followup", BooleanType.class,
            "deleted", BooleanType.class);
    Map<String, String> columns =
        Map.of(
            "value", "value",
            "storedBy", "storedby",
            "created", "created",
            "lastUpdated", "lastupdated",
            "comment", "comment",
            "followup", "followup",
            "deleted", "deleted");
    Set<String> notNull = Set.of("created", "lastUpdated", "deleted");

    assertEquals(types.keySet(), Set.of(persister.getPropertyNames()));
    for (String property : persister.getPropertyNames()) {
      assertArrayEquals(
          new String[] {columns.get(property)}, persister.getPropertyColumnNames(property));
      assertInstanceOf(types.get(property), persister.getPropertyType(property), property);
      int index = persister.getEntityMetamodel().getPropertyIndex(property);
      assertEquals(
          !notNull.contains(property), persister.getPropertyNullability()[index], property);
    }
  }

  @Test
  void shouldWriteEveryColumnOnPersist() {
    DataValue dataValue =
        DataValue.builder()
            .dataElement(dataElement)
            .period(period)
            .source(sourceA)
            .categoryOptionCombo(optionCombo)
            .attributeOptionCombo(optionCombo)
            .value("42")
            .storedBy("storer")
            .lastUpdated(getDate(2000, 2, 3))
            .comment("a comment")
            .followup(true)
            .deleted(true)
            .build();
    dataValue.setCreated(getDate(2000, 2, 1));
    entityManager.persist(dataValue);
    entityManager.flush();

    Map<String, Object> row = selectRow(sourceA);
    assertEquals("42", row.get("value"));
    assertEquals("storer", row.get("storedby"));
    assertEquals(getDate(2000, 2, 1), row.get("created"));
    assertEquals(getDate(2000, 2, 3), row.get("lastupdated"));
    assertEquals("a comment", row.get("comment"));
    assertEquals(Boolean.TRUE, row.get("followup"));
    assertEquals(Boolean.TRUE, row.get("deleted"));
  }

  @Test
  void shouldWriteUnsetFollowupAsFalse() {
    DataValue dataValue = new DataValue(dataElement, period, sourceA, optionCombo, optionCombo);
    entityManager.persist(dataValue);
    entityManager.flush();

    assertEquals(Boolean.FALSE, selectRow(sourceA).get("followup"));
  }

  @Test
  void shouldLoadEveryColumnAndKeepLoadedValueAsAuditValue() {
    jdbcTemplate.update(
        "insert into datavalue (dataelementid, periodid, sourceid, categoryoptioncomboid,"
            + " attributeoptioncomboid, value, storedby, created, lastupdated, comment, followup,"
            + " deleted) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        dataElement.getId(),
        period.getId(),
        sourceA.getId(),
        optionCombo.getId(),
        optionCombo.getId(),
        "17",
        "storer",
        getDate(2001, 1, 2),
        getDate(2001, 3, 4),
        "a comment",
        true,
        false);
    entityManager.clear();

    DataValue dataValue =
        entityManager.find(
            DataValue.class, new DataValue(dataElement, period, sourceA, optionCombo, optionCombo));

    assertNotNull(dataValue);
    assertEquals(dataElement.getUid(), dataValue.getDataElement().getUid());
    assertEquals(period.getIsoDate(), dataValue.getPeriod().getIsoDate());
    assertEquals(sourceA.getUid(), dataValue.getSource().getUid());
    assertEquals(optionCombo.getUid(), dataValue.getCategoryOptionCombo().getUid());
    assertEquals(optionCombo.getUid(), dataValue.getAttributeOptionCombo().getUid());
    assertEquals("17", dataValue.getValue());
    assertEquals("storer", dataValue.getStoredBy());
    assertEquals(getDate(2001, 1, 2), dataValue.getCreated());
    assertEquals(getDate(2001, 3, 4), dataValue.getLastUpdated());
    assertEquals("a comment", dataValue.getComment());
    assertTrue(dataValue.isFollowup());
    assertFalse(dataValue.isDeleted());

    dataValue.setValue("18");
    assertEquals("17", dataValue.getAuditValue());
  }

  @Test
  void shouldCountCheckAndDeleteThroughStoreQueries() {
    Date updated = getDate(2000, 6, 1);
    persist(sourceA, updated, false);
    persist(sourceB, updated, true);
    entityManager.flush();

    assertTrue(dataValueStore.dataValueExistsForDataElement(dataElement.getUid()));
    assertEquals(1, dataValueStore.getDataValueCountLastUpdatedBetween(updated, null, false));
    assertEquals(2, dataValueStore.getDataValueCountLastUpdatedBetween(null, updated, true));

    dataValueStore.deleteDataValues(sourceA);
    assertEquals(0, dataValueStore.getDataValueCountLastUpdatedBetween(updated, null, false));
    assertEquals(1, dataValueStore.getDataValueCountLastUpdatedBetween(updated, null, true));

    dataValueStore.deleteDataValuesByCategoryOptionCombo(List.of(optionCombo));
    assertFalse(dataValueStore.dataValueExistsForDataElement(dataElement.getUid()));
  }

  private void persist(OrganisationUnit source, Date lastUpdated, boolean deleted) {
    entityManager.persist(
        DataValue.builder()
            .dataElement(dataElement)
            .period(period)
            .source(source)
            .categoryOptionCombo(optionCombo)
            .attributeOptionCombo(optionCombo)
            .value("1")
            .lastUpdated(lastUpdated)
            .deleted(deleted)
            .build());
  }

  private Map<String, Object> selectRow(OrganisationUnit source) {
    return jdbcTemplate.queryForMap(
        SELECT_ROW,
        dataElement.getId(),
        period.getId(),
        source.getId(),
        optionCombo.getId(),
        optionCombo.getId());
  }
}
