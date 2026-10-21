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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValueAuditQueryParams;
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
    store =
        new HibernateDataValueAuditStore(
            entityManager,
            mock(JdbcTemplate.class),
            mock(ApplicationEventPublisher.class),
            mock(PeriodStore.class));
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
    org.mockito.Mockito.doReturn(0L).when(query).getSingleResult();
    store.countDataValueAudits(
        new DataValueAuditQueryParams().setCategoryOptionCombo(coc).setAttributeOptionCombo(aoc));
    assertBoundCombos();
  }

  @Test
  void readabilityBindsComboIds() {
    org.mockito.Mockito.doReturn(0L).when(query).getSingleResult();
    store.isReadable(new DataElement(), new OrganisationUnit(), coc, aoc);
    assertBoundCombos();
  }

  private void assertBoundCombos() {
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(session).createNativeQuery(sql.capture());
    String statement = sql.getValue();
    assertTrue(statement.startsWith("WITH coc_no_access AS MATERIALIZED ("), statement);
    assertTrue(statement.contains("AND coc_co.categoryoptioncomboid IN (:coc, :aoc)"), statement);
    for (CategoryOptionCombo combo : new CategoryOptionCombo[] {coc, aoc}) {
      assertFalse(statement.contains(Long.toString(combo.getId())), statement);
      assertFalse(statement.contains(combo.getUid()), statement);
    }
    verify(query).setParameter("coc", coc.getId());
    verify(query).setParameter("aoc", aoc.getId());
  }
}
