package org.hisp.dhis.dxf2.datavalueset;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import com.csvreader.CsvReader;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.hisp.dhis.commons.collection.CachingMap;
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
import org.hisp.dhis.i18n.I18n;
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
import org.hisp.dhis.period.PeriodType;
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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.hisp.dhis.system.notification.NotificationLevel.*;
import static org.hisp.dhis.util.DateUtils.parseDate;

import static org.hisp.dhis.external.conf.ConfigurationKey.CHANGELOG_AGGREGATE;

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
    public DataExportParams getFromUrl( Set<String> dataSets, Set<String> dataElementGroups, Set<String> periods, Date startDate, Date endDate,
        Set<String> organisationUnits, boolean includeChildren, Set<String> organisationUnitGroups, Set<String> attributeOptionCombos,
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

        if ( !params.hasPeriods() && !params.hasStartEndDate() && !params.hasLastUpdated() && !params.hasLastUpdatedDuration() )
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
            if ( !organisationUnitService.isInUserHierarchy( unit ) )
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
            CategoryOptionCombo optionCombo = categoryService.getDefaultCategoryOptionCombo(); //TODO

            CompleteDataSetRegistration registration = registrationService
                .getCompleteDataSetRegistration( params.getFirstDataSet(), params.getFirstPeriod(), params.getFirstOrganisationUnit(), optionCombo );

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

            SimpleNode simpleNode = complexNode.addChild( new SimpleNode( "categoryOptionCombo", categoryOptionCombo.getUid() ) );
            simpleNode.setAttribute( true );

            simpleNode = complexNode.addChild( new SimpleNode( "period", period != null ? period.getIsoDate() : "" ) );
            simpleNode.setAttribute( true );

            if ( organisationUnit != null )
            {
                if ( IdentifiableProperty.CODE.toString().toLowerCase().equals( ouScheme.toLowerCase() ) )
                {
                    simpleNode = complexNode.addChild( new SimpleNode( "orgUnit", organisationUnit.getCode() == null ? "" : organisationUnit.getCode() ) );
                    simpleNode.setAttribute( true );
                }
                else
                {
                    simpleNode = complexNode.addChild( new SimpleNode( "orgUnit", organisationUnit.getUid() == null ? "" : organisationUnit.getUid() ) );
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
    public ImportSummary saveDataValueSetPdf( InputStream in, ImportOptions importOptions )
    {
        return saveDataValueSetPdf( in, importOptions, null );
    }

    /**
     * There are specific id schemes for data elements and organisation units and
     * a generic id scheme for all objects. The specific id schemes will take
     * precedence over the generic id scheme. The generic id scheme also applies
     * to data set and category option combo.
     * <p>
     * The id schemes uses the following order of precedence:
     * <p>
     * <ul>
     * <li>Id scheme from the data value set</li>
     * <li>Id scheme from the import options</li>
     * <li>Default id scheme which is UID</li>
     * <ul>
     * <p>
     * If id scheme is specific in the data value set, any id schemes in the import
     * options will be ignored.
     */
    private ImportSummary saveDataValueSet( ImportOptions importOptions, JobConfiguration id, DataValueSet dataValueSet )
    {
        importOptions = ObjectUtils.firstNonNull( importOptions, ImportOptions.getDefaultImportOptions() );

        Clock clock = new Clock( log ).startClock().logTime( "Starting data value import, options: " + importOptions );
        NotificationLevel notificationLevel = importOptions.getNotificationLevel( INFO );
        notifier.clear( id ).notify( id, notificationLevel, "Process started" );

        ImportSummary summary = new ImportSummary()
            .setImportOptions( importOptions );

        boolean isIso8601 = calendarService.getSystemCalendar().isIso8601();
        boolean skipLockExceptionCheck = !lockExceptionStore.anyExists();

        log.info( String.format( "Is ISO calendar: %b, skip lock exception check: %b", isIso8601, skipLockExceptionCheck ) );

        I18n i18n = i18nManager.getI18n();
        final User currentUser = currentUserService.getCurrentUser();
        final String currentUserName = currentUser.getUsername();

        boolean auditEnabed = config.isEnabled( CHANGELOG_AGGREGATE );
        boolean hasSkipAuditAuth = currentUser != null && currentUser.isAuthorized( Authorities.F_SKIP_DATA_IMPORT_AUDIT );
        boolean skipAudit = ( importOptions.isSkipAudit() && hasSkipAuditAuth ) || !auditEnabed;

        log.info( String.format( "Skip audit: %b, has authority to skip: %b", skipAudit, hasSkipAuditAuth ) );

        // ---------------------------------------------------------------------
        // Get import options
        // ---------------------------------------------------------------------

        log.info( "Import options: " + importOptions );

        IdScheme dvSetIdScheme = IdScheme.from( dataValueSet.getIdSchemeProperty() );
        IdScheme dvSetDataElementIdScheme = IdScheme.from( dataValueSet.getDataElementIdSchemeProperty() );
        IdScheme dvSetOrgUnitIdScheme = IdScheme.from( dataValueSet.getOrgUnitIdSchemeProperty() );
        IdScheme dvSetCategoryOptComboIdScheme = IdScheme.from( dataValueSet.getCategoryOptionComboIdSchemeProperty() );
        IdScheme dvSetDataSetIdScheme = IdScheme.from( dataValueSet.getDataSetIdSchemeProperty() );

        log.info( "Data value set identifier scheme: " + dvSetIdScheme + ", data element: " + dvSetDataElementIdScheme +
            ", org unit: " + dvSetOrgUnitIdScheme + ", category option combo: " + dvSetCategoryOptComboIdScheme + ", data set: " + dvSetDataSetIdScheme );

        IdScheme idScheme = dvSetIdScheme.isNotNull() ? dvSetIdScheme : importOptions.getIdSchemes().getIdScheme();
        IdScheme dataElementIdScheme = dvSetDataElementIdScheme.isNotNull() ? dvSetDataElementIdScheme : importOptions.getIdSchemes().getDataElementIdScheme();
        IdScheme orgUnitIdScheme = dvSetOrgUnitIdScheme.isNotNull() ? dvSetOrgUnitIdScheme : importOptions.getIdSchemes().getOrgUnitIdScheme();
        IdScheme categoryOptComboIdScheme = dvSetCategoryOptComboIdScheme.isNotNull() ? dvSetCategoryOptComboIdScheme : importOptions.getIdSchemes().getCategoryOptionComboIdScheme();
        IdScheme dataSetIdScheme = dvSetDataSetIdScheme.isNotNull() ? dvSetDataSetIdScheme : importOptions.getIdSchemes().getDataSetIdScheme();

        log.info( "Identifier scheme: " + idScheme + ", data element: " + dataElementIdScheme +
            ", org unit: " + orgUnitIdScheme + ", category option combo: " + categoryOptComboIdScheme + ", data set: " + dataSetIdScheme );

        ImportStrategy strategy = dataValueSet.getStrategy() != null ?
            ImportStrategy.valueOf( dataValueSet.getStrategy() ) : importOptions.getImportStrategy();

        boolean dryRun = dataValueSet.getDryRun() != null ? dataValueSet.getDryRun() : importOptions.isDryRun();
        boolean skipExistingCheck = importOptions.isSkipExistingCheck();
        boolean strictPeriods = importOptions.isStrictPeriods() || (Boolean) systemSettingManager.getSystemSetting( SettingKey.DATA_IMPORT_STRICT_PERIODS );
        boolean strictDataElements = importOptions.isStrictDataElements() || (Boolean) systemSettingManager.getSystemSetting( SettingKey.DATA_IMPORT_STRICT_DATA_ELEMENTS );
        boolean strictCategoryOptionCombos = importOptions.isStrictCategoryOptionCombos() || (Boolean) systemSettingManager.getSystemSetting( SettingKey.DATA_IMPORT_STRICT_CATEGORY_OPTION_COMBOS );
        boolean strictAttrOptionCombos = importOptions.isStrictAttributeOptionCombos() || (Boolean) systemSettingManager.getSystemSetting( SettingKey.DATA_IMPORT_STRICT_ATTRIBUTE_OPTION_COMBOS );
        boolean strictOrgUnits = importOptions.isStrictOrganisationUnits() || (Boolean) systemSettingManager.getSystemSetting( SettingKey.DATA_IMPORT_STRICT_ORGANISATION_UNITS );
        boolean requireCategoryOptionCombo = importOptions.isRequireCategoryOptionCombo() || (Boolean) systemSettingManager.getSystemSetting( SettingKey.DATA_IMPORT_REQUIRE_CATEGORY_OPTION_COMBO );
        boolean requireAttrOptionCombo = importOptions.isRequireAttributeOptionCombo() || (Boolean) systemSettingManager.getSystemSetting( SettingKey.DATA_IMPORT_REQUIRE_ATTRIBUTE_OPTION_COMBO );
        boolean forceDataInput = inputUtils.canForceDataInput( currentUser, importOptions.isForce() );

        // ---------------------------------------------------------------------
        // Create meta-data maps
        // ---------------------------------------------------------------------

        CachingMap<String, DataElement> dataElementMap = new CachingMap<>();
        CachingMap<String, OrganisationUnit> orgUnitMap = new CachingMap<>();
        CachingMap<String, CategoryOptionCombo> optionComboMap = new CachingMap<>();
        CachingMap<String, DataSet> dataElementDataSetMap = new CachingMap<>();
        CachingMap<String, Period> periodMap = new CachingMap<>();
        CachingMap<String, Set<PeriodType>> dataElementPeriodTypesMap = new CachingMap<>();
        CachingMap<String, Set<CategoryOptionCombo>> dataElementCategoryOptionComboMap = new CachingMap<>();
        CachingMap<String, Set<CategoryOptionCombo>> dataElementAttrOptionComboMap = new CachingMap<>();
        CachingMap<String, Boolean> dataElementOrgUnitMap = new CachingMap<>();
        CachingMap<String, Boolean> dataSetLockedMap = new CachingMap<>();
        CachingMap<String, Period> dataElementLatestFuturePeriodMap = new CachingMap<>();
        CachingMap<String, Boolean> orgUnitInHierarchyMap = new CachingMap<>();
        CachingMap<String, DateRange> attrOptionComboDateRangeMap = new CachingMap<>();
        CachingMap<String, Boolean> attrOptionComboOrgUnitMap = new CachingMap<>();
        CachingMap<String, Optional<Set<String>>> dataElementOptionsMap = new CachingMap<>();
        CachingMap<String, Boolean> approvalMap = new CachingMap<>();
        CachingMap<String, Boolean> lowestApprovalLevelMap = new CachingMap<>();
        CachingMap<String, Boolean> periodOpenForDataElement = new CachingMap<>();

        // ---------------------------------------------------------------------
        // Get meta-data maps
        // ---------------------------------------------------------------------

        IdentifiableObjectCallable<DataElement> dataElementCallable = new IdentifiableObjectCallable<>(
            identifiableObjectManager, DataElement.class, dataElementIdScheme, null );
        IdentifiableObjectCallable<OrganisationUnit> orgUnitCallable = new IdentifiableObjectCallable<>(
            identifiableObjectManager, OrganisationUnit.class, orgUnitIdScheme, trimToNull( dataValueSet.getOrgUnit() ) );
        IdentifiableObjectCallable<CategoryOptionCombo> categoryOptionComboCallable = new CategoryOptionComboAclCallable(
            categoryService, categoryOptComboIdScheme, null );
        IdentifiableObjectCallable<CategoryOptionCombo> attributeOptionComboCallable = new CategoryOptionComboAclCallable(
            categoryService, categoryOptComboIdScheme, null );
        IdentifiableObjectCallable<Period> periodCallable = new PeriodCallable( periodService, null, trimToNull( dataValueSet.getPeriod() ) );

        // ---------------------------------------------------------------------
        // Heat caches
        // ---------------------------------------------------------------------

        if ( importOptions.isPreheatCacheDefaultFalse() )
        {
            dataElementMap.load( identifiableObjectManager.getAll( DataElement.class ), o -> o.getPropertyValue( dataElementIdScheme ) );
            orgUnitMap.load( identifiableObjectManager.getAll( OrganisationUnit.class ), o -> o.getPropertyValue( orgUnitIdScheme ) );
            optionComboMap.load( identifiableObjectManager.getAll( CategoryOptionCombo.class ), o -> o.getPropertyValue( categoryOptComboIdScheme ) );
        }

        // ---------------------------------------------------------------------
        // Get outer meta-data
        // ---------------------------------------------------------------------

        DataSet dataSet = dataValueSet.getDataSet() != null ? identifiableObjectManager.getObject( DataSet.class, dataSetIdScheme, dataValueSet.getDataSet() ) : null;

        Date completeDate = parseDate( dataValueSet.getCompleteDate() );

        Period outerPeriod = periodMap.get( trimToNull( dataValueSet.getPeriod() ), periodCallable );

        OrganisationUnit outerOrgUnit = orgUnitMap.get( trimToNull( dataValueSet.getOrgUnit() ), orgUnitCallable );

        CategoryOptionCombo fallbackCategoryOptionCombo = categoryService.getDefaultCategoryOptionCombo();

        CategoryOptionCombo outerAttrOptionCombo = null;

        Set<DataElement> dataSetDataElements = dataSet != null ? dataSet.getDataElements() : new HashSet<>();

        if ( dataValueSet.getAttributeOptionCombo() != null )
        {
            outerAttrOptionCombo = optionComboMap.get( trimToNull( dataValueSet.getAttributeOptionCombo() ), attributeOptionComboCallable.setId( trimToNull( dataValueSet.getAttributeOptionCombo() ) ) );
        }
        else if ( dataValueSet.getAttributeCategoryOptions() != null )
        {
            outerAttrOptionCombo = inputUtils.getAttributeOptionCombo( dataSet.getCategoryCombo(),
                new HashSet<>( dataValueSet.getAttributeCategoryOptions() ), idScheme );
        }

        // ---------------------------------------------------------------------
        // Validation
        // ---------------------------------------------------------------------

        if ( dataSet == null && trimToNull( dataValueSet.getDataSet() ) != null )
        {
            summary.getConflicts().add( new ImportConflict( dataValueSet.getDataSet(), "Data set not found or not accessible" ) );
            summary.setStatus( ImportStatus.ERROR );
        }

        if ( dataSet != null && !aclService.canDataWrite( currentUser, dataSet ) )
        {
            summary.getConflicts().add( new ImportConflict( dataValueSet.getDataSet(), "User does not have write access for DataSet: " + dataSet.getUid() ) );
            summary.setStatus( ImportStatus.ERROR );
        }

        if ( dataSet == null && strictDataElements )
        {
            summary.getConflicts().add( new ImportConflict( "DATA_IMPORT_STRICT_DATA_ELEMENTS", "A valid dataset is required" ) );
            summary.setStatus( ImportStatus.ERROR );
        }

        if ( outerOrgUnit == null && trimToNull( dataValueSet.getOrgUnit() ) != null )
        {
            summary.getConflicts().add( new ImportConflict( dataValueSet.getOrgUnit(), "Org unit not found or not accessible" ) );
            summary.setStatus( ImportStatus.ERROR );
        }

        if ( outerAttrOptionCombo == null && trimToNull( dataValueSet.getAttributeOptionCombo() ) != null )
        {
            summary.getConflicts().add( new ImportConflict( dataValueSet.getAttributeOptionCombo(), "Attribute option combo not found or not accessible" ) );
            summary.setStatus( ImportStatus.ERROR );
        }

        if ( ImportStatus.ERROR.equals( summary.getStatus() ) )
        {
            summary.setDescription( "Import process was aborted" );
            notifier.notify( id, WARN, "Import process aborted", true ).addJobSummary( id, summary, ImportSummary.class );
            dataValueSet.close();
            return summary;
        }

        if ( dataSet != null && completeDate != null )
        {
            notifier.notify( id, notificationLevel, "Completing data set" );
            handleComplete( dataSet, completeDate, outerPeriod, outerOrgUnit, fallbackCategoryOptionCombo, currentUserName, summary ); //TODO
        }
        else
        {
            summary.setDataSetComplete( Boolean.FALSE.toString() );
        }

        final Set<OrganisationUnit> currentOrgUnits = currentUserService.getCurrentUserOrganisationUnits();

        BatchHandler<DataValue> dataValueBatchHandler = batchHandlerFactory.createBatchHandler( DataValueBatchHandler.class ).init();
        BatchHandler<DataValueAudit> auditBatchHandler = skipAudit ? null : batchHandlerFactory.createBatchHandler( DataValueAuditBatchHandler.class ).init();

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
            org.hisp.dhis.dxf2.datavalue.DataValue dataValue = dataValueSet.getNextDataValue();

            totalCount++;

            final DataElement dataElement =
                dataElementMap.get( trimToNull( dataValue.getDataElement() ), dataElementCallable.setId( trimToNull( dataValue.getDataElement() ) ) );
            final Period period = outerPeriod != null ? outerPeriod :
                periodMap.get( trimToNull( dataValue.getPeriod() ), periodCallable.setId( trimToNull( dataValue.getPeriod() ) ) );
            final OrganisationUnit orgUnit = outerOrgUnit != null ? outerOrgUnit :
                orgUnitMap.get( trimToNull( dataValue.getOrgUnit() ), orgUnitCallable.setId( trimToNull( dataValue.getOrgUnit() ) ) );
            CategoryOptionCombo categoryOptionCombo =
                optionComboMap.get( trimToNull( dataValue.getCategoryOptionCombo() ), categoryOptionComboCallable.setId( trimToNull( dataValue.getCategoryOptionCombo() ) ) );
            CategoryOptionCombo attrOptionCombo = outerAttrOptionCombo != null ? outerAttrOptionCombo :
                optionComboMap.get( trimToNull( dataValue.getAttributeOptionCombo() ), attributeOptionComboCallable.setId( trimToNull( dataValue.getAttributeOptionCombo() ) ) );

            // -----------------------------------------------------------------
            // Potentially heat caches
            // -----------------------------------------------------------------

            if ( !dataElementMap.isCacheLoaded() && dataElementMap.getCacheMissCount() > CACHE_MISS_THRESHOLD )
            {
                dataElementMap.load( identifiableObjectManager.getAll( DataElement.class ), o -> o.getPropertyValue( dataElementIdScheme ) );

                log.info( "Data element cache heated after cache miss threshold reached" );
            }

            if ( !orgUnitMap.isCacheLoaded() && orgUnitMap.getCacheMissCount() > CACHE_MISS_THRESHOLD )
            {
                orgUnitMap.load( identifiableObjectManager.getAll( OrganisationUnit.class ), o -> o.getPropertyValue( orgUnitIdScheme ) );

                log.info( "Org unit cache heated after cache miss threshold reached" );
            }

            if ( !optionComboMap.isCacheLoaded() && optionComboMap.getCacheMissCount() > CACHE_MISS_THRESHOLD )
            {
                optionComboMap.load( identifiableObjectManager.getAll( CategoryOptionCombo.class ), o -> o.getPropertyValue(
                    categoryOptComboIdScheme ) );

                log.info( "Category Option Combo cache heated after cache miss threshold reached" );
            }

            // -----------------------------------------------------------------
            // Validation
            // -----------------------------------------------------------------

            if ( dataElement == null )
            {
                summary.getConflicts().add( new ImportConflict( dataValue.getDataElement(), "Data element not found or not accessible" ) );
                continue;
            }

            if ( period == null )
            {
                summary.getConflicts().add( new ImportConflict( dataValue.getPeriod(), "Period not valid" ) );
                continue;
            }

            if ( orgUnit == null )
            {
                summary.getConflicts().add( new ImportConflict( dataValue.getOrgUnit(), "Organisation unit not found or not accessible" ) );
                continue;
            }

            if ( categoryOptionCombo == null && trimToNull( dataValue.getCategoryOptionCombo() ) != null )
            {
                summary.getConflicts().add( new ImportConflict( dataValue.getCategoryOptionCombo(), "Category option combo not found or not accessible for writing data" ) );
                continue;
            }

            if ( categoryOptionCombo != null )
            {
                List<String> errors = accessManager.canWrite( currentUser, categoryOptionCombo );

                if ( !errors.isEmpty() )
                {
                    summary.getConflicts().addAll( errors.stream().map( s -> new ImportConflict( "dataValueSet", s ) ).collect( Collectors.toList() ) );
                    continue;
                }
            }

            if ( attrOptionCombo == null && trimToNull( dataValue.getAttributeOptionCombo() ) != null )
            {
                summary.getConflicts().add( new ImportConflict( dataValue.getAttributeOptionCombo(), "Attribute option combo not found or not accessible for writing data" ) );
                continue;
            }

            if ( attrOptionCombo != null )
            {
                List<String> errors = accessManager.canWrite( currentUser, attrOptionCombo );

                if ( !errors.isEmpty() )
                {
                    summary.getConflicts().addAll( errors.stream().map( s -> new ImportConflict( "dataValueSet", s ) ).collect( Collectors.toList() ) );
                    continue;
                }
            }

            boolean inUserHierarchy = orgUnitInHierarchyMap.get( orgUnit.getUid(), () -> orgUnit.isDescendant( currentOrgUnits ) );

            if ( !inUserHierarchy )
            {
                summary.getConflicts().add( new ImportConflict( orgUnit.getUid(), "Organisation unit not in hierarchy of current user: " + currentUserName ) );
                continue;
            }

            if ( dataValue.isNullValue() && !dataValue.isDeletedValue() )
            {
                summary.getConflicts().add( new ImportConflict( "Value", "Data value or comment not specified for data element: " + dataElement.getUid() ) );
                continue;
            }

            dataValue.setValueForced(
                ValidationUtils.normalizeBoolean( dataValue.getValue(), dataElement.getValueType() ) );

            String valueValid = ValidationUtils.dataValueIsValid( dataValue.getValue(), dataElement );

            if ( valueValid != null )
            {
                summary.getConflicts().add( new ImportConflict( dataValue.getValue(), i18n.getString( valueValid ) + ", must match data element type: " + dataElement.getUid() ) );
                continue;
            }

            String commentValid = ValidationUtils.commentIsValid( dataValue.getComment() );

            if ( commentValid != null )
            {
                summary.getConflicts().add( new ImportConflict( "Comment", i18n.getString( commentValid ) ) );
                continue;
            }

            Optional<Set<String>> optionCodes = dataElementOptionsMap.get( dataElement.getUid(), () -> dataElement.hasOptionSet() ?
                Optional.of( dataElement.getOptionSet().getOptionCodesAsSet() ) : Optional.empty() );

            if ( optionCodes.isPresent() && !optionCodes.get().contains( dataValue.getValue() ) )
            {
                summary.getConflicts().add( new ImportConflict( dataValue.getValue(), "Data value is not a valid option of the data element option set: " + dataElement.getUid() ) );
                continue;
            }

            // -----------------------------------------------------------------
            // Constraints
            // -----------------------------------------------------------------

            if ( categoryOptionCombo == null )
            {
                if ( requireCategoryOptionCombo )
                {
                    summary.getConflicts().add( new ImportConflict( dataValue.getValue(), "Category option combo is required but is not specified" ) );
                    continue;
                }
                else
                {
                    categoryOptionCombo = fallbackCategoryOptionCombo;
                }
            }

            if ( attrOptionCombo == null )
            {
                if ( requireAttrOptionCombo )
                {
                    summary.getConflicts().add( new ImportConflict( dataValue.getValue(), "Attribute option combo is required but is not specified" ) );
                    continue;
                }
                else
                {
                    attrOptionCombo = fallbackCategoryOptionCombo;
                }
            }

            if ( strictPeriods && !dataElementPeriodTypesMap.get( dataElement.getUid(),
                dataElement::getPeriodTypes ).contains( period.getPeriodType() ) )
            {
                summary.getConflicts().add( new ImportConflict( dataValue.getPeriod(),
                    "Period type of period: " + period.getIsoDate() + " not valid for data element: " + dataElement.getUid() ) );
                continue;
            }

            if ( strictDataElements && !dataSetDataElements.contains( dataElement ) )
            {
                summary.getConflicts().add( new ImportConflict( "DATA_IMPORT_STRICT_DATA_ELEMENTS",
                    "Data element: " + dataValue.getDataElement() + " is not part of dataset: " + dataSet.getUid() ) );
                continue;
            }

            if ( strictCategoryOptionCombos && !dataElementCategoryOptionComboMap.get( dataElement.getUid(),
                dataElement::getCategoryOptionCombos ).contains( categoryOptionCombo ) )
            {
                summary.getConflicts().add( new ImportConflict( categoryOptionCombo.getUid(),
                    "Category option combo: " + categoryOptionCombo.getUid() + " must be part of category combo of data element: " + dataElement.getUid() ) );
                continue;
            }

            if ( strictAttrOptionCombos && !dataElementAttrOptionComboMap.get( dataElement.getUid(),
                dataElement::getDataSetCategoryOptionCombos ).contains( attrOptionCombo ) )
            {
                summary.getConflicts().add( new ImportConflict( attrOptionCombo.getUid(),
                    "Attribute option combo: " + attrOptionCombo.getUid() + " must be part of category combo of data sets of data element: " + dataElement.getUid() ) );
                continue;
            }

            if ( strictOrgUnits && BooleanUtils.isFalse( dataElementOrgUnitMap.get( dataElement.getUid() + orgUnit.getUid(),
                () -> orgUnit.hasDataElement( dataElement ) ) ) )
            {
                summary.getConflicts().add( new ImportConflict( orgUnit.getUid(),
                    "Data element: " + dataElement.getUid() + " must be assigned through data sets to organisation unit: " + orgUnit.getUid() ) );
                continue;
            }

            boolean zeroAndInsignificant = ValidationUtils.dataValueIsZeroAndInsignificant( dataValue.getValue(), dataElement );

            if ( zeroAndInsignificant )
            {
                continue; // Ignore value
            }

            String storedByValid = ValidationUtils.storedByIsValid( dataValue.getStoredBy() );

            if ( storedByValid != null )
            {
                summary.getConflicts().add( new ImportConflict( dataValue.getStoredBy(), i18n.getString( storedByValid ) ) );
                continue;
            }

            String storedBy = dataValue.getStoredBy() == null || dataValue.getStoredBy().trim().isEmpty() ? currentUserName : dataValue.getStoredBy();

            final CategoryOptionCombo aoc = attrOptionCombo;

            DateRange aocDateRange = dataSet != null
                ? attrOptionComboDateRangeMap.get( attrOptionCombo.getUid() + dataSet.getUid(), () -> aoc.getDateRange( dataSet ) )
                : attrOptionComboDateRangeMap.get( attrOptionCombo.getUid() + dataElement.getUid(), () -> aoc.getDateRange( dataElement ) );

            if ( ( aocDateRange.getStartDate() != null && aocDateRange.getStartDate().after( period.getEndDate() ) )
                || ( aocDateRange.getEndDate() != null && aocDateRange.getEndDate().before( period.getStartDate() ) ) )
            {
                summary.getConflicts().add( new ImportConflict( orgUnit.getUid(),
                    "Period: " + period.getIsoDate() + " is not within date range of attribute option combo: " + attrOptionCombo.getUid() ) );
                continue;
            }

            if ( !attrOptionComboOrgUnitMap.get( attrOptionCombo.getUid() + orgUnit.getUid(), () ->
            {
                Set<OrganisationUnit> aocOrgUnits = aoc.getOrganisationUnits();
                return aocOrgUnits == null || orgUnit.isDescendant( aocOrgUnits );
            } ) )
            {
                summary.getConflicts().add( new ImportConflict( orgUnit.getUid(),
                    "Organisation unit: " + orgUnit.getUid() + " is not valid for attribute option combo: " + attrOptionCombo.getUid() ) );
                continue;
            }

            final DataSet approvalDataSet = dataSet != null ? dataSet : dataElementDataSetMap.get( dataElement.getUid(),
                dataElement::getApprovalDataSet );

            if ( approvalDataSet != null && !forceDataInput ) // Data element is assigned to at least one data set
            {
                if ( dataSetLockedMap.get( approvalDataSet.getUid() + period.getUid() + orgUnit.getUid(),
                    () -> isLocked( currentUser, approvalDataSet, period, orgUnit, skipLockExceptionCheck ) ) )
                {
                    summary.getConflicts().add( new ImportConflict( period.getIsoDate(), "Current date is past expiry days for period " +
                        period.getIsoDate() + " and data set: " + approvalDataSet.getUid() ) );
                    continue;
                }

                Period latestFuturePeriod = dataElementLatestFuturePeriodMap.get( dataElement.getUid(), dataElement::getLatestOpenFuturePeriod );

                if ( period.isAfter( latestFuturePeriod ) && isIso8601 )
                {
                    summary.getConflicts().add( new ImportConflict( period.getIsoDate(), "Period: " +
                        period.getIsoDate() + " is after latest open future period: " + latestFuturePeriod.getIsoDate() + " for data element: " + dataElement.getUid() ) );
                    continue;
                }

                DataApprovalWorkflow workflow = approvalDataSet.getWorkflow();

                if ( workflow != null )
                {
                    final String workflowPeriodAoc = workflow.getUid() + period.getUid() + attrOptionCombo.getUid();

                    if ( approvalMap.get( orgUnit.getUid() + workflowPeriodAoc, () ->
                    {
                        DataApproval lowestApproval = DataApproval.getLowestApproval( new DataApproval( null, workflow, period, orgUnit, aoc ) );

                        return lowestApproval != null && lowestApprovalLevelMap.get(
                            lowestApproval.getDataApprovalLevel().getUid()
                                + lowestApproval.getOrganisationUnit().getUid() + workflowPeriodAoc,
                            () -> approvalService.getDataApproval( lowestApproval ) != null );
                    } ) )
                    {
                        summary.getConflicts().add( new ImportConflict( orgUnit.getUid(),
                            "Data is already approved for data set: " + approvalDataSet.getUid() + " period: " + period.getIsoDate()
                                + " organisation unit: " + orgUnit.getUid() + " attribute option combo: " + attrOptionCombo.getUid() ) );
                        continue;
                    }
                }
            }

            if ( approvalDataSet != null && !forceDataInput && !approvalDataSet.isDataInputPeriodAndDateAllowed( period, new Date() ) )
            {
                summary.getConflicts().add( new ImportConflict( orgUnit.getUid(),
                    "Period: " + period.getIsoDate() + " is not open for this data set at this time: " + approvalDataSet.getUid() ) );
                continue;
            }

            if ( !forceDataInput && !periodOpenForDataElement.get( dataElement.getUid() + period.getIsoDate(), () -> dataElement.isDataInputAllowedForPeriodAndDate( period, new Date() ) ) )
            {
                summary.getConflicts().add( new ImportConflict( orgUnit.getUid(), "Period " + period.getName() + " does not conform to the open periods of associated data sets" ) );
                continue;
            }

            DataValue actualDataValue = null;
            if ( strategy.isDelete() && dataElement.isFileType() )
            {
                actualDataValue = dataValueService.getDataValue( dataElement, period, orgUnit, categoryOptionCombo, attrOptionCombo );
                if ( actualDataValue == null )
                {
                    summary.getConflicts().add( new ImportConflict( dataElement.getUid(), "No data value for file resource exist for the given combination" ) );
                    continue;
                }
            }

            // -----------------------------------------------------------------
            // Create data value
            // -----------------------------------------------------------------

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

            // -----------------------------------------------------------------
            // Save, update or delete data value
            // -----------------------------------------------------------------

            DataValue existingValue = !skipExistingCheck ? dataValueBatchHandler.findObject( internalValue ) : null;

            // -----------------------------------------------------------------
            // Check soft deleted data values on update and import
            // -----------------------------------------------------------------

            if ( !skipExistingCheck && existingValue != null && !existingValue.isDeleted() )
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
                            DataValueAudit auditValue = new DataValueAudit( internalValue, existingValue.getValue(), storedBy, auditType );

                            auditBatchHandler.addObject( auditValue );
                        }

                        if ( dataElement.isFileType() )
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
                        if ( dataElement.isFileType() && actualDataValue != null )
                        {
                            FileResource fr = fileResourceService.getFileResource( actualDataValue.getValue() );

                            fileResourceService.updateFileResource( fr );
                        }

                        dataValueBatchHandler.updateObject( internalValue );

                        if ( !skipAudit )
                        {
                            DataValueAudit auditValue = new DataValueAudit( internalValue, existingValue.getValue(), storedBy, AuditType.DELETE );

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

                                if ( dataElement.isFileType() )
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

                                if ( added && dataElement.isFileType() )
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

        if ( !skipAudit )
        {
            auditBatchHandler.flush();
        }

        int ignores = totalCount - importCount - updateCount - deleteCount;

        summary.setImportCount( new ImportCount( importCount, updateCount, ignores, deleteCount ) );
        summary.setStatus( summary.getConflicts().isEmpty() ? ImportStatus.SUCCESS : ImportStatus.WARNING );
        summary.setDescription( "Import process completed successfully" );

        clock.logTime( "Data value import done, total: " + totalCount + ", import: " + importCount + ", update: " + updateCount + ", delete: " + deleteCount );
        notifier.notify( id, notificationLevel, "Import done", true ).addJobSummary( id, notificationLevel, summary, ImportSummary.class );

        dataValueSet.close();

        return summary;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void handleComplete( DataSet dataSet, Date completeDate, Period period, OrganisationUnit orgUnit,
        CategoryOptionCombo attributeOptionCombo, String currentUserName, ImportSummary summary )
    {
        if ( orgUnit == null )
        {
            summary.getConflicts().add( new ImportConflict( OrganisationUnit.class.getSimpleName(), ERROR_OBJECT_NEEDED_TO_COMPLETE ) );
            return;
        }

        if ( period == null )
        {
            summary.getConflicts().add( new ImportConflict( Period.class.getSimpleName(), ERROR_OBJECT_NEEDED_TO_COMPLETE ) );
            return;
        }

        period = periodService.reloadPeriod( period );

        CompleteDataSetRegistration completeAlready = registrationService
            .getCompleteDataSetRegistration( dataSet, period, orgUnit, attributeOptionCombo );

        if ( completeAlready != null )
        {
            // At this point, DataSet is completed. Override, eventual non-completeness
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
     * @param dataSet                the data set.
     * @param period                 the period.
     * @param organisationUnit       the organisation unit.
     * @param skipLockExceptionCheck whether to skip lock exception check.
     */
    private boolean isLocked( User user, DataSet dataSet, Period period, OrganisationUnit organisationUnit, boolean skipLockExceptionCheck )
    {
        return dataSet.isLocked( user, period, null ) && (skipLockExceptionCheck || lockExceptionStore.getCount( dataSet, period, organisationUnit ) == 0L);
    }
}
