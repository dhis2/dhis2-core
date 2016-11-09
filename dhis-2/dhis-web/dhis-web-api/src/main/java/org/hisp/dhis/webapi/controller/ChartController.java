package org.hisp.dhis.webapi.controller;

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

import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.chart.ChartService;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.schema.descriptors.ChartSchemaDescriptor;
import org.hisp.dhis.system.util.CodecUtils;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.Set;

import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensions;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = ChartSchemaDescriptor.API_ENDPOINT )
public class ChartController
    extends AbstractCrudController<Chart>
{
    @Autowired
    private ChartService chartService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private IndicatorService indicatorService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DimensionService dimensionService;

    @Autowired
    private I18nManager i18nManager;

    @Autowired
    private ContextUtils contextUtils;

    //--------------------------------------------------------------------------
    // CRUD
    //--------------------------------------------------------------------------

    @Override
    protected Chart deserializeJsonEntity( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        Chart chart = super.deserializeJsonEntity( request, response );
        mergeChart( chart );

        return chart;
    }

    //--------------------------------------------------------------------------
    // Get data
    //--------------------------------------------------------------------------

    @RequestMapping( value = { "/{uid}/data", "/{uid}/data.png" }, method = RequestMethod.GET )
    public void getChart(
        @PathVariable( "uid" ) String uid,
        @RequestParam( value = "date", required = false ) Date date,
        @RequestParam( value = "ou", required = false ) String ou,
        @RequestParam( value = "width", defaultValue = "800", required = false ) int width,
        @RequestParam( value = "height", defaultValue = "500", required = false ) int height,
        @RequestParam( value = "attachment", required = false ) boolean attachment,
        HttpServletResponse response ) throws IOException, WebMessageException
    {
        Chart chart = chartService.getChartNoAcl( uid );

        if ( chart == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Chart does not exist: " + uid ) );
        }

        OrganisationUnit unit = ou != null ? organisationUnitService.getOrganisationUnit( ou ) : null;

        JFreeChart jFreeChart = chartService.getJFreeChart( chart, date, unit, i18nManager.getI18nFormat() );

        String filename = CodecUtils.filenameEncode( chart.getName() ) + ".png";

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_PNG, CacheStrategy.RESPECT_SYSTEM_SETTING, filename, attachment );

        ChartUtilities.writeChartAsPNG( response.getOutputStream(), jFreeChart, width, height );
    }

    @RequestMapping( value = { "/data", "/data.png" }, method = RequestMethod.GET )
    public void getChart(
        @RequestParam( value = "in" ) String indicatorUid,
        @RequestParam( value = "ou" ) String organisationUnitUid,
        @RequestParam( value = "periods", required = false ) boolean periods,
        @RequestParam( value = "width", defaultValue = "800", required = false ) int width,
        @RequestParam( value = "height", defaultValue = "500", required = false ) int height,
        @RequestParam( value = "skipTitle", required = false ) boolean skipTitle,
        @RequestParam( value = "attachment", required = false ) boolean attachment,
        HttpServletResponse response ) throws IOException
    {
        Indicator indicator = indicatorService.getIndicator( indicatorUid );
        OrganisationUnit unit = organisationUnitService.getOrganisationUnit( organisationUnitUid );

        JFreeChart chart;

        if ( periods )
        {
            chart = chartService.getJFreePeriodChart( indicator, unit, !skipTitle, i18nManager.getI18nFormat() );
        }
        else
        {
            chart = chartService.getJFreeOrganisationUnitChart( indicator, unit, !skipTitle, i18nManager.getI18nFormat() );
        }

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_PNG, CacheStrategy.RESPECT_SYSTEM_SETTING, "chart.png", attachment );

        ChartUtilities.writeChartAsPNG( response.getOutputStream(), chart, width, height );
    }

    @RequestMapping( value = { "/history/data", "/history/data.png" }, method = RequestMethod.GET )
    public void getHistoryChart(
        @RequestParam String de,
        @RequestParam String co,
        @RequestParam String cp,
        @RequestParam String pe,
        @RequestParam String ou,
        @RequestParam( defaultValue = "525", required = false ) int width,
        @RequestParam( defaultValue = "300", required = false ) int height,
        HttpServletResponse response ) throws IOException, WebMessageException
    {
        DataElement dataElement = dataElementService.getDataElement( de );

        if ( dataElement == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Data element does not exist: " + de ) );
        }

        DataElementCategoryOptionCombo categoryOptionCombo = categoryService.getDataElementCategoryOptionCombo( co );

        if ( categoryOptionCombo == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Category option combo does not exist: " + co ) );
        }
        
        DataElementCategoryOptionCombo attributeOptionCombo = categoryService.getDataElementCategoryOptionCombo( cp );

        if ( attributeOptionCombo == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Category option combo does not exist: " + cp ) );
        }

        Period period = PeriodType.getPeriodFromIsoString( pe );

        if ( period == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Period does not exist: " + pe ) );
        }

        OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( ou );

        if ( organisationUnit == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Organisation unit does not exist: " + ou ) );
        }

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_PNG, CacheStrategy.RESPECT_SYSTEM_SETTING, "chart.png", false );

        JFreeChart chart = chartService.getJFreeChartHistory( dataElement, categoryOptionCombo, attributeOptionCombo, period, organisationUnit, 13, i18nManager.getI18nFormat() );

        ChartUtilities.writeChartAsPNG( response.getOutputStream(), chart, width, height );
    }

    //--------------------------------------------------------------------------
    // Hooks
    //--------------------------------------------------------------------------

    @Override
    public void postProcessEntity( Chart chart ) throws Exception
    {
        chart.populateAnalyticalProperties();

        Set<OrganisationUnit> roots = currentUserService.getCurrentUser().getDataViewOrganisationUnitsWithFallback();

        for ( OrganisationUnit organisationUnit : chart.getOrganisationUnits() )
        {
            chart.getParentGraphMap().put( organisationUnit.getUid(), organisationUnit.getParentGraph( roots ) );
        }

        if ( chart.getPeriods() != null && !chart.getPeriods().isEmpty() )
        {
            I18nFormat format = i18nManager.getI18nFormat();

            for ( Period period : chart.getPeriods() )
            {
                period.setName( format.formatPeriod( period ) );
            }
        }
    }

    //--------------------------------------------------------------------------
    // Supportive methods
    //--------------------------------------------------------------------------

    private void mergeChart( Chart chart )
    {
        dimensionService.mergeAnalyticalObject( chart );

        chart.getFilterDimensions().clear();
        chart.getFilterDimensions().addAll( getDimensions( chart.getFilters() ) );

        if ( chart.getColumns() != null )
        {
            chart.setSeries( chart.getColumns().get( 0 ).getDimension() );
        }

        if ( chart.getRows() != null )
        {
            chart.setCategory( chart.getRows().get( 0 ).getDimension() );
        }
    }
}
