package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.PagerUtils;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dataset.LockException;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.hibernate.exception.ReadAccessDeniedException;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.Preset;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
@Controller
@RequestMapping( LockExceptionController.RESOURCE_PATH )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class LockExceptionController
{
    public static final String RESOURCE_PATH = "/lockExceptions";

    @Autowired
    private ContextService contextService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private WebMessageService webMessageService;

    @Autowired
    private AclService aclService;

    @Autowired
    private CurrentUserService userService;

    @Autowired
    private FieldFilterService fieldFilterService;

    // -------------------------------------------------------------------------
    // Resources
    // -------------------------------------------------------------------------

    @RequestMapping( method = RequestMethod.GET, produces = ContextUtils.CONTENT_TYPE_JSON )
    public @ResponseBody RootNode getLockExceptions( @RequestParam( required = false ) String key,
        @RequestParam Map<String, String> rpParameters, HttpServletRequest request, HttpServletResponse response )
        throws IOException, WebMessageException
    {
        List<String> filters = Lists.newArrayList( contextService.getParameterValues( "filter" ) );
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.ALL.getFields() );
        }

        List<LockException> lockExceptions = new ArrayList<>();

        if ( key != null )
        {
            LockException lockException = dataSetService.getLockException( MathUtils.parseInt( key ) );

            if ( lockException == null )
            {
                throw new WebMessageException( WebMessageUtils.notFound( "Cannot find LockException with key: " + key ) );
            }

            lockExceptions.add( lockException );
        }
        else if ( !filters.isEmpty() )
        {
            lockExceptions = dataSetService.filterLockExceptions( filters );
        }
        else
        {
            lockExceptions = dataSetService.getAllLockExceptions();
        }

        WebOptions options = new WebOptions( rpParameters );
        WebMetadata metadata = new WebMetadata();

        Pager pager = metadata.getPager();

        if ( options.hasPaging() && pager == null )
        {
            pager = new Pager( options.getPage(), lockExceptions.size(), options.getPageSize() );
            lockExceptions = PagerUtils.pageCollection( lockExceptions, pager );
        }

        RootNode rootNode = NodeUtils.createMetadata();

        if ( pager != null )
        {
            rootNode.addChild( NodeUtils.createPager( pager ) );
        }

        rootNode.addChild( fieldFilterService.filter( LockException.class, lockExceptions, fields ) );

        return rootNode;
    }

    @RequestMapping( method = RequestMethod.POST )
    public void addLockException( @RequestParam( "ou" ) String organisationUnitId, @RequestParam( "pe" ) String periodId,
        @RequestParam( "ds" ) String dataSetId, HttpServletRequest request, HttpServletResponse response ) throws WebMessageException
    {
        User user = userService.getCurrentUser();

        DataSet dataSet = dataSetService.getDataSet( dataSetId );

        Period period = periodService.reloadPeriod( PeriodType.getPeriodFromIsoString( periodId ) );

        if ( dataSet == null || period == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( " DataSet or Period is invalid" ) );
        }

        if ( !aclService.canUpdate( user, dataSet ) )
        {
            throw new ReadAccessDeniedException( "You don't have the proper permissions to update this object" );
        }

        boolean created = false;

        List<String> listOrgUnitIds = new ArrayList<>();

        if ( organisationUnitId.startsWith( "[" ) && organisationUnitId.endsWith( "]" ) )
        {
            String[] arrOrgUnitIds = organisationUnitId.substring( 1, organisationUnitId.length() - 1 ).split( "," );
            Collections.addAll( listOrgUnitIds, arrOrgUnitIds );
        }
        else
        {
            listOrgUnitIds.add( organisationUnitId );
        }

        if ( listOrgUnitIds.size() == 0 )
        {
            throw new WebMessageException( WebMessageUtils.conflict( " OrganisationUnit ID is invalid." ) );
        }

        for ( String id : listOrgUnitIds )
        {
            OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( id );

            if ( organisationUnit == null )
            {
                throw new WebMessageException( WebMessageUtils.conflict( "Can't find OrganisationUnit with id =" + id ) );
            }

            if ( organisationUnit.getDataSets().contains( dataSet ) )
            {
                LockException lockException = new LockException();

                lockException.setOrganisationUnit( organisationUnit );
                lockException.setDataSet( dataSet );
                lockException.setPeriod( period );
                dataSetService.addLockException( lockException );
                created = true;
            }
        }

        if ( created )
        {
            webMessageService.send( WebMessageUtils.created( "LockException created successfully." ), response, request );
        }
    }

    @RequestMapping( method = RequestMethod.DELETE )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteLockException( @RequestParam( "ou" ) String organisationUnitId, @RequestParam( "pe" ) String periodId,
        @RequestParam( "ds" ) String dataSetId, HttpServletRequest request, HttpServletResponse response ) throws WebMessageException
    {
        User user = userService.getCurrentUser();

        DataSet dataSet = dataSetService.getDataSet( dataSetId );

        Period period = periodService.reloadPeriod( PeriodType.getPeriodFromIsoString( periodId ) );
        OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( organisationUnitId );

        if ( !ObjectUtils.allNonNull( dataSet, period ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Can't find LockException with combination: dataSet=" + dataSetId + ", period=" + periodId ) );
        }

        if ( !aclService.canDelete( user, dataSet ) )
        {
            throw new ReadAccessDeniedException( "You don't have the proper permissions to delete this object." );
        }

        if ( organisationUnit != null )
        {
            dataSetService.deleteLockExceptionCombination( dataSet, period, organisationUnit );
        }
        else
        {
            dataSetService.deleteLockExceptionCombination( dataSet, period );
        }
    }
}
