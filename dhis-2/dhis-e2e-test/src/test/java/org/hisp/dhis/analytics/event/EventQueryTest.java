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
package org.hisp.dhis.analytics.event;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.helpers.PeriodHelper.getRelativePeriodDate;

import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.actions.analytics.AnalyticsEventActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.PeriodHelper.Period;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Groups e2e tests for Events "/query" endpoint.
 *
 * @author maikel arabori
 */
public class EventQueryTest extends AnalyticsApiTest
{
    private AnalyticsEventActions analyticsEventActions = new AnalyticsEventActions();

    @Test
    public void queryWithProgramAndProgramStageWhenTotalPagesIsFalse()
    {
        // Given
        final QueryParamsBuilder params = new QueryParamsBuilder();
        params.add( "dimension=pe:LAST_12_MONTHS,ou:ImspTQPwCqd" )
            .add( "stage=dBwrot7S420" )
            .add( "displayProperty=NAME" )
            .add( "totalPages=false" )
            .add( "outputType=EVENT" );

        // When
        final ApiResponse response = analyticsEventActions.query().get( "lxAQ7Zs9VYR", JSON, JSON, params );

        // Then
        response.validate()
            .statusCode( 200 )
            .body( "headers", hasSize( equalTo( 16 ) ) )
            .body( "rows", hasSize( equalTo( 3 ) ) )
            .body( "metaData.pager.page", equalTo( 1 ) )
            .body( "metaData.pager.pageSize", equalTo( 50 ) )
            .body( "metaData.pager.isLastPage", is( true ) )
            .body( "metaData.pager", not( hasKey( "total" ) ) )
            .body( "metaData.pager", not( hasKey( "pageCount" ) ) )
            .body( "metaData.items.ImspTQPwCqd.name", equalTo( "Sierra Leone" ) )
            .body( "metaData.items.dBwrot7S420.name", equalTo( "Antenatal care visit" ) )
            .body( "metaData.items.ou.name", equalTo( "Organisation unit" ) )
            .body( "metaData.items.lxAQ7Zs9VYR.name", equalTo( "Antenatal care visit" ) )
            .body( "metaData.items.LAST_12_MONTHS.name", equalTo( "Last 12 months" ) )
            .body( "metaData.dimensions.pe", hasSize( equalTo( 0 ) ) )
            .body( "metaData.dimensions.ou", hasSize( equalTo( 1 ) ) )
            .body( "metaData.dimensions.ou", hasItem( "ImspTQPwCqd" ) )
            .body( "height", equalTo( 3 ) )
            .body( "width", equalTo( 16 ) )
            .body( "headerWidth", equalTo( 16 ) );

        // Validate headers
        validateHeader( response, 0, "psi", "Event", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 1, "ps", "Program stage", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 2, "eventdate", "Visit date", "DATE", "java.time.LocalDate", false, true );
        validateHeader( response, 3, "storedby", "Stored by", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 4, "createdbydisplayname", "Created by", "TEXT", "java.lang.String",
            false, true );
        validateHeader( response, 5, "lastupdatedbydisplayname", "Last updated by", "TEXT",
            "java.lang.String", false, true );
        validateHeader( response, 6, "lastupdated", "Last updated on", "DATE", "java.time.LocalDate", false, true );
        validateHeader( response, 7, "scheduleddate", "Scheduled date", "DATE", "java.time.LocalDate", false, true );
        validateHeader( response, 8, "geometry", "Geometry", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 9, "longitude", "Longitude", "NUMBER", "java.lang.Double", false, true );
        validateHeader( response, 10, "latitude", "Latitude", "NUMBER", "java.lang.Double", false, true );
        validateHeader( response, 11, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 12, "oucode", "Organisation unit code", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 13, "programstatus", "Program status", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 14, "eventstatus", "Event status", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 15, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true );

        // Cannot validate results for relative period.
    }

    @Test
    public void queryWithProgramAndProgramStageWhenTotalPagesIsTrueByDefault()
    {
        // Given
        final QueryParamsBuilder params = new QueryParamsBuilder();
        params.add( "dimension=pe:LAST_12_MONTHS,ou:ImspTQPwCqd" )
            .add( "stage=dBwrot7S420" )
            .add( "displayProperty=NAME" )
            .add( "outputType=EVENT" );

        // When
        final ApiResponse response = analyticsEventActions.query().get( "lxAQ7Zs9VYR", JSON, JSON, params );

        // Then
        response.validate()
            .statusCode( 200 )
            .body( "headers", hasSize( equalTo( 16 ) ) )
            .body( "rows", hasSize( equalTo( 3 ) ) )
            .body( "metaData.pager.page", equalTo( 1 ) )
            .body( "metaData.pager.pageSize", equalTo( 50 ) )
            .body( "metaData.pager.total", equalTo( 3 ) )
            .body( "metaData.pager.pageCount", equalTo( 1 ) )
            .body( "metaData.pager", not( hasKey( "isLastPage" ) ) )
            .body( "metaData.items.ImspTQPwCqd.name", equalTo( "Sierra Leone" ) )
            .body( "metaData.items.dBwrot7S420.name", equalTo( "Antenatal care visit" ) )
            .body( "metaData.items.ou.name", equalTo( "Organisation unit" ) )
            .body( "metaData.items.lxAQ7Zs9VYR.name", equalTo( "Antenatal care visit" ) )
            .body( "metaData.items.LAST_12_MONTHS.name", equalTo( "Last 12 months" ) )
            .body( "metaData.dimensions.pe", hasSize( equalTo( 0 ) ) )
            .body( "metaData.dimensions.ou", hasSize( equalTo( 1 ) ) )
            .body( "metaData.dimensions.ou", hasItem( "ImspTQPwCqd" ) )
            .body( "height", equalTo( 3 ) )
            .body( "width", equalTo( 16 ) )
            .body( "headerWidth", equalTo( 16 ) );

        // Validate headers
        validateHeader( response, 0, "psi", "Event", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 1, "ps", "Program stage", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 2, "eventdate", "Visit date", "DATE", "java.time.LocalDate", false, true );
        validateHeader( response, 3, "storedby", "Stored by", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 4, "createdbydisplayname", "Created by", "TEXT", "java.lang.String",
            false, true );
        validateHeader( response, 5, "lastupdatedbydisplayname", "Last updated by", "TEXT",
            "java.lang.String", false, true );
        validateHeader( response, 6, "lastupdated", "Last updated on", "DATE", "java.time.LocalDate", false, true );
        validateHeader( response, 7, "scheduleddate", "Scheduled date", "DATE", "java.time.LocalDate", false, true );
        validateHeader( response, 8, "geometry", "Geometry", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 9, "longitude", "Longitude", "NUMBER", "java.lang.Double", false, true );
        validateHeader( response, 10, "latitude", "Latitude", "NUMBER", "java.lang.Double", false, true );
        validateHeader( response, 11, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 12, "oucode", "Organisation unit code", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 13, "programstatus", "Program status", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 14, "eventstatus", "Event status", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 15, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true );

        // Cannot validate results for relative period.
    }

    @Disabled
    @ParameterizedTest
    @EnumSource( value = Period.class, names = { "THIS_YEAR", "LAST_YEAR", "TODAY", "WEEKS_THIS_YEAR" } )
    @DisplayName( "api/analytics/events/query/iAI6kmFqoOc.json?dimension=pe:THIS_YEAR,ou:ImspTQPwCqd,eTaBehVASzG.VkoGQvbzHk2&stage=eTaBehVASzG&displayProperty=NAME&outputType=EVENT&desc=eventdate&relativePeriodDate=" )
    public void queryWithProgramAndProgramStageWhenTotalPagesIsTrueByDefault( Period relativePeriod )
    {
        // Given
        String relativePeriodDate = getRelativePeriodDate( relativePeriod );

        QueryParamsBuilder params = new QueryParamsBuilder();
        params.add( "dimension=pe:" + relativePeriod + ",ou:ImspTQPwCqd,eTaBehVASzG.VkoGQvbzHk2" )
            .add( "stage=eTaBehVASzG" )
            .add( "displayProperty=NAME" )
            .add( "outputType=EVENT" )
            .add( "desc=eventdate" );

        // Add (or not) relative period date param.
        if ( isNotEmpty( relativePeriodDate ) )
        {
            params.add( "relativePeriodDate=" + relativePeriodDate );
        }

        // When
        ApiResponse response = analyticsEventActions.query().get( "iAI6kmFqoOc", JSON, JSON, params );

        // Then
        response.validate()
            .statusCode( 200 )
            .body( "headers", hasSize( equalTo( 21 ) ) )
            .body( "rows", hasSize( equalTo( 3 ) ) )
            .body( "metaData.pager.page", equalTo( 1 ) )
            .body( "metaData.pager.pageSize", equalTo( 50 ) )
            .body( "metaData.pager.total", equalTo( 3 ) )
            .body( "metaData.pager.pageCount", equalTo( 1 ) )
            .body( "metaData.pager", not( hasKey( "isLastPage" ) ) )
            .body( "metaData.items.ImspTQPwCqd.name", equalTo( "Sierra Leone" ) )
            .body( "metaData.items.iAI6kmFqoOc.name", equalTo( "TA id schemes tracker program" ) )
            .body( "metaData.items.eTaBehVASzG.name", equalTo( "TA id schemes program stage" ) )
            .body( "metaData.items.ou.name", equalTo( "Organisation unit" ) )
            .body( "metaData.items." + relativePeriod + ".name", equalTo( relativePeriod.label() ) )
            .body( "metaData.items.VkoGQvbzHk2.name", equalTo( "TA id schemes data element" ) )
            .body( "metaData.items.'eTaBehVASzG.VkoGQvbzHk2'.name", equalTo( "TA id schemes data element" ) )
            .body( "metaData.dimensions.pe", hasSize( equalTo( 0 ) ) )
            .body( "metaData.dimensions.ou", hasSize( equalTo( 1 ) ) )
            .body( "metaData.dimensions.ou", hasItem( "ImspTQPwCqd" ) )
            .body( "height", equalTo( 3 ) )
            .body( "width", equalTo( 21 ) )
            .body( "headerWidth", equalTo( 21 ) )
            .body( "metaData.dimensions.'eTaBehVASzG.VkoGQvbzHk2'", hasSize( equalTo( 0 ) ) );

        // Validate headers
        validateHeader( response, 0, "psi", "Event", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 1, "ps", "Program stage", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 2, "eventdate", "Event date", "DATE", "java.time.LocalDate", false, true );
        validateHeader( response, 3, "storedby", "Stored by", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 4, "createdbydisplayname", "Created by", "TEXT", "java.lang.String",
            false, true );
        validateHeader( response, 5, "lastupdatedbydisplayname", "Last updated by", "TEXT",
            "java.lang.String", false, true );
        validateHeader( response, 6, "lastupdated", "Last updated on", "DATE", "java.time.LocalDate", false, true );
        validateHeader( response, 7, "scheduleddate", "Scheduled date", "DATE", "java.time.LocalDate", false, true );
        validateHeader( response, 8, "enrollmentdate", "Enrollment date", "DATE", "java.time.LocalDate", false, true );
        validateHeader( response, 9, "incidentdate", "Incident date", "DATE", "java.time.LocalDate", false, true );
        validateHeader( response, 10, "tei", "Tracked entity instance", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 11, "pi", "Program instance", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 12, "geometry", "Geometry", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 13, "longitude", "Longitude", "NUMBER", "java.lang.Double", false, true );
        validateHeader( response, 14, "latitude", "Latitude", "NUMBER", "java.lang.Double", false, true );
        validateHeader( response, 15, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 16, "oucode", "Organisation unit code", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 17, "programstatus", "Program status", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 18, "eventstatus", "Event status", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 19, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 20, "eTaBehVASzG.VkoGQvbzHk2", "TA id schemes data element", "TEXT",
            "java.lang.String", false, true );
    }

    @Disabled
    @ParameterizedTest
    @EnumSource( value = Period.class, names = { "LAST_YEAR", "LAST_6_MONTHS", "LAST_12_MONTHS", "LAST_5_YEARS" } )
    @DisplayName( "api/analytics/events/query/iAI6kmFqoOc.json?dimension=pe:THIS_YEAR,ou:ImspTQPwCqd,eTaBehVASzG.VkoGQvbzHk2&stage=eTaBehVASzG&displayProperty=NAME&outputType=EVENT&desc=eventdate&relativePeriodDate=" )
    public void queryWithProgramAndProgramStageWhenTotalPagesIsTrueByDefaultAndNoResultsForPeriod(
        Period relativePeriod )
    {
        // Given
        QueryParamsBuilder params = new QueryParamsBuilder();
        params.add( "dimension=pe:" + relativePeriod + ",ou:ImspTQPwCqd,eTaBehVASzG.VkoGQvbzHk2" )
            .add( "stage=eTaBehVASzG" )
            .add( "displayProperty=NAME" )
            .add( "outputType=EVENT" )
            .add( "desc=eventdate" );

        // When
        ApiResponse response = analyticsEventActions.query().get( "iAI6kmFqoOc", JSON, JSON, params );

        // Then
        response.validate()
            .statusCode( 200 )
            .body( "headers", hasSize( equalTo( 21 ) ) )
            .body( "rows", hasSize( equalTo( 0 ) ) )
            .body( "metaData.pager.page", equalTo( 1 ) )
            .body( "metaData.pager.pageSize", equalTo( 50 ) )
            .body( "metaData.pager.total", equalTo( 0 ) )
            .body( "metaData.pager.pageCount", equalTo( 0 ) )
            .body( "metaData.pager", not( hasKey( "isLastPage" ) ) )
            .body( "metaData.items.ImspTQPwCqd.name", equalTo( "Sierra Leone" ) )
            .body( "metaData.items.iAI6kmFqoOc.name", equalTo( "TA id schemes tracker program" ) )
            .body( "metaData.items.eTaBehVASzG.name", equalTo( "TA id schemes program stage" ) )
            .body( "metaData.items.ou.name", equalTo( "Organisation unit" ) )
            .body( "metaData.items." + relativePeriod + ".name", equalTo( relativePeriod.label() ) )
            .body( "metaData.items.VkoGQvbzHk2.name", equalTo( "TA id schemes data element" ) )
            .body( "metaData.items.'eTaBehVASzG.VkoGQvbzHk2'.name", equalTo( "TA id schemes data element" ) )
            .body( "metaData.dimensions.pe", hasSize( equalTo( 0 ) ) )
            .body( "metaData.dimensions.ou", hasSize( equalTo( 1 ) ) )
            .body( "metaData.dimensions.ou", hasItem( "ImspTQPwCqd" ) )
            .body( "height", equalTo( 0 ) )
            .body( "width", equalTo( 0 ) )
            .body( "headerWidth", equalTo( 21 ) )
            .body( "metaData.dimensions.'eTaBehVASzG.VkoGQvbzHk2'", hasSize( equalTo( 0 ) ) );

        // Validate headers
        validateHeader( response, 0, "psi", "Event", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 1, "ps", "Program stage", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 2, "eventdate", "Event date", "DATE", "java.time.LocalDate", false, true );
        validateHeader( response, 3, "storedby", "Stored by", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 4, "createdbydisplayname", "Created by", "TEXT", "java.lang.String",
            false, true );
        validateHeader( response, 5, "lastupdatedbydisplayname", "Last updated by", "TEXT",
            "java.lang.String", false, true );
        validateHeader( response, 6, "lastupdated", "Last updated on", "DATE", "java.time.LocalDate", false, true );
        validateHeader( response, 7, "scheduleddate", "Scheduled date", "DATE", "java.time.LocalDate", false, true );
        validateHeader( response, 8, "enrollmentdate", "Enrollment date", "DATE", "java.time.LocalDate", false, true );
        validateHeader( response, 9, "incidentdate", "Incident date", "DATE", "java.time.LocalDate", false, true );
        validateHeader( response, 10, "tei", "Tracked entity instance", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 11, "pi", "Program instance", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 12, "geometry", "Geometry", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 13, "longitude", "Longitude", "NUMBER", "java.lang.Double", false, true );
        validateHeader( response, 14, "latitude", "Latitude", "NUMBER", "java.lang.Double", false, true );
        validateHeader( response, 15, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 16, "oucode", "Organisation unit code", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 17, "programstatus", "Program status", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 18, "eventstatus", "Event status", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 19, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 20, "eTaBehVASzG.VkoGQvbzHk2", "TA id schemes data element", "TEXT",
            "java.lang.String", false, true );
    }
}
