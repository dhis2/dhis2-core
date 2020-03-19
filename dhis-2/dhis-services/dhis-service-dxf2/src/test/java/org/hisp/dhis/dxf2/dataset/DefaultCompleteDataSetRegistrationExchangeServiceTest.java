package org.hisp.dhis.dxf2.dataset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.DhisConvenienceTest.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.notifications.DataSetNotificationEventPublisher;
import org.hisp.dhis.datavalue.DefaultAggregateAccessManager;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.utils.InputUtils;
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.env.Environment;

import com.google.common.collect.Sets;

@RunWith( PowerMockRunner.class )
@PrepareForTest( DefaultCompleteDataSetRegistrationExchangeService.class )
@PowerMockIgnore("javax.management.*")
public class DefaultCompleteDataSetRegistrationExchangeServiceTest
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
    private BatchHandler batchHandler;

    @Mock
    private MetaDataCaches metaDataCaches;

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
    private Environment environment;

    @Mock
    private AclService aclService;

    private User user;

    private DefaultCompleteDataSetRegistrationExchangeService subject;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private CategoryOptionCombo DEFAULT_COC;

    @Before
    @SuppressWarnings( "unchecked" )
    public void setUp()
    {
        user = new User();

        when( environment.getActiveProfiles() ).thenReturn( new String[] { "test" } );
        when( currentUserService.getCurrentUser() ).thenReturn( user );
        InputUtils inputUtils = new InputUtils( categoryService, idObjManager );

        DefaultAggregateAccessManager aggregateAccessManager = new DefaultAggregateAccessManager( aclService );

        subject = new DefaultCompleteDataSetRegistrationExchangeService( cdsrStore, idObjManager, orgUnitService,
            notifier, i18nManager, batchHandlerFactory, systemSettingManager, categoryService, periodService,
            currentUserService, registrationService, inputUtils, aggregateAccessManager, notificationPublisher,
            messageService );

        DEFAULT_COC = new CategoryOptionCombo();

        when( notifier.clear( null ) ).thenReturn( notifier );
        when( systemSettingManager.getSystemSetting( SettingKey.DATA_IMPORT_STRICT_PERIODS ) ).thenReturn( false );
        when( systemSettingManager.getSystemSetting( SettingKey.DATA_IMPORT_STRICT_ATTRIBUTE_OPTION_COMBOS ) )
            .thenReturn( false );
        when( systemSettingManager.getSystemSetting( SettingKey.DATA_IMPORT_STRICT_ORGANISATION_UNITS ) )
            .thenReturn( false );
        when( systemSettingManager.getSystemSetting( SettingKey.DATA_IMPORT_REQUIRE_ATTRIBUTE_OPTION_COMBO ) )
            .thenReturn( false );

        when( currentUserService.getCurrentUsername() ).thenReturn( "john1" );
        when( currentUserService.getCurrentUserOrganisationUnits() )
            .thenReturn( Collections.singleton( createOrganisationUnit( 'A' ) ) );
        when( i18nManager.getI18n() ).thenReturn( i18n );

        when( categoryService.getDefaultCategoryOptionCombo() ).thenReturn( DEFAULT_COC );
        when( batchHandlerFactory.createBatchHandler( CompleteDataSetRegistrationBatchHandler.class ) )
            .thenReturn( batchHandler );
        when( batchHandler.init() ).thenReturn( batchHandler );

        // caches
        when( metaDataCaches.getDataSets() ).thenReturn( datasetCache );
        when( metaDataCaches.getPeriods() ).thenReturn( periodCache );
        when( metaDataCaches.getOrgUnits() ).thenReturn( orgUnitCache );
        aocCache = new CachingMap<>();
        when( metaDataCaches.getAttrOptionCombos() ).thenReturn( aocCache );
        when( metaDataCaches.getOrgUnitInHierarchyMap() ).thenReturn( orgUnitInHierarchyCache );
        when( metaDataCaches.getAttrOptComboOrgUnitMap() ).thenReturn( attrOptComboOrgUnitCache );

        //
        when( notifier.notify( null, NotificationLevel.INFO, "Import done", true ) ).thenReturn( notifier );
    }

    @Test
    public void verifyUserHasNoWritePermissionOnCategoryOption()
        throws Exception
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        DataSet dataSetA = createDataSet( 'A', new MonthlyPeriodType() );
        CategoryCombo categoryCombo = createCategoryCombo( 'A' );
        CategoryOption categoryOptionA = createCategoryOption( 'A' );
        CategoryOption categoryOptionB = createCategoryOption( 'B' );
        CategoryOptionCombo categoryOptionCombo = createCategoryOptionCombo( categoryCombo, categoryOptionA,
            categoryOptionB );
        Period period = createPeriod( "201907" );

        String payload = cretePayload( period, organisationUnit, dataSetA, categoryCombo, categoryOptionA, categoryOptionB );

        whenNew( MetaDataCaches.class ).withNoArguments().thenReturn( metaDataCaches );

        when( idObjManager.get( CategoryCombo.class, categoryCombo.getUid() ) ).thenReturn( categoryCombo );
        when( idObjManager.getObject( CategoryOption.class, IdScheme.UID, categoryOptionA.getUid() ) )
            .thenReturn( categoryOptionA );
        when( idObjManager.getObject( CategoryOption.class, IdScheme.UID, categoryOptionB.getUid() ) )
            .thenReturn( categoryOptionB );

        when( categoryService.getCategoryOptionCombo( categoryCombo,
            Sets.newHashSet( categoryOptionA, categoryOptionB ) ) ).thenReturn( categoryOptionCombo );

        when( datasetCache.get( eq( dataSetA.getUid() ), Mockito.any( ) )).thenReturn( dataSetA );
        when( periodCache.get( eq( period.getIsoDate() ), Mockito.any( ) ) ).thenReturn( period );
        when( orgUnitCache.get( eq( organisationUnit.getUid() ), Mockito.any( ) ) ).thenReturn( createOrganisationUnit( 'A' ) );

        when( orgUnitInHierarchyCache.get( eq( organisationUnit.getUid() ), Mockito.any( ) ) ).thenReturn( Boolean.TRUE );
        when( attrOptComboOrgUnitCache.get( eq( categoryOptionCombo.getUid() + organisationUnit.getUid() ), Mockito.any( ) ) )
            .thenReturn( Boolean.TRUE );
        when( categoryService.getCategoryOptionCombo( categoryOptionCombo.getUid() ) )
            .thenReturn( categoryOptionCombo );

        // force error on access check for Category Option Combo
        when( aclService.canDataWrite( user, dataSetA ) ).thenReturn( true );
        when( aclService.canDataWrite( user, categoryOptionA ) ).thenReturn( false );
        when( aclService.canDataWrite( user, categoryOptionB ) ).thenReturn( true );

        // call method under test
        ImportSummary summary = subject.saveCompleteDataSetRegistrationsJson(
            new ByteArrayInputStream( payload.getBytes() ), new ImportOptions() );

        assertThat( summary.getStatus(), is( ImportStatus.ERROR ) );
        assertThat( summary.getImportCount().getIgnored(), is( 1 ) );
        assertThat( summary.getConflicts(), hasSize( 1 ) );
        assertThat( summary.getConflicts().iterator().next().getValue(),
            is( "User has no data write access for CategoryOption: " + categoryOptionA.getUid() ) );
    }

    private String cretePayload( Period period, OrganisationUnit organisationUnit, DataSet dataSet,
        CategoryCombo categoryCombo, CategoryOption... categoryOptions )
    {
        return "{\"completeDataSetRegistrations\":[{\"cc\":\"" + categoryCombo.getUid() + "\","
            + "\"cp\":\"" + Arrays.stream( categoryOptions ).map( CategoryOption::getUid ).collect( Collectors.joining( ";" ) )
            + "\"," + "\"dataSet\":\"" + dataSet.getUid() + "\"," + "\"period\":\"" + period.getIsoDate() + "\","
            + "\"organisationUnit\":\"" + organisationUnit.getUid() + "\"," + "\"completed\":true}]}";
    }

}