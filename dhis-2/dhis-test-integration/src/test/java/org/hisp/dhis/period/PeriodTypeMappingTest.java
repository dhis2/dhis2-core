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
package org.hisp.dhis.period;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hibernate.Hibernate;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.enhanced.NoopOptimizer;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.type.IntegerType;
import org.hibernate.type.StringType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link PeriodType} mapping: one {@code periodtype} table for the whole hierarchy, with
 * the concrete subclass stored in the {@code name} discriminator column.
 */
@Transactional
class PeriodTypeMappingTest extends PostgresIntegrationTestBase {

  @Autowired private PeriodTypeStore periodTypeStore;

  @Autowired private PeriodStore periodStore;

  @Autowired private JdbcTemplate jdbcTemplate;

  private MetamodelImplementor metamodel;

  private SingleTableEntityPersister persister;

  @BeforeEach
  void setUp() {
    metamodel =
        entityManager
            .getEntityManagerFactory()
            .unwrap(SessionFactoryImplementor.class)
            .getMetamodel();
    persister = (SingleTableEntityPersister) metamodel.entityPersister(PeriodType.class);
  }

  @Test
  void shouldMapTableAndIdFromHibernateSequence() {
    assertEquals("periodtype", persister.getTableName());
    assertArrayEquals(new String[] {"periodtypeid"}, persister.getIdentifierColumnNames());
    assertInstanceOf(IntegerType.class, persister.getIdentifierType());

    SequenceStyleGenerator generator =
        assertInstanceOf(SequenceStyleGenerator.class, persister.getIdentifierGenerator());
    assertEquals("hibernate_sequence", generator.getDatabaseStructure().getPhysicalName().render());
    assertEquals(1, generator.getDatabaseStructure().getIncrementSize());
    assertInstanceOf(NoopOptimizer.class, generator.getOptimizer());
  }

  @Test
  void shouldMapNoPropertiesBesidesIdAndDiscriminator() {
    assertArrayEquals(new String[0], persister.getPropertyNames());
  }

  @Test
  void shouldMapEverySubclassToNameDiscriminatorInSameTable() {
    assertEquals("name", persister.getDiscriminatorColumnName());
    assertInstanceOf(StringType.class, persister.getDiscriminatorType());

    for (PeriodType periodType : PeriodType.getAvailablePeriodTypes()) {
      SingleTableEntityPersister subclass =
          (SingleTableEntityPersister) metamodel.entityPersister(periodType.getClass());

      assertEquals("periodtype", subclass.getTableName());
      assertEquals(PeriodType.class.getName(), subclass.getRootEntityName());
      assertEquals(periodType.getName(), subclass.getDiscriminatorValue());
    }
    assertEquals(
        PeriodType.getAvailablePeriodTypes().size() + 1,
        persister.getEntityMetamodel().getSubclassEntityNames().size());
  }

  @Test
  void shouldCacheWithNonstrictReadWriteInPeriodTypeRegion() {
    assertTrue(persister.canReadFromCache());
    assertEquals(
        PeriodType.class.getName(), persister.getCacheAccessStrategy().getRegion().getName());
    assertEquals(
        AccessType.NONSTRICT_READ_WRITE, persister.getCacheAccessStrategy().getAccessType());
  }

  @Test
  void shouldLoadEachStoredRowAsItsSubclass() {
    List<PeriodType> periodTypes = periodTypeStore.getAll();

    assertEquals(PeriodType.getAvailablePeriodTypes().size(), periodTypes.size());
    for (PeriodType periodType : PeriodType.getAvailablePeriodTypes()) {
      int id = idOf(periodType.getName());
      entityManager.clear();

      PeriodType loaded = entityManager.find(PeriodType.class, id);

      assertEquals(periodType.getClass(), loaded.getClass());
      assertEquals(id, loaded.getId());
      assertTrue(periodTypes.contains(loaded));
    }
  }

  @Test
  void shouldWriteSubclassAsDiscriminatorValue() {
    int storedId = idOf("FinancialNov");
    jdbcTemplate.update(
        "update periodtype set name = 'FinancialNovOld' where periodtypeid = ?", storedId);
    FinancialNovemberPeriodType periodType = new FinancialNovemberPeriodType();
    entityManager.persist(periodType);
    entityManager.flush();

    assertNotEquals(0, periodType.getId());
    assertNotEquals(storedId, periodType.getId());
    assertEquals(periodType.getId(), idOf("FinancialNov"));
  }

  @Test
  void shouldSelectDiscriminatorAsPeriodTypeClassInHql() {
    int id = idOf("QuarterlyNov");

    Object type =
        entityManager
            .createQuery("select pt.class from PeriodType pt where pt.id = :id")
            .setParameter("id", id)
            .getSingleResult();

    assertEquals("QuarterlyNov", type);
  }

  @Test
  void shouldResolveLazyReferenceToSubclass() {
    int id = idOf("WeeklyWednesday");
    entityManager.clear();

    PeriodType reference = entityManager.getReference(PeriodType.class, id);

    assertFalse(Hibernate.isInitialized(reference));
    assertEquals("WeeklyWednesday", reference.getName());
    assertEquals(new WeeklyWednesdayPeriodType(), reference);
    assertInstanceOf(WeeklyWednesdayPeriodType.class, Hibernate.unproxy(reference));
  }

  @Test
  void shouldLoadPeriodTypeOfStoredPeriod() {
    Period period = Period.of("2021July");
    periodStore.addPeriod(period);
    entityManager.clear();

    Period loaded = periodStore.get(period.getId());

    assertNotNull(loaded);
    assertInstanceOf(FinancialJulyPeriodType.class, Hibernate.unproxy(loaded.getPeriodType()));
    assertEquals(idOf("FinancialJuly"), loaded.getPeriodType().getId());
  }

  private int idOf(String name) {
    return jdbcTemplate.queryForObject(
        "select periodtypeid from periodtype where name = ?", Integer.class, name);
  }
}
