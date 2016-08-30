package org.hisp.dhis.webapi.controller;

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

import com.google.common.collect.Lists;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.dhis.datavalue.DataValueAuditService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.fieldfilter.FieldFilterService;
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
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAudit;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAuditService;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAudit;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAuditService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = "/audits", method = RequestMethod.GET )
@ApiVersion( { ApiVersion.Version.DEFAULT, ApiVersion.Version.ALL } )
public class AuditController
{
    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private DataValueAuditService dataValueAuditService;

    @Autowired
    private TrackedEntityDataValueAuditService trackedEntityDataValueAuditService;

    @Autowired
    private TrackedEntityAttributeValueAuditService trackedEntityAttributeValueAuditService;

    @Autowired
    private FieldFilterService fieldFilterService;

    @Autowired
    private ContextService contextService;

    @RequestMapping( value = "dataValue", method = RequestMethod.GET )
    public @ResponseBody RootNode getAggregateDataValueAudit(
        @RequestParam( required = false, defaultValue = "" ) List<String> ds,
        @RequestParam( required = false, defaultValue = "" ) List<String> de,
        @RequestParam( required = false, defaultValue = "" ) List<String> pe,
        @RequestParam( required = false, defaultValue = "" ) List<String> ou,
        @RequestParam( required = false ) String co,
        @RequestParam( required = false ) String cc,
        @RequestParam( required = false ) AuditType auditType,
        @RequestParam( required = false ) boolean skipPaging,
        @RequestParam( required = false, defaultValue = "50" ) int pageSize,
        @RequestParam( required = false, defaultValue = "1" ) int page
    ) throws WebMessageException
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
        DataElementCategoryOptionCombo categoryOptionCombo = getCategoryOptionCombo( co );
        DataElementCategoryOptionCombo attributeOptionCombo = getAttributeOptionCombo( cc );

        List<DataValueAudit> dataValueAudits;
        Pager pager = null;

        if ( skipPaging )
        {
            dataValueAudits = dataValueAuditService.getDataValueAudits( dataElements, periods,
                organisationUnits, categoryOptionCombo, attributeOptionCombo, auditType );
        }
        else
        {
            int total = dataValueAuditService.countDataValueAudits( dataElements, periods, organisationUnits, categoryOptionCombo,
                attributeOptionCombo, auditType );

            pager = new Pager( page, total, pageSize );

            dataValueAudits = dataValueAuditService.getDataValueAudits( dataElements, periods,
                organisationUnits, categoryOptionCombo, attributeOptionCombo, auditType, pager.getOffset(), pager.getPageSize() );
        }

        RootNode rootNode = NodeUtils.createMetadata();

        if ( pager != null )
        {
            rootNode.addChild( NodeUtils.createPager( pager ) );
        }

        CollectionNode trackedEntityAttributeValueAudits = rootNode.addChild( new CollectionNode( "dataValueAudits", true ) );
        trackedEntityAttributeValueAudits.addChildren( fieldFilterService.filter( DataValueAudit.class,
            dataValueAudits, fields ).getChildren() );

        return rootNode;
    }

    @RequestMapping( value = "trackedEntityDataValue", method = RequestMethod.GET )
    public @ResponseBody RootNode getTrackedEntityDataValueAudit(
        @RequestParam( required = false, defaultValue = "" ) List<String> de,
        @RequestParam( required = false, defaultValue = "" ) List<String> psi,
        @RequestParam( required = false ) AuditType auditType,
        @RequestParam( required = false ) boolean skipPaging,
        @RequestParam( required = false, defaultValue = "50" ) int pageSize,
        @RequestParam( required = false, defaultValue = "1" ) int page
    ) throws WebMessageException
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

        if ( skipPaging )
        {
            dataValueAudits = trackedEntityDataValueAuditService.getTrackedEntityDataValueAudits(
                dataElements, programStageInstances, auditType );
        }
        else
        {
            int total = trackedEntityDataValueAuditService.countTrackedEntityDataValueAudits( dataElements, programStageInstances, auditType );

            pager = new Pager( page, total, pageSize );

            dataValueAudits = trackedEntityDataValueAuditService.getTrackedEntityDataValueAudits(
                dataElements, programStageInstances, auditType, pager.getOffset(), pager.getPageSize() );
        }

        RootNode rootNode = NodeUtils.createMetadata();

        if ( pager != null )
        {
            rootNode.addChild( NodeUtils.createPager( pager ) );
        }

        CollectionNode trackedEntityAttributeValueAudits = rootNode.addChild( new CollectionNode( "trackedEntityDataValueAudits", true ) );
        trackedEntityAttributeValueAudits.addChildren( fieldFilterService.filter( TrackedEntityDataValueAudit.class,
            dataValueAudits, fields ).getChildren() );

        return rootNode;
    }

    @RequestMapping( value = "trackedEntityAttributeValue", method = RequestMethod.GET )
    public @ResponseBody RootNode getTrackedEntityAttributeValueAudit(
        @RequestParam( required = false, defaultValue = "" ) List<String> tea,
        @RequestParam( required = false, defaultValue = "" ) List<String> tei,
        @RequestParam( required = false ) AuditType auditType,
        @RequestParam( required = false ) boolean skipPaging,
        @RequestParam( required = false, defaultValue = "50" ) int pageSize,
        @RequestParam( required = false, defaultValue = "1" ) int page
    ) throws WebMessageException
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        List<TrackedEntityAttribute> trackedEntityAttributes = getTrackedEntityAttributes( tea );
        List<TrackedEntityInstance> trackedEntityInstances = getTrackedEntityInstances( tei );

        List<TrackedEntityAttributeValueAudit> attributeValueAudits;
        Pager pager = null;

        if ( skipPaging )
        {
            attributeValueAudits = trackedEntityAttributeValueAuditService.getTrackedEntityAttributeValueAudits(
                trackedEntityAttributes, trackedEntityInstances, auditType );
        }
        else
        {
            int total = trackedEntityAttributeValueAuditService.countTrackedEntityAttributeValueAudits( trackedEntityAttributes,
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

        CollectionNode trackedEntityAttributeValueAudits = rootNode.addChild( new CollectionNode( "trackedEntityAttributeValueAudits", true ) );
        trackedEntityAttributeValueAudits.addChildren( fieldFilterService.filter( TrackedEntityAttributeValueAudit.class,
            attributeValueAudits, fields ).getChildren() );

        return rootNode;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Helpers
    //-----------------------------------------------------------------------------------------------------------------

    private List<TrackedEntityInstance> getTrackedEntityInstances( List<String> teiIdentifiers ) throws WebMessageException
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

    private TrackedEntityInstance getTrackedEntityInstance( String tei ) throws WebMessageException
    {
        if ( tei == null )
        {
            return null;
        }

        TrackedEntityInstance trackedEntityInstance = manager.get( TrackedEntityInstance.class, tei );

        if ( trackedEntityInstance == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Illegal trackedEntityInstance identifier: " + tei ) );
        }

        return trackedEntityInstance;
    }

    private List<TrackedEntityAttribute> getTrackedEntityAttributes( List<String> teaIdentifiers ) throws WebMessageException
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

    private TrackedEntityAttribute getTrackedEntityAttribute( String tea ) throws WebMessageException
    {
        if ( tea == null )
        {
            return null;
        }

        TrackedEntityAttribute trackedEntityAttribute = manager.get( TrackedEntityAttribute.class, tea );

        if ( trackedEntityAttribute == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Illegal trackedEntityAttribute identifier: " + tea ) );
        }

        return trackedEntityAttribute;
    }

    private List<ProgramStageInstance> getProgramStageInstances( List<String> psIdentifiers ) throws WebMessageException
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

    private ProgramStageInstance getProgramStageInstance( String ps ) throws WebMessageException
    {
        if ( ps == null )
        {
            return null;
        }

        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( ps );

        if ( programStageInstance == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Illegal programStageInstance identifier: " + ps ) );
        }

        return programStageInstance;
    }

    private List<DataElement> getDataElementsByDataSet( List<String> dsIdentifiers ) throws WebMessageException
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

    private List<DataElement> getDataElements( List<String> deIdentifier ) throws WebMessageException
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

    private DataElement getDataElement( String de ) throws WebMessageException
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

    private List<Period> getPeriods( List<String> peIdentifiers ) throws WebMessageException
    {
        List<Period> periods = new ArrayList<>();

        for ( String pe : peIdentifiers )
        {
            Period period = PeriodType.getPeriodFromIsoString( pe );

            if ( period == null )
            {
                throw new WebMessageException( WebMessageUtils.conflict( "Illegal period identifier: " + pe ) );
            }
        }

        return periods;
    }

    private List<OrganisationUnit> getOrganisationUnit( List<String> ou ) throws WebMessageException
    {
        if ( ou == null )
        {
            return new ArrayList<>();
        }

        return manager.getByUid( OrganisationUnit.class, ou );
    }

    private DataElementCategoryOptionCombo getCategoryOptionCombo( @RequestParam String co ) throws WebMessageException
    {
        if ( co == null )
        {
            return null;
        }

        DataElementCategoryOptionCombo categoryOptionCombo = manager.search( DataElementCategoryOptionCombo.class, co );

        if ( categoryOptionCombo == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Illegal categoryOptionCombo identifier: " + co ) );
        }

        return categoryOptionCombo;
    }

    private DataElementCategoryOptionCombo getAttributeOptionCombo( @RequestParam String cc ) throws WebMessageException
    {
        if ( cc == null )
        {
            return null;
        }

        DataElementCategoryOptionCombo attributeOptionCombo = manager.search( DataElementCategoryOptionCombo.class, cc );

        if ( attributeOptionCombo == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Illegal attributeOptionCombo identifier: " + cc ) );
        }

        return attributeOptionCombo;
    }
}
