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
import org.hisp.dhis.common.DateRange;
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
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportCount;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
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
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.system.util.ValidationUtils;
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

        // ---------------------------------------------------------------------
        // Set up import configuration
        // ---------------------------------------------------------------------

        importOptions = importOptions != null ? importOptions : ImportOptions.getDefaultImportOptions();

        log.info( "Import options: " + importOptions );

        ImportConfig cfg = new ImportConfig( completeRegistrations, importOptions, systemSettingManager );

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

        int totalCount = doImport( completeRegistrations, cfg, importSummary, metaDataCallables, caches );

        notifier.notify( id, NotificationLevel.INFO, "Import done", true ).addTaskSummary( id, importSummary );

        ImportCount count = importSummary.getImportCount();

        clock.logTime( String.format( "Complete data set registration import done, total: %d, imported: %d, updated: %d, deleted: %d",
            totalCount, count.getImported(), count.getUpdated(), count.getDeleted() ) );

        completeRegistrations.close();

        return importSummary;
    }

    private int doImport( CompleteDataSetRegistrations completeRegistrations, ImportConfig config,
        ImportSummary summary, MetaDataCallables mdCallables, MetaDataCaches mdCaches )
    {
        final String currentUser = currentUserService.getCurrentUsername();
        final Set<OrganisationUnit> userOrgUnits = currentUserService.getCurrentUserOrganisationUnits();
        final I18n i18n = i18nManager.getI18n();

        BatchHandler<CompleteDataSetRegistration> batchHandler =
            batchHandlerFactory.createBatchHandler( CompleteDataSetRegistrationBatchHandler.class ).init();

        int importCount = 0, updateCount = 0, deleteCount = 0, totalCount = 0;

        Date now = new Date();

        DataElementCategoryOptionCombo fallbackCategoryOptionCombo = categoryService.getDefaultDataElementCategoryOptionCombo();

        while ( completeRegistrations.hasNextCompleteDataSetRegistration() )
        {
            org.hisp.dhis.dxf2.dataset.CompleteDataSetRegistration cdsr = completeRegistrations.getNextCompleteDataSetRegistration();
            totalCount++;

            // ---------------------------------------------------------------------
            // Init meta-data properties against meta-data cache
            // ---------------------------------------------------------------------

            MetaDataProperties mdProps = initMetaDataProperties( cdsr, mdCallables, mdCaches );

            heatCaches( mdCaches, config );

            // ---------------------------------------------------------------------
            // Meta-data validation
            // ---------------------------------------------------------------------

            boolean metaDataPropsIsValid = mdProps.isValid( summary, cdsr );

            if ( !metaDataPropsIsValid )
            {
                continue;
            }

            boolean inUserHierarchy =
                mdCaches.orgUnitInHierarchyMap.get( mdProps.orgUnit.getUid(), () -> mdProps.orgUnit.isDescendant( userOrgUnits ) );

            if ( !inUserHierarchy )
            {
                summary.getConflicts().add(
                    new ImportConflict( mdProps.orgUnit.getUid(), "Organisation unit is not in hierarchy of user: " + currentUser ) );
                continue;
            }

            // -----------------------------------------------------------------
            // Constraints validation
            // -----------------------------------------------------------------

            if ( mdProps.attrOptCombo == null )
            {
                if ( config.requireAttrOptionCombos )
                {
                    summary.getConflicts().add(
                        new ImportConflict( "Attribute option combo", "Attribute option combo is required but is not specified" ) );
                    continue;
                }
                else
                {
                    mdProps.attrOptCombo = fallbackCategoryOptionCombo;
                }
            }

            if ( config.strictPeriods && !hasMatchingPeriodTypes( mdProps, mdCaches, summary ) )
            {
                continue;
            }

            // TODO Consider validating attrOptCombos and data set assignment (like below)
//
//            if ( strictAttrOptionCombos && !dataElementAttrOptionComboMap.get( dataElement.getUid(),
//                () -> dataElement.getDataSetCategoryOptionCombos() ).contains( attrOptionCombo ) )
//            {
//                summary.getConflicts().add( new ImportConflict( attrOptionCombo.getUid(),
//                    "Attribute option combo: " + attrOptionCombo.getUid() + " must be part of category combo of data sets of data element: " + dataElement.getUid() ) );
//                continue;
//            }
//
//            if ( strictOrgUnits && BooleanUtils.isFalse( dataElementOrgUnitMap.get( dataElement.getUid() + orgUnit.getUid(),
//                () -> orgUnit.hasDataElement( dataElement ) ) ) )
//            {
//                summary.getConflicts().add( new ImportConflict( orgUnit.getUid(),
//                    "Data element: " + dataElement.getUid() + " must be assigned through data sets to organisation unit: " + orgUnit.getUid() ) );
//                continue;
//            }

            String storedBy = cdsr.getStoredBy();

            if ( !validateStoredBy( storedBy, summary, i18n ) )
            {
                continue;
            }

            cdsr.setStoredBy( StringUtils.isBlank( storedBy ) ? currentUser : storedBy );

            if ( !validateAttrOptCombo( mdProps, mdCaches, summary ) )
            {
                continue;
            }

            // TODO Check if Period is within range of data set?

            // -----------------------------------------------------------------
            // Create complete data set registration
            // -----------------------------------------------------------------

            CompleteDataSetRegistration internalCdsr = createCompleteDataSetRegistration( cdsr, mdProps, now );

            CompleteDataSetRegistration existingCdsr = config.skipExistingCheck ? null : batchHandler.findObject( internalCdsr );

            ImportStrategy strategy = config.strategy;

            boolean isDryRun = config.dryRun;

            if ( !config.skipExistingCheck && existingCdsr != null )
            {
                // CDSR already exists

                if ( strategy.isCreateAndUpdate() || strategy.isUpdate() )
                {
                    // Update existing CDSR

                    updateCount++;

                    if ( !isDryRun )
                    {
                        batchHandler.updateObject( internalCdsr );
                    }
                }
                else if ( strategy.isDelete() )
                {
                    // TODO Does 'delete' even make sense for CDSR?
                    // Replace existing CDSR

                    deleteCount++;

                    if ( !isDryRun )
                    {
                        batchHandler.deleteObject( internalCdsr );
                    }
                }
            }
            else
            {
                // CDSR does not already exist

                if ( strategy.isCreateAndUpdate() || strategy.isCreate() )
                {
                    if ( existingCdsr != null )
                    {
                        // Already exists -> update

                        importCount++;

                        if ( !isDryRun )
                        {
                            batchHandler.updateObject( internalCdsr );
                        }
                    }
                    else
                    {
                        // Does not exist -> add new CDSR

                        boolean added = false;

                        if ( !isDryRun )
                        {
                            added = batchHandler.addObject( internalCdsr );
                        }

                        if ( isDryRun || added )
                        {
                            importCount++;
                        }
                    }
                }
            }
        }

        batchHandler.flush();

        finalizeSummary( summary, totalCount, importCount, updateCount, deleteCount );

        return totalCount;
    }

    private static void finalizeSummary( ImportSummary summary, int totalCount, int importCount, int updateCount, int deleteCount )
    {
        int ignores = totalCount - importCount - updateCount - deleteCount;

        summary.setImportCount( new ImportCount( importCount, updateCount, ignores, deleteCount ) );
        summary.setStatus( ImportStatus.SUCCESS );
        summary.setDescription( "Import process completed successfully" );
    }

    private static CompleteDataSetRegistration createCompleteDataSetRegistration(
        org.hisp.dhis.dxf2.dataset.CompleteDataSetRegistration cdsr, MetaDataProperties mdProps, Date now )
    {
        return new CompleteDataSetRegistration(
            mdProps.dataSet,
            mdProps.period,
            mdProps.orgUnit,
            mdProps.attrOptCombo,
            cdsr.hasDate() ? DateUtils.parseDate( cdsr.getDate() ) : now,
            cdsr.getStoredBy()
        );
    }

    private static boolean validateAttrOptCombo( MetaDataProperties mdProps, MetaDataCaches mdCaches, ImportSummary summary )
    {
        final DataElementCategoryOptionCombo aoc = mdProps.attrOptCombo;
        final Period pe = mdProps.period;

        DateRange range = aoc.getDateRange();

        if ( ( range.getStartDate() != null && range.getStartDate().compareTo( pe.getStartDate() ) > 0 ) ||
            ( range.getEndDate() != null && range.getEndDate().compareTo( pe.getEndDate() ) < 0 ) )
        {
            summary.getConflicts().add( new ImportConflict( mdProps.orgUnit.getUid(),
                String.format( "Period: %s is not within range of attribute option combo: %s", pe.getIsoDate(), aoc.getUid() ) ) );
            return false;
        }

        final String aocOrgUnitKey = aoc.getUid() + mdProps.orgUnit.getUid();

        if ( !mdCaches.attrOptComboOrgUnitMap.get( aocOrgUnitKey, () -> {
                Set<OrganisationUnit> aocOrgUnits = aoc.getOrganisationUnits();
                return aocOrgUnits == null || mdProps.orgUnit.isDescendant( aocOrgUnits );
            }
        ) )
        {
            summary.getConflicts().add( new ImportConflict( mdProps.orgUnit.getUid(),
                String.format( "Organisation unit: %s is not valid for attribute option combo %s", mdProps.orgUnit.getUid(), aoc.getUid() )
            ) );

            return false;
        }

        return true;
    }

    private static boolean validateStoredBy( String storedBy, ImportSummary importSummary, I18n i18n )
    {
        String result = ValidationUtils.storedByIsValid( storedBy );

        if ( result == null )
        {
            return true;
        }

        importSummary.getConflicts().add( new ImportConflict( storedBy, i18n.getString( result ) ) );

        return false;
    }

    private static boolean hasMatchingPeriodTypes( MetaDataProperties props, MetaDataCaches mdCaches, ImportSummary summary )
    {
        if ( !props.dataSet.getPeriodType().equals( props.period.getPeriodType() ) )
        {
            summary.getConflicts().add(
                new ImportConflict( props.period.getUid(), String.format( "Period type of period: %s is not equal to the period type of data set: %s",
                    props.period.getIsoDate(), props.dataSet.getPeriodType() ) ) );

            return false;
        }

        return true;
    }

    private void heatCaches( MetaDataCaches caches, ImportConfig config )
    {
        if ( !caches.dataSets.isCacheLoaded() && exceedsThreshold( caches.dataSets ) )
        {
            caches.dataSets.load( idObjManager.getAll( DataSet.class ), ds -> ds.getPropertyValue( config.dsScheme ) );

            log.info( "Data set cache heated after cache miss threshold reached" );
        }

        if ( !caches.orgUnits.isCacheLoaded() && exceedsThreshold( caches.orgUnits ) )
        {
            caches.orgUnits.load( idObjManager.getAll( OrganisationUnit.class ), ou -> ou.getPropertyValue( config.ouScheme ) );

            log.info( "Org unit cache heated after cache miss threshold reached" );
        }

        // TODO Consider need for checking/re-heating attrOptCombo and period caches

        if ( !caches.attrOptionCombos.isCacheLoaded() && exceedsThreshold( caches.attrOptionCombos ) )
        {
            caches.attrOptionCombos.load( idObjManager.getAll( DataElementCategoryOptionCombo.class ), aoc -> aoc.getPropertyValue( config.aocScheme ) );

            log.info( "Attribute option combo cache heated after cache miss threshold reached" );
        }

        if ( !caches.periods.isCacheLoaded() && exceedsThreshold( caches.periods ) )
        {
            caches.periods.load( idObjManager.getAll( Period.class ), pe -> pe.getPropertyValue( null ) );
        }
    }

    private static MetaDataProperties initMetaDataProperties(
        org.hisp.dhis.dxf2.dataset.CompleteDataSetRegistration cdsr, MetaDataCallables callables, MetaDataCaches cache )
    {
        String
            ds = StringUtils.trimToNull( cdsr.getDataSet() ),
            pe = StringUtils.trimToNull( cdsr.getPeriod() ),
            ou = StringUtils.trimToNull( cdsr.getOrganisationUnit() ),
            aoc = StringUtils.trimToNull( cdsr.getAttributeOptionCombo() );

        return new MetaDataProperties(
            cache.dataSets.get( ds , callables.dataSetCallable.setId( ds ) ),
            cache.periods.get( pe, callables.periodCallable.setId( pe ) ),
            cache.orgUnits.get( ou, callables.orgUnitCallable.setId( ou ) ),
            cache.attrOptionCombos.get( aoc, callables.optionComboCallable.setId( aoc ) )
        );
    }

    private static boolean exceedsThreshold( CachingMap cachingMap )
    {
        return cachingMap.getCacheMissCount() > CACHE_MISS_THRESHOLD;
    }

    // -----------------------------------------------------------------
    // Internal classes
    // -----------------------------------------------------------------


    private static class MetaDataProperties
    {
        final DataSet dataSet;
        final Period period;
        final OrganisationUnit orgUnit;
        DataElementCategoryOptionCombo attrOptCombo;

        public MetaDataProperties( DataSet dataSet, Period period, OrganisationUnit orgUnit, DataElementCategoryOptionCombo attrOptCombo )
        {
            this.dataSet = dataSet;
            this.period = period;
            this.orgUnit = orgUnit;
            this.attrOptCombo = attrOptCombo;
        }

        public boolean isValid( ImportSummary importSummary, org.hisp.dhis.dxf2.dataset.CompleteDataSetRegistration cdsr )
        {
            if ( dataSet == null )
            {
                importSummary.getConflicts().add( new ImportConflict( cdsr.getDataSet(), "Data set not found or not accessible" ) );
                return false;
            }

            if ( period == null )
            {
                importSummary.getConflicts().add( new ImportConflict( cdsr.getPeriod(), "Period not valid" ) );
                return false;
            }

            if ( orgUnit == null )
            {
                importSummary.getConflicts().add(
                    new ImportConflict( cdsr.getOrganisationUnit(), "Organisation unit not found or not accessible" ) );
                return false;
            }

            if ( attrOptCombo == null && StringUtils.trimToNull( cdsr.getAttributeOptionCombo() ) != null )
            {
                importSummary.getConflicts().add(
                    new ImportConflict( cdsr.getAttributeOptionCombo(), "Attribute option combo not found or not accessible" ) );
                return false;
            }

            return true;
        }
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

    private static class MetaDataCaches
    {
        CachingMap<String, DataSet> dataSets = new CachingMap<>();
        CachingMap<String, OrganisationUnit> orgUnits = new CachingMap<>();
        CachingMap<String, Period> periods = new CachingMap<>();
        CachingMap<String, DataElementCategoryOptionCombo> attrOptionCombos = new CachingMap<>();
        CachingMap<String, Boolean> orgUnitInHierarchyMap = new CachingMap<>();
        CachingMap<String, Boolean> attrOptComboOrgUnitMap = new CachingMap<>();

        void preheat( IdentifiableObjectManager manager, final ImportConfig config )
        {
            dataSets.load( manager.getAll( DataSet.class ), ds -> ds.getPropertyValue( config.dsScheme ) );
            orgUnits.load( manager.getAll( OrganisationUnit.class ), ou -> ou.getPropertyValue( config.ouScheme ) );
            attrOptionCombos.load( manager.getAll( DataElementCategoryOptionCombo.class ), oc -> oc.getPropertyValue( config.aocScheme ) );
        }
    }

    private static class ImportConfig
    {
        IdScheme dsScheme, ouScheme, aocScheme;
        ImportStrategy strategy;
        boolean dryRun, skipExistingCheck, strictPeriods, strictAttrOptionCombos, strictOrgUnits, requireAttrOptionCombos;

        ImportConfig( CompleteDataSetRegistrations cdsr, ImportOptions options, SystemSettingManager manager )
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
            strictPeriods = options.isStrictPeriods() || (Boolean) manager.getSystemSetting( SettingKey.DATA_IMPORT_STRICT_PERIODS );
            strictAttrOptionCombos = options.isStrictAttributeOptionCombos() ||
                (Boolean) manager.getSystemSetting( SettingKey.DATA_IMPORT_STRICT_ATTRIBUTE_OPTION_COMBOS );
            strictOrgUnits = options.isStrictOrganisationUnits() ||
                (Boolean) manager.getSystemSetting( SettingKey.DATA_IMPORT_STRICT_ORGANISATION_UNITS );
            requireAttrOptionCombos = options.isRequireAttributeOptionCombo() ||
                (Boolean) manager.getSystemSetting( SettingKey.DATA_IMPORT_REQUIRE_ATTRIBUTE_OPTION_COMBO );
        }
    }
}
