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
package org.hisp.dhis.analytics.config;

import org.hisp.dhis.analytics.AnalyticsTableManager;
import org.hisp.dhis.analytics.AnalyticsTableService;
import org.hisp.dhis.analytics.table.DefaultAnalyticsTableService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilderProvider;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Luciano Fiandesio
 */
@Configuration("analyticsServiceConfig")
public class ServiceConfig {
  @Bean
  public SqlBuilder sqlBuilder(DhisConfigurationProvider config) {
    return new SqlBuilderProvider(config).getSqlBuilder();
  }

  @Bean("org.hisp.dhis.analytics.TeiAnalyticsTableService")
  public AnalyticsTableService teiAnalyticsTableManager(
      @Qualifier("org.hisp.dhis.analytics.TeiAnalyticsTableManager")
          AnalyticsTableManager tableManager,
      OrganisationUnitService organisationUnitService,
      DataElementService dataElementService,
      ResourceTableService resourceTableService,
      SystemSettingManager systemSettingManager) {
    return new DefaultAnalyticsTableService(
        tableManager,
        organisationUnitService,
        dataElementService,
        resourceTableService,
        systemSettingManager);
  }

  @Bean("org.hisp.dhis.analytics.TeiEventsAnalyticsTableService")
  public AnalyticsTableService teiEventsAnalyticsTableManager(
      @Qualifier("org.hisp.dhis.analytics.TeiEventsAnalyticsTableManager")
          AnalyticsTableManager tableManager,
      OrganisationUnitService organisationUnitService,
      DataElementService dataElementService,
      ResourceTableService resourceTableService,
      SystemSettingManager systemSettingManager) {
    return new DefaultAnalyticsTableService(
        tableManager,
        organisationUnitService,
        dataElementService,
        resourceTableService,
        systemSettingManager);
  }

  @Bean("org.hisp.dhis.analytics.TeiEnrollmentsAnalyticsTableService")
  public AnalyticsTableService teiEnrollmentsAnalyticsTableManager(
      @Qualifier("org.hisp.dhis.analytics.TeiEnrollmentsAnalyticsTableManager")
          AnalyticsTableManager tableManager,
      OrganisationUnitService organisationUnitService,
      DataElementService dataElementService,
      ResourceTableService resourceTableService,
      SystemSettingManager systemSettingManager) {
    return new DefaultAnalyticsTableService(
        tableManager,
        organisationUnitService,
        dataElementService,
        resourceTableService,
        systemSettingManager);
  }

  @Bean("org.hisp.dhis.analytics.AnalyticsTableService")
  public AnalyticsTableService analyticsTableService(
      @Qualifier("org.hisp.dhis.analytics.AnalyticsTableManager")
          AnalyticsTableManager tableManager,
      OrganisationUnitService organisationUnitService,
      DataElementService dataElementService,
      ResourceTableService resourceTableService,
      SystemSettingManager systemSettingManager) {
    return new DefaultAnalyticsTableService(
        tableManager,
        organisationUnitService,
        dataElementService,
        resourceTableService,
        systemSettingManager);
  }

  @Bean("org.hisp.dhis.analytics.CompletenessTableService")
  public AnalyticsTableService completenessTableService(
      @Qualifier("org.hisp.dhis.analytics.CompletenessTableManager")
          AnalyticsTableManager tableManager,
      OrganisationUnitService organisationUnitService,
      DataElementService dataElementService,
      ResourceTableService resourceTableService,
      SystemSettingManager systemSettingManager) {
    return new DefaultAnalyticsTableService(
        tableManager,
        organisationUnitService,
        dataElementService,
        resourceTableService,
        systemSettingManager);
  }

  @Bean("org.hisp.dhis.analytics.CompletenessTargetTableService")
  public AnalyticsTableService completenessTargetTableService(
      @Qualifier("org.hisp.dhis.analytics.CompletenessTargetTableManager")
          AnalyticsTableManager tableManager,
      OrganisationUnitService organisationUnitService,
      DataElementService dataElementService,
      ResourceTableService resourceTableService,
      SystemSettingManager systemSettingManager) {
    return new DefaultAnalyticsTableService(
        tableManager,
        organisationUnitService,
        dataElementService,
        resourceTableService,
        systemSettingManager);
  }

  @Bean("org.hisp.dhis.analytics.OrgUnitTargetTableService")
  public AnalyticsTableService orgUnitTargetTableService(
      @Qualifier("org.hisp.dhis.analytics.OrgUnitTargetTableManager")
          AnalyticsTableManager tableManager,
      OrganisationUnitService organisationUnitService,
      DataElementService dataElementService,
      ResourceTableService resourceTableService,
      SystemSettingManager systemSettingManager) {
    return new DefaultAnalyticsTableService(
        tableManager,
        organisationUnitService,
        dataElementService,
        resourceTableService,
        systemSettingManager);
  }

  @Bean("org.hisp.dhis.analytics.OwnershipAnalyticsTableService")
  public AnalyticsTableService ownershipAnalyticsTableManager(
      @Qualifier("org.hisp.dhis.analytics.OwnershipAnalyticsTableManager")
          AnalyticsTableManager tableManager,
      OrganisationUnitService organisationUnitService,
      DataElementService dataElementService,
      ResourceTableService resourceTableService,
      SystemSettingManager systemSettingManager) {
    return new DefaultAnalyticsTableService(
        tableManager,
        organisationUnitService,
        dataElementService,
        resourceTableService,
        systemSettingManager);
  }

  @Bean("org.hisp.dhis.analytics.EventAnalyticsTableService")
  public AnalyticsTableService eventAnalyticsTableService(
      @Qualifier("org.hisp.dhis.analytics.EventAnalyticsTableManager")
          AnalyticsTableManager tableManager,
      OrganisationUnitService organisationUnitService,
      DataElementService dataElementService,
      ResourceTableService resourceTableService,
      SystemSettingManager systemSettingManager) {
    return new DefaultAnalyticsTableService(
        tableManager,
        organisationUnitService,
        dataElementService,
        resourceTableService,
        systemSettingManager);
  }

  @Bean("org.hisp.dhis.analytics.ValidationResultTableService")
  public AnalyticsTableService validationResultTableService(
      @Qualifier("org.hisp.dhis.analytics.ValidationResultAnalyticsTableManager")
          AnalyticsTableManager tableManager,
      OrganisationUnitService organisationUnitService,
      DataElementService dataElementService,
      ResourceTableService resourceTableService,
      SystemSettingManager systemSettingManager) {
    return new DefaultAnalyticsTableService(
        tableManager,
        organisationUnitService,
        dataElementService,
        resourceTableService,
        systemSettingManager);
  }

  @Bean("org.hisp.dhis.analytics.EnrollmentAnalyticsTableService")
  public AnalyticsTableService enrollmentAnalyticsTableManager(
      @Qualifier("org.hisp.dhis.analytics.EnrollmentAnalyticsTableManager")
          AnalyticsTableManager tableManager,
      OrganisationUnitService organisationUnitService,
      DataElementService dataElementService,
      ResourceTableService resourceTableService,
      SystemSettingManager systemSettingManager) {
    return new DefaultAnalyticsTableService(
        tableManager,
        organisationUnitService,
        dataElementService,
        resourceTableService,
        systemSettingManager);
  }
}
