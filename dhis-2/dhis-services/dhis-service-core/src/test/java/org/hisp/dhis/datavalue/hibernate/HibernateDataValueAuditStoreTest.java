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

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.NativeQuery;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.dhis.datavalue.DataValueAuditQueryParams;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

class HibernateDataValueAuditStoreTest {
  private final Session session = mock(Session.class);
  private final NativeQuery query = mock(NativeQuery.class, RETURNS_SELF);
  private final CategoryOptionCombo coc = new CategoryOptionCombo();
  private final CategoryOptionCombo aoc = new CategoryOptionCombo();
  private HibernateDataValueAuditStore store;

  @BeforeEach
  void setUp() {
    SessionFactory sessionFactory = mock(SessionFactory.class);
    CurrentUserService currentUserService = mock(CurrentUserService.class);
    User reader = mock(User.class);
    when(currentUserService.getCurrentUser()).thenReturn(reader);
    when(sessionFactory.getCurrentSession()).thenReturn(session);
    store =
        new HibernateDataValueAuditStore(
            sessionFactory,
            mock(JdbcTemplate.class),
            mock(ApplicationEventPublisher.class),
            mock(PeriodStore.class),
            currentUserService);
    coc.setId(918273645L);
    coc.setUid("Coc91827364");
    aoc.setId(564738291L);
    aoc.setUid("Aoc56473829");
  }

  @Test
  void pinnedCountBindsComboIds() {
    when(session.createNativeQuery(anyString())).thenReturn(query);
    when(query.getSingleResult()).thenReturn(0L);
    store.countDataValueAudits(params());
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(session).createNativeQuery(sql.capture());
    assertBoundCombos(sql.getValue());
  }

  @Test
  void pinnedListBindsComboIds() {
    when(session.createNativeQuery(
            anyString(), org.mockito.ArgumentMatchers.eq(DataValueAudit.class)))
        .thenReturn(query);
    store.getDataValueAudits(params());
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(session)
        .createNativeQuery(sql.capture(), org.mockito.ArgumentMatchers.eq(DataValueAudit.class));
    assertBoundCombos(sql.getValue());
  }

  @Test
  void readableCheckBindsComboIds() {
    when(session.createNativeQuery(anyString())).thenReturn(query);
    when(query.getSingleResult()).thenReturn(0L);
    store.isReadable(new DataElement(), new OrganisationUnit(), coc, aoc);
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(session).createNativeQuery(sql.capture());
    assertBoundCombos(sql.getValue());
  }

  private DataValueAuditQueryParams params() {
    return new DataValueAuditQueryParams().setCategoryOptionCombo(coc).setAttributeOptionCombo(aoc);
  }

  private void assertBoundCombos(String sql) {
    String cte = sql.substring(0, sql.indexOf(" select", sql.indexOf("WITH coc_no_access")));
    assertTrue(cte.contains("coc_co.categoryoptioncomboid IN (:coc, :aoc)"), sql);
    assertFalse(sql.contains(Long.toString(coc.getId())), sql);
    assertFalse(sql.contains(Long.toString(aoc.getId())), sql);
    assertFalse(sql.contains(coc.getUid()), sql);
    assertFalse(sql.contains(aoc.getUid()), sql);
    verify(query).setParameter("coc", coc.getId());
    verify(query).setParameter("aoc", aoc.getId());
  }
}
