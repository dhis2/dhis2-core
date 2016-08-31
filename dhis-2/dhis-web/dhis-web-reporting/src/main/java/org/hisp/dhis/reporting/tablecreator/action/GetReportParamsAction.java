package org.hisp.dhis.reporting.tablecreator.action;

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

import com.opensymphony.xwork2.Action;

import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.DateTimeUnit;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.period.CalendarPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.RelativePeriods;
import org.hisp.dhis.report.Report;
import org.hisp.dhis.report.ReportService;
import org.hisp.dhis.report.ReportType;
import org.hisp.dhis.reporttable.ReportParams;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.reporttable.ReportTableService;
import org.hisp.dhis.system.filter.PastAndCurrentPeriodFilter;
import org.hisp.dhis.commons.filter.FilterUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hisp.dhis.reporttable.ReportTableService.MODE_REPORT;
import static org.hisp.dhis.reporttable.ReportTableService.MODE_REPORT_TABLE;

/**
 * @author Lars Helge Overland
 */
public class GetReportParamsAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ReportTableService reportTableService;

    public void setReportTableService( ReportTableService reportTableService )
    {
        this.reportTableService = reportTableService;
    }

    protected ReportService reportService;

    public void setReportService( ReportService reportService )
    {
        this.reportService = reportService;
    }

    private I18nFormat format;

    public void setFormat( I18nFormat format )
    {
        this.format = format;
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private String uid;

    public String getUid()
    {
        return uid;
    }

    public void setUid( String uid )
    {
        this.uid = uid;
    }

    private String mode;

    public String getMode()
    {
        return mode;
    }

    public void setMode( String mode )
    {
        this.mode = mode;
    }

    private ReportType type;

    public ReportType getType()
    {
        return type;
    }

    public void setType( ReportType type )
    {
        this.type = type;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private ReportParams reportParams;

    public ReportParams getReportParams()
    {
        return reportParams;
    }

    private List<IdentifiableObject> periods = new ArrayList<>();

    public List<IdentifiableObject> getPeriods()
    {
        return periods;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        if ( mode == null || uid == null )
        {
            return SUCCESS;
        }

        RelativePeriods relatives = null;

        if ( MODE_REPORT_TABLE.equals( mode ) )
        {
            ReportTable reportTable = reportTableService.getReportTable( uid );

            if ( reportTable != null )
            {
                reportParams = reportTable.getReportParams();
                relatives = reportTable.getRelatives();
            }
        }
        else if ( MODE_REPORT.equals( mode ) )
        {
            Report report = reportService.getReport( uid );

            if ( report != null && report.isTypeReportTable() )
            {
                reportParams = report.getReportTable().getReportParams();
                relatives = report.getReportTable().getRelatives();
            }
            else if ( report != null && (report.isTypeJdbc() || report.isTypeHtml()) )
            {
                reportParams = report.getReportParams();
                relatives = report.getRelatives();
            }

            if ( type == null && report != null )
            {
                type = report.getType(); // Set type based on report
            }
        }

        if ( reportParams != null && reportParams.isParamReportingMonth() && relatives != null )
        {
            CalendarPeriodType periodType = (CalendarPeriodType) relatives.getPeriodType();
            List<Period> periods = periodType.generateLast5Years( new Date() );
            Collections.reverse( periods );
            FilterUtils.filter( periods, new PastAndCurrentPeriodFilter() );

            Calendar calendar = PeriodType.getCalendar();

            for ( Period period_ : periods )
            {
                BaseIdentifiableObject period = new BaseIdentifiableObject();

                if ( calendar.isIso8601() )
                {
                    period.setUid( period_.getIsoDate() );
                    period.setDisplayName( format.formatPeriod( period_ ) );
                }
                else
                {
                    DateTimeUnit dateTimeUnit = calendar.fromIso( period_.getStartDate() );
                    period.setUid( period_.getPeriodType().getIsoDate( dateTimeUnit ) );
                    period.setDisplayName( format.formatPeriod( period_ ) );
                }

                this.periods.add( period );
            }
        }

        return SUCCESS;
    }
}
