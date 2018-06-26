package org.hisp.dhis.webapi.controller;

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

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.j2ee.servlets.BaseHttpServlet;
import net.sf.jasperreports.j2ee.servlets.ImageServlet;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.ServiceProvider;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.completeness.DataSetCompletenessResult;
import org.hisp.dhis.completeness.DataSetCompletenessService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.report.Report;
import org.hisp.dhis.report.ReportService;
import org.hisp.dhis.schema.descriptors.ReportSchemaDescriptor;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.system.util.CodecUtils;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;
import static org.hisp.dhis.system.util.CodecUtils.filenameEncode;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = ReportSchemaDescriptor.API_ENDPOINT )
public class ReportController
    extends AbstractCrudController<Report>
{
    @Autowired
    public ReportService reportService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private OrganisationUnitGroupService organisationUnitGroupService;

    @Autowired
    private I18nManager i18nManager;

    @Autowired
    private ContextUtils contextUtils;

    @Resource(name="dataCompletenessServiceProvider")
    private ServiceProvider<DataSetCompletenessService> serviceProvider;

    // -------------------------------------------------------------------------
    // CRUD
    // -------------------------------------------------------------------------

    @RequestMapping( value = "/{uid}/design", method = RequestMethod.PUT )
    @PreAuthorize( "hasRole('ALL')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void updateReportDesign( @PathVariable( "uid" ) String uid,
        @RequestBody String designContent,
        HttpServletResponse response ) throws Exception
    {
        Report report = reportService.getReport( uid );

        if ( report == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Report not found for identifier: " + uid ) );
        }

        report.setDesignContent( designContent );
        reportService.saveReport( report );
    }

    @RequestMapping( value = "/{uid}/design", method = RequestMethod.GET )
    public void getReportDesign( @PathVariable( "uid" ) String uid, HttpServletResponse response ) throws Exception
    {
        Report report = reportService.getReport( uid );

        if ( report == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Report not found for identifier: " + uid ) );
        }

        if ( report.getDesignContent() == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Report has no design content: " + uid ) );
        }

        if ( report.isTypeHtml() )
        {
            contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_HTML, CacheStrategy.NO_CACHE, filenameEncode( report.getName() ) + ".html", true );
        }
        else
        {
            contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_XML, CacheStrategy.NO_CACHE, filenameEncode( report.getName() ) + ".jrxml", true );
        }

        response.getWriter().write( report.getDesignContent() );
    }

    // -------------------------------------------------------------------------
    // Get data
    // -------------------------------------------------------------------------

    @RequestMapping( value = { "/{uid}/data", "/{uid}/data.pdf" }, method = RequestMethod.GET )
    public void getReportAsPdf( @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String organisationUnitUid,
        @RequestParam( value = "pe", required = false ) String period,
        @RequestParam( value = "date", required = false ) Date date,
        HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        getReport( request, response, uid, organisationUnitUid, period, date, "pdf", ContextUtils.CONTENT_TYPE_PDF, false );
    }

    @RequestMapping( value = "/{uid}/data.xls", method = RequestMethod.GET )
    public void getReportAsXls( @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String organisationUnitUid,
        @RequestParam( value = "pe", required = false ) String period,
        @RequestParam( value = "date", required = false ) Date date,
        HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        getReport( request, response, uid, organisationUnitUid, period, date, "xls", ContextUtils.CONTENT_TYPE_EXCEL, true );
    }

    @RequestMapping( value = "/{uid}/data.html", method = RequestMethod.GET )
    public void getReportAsHtml( @PathVariable( "uid" ) String uid,
        @RequestParam( value = "ou", required = false ) String organisationUnitUid,
        @RequestParam( value = "pe", required = false ) String period,
        @RequestParam( value = "date", required = false ) Date date,
        HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        getReport( request, response, uid, organisationUnitUid, period, date, "html", ContextUtils.CONTENT_TYPE_HTML, false );
    }

    // -------------------------------------------------------------------------
    // Images
    // -------------------------------------------------------------------------

    /**
     * This methods wraps the Jasper image servlet to avoid having a separate
     * servlet mapping around. Note that the path to images are relative to the
     * reports path in this controller.
     */
    @RequestMapping( value = "/jasperReports/img", method = RequestMethod.GET )
    public void getJasperImage( @RequestParam String image,
        HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        new ImageServlet().service( request, response );
    }

    @RequestMapping( value = "/rateSummary", method = RequestMethod.GET )
    public void getDataSetReport(
        @RequestParam( required = false ) String ds,
        @RequestParam String pe,
        @RequestParam String ou,
        @RequestParam String criteria,
        @RequestParam( required = false ) Set<String> groupUids,
        HttpServletResponse response) throws IOException, WebMessageException
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

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_HTML, CacheStrategy.RESPECT_SYSTEM_SETTING );

        // ---------------------------------------------------------------------
        // Assemble report
        // ---------------------------------------------------------------------
        List<DataSetCompletenessResult> mainResults = new ArrayList<>();
        List<DataSetCompletenessResult> footerResults = new ArrayList<>();
        DataSetCompletenessService completenessService = serviceProvider.provide( criteria );

        Set<Integer> groupIds = new HashSet<>(  );
        if ( groupUids != null ) {
            for( String groupUid : groupUids ) {
                OrganisationUnitGroup organisationUnitGroup = organisationUnitGroupService.getOrganisationUnitGroup( groupUid );
                if ( organisationUnitGroup != null ) {
                    groupIds.add( organisationUnitGroup.getId() );
                    System.out.println( "GroupId: " +  organisationUnitGroup.getId());
                }
            }
        }

        if ( selectedDataSet != null ) {
            mainResults = new ArrayList<>( completenessService.getDataSetCompleteness(
                selectedPeriod.getId(), getIdentifiers( selectedOrgunit.getChildren() ), selectedDataSet.getId(), groupIds ) );

            footerResults = new ArrayList<>(
                completenessService.getDataSetCompleteness( selectedPeriod.getId(), Arrays.asList( selectedOrgunit.getId() ),
                    selectedDataSet.getId(), groupIds ) );
        } else {
            mainResults = new ArrayList<>( completenessService.getDataSetCompleteness(
                selectedPeriod.getId(), selectedOrgunit.getId(), groupIds ) );
        }

        // ---------------------------------------------------------------------
        // Write response
        // ---------------------------------------------------------------------

        // FIXME move it a util method
        I18n i18n = i18nManager.getI18n();
        String title =
            ( selectedOrgunit != null ? selectedOrgunit.getName() : "" ) +
                ( selectedDataSet != null ? "-" + selectedDataSet.getName() : "" ) +
                ( selectedPeriod != null ? "-" + i18nManager.getI18nFormat().formatPeriod( selectedPeriod ) : "" );

        Grid grid = new ListGrid().setTitle( title );

        grid.addHeader( new GridHeader( i18n.getString( "name" ), false, true ) );
        grid.addHeader( new GridHeader( i18n.getString( "actual_reports" ), false, false ) );
        grid.addHeader( new GridHeader( i18n.getString( "expected_reports" ), false, false ) );
        grid.addHeader( new GridHeader( i18n.getString( "percent" ), false, false ) );
        grid.addHeader( new GridHeader( i18n.getString( "reports_on_time" ), false, false ) );
        grid.addHeader( new GridHeader( i18n.getString( "percent_on_time" ), false, false ) );

        for ( DataSetCompletenessResult result : mainResults )
        {
            // FIXME move it a util method
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
            // FIXME move it a util method
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

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void getReport( HttpServletRequest request, HttpServletResponse response, String uid, String organisationUnitUid, String isoPeriod,
        Date date, String type, String contentType, boolean attachment ) throws Exception
    {
        Report report = reportService.getReport( uid );

        if ( report == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Report not found for identifier: " + uid ) );
        }

        if ( organisationUnitUid == null && report.hasReportTable() && report.getReportTable().hasReportParams()
            && report.getReportTable().getReportParams().isOrganisationUnitSet() )
        {
            organisationUnitUid = organisationUnitService.getRootOrganisationUnits().iterator().next().getUid();
        }

        if ( report.isTypeHtml() )
        {
            contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_HTML, report.getCacheStrategy() );

            reportService.renderHtmlReport( response.getWriter(), uid, date, organisationUnitUid );
        }
        else
        {
            date = date != null ? date : new DateTime().minusMonths( 1 ).toDate();

            Period period = isoPeriod != null ? PeriodType.getPeriodFromIsoString( isoPeriod ) : new MonthlyPeriodType().createPeriod( date );

            String filename = CodecUtils.filenameEncode( report.getName() ) + "." + type;

            contextUtils.configureResponse( response, contentType, report.getCacheStrategy(), filename, attachment );

            JasperPrint print = reportService.renderReport( response.getOutputStream(), uid, period, organisationUnitUid, type );

            if ( "html".equals( type ) )
            {
                request.getSession().setAttribute( BaseHttpServlet.DEFAULT_JASPER_PRINT_SESSION_ATTRIBUTE, print );
            }
        }
    }
}
