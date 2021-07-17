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
package org.hisp.dhis.webapi.controller;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.audit.payloads.TrackedEntityInstanceAudit;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.PagerUtils;
import org.hisp.dhis.dataapproval.DataApprovalAudit;
import org.hisp.dhis.dataapproval.DataApprovalAuditQueryParams;
import org.hisp.dhis.dataapproval.DataApprovalAuditService;
import org.hisp.dhis.dataapproval.DataApprovalLevel;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.dhis.datavalue.DataValueAuditService;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.dxf2.webmessage.responses.FileResourceWebMessageResponse;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.FileResourceStorageStatus;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.Preset;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceAuditQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceAuditService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAudit;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAuditService;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAudit;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAuditService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Lists;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( "/audits" )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class AuditController
{
    private final IdentifiableObjectManager manager;

    private final ProgramStageInstanceService programStageInstanceService;

    private final DataValueAuditService dataValueAuditService;

    private final TrackedEntityDataValueAuditService trackedEntityDataValueAuditService;

    private final TrackedEntityAttributeValueAuditService trackedEntityAttributeValueAuditService;

    private final DataApprovalAuditService dataApprovalAuditService;

    private final TrackedEntityInstanceAuditService trackedEntityInstanceAuditService;

    private final FieldFilterService fieldFilterService;

    private final ContextService contextService;

    private final FileResourceService fileResourceService;

    public AuditController( IdentifiableObjectManager manager, ProgramStageInstanceService programStageInstanceService,
        DataValueAuditService dataValueAuditService,
        TrackedEntityDataValueAuditService trackedEntityDataValueAuditService,
        TrackedEntityAttributeValueAuditService trackedEntityAttributeValueAuditService,
        DataApprovalAuditService dataApprovalAuditService,
        TrackedEntityInstanceAuditService trackedEntityInstanceAuditService,
        FieldFilterService fieldFilterService,
        ContextService contextService, FileResourceService fileResourceService )
    {
        checkNotNull( manager );
        checkNotNull( programStageInstanceService );
        checkNotNull( dataValueAuditService );
        checkNotNull( trackedEntityDataValueAuditService );
        checkNotNull( trackedEntityAttributeValueAuditService );
        checkNotNull( dataApprovalAuditService );
        checkNotNull( fieldFilterService );
        checkNotNull( contextService );
        checkNotNull( fileResourceService );

        this.manager = manager;
        this.programStageInstanceService = programStageInstanceService;
        this.dataValueAuditService = dataValueAuditService;
        this.trackedEntityDataValueAuditService = trackedEntityDataValueAuditService;
        this.trackedEntityAttributeValueAuditService = trackedEntityAttributeValueAuditService;
        this.dataApprovalAuditService = dataApprovalAuditService;
        this.trackedEntityInstanceAuditService = trackedEntityInstanceAuditService;
        this.fieldFilterService = fieldFilterService;
        this.contextService = contextService;
        this.fileResourceService = fileResourceService;
    }

    /**
     * Returns the file with the given uid
     *
     * @param uid the unique id of the file resource
     */
    @GetMapping( "/files/{uid}" )
    public void getFileAudit( @PathVariable String uid, HttpServletResponse response )
        throws WebMessageException
    {
        FileResource fileResource = fileResourceService.getFileResource( uid );

        if ( fileResource == null || fileResource.getDomain() != FileResourceDomain.DATA_VALUE )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "No file found with uid '" + uid + "'" ) );
        }

        FileResourceStorageStatus storageStatus = fileResource.getStorageStatus();

        if ( storageStatus != FileResourceStorageStatus.STORED )
        {
            // HTTP 409, for lack of a more suitable status code
            WebMessage webMessage = WebMessageUtils.conflict(
                "The content is being processed and is not available yet. Try again later.",
                "The content requested is in transit to the file store and will be available at a later time." );
            webMessage.setResponse( new FileResourceWebMessageResponse( fileResource ) );

            throw new WebMessageException( webMessage );
        }

        response.setContentType( fileResource.getContentType() );
        response.setContentLength( new Long( fileResource.getContentLength() ).intValue() );
        response.setHeader( HttpHeaders.CONTENT_DISPOSITION, "filename=" + fileResource.getName() );

        try
        {
            fileResourceService.copyFileResourceContent( fileResource, response.getOutputStream() );
        }
        catch ( IOException e )
        {
            throw new WebMessageException( WebMessageUtils.error( "Failed fetching the file from storage",
                "There was an exception when trying to fetch the file from the storage backend, could be network or filesystem related" ) );
        }
    }

    @GetMapping( "dataValue" )
    public @ResponseBody RootNode getAggregateDataValueAudit(
        @RequestParam( required = false, defaultValue = "" ) List<String> ds,
        @RequestParam( required = false, defaultValue = "" ) List<String> de,
        @RequestParam( required = false, defaultValue = "" ) List<String> pe,
        @RequestParam( required = false, defaultValue = "" ) List<String> ou,
        @RequestParam( required = false ) String co,
        @RequestParam( required = false ) String cc,
        @RequestParam( required = false ) AuditType auditType,
        @RequestParam( required = false ) Boolean skipPaging,
        @RequestParam( required = false ) Boolean paging,
        @RequestParam( required = false, defaultValue = "50" ) int pageSize,
        @RequestParam( required = false, defaultValue = "1" ) int page )
        throws WebMessageException
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.ALL.getFields() );
        }

        List<DataElement> dataElements = new ArrayList<>();
        dataElements.addAll( getDataElements( de ) );
        dataElements.addAll( getDataElementsByDataSet( ds ) );

        List<Period> periods = getPeriods( pe );
        List<OrganisationUnit> organisationUnits = getOrganisationUnit( ou );
        CategoryOptionCombo categoryOptionCombo = getCategoryOptionCombo( co );
        CategoryOptionCombo attributeOptionCombo = getAttributeOptionCombo( cc );

        List<DataValueAudit> dataValueAudits;
        Pager pager = null;

        if ( PagerUtils.isSkipPaging( skipPaging, paging ) )
        {
            dataValueAudits = dataValueAuditService.getDataValueAudits( dataElements, periods,
                organisationUnits, categoryOptionCombo, attributeOptionCombo, auditType );
        }
        else
        {
            int total = dataValueAuditService.countDataValueAudits( dataElements, periods, organisationUnits,
                categoryOptionCombo,
                attributeOptionCombo, auditType );

            pager = new Pager( page, total, pageSize );

            dataValueAudits = dataValueAuditService.getDataValueAudits( dataElements, periods,
                organisationUnits, categoryOptionCombo, attributeOptionCombo, auditType, pager.getOffset(),
                pager.getPageSize() );
        }

        RootNode rootNode = NodeUtils.createMetadata();

        if ( pager != null )
        {
            rootNode.addChild( NodeUtils.createPager( pager ) );
        }

        CollectionNode trackedEntityAttributeValueAudits = rootNode
            .addChild( new CollectionNode( "dataValueAudits", true ) );
        trackedEntityAttributeValueAudits.addChildren( fieldFilterService.toCollectionNode( DataValueAudit.class,
            new FieldFilterParams( dataValueAudits, fields ) ).getChildren() );

        return rootNode;
    }

    @GetMapping( "trackedEntityDataValue" )
    public @ResponseBody RootNode getTrackedEntityDataValueAudit(
        @RequestParam( required = false, defaultValue = "" ) List<String> de,
        @RequestParam( required = false, defaultValue = "" ) List<String> psi,
        @RequestParam( required = false ) AuditType auditType,
        @RequestParam( required = false ) Boolean skipPaging,
        @RequestParam( required = false ) Boolean paging,
        @RequestParam( required = false, defaultValue = "50" ) int pageSize,
        @RequestParam( required = false, defaultValue = "1" ) int page )
        throws WebMessageException
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.ALL.getFields() );
        }

        List<DataElement> dataElements = getDataElements( de );
        List<ProgramStageInstance> programStageInstances = getProgramStageInstances( psi );

        List<TrackedEntityDataValueAudit> dataValueAudits;
        Pager pager = null;

        if ( PagerUtils.isSkipPaging( skipPaging, paging ) )
        {
            dataValueAudits = trackedEntityDataValueAuditService.getTrackedEntityDataValueAudits(
                dataElements, programStageInstances, auditType );
        }
        else
        {
            int total = trackedEntityDataValueAuditService.countTrackedEntityDataValueAudits( dataElements,
                programStageInstances, auditType );

            pager = new Pager( page, total, pageSize );

            dataValueAudits = trackedEntityDataValueAuditService.getTrackedEntityDataValueAudits(
                dataElements, programStageInstances, auditType, pager.getOffset(), pager.getPageSize() );
        }

        RootNode rootNode = NodeUtils.createMetadata();

        if ( pager != null )
        {
            rootNode.addChild( NodeUtils.createPager( pager ) );
        }

        CollectionNode trackedEntityAttributeValueAudits = rootNode
            .addChild( new CollectionNode( "trackedEntityDataValueAudits", true ) );
        trackedEntityAttributeValueAudits
            .addChildren( fieldFilterService.toCollectionNode( TrackedEntityDataValueAudit.class,
                new FieldFilterParams( dataValueAudits, fields ) ).getChildren() );

        return rootNode;
    }

    @GetMapping( "trackedEntityAttributeValue" )
    public @ResponseBody RootNode getTrackedEntityAttributeValueAudit(
        @RequestParam( required = false, defaultValue = "" ) List<String> tea,
        @RequestParam( required = false, defaultValue = "" ) List<String> tei,
        @RequestParam( required = false ) AuditType auditType,
        @RequestParam( required = false ) Boolean skipPaging,
        @RequestParam( required = false ) Boolean paging,
        @RequestParam( required = false, defaultValue = "50" ) int pageSize,
        @RequestParam( required = false, defaultValue = "1" ) int page )
        throws WebMessageException
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        List<TrackedEntityAttribute> trackedEntityAttributes = getTrackedEntityAttributes( tea );
        List<TrackedEntityInstance> trackedEntityInstances = getTrackedEntityInstances( tei );

        List<TrackedEntityAttributeValueAudit> attributeValueAudits;
        Pager pager = null;

        if ( PagerUtils.isSkipPaging( skipPaging, paging ) )
        {
            attributeValueAudits = trackedEntityAttributeValueAuditService.getTrackedEntityAttributeValueAudits(
                trackedEntityAttributes, trackedEntityInstances, auditType );
        }
        else
        {
            int total = trackedEntityAttributeValueAuditService.countTrackedEntityAttributeValueAudits(
                trackedEntityAttributes,
                trackedEntityInstances, auditType );

            pager = new Pager( page, total, pageSize );

            attributeValueAudits = trackedEntityAttributeValueAuditService.getTrackedEntityAttributeValueAudits(
                trackedEntityAttributes, trackedEntityInstances, auditType, pager.getOffset(), pager.getPageSize() );
        }

        RootNode rootNode = NodeUtils.createMetadata();

        if ( pager != null )
        {
            rootNode.addChild( NodeUtils.createPager( pager ) );
        }

        CollectionNode trackedEntityAttributeValueAudits = rootNode
            .addChild( new CollectionNode( "trackedEntityAttributeValueAudits", true ) );
        trackedEntityAttributeValueAudits
            .addChildren( fieldFilterService.toCollectionNode( TrackedEntityAttributeValueAudit.class,
                new FieldFilterParams( attributeValueAudits, fields ) ).getChildren() );

        return rootNode;
    }

    @GetMapping( "dataApproval" )
    public @ResponseBody RootNode getDataApprovalAudit(
        @RequestParam( required = false, defaultValue = "" ) List<String> dal,
        @RequestParam( required = false, defaultValue = "" ) List<String> wf,
        @RequestParam( required = false, defaultValue = "" ) List<String> ou,
        @RequestParam( required = false, defaultValue = "" ) List<String> aoc,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false ) Boolean skipPaging,
        @RequestParam( required = false ) Boolean paging,
        @RequestParam( required = false, defaultValue = "50" ) int pageSize,
        @RequestParam( required = false, defaultValue = "1" ) int page )
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.ALL.getFields() );
        }

        DataApprovalAuditQueryParams params = new DataApprovalAuditQueryParams();

        params.setLevels( new HashSet<>( getDataApprovalLevel( dal ) ) );
        params.setWorkflows( new HashSet<>( getDataApprovalWorkflow( wf ) ) );
        params.setOrganisationUnits( new HashSet<>( getOrganisationUnit( ou ) ) );
        params.setAttributeOptionCombos( new HashSet<>( getCategoryOptionCombo( aoc ) ) );
        params.setStartDate( startDate );
        params.setEndDate( endDate );

        List<DataApprovalAudit> audits = dataApprovalAuditService.getDataApprovalAudits( params );

        Pager pager;
        RootNode rootNode = NodeUtils.createMetadata();

        if ( !PagerUtils.isSkipPaging( skipPaging, paging ) )
        {
            pager = new Pager( page, audits.size(), pageSize );

            audits = audits.subList( pager.getOffset(),
                Math.min( pager.getOffset() + pager.getPageSize(), audits.size() ) );

            rootNode.addChild( NodeUtils.createPager( pager ) );
        }

        CollectionNode dataApprovalAudits = rootNode.addChild( new CollectionNode( "dataApprovalAudits", true ) );
        dataApprovalAudits.addChildren( fieldFilterService.toCollectionNode( DataApprovalAudit.class,
            new FieldFilterParams( audits, fields ) ).getChildren() );

        return rootNode;
    }

    @GetMapping( "trackedEntityInstance" )
    public @ResponseBody RootNode getTrackedEnityInstanceAudit(
        @RequestParam( required = false, defaultValue = "" ) List<String> tei,
        @RequestParam( required = false, defaultValue = "" ) List<String> user,
        @RequestParam( required = false ) AuditType auditType,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false ) Boolean skipPaging,
        @RequestParam( required = false ) Boolean paging,
        @RequestParam( required = false, defaultValue = "50" ) int pageSize,
        @RequestParam( required = false, defaultValue = "1" ) int page )
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.ALL.getFields() );
        }

        TrackedEntityInstanceAuditQueryParams params = new TrackedEntityInstanceAuditQueryParams();

        params.setTrackedEntityInstances( new HashSet<>( tei ) );
        params.setUsers( new HashSet<>( user ) );
        params.setAuditType( auditType );
        params.setStartDate( startDate );
        params.setEndDate( endDate );
        params.setSkipPaging( PagerUtils.isSkipPaging( skipPaging, paging ) );

        List<TrackedEntityInstanceAudit> teiAudits;
        Pager pager = null;

        if ( !params.isSkipPaging() )
        {
            int total = trackedEntityInstanceAuditService.getTrackedEntityInstanceAuditsCount( params );

            pager = new Pager( page, total, pageSize );

            params.setFirst( pager.getOffset() );
            params.setMax( pager.getPageSize() );
        }

        teiAudits = trackedEntityInstanceAuditService.getTrackedEntityInstanceAudits( params );

        RootNode rootNode = NodeUtils.createMetadata();

        if ( pager != null )
        {
            rootNode.addChild( NodeUtils.createPager( pager ) );
        }

        CollectionNode trackedEntityInstanceAudits = rootNode
            .addChild( new CollectionNode( "trackedEntityInstanceAudits", true ) );
        trackedEntityInstanceAudits.addChildren( fieldFilterService.toCollectionNode( TrackedEntityInstanceAudit.class,
            new FieldFilterParams( teiAudits, fields ) ).getChildren() );

        return rootNode;

    }

    // -----------------------------------------------------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------------------------------------------------

    private List<TrackedEntityInstance> getTrackedEntityInstances( List<String> teiIdentifiers )
        throws WebMessageException
    {
        List<TrackedEntityInstance> trackedEntityInstances = new ArrayList<>();

        for ( String tei : teiIdentifiers )
        {
            TrackedEntityInstance trackedEntityInstance = getTrackedEntityInstance( tei );

            if ( trackedEntityInstance != null )
            {
                trackedEntityInstances.add( trackedEntityInstance );
            }
        }

        return trackedEntityInstances;
    }

    private TrackedEntityInstance getTrackedEntityInstance( String tei )
        throws WebMessageException
    {
        if ( tei == null )
        {
            return null;
        }

        TrackedEntityInstance trackedEntityInstance = manager.get( TrackedEntityInstance.class, tei );

        if ( trackedEntityInstance == null )
        {
            throw new WebMessageException(
                WebMessageUtils.conflict( "Illegal trackedEntityInstance identifier: " + tei ) );
        }

        return trackedEntityInstance;
    }

    private List<TrackedEntityAttribute> getTrackedEntityAttributes( List<String> teaIdentifiers )
        throws WebMessageException
    {
        List<TrackedEntityAttribute> trackedEntityAttributes = new ArrayList<>();

        for ( String tea : teaIdentifiers )
        {
            TrackedEntityAttribute trackedEntityAttribute = getTrackedEntityAttribute( tea );

            if ( trackedEntityAttribute != null )
            {
                trackedEntityAttributes.add( trackedEntityAttribute );
            }
        }

        return trackedEntityAttributes;
    }

    private TrackedEntityAttribute getTrackedEntityAttribute( String tea )
        throws WebMessageException
    {
        if ( tea == null )
        {
            return null;
        }

        TrackedEntityAttribute trackedEntityAttribute = manager.get( TrackedEntityAttribute.class, tea );

        if ( trackedEntityAttribute == null )
        {
            throw new WebMessageException(
                WebMessageUtils.conflict( "Illegal trackedEntityAttribute identifier: " + tea ) );
        }

        return trackedEntityAttribute;
    }

    private List<ProgramStageInstance> getProgramStageInstances( List<String> psIdentifiers )
        throws WebMessageException
    {
        List<ProgramStageInstance> programStageInstances = new ArrayList<>();

        for ( String ps : psIdentifiers )
        {
            ProgramStageInstance programStageInstance = getProgramStageInstance( ps );

            if ( programStageInstance != null )
            {
                programStageInstances.add( programStageInstance );
            }
        }

        return programStageInstances;
    }

    private ProgramStageInstance getProgramStageInstance( String ps )
        throws WebMessageException
    {
        if ( ps == null )
        {
            return null;
        }

        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( ps );

        if ( programStageInstance == null )
        {
            throw new WebMessageException(
                WebMessageUtils.conflict( "Illegal programStageInstance identifier: " + ps ) );
        }

        return programStageInstance;
    }

    private List<DataElement> getDataElementsByDataSet( List<String> dsIdentifiers )
        throws WebMessageException
    {
        List<DataElement> dataElements = new ArrayList<>();

        for ( String ds : dsIdentifiers )
        {
            DataSet dataSet = manager.get( DataSet.class, ds );

            if ( dataSet == null )
            {
                throw new WebMessageException( WebMessageUtils.conflict( "Illegal dataSet identifier: " + ds ) );
            }

            dataElements.addAll( dataSet.getDataElements() );
        }

        return dataElements;
    }

    private List<DataElement> getDataElements( List<String> deIdentifier )
        throws WebMessageException
    {
        List<DataElement> dataElements = new ArrayList<>();

        for ( String de : deIdentifier )
        {
            DataElement dataElement = getDataElement( de );

            if ( dataElement != null )
            {
                dataElements.add( dataElement );
            }
        }

        return dataElements;
    }

    private DataElement getDataElement( String de )
        throws WebMessageException
    {
        if ( de == null )
        {
            return null;
        }

        DataElement dataElement = manager.search( DataElement.class, de );

        if ( dataElement == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Illegal dataElement identifier: " + de ) );
        }

        return dataElement;
    }

    private List<Period> getPeriods( List<String> peIdentifiers )
        throws WebMessageException
    {
        List<Period> periods = new ArrayList<>();

        for ( String pe : peIdentifiers )
        {
            Period period = PeriodType.getPeriodFromIsoString( pe );

            if ( period == null )
            {
                throw new WebMessageException( WebMessageUtils.conflict( "Illegal period identifier: " + pe ) );
            }
            else
            {
                periods.add( period );
            }
        }

        return periods;
    }

    private List<OrganisationUnit> getOrganisationUnit( List<String> ou )
    {
        if ( ou == null )
        {
            return new ArrayList<>();
        }

        return manager.getByUid( OrganisationUnit.class, ou );
    }

    private List<CategoryOptionCombo> getCategoryOptionCombo( List<String> coc )
    {
        if ( coc == null )
        {
            return new ArrayList<>();
        }

        return manager.getByUid( CategoryOptionCombo.class, coc );
    }

    private CategoryOptionCombo getCategoryOptionCombo( @RequestParam String co )
        throws WebMessageException
    {
        if ( co == null )
        {
            return null;
        }

        CategoryOptionCombo categoryOptionCombo = manager.search( CategoryOptionCombo.class, co );

        if ( categoryOptionCombo == null )
        {
            throw new WebMessageException(
                WebMessageUtils.conflict( "Illegal categoryOptionCombo identifier: " + co ) );
        }

        return categoryOptionCombo;
    }

    private CategoryOptionCombo getAttributeOptionCombo( @RequestParam String cc )
        throws WebMessageException
    {
        if ( cc == null )
        {
            return null;
        }

        CategoryOptionCombo attributeOptionCombo = manager.search( CategoryOptionCombo.class, cc );

        if ( attributeOptionCombo == null )
        {
            throw new WebMessageException(
                WebMessageUtils.conflict( "Illegal attributeOptionCombo identifier: " + cc ) );
        }

        return attributeOptionCombo;
    }

    private List<DataApprovalLevel> getDataApprovalLevel( @RequestParam List<String> dal )
    {
        if ( dal == null )
        {
            return new ArrayList<>();
        }

        return manager.getByUid( DataApprovalLevel.class, dal );
    }

    private List<DataApprovalWorkflow> getDataApprovalWorkflow( @RequestParam List<String> wf )
    {
        if ( wf == null )
        {
            return new ArrayList<>();
        }

        return manager.getByUid( DataApprovalWorkflow.class, wf );
    }
}
