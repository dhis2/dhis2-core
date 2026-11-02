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
package org.hisp.dhis.datavalue.hibernate;

import static org.hisp.dhis.test.TestBase.injectSecurityContextNoSettings;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.hypersistence.utils.hibernate.type.array.LongArrayType;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValueAuditQueryParams;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;

class HibernateDataValueAuditStoreSqlTest {
  private Session session;
  private NativeQuery<?> query;
  private JdbcTemplate jdbcTemplate;
  private HibernateDataValueAuditStore store;
  private CategoryOptionCombo coc;
  private CategoryOptionCombo aoc;

  @BeforeEach
  void setUp() {
    EntityManager entityManager = mock(EntityManager.class);
    session = mock(Session.class);
    query = mock(NativeQuery.class, RETURNS_SELF);
    when(entityManager.unwrap(Session.class)).thenReturn(session);
    when(session.createNativeQuery(anyString())).thenReturn(query);
    when(query.getSingleResult()).thenReturn(null);
    jdbcTemplate = mock(JdbcTemplate.class);
    store =
        new HibernateDataValueAuditStore(
            entityManager,
            jdbcTemplate,
            mock(ApplicationEventPublisher.class),
            mock(PeriodStore.class),
            mock(DhisConfigurationProvider.class));
    UserDetails user = mock(UserDetails.class);
    when(user.getUid()).thenReturn("readerUid01");
    injectSecurityContextNoSettings(user);
    coc = new CategoryOptionCombo();
    coc.setId(987654321L);
    coc.setUid("comboUid001");
    aoc = new CategoryOptionCombo();
    aoc.setId(987654322L);
    aoc.setUid("comboUid002");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void pinnedCountBindsComboIds() {
    doReturn(List.of(aoc.getId())).when(query).getResultList();
    doReturn(0L).when(query).getSingleResult();
    OrganisationUnit ou = new OrganisationUnit();
    ou.setId(12L);
    // a value: with its org unit given, the user's org units are not listed
    store.countDataValueAudits(
        new DataValueAuditQueryParams()
            .setOrgUnits(List.of(ou))
            .setCategoryOptionCombo(coc)
            .setAttributeOptionCombo(aoc));
    String statement = assertBoundCombos();
    assertTrue(statement.contains("unnest(CAST(:aocAllowed AS bigint[]))"), statement);
    verify(query)
        .setParameter(
            eq("aocAllowed"), aryEq(new Long[] {aoc.getId()}), eq(LongArrayType.INSTANCE));
  }

  @Test
  void readabilityBindsComboIds() {
    doReturn(List.of(aoc.getId())).when(query).getResultList();
    doReturn(0L).when(query).getSingleResult();
    store.isReadable(new DataElement(), new OrganisationUnit(), coc, aoc);
    assertBoundCombos();
  }

  @Test
  void listedOrgUnitsReplaceTheDescendantSet() {
    // the readable AOCs, then the user's org units
    doReturn(List.of(aoc.getId()), List.of(11L, 12L, 11L)).when(query).getResultList();
    doReturn(0L).when(query).getSingleResult();
    store.countDataValueAudits(new DataValueAuditQueryParams());

    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(session, times(3)).createNativeQuery(sql.capture());
    String statement = sql.getAllValues().get(2);
    assertTrue(statement.contains("AND dva.organisationunitid = ANY(:orgUnits)"), statement);
    assertFalse(statement.contains("ou_roots"), statement);
    assertFalse(statement.contains("~>=~"), statement);
    verify(query)
        .setParameter(eq("orgUnits"), aryEq(new Long[] {11L, 12L}), eq(LongArrayType.INSTANCE));
    verify(jdbcTemplate).execute("SET LOCAL plan_cache_mode = force_custom_plan");
  }

  /**
   * The readable attribute option combo lookup and the access checked statement bind the combos
   * instead of inlining them.
   *
   * @return the access checked statement
   */
  private String assertBoundCombos() {
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(session, times(2)).createNativeQuery(sql.capture());
    String aocLookup = sql.getAllValues().get(0);
    String statement = sql.getAllValues().get(1);
    assertTrue(aocLookup.contains("AND ccoc.categoryoptioncomboid = :aoc"), aocLookup);
    assertTrue(statement.startsWith("WITH\n  ou_roots AS ("), statement);
    assertTrue(statement.contains("coc_no_access AS MATERIALIZED ("), statement);
    assertTrue(statement.contains("AND coc_co.categoryoptioncomboid = :coc"), statement);
    for (String s : sql.getAllValues()) {
      for (CategoryOptionCombo combo : new CategoryOptionCombo[] {coc, aoc}) {
        assertFalse(s.contains(Long.toString(combo.getId())), s);
        assertFalse(s.contains(combo.getUid()), s);
      }
    }
    verify(query).setParameter("coc", coc.getId());
    verify(query, atLeastOnce()).setParameter("aoc", aoc.getId());
    return statement;
  }
}
