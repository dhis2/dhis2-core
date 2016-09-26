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

import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.legend.LegendService;
import org.hisp.dhis.mapgeneration.MapGenerationService;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.mapping.MapView;
import org.hisp.dhis.mapping.MappingService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.schema.descriptors.MapSchemaDescriptor;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Date;
import java.util.Set;

import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensions;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = MapSchemaDescriptor.API_ENDPOINT )
public class MapController
    extends AbstractCrudController<Map>
{
    private static final int MAP_MIN_WIDTH = 140;
    private static final int MAP_MIN_HEIGHT = 25;

    @Autowired
    private MappingService mappingService;

    @Autowired
    private LegendService legendService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private OrganisationUnitGroupService organisationUnitGroupService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private I18nManager i18nManager;

    @Autowired
    private MapGenerationService mapGenerationService;

    @Autowired
    private DimensionService dimensionService;

    @Autowired
    private UserService userService;

    @Autowired
    private ContextUtils contextUtils;

    //--------------------------------------------------------------------------
    // CRUD
    //--------------------------------------------------------------------------

    @Override
    @RequestMapping( method = RequestMethod.POST, consumes = "application/json" )
    @ResponseStatus( HttpStatus.CREATED )
    public void postJsonObject( HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        Map map = deserializeJsonEntity( request, response );
        map.getTranslations().clear();

        mappingService.addMap( map );

        response.addHeader( "Location", MapSchemaDescriptor.API_ENDPOINT + "/" + map.getUid() );
        webMessageService.send( WebMessageUtils.created( "Map created" ), response, request );
    }

    @Override
    @RequestMapping( value = "/{uid}", method = RequestMethod.PUT, consumes = "application/json" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void putJsonObject( @PathVariable String uid, HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        Map map = mappingService.getMap( uid );

        if ( map == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Map does not exist: " + uid ) );
        }

        MetadataImportParams params = importService.getParamsFromMap( contextService.getParameterValuesMap() );

        Map newMap = deserializeJsonEntity( request, response );

        map.mergeWith( newMap, params.getMergeMode() );

        mappingService.updateMap( map );
    }

    @Override
    protected void preUpdateEntity( Map map, Map newMap )
    {
        map.getMapViews().clear();

        if ( newMap.getUser() == null )
        {
            map.setUser( null );
        }
    }

    @Override
    protected Map deserializeJsonEntity( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        Map map = super.deserializeJsonEntity( request, response );
        mergeMap( map );

        return map;
    }

    //--------------------------------------------------------------------------
    // Get data
    //--------------------------------------------------------------------------

    @RequestMapping( value = { "/{uid}/data", "/{uid}/data.png" }, method = RequestMethod.GET )
    public void getMapData( @PathVariable String uid,
        @RequestParam( value = "date", required = false ) Date date,
        @RequestParam( value = "ou", required = false ) String ou,
        @RequestParam( required = false ) Integer width,
        @RequestParam( required = false ) Integer height,
        @RequestParam( value = "attachment", required = false ) boolean attachment,
        HttpServletResponse response ) throws Exception
    {
        Map map = mappingService.getMapNoAcl( uid );

        if ( map == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Map does not exist: " + uid ) );
        }

        if ( width != null && width < MAP_MIN_WIDTH )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Min map width is " + MAP_MIN_WIDTH + ": " + width ) );
        }

        if ( height != null && height < MAP_MIN_HEIGHT )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Min map height is " + MAP_MIN_HEIGHT + ": " + height ) );
        }

        OrganisationUnit unit = ou != null ? organisationUnitService.getOrganisationUnit( ou ) : null;

        renderMapViewPng( map, date, unit, width, height, attachment, response );
    }

    //--------------------------------------------------------------------------
    // Hooks
    //--------------------------------------------------------------------------

    @Override
    public void postProcessEntity( Map map ) throws Exception
    {
        I18nFormat format = i18nManager.getI18nFormat();

        Set<OrganisationUnit> roots = currentUserService.getCurrentUser().getDataViewOrganisationUnitsWithFallback();

        for ( MapView view : map.getMapViews() )
        {
            view.populateAnalyticalProperties();

            for ( OrganisationUnit organisationUnit : view.getOrganisationUnits() )
            {
                view.getParentGraphMap().put( organisationUnit.getUid(), organisationUnit.getParentGraph( roots ) );
            }

            if ( view.getPeriods() != null && !view.getPeriods().isEmpty() )
            {
                for ( Period period : view.getPeriods() )
                {
                    period.setName( format.formatPeriod( period ) );
                }
            }
        }
    }

    //--------------------------------------------------------------------------
    // Supportive methods
    //--------------------------------------------------------------------------

    private void mergeMap( Map map )
    {
        if ( map.getUser() != null )
        {
            map.setUser( userService.getUser( map.getUser().getUid() ) );
        }
        else
        {
            map.setUser( currentUserService.getCurrentUser() );
        }

        map.getMapViews().forEach( this::mergeMapView );
    }

    private void mergeMapView( MapView view )
    {
        dimensionService.mergeAnalyticalObject( view );
        dimensionService.mergeEventAnalyticalObject( view );

        view.getColumnDimensions().clear();
        view.getColumnDimensions().addAll( getDimensions( view.getColumns() ) );

        if ( view.getLegendSet() != null )
        {
            view.setLegendSet( legendService.getLegendSet( view.getLegendSet().getUid() ) );
        }

        if ( view.getOrganisationUnitGroupSet() != null )
        {
            view.setOrganisationUnitGroupSet( organisationUnitGroupService.getOrganisationUnitGroupSet( view.getOrganisationUnitGroupSet().getUid() ) );
        }

        if ( view.getProgram() != null )
        {
            view.setProgram( programService.getProgram( view.getProgram().getUid() ) );
        }

        if ( view.getProgramStage() != null )
        {
            view.setProgramStage( programStageService.getProgramStage( view.getProgramStage().getUid() ) );
        }
    }

    private void renderMapViewPng( Map map, Date date, OrganisationUnit unit, Integer width, Integer height, boolean attachment, HttpServletResponse response )
        throws Exception
    {
        BufferedImage image = mapGenerationService.generateMapImage( map, date, unit, width, height );

        if ( image != null )
        {
            contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_PNG, CacheStrategy.RESPECT_SYSTEM_SETTING, "map.png", attachment );



            ImageIO.write( image, "PNG", response.getOutputStream() );
        }
        else
        {
            response.setStatus( HttpServletResponse.SC_NO_CONTENT );
        }
    }
}
