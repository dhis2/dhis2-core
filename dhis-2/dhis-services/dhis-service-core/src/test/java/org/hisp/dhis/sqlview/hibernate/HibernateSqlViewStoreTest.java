/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.sqlview.hibernate;

import static org.hisp.dhis.common.TransactionMode.READ;
import static org.hisp.dhis.common.TransactionMode.WRITE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.Map;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
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

  @Mock EntityManager entityManager;
  @Mock JdbcTemplate jdbcTemplate;
  @Mock JdbcTemplate readOnlyJdbcTemplate;
  @Mock ApplicationEventPublisher publisher;
  @Mock AclService aclService;
  @Mock SystemSettingsProvider settingsProvider;
  @Mock Grid grid;
  HibernateSqlViewStore store;

  @BeforeEach
  public void setUp() throws Exception {
    when(settingsProvider.getCurrentSettings()).thenReturn(SystemSettings.of(Map.of()));
    store =
        new HibernateSqlViewStore(
            entityManager,
            jdbcTemplate,
            publisher,
            aclService,
            readOnlyJdbcTemplate,
            settingsProvider);
  }

  @Test
  @DisplayName("ensure the correct jdbc template is used for an SQL view with writes")
  void sqlViewJdbcTemplateWriteTest() {
    // when
    store.populateSqlViewGrid(grid, "sql", null, WRITE);

    // then
    verify(readOnlyJdbcTemplate, times(0)).queryForRowSet(anyString());
    verify(jdbcTemplate, times(1)).queryForRowSet(anyString());
  }

  @Test
  @DisplayName("ensure the correct jdbc template is used for an SQL view with reads")
  void sqlViewReadOnlyJdbcTemplateReadTest() {
    // when
    store.populateSqlViewGrid(grid, "sql", null, READ);

    // then
    verify(readOnlyJdbcTemplate, times(1)).queryForRowSet(anyString());
    verify(jdbcTemplate, times(0)).queryForRowSet(anyString());
  }
}
