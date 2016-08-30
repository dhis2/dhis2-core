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

import com.google.common.collect.ImmutableMap;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.commons.filter.FilterUtils;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.system.filter.OrganisationUnitWithValidCoordinatesFilter;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.webdomain.GeoFeature;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = GeoFeatureController.RESOURCE_PATH )
@ApiVersion( { ApiVersion.Version.DEFAULT, ApiVersion.Version.ALL } )
public class GeoFeatureController
{
    public static final String RESOURCE_PATH = "/geoFeatures";

    private static final Map<FeatureType, Integer> FEATURE_TYPE_MAP = ImmutableMap.<FeatureType, Integer>builder().
        put( FeatureType.POINT, GeoFeature.TYPE_POINT ).
        put( FeatureType.MULTI_POLYGON, GeoFeature.TYPE_POLYGON ).
        put( FeatureType.POLYGON, GeoFeature.TYPE_POLYGON ).build();

    @Autowired
    private DataQueryService dataQueryService;

    @Autowired
    private OrganisationUnitGroupService organisationUnitGroupService;

    @Autowired
    private RenderService renderService;

    @Autowired
    private CurrentUserService currentUserService;

    // -------------------------------------------------------------------------
    // Resources
    // -------------------------------------------------------------------------

    @RequestMapping( method = RequestMethod.GET, produces = { ContextUtils.CONTENT_TYPE_JSON, ContextUtils.CONTENT_TYPE_HTML } )
    public void getGeoFeaturesJson(
        @RequestParam String ou,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( defaultValue = "false", value = "includeGroupSets" ) boolean rpIncludeGroupSets,
        @RequestParam Map<String, String> parameters,
        HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        WebOptions options = new WebOptions( parameters );
        boolean includeGroupSets = "detailed".equals( options.getViewClass() ) || rpIncludeGroupSets;

        List<GeoFeature> features = getGeoFeatures( ou, displayProperty, relativePeriodDate, userOrgUnit, request, response, includeGroupSets );

        if ( features == null )
        {
            return;
        }

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        renderService.toJson( response.getOutputStream(), features );
    }

    @RequestMapping( method = RequestMethod.GET, produces = { "application/javascript" } )
    public void getGeoFeaturesJsonP(
        @RequestParam String ou,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( defaultValue = "callback" ) String callback,
        @RequestParam( defaultValue = "false", value = "includeGroupSets" ) boolean rpIncludeGroupSets,
        @RequestParam Map<String, String> parameters,
        HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        WebOptions options = new WebOptions( parameters );
        boolean includeGroupSets = "detailed".equals( options.getViewClass() ) || rpIncludeGroupSets;

        List<GeoFeature> features = getGeoFeatures( ou, displayProperty, relativePeriodDate, userOrgUnit, request, response, includeGroupSets );

        if ( features == null )
        {
            return;
        }

        response.setContentType( "application/javascript" );
        renderService.toJsonP( response.getOutputStream(), features, callback );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Returns list of geo features. Returns null if not modified based on the
     * request.
     *
     * @param ou                 the organisation unit parameter.
     * @param displayProperty    the display property.
     * @param relativePeriodDate the date to use as basis for relative periods.
     * @param userOrgUnit        the user organisation unit parameter.
     * @param request            the HTTP request.
     * @param response           the HTTP response.
     * @param includeGroupSets   whether to include organisation unit group sets.
     * @return a list of geo features or null.
     */
    private List<GeoFeature> getGeoFeatures( String ou, DisplayProperty displayProperty, Date relativePeriodDate,
        String userOrgUnit, HttpServletRequest request, HttpServletResponse response, boolean includeGroupSets )
    {
        Set<String> set = new HashSet<>();
        set.add( ou );

        DataQueryParams params = dataQueryService.getFromUrl( set, null, AggregationType.SUM, null,
            false, false, false, false, false, false, false, false, false, displayProperty, null, null, null, relativePeriodDate, userOrgUnit, null );

        DimensionalObject dim = params.getDimension( DimensionalObject.ORGUNIT_DIM_ID );

        List<OrganisationUnit> organisationUnits = DimensionalObjectUtils.asTypedList( dim.getItems() );

        FilterUtils.filter( organisationUnits, new OrganisationUnitWithValidCoordinatesFilter() );

        boolean modified = !ContextUtils.clearIfNotModified( request, response, organisationUnits );

        if ( !modified )
        {
            return null;
        }

        List<OrganisationUnitGroupSet> groupSets = includeGroupSets ? organisationUnitGroupService.getAllOrganisationUnitGroupSets() : null;

        List<GeoFeature> features = new ArrayList<>();

        Set<OrganisationUnit> roots = currentUserService.getCurrentUser().getDataViewOrganisationUnitsWithFallback();

        for ( OrganisationUnit unit : organisationUnits )
        {
            GeoFeature feature = new GeoFeature();
            
            Integer ty = unit.getFeatureType() != null ? FEATURE_TYPE_MAP.get( unit.getFeatureType() ) : null;
            
            feature.setId( unit.getUid() );
            feature.setCode( unit.getCode() );
            feature.setHcd( unit.hasChildrenWithCoordinates() );
            feature.setHcu( unit.hasCoordinatesUp() );
            feature.setLe( unit.getLevel() );
            feature.setPg( unit.getParentGraph( roots ) );
            feature.setPi( unit.getParent() != null ? unit.getParent().getUid() : null );
            feature.setPn( unit.getParent() != null ? unit.getParent().getDisplayName() : null );
            feature.setTy( ObjectUtils.firstNonNull( ty, 0 ) );
            feature.setCo( unit.getCoordinates() );
            feature.setNa( unit.getDisplayProperty( params.getDisplayProperty() ) );

            if ( includeGroupSets )
            {
                for ( OrganisationUnitGroupSet groupSet : groupSets )
                {
                    OrganisationUnitGroup group = unit.getGroupInGroupSet( groupSet );

                    if ( group != null )
                    {
                        feature.getDimensions().put( groupSet.getUid(), group.getName() );
                    }
                }
            }

            features.add( feature );
        }

        Collections.sort( features, ( o1, o2 ) -> Integer.valueOf( o1.getTy() ).compareTo( Integer.valueOf( o2.getTy() ) ) );

        return features;
    }
}
