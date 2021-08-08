/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.dxf2.datavalueset;

import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.hisp.dhis.external.conf.ConfigurationKey.CHANGELOG_AGGREGATE;
import static org.hisp.dhis.system.notification.NotificationLevel.ERROR;
import static org.hisp.dhis.system.notification.NotificationLevel.INFO;
import static org.hisp.dhis.system.notification.NotificationLevel.WARN;
import static org.hisp.dhis.util.DateUtils.parseDate;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.BooleanUtils;
import org.hisp.dhis.calendar.CalendarService;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.commons.util.StreamUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.LockExceptionStore;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.datavalueset.ImportContext.DataSetContext;
import org.hisp.dhis.dxf2.importsummary.ImportCount;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.pdfform.PdfDataEntryFormUtil;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.jdbc.batchhandler.DataValueAuditBatchHandler;
import org.hisp.dhis.jdbc.batchhandler.DataValueBatchHandler;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.callable.CategoryOptionComboAclCallable;
import org.hisp.dhis.system.callable.IdentifiableObjectCallable;
import org.hisp.dhis.system.callable.PeriodCallable;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.system.util.CsvUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.quick.BatchHandlerFactory;
import org.hisp.staxwax.factory.XMLFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.csvreader.CsvReader;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Note that a mock BatchHandler factory is being injected.
 *
 * @author Lars Helge Overland
 */
@Slf4j
@Service( "org.hisp.dhis.dxf2.datavalueset.DataValueSetService" )
@AllArgsConstructor
public class DefaultDataValueSetService implements DataValueSetService
{
    private static final String ERROR_OBJECT_NEEDED_TO_COMPLETE = "Must be provided to complete data set";

    private static final int CACHE_MISS_THRESHOLD = 250;

    private final IdentifiableObjectManager identifiableObjectManager;

    private final CategoryService categoryService;

    private final OrganisationUnitService organisationUnitService;

    private final PeriodService periodService;

    private BatchHandlerFactory batchHandlerFactory;

    private final CompleteDataSetRegistrationService registrationService;

    private CurrentUserService currentUserService;

    private final DataValueSetStore dataValueSetStore;

    private final SystemSettingManager systemSettingManager;

    private final LockExceptionStore lockExceptionStore;

    private final I18nManager i18nManager;

    private final Notifier notifier;

    private final InputUtils inputUtils;

    private final CalendarService calendarService;

    private final DataValueService dataValueService;

    private final FileResourceService fileResourceService;

    private final AclService aclService;

    private final DhisConfigurationProvider config;

    private final ObjectMapper jsonMapper;

    private final DataValueSetImportValidator importValidator;

    private final SchemaService schemaService;

    /**
     * Used only for testing, remove when test is refactored
     */
    @Deprecated
    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    /**
     * Used only for testing, remove when test is refactored
     */
    @Deprecated
    public void setBatchHandlerFactory( BatchHandlerFactory batchHandlerFactory )
    {
        this.batchHandlerFactory = batchHandlerFactory;
    }

    // -------------------------------------------------------------------------
    // DataValueSet implementation
    // -------------------------------------------------------------------------

    @Override
    public DataExportParams getFromUrl( Set<String> dataSets, Set<String> dataElementGroups, Set<String> periods,
        Date startDate, Date endDate,
        Set<String> organisationUnits, boolean includeChildren, Set<String> organisationUnitGroups,
        Set<String> attributeOptionCombos,
        boolean includeDeleted, Date lastUpdated, String lastUpdatedDuration, Integer limit, IdSchemes outputIdSchemes )
    {
        DataExportParams params = new DataExportParams();

        if ( dataSets != null )
        {
            params.getDataSets().addAll( identifiableObjectManager.getObjects(
                DataSet.class, IdentifiableProperty.UID, dataSets ) );
        }

        if ( dataElementGroups != null )
        {
            params.getDataElementGroups().addAll( identifiableObjectManager.getObjects(
                DataElementGroup.class, IdentifiableProperty.UID, dataElementGroups ) );
        }

        if ( periods != null && !periods.isEmpty() )
        {
            params.getPeriods().addAll( periodService.reloadIsoPeriods( new ArrayList<>( periods ) ) );
        }
        else if ( startDate != null && endDate != null )
        {
            params
                .setStartDate( startDate )
                .setEndDate( endDate );
        }

        if ( organisationUnits != null )
        {
            params.getOrganisationUnits().addAll( identifiableObjectManager.getObjects(
                OrganisationUnit.class, IdentifiableProperty.UID, organisationUnits ) );
        }

        if ( organisationUnitGroups != null )
        {
            params.getOrganisationUnitGroups().addAll( identifiableObjectManager.getObjects(
                OrganisationUnitGroup.class, IdentifiableProperty.UID, organisationUnitGroups ) );
        }

        if ( attributeOptionCombos != null )
        {
            params.getAttributeOptionCombos().addAll( identifiableObjectManager.getObjects(
                CategoryOptionCombo.class, IdentifiableProperty.UID, attributeOptionCombos ) );
        }

        return params
            .setIncludeChildren( includeChildren )
            .setIncludeDeleted( includeDeleted )
            .setLastUpdated( lastUpdated )
            .setLastUpdatedDuration( lastUpdatedDuration )
            .setLimit( limit )
            .setOutputIdSchemes( outputIdSchemes );
    }

    @Override
    public void validate( DataExportParams params )
    {
        ErrorMessage error = null;

        if ( params == null )
        {
            throw new IllegalQueryException( ErrorCode.E2000 );
        }

        if ( !params.hasDataElements() && !params.hasDataSets() && !params.hasDataElementGroups() )
        {
            error = new ErrorMessage( ErrorCode.E2001 );
        }

        if ( !params.hasPeriods() && !params.hasStartEndDate() && !params.hasLastUpdated()
            && !params.hasLastUpdatedDuration() )
        {
            error = new ErrorMessage( ErrorCode.E2002 );
        }

        if ( params.hasPeriods() && params.hasStartEndDate() )
        {
            error = new ErrorMessage( ErrorCode.E2003 );
        }

        if ( params.hasStartEndDate() && params.getStartDate().after( params.getEndDate() ) )
        {
            error = new ErrorMessage( ErrorCode.E2004 );
        }

        if ( params.hasLastUpdatedDuration() && DateUtils.getDuration( params.getLastUpdatedDuration() ) == null )
        {
            error = new ErrorMessage( ErrorCode.E2005 );
        }

        if ( !params.hasOrganisationUnits() && !params.hasOrganisationUnitGroups() )
        {
            error = new ErrorMessage( ErrorCode.E2006 );
        }

        if ( params.isIncludeChildren() && params.hasOrganisationUnitGroups() )
        {
            error = new ErrorMessage( ErrorCode.E2007 );
        }

        if ( params.isIncludeChildren() && !params.hasOrganisationUnits() )
        {
            error = new ErrorMessage( ErrorCode.E2008 );
        }

        if ( params.hasLimit() && params.getLimit() < 0 )
        {
            error = new ErrorMessage( ErrorCode.E2009, params.getLimit() );
        }

        if ( error != null )
        {
            log.warn( "Validation failed: " + error );

            throw new IllegalQueryException( error );
        }
    }

    @Override
    public void decideAccess( DataExportParams params )
    {
        User user = currentUserService.getCurrentUser();

        // Verify data set read sharing

        for ( DataSet dataSet : params.getDataSets() )
        {
            if ( !aclService.canDataRead( user, dataSet ) )
            {
                throw new IllegalQueryException( new ErrorMessage( ErrorCode.E2010, dataSet.getUid() ) );
            }
        }

        // Verify attribute option combination data read sharing

        for ( CategoryOptionCombo optionCombo : params.getAttributeOptionCombos() )
        {
            if ( !aclService.canDataRead( user, optionCombo ) )
            {
                throw new IllegalQueryException( new ErrorMessage( ErrorCode.E2011, optionCombo.getUid() ) );
            }
        }

        // Verify org unit being located within user data capture hierarchy

        for ( OrganisationUnit unit : params.getOrganisationUnits() )
        {
            if ( !organisationUnitService.isInUserDataViewHierarchy( unit ) )
            {
                throw new IllegalQueryException( new ErrorMessage( ErrorCode.E2012, unit.getUid() ) );
            }
        }
    }

    // -------------------------------------------------------------------------
    // Write
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void writeDataValueSetXml( DataExportParams params, OutputStream out )
    {
        decideAccess( params );
        validate( params );

        dataValueSetStore.writeDataValueSetXml( params, getCompleteDate( params ), out );
    }

    @Override
    @Transactional
    public void writeDataValueSetJson( DataExportParams params, OutputStream out )
    {
        decideAccess( params );
        validate( params );

        dataValueSetStore.writeDataValueSetJson( params, getCompleteDate( params ), out );
    }

    @Override
    @Transactional
    public void writeDataValueSetJson( Date lastUpdated, OutputStream outputStream, IdSchemes idSchemes )
    {
        dataValueSetStore.writeDataValueSetJson( lastUpdated, outputStream, idSchemes );
    }

    @Override
    @Transactional
    public void writeDataValueSetJson( Date lastUpdated, OutputStream outputStream, IdSchemes idSchemes, int pageSize,
        int page )
    {
        dataValueSetStore.writeDataValueSetJson( lastUpdated, outputStream, idSchemes, pageSize, page );
    }

    @Override
    @Transactional
    public void writeDataValueSetCsv( DataExportParams params, Writer writer )
    {
        decideAccess( params );
        validate( params );

        dataValueSetStore.writeDataValueSetCsv( params, getCompleteDate( params ), writer );
    }

    private Date getCompleteDate( DataExportParams params )
    {
        if ( params.isSingleDataValueSet() )
        {
            CategoryOptionCombo optionCombo = categoryService.getDefaultCategoryOptionCombo(); // TODO

            CompleteDataSetRegistration registration = registrationService
                .getCompleteDataSetRegistration( params.getFirstDataSet(), params.getFirstPeriod(),
                    params.getFirstOrganisationUnit(), optionCombo );

            return registration != null ? registration.getDate() : null;
        }

        return null;
    }

    // -------------------------------------------------------------------------
    // Template
    // -------------------------------------------------------------------------

    @Override
    public RootNode getDataValueSetTemplate( DataSet dataSet, Period period, List<String> orgUnits,
        boolean writeComments, String ouScheme, String deScheme )
    {
        RootNode rootNode = new RootNode( "dataValueSet" );
        rootNode.setNamespace( DxfNamespaces.DXF_2_0 );
        rootNode.setComment( "Data set: " + dataSet.getDisplayName() + " (" + dataSet.getUid() + ")" );

        CollectionNode collectionNode = rootNode.addChild( new CollectionNode( "dataValues" ) );
        collectionNode.setWrapping( false );

        if ( orgUnits.isEmpty() )
        {
            for ( DataElement dataElement : dataSet.getDataElements() )
            {
                CollectionNode collection = getDataValueTemplate( dataElement, deScheme, null, ouScheme, period,
                    writeComments );
                collectionNode.addChildren( collection.getChildren() );
            }
        }
        else
        {
            for ( String orgUnit : orgUnits )
            {
                OrganisationUnit organisationUnit = identifiableObjectManager.search( OrganisationUnit.class, orgUnit );

                if ( organisationUnit == null )
                {
                    continue;
                }

                for ( DataElement dataElement : dataSet.getDataElements() )
                {
                    CollectionNode collection = getDataValueTemplate( dataElement, deScheme, organisationUnit, ouScheme,
                        period, writeComments );
                    collectionNode.addChildren( collection.getChildren() );
                }
            }
        }

        return rootNode;
    }

    private CollectionNode getDataValueTemplate( DataElement dataElement, String deScheme,
        OrganisationUnit organisationUnit, String ouScheme, Period period, boolean comment )
    {
        CollectionNode collectionNode = new CollectionNode( "dataValues" );
        collectionNode.setWrapping( false );

        for ( CategoryOptionCombo categoryOptionCombo : dataElement.getSortedCategoryOptionCombos() )
        {
            ComplexNode complexNode = collectionNode.addChild( new ComplexNode( "dataValue" ) );

            String label = dataElement.getDisplayName();

            if ( !categoryOptionCombo.isDefault() )
            {
                label += " " + categoryOptionCombo.getDisplayName();
            }

            if ( comment )
            {
                complexNode.setComment( "Data element: " + label );
            }

            if ( IdentifiableProperty.CODE.toString().toLowerCase()
                .equals( deScheme.toLowerCase() ) )
            {
                SimpleNode simpleNode = complexNode.addChild( new SimpleNode( "dataElement", dataElement.getCode() ) );
                simpleNode.setAttribute( true );
            }
            else
            {
                SimpleNode simpleNode = complexNode.addChild( new SimpleNode( "dataElement", dataElement.getUid() ) );
                simpleNode.setAttribute( true );
            }

            SimpleNode simpleNode = complexNode
                .addChild( new SimpleNode( "categoryOptionCombo", categoryOptionCombo.getUid() ) );
            simpleNode.setAttribute( true );

            simpleNode = complexNode.addChild( new SimpleNode( "period", period != null ? period.getIsoDate() : "" ) );
            simpleNode.setAttribute( true );

            if ( organisationUnit != null )
            {
                if ( IdentifiableProperty.CODE.toString().equalsIgnoreCase( ouScheme ) )
                {
                    simpleNode = complexNode.addChild( new SimpleNode( "orgUnit",
                        organisationUnit.getCode() == null ? "" : organisationUnit.getCode() ) );
                    simpleNode.setAttribute( true );
                }
                else
                {
                    simpleNode = complexNode.addChild( new SimpleNode( "orgUnit",
                        organisationUnit.getUid() == null ? "" : organisationUnit.getUid() ) );
                    simpleNode.setAttribute( true );
                }
            }

            simpleNode = complexNode.addChild( new SimpleNode( "value", "" ) );
            simpleNode.setAttribute( true );
        }

        return collectionNode;
    }

    // -------------------------------------------------------------------------
    // Save
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ImportSummary saveDataValueSet( InputStream in )
    {
        return saveDataValueSet( in, ImportOptions.getDefaultImportOptions(), null );
    }

    @Override
    @Transactional
    public ImportSummary saveDataValueSetJson( InputStream in )
    {
        return saveDataValueSetJson( in, ImportOptions.getDefaultImportOptions(), null );
    }

    @Override
    @Transactional
    public ImportSummary saveDataValueSet( InputStream in, ImportOptions importOptions )
    {
        return saveDataValueSet( in, importOptions, null );
    }

    @Override
    @Transactional
    public ImportSummary saveDataValueSetJson( InputStream in, ImportOptions importOptions )
    {
        return saveDataValueSetJson( in, importOptions, null );
    }

    @Override
    @Transactional
    public ImportSummary saveDataValueSetCsv( InputStream in, ImportOptions importOptions )
    {
        return saveDataValueSetCsv( in, importOptions, null );
    }

    @Override
    @Transactional
    public ImportSummary saveDataValueSet( InputStream in, ImportOptions importOptions, JobConfiguration id )
    {
        try
        {
            in = StreamUtils.wrapAndCheckCompressionFormat( in );
            DataValueSet dataValueSet = new StreamingXmlDataValueSet( XMLFactory.getXMLReader( in ) );
            return saveDataValueSet( importOptions, id, dataValueSet );
        }
        catch ( Exception ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );
            notifier.notify( id, ERROR, "Process failed: " + ex.getMessage(), true );
            return new ImportSummary( ImportStatus.ERROR, "The import process failed: " + ex.getMessage() );
        }
    }

    @Override
    @Transactional
    public ImportSummary saveDataValueSetJson( InputStream in, ImportOptions importOptions, JobConfiguration id )
    {
        try
        {
            in = StreamUtils.wrapAndCheckCompressionFormat( in );
            DataValueSet dataValueSet = jsonMapper.readValue( in, DataValueSet.class );
            return saveDataValueSet( importOptions, id, dataValueSet );
        }
        catch ( Exception ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );
            notifier.notify( id, ERROR, "Process failed: " + ex.getMessage(), true );
            return new ImportSummary( ImportStatus.ERROR, "The import process failed: " + ex.getMessage() );
        }
    }

    @Override
    @Transactional
    public ImportSummary saveDataValueSetCsv( InputStream in, ImportOptions importOptions, JobConfiguration id )
    {
        try
        {
            in = StreamUtils.wrapAndCheckCompressionFormat( in );
            CsvReader csvReader = CsvUtils.getReader( in );

            if ( importOptions == null || importOptions.isFirstRowIsHeader() )
            {
                csvReader.readRecord(); // Ignore the first row
            }

            DataValueSet dataValueSet = new StreamingCsvDataValueSet( csvReader );
            return saveDataValueSet( importOptions, id, dataValueSet );
        }
        catch ( Exception ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );
            notifier.clear( id ).notify( id, ERROR, "Process failed: " + ex.getMessage(), true );
            return new ImportSummary( ImportStatus.ERROR, "The import process failed: " + ex.getMessage() );
        }
    }

    @Override
    @Transactional
    public ImportSummary saveDataValueSetPdf( InputStream in, ImportOptions importOptions, JobConfiguration id )
    {
        try
        {
            DataValueSet dataValueSet = PdfDataEntryFormUtil.getDataValueSet( in );
            return saveDataValueSet( importOptions, id, dataValueSet );
        }
        catch ( RuntimeException ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );
            notifier.clear( id ).notify( id, ERROR, "Process failed: " + ex.getMessage(), true );
            return new ImportSummary( ImportStatus.ERROR, "The import process failed: " + ex.getMessage() );
        }
    }

    @Override
    @Transactional
    public ImportSummary saveDataValueSetPdf( InputStream in, ImportOptions importOptions )
    {
        return saveDataValueSetPdf( in, importOptions, null );
    }

    /**
     * There are specific id schemes for data elements and organisation units
     * and a generic id scheme for all objects. The specific id schemes will
     * take precedence over the generic id scheme. The generic id scheme also
     * applies to data set and category option combo.
     * <p>
     * The id schemes uses the following order of precedence:
     * <p>
     * <ul>
     * <li>Id scheme from the data value set</li>
     * <li>Id scheme from the import options</li>
     * <li>Default id scheme which is UID</li>
     * <ul>
     * <p>
     * If id scheme is specific in the data value set, any id schemes in the
     * import options will be ignored.
     */
    private ImportSummary saveDataValueSet( ImportOptions importOptions, JobConfiguration id,
        DataValueSet dataValueSet )
    {
        final ImportContext context = createDataValueSetImportContext( importOptions, dataValueSet );
        logDataValueSetImportContextInfo( context );

        Clock clock = new Clock( log ).startClock()
            .logTime( "Starting data value import, options: " + context.getImportOptions() );
        NotificationLevel notificationLevel = context.getImportOptions().getNotificationLevel( INFO );
        notifier.clear( id ).notify( id, notificationLevel, "Process started" );

        // ---------------------------------------------------------------------
        // Heat caches
        // ---------------------------------------------------------------------
        preheatCaches( context );

        // ---------------------------------------------------------------------
        // Get outer meta-data
        // ---------------------------------------------------------------------
        ImportContext.DataSetContext dataSetContext = createDataSetContext( context, dataValueSet );

        // ---------------------------------------------------------------------
        // Validation
        // ---------------------------------------------------------------------

        if ( importValidator.abortDataSetImport( dataValueSet, context, dataSetContext ) )
        {
            context.getSummary().setDescription( "Import process was aborted" );
            notifier.notify( id, WARN, "Import process aborted", true )
                .addJobSummary( id, context.getSummary(), ImportSummary.class );
            dataValueSet.close();
            return context.getSummary();
        }

        Date completeDate = parseDate( dataValueSet.getCompleteDate() );
        if ( dataSetContext.getDataSet() != null && completeDate != null )
        {
            notifier.notify( id, notificationLevel, "Completing data set" );
            handleComplete( dataSetContext.getDataSet(), completeDate, dataSetContext.getOuterPeriod(),
                dataSetContext.getOuterOrgUnit(),
                dataSetContext.getFallbackCategoryOptionCombo(),
                context.getCurrentUserName(), context.getSummary() );
        }
        else
        {
            context.getSummary().setDataSetComplete( Boolean.FALSE.toString() );
        }

        final ImportCount importCount = new ImportCount();

        // ---------------------------------------------------------------------
        // Data values
        // ---------------------------------------------------------------------

        Date now = new Date();

        clock.logTime( "Validated outer meta-data" );
        notifier.notify( id, notificationLevel, "Importing data values" );

        int index = 0;
        while ( dataValueSet.hasNextDataValue() )
        {
            org.hisp.dhis.dxf2.datavalue.DataValue dataValue = dataValueSet.getNextDataValue();

            ImportContext.DataValueContext valueContext = createDataValueContext( index++,
                dataValue, context, dataSetContext );

            // -----------------------------------------------------------------
            // Potentially heat caches
            // -----------------------------------------------------------------

            autoPreheatCaches( context );

            // -----------------------------------------------------------------
            // Validation & Constraints
            // -----------------------------------------------------------------
            if ( importValidator.skipDataValue( dataValue, context, dataSetContext, valueContext ) )
            {
                importCount.incrementIgnored();
                continue;
            }

            // -----------------------------------------------------------------
            // Create data value
            // -----------------------------------------------------------------
            DataValue internalValue = createDataValue( dataValue, context, valueContext, now );

            // -----------------------------------------------------------------
            // Save, update or delete data value
            // -----------------------------------------------------------------
            DataValue existingValue = !context.isSkipExistingCheck()
                ? context.getDataValueBatchHandler().findObject( internalValue )
                : null;

            // -----------------------------------------------------------------
            // Preserve any existing created date unless overwritten by import
            // -----------------------------------------------------------------
            if ( existingValue != null && !dataValue.hasCreated() )
            {
                internalValue.setCreated( existingValue.getCreated() );
            }

            // -----------------------------------------------------------------
            // Check soft deleted data values on update and import
            // -----------------------------------------------------------------
            final ImportStrategy strategy = context.getStrategy();
            if ( !context.isSkipExistingCheck() && existingValue != null && !existingValue.isDeleted() )
            {
                if ( strategy.isCreateAndUpdate() || strategy.isUpdate() )
                {
                    saveDataValueUpdate( context, importCount, dataValue, valueContext, internalValue, existingValue );
                }
                else if ( strategy.isDelete() )
                {
                    saveDataValueDelete( context, importCount, dataValue, valueContext, internalValue, existingValue );
                }
                else
                {
                    importCount.incrementIgnored();
                }
            }
            else
            {
                if ( strategy.isCreateAndUpdate() || strategy.isCreate() )
                {
                    saveDataValueCreate( context, importCount, valueContext, internalValue, existingValue );
                }
                else
                {
                    importCount.incrementIgnored();
                }
            }
        }

        context.getDataValueBatchHandler().flush();

        if ( !context.isSkipAudit() )
        {
            context.getAuditBatchHandler().flush();
        }

        context.getSummary()
            .setImportCount( importCount )
            .setStatus( !context.getSummary().hasConflicts() ? ImportStatus.SUCCESS : ImportStatus.WARNING )
            .setDescription( "Import process completed successfully" );

        clock.logTime(
            "Data value import done, total: " + importCount.getTotalCount() + ", import: " + importCount.getImported()
                + ", update: "
                + importCount.getUpdated() + ", delete: " + importCount.getDeleted() );
        notifier.notify( id, notificationLevel, "Import done", true )
            .addJobSummary( id, notificationLevel, context.getSummary(), ImportSummary.class );

        dataValueSet.close();

        return context.getSummary();
    }

    private void saveDataValueCreate( ImportContext context, ImportCount importCount,
        ImportContext.DataValueContext valueContext, DataValue internalValue, DataValue existingValue )
    {
        if ( internalValue.isNullValue() )
        {
            importCount.incrementIgnored();
            return; // Ignore null values
        }
        if ( existingValue != null && existingValue.isDeleted() )
        {
            importCount.incrementImported();

            if ( !context.isDryRun() )
            {
                context.getDataValueBatchHandler().updateObject( internalValue );

                if ( valueContext.getDataElement().isFileType() )
                {
                    FileResource fr = fileResourceService.getFileResource( internalValue.getValue() );

                    fr.setAssigned( true );

                    fileResourceService.updateFileResource( fr );
                }
            }
            return;
        }
        boolean added = false;

        if ( !context.isDryRun() )
        {
            added = context.getDataValueBatchHandler().addObject( internalValue );

            if ( added && valueContext.getDataElement().isFileType() )
            {
                FileResource fr = fileResourceService.getFileResource( internalValue.getValue() );

                fr.setAssigned( true );

                fileResourceService.updateFileResource( fr );
            }
        }

        if ( context.isDryRun() || added )
        {
            importCount.incrementImported();
        }
    }

    private void saveDataValueDelete( ImportContext context, ImportCount importCount,
        org.hisp.dhis.dxf2.datavalue.DataValue dataValue, ImportContext.DataValueContext valueContext,
        DataValue internalValue, DataValue existingValue )
    {
        internalValue.setDeleted( true );

        importCount.incrementDeleted();

        if ( !context.isDryRun() )
        {
            DataValue actualDataValue = valueContext.getActualDataValue( dataValueService );
            if ( valueContext.getDataElement().isFileType() && actualDataValue != null )
            {
                FileResource fr = fileResourceService.getFileResource( actualDataValue.getValue() );

                fileResourceService.updateFileResource( fr );
            }

            context.getDataValueBatchHandler().updateObject( internalValue );

            if ( !context.isSkipAudit() )
            {
                DataValueAudit auditValue = new DataValueAudit( internalValue, existingValue.getValue(),
                    context.getStoredBy( dataValue ), AuditType.DELETE );

                context.getAuditBatchHandler().addObject( auditValue );
            }
        }
    }

    private void saveDataValueUpdate( ImportContext context, ImportCount importCount,
        org.hisp.dhis.dxf2.datavalue.DataValue dataValue, ImportContext.DataValueContext valueContext,
        DataValue internalValue, DataValue existingValue )
    {
        AuditType auditType = AuditType.UPDATE;
        if ( internalValue.isNullValue() || internalValue.isDeleted() )
        {
            internalValue.setDeleted( true );

            auditType = AuditType.DELETE;

            importCount.incrementDeleted();
        }
        else
        {
            importCount.incrementUpdated();
        }
        if ( !internalValue.isDeleted()
            && Objects.equals( existingValue.getValue(), internalValue.getValue() )
            && Objects.equals( existingValue.getComment(), internalValue.getComment() ) )
        {
            return; // avoid performing unnecessary updates
        }
        if ( !context.isDryRun() )
        {
            context.getDataValueBatchHandler().updateObject( internalValue );

            if ( !context.isSkipAudit() )
            {
                DataValueAudit auditValue = new DataValueAudit( internalValue, existingValue.getValue(),
                    context.getStoredBy( dataValue ), auditType );

                context.getAuditBatchHandler().addObject( auditValue );
            }

            if ( valueContext.getDataElement().isFileType() )
            {
                FileResource fr = fileResourceService.getFileResource( internalValue.getValue() );

                fr.setAssigned( true );

                fileResourceService.updateFileResource( fr );
            }

        }
    }

    private void preheatCaches( ImportContext context )
    {
        if ( context.getImportOptions().isPreheatCacheDefaultFalse() )
        {
            preheatDataElementCache( context );
            preheatOrgUnitCache( context );
            preheatOptionComboCache( context );
        }
    }

    private void autoPreheatCaches( ImportContext context )
    {
        if ( !context.getDataElementMap().isCacheLoaded()
            && context.getDataElementMap().getCacheMissCount() > CACHE_MISS_THRESHOLD )
        {
            preheatDataElementCache( context );

            log.info( "Data element cache heated after cache miss threshold reached" );
        }

        if ( !context.getOrgUnitMap().isCacheLoaded()
            && context.getOrgUnitMap().getCacheMissCount() > CACHE_MISS_THRESHOLD )
        {
            preheatOrgUnitCache( context );

            log.info( "Org unit cache heated after cache miss threshold reached" );
        }

        if ( !context.getOptionComboMap().isCacheLoaded()
            && context.getOptionComboMap().getCacheMissCount() > CACHE_MISS_THRESHOLD )
        {
            preheatOptionComboCache( context );

            log.info( "Category Option Combo cache heated after cache miss threshold reached" );
        }
    }

    private void preheatOptionComboCache( ImportContext context )
    {
        context.getOptionComboMap().load( identifiableObjectManager.getAll( CategoryOptionCombo.class ),
            o -> o.getPropertyValue( context.getCategoryOptComboIdScheme() ) );
    }

    private void preheatOrgUnitCache( ImportContext context )
    {
        context.getOrgUnitMap().load( identifiableObjectManager.getAll( OrganisationUnit.class ),
            o -> o.getPropertyValue( context.getOrgUnitIdScheme() ) );
    }

    private void preheatDataElementCache( ImportContext context )
    {
        context.getDataElementMap().load( identifiableObjectManager.getAll( DataElement.class ),
            o -> o.getPropertyValue( context.getDataElementIdScheme() ) );
    }

    private ImportContext createDataValueSetImportContext( ImportOptions options, DataValueSet data )
    {
        options = ObjectUtils.firstNonNull( options, ImportOptions.getDefaultImportOptions() );

        final User currentUser = currentUserService.getCurrentUser();

        boolean auditEnabled = config.isEnabled( CHANGELOG_AGGREGATE );
        boolean hasSkipAuditAuth = currentUser != null
            && currentUser.isAuthorized( Authorities.F_SKIP_DATA_IMPORT_AUDIT );
        boolean skipAudit = (options.isSkipAudit() && hasSkipAuditAuth) || !auditEnabled;

        SystemSettingManager settings = systemSettingManager;

        IdScheme dataElementIdScheme = createIdScheme( data.getDataElementIdSchemeProperty(), options,
            IdSchemes::getDataElementIdScheme );
        IdScheme orgUnitIdScheme = createIdScheme( data.getOrgUnitIdSchemeProperty(), options,
            IdSchemes::getOrgUnitIdScheme );
        IdScheme categoryOptComboIdScheme = createIdScheme( data.getCategoryOptionComboIdSchemeProperty(), options,
            IdSchemes::getCategoryOptionComboIdScheme );
        IdScheme dataSetIdScheme = createIdScheme( data.getDataSetIdSchemeProperty(), options,
            IdSchemes::getDataSetIdScheme );
        return ImportContext.builder()
            .importOptions( options )
            .summary( new ImportSummary().setImportOptions( options ) )
            .isIso8601( calendarService.getSystemCalendar().isIso8601() )
            .skipLockExceptionCheck( !lockExceptionStore.anyExists() )
            .i18n( i18nManager.getI18n() )
            .currentUser( currentUser )
            .currentOrgUnits( currentUserService.getCurrentUserOrganisationUnits() )
            .hasSkipAuditAuth( hasSkipAuditAuth )
            .skipAudit( skipAudit )
            .idScheme( createIdScheme( data.getIdSchemeProperty(), options, IdSchemes::getIdScheme ) )
            .dataElementIdScheme( dataElementIdScheme )
            .orgUnitIdScheme( orgUnitIdScheme )
            .categoryOptComboIdScheme( categoryOptComboIdScheme )
            .dataSetIdScheme( dataSetIdScheme )
            .strategy( data.getStrategy() != null
                ? ImportStrategy.valueOf( data.getStrategy() )
                : options.getImportStrategy() )
            .dryRun( data.getDryRun() != null ? data.getDryRun() : options.isDryRun() )
            .skipExistingCheck( options.isSkipExistingCheck() )
            .strictPeriods( options.isStrictPeriods()
                || (Boolean) settings.getSystemSetting( SettingKey.DATA_IMPORT_STRICT_PERIODS ) )
            .strictDataElements( options.isStrictDataElements()
                || (Boolean) settings.getSystemSetting( SettingKey.DATA_IMPORT_STRICT_DATA_ELEMENTS ) )
            .strictCategoryOptionCombos( options.isStrictCategoryOptionCombos()
                || (Boolean) settings.getSystemSetting( SettingKey.DATA_IMPORT_STRICT_CATEGORY_OPTION_COMBOS ) )
            .strictAttrOptionCombos( options.isStrictAttributeOptionCombos()
                || (Boolean) settings.getSystemSetting( SettingKey.DATA_IMPORT_STRICT_ATTRIBUTE_OPTION_COMBOS ) )
            .strictOrgUnits( options.isStrictOrganisationUnits()
                || (Boolean) settings.getSystemSetting( SettingKey.DATA_IMPORT_STRICT_ORGANISATION_UNITS ) )
            .requireCategoryOptionCombo( options.isRequireCategoryOptionCombo()
                || (Boolean) settings.getSystemSetting( SettingKey.DATA_IMPORT_REQUIRE_CATEGORY_OPTION_COMBO ) )
            .requireAttrOptionCombo( options.isRequireAttributeOptionCombo()
                || (Boolean) settings.getSystemSetting( SettingKey.DATA_IMPORT_REQUIRE_ATTRIBUTE_OPTION_COMBO ) )
            .forceDataInput( inputUtils.canForceDataInput( currentUser, options.isForce() ) )

            // data fetching state
            .dataElementCallable( new IdentifiableObjectCallable<>(
                identifiableObjectManager, DataElement.class, dataElementIdScheme, null ) )
            .orgUnitCallable( new IdentifiableObjectCallable<>(
                identifiableObjectManager, OrganisationUnit.class, orgUnitIdScheme,
                trimToNull( data.getOrgUnit() ) ) )
            .categoryOptionComboCallable( new CategoryOptionComboAclCallable(
                categoryService, categoryOptComboIdScheme, null ) )
            .attributeOptionComboCallable( new CategoryOptionComboAclCallable(
                categoryService, categoryOptComboIdScheme, null ) )
            .periodCallable( new PeriodCallable( periodService, null,
                trimToNull( data.getPeriod() ) ) )

            // data processing
            .dataValueBatchHandler( batchHandlerFactory
                .createBatchHandler( DataValueBatchHandler.class ).init() )
            .auditBatchHandler( skipAudit ? null
                : batchHandlerFactory.createBatchHandler( DataValueAuditBatchHandler.class ).init() )
            .singularNameForType( klass -> schemaService.getDynamicSchema( klass ).getSingular() )
            .build();
    }

    private void logDataValueSetImportContextInfo( ImportContext context )
    {
        log.info( String.format( "Is ISO calendar: %b, skip lock exception check: %b",
            context.isIso8601(), context.isSkipLockExceptionCheck() ) );
        log.info( String.format( "Skip audit: %b, has authority to skip: %b",
            context.isSkipAudit(), context.isHasSkipAuditAuth() ) );
        log.info( "Import options: " + context.getImportOptions() );
        log.info( String.format(
            "Identifier scheme: %s, data element: %s, org unit: %s, category option combo: %s, data set: %s",
            context.getIdScheme(), context.getDataElementIdScheme(), context.getOrgUnitIdScheme(),
            context.getCategoryOptComboIdScheme(), context.getDataSetIdScheme() ) );
    }

    private static IdScheme createIdScheme( IdScheme fromDataValueSet, ImportOptions options,
        Function<IdSchemes, IdScheme> getter )
    {
        IdScheme fromData = IdScheme.from( fromDataValueSet );
        return fromData.isNotNull() ? fromData : getter.apply( options.getIdSchemes() );
    }

    private ImportContext.DataSetContext createDataSetContext( ImportContext context, DataValueSet dataValueSet )
    {
        DataSet dataSet = dataValueSet.getDataSet() != null ? identifiableObjectManager.getObject( DataSet.class,
            context.getDataSetIdScheme(), dataValueSet.getDataSet() ) : null;
        CategoryOptionCombo outerAttrOptionCombo = null;

        if ( dataValueSet.getAttributeOptionCombo() != null )
        {
            outerAttrOptionCombo = context.getOptionComboMap().get(
                trimToNull( dataValueSet.getAttributeOptionCombo() ),
                context.getAttributeOptionComboCallable()
                    .setId( trimToNull( dataValueSet.getAttributeOptionCombo() ) ) );
        }
        else if ( dataValueSet.getAttributeCategoryOptions() != null && dataSet != null )
        {
            outerAttrOptionCombo = inputUtils.getAttributeOptionCombo( dataSet.getCategoryCombo(),
                new HashSet<>( dataValueSet.getAttributeCategoryOptions() ), context.getIdScheme() );
        }
        return ImportContext.DataSetContext.builder()
            .dataSet( dataSet )
            .outerPeriod(
                context.getPeriodMap().get( trimToNull( dataValueSet.getPeriod() ), context.getPeriodCallable() ) )
            .outerOrgUnit(
                context.getOrgUnitMap().get( trimToNull( dataValueSet.getOrgUnit() ), context.getOrgUnitCallable() ) )
            .fallbackCategoryOptionCombo( categoryService.getDefaultCategoryOptionCombo() )
            .outerAttrOptionCombo( outerAttrOptionCombo )
            .build();
    }

    private ImportContext.DataValueContext createDataValueContext( int index,
        org.hisp.dhis.dxf2.datavalue.DataValue dataValue, ImportContext context, DataSetContext dataSetContext )
    {
        return ImportContext.DataValueContext.builder()
            .index( index )
            .dataElement( context.getDataElementMap().get( trimToNull( dataValue.getDataElement() ),
                context.getDataElementCallable().setId( trimToNull( dataValue.getDataElement() ) ) ) )
            .period( dataSetContext.getOuterPeriod() != null ? dataSetContext.getOuterPeriod()
                : context.getPeriodMap().get( trimToNull( dataValue.getPeriod() ),
                    context.getPeriodCallable().setId( trimToNull( dataValue.getPeriod() ) ) ) )
            .orgUnit( dataSetContext.getOuterOrgUnit() != null ? dataSetContext.getOuterOrgUnit()
                : context.getOrgUnitMap().get( trimToNull( dataValue.getOrgUnit() ),
                    context.getOrgUnitCallable().setId( trimToNull( dataValue.getOrgUnit() ) ) ) )
            .categoryOptionCombo( context.getOptionComboMap().get(
                trimToNull( dataValue.getCategoryOptionCombo() ),
                context.getCategoryOptionComboCallable().setId( trimToNull( dataValue.getCategoryOptionCombo() ) ) ) )
            .attrOptionCombo(
                dataSetContext.getOuterAttrOptionCombo() != null ? dataSetContext.getOuterAttrOptionCombo()
                    : context.getOptionComboMap().get( trimToNull( dataValue.getAttributeOptionCombo() ),
                        context.getAttributeOptionComboCallable()
                            .setId( trimToNull( dataValue.getAttributeOptionCombo() ) ) ) )
            .build();
    }

    private DataValue createDataValue( org.hisp.dhis.dxf2.datavalue.DataValue dataValue, ImportContext context,
        ImportContext.DataValueContext valueContext, Date now )
    {
        DataValue internalValue = new DataValue();

        internalValue.setDataElement( valueContext.getDataElement() );
        internalValue.setPeriod( valueContext.getPeriod() );
        internalValue.setSource( valueContext.getOrgUnit() );
        internalValue.setCategoryOptionCombo( valueContext.getCategoryOptionCombo() );
        internalValue.setAttributeOptionCombo( valueContext.getAttrOptionCombo() );
        internalValue.setValue( trimToNull( dataValue.getValue() ) );
        internalValue.setStoredBy( context.getStoredBy( dataValue ) );
        internalValue.setCreated( dataValue.hasCreated() ? parseDate( dataValue.getCreated() ) : now );
        internalValue.setLastUpdated( dataValue.hasLastUpdated() ? parseDate( dataValue.getLastUpdated() ) : now );
        internalValue.setComment( trimToNull( dataValue.getComment() ) );
        internalValue.setFollowup( dataValue.getFollowup() );
        internalValue.setDeleted( BooleanUtils.isTrue( dataValue.getDeleted() ) );
        return internalValue;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void handleComplete( DataSet dataSet, Date completeDate, Period period, OrganisationUnit orgUnit,
        CategoryOptionCombo attributeOptionCombo, String currentUserName, ImportSummary summary )
    {
        if ( orgUnit == null )
        {
            summary.addConflict( OrganisationUnit.class.getSimpleName(), ERROR_OBJECT_NEEDED_TO_COMPLETE );
            return;
        }

        if ( period == null )
        {
            summary.addConflict( Period.class.getSimpleName(), ERROR_OBJECT_NEEDED_TO_COMPLETE );
            return;
        }

        period = periodService.reloadPeriod( period );

        CompleteDataSetRegistration completeAlready = registrationService
            .getCompleteDataSetRegistration( dataSet, period, orgUnit, attributeOptionCombo );

        if ( completeAlready != null )
        {
            // At this point, DataSet is completed. Override, eventual
            // non-completeness
            completeAlready.setDate( completeDate );
            completeAlready.setStoredBy( currentUserName );
            completeAlready.setLastUpdated( new Date() );
            completeAlready.setLastUpdatedBy( currentUserName );
            completeAlready.setCompleted( true );

            registrationService.updateCompleteDataSetRegistration( completeAlready );
        }
        else
        {
            CompleteDataSetRegistration registration = new CompleteDataSetRegistration( dataSet, period, orgUnit,
                attributeOptionCombo, completeDate, currentUserName, new Date(), currentUserName, true );

            registrationService.saveCompleteDataSetRegistration( registration );
        }

        summary.setDataSetComplete( DateUtils.getMediumDateString( completeDate ) );
    }

}
