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

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.encryption.EncryptionStatus;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.system.startup.TransactionContextStartupRoutine;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.SystemUser;

@Slf4j
public class ConfigurationPopulator extends TransactionContextStartupRoutine {
  private final ConfigurationService configurationService;

  private final DhisConfigurationProvider dhisConfigurationProvider;

  public ConfigurationPopulator(
      ConfigurationService configurationService,
      DhisConfigurationProvider dhisConfigurationProvider) {
    checkNotNull(configurationService);
    checkNotNull(dhisConfigurationProvider);

    this.configurationService = configurationService;
    this.dhisConfigurationProvider = dhisConfigurationProvider;
  }

  @Override
  public void executeInTransaction() {
    SystemUser actingUser = new SystemUser();
    boolean hasCurrentUser = CurrentUserUtil.hasCurrentUser();
    if (!hasCurrentUser) {
      injectUserInSecurityContext(actingUser);
    }

    checkSecurityConfiguration();

    Configuration config = configurationService.getConfiguration();

    if (config != null && config.getSystemId() == null) {
      config.setSystemId(UUID.randomUUID().toString());
      configurationService.setConfiguration(config);
    }

    if (!hasCurrentUser) {
      clearSecurityContext();
    }
  }

  private void checkSecurityConfiguration() {
    EncryptionStatus status = dhisConfigurationProvider.getEncryptionStatus();

    if (!status.isOk()) {
      log.warn("Encryption not configured: " + status.getKey());
    } else {
      log.info("Encryption is available");
    }
  }
}
