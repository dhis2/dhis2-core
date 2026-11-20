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
package org.hisp.dhis.startup;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.user.CurrentUserUtil.clearSecurityContext;
import static org.hisp.dhis.user.CurrentUserUtil.injectUserInSecurityContext;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.configuration.ServerBaseUrlValidator;
import org.hisp.dhis.encryption.EncryptionStatus;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.period.PeriodTypeStore;
import org.hisp.dhis.system.startup.TransactionContextStartupRoutine;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.SystemUser;

@Slf4j
public class ConfigurationPopulator extends TransactionContextStartupRoutine {
  private final ConfigurationService configurationService;

  private final DhisConfigurationProvider dhisConfigurationProvider;

  private final PeriodTypeStore periodTypeStore;

  public ConfigurationPopulator(
      ConfigurationService configurationService,
      DhisConfigurationProvider dhisConfigurationProvider,
      PeriodTypeStore periodTypeStore) {
    checkNotNull(configurationService);
    checkNotNull(dhisConfigurationProvider);
    checkNotNull(periodTypeStore);

    this.configurationService = configurationService;
    this.dhisConfigurationProvider = dhisConfigurationProvider;
    this.periodTypeStore = periodTypeStore;
  }

  @Override
  public void executeInTransaction() {
    SystemUser actingUser = new SystemUser();
    boolean hasCurrentUser = CurrentUserUtil.hasCurrentUser();
    if (!hasCurrentUser) {
      injectUserInSecurityContext(actingUser);
    }

    checkSecurityConfiguration();
    checkServerBaseUrl();

    Configuration config = configurationService.getConfiguration();

    if (config != null) {
      boolean changed = false;

      if (config.getSystemId() == null) {
        config.setSystemId(UUID.randomUUID().toString());
        changed = true;
      }

      if (ensureDataOutputPeriodTypes(config)) {
        changed = true;
      }

      if (changed) {
        configurationService.setConfiguration(config);
      }
    }

    if (!hasCurrentUser) {
      clearSecurityContext();
    }
  }

  /**
   * Makes sure the data output period types are populated. On a new (blank) database none are
   * configured, so all available period types are enabled, matching what the 2.43 migration did for
   * existing databases. Yearly is always enabled, as analytics tables are partitioned by year and
   * the analytics table export depends on the yearly column.
   *
   * @param config the {@link Configuration}.
   * @return true if the configuration was changed, false otherwise.
   */
  private boolean ensureDataOutputPeriodTypes(Configuration config) {
    Set<PeriodType> current = config.getDataOutputPeriodTypes();
    List<PeriodType> available = periodTypeStore.getAll();

    if (current == null || current.isEmpty()) {
      if (available.isEmpty()) {
        return false;
      }

      config.setDataOutputPeriodTypes(new LinkedHashSet<>(available));
      log.info("Enabled all {} period types as data output period types", available.size());
      return true;
    }

    boolean hasYearly =
        current.stream().anyMatch(pt -> pt.getPeriodTypeEnum() == PeriodTypeEnum.YEARLY);

    if (hasYearly) {
      return false;
    }

    return available.stream()
        .filter(pt -> pt.getPeriodTypeEnum() == PeriodTypeEnum.YEARLY)
        .findFirst()
        .map(
            yearly -> {
              Set<PeriodType> updated = new LinkedHashSet<>(current);
              updated.add(yearly);
              config.setDataOutputPeriodTypes(updated);
              log.info("Enabled mandatory Yearly data output period type");
              return true;
            })
        .orElse(false);
  }

  private void checkSecurityConfiguration() {
    EncryptionStatus status = dhisConfigurationProvider.getEncryptionStatus();

    if (!status.isOk()) {
      log.warn("Encryption not configured: " + status.getKey());
    } else {
      log.info("Encryption is available");
    }
  }

  private void checkServerBaseUrl() {
    ServerBaseUrlValidator.validate(dhisConfigurationProvider.getServerBaseUrl())
        .ifPresent(problem -> log.warn("{} {}", problem, ServerBaseUrlValidator.HINT));
  }
}
