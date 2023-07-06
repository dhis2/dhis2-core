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
package org.hisp.dhis.config;

import org.hibernate.SessionFactory;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.dataelement.DataElementDefaultDimensionPopulator;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.i18n.I18nLocaleService;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.period.PeriodTypePopulator;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.startup.ConfigurationPopulator;
import org.hisp.dhis.startup.DefaultAdminUserPopulator;
import org.hisp.dhis.startup.I18nLocalePopulator;
import org.hisp.dhis.startup.ModelUpgrader;
import org.hisp.dhis.startup.SchedulerStart;
import org.hisp.dhis.startup.SettingUpgrader;
import org.hisp.dhis.startup.TwoFAPopulator;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Luciano Fiandesio
 */
@Configuration
public class StartupConfig {
  @Bean("org.hisp.dhis.period.PeriodTypePopulator")
  public PeriodTypePopulator periodTypePopulator(
      PeriodStore periodStore, SessionFactory sessionFactory) {
    PeriodTypePopulator populator = new PeriodTypePopulator(periodStore, sessionFactory);
    populator.setName("PeriodTypePopulator");
    populator.setRunlevel(3);
    return populator;
  }

  @Bean
  public TwoFAPopulator twoFAPopulator(
      UserService userService, CurrentUserService currentUserService) {
    TwoFAPopulator populator = new TwoFAPopulator(userService, currentUserService);
    populator.setName("TwoFAPopulator");
    populator.setRunlevel(3);
    populator.setSkipInTests(true);
    return populator;
  }

  @Bean("org.hisp.dhis.dataelement.DataElementDefaultDimensionPopulator")
  public DataElementDefaultDimensionPopulator dataElementDefaultDimensionPopulator(
      DataElementService dataElementService, CategoryService categoryService) {
    DataElementDefaultDimensionPopulator populator =
        new DataElementDefaultDimensionPopulator(dataElementService, categoryService);
    populator.setName("DataElementDefaultDimensionPopulator");
    populator.setRunlevel(4);
    return populator;
  }

  @Bean("org.hisp.dhis.startup.ConfigurationPopulator")
  public ConfigurationPopulator configurationPopulator(
      ConfigurationService configurationService,
      DhisConfigurationProvider dhisConfigurationProvider) {
    ConfigurationPopulator populator =
        new ConfigurationPopulator(configurationService, dhisConfigurationProvider);
    populator.setName("ConfigurationPopulator");
    populator.setRunlevel(12);
    populator.setSkipInTests(true);
    return populator;
  }

  @Bean("org.hisp.dhis.startup.I18nLocalePopulator")
  public I18nLocalePopulator i18nLocalePopulator(I18nLocaleService i18nLocaleService) {
    I18nLocalePopulator populator = new I18nLocalePopulator(i18nLocaleService);
    populator.setName("I18nLocalePopulator");
    populator.setRunlevel(13);
    populator.setSkipInTests(true);
    return populator;
  }

  @Bean("org.hisp.dhis.startup.ModelUpgrader")
  public ModelUpgrader modelUpgrader(
      OrganisationUnitService organisationUnitService, CategoryService categoryService) {
    ModelUpgrader upgrader = new ModelUpgrader(organisationUnitService, categoryService);
    upgrader.setName("ModelUpgrader");
    upgrader.setRunlevel(7);
    upgrader.setSkipInTests(true);
    return upgrader;
  }

  @Bean("org.hisp.dhis.startup.SettingUpgrader")
  public SettingUpgrader settingUpgrader(SystemSettingManager systemSettingManager) {
    SettingUpgrader upgrader = new SettingUpgrader(systemSettingManager);
    upgrader.setRunlevel(14);
    upgrader.setName("SettingUpgrader");
    upgrader.setSkipInTests(true);
    return upgrader;
  }

  @Bean("org.hisp.dhis.startup.DefaultAdminUserPopulator")
  public DefaultAdminUserPopulator defaultAdminUserPopulator(UserService userService) {
    DefaultAdminUserPopulator upgrader = new DefaultAdminUserPopulator(userService);
    upgrader.setName("defaultAdminUserPopulator");
    upgrader.setRunlevel(2);
    upgrader.setSkipInTests(true);
    return upgrader;
  }

  @Bean
  public SchedulerStart schedulerStart(
      SystemSettingManager systemSettingManager,
      JobConfigurationService jobConfigurationService,
      SchedulingManager schedulingManager,
      MessageService messageService,
      DhisConfigurationProvider configurationProvider) {
    SchedulerStart schedulerStart =
        new SchedulerStart(
            systemSettingManager,
            configurationProvider.isEnabled(ConfigurationKey.REDIS_ENABLED),
            configurationProvider.getProperty(ConfigurationKey.LEADER_TIME_TO_LIVE),
            jobConfigurationService,
            schedulingManager,
            messageService);
    schedulerStart.setRunlevel(15);
    schedulerStart.setSkipInTests(true);
    return schedulerStart;
  }
}
