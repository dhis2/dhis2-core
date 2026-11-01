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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.enhanced.NoopOptimizer;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.type.BooleanType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Tests the persistence mapping of {@link RelativePeriods} onto the relativeperiods table. */
@Transactional
class RelativePeriodsMappingTest extends PostgresIntegrationTestBase {

  /** The flag properties; each is stored in the column of the same name in lower case. */
  private static final List<String> FLAGS =
      List.of(
          "thisDay",
          "yesterday",
          "last3Days",
          "last7Days",
          "last14Days",
          "last30Days",
          "last60Days",
          "last90Days",
          "last180Days",
          "thisMonth",
          "lastMonth",
          "thisBimonth",
          "lastBimonth",
          "thisQuarter",
          "lastQuarter",
          "thisSixMonth",
          "lastSixMonth",
          "weeksThisYear",
          "monthsThisYear",
          "biMonthsThisYear",
          "quartersThisYear",
          "thisYear",
          "monthsLastYear",
          "quartersLastYear",
          "lastYear",
          "last5Years",
          "last10Years",
          "last12Months",
          "last6Months",
          "last3Months",
          "last6BiMonths",
          "last4Quarters",
          "last2SixMonths",
          "thisFinancialYear",
          "lastFinancialYear",
          "last5FinancialYears",
          "last10FinancialYears",
          "thisWeek",
          "lastWeek",
          "thisBiWeek",
          "lastBiWeek",
          "last4Weeks",
          "last4BiWeeks",
          "last12Weeks",
          "last52Weeks");

  /** The flag columns Flyway left nullable; every other flag column is NOT NULL. */
  private static final List<String> NULLABLE_FLAGS =
      List.of("weeksThisYear", "biMonthsThisYear", "thisBiWeek", "lastBiWeek", "last4BiWeeks");

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void shouldMapRelativePeriodsToItsTableColumnsAndSequence() {
    AbstractEntityPersister persister = persister();

    assertEquals("relativeperiods", persister.getTableName());
    assertArrayEquals(new String[] {"relativeperiodsid"}, persister.getIdentifierColumnNames());
    SequenceStyleGenerator generator =
        assertInstanceOf(SequenceStyleGenerator.class, persister.getIdentifierGenerator());
    assertEquals(
        "hibernate_sequence",
        generator.getDatabaseStructure().getPhysicalName().getObjectName().getText());
    assertEquals(1, generator.getDatabaseStructure().getIncrementSize());
    assertInstanceOf(NoopOptimizer.class, generator.getOptimizer());

    assertEquals(FLAGS.size(), persister.getPropertyNames().length);
    for (String flag : FLAGS) {
      assertEquals(flag.toLowerCase(), persister.getPropertyColumnNames(flag)[0], flag + " column");
      assertInstanceOf(BooleanType.class, persister.getPropertyType(flag), flag + " type");
    }
  }

  @Test
  void shouldMapFlagNullabilityAsInTheDatabase() {
    AbstractEntityPersister persister = persister();

    for (String flag : FLAGS) {
      boolean nullable = persister.getPropertyNullability()[persister.getPropertyIndex(flag)];
      assertEquals(NULLABLE_FLAGS.contains(flag), nullable, flag + " nullability");
    }
  }

  @Test
  void shouldCacheRelativePeriodsReadWrite() {
    AbstractEntityPersister persister = persister();

    assertTrue(persister.canWriteToCache());
    assertEquals(AccessType.READ_WRITE, persister.getCacheAccessStrategy().getAccessType());
  }

  @Test
  void shouldWriteEachFlagToItsOwnColumn() {
    AbstractEntityPersister persister = persister();

    for (String flag : FLAGS) {
      RelativePeriods relativePeriods = new RelativePeriods();
      persister.setPropertyValue(relativePeriods, persister.getPropertyIndex(flag), true);
      entityManager.persist(relativePeriods);
      entityManager.flush();

      Map<String, Object> row =
          jdbcTemplate.queryForMap(
              "select * from relativeperiods where relativeperiodsid = ?", relativePeriods.getId());
      for (String column : FLAGS) {
        assertEquals(
            column.equals(flag),
            row.get(column.toLowerCase()),
            "column " + column.toLowerCase() + " after setting " + flag);
      }
    }
  }

  @Test
  void shouldReadEachFlagFromItsOwnColumn() {
    AbstractEntityPersister persister = persister();
    String insert =
        "insert into relativeperiods (relativeperiodsid, "
            + String.join(", ", FLAGS.stream().map(String::toLowerCase).toList())
            + ") values ("
            + String.join(", ", Collections.nCopies(FLAGS.size() + 1, "?"))
            + ")";

    for (String flag : FLAGS) {
      int id = jdbcTemplate.queryForObject("select nextval('hibernate_sequence')", Integer.class);
      jdbcTemplate.update(
          insert, Stream.concat(Stream.of(id), FLAGS.stream().map(flag::equals)).toArray());

      RelativePeriods loaded = entityManager.find(RelativePeriods.class, id);

      for (String property : FLAGS) {
        assertEquals(
            property.equals(flag),
            persister.getPropertyValue(loaded, property),
            property + " after setting column " + flag.toLowerCase());
      }
    }
  }

  private AbstractEntityPersister persister() {
    return (AbstractEntityPersister)
        entityManagerFactory
            .unwrap(SessionFactoryImplementor.class)
            .getMetamodel()
            .entityPersister(RelativePeriods.class);
  }
}
