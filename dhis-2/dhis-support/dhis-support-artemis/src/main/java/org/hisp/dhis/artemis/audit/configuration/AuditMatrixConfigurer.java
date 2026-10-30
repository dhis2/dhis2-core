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
package org.hisp.dhis.artemis.audit.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.artemis.config.ArtemisConfigData;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.stereotype.Component;

/**
 * Configures the Audit Matrix based on configuration properties from dhis.conf
 *
 * <p>This configurator uses properties with prefix "audit.". Each property prefixed with "audit."
 * must match the (lowercase) name of an {@see AuditScope} and must contain a semi-colon list of
 * valid {@see AuditType} names: (READ;UPDATE;...).
 *
 * <p>Example:
 *
 * <p>audit.tracker=CREATE;READ;UPDATE;DELETE
 *
 * @author Luciano Fiandesio
 */
@Component
@RequiredArgsConstructor
public class AuditMatrixConfigurer {
  private final DhisConfigurationProvider config;

  private final ArtemisConfigData artemisConfig;

  private static final String PROPERTY_PREFIX = "audit.";

  private static final String AUDIT_TYPE_STRING_SEPAR = ";";

  /**
   * Default Audit configuration: CREATE, UPDATE, DELETE, and SECURITY operations are audited by
   * default. Other Audit types have to be explicitly enabled by the user.
   */
  private static final Set<AuditType> DEFAULT_AUDIT_CONFIGURATION =
      Collections.unmodifiableSet(
          EnumSet.of(AuditType.CREATE, AuditType.UPDATE, AuditType.DELETE, AuditType.SECURITY));

  public Map<AuditScope, Set<AuditType>> configure() {
    Map<AuditScope, Set<AuditType>> matrix = new EnumMap<>(AuditScope.class);

    for (AuditScope scope : AuditScope.values()) {
      Optional<ConfigurationKey> confKey =
          ConfigurationKey.getByKey(PROPERTY_PREFIX + scope.name().toLowerCase());

      if (confKey.isPresent() && !StringUtils.isEmpty(config.getProperty(confKey.get()))) {
        matrix.put(scope, parseAuditTypes(scope, config.getProperty(confKey.get())));
      } else {
        matrix.put(scope, DEFAULT_AUDIT_CONFIGURATION);
      }
    }

    disableScopesWithNoConsumer(matrix);

    return matrix;
  }

  private static Set<AuditType> parseAuditTypes(AuditScope scope, String configuredTypesString) {
    Set<AuditType> auditTypes = EnumSet.noneOf(AuditType.class);
    List<String> invalid = new ArrayList<>();
    for (String token : configuredTypesString.split(AUDIT_TYPE_STRING_SEPAR)) {
      String trimmed = token.trim();
      // DISABLED is not an AuditType enum value but is documented as a way to disable a scope
      // (e.g. audit.tracker=DISABLED). It worked by accident before: the old code iterated all
      // AuditType values checking if each was in the input, and since "DISABLED" matched none,
      // every type got false. We preserve this behavior explicitly.
      if (trimmed.isEmpty() || "DISABLED".equals(trimmed)) {
        continue;
      }
      try {
        auditTypes.add(AuditType.valueOf(trimmed));
      } catch (IllegalArgumentException e) {
        invalid.add(trimmed);
      }
    }

    if (!invalid.isEmpty()) {
      String validTypes =
          Arrays.stream(AuditType.values()).map(AuditType::name).collect(Collectors.joining(", "));
      throw new IllegalArgumentException(
          String.format(
              "Invalid audit type(s) %s in config audit.%s. Valid types are: %s, DISABLED",
              invalid, scope.name().toLowerCase(), validTypes));
    }

    return auditTypes;
  }

  /**
   * Disables audit scopes that have no consumer in embedded Artemis mode to avoid the per-entity
   * JMS pipeline (entity serialization, JSON serialization, publish, consume, deserialize) running
   * only to discard every message. When both sinks (logger and database) are off for a scope and no
   * external consumer can connect to the embedded broker, the pipeline has no consumer.
   */
  private void disableScopesWithNoConsumer(Map<AuditScope, Set<AuditType>> matrix) {
    if (!artemisConfig.isEmbedded() || config.isEnabled(ConfigurationKey.AUDIT_DATABASE)) {
      return;
    }

    for (AuditScope scope : AuditScope.values()) {
      if (!isLoggerEnabled(scope)) {
        matrix.put(scope, Set.of());
      }
    }
  }

  /**
   * Checks if audit logging is enabled for a scope. This must match the logic in each scope's
   * consumer. TrackerAuditConsumer uses a hardcoded fallback of "off" for audit.logger instead of
   * the key's built-in default of "on". When audit.logger was flipped from off to on
   * (https://github.com/dhis2/dhis2-core/pull/8785) TrackerAuditConsumer was not updated, so it
   * diverges from other consumers when audit.logger is absent from dhis.conf.
   *
   * @see org.hisp.dhis.audit.consumers.TrackerAuditConsumer
   */
  private boolean isLoggerEnabled(AuditScope scope) {
    if (scope == AuditScope.TRACKER) {
      return Objects.equals(
          config.getPropertyOrDefault(ConfigurationKey.AUDIT_LOGGER, "off"), "on");
    }
    return config.isEnabled(ConfigurationKey.AUDIT_LOGGER);
  }
}
