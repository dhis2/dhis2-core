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

import static com.google.common.base.Preconditions.checkNotNull;
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
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.BooleanUtils;
import org.hisp.dhis.calendar.CalendarService;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.DateRange;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.commons.util.StreamUtils;
import org.hisp.dhis.dataapproval.DataApproval;
import org.hisp.dhis.dataapproval.DataApprovalService;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.LockExceptionStore;
import org.hisp.dhis.datavalue.AggregateAccessManager;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
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
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.quick.BatchHandler;
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
public class DefaultDataValueSetService
    implements DataValueSetService
{
    private static final String ERROR_OBJECT_NEEDED_TO_COMPLETE = "Must be provided to complete data set";

    private static final int CACHE_MISS_THRESHOLD = 250;

    private final IdentifiableObjectManager identifiableObjectManager;

    private final CategoryService categoryService;

    private final OrganisationUnitService organisationUnitService;

    private final PeriodService periodService;

    private final DataApprovalService approvalService;

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

    private final AggregateAccessManager accessManager;

    private final DhisConfigurationProvider config;

    private final ObjectMapper jsonMapper;

    public DefaultDataValueSetService(
        IdentifiableObjectManager identifiableObjectManager,
        CategoryService categoryService,
        OrganisationUnitService organisationUnitService,
        PeriodService periodService,
        DataApprovalService approvalService,
        BatchHandlerFactory batchHandlerFactory,
        CompleteDataSetRegistrationService registrationService,
        CurrentUserService currentUserService,
        DataValueSetStore dataValueSetStore,
        SystemSettingManager systemSettingManager,
        LockExceptionStore lockExceptionStore,
        I18nManager i18nManager,
        Notifier notifier,
        InputUtils inputUtils,
        CalendarService calendarService,
        DataValueService dataValueService,
        FileResourceService fileResourceService,
        AclService aclService,
        AggregateAccessManager accessManager,
        DhisConfigurationProvider config,
        ObjectMapper jsonMapper )
    {
        checkNotNull( identifiableObjectManager );
        checkNotNull( categoryService );
        checkNotNull( organisationUnitService );
        checkNotNull( periodService );
        checkNotNull( approvalService );
        checkNotNull( batchHandlerFactory );
        checkNotNull( registrationService );
        checkNotNull( currentUserService );
        checkNotNull( dataValueSetStore );
        checkNotNull( systemSettingManager );
        checkNotNull( lockExceptionStore );
        checkNotNull( i18nManager );
        checkNotNull( notifier );
        checkNotNull( inputUtils );
        checkNotNull( calendarService );
        checkNotNull( dataValueService );
        checkNotNull( fileResourceService );
        checkNotNull( aclService );
        checkNotNull( accessManager );
        checkNotNull( config );
        checkNotNull( jsonMapper );

        this.identifiableObjectManager = identifiableObjectManager;
        this.categoryService = categoryService;
        this.organisationUnitService = organisationUnitService;
        this.periodService = periodService;
        this.approvalService = approvalService;
        this.batchHandlerFactory = batchHandlerFactory;
        this.registrationService = registrationService;
        this.currentUserService = currentUserService;
        this.dataValueSetStore = dataValueSetStore;
        this.systemSettingManager = systemSettingManager;
        this.lockExceptionStore = lockExceptionStore;
        this.i18nManager = i18nManager;
        this.notifier = notifier;
        this.inputUtils = inputUtils;
        this.calendarService = calendarService;
        this.dataValueService = dataValueService;
        this.fileResourceService = fileResourceService;
        this.aclService = aclService;
        this.accessManager = accessManager;
        this.config = config;
        this.jsonMapper = jsonMapper;
    }

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
                if ( IdentifiableProperty.CODE.toString().toLowerCase().equals( ouScheme.toLowerCase() ) )
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

    private ImportContext createDataValueSetImportContext( ImportOptions options, DataValueSet data )
    {
        options = ObjectUtils.firstNonNull( options, ImportOptions.getDefaultImportOptions() );

        final User currentUser = currentUserService.getCurrentUser();

        boolean auditEnabled = config.isEnabled( CHANGELOG_AGGREGATE );
        boolean hasSkipAuditAuth = currentUser != null
            && currentUser.isAuthorized( Authorities.F_SKIP_DATA_IMPORT_AUDIT );
        boolean skipAudit = (options.isSkipAudit() && hasSkipAuditAuth) || !auditEnabled;

        SystemSettingManager settings = systemSettingManager;

        return ImportContext.builder()
            .importOptions( options )
            .summary( new ImportSummary().setImportOptions( options ) )
            .isIso8601( calendarService.getSystemCalendar().isIso8601() )
            .skipLockExceptionCheck( !lockExceptionStore.anyExists() )
            .i18n( i18nManager.getI18n() )
            .currentUser( currentUser )
            .hasSkipAuditAuth( hasSkipAuditAuth )
            .skipAudit( skipAudit )
            .idScheme( createIdScheme( data.getIdSchemeProperty(), options, IdSchemes::getIdScheme ) )
            .dataElementIdScheme(
                createIdScheme( data.getDataElementIdSchemeProperty(), options, IdSchemes::getDataElementIdScheme ) )
            .orgUnitIdScheme(
                createIdScheme( data.getOrgUnitIdSchemeProperty(), options, IdSchemes::getOrgUnitIdScheme ) )
            .categoryOptComboIdScheme( createIdScheme( data.getCategoryOptionComboIdSchemeProperty(), options,
                IdSchemes::getCategoryOptionComboIdScheme ) )
            .dataSetIdScheme(
                createIdScheme( data.getDataSetIdSchemeProperty(), options, IdSchemes::getDataSetIdScheme ) )
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
        // Get meta-data maps
        // ---------------------------------------------------------------------

        IdentifiableObjectCallable<DataElement> dataElementCallable = new IdentifiableObjectCallable<>(
            identifiableObjectManager, DataElement.class, context.getDataElementIdScheme(), null );
        IdentifiableObjectCallable<OrganisationUnit> orgUnitCallable = new IdentifiableObjectCallable<>(
            identifiableObjectManager, OrganisationUnit.class, context.getOrgUnitIdScheme(),
            trimToNull( dataValueSet.getOrgUnit() ) );
        IdentifiableObjectCallable<CategoryOptionCombo> categoryOptionComboCallable = new CategoryOptionComboAclCallable(
            categoryService, context.getCategoryOptComboIdScheme(), null );
        IdentifiableObjectCallable<CategoryOptionCombo> attributeOptionComboCallable = new CategoryOptionComboAclCallable(
            categoryService, context.getCategoryOptComboIdScheme(), null );
        IdentifiableObjectCallable<Period> periodCallable = new PeriodCallable( periodService, null,
            trimToNull( dataValueSet.getPeriod() ) );

        // ---------------------------------------------------------------------
        // Heat caches
        // ---------------------------------------------------------------------

        if ( context.getImportOptions().isPreheatCacheDefaultFalse() )
        {
            context.getDataElementMap().load( identifiableObjectManager.getAll( DataElement.class ),
                o -> o.getPropertyValue( context.getDataElementIdScheme() ) );
            context.getOrgUnitMap().load( identifiableObjectManager.getAll( OrganisationUnit.class ),
                o -> o.getPropertyValue( context.getOrgUnitIdScheme() ) );
            context.getOptionComboMap().load( identifiableObjectManager.getAll( CategoryOptionCombo.class ),
                o -> o.getPropertyValue( context.getCategoryOptComboIdScheme() ) );
        }

        // ---------------------------------------------------------------------
        // Get outer meta-data
        // ---------------------------------------------------------------------

        DataSet dataSet = dataValueSet.getDataSet() != null
            ? identifiableObjectManager.getObject( DataSet.class, context.getDataSetIdScheme(),
                dataValueSet.getDataSet() )
            : null;

        Date completeDate = parseDate( dataValueSet.getCompleteDate() );

        Period outerPeriod = context.getPeriodMap().get( trimToNull( dataValueSet.getPeriod() ), periodCallable );

        OrganisationUnit outerOrgUnit = context.getOrgUnitMap().get( trimToNull( dataValueSet.getOrgUnit() ),
            orgUnitCallable );

        CategoryOptionCombo fallbackCategoryOptionCombo = categoryService.getDefaultCategoryOptionCombo();

        CategoryOptionCombo outerAttrOptionCombo = null;

        Set<DataElement> dataSetDataElements = dataSet != null ? dataSet.getDataElements() : new HashSet<>();

        if ( dataValueSet.getAttributeOptionCombo() != null )
        {
            outerAttrOptionCombo = context.getOptionComboMap().get(
                trimToNull( dataValueSet.getAttributeOptionCombo() ),
                attributeOptionComboCallable.setId( trimToNull( dataValueSet.getAttributeOptionCombo() ) ) );
        }
        else if ( dataValueSet.getAttributeCategoryOptions() != null )
        {
            outerAttrOptionCombo = inputUtils.getAttributeOptionCombo( dataSet.getCategoryCombo(),
                new HashSet<>( dataValueSet.getAttributeCategoryOptions() ), context.getIdScheme() );
        }

        // ---------------------------------------------------------------------
        // Validation
        // ---------------------------------------------------------------------

        if ( dataSet == null && trimToNull( dataValueSet.getDataSet() ) != null )
        {
            context.error().addConflict( dataValueSet.getDataSet(), "Data set not found or not accessible" );
        }

        if ( dataSet != null && !aclService.canDataWrite( context.getCurrentUser(), dataSet ) )
        {
            context.error().addConflict( dataValueSet.getDataSet(),
                "User does not have write access for DataSet: " + dataSet.getUid() );
        }

        if ( dataSet == null && context.isStrictDataElements() )
        {
            context.error().addConflict( "DATA_IMPORT_STRICT_DATA_ELEMENTS", "A valid dataset is required" );
        }

        if ( outerOrgUnit == null && trimToNull( dataValueSet.getOrgUnit() ) != null )
        {
            context.error().addConflict( dataValueSet.getOrgUnit(), "Org unit not found or not accessible" );
        }

        if ( outerAttrOptionCombo == null && trimToNull( dataValueSet.getAttributeOptionCombo() ) != null )
        {
            context.error().addConflict( dataValueSet.getAttributeOptionCombo(),
                "Attribute option combo not found or not accessible" );
        }

        if ( context.getSummary().isStatus( ImportStatus.ERROR ) )
        {
            context.getSummary().setDescription( "Import process was aborted" );
            notifier.notify( id, WARN, "Import process aborted", true )
                .addJobSummary( id, context.getSummary(), ImportSummary.class );
            dataValueSet.close();
            return context.getSummary();
        }

        if ( dataSet != null && completeDate != null )
        {
            notifier.notify( id, notificationLevel, "Completing data set" );
            handleComplete( dataSet, completeDate, outerPeriod, outerOrgUnit, fallbackCategoryOptionCombo,
                context.getCurrentUserName(), context.getSummary() ); // TODO
        }
        else
        {
            context.getSummary().setDataSetComplete( Boolean.FALSE.toString() );
        }

        final Set<OrganisationUnit> currentOrgUnits = currentUserService.getCurrentUserOrganisationUnits();

        BatchHandler<DataValue> dataValueBatchHandler = batchHandlerFactory
            .createBatchHandler( DataValueBatchHandler.class ).init();
        BatchHandler<DataValueAudit> auditBatchHandler = context.isSkipAudit() ? null
            : batchHandlerFactory.createBatchHandler( DataValueAuditBatchHandler.class ).init();

        int importCount = 0;
        int updateCount = 0;
        int deleteCount = 0;
        int totalCount = 0;

        // ---------------------------------------------------------------------
        // Data values
        // ---------------------------------------------------------------------

        Date now = new Date();

        clock.logTime( "Validated outer meta-data" );
        notifier.notify( id, notificationLevel, "Importing data values" );

        while ( dataValueSet.hasNextDataValue() )
        {
            totalCount++;

            org.hisp.dhis.dxf2.datavalue.DataValue dataValue = dataValueSet.getNextDataValue();

            DataValueImportContext valueContext = DataValueImportContext.builder()
                .dataValue( dataValue )
                .dataElement( context.getDataElementMap().get( trimToNull( dataValue.getDataElement() ),
                    dataElementCallable.setId( trimToNull( dataValue.getDataElement() ) ) ) )
                .period( outerPeriod != null ? outerPeriod
                    : context.getPeriodMap().get( trimToNull( dataValue.getPeriod() ),
                        periodCallable.setId( trimToNull( dataValue.getPeriod() ) ) ) )
                .orgUnit( outerOrgUnit != null ? outerOrgUnit
                    : context.getOrgUnitMap().get( trimToNull( dataValue.getOrgUnit() ),
                        orgUnitCallable.setId( trimToNull( dataValue.getOrgUnit() ) ) ) )
                .categoryOptionCombo( context.getOptionComboMap().get(
                    trimToNull( dataValue.getCategoryOptionCombo() ),
                    categoryOptionComboCallable.setId( trimToNull( dataValue.getCategoryOptionCombo() ) ) ) )
                .attrOptionCombo( outerAttrOptionCombo != null ? outerAttrOptionCombo
                    : context.getOptionComboMap().get( trimToNull( dataValue.getAttributeOptionCombo() ),
                        attributeOptionComboCallable.setId( trimToNull( dataValue.getAttributeOptionCombo() ) ) ) )
                .build();

            // -----------------------------------------------------------------
            // Potentially heat caches
            // -----------------------------------------------------------------

            if ( !context.getDataElementMap().isCacheLoaded()
                && context.getDataElementMap().getCacheMissCount() > CACHE_MISS_THRESHOLD )
            {
                context.getDataElementMap().load( identifiableObjectManager.getAll( DataElement.class ),
                    o -> o.getPropertyValue( context.getDataElementIdScheme() ) );

                log.info( "Data element cache heated after cache miss threshold reached" );
            }

            if ( !context.getOrgUnitMap().isCacheLoaded()
                && context.getOrgUnitMap().getCacheMissCount() > CACHE_MISS_THRESHOLD )
            {
                context.getOrgUnitMap().load( identifiableObjectManager.getAll( OrganisationUnit.class ),
                    o -> o.getPropertyValue( context.getOrgUnitIdScheme() ) );

                log.info( "Org unit cache heated after cache miss threshold reached" );
            }

            if ( !context.getOptionComboMap().isCacheLoaded()
                && context.getOptionComboMap().getCacheMissCount() > CACHE_MISS_THRESHOLD )
            {
                context.getOptionComboMap().load( identifiableObjectManager.getAll( CategoryOptionCombo.class ),
                    o -> o.getPropertyValue( context.getCategoryOptComboIdScheme() ) );

                log.info( "Category Option Combo cache heated after cache miss threshold reached" );
            }

            // -----------------------------------------------------------------
            // Validation
            // -----------------------------------------------------------------

            if ( valueContext.getDataElement() == null )
            {
                context.addConflict( dataValue.getDataElement(), "Data element not found or not accessible" );
                continue;
            }

            if ( valueContext.getPeriod() == null )
            {
                context.addConflict( dataValue.getPeriod(), "Period not valid" );
                continue;
            }

            if ( valueContext.getOrgUnit() == null )
            {
                context.addConflict( dataValue.getOrgUnit(), "Organisation unit not found or not accessible" );
                continue;
            }

            if ( valueContext.getCategoryOptionCombo() == null
                && trimToNull( dataValue.getCategoryOptionCombo() ) != null )
            {
                context.addConflict( dataValue.getCategoryOptionCombo(),
                    "Category option combo not found or not accessible for writing data" );
                continue;
            }

            if ( valueContext.getCategoryOptionCombo() != null )
            {
                List<String> errors = accessManager.canWrite( context.getCurrentUser(),
                    valueContext.getCategoryOptionCombo() );

                if ( !errors.isEmpty() )
                {
                    context.addConflicts( "dataValueSet", errors );
                    continue;
                }
            }

            if ( valueContext.getAttrOptionCombo() == null
                && trimToNull( dataValue.getAttributeOptionCombo() ) != null )
            {
                context.addConflict( dataValue.getAttributeOptionCombo(),
                    "Attribute option combo not found or not accessible for writing data" );
                continue;
            }

            if ( valueContext.getAttrOptionCombo() != null )
            {
                List<String> errors = accessManager.canWrite( context.getCurrentUser(),
                    valueContext.getAttrOptionCombo() );

                if ( !errors.isEmpty() )
                {
                    context.addConflicts( "dataValueSet", errors );
                    continue;
                }
            }

            boolean inUserHierarchy = context.getOrgUnitInHierarchyMap().get( valueContext.getOrgUnit().getUid(),
                () -> valueContext.getOrgUnit().isDescendant( currentOrgUnits ) );

            if ( !inUserHierarchy )
            {
                context.addConflict( valueContext.getOrgUnit().getUid(),
                    "Organisation unit not in hierarchy of current user: " + context.getCurrentUserName() );
                continue;
            }

            if ( dataValue.isNullValue() && !dataValue.isDeletedValue() )
            {
                context.addConflict( "Value",
                    "Data value or comment not specified for data element: " + valueContext.getDataElement().getUid() );
                continue;
            }

            dataValue.setValueForced(
                ValidationUtils.normalizeBoolean( dataValue.getValue(),
                    valueContext.getDataElement().getValueType() ) );

            String valueValid = ValidationUtils.dataValueIsValid( dataValue.getValue(), valueContext.getDataElement() );

            if ( valueValid != null )
            {
                context.addConflict( dataValue.getValue(),
                    context.getI18n().getString( valueValid ) + ", must match data element type: "
                        + valueContext.getDataElement().getUid() );
                continue;
            }

            String commentValid = ValidationUtils.commentIsValid( dataValue.getComment() );

            if ( commentValid != null )
            {
                context.addConflict( "Comment", context.getI18n().getString( commentValid ) );
                continue;
            }

            Optional<Set<String>> optionCodes = context.getDataElementOptionsMap().get(
                valueContext.getDataElement().getUid(),
                () -> valueContext.getDataElement().hasOptionSet()
                    ? Optional.of( valueContext.getDataElement().getOptionSet().getOptionCodesAsSet() )
                    : Optional.empty() );

            if ( optionCodes.isPresent() && !optionCodes.get().contains( dataValue.getValue() ) )
            {
                context.addConflict( dataValue.getValue(),
                    "Data value is not a valid option of the data element option set: "
                        + valueContext.getDataElement().getUid() );
                continue;
            }

            // -----------------------------------------------------------------
            // Constraints
            // -----------------------------------------------------------------

            if ( valueContext.getCategoryOptionCombo() == null )
            {
                if ( context.isRequireCategoryOptionCombo() )
                {
                    context.addConflict( dataValue.getValue(),
                        "Category option combo is required but is not specified" );
                    continue;
                }
                else
                {
                    valueContext.setCategoryOptionCombo( fallbackCategoryOptionCombo );
                }
            }

            if ( valueContext.getAttrOptionCombo() == null )
            {
                if ( context.isRequireAttrOptionCombo() )
                {
                    context.addConflict( dataValue.getValue(),
                        "Attribute option combo is required but is not specified" );
                    continue;
                }
                else
                {
                    valueContext.setAttrOptionCombo( fallbackCategoryOptionCombo );
                }
            }

            if ( context.isStrictPeriods() && !context.getDataElementPeriodTypesMap()
                .get( valueContext.getDataElement().getUid(),
                    valueContext.getDataElement()::getPeriodTypes )
                .contains( valueContext.getPeriod().getPeriodType() ) )
            {
                context.addConflict( dataValue.getPeriod(),
                    "Period type of period: " + valueContext.getPeriod().getIsoDate() + " not valid for data element: "
                        + valueContext.getDataElement().getUid() );
                continue;
            }

            if ( context.isStrictDataElements() && !dataSetDataElements.contains( valueContext.getDataElement() ) )
            {
                context.addConflict( "DATA_IMPORT_STRICT_DATA_ELEMENTS",
                    "Data element: " + dataValue.getDataElement() + " is not part of dataset: " + dataSet.getUid() );
                continue;
            }

            if ( context.isStrictCategoryOptionCombos()
                && !context.getDataElementCategoryOptionComboMap().get( valueContext.getDataElement().getUid(),
                    valueContext.getDataElement()::getCategoryOptionCombos )
                    .contains( valueContext.getCategoryOptionCombo() ) )
            {
                context.addConflict( valueContext.getCategoryOptionCombo().getUid(),
                    "Category option combo: " + valueContext.getCategoryOptionCombo().getUid()
                        + " must be part of category combo of data element: "
                        + valueContext.getDataElement().getUid() );
                continue;
            }

            if ( context.isStrictAttrOptionCombos()
                && !context.getDataElementAttrOptionComboMap().get( valueContext.getDataElement().getUid(),
                    valueContext.getDataElement()::getDataSetCategoryOptionCombos )
                    .contains( valueContext.getAttrOptionCombo() ) )
            {
                context.addConflict( valueContext.getAttrOptionCombo().getUid(),
                    "Attribute option combo: " + valueContext.getAttrOptionCombo().getUid()
                        + " must be part of category combo of data sets of data element: "
                        + valueContext.getDataElement().getUid() );
                continue;
            }

            if ( context.isStrictOrgUnits()
                && BooleanUtils
                    .isFalse( context.getDataElementOrgUnitMap().get(
                        valueContext.getDataElement().getUid() + valueContext.getOrgUnit().getUid(),
                        () -> valueContext.getOrgUnit().hasDataElement( valueContext.getDataElement() ) ) ) )
            {
                context.addConflict( valueContext.getOrgUnit().getUid(),
                    "Data element: " + valueContext.getDataElement().getUid()
                        + " must be assigned through data sets to organisation unit: "
                        + valueContext.getOrgUnit().getUid() );
                continue;
            }

            boolean zeroAndInsignificant = ValidationUtils.dataValueIsZeroAndInsignificant( dataValue.getValue(),
                valueContext.getDataElement() );

            if ( zeroAndInsignificant )
            {
                continue; // Ignore value
            }

            String storedByValid = ValidationUtils.storedByIsValid( dataValue.getStoredBy() );

            if ( storedByValid != null )
            {
                context.addConflict( dataValue.getStoredBy(), context.getI18n().getString( storedByValid ) );
                continue;
            }

            String storedBy = dataValue.getStoredBy() == null || dataValue.getStoredBy().trim().isEmpty()
                ? context.getCurrentUserName()
                : dataValue.getStoredBy();

            final CategoryOptionCombo aoc = valueContext.getAttrOptionCombo();

            DateRange aocDateRange = dataSet != null
                ? context.getAttrOptionComboDateRangeMap().get(
                    valueContext.getAttrOptionCombo().getUid() + dataSet.getUid(),
                    () -> aoc.getDateRange( dataSet ) )
                : context.getAttrOptionComboDateRangeMap().get(
                    valueContext.getAttrOptionCombo().getUid() + valueContext.getDataElement().getUid(),
                    () -> aoc.getDateRange( valueContext.getDataElement() ) );

            if ( (aocDateRange.getStartDate() != null
                && aocDateRange.getStartDate().after( valueContext.getPeriod().getEndDate() ))
                || (aocDateRange.getEndDate() != null
                    && aocDateRange.getEndDate().before( valueContext.getPeriod().getStartDate() )) )
            {
                context.addConflict( valueContext.getOrgUnit().getUid(),
                    "Period: " + valueContext.getPeriod().getIsoDate()
                        + " is not within date range of attribute option combo: "
                        + valueContext.getAttrOptionCombo().getUid() );
                continue;
            }

            if ( !context.getAttrOptionComboOrgUnitMap()
                .get( valueContext.getAttrOptionCombo().getUid() + valueContext.getOrgUnit().getUid(), () -> {
                    Set<OrganisationUnit> aocOrgUnits = aoc.getOrganisationUnits();
                    return aocOrgUnits == null || valueContext.getOrgUnit().isDescendant( aocOrgUnits );
                } ) )
            {
                context.addConflict( valueContext.getOrgUnit().getUid(),
                    "Organisation unit: " + valueContext.getOrgUnit().getUid()
                        + " is not valid for attribute option combo: "
                        + valueContext.getAttrOptionCombo().getUid() );
                continue;
            }

            final DataSet approvalDataSet = dataSet != null ? dataSet
                : context.getDataElementDataSetMap().get( valueContext.getDataElement().getUid(),
                    valueContext.getDataElement()::getApprovalDataSet );

            // Data element is assigned to at least one data set
            if ( approvalDataSet != null && !context.isForceDataInput() )
            {
                if ( context.getDataSetLockedMap().get(
                    approvalDataSet.getUid() + valueContext.getPeriod().getUid() + valueContext.getOrgUnit().getUid(),
                    () -> isLocked( context.getCurrentUser(), approvalDataSet, valueContext.getPeriod(),
                        valueContext.getOrgUnit(),
                        context.isSkipLockExceptionCheck() ) ) )
                {
                    context.addConflict( valueContext.getPeriod().getIsoDate(),
                        "Current date is past expiry days for period " +
                            valueContext.getPeriod().getIsoDate() + " and data set: " + approvalDataSet.getUid() );
                    continue;
                }

                Period latestFuturePeriod = context.getDataElementLatestFuturePeriodMap().get(
                    valueContext.getDataElement().getUid(),
                    valueContext.getDataElement()::getLatestOpenFuturePeriod );

                if ( valueContext.getPeriod().isAfter( latestFuturePeriod ) && context.isIso8601() )
                {
                    context.addConflict( valueContext.getPeriod().getIsoDate(), "Period: " +
                        valueContext.getPeriod().getIsoDate() + " is after latest open future period: "
                        + latestFuturePeriod.getIsoDate()
                        + " for data element: " + valueContext.getDataElement().getUid() );
                    continue;
                }

                DataApprovalWorkflow workflow = approvalDataSet.getWorkflow();

                if ( workflow != null )
                {
                    final String workflowPeriodAoc = workflow.getUid() + valueContext.getPeriod().getUid()
                        + valueContext.getAttrOptionCombo().getUid();

                    if ( context.getApprovalMap().get( valueContext.getOrgUnit().getUid() + workflowPeriodAoc, () -> {
                        DataApproval lowestApproval = DataApproval
                            .getLowestApproval( new DataApproval( null, workflow, valueContext.getPeriod(),
                                valueContext.getOrgUnit(), aoc ) );

                        return lowestApproval != null && context.getLowestApprovalLevelMap().get(
                            lowestApproval.getDataApprovalLevel().getUid()
                                + lowestApproval.getOrganisationUnit().getUid() + workflowPeriodAoc,
                            () -> approvalService.getDataApproval( lowestApproval ) != null );
                    } ) )
                    {
                        context.addConflict( valueContext.getOrgUnit().getUid(),
                            "Data is already approved for data set: " + approvalDataSet.getUid() + " period: "
                                + valueContext.getPeriod().getIsoDate()
                                + " organisation unit: " + valueContext.getOrgUnit().getUid()
                                + " attribute option combo: "
                                + valueContext.getAttrOptionCombo().getUid() );
                        continue;
                    }
                }
            }

            if ( approvalDataSet != null && !context.isForceDataInput()
                && !approvalDataSet.isDataInputPeriodAndDateAllowed( valueContext.getPeriod(), new Date() ) )
            {
                context.addConflict( valueContext.getOrgUnit().getUid(),
                    "Period: " + valueContext.getPeriod().getIsoDate() + " is not open for this data set at this time: "
                        + approvalDataSet.getUid() );
                continue;
            }

            if ( !context.isForceDataInput()
                && !context.getPeriodOpenForDataElement().get(
                    valueContext.getDataElement().getUid() + valueContext.getPeriod().getIsoDate(),
                    () -> valueContext.getDataElement().isDataInputAllowedForPeriodAndDate( valueContext.getPeriod(),
                        new Date() ) ) )
            {
                context.addConflict( valueContext.getOrgUnit().getUid(),
                    "Period " + valueContext.getPeriod().getName()
                        + " does not conform to the open periods of associated data sets" );
                continue;
            }

            DataValue actualDataValue = null;
            if ( context.getStrategy().isDelete() && valueContext.getDataElement().isFileType() )
            {
                actualDataValue = dataValueService.getDataValue( valueContext.getDataElement(),
                    valueContext.getPeriod(), valueContext.getOrgUnit(), valueContext.getCategoryOptionCombo(),
                    valueContext.getAttrOptionCombo() );
                if ( actualDataValue == null )
                {
                    context.addConflict( valueContext.getDataElement().getUid(),
                        "No data value for file resource exist for the given combination" );
                    continue;
                }
            }

            // -----------------------------------------------------------------
            // Create data value
            // -----------------------------------------------------------------

            DataValue internalValue = createDataValue( dataValue, valueContext.getDataElement(),
                valueContext.getPeriod(), valueContext.getOrgUnit(), valueContext.getCategoryOptionCombo(),
                valueContext.getAttrOptionCombo(), storedBy, now );

            // -----------------------------------------------------------------
            // Save, update or delete data value
            // -----------------------------------------------------------------

            DataValue existingValue = !context.isSkipExistingCheck()
                ? dataValueBatchHandler.findObject( internalValue )
                : null;

            // -----------------------------------------------------------------
            // Check soft deleted data values on update and import
            // -----------------------------------------------------------------

            final ImportStrategy strategy = context.getStrategy();
            final boolean dryRun = context.isDryRun();
            final boolean skipAudit = context.isSkipAudit();
            if ( !context.isSkipExistingCheck() && existingValue != null && !existingValue.isDeleted() )
            {
                if ( strategy.isCreateAndUpdate() || strategy.isUpdate() )
                {
                    AuditType auditType = AuditType.UPDATE;

                    if ( internalValue.isNullValue() || internalValue.isDeleted() )
                    {
                        internalValue.setDeleted( true );

                        auditType = AuditType.DELETE;

                        deleteCount++;
                    }
                    else
                    {
                        updateCount++;
                    }

                    if ( !dryRun )
                    {
                        dataValueBatchHandler.updateObject( internalValue );

                        if ( !skipAudit )
                        {
                            DataValueAudit auditValue = new DataValueAudit( internalValue, existingValue.getValue(),
                                storedBy, auditType );

                            auditBatchHandler.addObject( auditValue );
                        }

                        if ( valueContext.getDataElement().isFileType() )
                        {
                            FileResource fr = fileResourceService.getFileResource( internalValue.getValue() );

                            fr.setAssigned( true );

                            fileResourceService.updateFileResource( fr );
                        }

                    }
                }
                else if ( strategy.isDelete() )
                {
                    internalValue.setDeleted( true );

                    deleteCount++;

                    if ( !dryRun )
                    {
                        if ( valueContext.getDataElement().isFileType() && actualDataValue != null )
                        {
                            FileResource fr = fileResourceService.getFileResource( actualDataValue.getValue() );

                            fileResourceService.updateFileResource( fr );
                        }

                        dataValueBatchHandler.updateObject( internalValue );

                        if ( !skipAudit )
                        {
                            DataValueAudit auditValue = new DataValueAudit( internalValue, existingValue.getValue(),
                                storedBy, AuditType.DELETE );

                            auditBatchHandler.addObject( auditValue );
                        }
                    }
                }
            }
            else
            {
                if ( strategy.isCreateAndUpdate() || strategy.isCreate() )
                {
                    if ( !internalValue.isNullValue() ) // Ignore null values
                    {
                        if ( existingValue != null && existingValue.isDeleted() )
                        {
                            importCount++;

                            if ( !dryRun )
                            {
                                dataValueBatchHandler.updateObject( internalValue );

                                if ( valueContext.getDataElement().isFileType() )
                                {
                                    FileResource fr = fileResourceService.getFileResource( internalValue.getValue() );

                                    fr.setAssigned( true );

                                    fileResourceService.updateFileResource( fr );
                                }
                            }
                        }
                        else
                        {
                            boolean added = false;

                            if ( !dryRun )
                            {
                                added = dataValueBatchHandler.addObject( internalValue );

                                if ( added && valueContext.getDataElement().isFileType() )
                                {
                                    FileResource fr = fileResourceService.getFileResource( internalValue.getValue() );

                                    fr.setAssigned( true );

                                    fileResourceService.updateFileResource( fr );
                                }
                            }

                            if ( dryRun || added )
                            {
                                importCount++;
                            }
                        }
                    }
                }
            }
        }

        dataValueBatchHandler.flush();

        if ( !context.isSkipAudit() )
        {
            auditBatchHandler.flush();
        }

        int ignores = totalCount - importCount - updateCount - deleteCount;

        context.getSummary()
            .setImportCount( new ImportCount( importCount, updateCount, ignores, deleteCount ) )
            .setStatus( context.getSummary().getConflicts().isEmpty() ? ImportStatus.SUCCESS : ImportStatus.WARNING )
            .setDescription( "Import process completed successfully" );

        clock.logTime( "Data value import done, total: " + totalCount + ", import: " + importCount + ", update: "
            + updateCount + ", delete: " + deleteCount );
        notifier.notify( id, notificationLevel, "Import done", true )
            .addJobSummary( id, notificationLevel, context.getSummary(), ImportSummary.class );

        dataValueSet.close();

        return context.getSummary();
    }

    private DataValue createDataValue( org.hisp.dhis.dxf2.datavalue.DataValue dataValue, DataElement dataElement,
        Period period, OrganisationUnit orgUnit, CategoryOptionCombo categoryOptionCombo,
        CategoryOptionCombo attrOptionCombo, String storedBy, Date now )
    {
        DataValue internalValue = new DataValue();

        internalValue.setDataElement( dataElement );
        internalValue.setPeriod( period );
        internalValue.setSource( orgUnit );
        internalValue.setCategoryOptionCombo( categoryOptionCombo );
        internalValue.setAttributeOptionCombo( attrOptionCombo );
        internalValue.setValue( trimToNull( dataValue.getValue() ) );
        internalValue.setStoredBy( storedBy );
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
            summary.getConflicts()
                .add( new ImportConflict( OrganisationUnit.class.getSimpleName(), ERROR_OBJECT_NEEDED_TO_COMPLETE ) );
            return;
        }

        if ( period == null )
        {
            summary.getConflicts()
                .add( new ImportConflict( Period.class.getSimpleName(), ERROR_OBJECT_NEEDED_TO_COMPLETE ) );
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

    /**
     * Checks whether the given data set is locked.
     *
     * @param dataSet the data set.
     * @param period the period.
     * @param organisationUnit the organisation unit.
     * @param skipLockExceptionCheck whether to skip lock exception check.
     */
    private boolean isLocked( User user, DataSet dataSet, Period period, OrganisationUnit organisationUnit,
        boolean skipLockExceptionCheck )
    {
        return dataSet.isLocked( user, period, null )
            && (skipLockExceptionCheck || lockExceptionStore.getCount( dataSet, period, organisationUnit ) == 0L);
    }
}
