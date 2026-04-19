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

import static org.hisp.dhis.audit.AuditScope.AGGREGATE;
import static org.hisp.dhis.audit.AuditScope.METADATA;
import static org.hisp.dhis.audit.AuditScope.TRACKER;
import static org.hisp.dhis.audit.AuditType.CREATE;
import static org.hisp.dhis.audit.AuditType.DELETE;
import static org.hisp.dhis.audit.AuditType.READ;
import static org.hisp.dhis.audit.AuditType.SECURITY;
import static org.hisp.dhis.audit.AuditType.UPDATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;
import org.hisp.dhis.artemis.config.ArtemisConfigData;
import org.hisp.dhis.artemis.config.ArtemisMode;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditMatrixConfigurerTest {
  @Mock private DhisConfigurationProvider config;

  private ArtemisConfigData artemisConfig;

  private AuditMatrixConfigurer subject;

  private Map<AuditScope, Set<AuditType>> matrix;

  @BeforeEach
  public void setUp() {
    artemisConfig = new ArtemisConfigData();
    artemisConfig.setMode(ArtemisMode.EMBEDDED);
    // lenient because configure() iterates all scopes calling getProperty() and isEnabled()
    // with different ConfigurationKey args, but tests only stub the ones relevant to each case
    this.subject = new AuditMatrixConfigurer(config, artemisConfig);
  }

  @Test
  void verifyConfigurationForMatrixIsIngested() {
    enableDatabaseSink();
    when(config.getProperty(ConfigurationKey.AUDIT_METADATA_MATRIX)).thenReturn("READ;");
    when(config.getProperty(ConfigurationKey.AUDIT_AGGREGATE_MATRIX))
        .thenReturn("CREATE;UPDATE;DELETE");
    when(config.getProperty(ConfigurationKey.AUDIT_TRACKER_MATRIX))
        .thenReturn("CREATE;READ;UPDATE;DELETE");

    matrix = this.subject.configure();

    assertEquals(Set.of(READ), matrix.get(METADATA));
    assertEquals(Set.of(CREATE, UPDATE, DELETE), matrix.get(AGGREGATE));
    assertEquals(Set.of(CREATE, READ, UPDATE, DELETE), matrix.get(TRACKER));
  }

  @Test
  void verifyDisabledAsOnlyValue() {
    when(config.getProperty(ConfigurationKey.AUDIT_METADATA_MATRIX)).thenReturn("DISABLED");
    when(config.getProperty(ConfigurationKey.AUDIT_AGGREGATE_MATRIX)).thenReturn("DISABLED");
    when(config.getProperty(ConfigurationKey.AUDIT_TRACKER_MATRIX)).thenReturn("DISABLED");

    matrix = this.subject.configure();

    assertEquals(Set.of(), matrix.get(METADATA));
    assertEquals(Set.of(), matrix.get(AGGREGATE));
    assertEquals(Set.of(), matrix.get(TRACKER));
  }

  @Test
  void verifyDisabledCombinedWithValidTypesEnablesTypes() {
    enableDatabaseSink();
    when(config.getProperty(ConfigurationKey.AUDIT_TRACKER_MATRIX))
        .thenReturn("DISABLED;CREATE;UPDATE");

    matrix = this.subject.configure();

    assertEquals(Set.of(CREATE, UPDATE), matrix.get(TRACKER));
  }

  @Test
  void verifyInvalidConfigurationThrows() {
    when(config.getProperty(ConfigurationKey.AUDIT_METADATA_MATRIX)).thenReturn("READX;UPDATE");

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> this.subject.configure());
    assertTrue(ex.getMessage().contains("READX"));
    assertTrue(ex.getMessage().contains("audit.metadata"));
  }

  @Test
  void verifyWhitespaceInConfigIsTrimmed() {
    enableLoggerSink();
    when(config.getProperty(ConfigurationKey.AUDIT_METADATA_MATRIX))
        .thenReturn(" CREATE ; UPDATE ; DELETE ");

    matrix = this.subject.configure();

    assertEquals(Set.of(CREATE, UPDATE, DELETE), matrix.get(METADATA));
  }

  @Test
  void verifyDefaultConfigDisablesTrackerInEmbeddedMode() {
    // audit.logger absent from dhis.conf: defaults to "on" so isEnabled() returns true.
    // TrackerAuditConsumer uses getPropertyOrDefault(AUDIT_LOGGER, "off") which returns "off"
    // since the property is absent. Metadata/aggregate have logger on, tracker has it off.
    enableLoggerSink();

    matrix = this.subject.configure();

    assertEquals(Set.of(CREATE, UPDATE, DELETE, SECURITY), matrix.get(METADATA));
    assertEquals(Set.of(CREATE, UPDATE, DELETE, SECURITY), matrix.get(AGGREGATE));
    assertEquals(Set.of(), matrix.get(TRACKER));
  }

  @Test
  void verifyTrackerEnabledWhenDatabaseSinkIsOn() {
    enableDatabaseSink();

    matrix = this.subject.configure();

    assertEquals(Set.of(CREATE, UPDATE, DELETE, SECURITY), matrix.get(METADATA));
    assertEquals(Set.of(CREATE, UPDATE, DELETE, SECURITY), matrix.get(AGGREGATE));
    assertEquals(Set.of(CREATE, UPDATE, DELETE, SECURITY), matrix.get(TRACKER));
  }

  @Test
  void verifyTrackerEnabledWhenLoggerSetInConfig() {
    // audit.logger=on explicitly set in dhis.conf: both isEnabled() and
    // getPropertyOrDefault(AUDIT_LOGGER, "off") return "on", so tracker logger is enabled
    enableLoggerSink();
    when(config.getPropertyOrDefault(ConfigurationKey.AUDIT_LOGGER, "off")).thenReturn("on");

    matrix = this.subject.configure();

    assertEquals(Set.of(CREATE, UPDATE, DELETE, SECURITY), matrix.get(METADATA));
    assertEquals(Set.of(CREATE, UPDATE, DELETE, SECURITY), matrix.get(AGGREGATE));
    assertEquals(Set.of(CREATE, UPDATE, DELETE, SECURITY), matrix.get(TRACKER));
  }

  @Test
  void verifySinksOffKeepsAllScopesEnabledInNativeMode() {
    // sinks off but external consumers can connect in native mode
    artemisConfig.setMode(ArtemisMode.NATIVE);
    subject = new AuditMatrixConfigurer(config, artemisConfig);

    matrix = this.subject.configure();

    assertEquals(Set.of(CREATE, UPDATE, DELETE, SECURITY), matrix.get(METADATA));
    assertEquals(Set.of(CREATE, UPDATE, DELETE, SECURITY), matrix.get(AGGREGATE));
    assertEquals(Set.of(CREATE, UPDATE, DELETE, SECURITY), matrix.get(TRACKER));
  }

  @Test
  void verifySinksOffDisablesAllScopesInEmbeddedMode() {
    matrix = this.subject.configure();

    assertEquals(Set.of(), matrix.get(METADATA));
    assertEquals(Set.of(), matrix.get(AGGREGATE));
    assertEquals(Set.of(), matrix.get(TRACKER));
  }

  @Test
  void verifyExplicitMatrixOverriddenWhenSinksOffInEmbeddedMode() {
    when(config.getProperty(ConfigurationKey.AUDIT_TRACKER_MATRIX))
        .thenReturn("CREATE;UPDATE;DELETE");

    matrix = this.subject.configure();

    // explicitly configured but both sinks off + embedded = no consumer, so disabled
    assertEquals(Set.of(), matrix.get(TRACKER));
  }

  private void enableLoggerSink() {
    when(config.isEnabled(ConfigurationKey.AUDIT_LOGGER)).thenReturn(true);
  }

  private void enableDatabaseSink() {
    when(config.isEnabled(ConfigurationKey.AUDIT_DATABASE)).thenReturn(true);
  }
}
