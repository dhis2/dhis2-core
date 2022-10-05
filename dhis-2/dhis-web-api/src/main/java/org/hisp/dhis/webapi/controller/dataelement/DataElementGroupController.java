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
package org.hisp.dhis.webapi.controller.dataelement;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.PagerUtils;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.common.TranslateParams;
import org.hisp.dhis.dxf2.metadata.MetadataExportParams;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.schema.descriptors.DataElementGroupSchemaDescriptor;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.collect.Lists;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = DataElementGroupSchemaDescriptor.API_ENDPOINT )
public class DataElementGroupController
    extends AbstractCrudController<DataElementGroup>
{
    @Autowired
    private CategoryService dataElementCategoryService;

    @Autowired
    private DataElementService dataElementService;

    @GetMapping( "/{uid}/operands" )
    public String getOperands( @PathVariable( "uid" ) String uid, @RequestParam Map<String, String> parameters,
        Model model,
        TranslateParams translateParams, HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        WebOptions options = new WebOptions( parameters );
        setTranslationParams( translateParams );
        List<DataElementGroup> dataElementGroups = getEntity( uid, NO_WEB_OPTIONS );

        if ( dataElementGroups.isEmpty() )
        {
            throw new WebMessageException( notFound( "DataElementGroup not found for uid: " + uid ) );
        }

        WebMetadata metadata = new WebMetadata();
        List<DataElementOperand> dataElementOperands = Lists
            .newArrayList( dataElementCategoryService.getOperands( dataElementGroups.get( 0 ).getMembers() ) );
        Collections.sort( dataElementOperands );

        metadata.setDataElementOperands( dataElementOperands );

        if ( options.hasPaging() )
        {
            Pager pager = new Pager( options.getPage(), dataElementOperands.size(), options.getPageSize() );
            metadata.setPager( pager );
            dataElementOperands = PagerUtils.pageCollection( dataElementOperands, pager );
        }

        metadata.setDataElementOperands( dataElementOperands );
        linkService.generateLinks( metadata, false );

        model.addAttribute( "model", metadata );
        model.addAttribute( "viewClass", options.getViewClass( "basic" ) );

        return StringUtils.uncapitalize( getEntitySimpleName() );
    }

    @GetMapping( "/{uid}/operands/query/{q}" )
    public String getOperandsByQuery( @PathVariable( "uid" ) String uid,
        @PathVariable( "q" ) String q, @RequestParam Map<String, String> parameters, TranslateParams translateParams,
        Model model,
        HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        WebOptions options = new WebOptions( parameters );
        setTranslationParams( translateParams );
        List<DataElementGroup> dataElementGroups = getEntity( uid, NO_WEB_OPTIONS );

        if ( dataElementGroups.isEmpty() )
        {
            throw new WebMessageException( notFound( "DataElementGroup not found for uid: " + uid ) );
        }

        WebMetadata metadata = new WebMetadata();
        List<DataElementOperand> dataElementOperands = Lists.newArrayList();

        for ( DataElementOperand dataElementOperand : dataElementCategoryService
            .getOperands( dataElementGroups.get( 0 ).getMembers() ) )
        {
            if ( dataElementOperand.getDisplayName().toLowerCase().contains( q.toLowerCase() ) )
            {
                dataElementOperands.add( dataElementOperand );
            }
        }

        metadata.setDataElementOperands( dataElementOperands );

        if ( options.hasPaging() )
        {
            Pager pager = new Pager( options.getPage(), dataElementOperands.size(), options.getPageSize() );
            metadata.setPager( pager );
            dataElementOperands = PagerUtils.pageCollection( dataElementOperands, pager );
        }

        metadata.setDataElementOperands( dataElementOperands );
        linkService.generateLinks( metadata, false );

        model.addAttribute( "model", metadata );
        model.addAttribute( "viewClass", options.getViewClass( "basic" ) );

        return StringUtils.uncapitalize( getEntitySimpleName() );
    }

    @GetMapping( "/{uid}/metadata" )
    public ResponseEntity<MetadataExportParams> getDataElementGroupWithDependencies(
        @PathVariable( "uid" ) String dataElementGroupId,
        @RequestParam( required = false, defaultValue = "false" ) boolean download )
        throws WebMessageException,
        IOException
    {
        DataElementGroup dataElementGroup = dataElementService.getDataElementGroup( dataElementGroupId );

        if ( dataElementGroup == null )
        {
            throw new WebMessageException(
                notFound( "DataElementGroup not found for uid: " + dataElementGroupId ) );
        }

        MetadataExportParams exportParams = exportService.getParamsFromMap( contextService.getParameterValuesMap() );
        exportService.validate( exportParams );
        exportParams.setObjectExportWithDependencies( dataElementGroup );

        return ResponseEntity.ok( exportParams );
    }
}
