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
 *
 */

import org.hisp.dhis.schema.MergeParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.legend.LegendSetService;
import org.hisp.dhis.schema.descriptors.LegendSetSchemaDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = LegendSetSchemaDescriptor.API_ENDPOINT )
public class LegendSetController
    extends AbstractCrudController<LegendSet>
{
    @Autowired
    private LegendSetService legendSetService;

    @Override
    @RequestMapping( method = RequestMethod.POST, consumes = "application/json" )
    @PreAuthorize( "hasRole('F_GIS_ADMIN') or hasRole('F_LEGEND_SET_PUBLIC_ADD') or hasRole('F_LEGEND_SET_PRIVATE_ADD') or hasRole('ALL')" )
    @ResponseStatus( HttpStatus.CREATED )
    public void postJsonObject( HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        LegendSet legendSet = renderService.fromJson( request.getInputStream(), LegendSet.class );
        legendSet.getTranslations().clear();

        legendSetService.addLegendSet( legendSet );

        response.addHeader( "Location", LegendSetSchemaDescriptor.API_ENDPOINT + "/" + legendSet.getUid() );
        webMessageService.send( WebMessageUtils.created( "Legend set created" ), response, request );
    }

    @Override
    @RequestMapping( value = "/{uid}", method = RequestMethod.PUT, consumes = "application/json" )
    @PreAuthorize( "hasRole('F_GIS_ADMIN') or hasRole('F_LEGEND_SET_PUBLIC_ADD') or hasRole('F_LEGEND_SET_PRIVATE_ADD')  or hasRole('ALL')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void putJsonObject( @PathVariable String uid, HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        LegendSet legendSet = legendSetService.getLegendSet( uid );

        if ( legendSet == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Legend set does not exist: " + uid ) );
        }

        MetadataImportParams params = importService.getParamsFromMap( contextService.getParameterValuesMap() );

        LegendSet newLegendSet = renderService.fromJson( request.getInputStream(), LegendSet.class );
        newLegendSet.setUser( currentUserService.getCurrentUser() );
        newLegendSet.setUid( legendSet.getUid() );

        mergeService.merge( new MergeParams<>( newLegendSet, legendSet ).setMergeMode( params.getMergeMode() ) );
        legendSetService.updateLegendSet( legendSet );
    }

    @Override
    @RequestMapping( value = "/{uid}", method = RequestMethod.DELETE )
    @PreAuthorize( "hasRole('F_GIS_ADMIN') or hasRole('F_LEGEND_SET_DELETE') or hasRole('ALL')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteObject( @PathVariable String uid, HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        LegendSet legendSet = legendSetService.getLegendSet( uid );

        if ( legendSet == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Legend set does not exist: " + uid ) );
        }

        legendSet.getLegends().clear();
        legendSetService.deleteLegendSet( legendSet );
    }
}
