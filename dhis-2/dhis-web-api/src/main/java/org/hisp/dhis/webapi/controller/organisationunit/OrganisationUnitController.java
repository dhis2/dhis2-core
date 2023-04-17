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
package org.hisp.dhis.webapi.controller.organisationunit;

import static java.lang.Math.max;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.system.util.GeoUtils.getCoordinatesFromGeometry;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.common.OrderParams;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfilter.Defaults;
import org.hisp.dhis.merge.orgunit.OrgUnitMergeQuery;
import org.hisp.dhis.merge.orgunit.OrgUnitMergeService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitQueryParams;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.organisationunit.comparator.OrganisationUnitByLevelComparator;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.schema.descriptors.OrganisationUnitSchemaDescriptor;
import org.hisp.dhis.split.orgunit.OrgUnitSplitQuery;
import org.hisp.dhis.split.orgunit.OrgUnitSplitService;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.dhis.version.VersionService;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.webdomain.StreamingJsonRoot;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@OpenApi.Tags( "metadata" )
@Controller
@RequestMapping( value = OrganisationUnitSchemaDescriptor.API_ENDPOINT )
public class OrganisationUnitController
    extends AbstractCrudController<OrganisationUnit>
{
    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private VersionService versionService;

    @Autowired
    private OrgUnitSplitService orgUnitSplitService;

    @Autowired
    private OrgUnitMergeService orgUnitMergeService;

    @ResponseStatus( HttpStatus.OK )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_ORGANISATION_UNIT_SPLIT')" )
    @PostMapping( value = "/split", produces = APPLICATION_JSON_VALUE )
    public @ResponseBody WebMessage splitOrgUnits( @RequestBody OrgUnitSplitQuery query )
    {
        orgUnitSplitService.split( orgUnitSplitService.getFromQuery( query ) );

        return ok( "Organisation unit split" );
    }

    @ResponseStatus( HttpStatus.OK )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_ORGANISATION_UNIT_MERGE')" )
    @PostMapping( value = "/merge", produces = APPLICATION_JSON_VALUE )
    public @ResponseBody WebMessage mergeOrgUnits( @RequestBody OrgUnitMergeQuery query )
    {
        orgUnitMergeService.merge( orgUnitMergeService.getFromQuery( query ) );

        return ok( "Organisation units merged" );
    }

    @OpenApi.Param( name = "fields", value = String[].class )
    @OpenApi.Param( name = "filter", value = String[].class )
    @OpenApi.Params( WebOptions.class )
    @OpenApi.Response( ObjectListResponse.class )
    @GetMapping( "/{uid}/children" )
    public @ResponseBody ResponseEntity<StreamingJsonRoot<OrganisationUnit>> getChildren(
        @OpenApi.Param( UID.class ) @PathVariable( "uid" ) String uid,
        @RequestParam Map<String, String> rpParameters, OrderParams orderParams,
        HttpServletResponse response, @CurrentUser User currentUser )
        throws ForbiddenException,
        BadRequestException,
        NotFoundException
    {
        OrganisationUnit parent = getEntity( uid );
        return getObjectList( rpParameters, orderParams, response, currentUser, false,
            params -> List.copyOf( parent.getChildren() ) );
    }

    @OpenApi.Param( name = "fields", value = String[].class )
    @OpenApi.Param( name = "filter", value = String[].class )
    @OpenApi.Params( WebOptions.class )
    @OpenApi.Response( ObjectListResponse.class )
    @GetMapping( value = "/{uid}/children", params = "level" )
    public @ResponseBody ResponseEntity<StreamingJsonRoot<OrganisationUnit>> getChildrenWithLevel(
        @OpenApi.Param( UID.class ) @PathVariable( "uid" ) String uid,
        @RequestParam int level,
        @RequestParam Map<String, String> rpParameters, OrderParams orderParams,
        HttpServletResponse response, @CurrentUser User currentUser )
        throws ForbiddenException,
        BadRequestException,
        NotFoundException
    {
        OrganisationUnit parent = getEntity( uid );
        return getObjectList( rpParameters, orderParams, response, currentUser, false, params -> organisationUnitService
            .getOrganisationUnitsAtLevel( parent.getLevel() + max( 0, level ), parent ) );
    }

    @OpenApi.Param( name = "fields", value = String[].class )
    @OpenApi.Param( name = "filter", value = String[].class )
    @OpenApi.Params( WebOptions.class )
    @OpenApi.Response( ObjectListResponse.class )
    @GetMapping( "/{uid}/descendants" )
    public @ResponseBody ResponseEntity<StreamingJsonRoot<OrganisationUnit>> getDescendants(
        @OpenApi.Param( UID.class ) @PathVariable( "uid" ) String uid,
        @RequestParam Map<String, String> rpParameters, OrderParams orderParams,
        HttpServletResponse response, @CurrentUser User currentUser )
        throws ForbiddenException,
        BadRequestException,
        NotFoundException
    {
        OrganisationUnit parent = getEntity( uid );
        return getObjectList( rpParameters, orderParams, response, currentUser, false,
            params -> organisationUnitService.getOrganisationUnitWithChildren( parent.getUid() ) );
    }

    @OpenApi.Param( name = "fields", value = String[].class )
    @OpenApi.Param( name = "filter", value = String[].class )
    @OpenApi.Params( WebOptions.class )
    @OpenApi.Response( ObjectListResponse.class )
    @GetMapping( "/{uid}/ancestors" )
    public @ResponseBody ResponseEntity<StreamingJsonRoot<OrganisationUnit>> getAncestors(
        @OpenApi.Param( UID.class ) @PathVariable( "uid" ) String uid,
        @RequestParam Map<String, String> rpParameters, OrderParams orderParams,
        HttpServletResponse response, @CurrentUser User currentUser )
        throws ForbiddenException,
        BadRequestException,
        NotFoundException
    {
        OrganisationUnit parent = getEntity( uid );
        return getObjectList( rpParameters, orderParams, response, currentUser, false, params -> {
            List<OrganisationUnit> res = new ArrayList<>();
            res.addAll( parent.getAncestors() );
            Collections.reverse( res );
            res.add( 0, parent );
            return res;
        } );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    protected List<OrganisationUnit> getEntityList( WebMetadata metadata, WebOptions options, List<String> filters,
        List<Order> orders )
        throws QueryParserException
    {
        List<OrganisationUnit> objects = Lists.newArrayList();

        User currentUser = currentUserService.getCurrentUser();

        boolean anySpecialPropertySet = ObjectUtils.anyIsTrue( options.isTrue( "userOnly" ),
            options.isTrue( "userDataViewOnly" ), options.isTrue( "userDataViewFallback" ),
            options.isTrue( "levelSorted" ) );
        boolean anyQueryPropertySet = ObjectUtils.firstNonNull( options.get( "query" ), options.getInt( "level" ),
            options.getInt( "maxLevel" ) ) != null || options.isTrue( "withinUserHierarchy" )
            || options.isTrue( "withinUserSearchHierarchy" );
        String memberObject = options.get( "memberObject" );
        String memberCollection = options.get( "memberCollection" );

        // ---------------------------------------------------------------------
        // Special parameter handling
        // ---------------------------------------------------------------------

        if ( options.isTrue( "userOnly" ) )
        {
            objects = new ArrayList<>( currentUser.getOrganisationUnits() );
        }
        else if ( options.isTrue( "userDataViewOnly" ) )
        {
            objects = new ArrayList<>( currentUser.getDataViewOrganisationUnits() );
        }
        else if ( options.isTrue( "userDataViewFallback" ) )
        {
            if ( currentUser.hasDataViewOrganisationUnit() )
            {
                objects = new ArrayList<>( currentUser.getDataViewOrganisationUnits() );
            }
            else
            {
                objects = organisationUnitService.getOrganisationUnitsAtLevel( 1 );
            }
        }
        else if ( options.isTrue( "levelSorted" ) )
        {
            objects = new ArrayList<>( manager.getAll( getEntityClass() ) );
            objects.sort( OrganisationUnitByLevelComparator.INSTANCE );
        }

        // ---------------------------------------------------------------------
        // OrganisationUnitQueryParams query parameter handling
        // ---------------------------------------------------------------------

        else if ( anyQueryPropertySet )
        {
            OrganisationUnitQueryParams params = new OrganisationUnitQueryParams();
            params.setQuery( options.get( "query" ) );
            params.setLevel( options.getInt( "level" ) );
            params.setMaxLevels( options.getInt( "maxLevel" ) );

            params.setParents( options.isTrue( "withinUserHierarchy" ) ? currentUser.getOrganisationUnits()
                : options.isTrue( "withinUserSearchHierarchy" )
                    ? currentUser.getTeiSearchOrganisationUnitsWithFallback()
                    : Sets.newHashSet() );

            objects = organisationUnitService.getOrganisationUnitsByQuery( params );
        }

        // ---------------------------------------------------------------------
        // Standard Query handling
        // ---------------------------------------------------------------------

        Query query = queryService.getQueryFromUrl( getEntityClass(), filters, orders, getPaginationData( options ),
            options.getRootJunction() );
        query.setUser( currentUser );
        query.setDefaultOrder();
        query.setDefaults( Defaults.valueOf( options.get( "defaults", DEFAULTS ) ) );

        if ( anySpecialPropertySet || anyQueryPropertySet )
        {
            query.setObjects( objects );
        }

        List<OrganisationUnit> list = (List<OrganisationUnit>) queryService.query( query );

        // ---------------------------------------------------------------------
        // Collection member count in hierarchy handling
        // ---------------------------------------------------------------------

        if ( memberObject != null && memberCollection != null )
        {
            Optional<? extends IdentifiableObject> member = manager.find( memberObject );
            if ( member.isPresent() )
            {
                for ( OrganisationUnit unit : list )
                {
                    Long count = organisationUnitService.getOrganisationUnitHierarchyMemberCount( unit, member.get(),
                        memberCollection );

                    unit.setMemberCount( (count != null ? count.intValue() : 0) );
                }
            }
        }

        return list;
    }

    @OpenApi.Param( name = "fields", value = String[].class )
    @OpenApi.Param( name = "filter", value = String[].class )
    @OpenApi.Params( WebOptions.class )
    @OpenApi.Response( ObjectListResponse.class )
    @GetMapping( "/{uid}/parents" )
    public @ResponseBody ResponseEntity<StreamingJsonRoot<OrganisationUnit>> getParents(
        @OpenApi.Param( UID.class ) @PathVariable( "uid" ) String uid,
        @RequestParam Map<String, String> rpParameters, OrderParams orderParams,
        HttpServletResponse response, @CurrentUser User currentUser )
        throws ForbiddenException,
        BadRequestException,
        NotFoundException
    {
        OrganisationUnit root = getEntity( uid );
        return getObjectList( rpParameters, orderParams, response, currentUser, false, params -> {
            OrganisationUnit parent = root.getParent();
            List<OrganisationUnit> res = new ArrayList<>();
            while ( parent != null )
            {
                res.add( parent );
                parent = parent.getParent();
            }
            return res;
        } );
    }

    @GetMapping( value = { "", ".geojson" }, produces = { "application/json+geo",
        "application/json+geojson" } )
    public void getGeoJson(
        @RequestParam( value = "level", required = false ) List<Integer> rpLevels,
        @OpenApi.Param( { UID[].class,
            OrganisationUnit.class } ) @RequestParam( value = "parent", required = false ) List<String> rpParents,
        @RequestParam( value = "properties", required = false, defaultValue = "true" ) boolean rpProperties,
        @CurrentUser User currentUser, HttpServletResponse response )
        throws IOException
    {
        rpLevels = rpLevels != null ? rpLevels : new ArrayList<>();
        rpParents = rpParents != null ? rpParents : new ArrayList<>();

        List<OrganisationUnit> parents = manager.getByUid( OrganisationUnit.class, rpParents );

        if ( rpLevels.isEmpty() )
        {
            rpLevels.add( 1 );
        }

        if ( parents.isEmpty() )
        {
            parents.addAll( organisationUnitService.getRootOrganisationUnits() );
        }

        List<OrganisationUnit> organisationUnits = organisationUnitService.getOrganisationUnitsAtLevels( rpLevels,
            parents );

        response.setContentType( APPLICATION_JSON_VALUE );

        try ( JsonGenerator generator = new JsonFactory().createGenerator( response.getOutputStream() ) )
        {

            generator.writeStartObject();
            generator.writeStringField( "type", "FeatureCollection" );
            generator.writeArrayFieldStart( "features" );

            for ( OrganisationUnit organisationUnit : organisationUnits )
            {
                writeFeature( generator, organisationUnit, rpProperties, currentUser );
            }

            generator.writeEndArray();
            generator.writeEndObject();
        }
    }

    private void writeFeature( JsonGenerator generator, OrganisationUnit organisationUnit,
        boolean includeProperties, User user )
        throws IOException
    {
        if ( organisationUnit.getGeometry() == null )
        {
            return;
        }

        generator.writeStartObject();

        generator.writeStringField( "type", "Feature" );
        generator.writeStringField( "id", organisationUnit.getUid() );

        generator.writeObjectFieldStart( "geometry" );
        generator.writeObjectField( "type", organisationUnit.getGeometry().getGeometryType() );

        generator.writeFieldName( "coordinates" );
        generator.writeRawValue( getCoordinatesFromGeometry( organisationUnit.getGeometry() ) );

        generator.writeEndObject();

        generator.writeObjectFieldStart( "properties" );

        if ( includeProperties )
        {
            Set<OrganisationUnit> roots = user.getDataViewOrganisationUnitsWithFallback();

            generator.writeStringField( "code", organisationUnit.getCode() );
            generator.writeStringField( "name", organisationUnit.getName() );
            generator.writeStringField( "level", String.valueOf( organisationUnit.getLevel() ) );

            if ( organisationUnit.getParent() != null )
            {
                generator.writeStringField( "parent", organisationUnit.getParent().getUid() );
            }

            generator.writeStringField( "parentGraph", organisationUnit.getParentGraph( roots ) );

            generator.writeArrayFieldStart( "groups" );

            for ( OrganisationUnitGroup group : organisationUnit.getGroups() )
            {
                generator.writeString( group.getUid() );
            }

            generator.writeEndArray();
        }

        generator.writeEndObject();

        generator.writeEndObject();
    }

    @Override
    protected void postCreateEntity( OrganisationUnit entity )
    {
        versionService.updateVersion( VersionService.ORGANISATIONUNIT_VERSION );
    }

    @Override
    protected void postUpdateEntity( OrganisationUnit entity )
    {
        versionService.updateVersion( VersionService.ORGANISATIONUNIT_VERSION );
    }

    @Override
    protected void postDeleteEntity( String entityUID )
    {
        versionService.updateVersion( VersionService.ORGANISATIONUNIT_VERSION );
    }
}
