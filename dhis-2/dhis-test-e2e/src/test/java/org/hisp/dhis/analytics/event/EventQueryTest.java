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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;

import java.util.List;

import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.actions.analytics.AnalyticsEventActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.Test;

/**
 * Groups e2e tests for Events "/query" endpoint.
 *
 * @author maikel arabori
 */
public class EventQueryTest extends AnalyticsApiTest
{
    private final AnalyticsEventActions analyticsEventActions = new AnalyticsEventActions();

    @Test
    public void queryWithProgramAndProgramStageWhenTotalPagesIsFalse()
    {
        // Given
        QueryParamsBuilder params = new QueryParamsBuilder()
            .add( "dimension=pe:LAST_12_MONTHS,ou:ImspTQPwCqd" )
            .add( "stage=dBwrot7S420" )
            .add( "displayProperty=NAME" )
            .add( "outputType=EVENT" )
            .add( "totalPages=false" )
            .add( "desc=lastupdated" )
            .add( "relativePeriodDate=2022-09-27" );

        // When
        ApiResponse response = analyticsEventActions.query().get( "lxAQ7Zs9VYR", JSON, JSON, params );

        // Then
        response.validate()
            .statusCode( 200 )
            .body( "headers", hasSize( equalTo( 17 ) ) )
            .body( "width", equalTo( 17 ) )
            .body( "headerWidth", equalTo( 17 ) )
            .body( "rows", hasSize( equalTo( 3 ) ) )
            .body( "height", equalTo( 3 ) )

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
            .body( "metaData.dimensions.ou", hasItem( "ImspTQPwCqd" ) );

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
        validateHeader( response, 12, "ounamehierarchy", "Organisation unit name hierarchy", "TEXT", "java.lang.String",
            false, true );
        validateHeader( response, 13, "oucode", "Organisation unit code", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 14, "programstatus", "Program status", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 15, "eventstatus", "Event status", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 16, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true );

        // Validate the first three rows, as samples.
        validateRow( response, 0,
            List.of( "ohAH6BXIMad",
                "dBwrot7S420",
                "2022-04-07 00:00:00.0",
                "",
                "",
                "",
                "2018-04-12 16:05:41.933",
                "",
                "",
                "0.0",
                "0.0",
                "Ngelehun CHC",
                "Sierra Leone / Bo / Badjia / Ngelehun CHC",
                "OU_559",
                "ACTIVE",
                "ACTIVE",
                "DiszpKrYNg8" ) );

        validateRow( response, 1,
            List.of( "onXW2DQHRGS",
                "dBwrot7S420",
                "2022-04-01 00:00:00.0",
                "",
                "",
                "",
                "2018-04-12 16:05:28.015",
                "",
                "",
                "0.0",
                "0.0",
                "Ngelehun CHC",
                "Sierra Leone / Bo / Badjia / Ngelehun CHC",
                "OU_559",
                "ACTIVE",
                "ACTIVE",
                "DiszpKrYNg8" ) );

        validateRow( response, 2,
            List.of( "A7vnB73x5Xw",
                "dBwrot7S420",
                "2022-04-01 00:00:00.0",
                "",
                "",
                "",
                "2018-04-12 16:05:16.957",
                "",
                "",
                "0.0",
                "0.0",
                "Ngelehun CHC",
                "Sierra Leone / Bo / Badjia / Ngelehun CHC",
                "OU_559",
                "ACTIVE",
                "ACTIVE",
                "DiszpKrYNg8" ) );
    }

    @Test
    public void queryWithProgramAndProgramStageWhenTotalPagesIsTrueByDefault()
    {
        // Given
        QueryParamsBuilder params = new QueryParamsBuilder()
            .add( "dimension=pe:LAST_12_MONTHS,ou:ImspTQPwCqd" )
            .add( "stage=dBwrot7S420" )
            .add( "displayProperty=NAME" )
            .add( "outputType=EVENT" )
            .add( "desc=lastupdated" )
            .add( "relativePeriodDate=2022-09-22" );

        // When
        ApiResponse response = analyticsEventActions.query().get( "lxAQ7Zs9VYR", JSON, JSON, params );

        // Then
        response.validate()
            .statusCode( 200 )
            .body( "headers", hasSize( equalTo( 17 ) ) )
            .body( "width", equalTo( 17 ) )
            .body( "headerWidth", equalTo( 17 ) )
            .body( "rows", hasSize( equalTo( 3 ) ) )
            .body( "height", equalTo( 3 ) )

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
            .body( "metaData.dimensions.ou", hasItem( "ImspTQPwCqd" ) );

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
        validateHeader( response, 12, "ounamehierarchy", "Organisation unit name hierarchy", "TEXT", "java.lang.String",
            false, true );
        validateHeader( response, 13, "oucode", "Organisation unit code", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 14, "programstatus", "Program status", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 15, "eventstatus", "Event status", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 16, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true );

        // Validate the first three rows, as samples.
        validateRow( response, 0,
            List.of( "ohAH6BXIMad",
                "dBwrot7S420",
                "2022-04-07 00:00:00.0",
                "",
                "",
                "",
                "2018-04-12 16:05:41.933",
                "",
                "",
                "0.0",
                "0.0",
                "Ngelehun CHC",
                "Sierra Leone / Bo / Badjia / Ngelehun CHC",
                "OU_559",
                "ACTIVE",
                "ACTIVE",
                "DiszpKrYNg8" ) );

        validateRow( response, 1,
            List.of( "onXW2DQHRGS",
                "dBwrot7S420",
                "2022-04-01 00:00:00.0",
                "",
                "",
                "",
                "2018-04-12 16:05:28.015",
                "",
                "",
                "0.0",
                "0.0",
                "Ngelehun CHC",
                "Sierra Leone / Bo / Badjia / Ngelehun CHC",
                "OU_559",
                "ACTIVE",
                "ACTIVE",
                "DiszpKrYNg8" ) );

        validateRow( response, 2,
            List.of( "A7vnB73x5Xw",
                "dBwrot7S420",
                "2022-04-01 00:00:00.0",
                "",
                "",
                "",
                "2018-04-12 16:05:16.957",
                "",
                "",
                "0.0",
                "0.0",
                "Ngelehun CHC",
                "Sierra Leone / Bo / Badjia / Ngelehun CHC",
                "OU_559",
                "ACTIVE",
                "ACTIVE",
                "DiszpKrYNg8" ) );
    }

    @Test
    public void queryWithProgramAndProgramStageFilteringByEventDateUsingFixedPeriods()
    {
        // Given
        QueryParamsBuilder params = new QueryParamsBuilder()
            .add( "dimension=ou:ImspTQPwCqd" )
            .add( "stage=Zj7UnCAulEk" )
            .add( "headers=eventdate,ouname,lastupdated" )
            .add( "totalPages=false" )
            .add( "eventDate=202204,202207" )
            .add( "displayProperty=NAME" )
            .add( "outputType=EVENT" )
            .add( "desc=lastupdated" )
            .add( "pageSize=100" )
            .add( "page=1" )
            .add( "includeMetadataDetails=true" )
            .add( "relativePeriodDate=2022-09-22" );

        // When
        ApiResponse response = analyticsEventActions.query().get( "eBAyeGv0exc", JSON, JSON, params );

        // Then
        response.validate()
            .statusCode( 200 )
            .body( "headers", hasSize( equalTo( 3 ) ) )
            .body( "width", equalTo( 3 ) )
            .body( "headerWidth", equalTo( 3 ) )
            .body( "rows", hasSize( equalTo( 100 ) ) )
            .body( "height", equalTo( 100 ) )

            .body( "metaData.pager.page", equalTo( 1 ) )
            .body( "metaData.pager.pageSize", equalTo( 100 ) )
            .body( "metaData.pager.total", equalTo( null ) )
            .body( "metaData.pager.pageCount", equalTo( null ) )
            .body( "metaData.pager.isLastPage", equalTo( false ) )

            .body( "metaData.items.202204.name", equalTo( "April 2022" ) )
            .body( "metaData.items.202207.name", equalTo( "July 2022" ) )

            .body( "metaData.items.ImspTQPwCqd.uid", equalTo( "ImspTQPwCqd" ) )
            .body( "metaData.items.ImspTQPwCqd.code", equalTo( "OU_525" ) )
            .body( "metaData.items.ImspTQPwCqd.name", equalTo( "Sierra Leone" ) )
            .body( "metaData.items.ImspTQPwCqd.dimensionItemType", equalTo( "ORGANISATION_UNIT" ) )
            .body( "metaData.items.ImspTQPwCqd.valueType", equalTo( "NUMBER" ) )
            .body( "metaData.items.ImspTQPwCqd.totalAggregationType", equalTo( "SUM" ) )

            .body( "metaData.items.eBAyeGv0exc.uid", equalTo( "eBAyeGv0exc" ) )
            .body( "metaData.items.eBAyeGv0exc.name", equalTo( "Inpatient morbidity and mortality" ) )

            .body( "metaData.items.ou.uid", equalTo( "ou" ) )
            .body( "metaData.items.ou.name", equalTo( "Organisation unit" ) )
            .body( "metaData.items.ou.dimensionType", equalTo( "ORGANISATION_UNIT" ) )

            .body( "metaData.items.Zj7UnCAulEk.uid", equalTo( "Zj7UnCAulEk" ) )
            .body( "metaData.items.Zj7UnCAulEk.name", equalTo( "Inpatient morbidity and mortality" ) )
            .body( "metaData.items.Zj7UnCAulEk.description", equalTo( "Anonymous and ICD-10 coded inpatient data" ) )

            .body( "metaData.dimensions.pe", hasSize( equalTo( 0 ) ) )
            .body( "metaData.dimensions.ou", hasSize( equalTo( 1 ) ) )
            .body( "metaData.dimensions.ou", hasItem( "ImspTQPwCqd" ) );

        // Validate headers.
        validateHeader( response, 0, "eventdate", "Report date", "DATE", "java.time.LocalDate", false, true );
        validateHeader( response, 1, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 2, "lastupdated", "Last updated on", "DATE", "java.time.LocalDate", false, true );

        // Validate the first three rows, as samples.
        validateRow( response, 0,
            List.of( "2022-04-03 00:00:00.0",
                "Kayongoro MCHP",
                "2018-04-21 14:07:16.471" ) );

        validateRow( response, 1,
            List.of( "2022-04-23 00:00:00.0",
                "Bendu (Yawei) CHP",
                "2018-04-21 14:07:16.233" ) );

        validateRow( response, 2,
            List.of( "2022-07-31 00:00:00.0",
                "Mendekelema CHP",
                "2018-04-21 14:07:16.214" ) );
    }

    @Test
    public void queryWithProgramAndProgramStageFilteringByEventDateUsingRelativePeriod()
    {
        // Given
        QueryParamsBuilder params = new QueryParamsBuilder()
            .add( "dimension=ou:ImspTQPwCqd" )
            .add( "stage=Zj7UnCAulEk" )
            .add( "headers=eventdate,ouname" )
            .add( "totalPages=false" )
            .add( "eventDate=LAST_12_MONTHS" )
            .add( "displayProperty=NAME" )
            .add( "outputType=EVENT" )
            .add( "desc=lastupdated" )
            .add( "pageSize=100" )
            .add( "page=1" )
            .add( "includeMetadataDetails=true" )
            .add( "relativePeriodDate=2022-09-22" );

        // When
        ApiResponse response = analyticsEventActions.query().get( "eBAyeGv0exc", JSON, JSON, params );

        // Then
        response.validate()
            .statusCode( 200 )
            .body( "headers", hasSize( equalTo( 2 ) ) )
            .body( "width", equalTo( 2 ) )
            .body( "headerWidth", equalTo( 2 ) )
            .body( "rows", hasSize( equalTo( 100 ) ) )
            .body( "height", equalTo( 100 ) )

            .body( "metaData.pager.page", equalTo( 1 ) )
            .body( "metaData.pager.pageSize", equalTo( 100 ) )
            .body( "metaData.pager.total", equalTo( null ) )
            .body( "metaData.pager.pageCount", equalTo( null ) )
            .body( "metaData.pager.isLastPage", equalTo( false ) )

            .body( "metaData.items.LAST_12_MONTHS.name", equalTo( "Last 12 months" ) );

        // Validate headers.
        validateHeader( response, 0, "eventdate", "Report date", "DATE", "java.time.LocalDate", false, true );
        validateHeader( response, 1, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true );

        // Validate the first three rows, as samples.
        validateRow( response, 0,
            List.of( "2022-08-02 00:00:00.0",
                "Ngelehun CHC" ) );

        validateRow( response, 1,
            List.of( "2022-08-02 00:00:00.0",
                "Ngelehun CHC" ) );

        validateRow( response, 2,
            List.of( "2022-08-01 00:00:00.0",
                "Ngelehun CHC" ) );
    }

    @Test
    public void queryWithProgramAndProgramStageFilteringByEventDateUsingStartEndDates()
    {
        // Given
        QueryParamsBuilder params = new QueryParamsBuilder()
            .add( "dimension=ou:ImspTQPwCqd" )
            .add( "stage=Zj7UnCAulEk" )
            .add( "headers=eventdate,ouname" )
            .add( "totalPages=false" )
            .add( "eventDate=2021-03-02_2022-03-13" )
            .add( "displayProperty=NAME" )
            .add( "outputType=EVENT" )
            .add( "desc=lastupdated" )
            .add( "pageSize=100" )
            .add( "page=1" )
            .add( "includeMetadataDetails=true" )
            .add( "relativePeriodDate=2022-09-22" );

        // When
        ApiResponse response = analyticsEventActions.query().get( "eBAyeGv0exc", JSON, JSON, params );

        // Then
        response.validate()
            .statusCode( 200 )
            .body( "headers", hasSize( equalTo( 2 ) ) )
            .body( "width", equalTo( 2 ) )
            .body( "headerWidth", equalTo( 2 ) )
            .body( "rows", hasSize( equalTo( 100 ) ) )
            .body( "height", equalTo( 100 ) )

            .body( "metaData.pager.page", equalTo( 1 ) )
            .body( "metaData.pager.pageSize", equalTo( 100 ) )
            .body( "metaData.pager.total", equalTo( null ) )
            .body( "metaData.pager.pageCount", equalTo( null ) )
            .body( "metaData.pager.isLastPage", equalTo( false ) )

            .body( "metaData.items.2021-03-02_2022-03-13.name", equalTo( "2021-03-02 - 2022-03-13" ) );

        // Validate headers.
        validateHeader( response, 0, "eventdate", "Report date", "DATE", "java.time.LocalDate", false, true );
        validateHeader( response, 1, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true );

        // Validate the first three rows, as samples.
        validateRow( response, 0,
            List.of( "2021-11-04 00:00:00.0",
                "Ngelehun CHC" ) );

        validateRow( response, 1,
            List.of( "2021-10-07 00:00:00.0",
                "Ngelehun CHC" ) );

        validateRow( response, 2,
            List.of( "2021-11-05 00:00:00.0",
                "Ngelehun CHC" ) );
    }

    @Test
    public void queryWithProgramAndProgramStageAndProgramIndicatorFilteringByEventDateUsingRelativePeriod()
    {
        // Given
        QueryParamsBuilder params = new QueryParamsBuilder()
            .add( "dimension=ou:USER_ORGUNIT,p2Zxg0wcPQ3" )
            .add( "stage=A03MvHHogjR" )
            .add( "headers=ouname,p2Zxg0wcPQ3,eventdate" )
            .add( "totalPages=false" )
            .add( "eventDate=LAST_12_MONTHS" )
            .add( "displayProperty=NAME" )
            .add( "outputType=EVENT" )
            .add( "desc=lastupdated" )
            .add( "pageSize=100" )
            .add( "page=1" )
            .add( "includeMetadataDetails=true" )
            .add( "relativePeriodDate=2022-09-22" );

        // When
        ApiResponse response = analyticsEventActions.query().get( "IpHINAT79UW", JSON, JSON, params );

        // Then
        response.validate()
            .statusCode( 200 )
            .body( "headers", hasSize( equalTo( 3 ) ) )
            .body( "width", equalTo( 3 ) )
            .body( "headerWidth", equalTo( 3 ) )
            .body( "rows", hasSize( equalTo( 100 ) ) )
            .body( "height", equalTo( 100 ) )

            .body( "metaData.pager.page", equalTo( 1 ) )
            .body( "metaData.pager.pageSize", equalTo( 100 ) )
            .body( "metaData.pager.total", equalTo( null ) )
            .body( "metaData.pager.pageCount", equalTo( null ) )
            .body( "metaData.pager.isLastPage", equalTo( false ) )

            .body( "metaData.items.LAST_12_MONTHS.name", equalTo( "Last 12 months" ) )

            .body( "metaData.items.ImspTQPwCqd.uid", equalTo( "ImspTQPwCqd" ) )
            .body( "metaData.items.ImspTQPwCqd.code", equalTo( "OU_525" ) )
            .body( "metaData.items.ImspTQPwCqd.name", equalTo( "Sierra Leone" ) )
            .body( "metaData.items.ImspTQPwCqd.dimensionItemType", equalTo( "ORGANISATION_UNIT" ) )
            .body( "metaData.items.ImspTQPwCqd.valueType", equalTo( "NUMBER" ) )
            .body( "metaData.items.ImspTQPwCqd.totalAggregationType", equalTo( "SUM" ) )

            .body( "metaData.items.p2Zxg0wcPQ3.uid", equalTo( "p2Zxg0wcPQ3" ) )
            .body( "metaData.items.p2Zxg0wcPQ3.code", equalTo( "BCG_DOSE" ) )
            .body( "metaData.items.p2Zxg0wcPQ3.name", equalTo( "BCG doses" ) )
            .body( "metaData.items.p2Zxg0wcPQ3.dimensionItemType", equalTo( "PROGRAM_INDICATOR" ) )
            .body( "metaData.items.p2Zxg0wcPQ3.valueType", equalTo( "NUMBER" ) )
            .body( "metaData.items.p2Zxg0wcPQ3.totalAggregationType", equalTo( "SUM" ) )

            .body( "metaData.items.IpHINAT79UW.uid", equalTo( "IpHINAT79UW" ) )
            .body( "metaData.items.IpHINAT79UW.name", equalTo( "Child Programme" ) )

            .body( "metaData.items.ou.uid", equalTo( "ou" ) )
            .body( "metaData.items.ou.name", equalTo( "Organisation unit" ) )
            .body( "metaData.items.ou.dimensionType", equalTo( "ORGANISATION_UNIT" ) )

            .body( "metaData.items.A03MvHHogjR.uid", equalTo( "A03MvHHogjR" ) )
            .body( "metaData.items.A03MvHHogjR.name", equalTo( "Birth" ) )
            .body( "metaData.items.A03MvHHogjR.description", equalTo( "Birth of the baby" ) )

            .body( "metaData.dimensions.pe", hasSize( equalTo( 0 ) ) )
            .body( "metaData.dimensions.ou", hasSize( equalTo( 1 ) ) )
            .body( "metaData.dimensions.ou", hasItem( "ImspTQPwCqd" ) )
            .body( "metaData.dimensions.p2Zxg0wcPQ3", hasSize( equalTo( 0 ) ) );

        // Validate headers.
        validateHeader( response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 1, "p2Zxg0wcPQ3", "BCG doses", "NUMBER", "java.lang.Double", false, true );
        validateHeader( response, 2, "eventdate", "Report date", "DATE", "java.time.LocalDate", false, true );

        // Validate the first three rows, as samples.
        validateRow( response, 0,
            List.of( "Ngelehun CHC",
                "0.0",
                "2022-02-27 00:00:00.0" ) );

        validateRow( response, 1,
            List.of( "Ngelehun CHC",
                "0.0",
                "2022-05-27 00:00:00.0" ) );

        validateRow( response, 2,
            List.of( "Ngelehun CHC",
                "1.0",
                "2022-01-19 00:00:00.0" ) );
    }

    @Test
    public void queryWithProgramAndProgramStageAndProgramIndicatorFilteringByEventDateUsingFixedDates()
    {
        // Given
        QueryParamsBuilder params = new QueryParamsBuilder()
            .add( "dimension=ou:USER_ORGUNIT,p2Zxg0wcPQ3" )
            .add( "stage=A03MvHHogjR" )
            .add( "headers=ouname,p2Zxg0wcPQ3,eventdate" )
            .add( "totalPages=false" )
            .add( "eventDate=2021-03-02_2023-03-01" )
            .add( "displayProperty=NAME" )
            .add( "outputType=EVENT" )
            .add( "desc=lastupdated" )
            .add( "pageSize=100" )
            .add( "page=1" )
            .add( "includeMetadataDetails=true" )
            .add( "relativePeriodDate=2022-09-22" );

        // When
        ApiResponse response = analyticsEventActions.query().get( "IpHINAT79UW", JSON, JSON, params );

        // Then
        response.validate()
            .statusCode( 200 )
            .body( "headers", hasSize( equalTo( 3 ) ) )
            .body( "width", equalTo( 3 ) )
            .body( "headerWidth", equalTo( 3 ) )
            .body( "rows", hasSize( equalTo( 100 ) ) )
            .body( "height", equalTo( 100 ) )

            .body( "metaData.pager.page", equalTo( 1 ) )
            .body( "metaData.pager.pageSize", equalTo( 100 ) )
            .body( "metaData.pager.total", equalTo( null ) )
            .body( "metaData.pager.pageCount", equalTo( null ) )
            .body( "metaData.pager.isLastPage", equalTo( false ) );

        // Validate headers.
        validateHeader( response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 1, "p2Zxg0wcPQ3", "BCG doses", "NUMBER", "java.lang.Double", false, true );
        validateHeader( response, 2, "eventdate", "Report date", "DATE", "java.time.LocalDate", false, true );

        // Validate the first three rows, as samples.
        validateRow( response, 0,
            List.of( "Ngelehun CHC",
                "0.0",
                "2022-02-27 00:00:00.0" ) );

        validateRow( response, 1,
            List.of( "Ngelehun CHC",
                "1.0",
                "2022-12-29 00:00:00.0" ) );

        validateRow( response, 2,
            List.of( "Ngelehun CHC",
                "0.0",
                "2021-08-15 00:00:00.0" ) );
    }

    @Test
    public void eventQueryWithProgramAndRepeatableProgramStage()
    {
        // Given
        QueryParamsBuilder params = new QueryParamsBuilder()
            .add( "dimension=edqlbukwRfQ.vANAXwtLwcT,ou:ImspTQPwCqd" )
            .add( "headers=ou,ounamehierarchy,edqlbukwRfQ.vANAXwtLwcT" )
            .add( "stage=edqlbukwRfQ" )
            .add( "displayProperty=NAME" )
            .add( "outputType=EVENT" )
            .add( "desc=incidentdate" )
            .add( "totalPages=false" )
            .add( "pageSize=100" )
            .add( "page=1" )
            .add( "rowContext=true" );

        // When
        ApiResponse response = analyticsEventActions.query().get( "WSGAb5XwJ3Y", JSON, JSON, params );
        response.validate()
            .statusCode( 200 )
            .body( "headers", hasSize( equalTo( 3 ) ) )
            .body( "rows", hasSize( equalTo( 100 ) ) );

        validateHeader( response, 0, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 1, "ounamehierarchy", "Organisation unit name hierarchy", "TEXT", "java.lang.String",
            false, true );
        validateHeader( response, 2, "edqlbukwRfQ.vANAXwtLwcT", "WHOMCH Hemoglobin value", "NUMBER",
            "java.lang.Double", false, true );

        validateRow( response,
            List.of( "NjyJYiIuKIG",
                "Sierra Leone / Bombali / Sella Limba / Kathanta Yimbor CHC",
                "14.0" ) );
        validateRow( response,
            List.of( "xATvj8pdYoT",
                "Sierra Leone / Kailahun / Peje Bongre / Grima Jou MCHP",
                "24.0" ) );
    }

    @Test
    void testMetadataInfoForOptionSetForQuery()
    {
        // Given
        QueryParamsBuilder params = new QueryParamsBuilder()
            .add(
                "dimension=ou:ImspTQPwCqd,pe:LAST_12_MONTHS,C0aLZo75dgJ.B6TnnFMgmCk,C0aLZo75dgJ.Z1rLc1rVHK8,C0aLZo75dgJ.CklPZdOd6H1" )
            .add( "filter=C0aLZo75dgJ.vTKipVM0GsX,C0aLZo75dgJ.h5FuguPFF2j,C0aLZo75dgJ.aW66s2QSosT" )
            .add( "stage=C0aLZo75dgJ" )
            .add( "displayProperty=NAME" )
            .add( "outputType=ENROLLMENT" )
            .add( "totalPages=false" );

        // When
        ApiResponse response = analyticsEventActions.query().get( "qDkgAbB5Jlk", JSON, JSON, params );
        response.validate()
            .statusCode( 200 )
            .body( "headers", hasSize( equalTo( 24 ) ) )
            .body( "height", equalTo( 0 ) )
            .body( "width", equalTo( 0 ) )
            .body( "rows", hasSize( equalTo( 0 ) ) )

            .body( "metaData.items", hasKey( "CklPZdOd6H1" ) )
            .body( "metaData.items", not( hasKey( "AZK4rjJCss5" ) ) )
            .body( "metaData.items", not( hasKey( "UrUdMteQzlT" ) ) );

        validateHeader( response, 22, "C0aLZo75dgJ.CklPZdOd6H1", "Sex", "TEXT", "java.lang.String",
            false, true, "hiQ3QFheQ3O" );
    }
}
