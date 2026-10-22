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

import com.vladmihalcea.hibernate.type.array.LongArrayType;
import java.util.ArrayList;
import java.util.List;
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

class HibernateDataValueAuditStoreSqlTest {
  private final Session session = mock(Session.class);
  private final NativeQuery<?> query = mock(NativeQuery.class, RETURNS_SELF);
  private final CategoryOptionCombo coc = new CategoryOptionCombo();
  private final CategoryOptionCombo aoc = new CategoryOptionCombo();
  private HibernateDataValueAuditStore store;

  @BeforeEach
  void setUp() {
    SessionFactory sessionFactory = mock(SessionFactory.class);
    CurrentUserService currentUserService = mock(CurrentUserService.class);
    User reader = mock(User.class);
    when(reader.getUid()).thenReturn("readerUid01");
    when(currentUserService.getCurrentUser()).thenReturn(reader);
    when(sessionFactory.getCurrentSession()).thenReturn(session);
    when(session.createNativeQuery(anyString())).thenReturn(query);
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
    // the readable attribute option combos
    doReturn(new ArrayList<>(List.of(aoc.getId()))).when(query).getResultList();
  }

  @Test
  void pinnedCountBindsComboIds() {
    doReturn(0L).when(query).getSingleResult();
    store.countDataValueAudits(params());
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(session, times(2)).createNativeQuery(sql.capture());
    assertAocLookup(sql.getAllValues().get(0));
    assertAocListBound(assertBoundCombos(sql.getAllValues().get(1)));
  }

  @Test
  void pinnedListBindsComboIds() {
    doReturn(query).when(session).createNativeQuery(anyString(), eq(DataValueAudit.class));
    store.getDataValueAudits(params());
    ArgumentCaptor<String> lookup = ArgumentCaptor.forClass(String.class);
    verify(session).createNativeQuery(lookup.capture());
    assertAocLookup(lookup.getValue());
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(session).createNativeQuery(sql.capture(), eq(DataValueAudit.class));
    assertAocListBound(assertBoundCombos(sql.getValue()));
  }

  @Test
  void readableCheckBindsComboIds() {
    doReturn(0L).when(query).getSingleResult();
    store.isReadable(new DataElement(), new OrganisationUnit(), coc, aoc);
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(session, times(2)).createNativeQuery(sql.capture());
    assertAocLookup(sql.getAllValues().get(0));
    assertBoundCombos(sql.getAllValues().get(1));
  }

  private DataValueAuditQueryParams params() {
    return new DataValueAuditQueryParams().setCategoryOptionCombo(coc).setAttributeOptionCombo(aoc);
  }

  /** The readable attribute option combo lookup binds the attribute option combo. */
  private void assertAocLookup(String sql) {
    assertTrue(sql.contains("AND ccoc.categoryoptioncomboid = :aoc"), sql);
    assertTrue(sql.contains("JOIN dataelementcategoryoption co"), sql);
    assertNoComboInlined(sql);
    verify(query, atLeastOnce()).setParameter("aoc", aoc.getId());
  }

  /**
   * The access checked statement binds the category option combo instead of inlining the combos.
   *
   * @return the statement
   */
  private String assertBoundCombos(String sql) {
    assertTrue(sql.startsWith("WITH\n  coc_no_access AS ("), sql);
    assertTrue(sql.contains("AND coc_co.categoryoptioncomboid = :coc"), sql);
    assertTrue(sql.contains("JOIN dataelementcategoryoption co"), sql);
    assertNoComboInlined(sql);
    verify(query).setParameter("coc", coc.getId());
    return sql;
  }

  private void assertAocListBound(String sql) {
    assertTrue(sql.contains("unnest(CAST(:aocAllowed AS bigint[]))"), sql);
    verify(query)
        .setParameter(
            eq("aocAllowed"), aryEq(new Long[] {aoc.getId()}), eq(LongArrayType.INSTANCE));
  }

  private void assertNoComboInlined(String sql) {
    for (CategoryOptionCombo combo : List.of(coc, aoc)) {
      assertFalse(sql.contains(Long.toString(combo.getId())), sql);
      assertFalse(sql.contains(combo.getUid()), sql);
    }
  }
}
