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
package org.hisp.dhis.webapi.controller.datastatistics;

import static java.util.Calendar.DATE;
import static java.util.Calendar.MILLISECOND;
import static org.hisp.dhis.datastatistics.DataStatisticsEventType.EVENT_CHART_VIEW;
import static org.hisp.dhis.datastatistics.DataStatisticsEventType.EVENT_REPORT_VIEW;
import static org.hisp.dhis.datastatistics.DataStatisticsEventType.EVENT_VISUALIZATION_VIEW;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.analytics.SortOrder;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.datastatistics.AggregatedStatistics;
import org.hisp.dhis.datastatistics.DataStatisticsEvent;
import org.hisp.dhis.datastatistics.DataStatisticsEventType;
import org.hisp.dhis.datastatistics.DataStatisticsService;
import org.hisp.dhis.datastatistics.EventInterval;
import org.hisp.dhis.datastatistics.FavoriteStatistics;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Yrjan A. F. Fraschetti
 * @author Julie Hill Roa
 */
@Controller
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@RequestMapping( DataStatisticsController.RESOURCE_PATH )
public class DataStatisticsController
{
    public static final String RESOURCE_PATH = "/dataStatistics";

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private DataStatisticsService dataStatisticsService;

    @PostMapping
    @ResponseStatus( HttpStatus.CREATED )
    public void saveEvent( @RequestParam DataStatisticsEventType eventType, String favorite )
    {
        Date timestamp = new Date();
        String username = currentUserService.getCurrentUsername();

        DataStatisticsEvent event = new DataStatisticsEvent( eventType, timestamp, username, favorite );
        dataStatisticsService.addEvent( event );

        addStatisticsForEventChartOrReport( eventType, favorite, timestamp, username );
    }

    /**
     * @deprecated Logic needed to assist the deprecation process of event chart
     *             and event report.
     *
     * @param eventType
     * @param favorite
     * @param timestamp
     * @param username
     */
    @Deprecated
    private void addStatisticsForEventChartOrReport( final DataStatisticsEventType eventType, final String favorite,
        final Date timestamp, final String username )
    {
        if ( eventType == EVENT_CHART_VIEW || eventType == EVENT_REPORT_VIEW )
        {
            // For each EVENT_CHART_VIEW or EVENT_REPORT_VIEW we also add a
            // EVENT_VISUALIZATION_VIEW event.
            dataStatisticsService
                .addEvent( new DataStatisticsEvent( EVENT_VISUALIZATION_VIEW, timestamp, username, favorite ) );
        }
    }

    @GetMapping
    public @ResponseBody List<AggregatedStatistics> getReports( @RequestParam Date startDate,
        @RequestParam Date endDate, @RequestParam EventInterval interval, HttpServletResponse response )
        throws WebMessageException
    {
        if ( startDate.after( endDate ) )
        {
            throw new WebMessageException( conflict( "Start date is after end date" ) );
        }

        // The endDate is arriving as: "2019-09-28". After the conversion below
        // it will become: "2019-09-28 23:59:59.999"
        endDate = DateUtils.calculateDateFrom( endDate, 1, DATE );
        endDate = DateUtils.calculateDateFrom( endDate, -1, MILLISECOND );

        setNoStore( response );
        return dataStatisticsService.getReports( startDate, endDate, interval );
    }

    @GetMapping( "/favorites" )
    public @ResponseBody List<FavoriteStatistics> getTopFavorites( @RequestParam DataStatisticsEventType eventType,
        @RequestParam( required = false ) Integer pageSize, @RequestParam( required = false ) SortOrder sortOrder,
        @RequestParam( required = false ) String username, HttpServletResponse response )
        throws WebMessageException
    {
        pageSize = ObjectUtils.firstNonNull( pageSize, 20 );
        sortOrder = ObjectUtils.firstNonNull( sortOrder, SortOrder.DESC );

        setNoStore( response );
        return dataStatisticsService.getTopFavorites( eventType, pageSize, sortOrder, username );
    }

    @GetMapping( "/favorites/{uid}" )
    public @ResponseBody FavoriteStatistics getFavoriteStatistics( @PathVariable( "uid" ) String uid )
    {
        return dataStatisticsService.getFavoriteStatistics( uid );
    }

    @PreAuthorize( "hasRole('ALL')" )
    @ResponseStatus( HttpStatus.CREATED )
    @PostMapping( "/snapshot" )
    public void saveSnapshot()
    {
        dataStatisticsService.saveDataStatisticsSnapshot( NoopJobProgress.INSTANCE );
    }
}
