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

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.j2ee.servlets.BaseHttpServlet;
import net.sf.jasperreports.j2ee.servlets.ImageServlet;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.report.Report;
import org.hisp.dhis.report.ReportService;
import org.hisp.dhis.schema.descriptors.ReportSchemaDescriptor;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

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
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ContextUtils contextUtils;

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
