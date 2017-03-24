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
 */

import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.legend.LegendSetService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.reporttable.ReportTableService;
import org.hisp.dhis.schema.descriptors.ReportTableSchemaDescriptor;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.Set;

import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensions;
import static org.hisp.dhis.system.util.CodecUtils.filenameEncode;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = ReportTableSchemaDescriptor.API_ENDPOINT )
public class ReportTableController
    extends AbstractCrudController<ReportTable>
{
    @Autowired
    public ReportTableService reportTableService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DimensionService dimensionService;

    @Autowired
    private LegendSetService legendSetService;

    @Autowired
    private I18nManager i18nManager;

    @Autowired
    private ContextUtils contextUtils;

    //--------------------------------------------------------------------------
    // CRUD
    //--------------------------------------------------------------------------

    @Override
    protected ReportTable deserializeJsonEntity( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        ReportTable reportTable = super.deserializeJsonEntity( request, response );
        mergeReportTable( reportTable );

        return reportTable;
    }

    //--------------------------------------------------------------------------
    // GET - Report table data
    //--------------------------------------------------------------------------

    @RequestMapping( value = "/{uid}/data", method = RequestMethod.GET ) // For json, jsonp
    public @ResponseBody Grid getReportTableData( @PathVariable( "uid" ) String uid, Model model,
        @RequestParam( value = "ou", required = false ) String organisationUnitUid,
        @RequestParam( value = "date", required = false ) Date date,
        HttpServletResponse response ) throws Exception
    {
        return getReportTableGrid( uid, organisationUnitUid, date );
    }

    @RequestMapping( value = "/{uid}/data.html", method = RequestMethod.GET )
    public void getReportTableHtml( @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String organisationUnitUid,
        @RequestParam( value = "date", required = false ) Date date,
        HttpServletResponse response ) throws Exception
    {
        Grid grid = getReportTableGrid( uid, organisationUnitUid, date );

        String filename = filenameEncode( grid.getTitle() ) + ".html";
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_HTML, CacheStrategy.RESPECT_SYSTEM_SETTING, filename, false );

        GridUtils.toHtml( grid, response.getWriter() );
    }

    @RequestMapping( value = "/{uid}/data.html+css", method = RequestMethod.GET )
    public void getReportTableHtmlCss( @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String organisationUnitUid,
        @RequestParam( value = "date", required = false ) Date date,
        HttpServletResponse response ) throws Exception
    {
        Grid grid = getReportTableGrid( uid, organisationUnitUid, date );

        String filename = filenameEncode( grid.getTitle() ) + ".html";
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_HTML, CacheStrategy.RESPECT_SYSTEM_SETTING, filename, false );

        GridUtils.toHtmlCss( grid, response.getWriter() );
    }

    @RequestMapping( value = "/{uid}/data.xml", method = RequestMethod.GET )
    public void getReportTableXml( @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String organisationUnitUid,
        @RequestParam( value = "date", required = false ) Date date,
        HttpServletResponse response ) throws Exception
    {
        Grid grid = getReportTableGrid( uid, organisationUnitUid, date );

        String filename = filenameEncode( grid.getTitle() ) + ".xml";
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_XML, CacheStrategy.RESPECT_SYSTEM_SETTING, filename, false );

        GridUtils.toXml( grid, response.getOutputStream() );
    }

    @RequestMapping( value = "/{uid}/data.pdf", method = RequestMethod.GET )
    public void getReportTablePdf( @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String organisationUnitUid,
        @RequestParam( value = "date", required = false ) Date date,
        HttpServletResponse response ) throws Exception
    {
        Grid grid = getReportTableGrid( uid, organisationUnitUid, date );

        String filename = filenameEncode( grid.getTitle() ) + ".pdf";
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_PDF, CacheStrategy.RESPECT_SYSTEM_SETTING, filename, false );

        GridUtils.toPdf( grid, response.getOutputStream() );
    }

    @RequestMapping( value = "/{uid}/data.xls", method = RequestMethod.GET )
    public void getReportTableXls( @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String organisationUnitUid,
        @RequestParam( value = "date", required = false ) Date date,
        HttpServletResponse response ) throws Exception
    {
        Grid grid = getReportTableGrid( uid, organisationUnitUid, date );

        String filename = filenameEncode( grid.getTitle() ) + ".xls";
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_EXCEL, CacheStrategy.RESPECT_SYSTEM_SETTING, filename, true );

        GridUtils.toXls( grid, response.getOutputStream() );
    }

    @RequestMapping( value = "/{uid}/data.csv", method = RequestMethod.GET )
    public void getReportTableCsv( @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String organisationUnitUid,
        @RequestParam( value = "date", required = false ) Date date,
        HttpServletResponse response ) throws Exception
    {
        Grid grid = getReportTableGrid( uid, organisationUnitUid, date );

        String filename = filenameEncode( grid.getTitle() ) + ".csv";
        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_CSV, CacheStrategy.RESPECT_SYSTEM_SETTING, filename, true );

        GridUtils.toCsv( grid, response.getWriter() );
    }

    private Grid getReportTableGrid( String uid, String organisationUnitUid, Date date )
        throws Exception
    {
        ReportTable reportTable = reportTableService.getReportTableNoAcl( uid );

        if ( organisationUnitUid == null && reportTable.hasReportParams() && reportTable.getReportParams().isOrganisationUnitSet() )
        {
            organisationUnitUid = organisationUnitService.getRootOrganisationUnits().iterator().next().getUid();
        }

        date = date != null ? date : new Date();

        return reportTableService.getReportTableGrid( uid, date, organisationUnitUid );
    }

    //--------------------------------------------------------------------------
    // Hooks
    //--------------------------------------------------------------------------

    @Override
    protected void postProcessEntity( ReportTable reportTable ) throws Exception
    {
        reportTable.populateAnalyticalProperties();

        Set<OrganisationUnit> roots = currentUserService.getCurrentUser().getDataViewOrganisationUnitsWithFallback();

        for ( OrganisationUnit organisationUnit : reportTable.getOrganisationUnits() )
        {
            reportTable.getParentGraphMap().put( organisationUnit.getUid(), organisationUnit.getParentGraph( roots ) );
        }

        I18nFormat format = i18nManager.getI18nFormat();

        if ( reportTable.getPeriods() != null && !reportTable.getPeriods().isEmpty() )
        {
            for ( Period period : reportTable.getPeriods() )
            {
                period.setName( format.formatPeriod( period ) );
            }
        }
    }

    //--------------------------------------------------------------------------
    // Supportive methods
    //--------------------------------------------------------------------------

    private void mergeReportTable( ReportTable reportTable )
    {
        dimensionService.mergeAnalyticalObject( reportTable );

        reportTable.getColumnDimensions().clear();
        reportTable.getRowDimensions().clear();
        reportTable.getFilterDimensions().clear();

        reportTable.getColumnDimensions().addAll( getDimensions( reportTable.getColumns() ) );
        reportTable.getRowDimensions().addAll( getDimensions( reportTable.getRows() ) );
        reportTable.getFilterDimensions().addAll( getDimensions( reportTable.getFilters() ) );

        if ( reportTable.getLegendSet() != null )
        {
            reportTable.setLegendSet( legendSetService.getLegendSet( reportTable.getLegendSet().getUid() ) );
        }
    }
}
