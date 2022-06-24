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
import org.hisp.dhis.analytics.table.DefaultAnalyticsTableService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.notification.Notifier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Luciano Fiandesio
 */
@Configuration( "analyticsServiceConfig" )
public class ServiceConfig
{
    @Bean( "org.hisp.dhis.analytics.TrackedEntityInstanceAnalyticsTableService" )
    public DefaultAnalyticsTableService trackedEntityInstanceAnalyticsTableManager(
        @Qualifier( "org.hisp.dhis.analytics.TrackedEntityInstanceAnalyticsTableManager" ) AnalyticsTableManager tableManager,
        OrganisationUnitService organisationUnitService, DataElementService dataElementService,
        ResourceTableService resourceTableService, Notifier notifier, SystemSettingManager systemSettingManager )
    {
        return new DefaultAnalyticsTableService( tableManager, organisationUnitService, dataElementService,
            resourceTableService, systemSettingManager );
    }

    @Bean( "org.hisp.dhis.analytics.AnalyticsTableService" )
    public DefaultAnalyticsTableService analyticsTableService(
        @Qualifier( "org.hisp.dhis.analytics.AnalyticsTableManager" ) AnalyticsTableManager tableManager,
        OrganisationUnitService organisationUnitService, DataElementService dataElementService,
        ResourceTableService resourceTableService, Notifier notifier, SystemSettingManager systemSettingManager )
    {
        return new DefaultAnalyticsTableService( tableManager, organisationUnitService, dataElementService,
            resourceTableService, systemSettingManager );
    }

    @Bean( "org.hisp.dhis.analytics.CompletenessTableService" )
    public DefaultAnalyticsTableService completenessTableService(
        @Qualifier( "org.hisp.dhis.analytics.CompletenessTableManager" ) AnalyticsTableManager tableManager,
        OrganisationUnitService organisationUnitService, DataElementService dataElementService,
        ResourceTableService resourceTableService, Notifier notifier, SystemSettingManager systemSettingManager )
    {
        return new DefaultAnalyticsTableService( tableManager, organisationUnitService, dataElementService,
            resourceTableService, systemSettingManager );
    }

    @Bean( "org.hisp.dhis.analytics.CompletenessTargetTableService" )
    public DefaultAnalyticsTableService completenessTargetTableService(
        @Qualifier( "org.hisp.dhis.analytics.CompletenessTargetTableManager" ) AnalyticsTableManager tableManager,
        OrganisationUnitService organisationUnitService, DataElementService dataElementService,
        ResourceTableService resourceTableService, Notifier notifier, SystemSettingManager systemSettingManager )
    {
        return new DefaultAnalyticsTableService( tableManager, organisationUnitService, dataElementService,
            resourceTableService, systemSettingManager );
    }

    @Bean( "org.hisp.dhis.analytics.OrgUnitTargetTableService" )
    public DefaultAnalyticsTableService orgUnitTargetTableService(
        @Qualifier( "org.hisp.dhis.analytics.OrgUnitTargetTableManager" ) AnalyticsTableManager tableManager,
        OrganisationUnitService organisationUnitService, DataElementService dataElementService,
        ResourceTableService resourceTableService, Notifier notifier, SystemSettingManager systemSettingManager )
    {
        return new DefaultAnalyticsTableService( tableManager, organisationUnitService, dataElementService,
            resourceTableService, systemSettingManager );
    }

    @Bean( "org.hisp.dhis.analytics.EventAnalyticsTableService" )
    public DefaultAnalyticsTableService eventAnalyticsTableService(
        @Qualifier( "org.hisp.dhis.analytics.EventAnalyticsTableManager" ) AnalyticsTableManager tableManager,
        OrganisationUnitService organisationUnitService, DataElementService dataElementService,
        ResourceTableService resourceTableService, Notifier notifier, SystemSettingManager systemSettingManager )
    {
        return new DefaultAnalyticsTableService( tableManager, organisationUnitService, dataElementService,
            resourceTableService, systemSettingManager );
    }

    @Bean( "org.hisp.dhis.analytics.ValidationResultTableService" )
    public DefaultAnalyticsTableService validationResultTableService(
        @Qualifier( "org.hisp.dhis.analytics.ValidationResultAnalyticsTableManager" ) AnalyticsTableManager tableManager,
        OrganisationUnitService organisationUnitService, DataElementService dataElementService,
        ResourceTableService resourceTableService, Notifier notifier, SystemSettingManager systemSettingManager )
    {
        return new DefaultAnalyticsTableService( tableManager, organisationUnitService, dataElementService,
            resourceTableService, systemSettingManager );
    }

    @Bean( "org.hisp.dhis.analytics.EnrollmentAnalyticsTableService" )
    public DefaultAnalyticsTableService enrollmentAnalyticsTableManager(
        @Qualifier( "org.hisp.dhis.analytics.EnrollmentAnalyticsTableManager" ) AnalyticsTableManager tableManager,
        OrganisationUnitService organisationUnitService, DataElementService dataElementService,
        ResourceTableService resourceTableService, Notifier notifier, SystemSettingManager systemSettingManager )
    {
        return new DefaultAnalyticsTableService( tableManager, organisationUnitService, dataElementService,
            resourceTableService, systemSettingManager );
    }
}
