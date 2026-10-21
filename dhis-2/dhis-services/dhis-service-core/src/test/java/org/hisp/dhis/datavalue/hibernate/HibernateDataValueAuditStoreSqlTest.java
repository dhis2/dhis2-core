/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.datavalue.hibernate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.persistence.EntityManager;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class HibernateDataValueAuditStoreSqlTest {
  private Session session;
  private NativeQuery query;
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
    when(query.getSingleResult()).thenReturn(0L);
    store =
        new HibernateDataValueAuditStore(
            entityManager,
            mock(JdbcTemplate.class),
            mock(ApplicationEventPublisher.class),
            mock(PeriodStore.class));
    UserDetails reader = mock(UserDetails.class);
    when(reader.getUid()).thenReturn("readerUid01");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(reader, null, java.util.List.of()));
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
  void pinnedCountBindsComboRestriction() {
    store.countDataValueAudits(
        new DataValueAuditQueryParams().setCategoryOptionCombo(coc).setAttributeOptionCombo(aoc));
    assertComboParameters();
  }

  @Test
  void readabilityBindsComboRestriction() {
    store.isReadable(new DataElement(), new OrganisationUnit(), coc, aoc);
    assertComboParameters();
  }

  private void assertComboParameters() {
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(session).createNativeQuery(sql.capture());
    assertTrue(
        sql.getValue().contains("AND coc_co.categoryoptioncomboid IN (:coc, :aoc)"),
        sql.getValue());
    for (CategoryOptionCombo combo : java.util.List.of(coc, aoc)) {
      assertFalse(sql.getValue().contains(Long.toString(combo.getId())), sql.getValue());
      assertFalse(sql.getValue().contains(combo.getUid()), sql.getValue());
    }
    verify(query).setParameter("coc", coc.getId());
    verify(query).setParameter("aoc", aoc.getId());
  }
}
