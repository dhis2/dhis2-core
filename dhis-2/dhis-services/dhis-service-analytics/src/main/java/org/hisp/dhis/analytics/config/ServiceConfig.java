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
package org.hisp.dhis.analytics.config;

import org.hisp.dhis.analytics.AnalyticsTableManager;
import org.hisp.dhis.analytics.AnalyticsTableService;
import org.hisp.dhis.analytics.table.DefaultAnalyticsTableService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.db.AnalyticsSqlBuilderProvider;
import org.hisp.dhis.db.SqlBuilderProvider;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Luciano Fiandesio
 */
@Configuration("analyticsServiceConfig")
public class ServiceConfig {
  @Bean
  public SqlBuilder sqlBuilder(SqlBuilderProvider provider) {
    return provider.getSqlBuilder();
  }

  /**
   * A dedicated {@link org.hisp.dhis.db.sql.SqlBuilder} for Postgres. Used for classes that require
   * a Postgres-specific dialect, regardless of the selected analytics database.
   */
  @Bean("postgresSqlBuilder")
  public SqlBuilder postgresSqlBuilder() {
    return new PostgreSqlBuilder();
  }

  @Bean
  public AnalyticsSqlBuilder analyticsSqlBuilder(AnalyticsSqlBuilderProvider provider) {
    return provider.getAnalyticsSqlBuilder();
  }

  @Bean("org.hisp.dhis.analytics.TrackedEntityAnalyticsTableService")
  public AnalyticsTableService trackedEntityAnalyticsTableManager(
      @Qualifier("org.hisp.dhis.analytics.TrackedEntityAnalyticsTableManager")
          AnalyticsTableManager tableManager,
      OrganisationUnitService organisationUnitService,
      DataElementService dataElementService,
      ResourceTableService resourceTableService,
      SystemSettingsProvider settingsProvider,
      @Qualifier("postgresSqlBuilder") SqlBuilder sqlBuilder) {
    return new DefaultAnalyticsTableService(
        tableManager,
        organisationUnitService,
        dataElementService,
        resourceTableService,
        settingsProvider,
        sqlBuilder);
  }

  @Bean("org.hisp.dhis.analytics.TrackedEntityEventsAnalyticsTableService")
  public AnalyticsTableService trackedEntityEventsAnalyticsTableManager(
      @Qualifier("org.hisp.dhis.analytics.TrackedEntityEventsAnalyticsTableManager")
          AnalyticsTableManager tableManager,
      OrganisationUnitService organisationUnitService,
      DataElementService dataElementService,
      ResourceTableService resourceTableService,
      SystemSettingsProvider settingsProvider,
      @Qualifier("postgresSqlBuilder") SqlBuilder sqlBuilder) {
    return new DefaultAnalyticsTableService(
        tableManager,
        organisationUnitService,
        dataElementService,
        resourceTableService,
        settingsProvider,
        sqlBuilder);
  }

  @Bean("org.hisp.dhis.analytics.TrackedEntityEnrollmentsAnalyticsTableService")
  public AnalyticsTableService trackedEntityEnrollmentsAnalyticsTableManager(
      @Qualifier("org.hisp.dhis.analytics.TrackedEntityEnrollmentsAnalyticsTableManager")
          AnalyticsTableManager tableManager,
      OrganisationUnitService organisationUnitService,
      DataElementService dataElementService,
      ResourceTableService resourceTableService,
      SystemSettingsProvider settingsProvider,
      @Qualifier("postgresSqlBuilder") SqlBuilder sqlBuilder) {
    return new DefaultAnalyticsTableService(
        tableManager,
        organisationUnitService,
        dataElementService,
        resourceTableService,
        settingsProvider,
        sqlBuilder);
  }

  @Bean("org.hisp.dhis.analytics.AnalyticsTableService")
  public AnalyticsTableService analyticsTableService(
      @Qualifier("org.hisp.dhis.analytics.AnalyticsTableManager")
          AnalyticsTableManager tableManager,
      OrganisationUnitService organisationUnitService,
      DataElementService dataElementService,
      ResourceTableService resourceTableService,
      SystemSettingsProvider settingsProvider,
      SqlBuilder sqlBuilder) {
    return new DefaultAnalyticsTableService(
        tableManager,
        organisationUnitService,
        dataElementService,
        resourceTableService,
        settingsProvider,
        sqlBuilder);
  }

  @Bean("org.hisp.dhis.analytics.CompletenessTableService")
  public AnalyticsTableService completenessTableService(
      @Qualifier("org.hisp.dhis.analytics.CompletenessTableManager")
          AnalyticsTableManager tableManager,
      OrganisationUnitService organisationUnitService,
      DataElementService dataElementService,
      ResourceTableService resourceTableService,
      SystemSettingsProvider settingsProvider,
      SqlBuilder sqlBuilder) {
    return new DefaultAnalyticsTableService(
        tableManager,
        organisationUnitService,
        dataElementService,
        resourceTableService,
        settingsProvider,
        sqlBuilder);
  }

  @Bean("org.hisp.dhis.analytics.CompletenessTargetTableService")
  public AnalyticsTableService completenessTargetTableService(
      @Qualifier("org.hisp.dhis.analytics.CompletenessTargetTableManager")
          AnalyticsTableManager tableManager,
      OrganisationUnitService organisationUnitService,
      DataElementService dataElementService,
      ResourceTableService resourceTableService,
      SystemSettingsProvider settingsProvider,
      SqlBuilder sqlBuilder) {
    return new DefaultAnalyticsTableService(
        tableManager,
        organisationUnitService,
        dataElementService,
        resourceTableService,
        settingsProvider,
        sqlBuilder);
  }

  @Bean("org.hisp.dhis.analytics.OrgUnitTargetTableService")
  public AnalyticsTableService orgUnitTargetTableService(
      @Qualifier("org.hisp.dhis.analytics.OrgUnitTargetTableManager")
          AnalyticsTableManager tableManager,
      OrganisationUnitService organisationUnitService,
      DataElementService dataElementService,
      ResourceTableService resourceTableService,
      SystemSettingsProvider settingsProvider,
      SqlBuilder sqlBuilder) {
    return new DefaultAnalyticsTableService(
        tableManager,
        organisationUnitService,
        dataElementService,
        resourceTableService,
        settingsProvider,
        sqlBuilder);
  }

  @Bean("org.hisp.dhis.analytics.OwnershipAnalyticsTableService")
  public AnalyticsTableService ownershipAnalyticsTableManager(
      @Qualifier("org.hisp.dhis.analytics.OwnershipAnalyticsTableManager")
          AnalyticsTableManager tableManager,
      OrganisationUnitService organisationUnitService,
      DataElementService dataElementService,
      ResourceTableService resourceTableService,
      SystemSettingsProvider settingsProvider,
      SqlBuilder sqlBuilder) {
    return new DefaultAnalyticsTableService(
        tableManager,
        organisationUnitService,
        dataElementService,
        resourceTableService,
        settingsProvider,
        sqlBuilder);
  }

  @Bean("org.hisp.dhis.analytics.EventAnalyticsTableService")
  public AnalyticsTableService eventAnalyticsTableService(
      @Qualifier("org.hisp.dhis.analytics.EventAnalyticsTableManager")
          AnalyticsTableManager tableManager,
      OrganisationUnitService organisationUnitService,
      DataElementService dataElementService,
      ResourceTableService resourceTableService,
      SystemSettingsProvider settingsProvider,
      SqlBuilder sqlBuilder) {
    return new DefaultAnalyticsTableService(
        tableManager,
        organisationUnitService,
        dataElementService,
        resourceTableService,
        settingsProvider,
        sqlBuilder);
  }

  @Bean("org.hisp.dhis.analytics.ValidationResultTableService")
  public AnalyticsTableService validationResultTableService(
      @Qualifier("org.hisp.dhis.analytics.ValidationResultAnalyticsTableManager")
          AnalyticsTableManager tableManager,
      OrganisationUnitService organisationUnitService,
      DataElementService dataElementService,
      ResourceTableService resourceTableService,
      SystemSettingsProvider settingsProvider,
      SqlBuilder sqlBuilder) {
    return new DefaultAnalyticsTableService(
        tableManager,
        organisationUnitService,
        dataElementService,
        resourceTableService,
        settingsProvider,
        sqlBuilder);
  }

  @Bean("org.hisp.dhis.analytics.EnrollmentAnalyticsTableService")
  public AnalyticsTableService enrollmentAnalyticsTableManager(
      @Qualifier("org.hisp.dhis.analytics.EnrollmentAnalyticsTableManager")
          AnalyticsTableManager tableManager,
      OrganisationUnitService organisationUnitService,
      DataElementService dataElementService,
      ResourceTableService resourceTableService,
      SystemSettingsProvider settingsProvider,
      SqlBuilder sqlBuilder) {
    return new DefaultAnalyticsTableService(
        tableManager,
        organisationUnitService,
        dataElementService,
        resourceTableService,
        settingsProvider,
        sqlBuilder);
  }
}
