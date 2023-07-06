/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.program.jdbc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Array;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.association.AbstractOrganisationUnitAssociationsQueryBuilder;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.TestCache;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.user.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

/**
 * @author <luca@dhis2.org>
 */
@ExtendWith(MockitoExtension.class)
class JdbcOrgUnitAssociationsStoreTest {
  private JdbcOrgUnitAssociationsStore jdbcOrgUnitAssociationsStore;

  @Mock private JdbcTemplate jdbcTemplate;

  @Mock private AbstractOrganisationUnitAssociationsQueryBuilder queryBuilder;

  @Mock private Array orgUnitArray;

  @Mock private ResultSet resultSet;

  @Captor private ArgumentCaptor<ResultSetExtractor<?>> resultSetExtractorArgumentCaptor;

  private final Cache<Set<String>> programToOrgUnitCache = new TestCache<>();

  private final String program = CodeGenerator.generateUid();

  private final String orgUnitA = CodeGenerator.generateUid();

  private final String orgUnitB = CodeGenerator.generateUid();

  @BeforeEach
  void setUpTest() {
    jdbcOrgUnitAssociationsStore =
        new JdbcOrgUnitAssociationsStore(
            mock(CurrentUserService.class), jdbcTemplate, queryBuilder, programToOrgUnitCache);
  }

  @Test
  void shouldFindAssociationWhenOrgUnitIsCached() {
    programToOrgUnitCache.put(program, Set.of(orgUnitA));

    assertTrue(jdbcOrgUnitAssociationsStore.checkOrganisationUnitsAssociations(program, orgUnitA));
    verify(jdbcTemplate, times(0)).query(anyString(), resultSetExtractorArgumentCaptor.capture());
  }

  @Test
  void shouldNotFindAssociationWhenProgramHasNoOrgUnitAssociation() {
    when(queryBuilder.buildSqlQueryForRawAssociation(
            new HashSet<>(Collections.singletonList(program))))
        .thenReturn("query");

    when(jdbcTemplate.query(anyString(), resultSetExtractorArgumentCaptor.capture()))
        .thenAnswer(
            (invocation) -> {
              when(orgUnitArray.getArray()).thenReturn(new String[] {orgUnitA});

              ResultSetExtractor<Map<Integer, String>> resultSetExtractor =
                  invocation.getArgument(1);

              when(resultSet.next()).thenReturn(true, false);

              Mockito.when(resultSet.getString(1)).thenReturn(program);

              Mockito.when(resultSet.getArray(2)).thenReturn(orgUnitArray);

              return resultSetExtractor.extractData(resultSet);
            });

    assertFalse(jdbcOrgUnitAssociationsStore.checkOrganisationUnitsAssociations(program, orgUnitB));
    verify(jdbcTemplate, times(1)).query(anyString(), resultSetExtractorArgumentCaptor.capture());
  }

  @Test
  void shouldNotFindAssociationWhenProgramOrgUnitDoNotExists() {
    when(queryBuilder.buildSqlQueryForRawAssociation(
            new HashSet<>(Collections.singletonList(program))))
        .thenReturn("query");

    when(jdbcTemplate.query(anyString(), resultSetExtractorArgumentCaptor.capture()))
        .thenAnswer(
            (invocation) -> {
              ResultSetExtractor<Map<Integer, String>> resultSetExtractor =
                  invocation.getArgument(1);

              when(resultSet.next()).thenReturn(false);

              return resultSetExtractor.extractData(resultSet);
            });

    assertFalse(jdbcOrgUnitAssociationsStore.checkOrganisationUnitsAssociations(program, orgUnitA));
    verify(jdbcTemplate, times(1)).query(anyString(), resultSetExtractorArgumentCaptor.capture());
  }

  @Test
  void shouldFindAssociationWhenOrgUnitIsNotCachedButProgramOrgUnitExists() {
    programToOrgUnitCache.put(program, Set.of(orgUnitA));

    when(queryBuilder.buildSqlQueryForRawAssociation(
            new HashSet<>(Collections.singletonList(program))))
        .thenReturn("query");

    when(jdbcTemplate.query(anyString(), resultSetExtractorArgumentCaptor.capture()))
        .thenAnswer(
            (invocation) -> {
              when(orgUnitArray.getArray()).thenReturn(new String[] {orgUnitA, orgUnitB});

              ResultSetExtractor<Map<Integer, String>> resultSetExtractor =
                  invocation.getArgument(1);

              when(resultSet.next()).thenReturn(true, false);

              Mockito.when(resultSet.getString(1)).thenReturn(program);

              Mockito.when(resultSet.getArray(2)).thenReturn(orgUnitArray);

              return resultSetExtractor.extractData(resultSet);
            });

    assertTrue(jdbcOrgUnitAssociationsStore.checkOrganisationUnitsAssociations(program, orgUnitB));
    verify(jdbcTemplate, times(1)).query(anyString(), resultSetExtractorArgumentCaptor.capture());
  }

  @Test
  void shouldNotFindAssociationWhenOrgUnitIsNotCachedAndProgramOrgUnitNotExists() {
    programToOrgUnitCache.put(program, Set.of(orgUnitA));

    when(queryBuilder.buildSqlQueryForRawAssociation(
            new HashSet<>(Collections.singletonList(program))))
        .thenReturn("query");

    when(jdbcTemplate.query(anyString(), resultSetExtractorArgumentCaptor.capture()))
        .thenAnswer(
            (invocation) -> {
              when(orgUnitArray.getArray()).thenReturn(new String[] {orgUnitA});

              ResultSetExtractor<Map<Integer, String>> resultSetExtractor =
                  invocation.getArgument(1);

              when(resultSet.next()).thenReturn(true, false);

              Mockito.when(resultSet.getString(1)).thenReturn(program);

              Mockito.when(resultSet.getArray(2)).thenReturn(orgUnitArray);

              return resultSetExtractor.extractData(resultSet);
            });

    assertFalse(jdbcOrgUnitAssociationsStore.checkOrganisationUnitsAssociations(program, orgUnitB));
    verify(jdbcTemplate, times(1)).query(anyString(), resultSetExtractorArgumentCaptor.capture());
  }
}
