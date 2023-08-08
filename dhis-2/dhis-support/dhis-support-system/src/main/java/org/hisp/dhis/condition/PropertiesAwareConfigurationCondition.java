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
package org.hisp.dhis.condition;

import org.hisp.dhis.commons.util.SystemUtils;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DefaultDhisConfigurationProvider;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.config.ServiceConfig;
import org.hisp.dhis.external.location.DefaultLocationManager;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;

/**
 * Loads the DHIS2 configuration provider within the context of a Spring Configuration condition.
 * This is required, since the {@link DefaultDhisConfigurationProvider} is not available as Spring
 * Bean when the condition is evaluated.
 *
 * @author Luciano Fiandesio
 */
public abstract class PropertiesAwareConfigurationCondition implements ConfigurationCondition {
  protected DhisConfigurationProvider getConfiguration() {
    DefaultLocationManager locationManager =
        (DefaultLocationManager) new ServiceConfig().locationManager();
    locationManager.init();
    DefaultDhisConfigurationProvider dhisConfigurationProvider =
        new DefaultDhisConfigurationProvider(locationManager);
    dhisConfigurationProvider.init();

    return dhisConfigurationProvider;
  }

  protected boolean isTestRun(ConditionContext context) {
    return SystemUtils.isTestRun(context.getEnvironment().getActiveProfiles());
  }

  protected boolean isAuditTest(ConditionContext context) {
    return SystemUtils.isAuditTest(context.getEnvironment().getActiveProfiles());
  }

  protected boolean getBooleanValue(ConfigurationKey key) {
    return getConfiguration().isEnabled(key);
  }
}
