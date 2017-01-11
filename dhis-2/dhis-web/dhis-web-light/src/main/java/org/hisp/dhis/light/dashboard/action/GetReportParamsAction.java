package org.hisp.dhis.light.dashboard.action;

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

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.oust.manager.SelectionTreeManager;
import org.hisp.dhis.period.CalendarPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.reporttable.ReportParams;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.reporttable.ReportTableService;
import org.hisp.dhis.system.filter.PastAndCurrentPeriodFilter;
import org.hisp.dhis.commons.filter.FilterUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.Action;

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
    
    private SelectionTreeManager selectionTreeManager;

    public void setSelectionTreeManager( SelectionTreeManager selectionTreeManager )
    {
        this.selectionTreeManager = selectionTreeManager;
    }

    @Autowired
    private CurrentUserService currentUserService;
    
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

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private ReportParams reportParams;

    public ReportParams getReportParams()
    {
        return reportParams;
    }

    private List<Period> periods;

    public List<Period> getPeriods()
    {
        return periods;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        User user = currentUserService.getCurrentUser();
        
        selectionTreeManager.setSelectedOrganisationUnit( user.getOrganisationUnit() );
        
        if ( uid != null )
        {
            ReportTable reportTable = reportTableService.getReportTable( uid, ReportTableService.MODE_REPORT_TABLE );

            if ( reportTable != null )
            {
                reportParams = reportTable.getReportParams();
                                
                if ( reportParams.isParamReportingMonth() && reportTable.getRelatives() != null )
                {
                    CalendarPeriodType periodType = (CalendarPeriodType) reportTable.getRelatives().getPeriodType();
                    periods = periodType.generateLast5Years( new Date() );
                    Collections.reverse( periods );
                    FilterUtils.filter( periods, new PastAndCurrentPeriodFilter() );
                    
                    for ( Period period : periods )
                    {
                        period.setName( format.formatPeriod( period ) );
                    }
                }
            }
        }
        
        return SUCCESS;
    }
}
