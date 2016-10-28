package org.hisp.dhis.dxf2.dataset;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.jdbc.batchhandler.CompleteDataSetRegistrationBatchHandler;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.callable.CategoryOptionComboAclCallable;
import org.hisp.dhis.system.callable.IdentifiableObjectCallable;
import org.hisp.dhis.system.callable.PeriodCallable;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.quick.BatchHandler;
import org.hisp.quick.BatchHandlerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Set;

/**
 * @author Halvdan Hoem Grelland
 */
public class DefaultCompleteDataSetRegistrationService
    implements CompleteDataSetRegistrationService
{
    private static final Log log = LogFactory.getLog( DefaultCompleteDataSetRegistrationService.class );

    private static final int CACHE_MISS_THRESHOLD = 500; // Arbitrarily chosen from dxf2 DefaultDataValueSetService

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private CompleteDataSetRegistrationStore cdsrStore;

    @Autowired
    private IdentifiableObjectManager idObjManager;

    @Autowired
    private OrganisationUnitService orgUnitService;

    @Autowired
    private Notifier notifier;

    @Autowired
    private I18nManager i18nManager;

    @Autowired
    private BatchHandlerFactory batchHandlerFactory;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private CurrentUserService currentUserService;

    // -------------------------------------------------------------------------
    // CompleteDataSetRegistrationService implementation
    // -------------------------------------------------------------------------

    @Override
    public ExportParams getFromUrl( Set<String> dataSets, Set<String> orgUnits, Set<String> orgUnitGroups, Set<String> periods,
        Date startDate, Date endDate, boolean includeChildren, Date created, String createdDuration, Integer limit, IdSchemes idSchemes )
    {
        ExportParams params = new ExportParams();

        if ( dataSets != null )
        {
            params.getDataSets().addAll( idObjManager.getObjects( DataSet.class, IdentifiableProperty.UID, dataSets ) );
        }

        if ( orgUnits != null )
        {
            params.getOrganisationUnits().addAll( idObjManager.getObjects( OrganisationUnit.class, IdentifiableProperty.UID, orgUnits ) );
        }

        if ( orgUnitGroups != null )
        {
            params.getOrganisationUnitGroups().addAll(
                idObjManager.getObjects( OrganisationUnitGroup.class, IdentifiableProperty.UID, orgUnitGroups ) );
        }

        if ( periods != null && !periods.isEmpty() )
        {
            params.getPeriods().addAll( idObjManager.getObjects( Period.class, IdentifiableProperty.UID, periods ) );
        }

        if ( startDate != null && endDate != null )
        {
            params
                .setStartDate( startDate )
                .setEndDate( endDate );
        }

        params
            .setIncludeChildren( includeChildren )
            .setCreated( created )
            .setCreatedDuration( createdDuration )
            .setLimit( limit )
            .setOutputIdSchemes( idSchemes );

        return params;
    }

    public void validate( ExportParams params ) throws IllegalQueryException
    {
        if ( params == null )
        {
            throw new IllegalArgumentException( "ExportParams must be non-null" );
        }

        if ( params.getDataSets().isEmpty() )
        {
            validationError( "At least one data set must be specified" );
        }

        if ( !params.hasPeriods() && !params.hasStartEndDate() && !params.hasCreated() && !params.hasCreatedDuration() )
        {
            validationError( "At least one valid period, start/end dates, created or created duration must be specified" );
        }

        if ( params.hasPeriods() && params.hasStartEndDate() )
        {
            validationError( "Both periods and start/end date cannot be specified" );
        }

        if ( params.hasStartEndDate() && params.getStartDate().after( params.getEndDate() ) )
        {
            validationError( "Start date must be before end date" );
        }

        if ( params.hasCreatedDuration() && DateUtils.getDuration( params.getCreatedDuration() ) == null )
        {
            validationError( "Duration is not valid: " + params.getCreatedDuration() );
        }

        if ( !params.hasOrganisationUnits() && !params.hasOrganisationUnitGroups() )
        {
            validationError( "At least one valid organisation unit or organisation unit group must be specified" );
        }

        if ( params.isIncludeChildren() && params.hasOrganisationUnitGroups() )
        {
            validationError( "Children cannot be included for organisation unit groups" );
        }

        if ( params.isIncludeChildren() && !params.hasOrganisationUnits() )
        {
            validationError( "At least one organisation unit must be specified when children are included" );
        }

        if ( params.hasLimit() && params.getLimit() < 0 )
        {
            validationError( "Limit cannot be less than zero: " + params.getLimit() );
        }
    }

    @Override
    public void decideAccess( ExportParams params ) throws IllegalQueryException
    {
        for ( OrganisationUnit ou : params.getOrganisationUnits() )
        {
            if ( !orgUnitService.isInUserHierarchy( ou ) )
            {
                throw new IllegalQueryException( "User is not allowed to view org unit: " + ou.getUid() );
            }
        }
    }

    @Override
    public void writeCompleteDataSetRegistrationsXml( ExportParams params, OutputStream out )
    {
        decideAccess( params );
        validate( params );

        cdsrStore.writeCompleteDataSetRegistrationsXml( params, out );
    }

    @Override
    public void writeCompleteDataSetRegistrationsJson( ExportParams params, OutputStream out )
    {
        decideAccess( params );
        validate( params );

        cdsrStore.writeCompleteDataSetRegistrationsJson( params, out );
    }

    @Override
    public ImportSummary saveCompleteDataSetRegistrationsXml( InputStream in )
    {
        return saveCompleteDataSetRegistrationsXml( in, ImportOptions.getDefaultImportOptions(), null );
    }

    @Override
    public ImportSummary saveCompleteDataSetRegistrationsXml( InputStream in, ImportOptions importOptions )
    {
        return saveCompleteDataSetRegistrationsXml( in, importOptions, null );
    }

    @Override
    public ImportSummary saveCompleteDataSetRegistrationsXml( InputStream in, ImportOptions importOptions, TaskId taskId )
    {
        // TODO
        return null;
    }

    @Override
    public ImportSummary saveCompleteDataSetRegistrationsJson( InputStream in )
    {
        return saveCompleteDataSetRegistrationsJson( in, ImportOptions.getDefaultImportOptions(), null );
    }

    @Override
    public ImportSummary saveCompleteDataSetRegistrationsJson( InputStream in, ImportOptions importOptions )
    {
        return saveCompleteDataSetRegistrationsJson( in, ImportOptions.getDefaultImportOptions(), null );
    }

    @Override
    public ImportSummary saveCompleteDataSetRegistrationsJson( InputStream in, ImportOptions importOptions, TaskId taskId )
    {
        // TODO
        return null;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void validationError( String message ) throws IllegalQueryException
    {
        log.warn( "Validation error: " + message );

        throw new IllegalQueryException( message );
    }

    private ImportSummary saveCompleteDataSetRegistrations( ImportOptions importOptions, TaskId id, CompleteDataSetRegistrations completeRegistrations )
    {
        Clock clock = new Clock( log ).startClock().logTime( "Starting complete data set registration import, options: " + importOptions );
        notifier.clear( id ).notify( id, "Process started" );

        ImportSummary importSummary = new ImportSummary();

        I18n i18n = i18nManager.getI18n();

        // ---------------------------------------------------------------------
        // Set up import configuration
        // ---------------------------------------------------------------------

        importOptions = importOptions != null ? importOptions : ImportOptions.getDefaultImportOptions();

        log.info( "Import options: " + importOptions );

        ImportConfig cfg = new ImportConfig( completeRegistrations, importOptions );

        // ---------------------------------------------------------------------
        // Set up meta-data
        // ---------------------------------------------------------------------

        MetaDataCaches caches = new MetaDataCaches();
        MetaDataCallables metaDataCallables = new MetaDataCallables( cfg, completeRegistrations );

        if ( importOptions.isPreheatCacheDefaultFalse() )
        {
            caches.preheat( idObjManager, cfg );
        }

        // ---------------------------------------------------------------------
        // Get outer meta-data
        // ---------------------------------------------------------------------

        // ---------------------------------------------------------------------
        // Validation
        // ---------------------------------------------------------------------

        // TODO clock.logTime( "Validated out meta-data" ); etc etc.

        // ---------------------------------------------------------------------
        // Do import
        // ---------------------------------------------------------------------

        notifier.notify( id, "Importing complete data set registrations" );

        doImport( completeRegistrations, cfg, importOptions, id, metaDataCallables, caches );

    }

    private void doImport( CompleteDataSetRegistrations completeRegistrations, ImportConfig cfg, ImportOptions options, TaskId id,
        MetaDataCallables mdCallables, MetaDataCaches mdCaches )
    {
        final String currentUser = currentUserService.getCurrentUsername();
        final Set<OrganisationUnit> userOrgUnits = currentUserService.getCurrentUserOrganisationUnits();

        BatchHandler<CompleteDataSetRegistration> completeRegistrationsBatchHandler =
            batchHandlerFactory.createBatchHandler( CompleteDataSetRegistrationBatchHandler.class ).init();

        int importCount = 0, updateCount = 0, deleteCount = 0, totalCount = 0;

        Date now = new Date();

        while( completeRegistrations.hasNextCompleteDataSetRegistration() )
        {
            org.hisp.dhis.dxf2.dataset.CompleteDataSetRegistration cr = completeRegistrations.getNextCompleteDataSetRegistration();
            totalCount++;

            // ---------------------------------------------------------------------
            // Init meta-data properties against meta-data cache
            // ---------------------------------------------------------------------

            DataSet dataSet = null;
            Period period = null;
            OrganisationUnit orgUnit = null;
            DataElementCategoryOptionCombo attrOptCombo = null;

            initMetaDataProperties( cr, dataSet, period, orgUnit, attrOptCombo, mdCallables, mdCaches );

            // ---------------------------------------------------------------------
            // Cache heating
            // ---------------------------------------------------------------------

        }
    }

    private void heatCaches( MetaDataCaches caches, ImportConfig config )
    {
        if ( caches.dataSets.isCacheLoaded() && exceedsThreshold( caches.dataSets ) )
        {
            caches.dataSets.load( idObjManager.getAll( DataSet.class ), ds -> ds.getPropertyValue( config.dsScheme ) );
        }
    }

    private static void initMetaDataProperties( org.hisp.dhis.dxf2.dataset.CompleteDataSetRegistration cdsr, DataSet dataSet, Period period,
        OrganisationUnit orgUnit, DataElementCategoryOptionCombo attrOptcombo, MetaDataCallables callables, MetaDataCaches cache )
    {
        String
            ds = ttn( cdsr.getDataSet() ),
            pe = ttn( cdsr.getPeriod() ),
            ou = ttn( cdsr.getOrganisationUnit() ),
            aoc = ttn( cdsr.getAttributeOptionCombo() );

        dataSet = cache.dataSets.get( ds , callables.dataSetCallable.setId( ds ) );
        period = cache.periods.get( pe, callables.periodCallable.setId( pe ) );
        orgUnit = cache.orgUnits.get( ou, callables.orgUnitCallable.setId( ou ) );
        attrOptcombo = cache.attrOptionCombos.get( aoc, callables.optionComboCallable.setId( aoc ) );
    }

    private static boolean exceedsThreshold( CachingMap cachingMap )
    {
        return cachingMap.getCacheMissCount() > CACHE_MISS_THRESHOLD;
    }

    /**
     * Trim-to-null shorthand
     */
    private static String ttn( String str )
    {
        return StringUtils.trimToNull( str );
    }

    private class MetaDataCallables
    {
        final IdentifiableObjectCallable<DataSet> dataSetCallable;
        final IdentifiableObjectCallable<OrganisationUnit> orgUnitCallable;
        final IdentifiableObjectCallable<DataElementCategoryOptionCombo> optionComboCallable;
        final IdentifiableObjectCallable<Period> periodCallable;

        MetaDataCallables( ImportConfig config, CompleteDataSetRegistrations cdsr )
        {
            dataSetCallable = new IdentifiableObjectCallable<>( idObjManager, DataSet.class, config.dsScheme, null );
            orgUnitCallable = new IdentifiableObjectCallable<>( idObjManager, OrganisationUnit.class, config.ouScheme, null );
            optionComboCallable = new CategoryOptionComboAclCallable( categoryService, config.aocScheme, null );
            periodCallable = new PeriodCallable( periodService, null, null );
        }
    }

    private class MetaDataCaches
    {
        CachingMap<String, DataSet> dataSets = new CachingMap<>();
        CachingMap<String, OrganisationUnit> orgUnits = new CachingMap<>();
        CachingMap<String, Period> periods = new CachingMap<>();
        CachingMap<String, DataElementCategoryOptionCombo> attrOptionCombos = new CachingMap<>();

        void preheat( IdentifiableObjectManager manager, final ImportConfig config )
        {
            dataSets.load( manager.getAll( DataSet.class ), ds -> ds.getPropertyValue( config.dsScheme ) );
            orgUnits.load( manager.getAll( OrganisationUnit.class ), ou -> ou.getPropertyValue( config.ouScheme ) );
            attrOptionCombos.load( manager.getAll( DataElementCategoryOptionCombo.class ), oc -> oc.getPropertyValue( config.aocScheme ) );
        }
    }

    private class ImportConfig
    {
        IdScheme dsScheme, ouScheme, aocScheme;
        ImportStrategy strategy;
        boolean dryRun, skipExistingCheck, strictPeriods, strictAttrOptionCombos, strictOrgUnits, requireAttrOptionCombos;

        ImportConfig( CompleteDataSetRegistrations cdsr, ImportOptions options )
        {
            dsScheme = IdScheme.from( cdsr.getIdSchemeProperty() );
            ouScheme = IdScheme.from( cdsr.getOrgUnitIdSchemeProperty() );
            aocScheme = IdScheme.from( cdsr.getAttributeOptionComboIdSchemeProperty() );

            log.info(
                String.format( "Data set scheme: %s, org unit scheme: %s, attribute option combo scheme: %s", dsScheme, ouScheme, aocScheme )
            );

            strategy = cdsr.getStrategy() != null ?
                ImportStrategy.valueOf( cdsr.getStrategy() ) : options.getImportStrategy();

            dryRun = cdsr.getDryRun() != null ? cdsr.getDryRun() : options.isDryRun();

            skipExistingCheck = options.isSkipExistingCheck();
            strictPeriods = options.isStrictPeriods() || get( SettingKey.DATA_IMPORT_STRICT_PERIODS );
            strictAttrOptionCombos = options.isStrictAttributeOptionCombos() || get( SettingKey.DATA_IMPORT_STRICT_ATTRIBUTE_OPTION_COMBOS );
            strictOrgUnits = options.isStrictOrganisationUnits() || get( SettingKey.DATA_IMPORT_STRICT_ORGANISATION_UNITS );
            requireAttrOptionCombos = options.isRequireAttributeOptionCombo() || get( SettingKey.DATA_IMPORT_REQUIRE_ATTRIBUTE_OPTION_COMBO );
        }

        private boolean get( SettingKey key )
        {
            return (Boolean) systemSettingManager.getSystemSetting( key );
        }
    }
}
