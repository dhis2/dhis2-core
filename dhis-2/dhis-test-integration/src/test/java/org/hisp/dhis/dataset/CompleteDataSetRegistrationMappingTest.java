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
package org.hisp.dhis.dataset;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * Tests the {@link CompleteDataSetRegistration} mapping: the {@code completedatasetregistration}
 * table keyed by data set, period, organisation unit and attribute option combo.
 */
@Transactional
class CompleteDataSetRegistrationMappingTest extends PostgresIntegrationTestBase {

  @Autowired private CompleteDataSetRegistrationStore registrationStore;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private PeriodService periodService;

  @Autowired private DataSetService dataSetService;

  @Autowired private PeriodStore periodStore;

  @Autowired private CategoryService categoryService;

  @Autowired private JdbcTemplate jdbcTemplate;

  private AbstractEntityPersister persister;

  private DataSet dataSet;

  private Period period;

  private OrganisationUnit sourceA;

  private OrganisationUnit sourceB;

  private CategoryOptionCombo attributeOptionCombo;

  @BeforeEach
  void setUp() {
    persister =
        (AbstractEntityPersister)
            entityManager
                .getEntityManagerFactory()
                .unwrap(SessionFactoryImplementor.class)
                .getMetamodel()
                .entityPersister(CompleteDataSetRegistration.class);

    periodStore.invalidateCache();
    sourceA = createOrganisationUnit('A');
    sourceB = createOrganisationUnit('B');
    manager.save(List.of(sourceA, sourceB));
    period = createPeriod(new MonthlyPeriodType(), getDate(2000, 1, 1), getDate(2000, 1, 31));
    periodService.addPeriod(period);
    dataSet = createDataSet('A', new MonthlyPeriodType());
    dataSetService.addDataSet(dataSet);
    attributeOptionCombo = categoryService.getDefaultCategoryOptionCombo();
    entityManager.flush();
  }

  @Test
  void shouldMapTableAndCompositeIdOfFourLazyReferences() {
    assertEquals("completedatasetregistration", persister.getTableName());
    assertTrue(persister.getEntityMetamodel().getIdentifierProperty().isEmbedded());
    Map<String, String> columns =
        Map.of(
            "dataSet", "datasetid",
            "period", "periodid",
            "source", "sourceid",
            "attributeOptionCombo", "attributeoptioncomboid");
    Map<String, Class<?>> targets =
        Map.of(
            "dataSet", DataSet.class,
            "period", Period.class,
            "source", OrganisationUnit.class,
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
    Map<String, Class<? extends Type>> expected =
        Map.of(
            "date", TimestampType.class,
            "storedBy", StringType.class,
            "lastUpdated", TimestampType.class,
            "lastUpdatedBy", StringType.class,
            "completed", BooleanType.class);
    Map<String, String> columns =
        Map.of(
            "date", "date",
            "storedBy", "storedby",
            "lastUpdated", "lastupdated",
            "lastUpdatedBy", "lastupdatedby",
            "completed", "completed");

    assertEquals(expected.keySet(), Set.of(persister.getPropertyNames()));
    for (String property : persister.getPropertyNames()) {
      assertArrayEquals(
          new String[] {columns.get(property)}, persister.getPropertyColumnNames(property));
      assertInstanceOf(expected.get(property), persister.getPropertyType(property), property);
      int index = persister.getEntityMetamodel().getPropertyIndex(property);
      assertEquals(
          !"completed".equals(property), persister.getPropertyNullability()[index], property);
    }
  }

  @Test
  void shouldWriteEveryColumnOnSave() {
    CompleteDataSetRegistration registration =
        new CompleteDataSetRegistration(
            dataSet,
            period,
            sourceA,
            attributeOptionCombo,
            getDate(2000, 2, 3),
            "storer",
            null,
            "updater",
            false);
    registrationStore.saveCompleteDataSetRegistration(registration);
    entityManager.flush();

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select * from completedatasetregistration where datasetid = ? and periodid = ?"
                + " and sourceid = ? and attributeoptioncomboid = ?",
            dataSet.getId(),
            period.getId(),
            sourceA.getId(),
            attributeOptionCombo.getId());

    assertEquals(getDate(2000, 2, 3), row.get("date"));
    assertEquals("storer", row.get("storedby"));
    assertEquals(registration.getLastUpdated(), row.get("lastupdated"));
    assertEquals("updater", row.get("lastupdatedby"));
    assertEquals(false, row.get("completed"));
  }

  @Test
  void shouldLoadEveryColumnIncludingIdColumns() {
    jdbcTemplate.update(
        "insert into completedatasetregistration (datasetid, periodid, sourceid,"
            + " attributeoptioncomboid, date, storedby, lastupdated, lastupdatedby, completed)"
            + " values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        dataSet.getId(),
        period.getId(),
        sourceA.getId(),
        attributeOptionCombo.getId(),
        getDate(2001, 1, 2),
        "storer",
        getDate(2001, 3, 4),
        "updater",
        true);

    entityManager.clear();

    List<CompleteDataSetRegistration> registrations =
        registrationStore.getAllCompleteDataSetRegistrations();

    assertEquals(1, registrations.size());
    CompleteDataSetRegistration registration = registrations.get(0);
    assertEquals(dataSet.getUid(), registration.getDataSet().getUid());
    assertEquals(period.getIsoDate(), registration.getPeriod().getIsoDate());
    assertEquals(sourceA.getUid(), registration.getSource().getUid());
    assertEquals(attributeOptionCombo.getUid(), registration.getAttributeOptionCombo().getUid());
    assertEquals(getDate(2001, 1, 2), registration.getDate());
    assertEquals("storer", registration.getStoredBy());
    assertEquals(getDate(2001, 3, 4), registration.getLastUpdated());
    assertEquals("updater", registration.getLastUpdatedBy());
    assertTrue(registration.getCompleted());
  }

  @Test
  void shouldFindByCompositeIdCountDistinctAndDeleteBySource() {
    registrationStore.saveCompleteDataSetRegistration(
        new CompleteDataSetRegistration(dataSet, period, sourceA, attributeOptionCombo, true));
    registrationStore.saveCompleteDataSetRegistration(
        new CompleteDataSetRegistration(dataSet, period, sourceB, attributeOptionCombo, false));
    entityManager.flush();
    entityManager.clear();

    CompleteDataSetRegistration found =
        registrationStore.getCompleteDataSetRegistration(
            dataSet, period, sourceB, attributeOptionCombo);
    assertNotNull(found);
    assertEquals(sourceB.getUid(), found.getSource().getUid());
    assertFalse(found.getCompleted());
    assertEquals(2, registrationStore.getCompleteDataSetCountLastUpdatedAfter(getDate(2000, 1, 1)));

    registrationStore.deleteCompleteDataSetRegistrations(sourceA);

    assertNull(
        registrationStore.getCompleteDataSetRegistration(
            dataSet, period, sourceA, attributeOptionCombo));
    assertEquals(1, registrationStore.getCompleteDataSetCountLastUpdatedAfter(getDate(2000, 1, 1)));
  }
}
