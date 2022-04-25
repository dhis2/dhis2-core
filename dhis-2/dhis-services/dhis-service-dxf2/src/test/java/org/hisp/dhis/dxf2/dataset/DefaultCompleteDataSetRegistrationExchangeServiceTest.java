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
package org.hisp.dhis.dxf2.dataset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.DhisConvenienceTest.assertIllegalQueryEx;
import static org.hisp.dhis.DhisConvenienceTest.createCategoryCombo;
import static org.hisp.dhis.DhisConvenienceTest.createCategoryOption;
import static org.hisp.dhis.DhisConvenienceTest.createCategoryOptionCombo;
import static org.hisp.dhis.DhisConvenienceTest.createDataSet;
import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnit;
import static org.hisp.dhis.DhisConvenienceTest.createPeriod;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import org.hisp.dhis.cache.CacheBuilderProvider;
import org.hisp.dhis.cache.DefaultCacheBuilderProvider;
import org.hisp.dhis.cache.DefaultCacheProvider;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.notifications.DataSetNotificationEventPublisher;
import org.hisp.dhis.datavalue.DefaultAggregateAccessManager;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.jdbc.batchhandler.CompleteDataSetRegistrationBatchHandler;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.quick.BatchHandler;
import org.hisp.quick.BatchHandlerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import com.google.common.collect.Sets;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith( MockitoExtension.class )
class DefaultCompleteDataSetRegistrationExchangeServiceTest
{
    @Mock
    private CompleteDataSetRegistrationExchangeStore cdsrStore;

    @Mock
    private IdentifiableObjectManager idObjManager;

    @Mock
    private OrganisationUnitService orgUnitService;

    @Mock
    private Notifier notifier;

    @Mock
    private I18nManager i18nManager;

    @Mock
    private BatchHandlerFactory batchHandlerFactory;

    @Mock
    private SystemSettingManager systemSettingManager;

    @Mock
    private CategoryService categoryService;

    @Mock
    private PeriodService periodService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private CompleteDataSetRegistrationService registrationService;

    @Mock
    private DataSetNotificationEventPublisher notificationPublisher;

    @Mock
    private MessageService messageService;

    @Mock
    private I18n i18n;

    @Mock
    private BatchHandler<CompleteDataSetRegistration> batchHandler;

    // Cache mocks //

    @Mock
    private CachingMap<String, DataSet> datasetCache;

    @Mock
    private CachingMap<String, Period> periodCache;

    @Mock
    private CachingMap<String, OrganisationUnit> orgUnitCache;

    @Mock
    private CachingMap<String, Boolean> orgUnitInHierarchyCache;

    @Mock
    private CachingMap<String, Boolean> attrOptComboOrgUnitCache;

    // this one is not a mock, because we are interested in triggering
    // the actual fetching of the cache element from the database
    private CachingMap<String, CategoryOptionCombo> aocCache;

    @Mock
    private DhisConfigurationProvider dhisConfigurationProvider;

    @Mock
    private Environment environment;

    @Mock
    private AclService aclService;

    private User user;

    private DefaultCompleteDataSetRegistrationExchangeService subject;

    private CategoryOptionCombo DEFAULT_COC;

    @BeforeEach
    public void setUp()
    {
        user = new User();

        when( environment.getActiveProfiles() ).thenReturn( new String[] { "test" } );
        when( dhisConfigurationProvider.getProperty( ConfigurationKey.SYSTEM_CACHE_MAX_SIZE_FACTOR ) )
            .thenReturn( "1" );
        CacheBuilderProvider cacheBuilderProvider = new DefaultCacheBuilderProvider();

        DefaultCacheProvider cacheContext = new DefaultCacheProvider( cacheBuilderProvider, environment,
            dhisConfigurationProvider );
        InputUtils inputUtils = new InputUtils( categoryService, idObjManager, cacheContext );

        DefaultAggregateAccessManager aggregateAccessManager = new DefaultAggregateAccessManager( aclService,
            cacheContext );

        subject = new DefaultCompleteDataSetRegistrationExchangeService( cdsrStore, idObjManager, orgUnitService,
            notifier, i18nManager, batchHandlerFactory, systemSettingManager, categoryService, periodService,
            currentUserService, registrationService, inputUtils, aggregateAccessManager, notificationPublisher,
            messageService, JacksonObjectMapperConfig.staticJsonMapper(), orgUnitService );

        DEFAULT_COC = new CategoryOptionCombo();
    }

    @Test
    void verifyUserHasNoWritePermissionOnCategoryOption()
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        DataSet dataSetA = createDataSet( 'A', new MonthlyPeriodType() );
        CategoryCombo categoryCombo = createCategoryCombo( 'A' );
        CategoryOption categoryOptionA = createCategoryOption( 'A' );
        CategoryOption categoryOptionB = createCategoryOption( 'B' );
        CategoryOptionCombo categoryOptionCombo = createCategoryOptionCombo( categoryCombo, categoryOptionA,
            categoryOptionB );
        Period period = createPeriod( "201907" );

        String payload = createPayload( period, organisationUnit, dataSetA, categoryCombo, categoryOptionA,
            categoryOptionB );

        try ( MockedConstruction<MetadataCaches> mocked = mockConstruction( MetadataCaches.class,
            ( mock, context ) -> {
                when( mock.getDataSets() ).thenReturn( datasetCache );
                when( mock.getPeriods() ).thenReturn( periodCache );
                when( mock.getOrgUnits() ).thenReturn( orgUnitCache );
                aocCache = new CachingMap<>();
                when( mock.getAttrOptionCombos() ).thenReturn( aocCache );
                when( mock.getOrgUnitInHierarchyMap() ).thenReturn( orgUnitInHierarchyCache );
                when( mock.getAttrOptComboOrgUnitMap() ).thenReturn( attrOptComboOrgUnitCache );
            } ) )
        {
            when( currentUserService.getCurrentUser() ).thenReturn( user );
            when( batchHandler.init() ).thenReturn( batchHandler );
            when( idObjManager.get( CategoryCombo.class, categoryCombo.getUid() ) ).thenReturn( categoryCombo );
            when( idObjManager.getObject( CategoryOption.class, IdScheme.UID, categoryOptionA.getUid() ) )
                .thenReturn( categoryOptionA );
            when( idObjManager.getObject( CategoryOption.class, IdScheme.UID, categoryOptionB.getUid() ) )
                .thenReturn( categoryOptionB );

            when( categoryService.getCategoryOptionCombo( categoryCombo,
                Sets.newHashSet( categoryOptionA, categoryOptionB ) ) ).thenReturn( categoryOptionCombo );

            when( datasetCache.get( eq( dataSetA.getUid() ), any() ) ).thenReturn( dataSetA );
            when( periodCache.get( eq( period.getIsoDate() ), any() ) ).thenReturn( period );
            when( orgUnitCache.get( eq( organisationUnit.getUid() ), any() ) )
                .thenReturn( createOrganisationUnit( 'A' ) );

            when( orgUnitInHierarchyCache.get( eq( organisationUnit.getUid() ), any() ) ).thenReturn( Boolean.TRUE );
            when(
                attrOptComboOrgUnitCache.get( eq( categoryOptionCombo.getUid() + organisationUnit.getUid() ), any() ) )
                    .thenReturn( Boolean.TRUE );
            when( categoryService.getCategoryOptionCombo( categoryOptionCombo.getUid() ) )
                .thenReturn( categoryOptionCombo );

            // force error on access check for Category Option Combo
            when( aclService.canDataWrite( user, dataSetA ) ).thenReturn( true );
            when( aclService.canDataWrite( user, categoryOptionA ) ).thenReturn( false );
            when( aclService.canDataWrite( user, categoryOptionB ) ).thenReturn( true );

            when( notifier.clear( null ) ).thenReturn( notifier );
            when( systemSettingManager.getBoolSetting( SettingKey.DATA_IMPORT_STRICT_PERIODS ) )
                .thenReturn( false );
            when( systemSettingManager.getBoolSetting( SettingKey.DATA_IMPORT_STRICT_ATTRIBUTE_OPTION_COMBOS ) )
                .thenReturn( false );
            when( systemSettingManager.getBoolSetting( SettingKey.DATA_IMPORT_STRICT_ORGANISATION_UNITS ) )
                .thenReturn( false );
            when( systemSettingManager.getBoolSetting( SettingKey.DATA_IMPORT_REQUIRE_ATTRIBUTE_OPTION_COMBO ) )
                .thenReturn( false );

            when( currentUserService.getCurrentUserOrganisationUnits() )
                .thenReturn( Collections.singleton( createOrganisationUnit( 'A' ) ) );
            when( i18nManager.getI18n() ).thenReturn( i18n );

            when( categoryService.getDefaultCategoryOptionCombo() ).thenReturn( DEFAULT_COC );
            when( batchHandlerFactory.createBatchHandler( CompleteDataSetRegistrationBatchHandler.class ) )
                .thenReturn( batchHandler );

            when( notifier.notify( any(), anyString() ) ).thenReturn( notifier );
            when( notifier.notify( null, NotificationLevel.INFO, "Import done", true ) ).thenReturn( notifier );

            // call method under test
            ImportSummary summary = subject.saveCompleteDataSetRegistrationsJson(
                new ByteArrayInputStream( payload.getBytes() ), new ImportOptions() );

            assertThat( summary.getStatus(), is( ImportStatus.ERROR ) );
            assertThat( summary.getImportCount().getIgnored(), is( 1 ) );
            assertEquals( 1, summary.getConflictCount() );
            assertThat( summary.getConflicts().iterator().next().getValue(),
                is( "User has no data write access for CategoryOption: " + categoryOptionA.getUid() ) );
        }
    }

    @Test
    void testValidateAssertMissingDataSet()
    {
        ExportParams params = new ExportParams()
            .setOrganisationUnits( Sets.newHashSet( new OrganisationUnit() ) )
            .setPeriods( Sets.newHashSet( new Period() ) );

        assertIllegalQueryEx(
            assertThrows( IllegalQueryException.class, () -> subject.validate( params ) ),
            ErrorCode.E2013 );
    }

    private String createPayload( Period period, OrganisationUnit organisationUnit, DataSet dataSet,
        CategoryCombo categoryCombo, CategoryOption... categoryOptions )
    {
        return "{\"completeDataSetRegistrations\":[{\"cc\":\"" + categoryCombo.getUid() + "\","
            + "\"cp\":\""
            + Arrays.stream( categoryOptions ).map( CategoryOption::getUid ).collect( Collectors.joining( ";" ) )
            + "\"," + "\"dataSet\":\"" + dataSet.getUid() + "\"," + "\"period\":\"" + period.getIsoDate() + "\","
            + "\"organisationUnit\":\"" + organisationUnit.getUid() + "\"," + "\"completed\":true}]}";
    }

}
