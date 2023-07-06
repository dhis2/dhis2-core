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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.created;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.forbidden;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.PagerUtils;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dataset.LockException;
import org.hisp.dhis.dataset.comparator.LockExceptionNameComparator;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.hibernate.exception.ReadAccessDeniedException;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
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
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.google.common.collect.Lists;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
@Controller
@RequestMapping( LockExceptionController.RESOURCE_PATH )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class LockExceptionController extends AbstractGistReadOnlyController<LockException>
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
    private AclService aclService;

    @Autowired
    private CurrentUserService userService;

    @Autowired
    private FieldFilterService fieldFilterService;

    @Autowired
    private I18nManager i18nManager;

    @Autowired
    private CurrentUserService currentUserService;

    // -------------------------------------------------------------------------
    // Resources
    // -------------------------------------------------------------------------

    @GetMapping( produces = ContextUtils.CONTENT_TYPE_JSON )
    public @ResponseBody RootNode getLockExceptions( @RequestParam( required = false ) String key,
        @RequestParam Map<String, String> rpParameters, HttpServletRequest request, HttpServletResponse response )
        throws WebMessageException
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
                throw new WebMessageException(
                    notFound( "Cannot find LockException with key: " + key ) );
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

        I18nFormat format = this.i18nManager.getI18nFormat();

        for ( LockException lockException : lockExceptions )
        {
            lockException.getPeriod().setName( format.formatPeriod( lockException.getPeriod() ) );
        }

        rootNode.addChild( fieldFilterService.toCollectionNode( LockException.class,
            new FieldFilterParams( lockExceptions, fields ) ) );

        return rootNode;
    }

    @GetMapping( value = "/combinations", produces = ContextUtils.CONTENT_TYPE_JSON )
    public @ResponseBody RootNode getLockExceptionCombinations()
    {

        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.ALL.getFields() );
        }

        List<LockException> lockExceptions = this.dataSetService.getLockExceptionCombinations();
        I18nFormat format = this.i18nManager.getI18nFormat();

        for ( LockException lockException : lockExceptions )
        {
            lockException.getPeriod().setName( format.formatPeriod( lockException.getPeriod() ) );
        }

        Collections.sort( lockExceptions, new LockExceptionNameComparator() );

        RootNode rootNode = NodeUtils.createMetadata();
        rootNode.addChild( fieldFilterService.toCollectionNode( LockException.class,
            new FieldFilterParams( lockExceptions, fields ) ) );

        return rootNode;
    }

    @PostMapping
    @ResponseBody
    public WebMessage addLockException( @RequestParam( "ou" ) String organisationUnitId,
        @RequestParam( "pe" ) String periodId,
        @RequestParam( "ds" ) String dataSetId )
    {
        User user = userService.getCurrentUser();

        DataSet dataSet = dataSetService.getDataSet( dataSetId );

        Period period = periodService.reloadPeriod( PeriodType.getPeriodFromIsoString( periodId ) );

        if ( dataSet == null || period == null )
        {
            return conflict( " DataSet or Period is invalid" );
        }

        if ( !aclService.canUpdate( user, dataSet ) )
        {
            return forbidden( "You don't have the proper permissions to update this object" );
        }

        List<String> listOrgUnitIds = new ArrayList<>();

        if ( organisationUnitId.startsWith( "[" ) && organisationUnitId.endsWith( "]" ) )
        {
            String[] arrOrgUnitIds = organisationUnitId.substring( 1, organisationUnitId.length() - 1 ).split( "," );
            Collections.addAll( listOrgUnitIds, arrOrgUnitIds );
        }
        else if ( !organisationUnitId.isEmpty() )
        {
            listOrgUnitIds.add( organisationUnitId );
        }

        if ( listOrgUnitIds.isEmpty() )
        {
            return conflict( "OrganisationUnit ID is invalid." );
        }

        boolean added = false;
        for ( String id : listOrgUnitIds )
        {
            OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( id );

            if ( organisationUnit == null )
            {
                return conflict( "Can't find OrganisationUnit with id =" + id );
            }
            if ( !canCapture( organisationUnit ) )
            {
                return forbidden(
                    "You can only add a lock exceptions to your data capture organisation units." );
            }

            if ( organisationUnit.getDataSets().contains( dataSet ) )
            {
                LockException lockException = new LockException();

                lockException.setOrganisationUnit( organisationUnit );
                lockException.setDataSet( dataSet );
                lockException.setPeriod( period );
                dataSetService.addLockException( lockException );
                added = true;
            }
        }
        if ( !added )
        {
            return conflict( String.format(
                "None of the target organisation unit(s) %s is linked to the specified data set: %s",
                String.join( ",", listOrgUnitIds ), dataSetId ) );
        }
        return created( "LockException created successfully." );
    }

    @DeleteMapping
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteLockException( @RequestParam( name = "ou", required = false ) String organisationUnitId,
        @RequestParam( "pe" ) String periodId,
        @RequestParam( "ds" ) String dataSetId, HttpServletRequest request, HttpServletResponse response )
        throws WebMessageException
    {
        User user = userService.getCurrentUser();

        DataSet dataSet = dataSetService.getDataSet( dataSetId );

        Period period = periodService.reloadPeriod( PeriodType.getPeriodFromIsoString( periodId ) );
        OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( organisationUnitId );

        if ( !ObjectUtils.allNonNull( dataSet, period ) )
        {
            throw new WebMessageException( conflict(
                "Can't find LockException with combination: dataSet=" + dataSetId + ", period=" + periodId ) );
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

    private boolean canCapture( OrganisationUnit captureTarget )
    {
        return currentUserService.currentUserIsSuper()
            || currentUserService.getCurrentUserOrganisationUnits().stream().anyMatch(
                ou -> captureTarget.getPath().startsWith( ou.getPath() ) );
    }
}
