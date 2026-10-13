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
package org.hisp.dhis.period;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.util.Iterator;
import java.util.List;
import org.hibernate.FetchMode;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.enhanced.NoopOptimizer;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.ManyToOneType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Torgeir Lorange Ostby
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class PeriodStoreTest extends PostgresIntegrationTestBase {
  @Autowired private PeriodStore periodStore;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private EntityManagerFactory entityManagerFactory;

  /** The store caches ids by ISO period; the rows of each test are rolled back. */
  @AfterEach
  void invalidatePeriodIdCache() {
    periodStore.invalidateCache();
  }

  // -------------------------------------------------------------------------
  // Period
  // -------------------------------------------------------------------------
  @Test
  void testAddPeriod() {
    List<PeriodType> periodTypes = periodStore.getAllPeriodTypes();
    Iterator<PeriodType> it = periodTypes.iterator();
    PeriodType periodTypeA = it.next();
    PeriodType periodTypeB = it.next();
    PeriodType periodTypeC = it.next();
    Period periodA = new Period(periodTypeA, getDay(1), getDay(2));
    Period periodB = new Period(periodTypeB, getDay(2), getDay(3));
    Period periodC = new Period(periodTypeC, getDay(2), getDay(3));
    periodStore.addPeriod(periodA);
    long idA = periodA.getId();
    periodStore.addPeriod(periodB);
    long idB = periodB.getId();
    periodStore.addPeriod(periodC);
    long idC = periodC.getId();
    periodA = periodStore.get(idA);
    assertNotNull(periodA);
    assertEquals(idA, periodA.getId());
    assertEquals(periodTypeA, periodA.getPeriodType());
    assertEquals(getDay(1), periodA.getStartDate());
    assertEquals(getDay(2), periodA.getEndDate());
    periodB = periodStore.get(idB);
    assertNotNull(periodB);
    assertEquals(idB, periodB.getId());
    assertEquals(periodTypeB, periodB.getPeriodType());
    assertEquals(getDay(2), periodB.getStartDate());
    assertEquals(getDay(3), periodB.getEndDate());
    periodC = periodStore.get(idC);
    assertNotNull(periodC);
    assertEquals(idC, periodC.getId());
    assertEquals(periodTypeC, periodC.getPeriodType());
    assertEquals(getDay(2), periodC.getStartDate());
    assertEquals(getDay(3), periodC.getEndDate());
  }

  @Test
  void testGetAllPeriods() {
    List<PeriodType> periodTypes = periodStore.getAllPeriodTypes();
    Iterator<PeriodType> it = periodTypes.iterator();
    PeriodType periodTypeA = it.next();
    PeriodType periodTypeB = it.next();
    PeriodType periodTypeC = it.next();
    Period periodA = new Period(periodTypeA, getDay(1), getDay(1));
    Period periodB = new Period(periodTypeB, getDay(8), getDay(8));
    Period periodC = new Period(periodTypeC, getDay(10), getDay(10));
    periodStore.addPeriod(periodA);
    periodStore.addPeriod(periodB);
    periodStore.addPeriod(periodC);

    List<Period> periods = periodStore.getAll();

    assertNotNull(periods);
    assertEquals(3, periods.size());
    assertTrue(periods.contains(periodA));
    assertTrue(periods.contains(periodB));
    assertTrue(periods.contains(periodC));
  }

  // -------------------------------------------------------------------------
  // Mapping
  // -------------------------------------------------------------------------

  @Test
  @DisplayName(
      "the id is generated by a sequence generator on hibernate_sequence, one nextval per id")
  void testIdGeneratorUsesHibernateSequence() {
    SequenceStyleGenerator generator =
        assertInstanceOf(SequenceStyleGenerator.class, persister().getIdentifierGenerator());

    assertEquals(
        "hibernate_sequence",
        generator.getDatabaseStructure().getPhysicalName().getObjectName().getText());
    assertEquals(1, generator.getDatabaseStructure().getIncrementSize());
    assertInstanceOf(NoopOptimizer.class, generator.getOptimizer());
  }

  @Test
  @DisplayName("period type, start date and end date are read back from their columns")
  void testColumnsRoundTrip() {
    Period saved = add("202101");

    assertEquals(
        List.of("Monthly", "2021-01-01", "2021-01-31", "202101"),
        jdbcTemplate.queryForObject(
            """
            select pt.name, p.startdate, p.enddate, p.iso
            from period p join periodtype pt on pt.periodtypeid = p.periodtypeid
            where p.periodid = ?""",
            (rs, row) ->
                List.of(
                    rs.getString("name"),
                    rs.getString("startdate"),
                    rs.getString("enddate"),
                    rs.getString("iso")),
            saved.getId()));

    Period reloaded = reload(saved);
    assertEquals(saved.getId(), reloaded.getId());
    assertEquals("Monthly", reloaded.getPeriodType().getName());
    assertEquals("2021-01-01", reloaded.getStartDateString());
    assertEquals("2021-01-31", reloaded.getEndDateString());
    assertEquals("202101", reloaded.getIsoDate());
  }

  @Test
  @DisplayName("start and end date are bound as SQL date, not as timestamp")
  void testDatesAreBoundAsSqlDate() {
    Period reloaded = reload(add("202102"));
    assertSame(java.sql.Date.class, reloaded.getStartDate().getClass());
    assertSame(java.sql.Date.class, reloaded.getEndDate().getClass());
  }

  @Test
  @DisplayName("start date, end date and iso are mapped not-null and the period type nullable")
  void testNullability() {
    AbstractEntityPersister persister = persister();
    boolean[] nullability = persister.getPropertyNullability();

    assertFalse(nullability[index("startDate")]);
    assertFalse(nullability[index("endDate")]);
    assertFalse(nullability[index("isoDate")]);
    assertTrue(nullability[index("periodType")]);
  }

  @Test
  @DisplayName("iso is generated by the database on insert and update and excluded from locking")
  void testIsoIsDatabaseGenerated() {
    AbstractEntityPersister persister = persister();
    int iso = index("isoDate");

    assertFalse(persister.getPropertyInsertability()[iso]);
    assertFalse(persister.getPropertyUpdateability()[iso]);
    assertFalse(persister.getPropertyVersionability()[iso]);
    assertEquals(
        GenerationTiming.ALWAYS,
        persister
            .getEntityMetamodel()
            .getInDatabaseValueGenerationStrategies()[iso]
            .getGenerationTiming());
  }

  @Test
  @DisplayName("a loaded period derives its ISO value from type and dates, not from the iso column")
  void testLoadedIsoDerivesFromTypeAndDates() {
    Period saved = add("202103");
    jdbcTemplate.update("update period set iso = 'stale' where periodid = ?", saved.getId());

    assertEquals("202103", reload(saved).getIsoDate());
  }

  @Test
  @DisplayName("the period type is fetched eagerly by a separate select")
  void testPeriodTypeIsFetchedEagerlyBySelect() {
    AbstractEntityPersister persister = persister();
    ManyToOneType type =
        assertInstanceOf(ManyToOneType.class, persister.getPropertyType("periodType"));

    assertTrue(type.isEager(null));
    assertEquals(FetchMode.SELECT, persister.getFetchMode(index("periodType")));
    assertSame(MonthlyPeriodType.class, reload(add("202104")).getPeriodType().getClass());
  }

  @Test
  @DisplayName("the entity is immutable: changes to a loaded period are not written")
  void testImmutable() {
    Period saved = add("202105");
    Period loaded = reload(saved);
    loaded.setEndDate(getDate(2021, 6, 30));
    entityManager.flush();

    assertEquals(
        "2021-05-31",
        jdbcTemplate.queryForObject(
            "select enddate from period where periodid = ?", String.class, saved.getId()));
  }

  @Test
  @DisplayName("the entity is second-level cached with the read-write strategy")
  void testReadWriteEntityCache() {
    AbstractEntityPersister persister = persister();

    assertTrue(persister.canReadFromCache());
    assertEquals(AccessType.READ_WRITE, persister.getCacheAccessStrategy().getAccessType());
  }

  // -------------------------------------------------------------------------
  // Store lookups (the store's native SQL against the mapped columns)
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("adding a period that is already stored links it to the stored row")
  void testAddExistingPeriodLinksStoredRow() {
    Period first = add("202106");
    periodStore.invalidateCache();
    Period second = add("202106");

    assertEquals(first.getId(), second.getId());
    assertEquals(
        1,
        jdbcTemplate.queryForObject(
            "select count(*) from period where iso = '202106'", Integer.class));
  }

  @Test
  @DisplayName("reloading links a transient period to its stored id, or returns null if not stored")
  void testReloadPeriod() {
    Period saved = add("202107");
    periodStore.invalidateCache();

    Period transientPeriod = Period.of("202107");
    assertSame(transientPeriod, periodStore.reloadPeriod(transientPeriod));
    assertEquals(saved.getId(), transientPeriod.getId());
    assertNull(periodStore.reloadPeriod(Period.of("199001")));
  }

  @Test
  @DisplayName("a period is found by its start date and period type")
  void testGetPeriodFromDates() {
    Period saved = add("202108");
    periodStore.invalidateCache();

    Period found = periodStore.getPeriodFromDates(getDate(2021, 8, 1), new MonthlyPeriodType());
    assertEquals(saved.getId(), found.getId());
    assertEquals("202108", found.getIsoDate());
  }

  @Test
  @DisplayName("periods between dates of a period type are mapped from the stored rows")
  void testGetPeriodsBetweenDatesOfType() {
    Period saved = add("202109");
    add("2021Q3");
    clearSessionAndCache();

    List<Period> periods =
        periodStore.getPeriodsBetweenDates(
            new MonthlyPeriodType(), getDate(2021, 9, 1), getDate(2021, 9, 30));

    assertEquals(1, periods.size());
    Period period = periods.get(0);
    assertEquals(saved.getId(), period.getId());
    assertSame(MonthlyPeriodType.class, period.getPeriodType().getClass());
    assertEquals("2021-09-01", period.getStartDateString());
    assertEquals("2021-09-30", period.getEndDateString());
    assertEquals("202109", period.getIsoDate());
  }

  @Test
  @DisplayName("intersecting periods are mapped from the stored rows")
  void testGetIntersectingPeriods() {
    Period month = add("202110");
    Period quarter = add("2021Q4");
    add("202112");
    clearSessionAndCache();

    List<Period> periods =
        periodStore.getIntersectingPeriods(getDate(2021, 10, 15), getDate(2021, 11, 15));

    assertEquals(
        List.of(month.getId(), quarter.getId()),
        periods.stream().map(Period::getId).sorted().toList());
    assertTrue(periods.contains(Period.of("2021Q4")));
  }

  private Period add(String isoPeriod) {
    Period period = Period.of(isoPeriod);
    periodStore.addPeriod(period);
    return period;
  }

  private Period reload(Period period) {
    clearSessionAndCache();
    return entityManager.find(Period.class, period.getId());
  }

  private void clearSessionAndCache() {
    clearSession();
    entityManagerFactory.getCache().evict(Period.class);
  }

  private AbstractEntityPersister persister() {
    return (AbstractEntityPersister)
        entityManagerFactory
            .unwrap(SessionFactoryImplementor.class)
            .getMetamodel()
            .entityPersister(Period.class);
  }

  private int index(String property) {
    EntityMetamodel metamodel = persister().getEntityMetamodel();
    return metamodel.getPropertyIndex(property);
  }
}
