package org.hisp.dhis.reporting.reportviewer.action;

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

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.commons.action.AbstractRelativePeriodsAction;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.report.Report;
import org.hisp.dhis.report.ReportService;
import org.hisp.dhis.report.ReportType;
import org.hisp.dhis.reporttable.ReportParams;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.reporttable.ReportTableService;

/**
 * @author Lars Helge Overland
 */
public class AddReportAction
    extends AbstractRelativePeriodsAction
{
    private static final Log log = LogFactory.getLog( AddReportAction.class );
    
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ReportService reportService;

    public void setReportService( ReportService reportService )
    {
        this.reportService = reportService;
    }

    private ReportTableService reportTableService;

    public void setReportTableService( ReportTableService reportTableService )
    {
        this.reportTableService = reportTableService;
    }
    
    // -----------------------------------------------------------------------
    // I18n
    // -----------------------------------------------------------------------

    protected I18n i18n;
    
    public void setI18n( I18n i18n )
    {
        this.i18n = i18n;
    }
    
    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private Integer id;

    public void setId( Integer id )
    {
        this.id = id;
    }
    
    private String name;

    public void setName( String name )
    {
        this.name = name;
    }
    
    private ReportType type;

    public void setType( ReportType type )
    {
        this.type = type;
    }

    private Integer reportTableId;
    
    public void setReportTableId( Integer reportTableId )
    {
        this.reportTableId = reportTableId;
    }

    private File file;

    public void setUpload( File file )
    {
        this.file = file;
    }
    
    private String fileName;
    
    public void setUploadFileName( String fileName )
    {
        this.fileName = fileName;
    }
    
    private String contentType;
    
    public void setUploadContentType( String contentType )
    {
        this.contentType = contentType;
    }
    
    private String currentDesign;

    public void setCurrentDesign( String currentDesign )
    {
        this.currentDesign = currentDesign;
    }

    private boolean paramReportingMonth;

    public void setParamReportingMonth( boolean paramReportingMonth )
    {
        this.paramReportingMonth = paramReportingMonth;
    }

    private boolean paramOrganisationUnit;

    public void setParamOrganisationUnit( boolean paramOrganisationUnit )
    {
        this.paramOrganisationUnit = paramOrganisationUnit;
    }

    private String cacheStrategy;

    public void setCacheStrategy( String cacheStrategy )
    {
        this.cacheStrategy = cacheStrategy;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private String message;
    
    public String getMessage()
    {
        return message;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        // ---------------------------------------------------------------------
        // New report or update existing object?
        // ---------------------------------------------------------------------

        boolean isNewReport = id == null;

        // ---------------------------------------------------------------------
        // Set fileName to the current design file name in case of update
        // ---------------------------------------------------------------------

        if ( fileName == null && currentDesign != null )
        {
            fileName = currentDesign;
        }
        
        // ---------------------------------------------------------------------
        // Validation
        // ---------------------------------------------------------------------
        
        if ( id == null && ( fileName == null || fileName.trim().length() == 0 ) )
        {
            return ERROR;
        }

        // ---------------------------------------------------------------------
        // Create report
        // ---------------------------------------------------------------------

        Report report = isNewReport ? new Report() : reportService.getReport( id );
        
        ReportTable reportTable = reportTableService.getReportTable( reportTableId );
        
        ReportParams reportParams = new ReportParams( paramReportingMonth, false, false, paramOrganisationUnit );
        
        report.setName( name );
        report.setType( type );
        report.setReportTable( reportTable );
        report.setRelatives( getRelativePeriods() );
        report.setReportParams( reportParams );
        
        log.info( "Upload file name: " + fileName + ", content type: " + contentType );

        // ---------------------------------------------------------------------
        // Design file upload
        // ---------------------------------------------------------------------

        if ( file != null )
        {
            report.setDesignContent( FileUtils.readFileToString( file ) );
        }

        // ---------------------------------------------------------------------
        // Cache strategy
        // ---------------------------------------------------------------------

        if ( cacheStrategy != null )
        {
            CacheStrategy strategy = EnumUtils.getEnum( CacheStrategy.class, cacheStrategy );
            report.setCacheStrategy( strategy != null ? strategy : Report.DEFAULT_CACHE_STRATEGY );
        }
        else if ( isNewReport )
        {
            report.setCacheStrategy( CacheStrategy.RESPECT_SYSTEM_SETTING );
        }
        
        reportService.saveReport( report );
        
        return SUCCESS;
    }
}
