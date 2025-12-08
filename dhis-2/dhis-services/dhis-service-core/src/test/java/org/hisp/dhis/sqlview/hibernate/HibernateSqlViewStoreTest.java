/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.sqlview.hibernate;

import static org.hisp.dhis.common.TransactionMode.READ;
import static org.hisp.dhis.common.TransactionMode.WRITE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.hibernate.SessionFactory;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class HibernateSqlViewStoreTest {

  @Mock SessionFactory sessionFactory;
  @Mock JdbcTemplate jdbcTemplate;
  @Mock JdbcTemplate readOnlyJdbcTemplate;
  @Mock ApplicationEventPublisher publisher;
  @Mock CurrentUserService currentUserService;
  @Mock AclService aclService;
  @Mock StatementBuilder statementBuilder;
  @Mock SystemSettingManager systemSettingManager;
  @Mock Grid grid;
  HibernateSqlViewStore store;

  @BeforeEach
  void setUp() {
    store =
        new HibernateSqlViewStore(
            sessionFactory,
            jdbcTemplate,
            publisher,
            currentUserService,
            aclService,
            statementBuilder,
            readOnlyJdbcTemplate,
            systemSettingManager);
  }

  @Test
  @DisplayName("ensure the correct jdbc template is used for an SQL view with writes")
  void sqlViewJdbcTemplateWriteTest() {
    // when
    store.populateSqlViewGrid(grid, "sql", null, WRITE);

    // then
    verify(readOnlyJdbcTemplate, times(0)).queryForRowSet(anyString(), isNull());
    verify(jdbcTemplate, times(1)).queryForRowSet(anyString(), isNull());
  }

  @Test
  @DisplayName("ensure the correct jdbc template is used for an SQL view with reads")
  void sqlViewReadOnlyJdbcTemplateReadTest() {
    // when
    store.populateSqlViewGrid(grid, "sql", null, READ);

    // then
    verify(readOnlyJdbcTemplate, times(1)).queryForRowSet(anyString(), isNull());
    verify(jdbcTemplate, times(0)).queryForRowSet(anyString(), isNull());
  }
}
