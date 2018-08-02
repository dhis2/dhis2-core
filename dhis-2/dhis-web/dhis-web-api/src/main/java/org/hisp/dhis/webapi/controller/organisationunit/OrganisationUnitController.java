package org.hisp.dhis.webapi.controller.organisationunit;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.ServiceProvider;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.completeness.DataSetCompletenessResult;
import org.hisp.dhis.completeness.DataSetCompletenessService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dxf2.common.TranslateParams;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.fieldfilter.Defaults;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitQueryParams;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.organisationunit.comparator.OrganisationUnitByLevelComparator;
import org.hisp.dhis.orgunitdistribution.OrgUnitDistributionService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.schema.descriptors.OrganisationUnitSchemaDescriptor;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.dhis.version.VersionService;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.jfree.chart.JFreeChart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = OrganisationUnitSchemaDescriptor.API_ENDPOINT )
public class OrganisationUnitController
    extends AbstractCrudController<OrganisationUnit>
{
    /* static configurations for Distribution Chart */
    private static final int DISTRIBUTION_CHART_HEIGHT = 600;
    private static final int DISTRIBUTION_CHART_WIDTH = 800;
    private static final String DISTRIBUTION_CHART_IMAGE_FORMAT = "png";

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private OrganisationUnitGroupService organisationUnitGroupService;

    @Autowired
    private OrgUnitDistributionService distributionService;

    @Autowired
    private VersionService versionService;

    @Autowired
    private I18nManager i18nManager;

    @Autowired
    private ContextUtils contextUtils;

    @Resource( name = "dataCompletenessServiceProvider" )
    private ServiceProvider<DataSetCompletenessService> serviceProvider;

    @Override
    @SuppressWarnings( "unchecked" )
    protected List<OrganisationUnit> getEntityList( WebMetadata metadata, WebOptions options, List<String> filters, List<Order> orders )
        throws QueryParserException
    {
        List<OrganisationUnit> objects = Lists.newArrayList();

        User currentUser = currentUserService.getCurrentUser();

        boolean anySpecialPropertySet = ObjectUtils.anyIsTrue( options.isTrue( "userOnly" ),
            options.isTrue( "userDataViewOnly" ), options.isTrue( "userDataViewFallback" ), options.isTrue( "levelSorted" ) );
        boolean anyQueryPropertySet = ObjectUtils.firstNonNull( options.get( "query" ), options.getInt( "level" ),
            options.getInt( "maxLevel" ) ) != null || options.isTrue( "withinUserHierarchy" );
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
            Collections.sort( objects, OrganisationUnitByLevelComparator.INSTANCE );
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
            params.setParents( options.isTrue( "withinUserHierarchy" ) ? currentUser.getOrganisationUnits() : Sets.newHashSet() );

            objects = organisationUnitService.getOrganisationUnitsByQuery( params );
        }

        // ---------------------------------------------------------------------
        // Standard Query handling
        // ---------------------------------------------------------------------

        Query query = queryService.getQueryFromUrl( getEntityClass(), filters, orders, options.getRootJunction() );
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

        IdentifiableObject member = null;

        if ( memberObject != null && memberCollection != null && (member = manager.get( memberObject )) != null )
        {
            for ( OrganisationUnit unit : list )
            {
                Long count = organisationUnitService.getOrganisationUnitHierarchyMemberCount( unit, member, memberCollection );

                unit.setMemberCount( (count != null ? count.intValue() : 0) );
            }
        }

        return list;
    }

    @Override
    protected List<OrganisationUnit> getEntity( String uid, WebOptions options )
    {
        OrganisationUnit organisationUnit = manager.get( getEntityClass(), uid );

        List<OrganisationUnit> organisationUnits = Lists.newArrayList();

        if ( organisationUnit == null )
        {
            return organisationUnits;
        }

        if ( options.contains( "includeChildren" ) )
        {
            options.getOptions().put( "useWrapper", "true" );
            organisationUnits.add( organisationUnit );
            organisationUnits.addAll( organisationUnit.getChildren() );
        }
        else if ( options.contains( "includeDescendants" ) )
        {
            options.getOptions().put( "useWrapper", "true" );
            organisationUnits.addAll( organisationUnitService.getOrganisationUnitWithChildren( uid ) );
        }
        else if ( options.contains( "includeAncestors" ) )
        {
            options.getOptions().put( "useWrapper", "true" );
            organisationUnits.add( organisationUnit );
            List<OrganisationUnit> ancestors = organisationUnit.getAncestors();
            Collections.reverse( ancestors );
            organisationUnits.addAll( ancestors );
        }
        else if ( options.contains( "level" ) )
        {
            options.getOptions().put( "useWrapper", "true" );
            int level = options.getInt( "level" );
            int ouLevel = organisationUnit.getLevel();
            int targetLevel = ouLevel + level;
            organisationUnits.addAll( organisationUnitService.getOrganisationUnitsAtLevel( targetLevel, organisationUnit ) );
        }
        else
        {
            organisationUnits.add( organisationUnit );
        }

        return organisationUnits;
    }

    @RequestMapping( value = "/{uid}/parents", method = RequestMethod.GET )
    public @ResponseBody List<OrganisationUnit> getEntityList( @PathVariable( "uid" ) String uid,
        @RequestParam Map<String, String> parameters, Model model, TranslateParams translateParams,
        HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        setUserContext( translateParams );
        OrganisationUnit organisationUnit = manager.get( getEntityClass(), uid );
        List<OrganisationUnit> organisationUnits = Lists.newArrayList();

        if ( organisationUnit != null )
        {
            OrganisationUnit organisationUnitParent = organisationUnit.getParent();

            while ( organisationUnitParent != null )
            {
                organisationUnits.add( organisationUnitParent );
                organisationUnitParent = organisationUnitParent.getParent();
            }
        }

        WebMetadata metadata = new WebMetadata();
        metadata.setOrganisationUnits( organisationUnits );

        return organisationUnits;
    }

    @RequestMapping( value = "", method = RequestMethod.GET, produces = { "application/json+geo", "application/json+geojson" } )
    public void getGeoJson(
        @RequestParam( value = "level", required = false ) List<Integer> rpLevels,
        @RequestParam( value = "parent", required = false ) List<String> rpParents,
        @RequestParam( value = "properties", required = false, defaultValue = "true" ) boolean rpProperties,
        User currentUser, HttpServletResponse response ) throws IOException
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

        List<OrganisationUnit> organisationUnits = organisationUnitService.getOrganisationUnitsAtLevels( rpLevels, parents );

        response.setContentType( "application/json" );

        JsonFactory jsonFactory = new JsonFactory();
        JsonGenerator generator = jsonFactory.createGenerator( response.getOutputStream() );

        generator.writeStartObject();
        generator.writeStringField( "type", "FeatureCollection" );
        generator.writeArrayFieldStart( "features" );

        for ( OrganisationUnit organisationUnit : organisationUnits )
        {
            writeFeature( generator, organisationUnit, rpProperties, currentUser );
        }

        generator.writeEndArray();
        generator.writeEndObject();

        generator.close();
    }

    @RequestMapping( value = "{ou}/rateSummary", method = RequestMethod.GET )
    public void getDataSetReport( @PathVariable( "ou" ) String ou,
        @RequestParam( required = false ) String ds,
        @RequestParam String pe,
        @RequestParam String criteria,
        @RequestParam( required = false ) Set<String> groupUids,
        HttpServletResponse response )
        throws IOException, WebMessageException
    {
        OrganisationUnit selectedOrgunit = organisationUnitService.getOrganisationUnit( ou );
        DataSet selectedDataSet = dataSetService.getDataSet( ds );
        Period selectedPeriod = PeriodType.getPeriodFromIsoString( pe );

        if ( selectedOrgunit == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Illegal organisation unit identifier: " + ou ) );
        }

        if ( selectedPeriod == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Illegal period identifier: " + pe ) );
        }

        selectedPeriod = periodService.reloadPeriod( selectedPeriod );

        // ---------------------------------------------------------------------
        // Configure response
        // ---------------------------------------------------------------------

        contextUtils
            .configureResponse( response, ContextUtils.CONTENT_TYPE_HTML, CacheStrategy.RESPECT_SYSTEM_SETTING );

        // ---------------------------------------------------------------------
        // Assemble report
        // ---------------------------------------------------------------------
        List<DataSetCompletenessResult> mainResults;
        List<DataSetCompletenessResult> footerResults = new ArrayList<>();
        DataSetCompletenessService completenessService = serviceProvider.provide( criteria );

        Set<Integer> groupIds = new HashSet<>();
        if ( groupUids != null )
        {
            for ( String groupUid : groupUids )
            {
                OrganisationUnitGroup organisationUnitGroup = organisationUnitGroupService
                    .getOrganisationUnitGroup( groupUid );
                if ( organisationUnitGroup != null )
                {
                    groupIds.add( organisationUnitGroup.getId() );
                }
            }
        }

        if ( selectedDataSet != null )
        {
            mainResults = new ArrayList<>( completenessService.getDataSetCompleteness(
                selectedPeriod.getId(), getIdentifiers( selectedOrgunit.getChildren() ), selectedDataSet.getId(),
                groupIds ) );

            footerResults = new ArrayList<>(
                completenessService
                    .getDataSetCompleteness( selectedPeriod.getId(), Arrays.asList( selectedOrgunit.getId() ),
                        selectedDataSet.getId(), groupIds ) );
        }
        else
        {
            mainResults = new ArrayList<>( completenessService.getDataSetCompleteness(
                selectedPeriod.getId(), selectedOrgunit.getId(), groupIds ) );
        }

        // ---------------------------------------------------------------------
        // Write response
        // ---------------------------------------------------------------------
        I18n i18n = i18nManager.getI18n();
        String title =
            (selectedOrgunit != null ? selectedOrgunit.getName() : "") +
                (selectedDataSet != null ? "-" + selectedDataSet.getName() : "") +
                (selectedPeriod != null ? "-" + i18nManager.getI18nFormat().formatPeriod( selectedPeriod ) : "");

        Grid grid = new ListGrid().setTitle( title );

        grid.addHeader( new GridHeader( i18n.getString( "name" ), false, true ) );
        grid.addHeader( new GridHeader( i18n.getString( "actual_reports" ), false, false ) );
        grid.addHeader( new GridHeader( i18n.getString( "expected_reports" ), false, false ) );
        grid.addHeader( new GridHeader( i18n.getString( "percent" ), false, false ) );
        grid.addHeader( new GridHeader( i18n.getString( "reports_on_time" ), false, false ) );
        grid.addHeader( new GridHeader( i18n.getString( "percent_on_time" ), false, false ) );

        for ( DataSetCompletenessResult result : mainResults )
        {
            grid.addRow();
            grid.addValue( result.getName() );
            grid.addValue( result.getRegistrations() );
            grid.addValue( result.getSources() );
            grid.addValue( result.getPercentage() );
            grid.addValue( result.getRegistrationsOnTime() );
            grid.addValue( result.getPercentageOnTime() );
        }

        if ( grid.getWidth() >= 4 )
        {
            grid.sortGrid( 4, 1 );
        }

        for ( DataSetCompletenessResult result : footerResults )
        {
            grid.addRow();
            grid.addValue( result.getName() );
            grid.addValue( result.getRegistrations() );
            grid.addValue( result.getSources() );
            grid.addValue( result.getPercentage() );
            grid.addValue( result.getRegistrationsOnTime() );
            grid.addValue( result.getPercentageOnTime() );
        }

        Writer output = response.getWriter();
        GridUtils.toHtmlCss( grid, output );
    }

    @RequestMapping( value = "{ou}/distributionReport", method = RequestMethod.GET )
    public void getDistributionReport( @PathVariable( "ou" ) String ou,
        @RequestParam String groupSetId,
        HttpServletResponse response) throws IOException, WebMessageException
    {
        OrganisationUnit selectedOrgunit = organisationUnitService.getOrganisationUnit( ou );
        OrganisationUnitGroupSet selectedGroupSet = organisationUnitGroupService.getOrganisationUnitGroupSet( groupSetId );

        if ( selectedOrgunit == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Illegal organisation unit identifier: " + ou ) );
        }

        if ( selectedGroupSet == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Illegal organisation unit group set identifier: " + groupSetId ) );
        }

        // ---------------------------------------------------------------------
        // Configure response
        // ---------------------------------------------------------------------
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_HTML, CacheStrategy.RESPECT_SYSTEM_SETTING );

        // ---------------------------------------------------------------------
        // Assemble report
        // ---------------------------------------------------------------------
        Grid grid = distributionService.getOrganisationUnitDistribution( selectedGroupSet, selectedOrgunit, false );

        // ---------------------------------------------------------------------
        // Write response
        // ---------------------------------------------------------------------
        Writer output = response.getWriter();
        GridUtils.toHtmlCss( grid, output );
    }

    @RequestMapping( value = "{ou}/distributionChart", method = RequestMethod.GET )
    public @ResponseBody byte[] getDistributionChart( @PathVariable( "ou" ) String ou,
        @RequestParam String groupSetId,
        HttpServletResponse response) throws IOException, WebMessageException
    {
        OrganisationUnit selectedOrgunit = organisationUnitService.getOrganisationUnit( ou );
        OrganisationUnitGroupSet selectedGroupSet = organisationUnitGroupService.getOrganisationUnitGroupSet( groupSetId );

        if ( selectedOrgunit == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Illegal organisation unit identifier: " + ou ) );
        }

        if ( selectedGroupSet == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Illegal organisation unit group set identifier: " + groupSetId ) );
        }

        // ---------------------------------------------------------------------
        // Assemble chart
        // ---------------------------------------------------------------------
        JFreeChart chart = distributionService.getOrganisationUnitDistributionChart( selectedGroupSet, selectedOrgunit );

        // ---------------------------------------------------------------------
        // Write response
        // ---------------------------------------------------------------------
        BufferedImage bufferedImage = chart.createBufferedImage(DISTRIBUTION_CHART_WIDTH, DISTRIBUTION_CHART_HEIGHT);
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, DISTRIBUTION_CHART_IMAGE_FORMAT, bas);

        return bas.toByteArray();
    }

    public void writeFeature( JsonGenerator generator, OrganisationUnit organisationUnit,
        boolean includeProperties, User user ) throws IOException
    {
        if ( organisationUnit.getFeatureType() == null || organisationUnit.getCoordinates() == null )
        {
            return;
        }

        FeatureType featureType = organisationUnit.getFeatureType();

        // If featureType is anything other than Point, just assume MultiPolygon

        if ( !(featureType == FeatureType.POINT) )
        {
            featureType = FeatureType.MULTI_POLYGON;
        }

        generator.writeStartObject();

        generator.writeStringField( "type", "Feature" );
        generator.writeStringField( "id", organisationUnit.getUid() );

        generator.writeObjectFieldStart( "geometry" );
        generator.writeObjectField( "type", featureType.value() );

        generator.writeFieldName( "coordinates" );
        generator.writeRawValue( organisationUnit.getCoordinates() );

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
    protected void postDeleteEntity()
    {
        versionService.updateVersion( VersionService.ORGANISATIONUNIT_VERSION );
    }
}
