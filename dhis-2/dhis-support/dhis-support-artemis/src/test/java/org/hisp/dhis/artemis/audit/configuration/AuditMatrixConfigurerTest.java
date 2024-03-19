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
package org.hisp.dhis.artemis.audit.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.audit.AuditScope.AGGREGATE;
import static org.hisp.dhis.audit.AuditScope.METADATA;
import static org.hisp.dhis.audit.AuditScope.TRACKER;
import static org.hisp.dhis.audit.AuditType.CREATE;
import static org.hisp.dhis.audit.AuditType.DELETE;
import static org.hisp.dhis.audit.AuditType.READ;
import static org.hisp.dhis.audit.AuditType.SEARCH;
import static org.hisp.dhis.audit.AuditType.SECURITY;
import static org.hisp.dhis.audit.AuditType.UPDATE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith(MockitoExtension.class)
class AuditMatrixConfigurerTest {
  @Mock private DhisConfigurationProvider config;

  private AuditMatrixConfigurer subject;

  private Map<AuditScope, Map<AuditType, Boolean>> matrix;

  @BeforeEach
  public void setUp() {
    this.subject = new AuditMatrixConfigurer(config);
  }

  @Test
  void verifyConfigurationForMatrixIsIngested() {
    when(config.getProperty(ConfigurationKey.AUDIT_METADATA_MATRIX)).thenReturn("READ;");
    when(config.getProperty(ConfigurationKey.AUDIT_TRACKER_MATRIX))
        .thenReturn("CREATE;READ;UPDATE;DELETE");
    when(config.getProperty(ConfigurationKey.AUDIT_AGGREGATE_MATRIX))
        .thenReturn("CREATE;UPDATE;DELETE");

    matrix = this.subject.configure();

    assertThat(matrix.get(METADATA).keySet(), hasSize(6));
    assertMatrixEnabled(METADATA, READ);
    assertMatrixDisabled(METADATA, CREATE, UPDATE, DELETE);

    assertThat(matrix.get(TRACKER).keySet(), hasSize(6));
    assertMatrixDisabled(TRACKER, SEARCH, SECURITY);
    assertMatrixEnabled(TRACKER, CREATE, UPDATE, DELETE, READ);

    assertThat(matrix.get(AGGREGATE).keySet(), hasSize(6));
    assertMatrixDisabled(AGGREGATE, READ, SECURITY, SEARCH);
    assertMatrixEnabled(AGGREGATE, CREATE, UPDATE, DELETE);
  }

  @Test
  void allDisabled() {
    when(config.getProperty(ConfigurationKey.AUDIT_METADATA_MATRIX)).thenReturn("DISABLED");
    when(config.getProperty(ConfigurationKey.AUDIT_TRACKER_MATRIX)).thenReturn("DISABLED");
    when(config.getProperty(ConfigurationKey.AUDIT_AGGREGATE_MATRIX)).thenReturn("DISABLED");

    matrix = this.subject.configure();

    assertMatrixAllDisabled(METADATA);
    assertMatrixAllDisabled(TRACKER);
    assertMatrixAllDisabled(AGGREGATE);
  }

  @Test
  void verifyInvalidConfigurationIsIgnored() {
    when(config.getProperty(ConfigurationKey.AUDIT_METADATA_MATRIX)).thenReturn("READX;UPDATE");

    matrix = this.subject.configure();
    assertThat(matrix.get(METADATA).keySet(), hasSize(6));
    assertAllFalseBut(matrix.get(METADATA), UPDATE);
  }

  @Test
  void verifyDefaultAuditingConfiguration() {
    matrix = this.subject.configure();
    assertMatrixDisabled(METADATA, READ);
    assertMatrixEnabled(METADATA, CREATE);
    assertMatrixEnabled(METADATA, UPDATE);
    assertMatrixEnabled(METADATA, DELETE);

    assertMatrixDisabled(TRACKER, READ);
    assertMatrixEnabled(TRACKER, CREATE);
    assertMatrixEnabled(TRACKER, UPDATE);
    assertMatrixEnabled(TRACKER, DELETE);

    assertMatrixDisabled(AGGREGATE, READ);
    assertMatrixEnabled(AGGREGATE, CREATE);
    assertMatrixEnabled(AGGREGATE, UPDATE);
    assertMatrixEnabled(AGGREGATE, DELETE);
  }

  private void assertAllFalseBut(
      Map<AuditType, Boolean> auditTypeBooleanMap, AuditType trueAuditType) {
    for (AuditType auditType : auditTypeBooleanMap.keySet()) {
      if (!auditType.name().equals(trueAuditType.name())) {
        assertFalse(auditTypeBooleanMap.get(auditType));
      } else {
        assertTrue(auditTypeBooleanMap.get(auditType));
      }
    }
  }

  private void assertMatrixEnabled(AuditScope auditScope, AuditType... auditTypes) {
    for (AuditType auditType : auditTypes) {
      assertThat(
          "Expecting true for audit type: " + auditType.name(),
          matrix.get(auditScope).get(auditType),
          is(true));
    }
  }

  private void assertMatrixDisabled(AuditScope auditScope, AuditType... auditTypes) {
    for (AuditType auditType : auditTypes) {
      assertThat(
          "Expecting false for audit type: " + auditType.name(),
          matrix.get(auditScope).get(auditType),
          is(false));
    }
  }

  private void assertMatrixAllDisabled(AuditScope auditScope) {
    for (AuditType auditType : AuditType.values()) {
      assertThat(
          "Expecting false for audit type: " + auditType.name(),
          matrix.get(auditScope).get(auditType),
          is(false));
    }
  }
}
