/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.config;

import static org.hisp.dhis.external.conf.ConfigurationKey.MONITORING_SQL_CONTEXT_KEYS;
import static org.hisp.dhis.log.MdcKeys.MDC_CONTROLLER;
import static org.hisp.dhis.log.MdcKeys.MDC_METHOD;
import static org.hisp.dhis.log.MdcKeys.MDC_REQUEST_ID;
import static org.hisp.dhis.log.MdcKeys.MDC_SESSION_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.util.LinkedHashMap;
import java.util.List;
import net.ttddyy.dsproxy.transform.TransformInfo;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
class DataSourceConfigTest {

  @Mock private DhisConfigurationProvider config;

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void buildKeyMapUsesValidKeysOrder() {
    when(config.getProperty(MONITORING_SQL_CONTEXT_KEYS))
        .thenReturn("sessionId,controller,requestId,method");

    LinkedHashMap<String, String> keyMap = DataSourceConfig.buildKeyMap(config);

    assertEquals(
        List.of("controller", "method", "request_id", "session_id"),
        List.copyOf(keyMap.values()));
  }

  @Test
  void buildKeyMapSubset() {
    when(config.getProperty(MONITORING_SQL_CONTEXT_KEYS)).thenReturn("sessionId,controller");

    LinkedHashMap<String, String> keyMap = DataSourceConfig.buildKeyMap(config);

    assertEquals(List.of("controller", "session_id"), List.copyOf(keyMap.values()));
  }

  @Test
  void buildKeyMapRejectsInvalidKeys() {
    when(config.getProperty(MONITORING_SQL_CONTEXT_KEYS)).thenReturn("controller,bogus");

    assertThrows(IllegalStateException.class, () -> DataSourceConfig.buildKeyMap(config));
  }

  @Test
  void addMdcCommentPrependsComment() {
    MDC.put(MDC_CONTROLLER, "UserController");
    MDC.put(MDC_METHOD, "getUser");

    LinkedHashMap<String, String> keyMap = new LinkedHashMap<>();
    keyMap.put(MDC_CONTROLLER, "controller");
    keyMap.put(MDC_METHOD, "method");

    TransformInfo info = new TransformInfo(PreparedStatement.class, "ds", "select 1", false, 0);

    assertEquals(
        "/* controller='UserController',method='getUser' */ select 1",
        DataSourceConfig.addMdcComment(info, keyMap));
  }

  @Test
  void addMdcCommentSkipsMissingKeys() {
    MDC.put(MDC_CONTROLLER, "UserController");

    LinkedHashMap<String, String> keyMap = new LinkedHashMap<>();
    keyMap.put(MDC_CONTROLLER, "controller");
    keyMap.put(MDC_METHOD, "method");
    keyMap.put(MDC_REQUEST_ID, "request_id");

    TransformInfo info = new TransformInfo(PreparedStatement.class, "ds", "select 1", false, 0);

    assertEquals(
        "/* controller='UserController' */ select 1",
        DataSourceConfig.addMdcComment(info, keyMap));
  }

  @Test
  void addMdcCommentReturnsQueryWhenMdcEmpty() {
    LinkedHashMap<String, String> keyMap = new LinkedHashMap<>();
    keyMap.put(MDC_CONTROLLER, "controller");

    TransformInfo info = new TransformInfo(PreparedStatement.class, "ds", "select 1", false, 0);

    assertEquals("select 1", DataSourceConfig.addMdcComment(info, keyMap));
  }

  @Test
  void addMdcCommentReturnsQueryWhenNoKeysMatch() {
    MDC.put(MDC_SESSION_ID, "abc123");

    LinkedHashMap<String, String> keyMap = new LinkedHashMap<>();
    keyMap.put(MDC_CONTROLLER, "controller");

    TransformInfo info = new TransformInfo(PreparedStatement.class, "ds", "select 1", false, 0);

    assertEquals("select 1", DataSourceConfig.addMdcComment(info, keyMap));
  }
}
