package org.hisp.dhis.webapi.controller.mapping;

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
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.dxf2.common.TranslateParams;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.mapgeneration.MapGenerationService;
import org.hisp.dhis.mapping.MapView;
import org.hisp.dhis.mapping.MappingService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.schema.descriptors.MapViewSchemaDescriptor;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = MapViewSchemaDescriptor.API_ENDPOINT )
public class MapViewController
    extends AbstractCrudController<MapView>
{
    @Autowired
    private MappingService mappingService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private MapGenerationService mapGenerationService;

    @Autowired
    private ContextUtils contextUtils;

    //--------------------------------------------------------------------------
    // Get data
    //--------------------------------------------------------------------------

    @RequestMapping( value = { "/{uid}/data", "/{uid}/data.png" }, method = RequestMethod.GET )
    public void getMapViewData( @PathVariable String uid, HttpServletResponse response ) throws Exception
    {
        MapView mapView = mappingService.getMapView( uid );

        if ( mapView == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Map view does not exist: " + uid ) );
        }

        renderMapViewPng( mapView, response );
    }

    @RequestMapping( value = { "/data", "/data.png" }, method = RequestMethod.GET )
    public void getMapView( Model model,
        @RequestParam( value = "in" ) String indicatorUid,
        @RequestParam( value = "ou" ) String organisationUnitUid,
        @RequestParam( value = "level", required = false ) Integer level,
        HttpServletResponse response ) throws Exception
    {
        if ( level == null )
        {
            OrganisationUnit unit = organisationUnitService.getOrganisationUnit( organisationUnitUid );
            level = unit.getLevel();
            level++;
        }

        MapView mapView = mappingService.getIndicatorLastYearMapView( indicatorUid, organisationUnitUid, level );

        renderMapViewPng( mapView, response );
    }

    //--------------------------------------------------------------------------
    // Hooks
    //--------------------------------------------------------------------------

    @Override
    protected List<MapView> getEntityList( WebMetadata metadata, WebOptions options, List<String> filters,
        List<Order> orders, TranslateParams translateParams ) throws QueryParserException
    {
        List<MapView> entityList;

        if ( options.getOptions().containsKey( "query" ) )
        {
            entityList = Lists.newArrayList( manager.filter( getEntityClass(), options.getOptions().get( "query" ) ) );
        }
        else
        {
            entityList = new ArrayList<>( manager.getAll( getEntityClass() ) );
        }

        return entityList;
    }

    //--------------------------------------------------------------------------
    // Supportive methods
    //--------------------------------------------------------------------------

    private void renderMapViewPng( MapView mapView, HttpServletResponse response )
        throws Exception
    {
        BufferedImage image = mapGenerationService.generateMapImage( mapView );

        if ( image != null )
        {
            contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_PNG, CacheStrategy.RESPECT_SYSTEM_SETTING, "mapview.png", false );

            ImageIO.write( image, "PNG", response.getOutputStream() );
        }
        else
        {
            response.setStatus( HttpServletResponse.SC_NO_CONTENT );
        }
    }
}
