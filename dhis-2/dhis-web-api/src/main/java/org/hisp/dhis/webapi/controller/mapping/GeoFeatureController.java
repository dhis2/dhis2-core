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
package org.hisp.dhis.webapi.controller.mapping;

import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_GROUP_DIM_ID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.common.DataQueryRequest;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.coordinate.CoordinateObject;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.webdomain.GeoFeature;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.ImmutableMap;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = GeoFeatureController.RESOURCE_PATH )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class GeoFeatureController
{
    public static final String RESOURCE_PATH = "/geoFeatures";

    private static final CacheControl GEOFEATURE_CACHE = CacheControl.maxAge( 2, TimeUnit.HOURS ).cachePrivate();

    private static final Map<FeatureType, Integer> FEATURE_TYPE_MAP = ImmutableMap.<FeatureType, Integer> builder()
        .put( FeatureType.POINT, GeoFeature.TYPE_POINT ).put( FeatureType.MULTI_POLYGON, GeoFeature.TYPE_POLYGON )
        .put( FeatureType.POLYGON, GeoFeature.TYPE_POLYGON ).build();

    private final DataQueryService dataQueryService;

    private final OrganisationUnitGroupService organisationUnitGroupService;

    private final CurrentUserService currentUserService;

    private final RenderService renderService;

    public GeoFeatureController( DataQueryService dataQueryService,
        OrganisationUnitGroupService organisationUnitGroupService, CurrentUserService currentUserService,
        RenderService renderService )
    {
        this.dataQueryService = dataQueryService;
        this.organisationUnitGroupService = organisationUnitGroupService;
        this.currentUserService = currentUserService;
        this.renderService = renderService;
    }

    // -------------------------------------------------------------------------
    // Resources
    // -------------------------------------------------------------------------

    @GetMapping( produces = { ContextUtils.CONTENT_TYPE_JSON, ContextUtils.CONTENT_TYPE_HTML } )
    @ResponseBody
    public ResponseEntity<List<GeoFeature>> getGeoFeaturesJson(
        @RequestParam( required = false ) String ou,
        @RequestParam( required = false ) String oug,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( defaultValue = "false", value = "includeGroupSets" ) boolean rpIncludeGroupSets,
        @RequestParam Map<String, String> parameters,
        DhisApiVersion apiVersion,
        HttpServletRequest request, HttpServletResponse response )
    {
        WebOptions options = new WebOptions( parameters );
        boolean includeGroupSets = "detailed".equals( options.getViewClass() ) || rpIncludeGroupSets;

        List<GeoFeature> features = getGeoFeatures( ou, oug, displayProperty, relativePeriodDate, userOrgUnit, request,
            response, includeGroupSets, apiVersion );

        return ResponseEntity.ok()
            .header( HttpHeaders.CACHE_CONTROL, GEOFEATURE_CACHE.getHeaderValue() )
            .body( features );
    }

    @GetMapping( produces = { "application/javascript" } )
    public void getGeoFeaturesJsonP(
        @RequestParam( required = false ) String ou,
        @RequestParam( required = false ) String oug,
        @RequestParam( required = false ) DisplayProperty displayProperty,
        @RequestParam( required = false ) Date relativePeriodDate,
        @RequestParam( required = false ) String userOrgUnit,
        @RequestParam( defaultValue = "callback" ) String callback,
        @RequestParam( defaultValue = "false", value = "includeGroupSets" ) boolean rpIncludeGroupSets,
        @RequestParam Map<String, String> parameters,
        DhisApiVersion apiVersion,
        HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        WebOptions options = new WebOptions( parameters );
        boolean includeGroupSets = "detailed".equals( options.getViewClass() ) || rpIncludeGroupSets;

        List<GeoFeature> features = getGeoFeatures( ou, oug, displayProperty, relativePeriodDate, userOrgUnit, request,
            response, includeGroupSets, apiVersion );

        if ( features == null )
        {
            return;
        }

        ContextUtils.setCacheControl( response, GEOFEATURE_CACHE );
        response.setContentType( "application/javascript" );
        renderService.toJsonP( response.getOutputStream(), features, callback );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Returns a list of {@link GeoFeature}. Returns null if not modified based
     * on the request.
     *
     * @param ou the organisation unit parameter
     * @param oug the organisation unit group parameter
     * @param displayProperty the display property.
     * @param relativePeriodDate the date to use as basis for relative periods.
     * @param userOrgUnit the user organisation unit parameter.
     * @param request the HTTP request.
     * @param response the HTTP response.
     * @param includeGroupSets whether to include organisation unit group sets.
     * @return a list of geo features or null.
     */
    private List<GeoFeature> getGeoFeatures( String ou, String oug, DisplayProperty displayProperty,
        Date relativePeriodDate,
        String userOrgUnit, HttpServletRequest request, HttpServletResponse response, boolean includeGroupSets,
        DhisApiVersion apiVersion )
    {
        Set<String> dimensionParams = new HashSet<>();
        dimensionParams.add( ou );
        dimensionParams.add( oug );

        DataQueryRequest dataQueryRequest = DataQueryRequest.newBuilder()
            .dimension( dimensionParams )
            .aggregationType( AggregationType.SUM )
            .displayProperty( displayProperty )
            .relativePeriodDate( relativePeriodDate )
            .userOrgUnit( userOrgUnit )
            .apiVersion( apiVersion ).build();

        DataQueryParams params = dataQueryService.getFromRequest( dataQueryRequest );

        boolean useOrgUnitGroup = ou == null;
        DimensionalObject dimensionalObject = params
            .getDimension( useOrgUnitGroup ? ORGUNIT_GROUP_DIM_ID : ORGUNIT_DIM_ID );

        if ( dimensionalObject == null )
        {
            throw new IllegalArgumentException( "Dimension is present in query without any valid dimension options" );
        }

        List<DimensionalItemObject> dimensionalItemObjects = DimensionalObjectUtils
            .asTypedList( dimensionalObject.getItems() );

        dimensionalItemObjects = dimensionalItemObjects.stream().filter( object -> {
            CoordinateObject coordinateObject = (CoordinateObject) object;
            return coordinateObject != null &&
                coordinateObject.getFeatureType() != null &&
                coordinateObject.hasCoordinates() &&
                (coordinateObject.getFeatureType() != FeatureType.POINT
                    || ValidationUtils.coordinateIsValid( coordinateObject.getCoordinates() ));
        } ).collect( Collectors.toList() );

        boolean modified = !ContextUtils.clearIfNotModified( request, response, dimensionalItemObjects );

        if ( !modified )
        {
            return null;
        }

        return getGeoFeatures( params, dimensionalItemObjects, includeGroupSets, useOrgUnitGroup );
    }

    /**
     * Returns a list of {@link GeoFeature}.
     *
     * @param params the {@link DataQueryParams}.
     * @param dimensionalItemObjects the list of {@link DimensionalItemObject}.
     * @param includeGroupSets whether to include group sets.
     * @param useOrgUnitGroup whether to use org unit group when retrieving
     *        features.
     * @return a list of {@link GeoFeature}.
     */
    private List<GeoFeature> getGeoFeatures( DataQueryParams params,
        List<DimensionalItemObject> dimensionalItemObjects, boolean includeGroupSets, boolean useOrgUnitGroup )
    {
        List<GeoFeature> features = new ArrayList<>();

        List<OrganisationUnitGroupSet> groupSets = includeGroupSets
            ? organisationUnitGroupService.getAllOrganisationUnitGroupSets()
            : new ArrayList<>();

        Set<OrganisationUnit> roots = currentUserService.getCurrentUser().getDataViewOrganisationUnitsWithFallback();

        for ( DimensionalItemObject unit : dimensionalItemObjects )
        {
            GeoFeature feature = new GeoFeature();

            CoordinateObject coordinateObject = (CoordinateObject) unit;

            Integer ty = coordinateObject.getFeatureType() != null
                ? FEATURE_TYPE_MAP.get( coordinateObject.getFeatureType() )
                : null;

            feature.setId( unit.getUid() );
            feature.setCode( unit.getCode() );
            feature.setHcd( coordinateObject.hasDescendantsWithCoordinates() );

            if ( !useOrgUnitGroup )
            {
                OrganisationUnit castUnit = (OrganisationUnit) unit;
                feature.setHcu( castUnit.hasCoordinatesUp() );
                feature.setLe( castUnit.getLevel() );
                feature.setPg( castUnit.getParentGraph( roots ) );
                feature.setPi( castUnit.getParent() != null ? castUnit.getParent().getUid() : null );
                feature.setPn( castUnit.getParent() != null ? castUnit.getParent().getDisplayName() : null );

                if ( includeGroupSets )
                {
                    for ( OrganisationUnitGroupSet groupSet : groupSets )
                    {
                        OrganisationUnitGroup group = castUnit.getGroupInGroupSet( groupSet );

                        if ( group != null )
                        {
                            feature.getDimensions().put( groupSet.getUid(), group.getUid() );
                        }
                    }
                }
            }

            feature.setTy( ObjectUtils.firstNonNull( ty, 0 ) );
            feature.setCo( coordinateObject.getCoordinates() );
            feature.setNa( unit.getDisplayProperty( params.getDisplayProperty() ) );

            features.add( feature );
        }

        features.sort( Comparator.comparing( GeoFeature::getTy ) );

        return features;
    }
}
