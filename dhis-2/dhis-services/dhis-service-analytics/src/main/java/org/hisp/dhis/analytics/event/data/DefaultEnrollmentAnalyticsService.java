package org.hisp.dhis.analytics.event.data;

/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Date;

import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.event.*;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.util.Timer;
import org.springframework.stereotype.Service;

/**
 * @author Markus Bekken
 */
@Service( "org.hisp.dhis.analytics.event.EnrollmentAnalyticsService" )
public class DefaultEnrollmentAnalyticsService
    extends
    AbstractAnalyticsService
    implements
    EnrollmentAnalyticsService
{
    private static final String NAME_TEI = "Tracked entity instance";
    private static final String NAME_PI  = "Enrollment";
    private static final String NAME_GEOMETRY = "Geometry";
    private static final String NAME_ENROLLMENT_DATE = "Enrollment date";
    private static final String NAME_INCIDENT_DATE = "Incident date";
    private static final String NAME_LONGITUDE = "Longitude";
    private static final String NAME_LATITUDE = "Latitude";
    private static final String NAME_ORG_UNIT_NAME = "Organisation unit name";
    private static final String NAME_ORG_UNIT_CODE = "Organisation unit code";

    private final EnrollmentAnalyticsManager enrollmentAnalyticsManager;

    private final EventQueryPlanner queryPlanner;

    public DefaultEnrollmentAnalyticsService( EnrollmentAnalyticsManager enrollmentAnalyticsManager,
        AnalyticsSecurityManager securityManager, EventQueryPlanner queryPlanner, EventQueryValidator queryValidator )
    {
        super( securityManager, queryValidator );

        checkNotNull( enrollmentAnalyticsManager );
        checkNotNull( queryPlanner );

        this.enrollmentAnalyticsManager = enrollmentAnalyticsManager;
        this.queryPlanner = queryPlanner;
    }

    // -------------------------------------------------------------------------
    // EventAnalyticsService implementation
    // -------------------------------------------------------------------------

    @Override
    public Grid getEnrollments( EventQueryParams params )
    {
        return getGrid(params);
    }

    @Override
    protected Grid createGridWithHeaders( EventQueryParams params )
    {
        Grid grid = new ListGrid();
     
        grid.addHeader( new GridHeader( ITEM_PI, NAME_PI, ValueType.TEXT, String.class.getName(), false, true ) )
            .addHeader( new GridHeader( ITEM_TEI, NAME_TEI, ValueType.TEXT, String.class.getName(), false, true ) )
            .addHeader( new GridHeader( ITEM_ENROLLMENT_DATE, NAME_ENROLLMENT_DATE, ValueType.DATE, Date.class.getName(), false, true ) )
            .addHeader( new GridHeader( ITEM_INCIDENT_DATE, NAME_INCIDENT_DATE, ValueType.DATE, Date.class.getName(), false, true ) )
            .addHeader( new GridHeader( ITEM_GEOMETRY, NAME_GEOMETRY, ValueType.TEXT, String.class.getName(), false, true ) )
            .addHeader( new GridHeader( ITEM_LONGITUDE, NAME_LONGITUDE, ValueType.NUMBER, Double.class.getName(), false, true ) )
            .addHeader( new GridHeader( ITEM_LATITUDE, NAME_LATITUDE, ValueType.NUMBER, Double.class.getName(), false, true ) )
            .addHeader( new GridHeader( ITEM_ORG_UNIT_NAME, NAME_ORG_UNIT_NAME, ValueType.TEXT, String.class.getName(), false, true ) )
            .addHeader( new GridHeader( ITEM_ORG_UNIT_CODE, NAME_ORG_UNIT_CODE, ValueType.TEXT, String.class.getName(), false, true ) );
        return grid;
    }

    @Override
    protected long addEventData(Grid grid, EventQueryParams params )
    {
        Timer timer = new Timer().start().disablePrint();

        params = queryPlanner.planEnrollmentQuery( params );

        timer.getSplitTime( "Planned event query, got partitions: " + params.getPartitions() );

        long count = 0;

        if ( params.isPaging() )
        {
            count += enrollmentAnalyticsManager.getEnrollmentCount( params );
        }

        enrollmentAnalyticsManager.getEnrollments( params, grid, queryValidator.getMaxLimit() );

        timer.getTime( "Got enrollments " + grid.getHeight() );

        return count;
    }
}
