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
package org.hisp.dhis.analytics.tei;

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
import org.hisp.dhis.actions.analytics.AnalyticsTeiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.Test;

/**
 * Groups e2e tests for Tracked Entities "/query" endpoint.
 *
 * @author maikel arabori
 */
public class TrackedEntityQueryTest extends AnalyticsApiTest
{
    private AnalyticsTeiActions analyticsTeiActions = new AnalyticsTeiActions();

    @Test
    public void queryWithProgramAndProgramStageWhenTotalPagesIsFalse()
    {
        // Given
        QueryParamsBuilder params = new QueryParamsBuilder()
            .add( "dimension=ou:ImspTQPwCqd" )
            .add( "program=IpHINAT79UW" )
            .add( "asc=IpHINAT79UW.w75KJ2mc4zz" )
            .add( "totalPages=false" )
            .add( "pageSize=100" )
            .add( "page=1" )
            .add( "relativePeriodDate=2022-09-27" );

        // When
        ApiResponse response = analyticsTeiActions.query().get( "nEenWmSyUEp", JSON, JSON, params );

        // Then
        response.validate()
            .statusCode( 200 )
            .body( "headers", hasSize( equalTo( 14 ) ) )
            .body( "rows", hasSize( equalTo( 0 ) ) )
            .body( "metaData.pager.page", equalTo( 1 ) )
            .body( "metaData.pager.pageSize", equalTo( 100 ) )
            .body( "metaData.pager.isLastPage", is( false ) )
            .body( "metaData.pager", not( hasKey( "total" ) ) )
            .body( "metaData.pager", not( hasKey( "pageCount" ) ) )
            .body( "metaData.items.ImspTQPwCqd.name", equalTo( "Sierra Leone" ) )
            .body( "metaData.items.lZGmxYbs97q.name", equalTo( "Unique ID" ) )
            .body( "metaData.items.zDhUuAYrxNC.name", equalTo( "Last name" ) )
            .body( "metaData.items.IpHINAT79UW.name", equalTo( "Child Programme" ) )
            .body( "metaData.items.ZzYYXq4fJie.name", equalTo( "Baby Postnatal" ) )
            .body( "metaData.items.w75KJ2mc4zz.name", equalTo( "First name" ) )
            .body( "metaData.items.A03MvHHogjR.name", equalTo( "Birth" ) )
            .body( "metaData.items.cejWyOfXge6.name", equalTo( "Gender" ) )
            .body( "metaData.items.ou.name", equalTo( "Organisation unit" ) )
            .body( "metaData.dimensions", hasKey( "lZGmxYbs97q" ) )
            .body( "metaData.dimensions", hasKey( "zDhUuAYrxNC" ) )
            .body( "metaData.dimensions", hasKey( "pe" ) )
            .body( "metaData.dimensions", hasKey( "w75KJ2mc4zz" ) )
            .body( "metaData.dimensions", hasKey( "cejWyOfXge6" ) )
            .body( "metaData.dimensions.ou", hasSize( equalTo( 1 ) ) )
            .body( "metaData.dimensions.ou", hasItem( "ImspTQPwCqd" ) )
            .body( "height", equalTo( 0 ) )
            .body( "width", equalTo( 0 ) )
            .body( "headerWidth", equalTo( 14 ) );

        // Validate headers
        validateHeader( response, 0, "trackedentityinstanceuid", "Tracked Entity Instance", "TEXT", "java.lang.String",
            false, true );
        validateHeader( response, 1, "lastupdated", "Last Updated", "DATETIME", "java.time.LocalDateTime", false,
            true );
        validateHeader( response, 2, "createdbydisplayname", "Created by (display name)", "TEXT", "java.lang.String",
            false, true );
        validateHeader( response, 3, "lastupdatedbydisplayname", "Last updated by (display name)", "TEXT",
            "java.lang.String", false, true );
        validateHeader( response, 4, "geometry", "Geometry", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 5, "longitude", "Longitude", "NUMBER", "java.lang.Double", false, true );
        validateHeader( response, 6, "latitude", "Latitude", "NUMBER", "java.lang.Double", false, true );
        validateHeader( response, 7, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 8, "oucode", "Organisation unit code", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 9, "enrollments", "Enrollments", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 10, "lZGmxYbs97q", "Unique ID", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 11, "w75KJ2mc4zz", "First name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 12, "zDhUuAYrxNC", "Last name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 13, "cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true );
    }

    @Test
    public void queryWithProgramOnly()
    {
        // Given
        QueryParamsBuilder params = new QueryParamsBuilder()
            .add( "program=IpHINAT79UW" )
            .add( "asc=lastupdated" )
            .add( "relativePeriodDate=2022-09-27" );

        // When
        ApiResponse response = analyticsTeiActions.query().get( "nEenWmSyUEp", JSON, JSON, params );

        // Then
        response.validate()
            .statusCode( 200 )
            .body( "headers", hasSize( equalTo( 14 ) ) )
            .body( "rows", hasSize( equalTo( 50 ) ) )
            .body( "metaData.pager.page", equalTo( 1 ) )
            .body( "metaData.pager.pageSize", equalTo( 50 ) )
            .body( "metaData.pager.isLastPage", is( false ) )
            .body( "metaData.pager", not( hasKey( "total" ) ) )
            .body( "metaData.pager", not( hasKey( "pageCount" ) ) )
            .body( "metaData.items.ImspTQPwCqd.name", equalTo( null ) )
            .body( "metaData.items.lZGmxYbs97q.name", equalTo( "Unique ID" ) )
            .body( "metaData.items.zDhUuAYrxNC.name", equalTo( "Last name" ) )
            .body( "metaData.items.IpHINAT79UW.name", equalTo( "Child Programme" ) )
            .body( "metaData.items.ZzYYXq4fJie.name", equalTo( "Baby Postnatal" ) )
            .body( "metaData.items.w75KJ2mc4zz.name", equalTo( "First name" ) )
            .body( "metaData.items.A03MvHHogjR.name", equalTo( "Birth" ) )
            .body( "metaData.items.cejWyOfXge6.name", equalTo( "Gender" ) )
            .body( "metaData.items.ou.name", equalTo( null ) )
            .body( "metaData.dimensions", hasKey( "lZGmxYbs97q" ) )
            .body( "metaData.dimensions", hasKey( "zDhUuAYrxNC" ) )
            .body( "metaData.dimensions", hasKey( "pe" ) )
            .body( "metaData.dimensions", hasKey( "w75KJ2mc4zz" ) )
            .body( "metaData.dimensions", hasKey( "cejWyOfXge6" ) )
            .body( "metaData.dimensions", not( hasKey( "ou" ) ) )
            .body( "height", equalTo( 50 ) )
            .body( "width", equalTo( 14 ) )
            .body( "headerWidth", equalTo( 14 ) );

        // Validate headers
        validateHeader( response, 0, "trackedentityinstanceuid", "Tracked Entity Instance", "TEXT", "java.lang.String",
            false, true );
        validateHeader( response, 1, "lastupdated", "Last Updated", "DATETIME", "java.time.LocalDateTime", false,
            true );
        validateHeader( response, 2, "createdbydisplayname", "Created by (display name)", "TEXT", "java.lang.String",
            false, true );
        validateHeader( response, 3, "lastupdatedbydisplayname", "Last updated by (display name)", "TEXT",
            "java.lang.String", false, true );
        validateHeader( response, 4, "geometry", "Geometry", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 5, "longitude", "Longitude", "NUMBER", "java.lang.Double", false, true );
        validateHeader( response, 6, "latitude", "Latitude", "NUMBER", "java.lang.Double", false, true );
        validateHeader( response, 7, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 8, "oucode", "Organisation unit code", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 9, "enrollments", "Enrollments", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 10, "lZGmxYbs97q", "Unique ID", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 11, "w75KJ2mc4zz", "First name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 12, "zDhUuAYrxNC", "Last name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 13, "cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true );

        // Validate the first three rows, as samples.
        validateRow( response, 0,
            List.of( "oi3PMIGYJH8",
                "2014-07-23 12:45:49.787",
                "",
                "",
                "",
                "",
                "",
                "Ngelehun CHC",
                "OU_559",
                "[{\"programUid\" : \"IpHINAT79UW\", \"programInstanceUid\" : \"EbRsJr8LSSO\", \"enrollmentDate\" : \"2022-07-02T02:00:00\", \"incidentDate\" : \"2022-07-08T02:00:00\", \"endDate\" : null, \"events\" : [{\"programStageUid\" : \"ZzYYXq4fJie\", \"programStageInstanceUid\" : \"nwe6hM3ZDJ0\", \"executionDate\" : null, \"dueDate\" : \"2021-07-14T02:00:00\", \"eventDataValues\" : {}}, {\"programStageUid\" : \"A03MvHHogjR\", \"programStageInstanceUid\" : \"zfzS9WeO0uM\", \"executionDate\" : \"2021-07-03T00:00:00\", \"dueDate\" : \"2021-07-23T12:46:11.472\", \"eventDataValues\" : {\"UXz7xuGCEhU\": {\"value\": \"1231\", \"created\": \"2014-07-23T12:46:04.45\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:04.45\", \"providedElsewhere\": false}, \"X8zyunlgUfM\": {\"value\": \"Replacement\", \"created\": \"2014-07-23T12:46:11.485\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:11.485\", \"providedElsewhere\": false}, \"a3kGcGDCuk6\": {\"value\": \"34\", \"created\": \"2014-07-23T12:45:58.15\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:45:58.15\", \"providedElsewhere\": false}, \"bx6fsa0t90x\": {\"value\": \"Yes\", \"created\": \"2014-07-23T12:46:07.795\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:07.795\", \"providedElsewhere\": false}, \"ebaJjqltK5N\": {\"value\": \"1\", \"created\": \"2014-07-23T12:46:09.026\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:09.026\", \"providedElsewhere\": false}, \"wQLfBvPrXqq\": {\"value\": \"NVP only\", \"created\": \"2014-07-23T12:46:06.385\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:06.385\", \"providedElsewhere\": false}}}, {\"programStageUid\" : \"ZzYYXq4fJie\", \"programStageInstanceUid\" : \"hUQEgo9b5dz\", \"executionDate\" : \"2021-07-24T00:00:00\", \"dueDate\" : \"2021-07-23T12:47:03.466\", \"eventDataValues\" : {\"BeynU4L6VCQ\": {\"value\": \"No\", \"created\": \"2014-07-23T12:46:53.337\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:53.337\", \"providedElsewhere\": false}, \"GQY2lXrypjO\": {\"value\": \"1214\", \"created\": \"2014-07-23T12:46:22.459\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:22.459\", \"providedElsewhere\": false}, \"HLmTEmupdX0\": {\"value\": \"No\", \"created\": \"2014-07-23T12:46:30.081\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:30.081\", \"providedElsewhere\": false}, \"OuJ6sgPyAbC\": {\"value\": \"High blood pressure.\", \"created\": \"2014-07-23T12:47:03.479\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:47:03.479\", \"providedElsewhere\": false}, \"X8zyunlgUfM\": {\"value\": \"Replacement\", \"created\": \"2014-07-23T12:46:23.958\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:23.958\", \"providedElsewhere\": false}, \"aei1xRjSU2l\": {\"value\": \"Yes\", \"created\": \"2014-07-23T12:46:52.451\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:52.451\", \"providedElsewhere\": false}, \"bx6fsa0t90x\": {\"value\": \"Yes\", \"created\": \"2014-07-23T12:46:25.026\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:25.026\", \"providedElsewhere\": false}, \"cYGaxwK615G\": {\"value\": \"Positive\", \"created\": \"2014-07-23T12:46:37.473\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:37.473\", \"providedElsewhere\": false}, \"ebaJjqltK5N\": {\"value\": \"1\", \"created\": \"2014-07-23T12:46:26.192\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:26.192\", \"providedElsewhere\": false}, \"hDZbpskhqDd\": {\"value\": \"Blood\", \"created\": \"2014-07-23T12:46:47.84\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:47.84\", \"providedElsewhere\": false}, \"pOe0ogW4OWd\": {\"value\": \"12\", \"created\": \"2014-07-23T12:46:28.192\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:28.192\", \"providedElsewhere\": false}, \"sj3j9Hwc7so\": {\"value\": \"TDF/3TC/EFV - 1\", \"created\": \"2014-07-23T12:46:51.127\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:51.127\", \"providedElsewhere\": false}}}]}]",
                "",
                "Evelyn",
                "Jackson",
                "Female" ) );

        validateRow( response, 1,
            List.of( "mYyHxkNAOr2",
                "2014-09-23 20:01:44.961",
                "",
                "",
                "",
                "",
                "",
                "Ngelehun CHC",
                "OU_559",
                "[{\"programUid\" : \"IpHINAT79UW\", \"programInstanceUid\" : \"LZg2cOvrAlU\", \"enrollmentDate\" : \"2022-09-04T02:00:00\", \"incidentDate\" : \"2002-09-07T02:00:00\", \"endDate\" : null, \"events\" : [{\"programStageUid\" : \"A03MvHHogjR\", \"programStageInstanceUid\" : \"ea2QqDBbvMX\", \"executionDate\" : \"2021-05-22T00:00:00\", \"dueDate\" : \"2014-10-02T00:00:00\", \"eventDataValues\" : {\"UXz7xuGCEhU\": {\"value\": \"3500\", \"created\": \"2014-11-15T19:11:25.977\", \"storedBy\": null, \"lastUpdated\": \"2014-11-15T19:11:25.977\", \"providedElsewhere\": false}, \"X8zyunlgUfM\": {\"value\": \"Replacement\", \"created\": \"2014-11-15T19:11:30.088\", \"storedBy\": null, \"lastUpdated\": \"2014-11-15T19:11:30.088\", \"providedElsewhere\": false}, \"a3kGcGDCuk6\": {\"value\": \"5\", \"created\": \"2014-11-15T19:11:22.476\", \"storedBy\": null, \"lastUpdated\": \"2014-11-15T19:11:22.476\", \"providedElsewhere\": false}, \"bx6fsa0t90x\": {\"value\": \"Yes\", \"created\": \"2014-11-15T19:11:28.148\", \"storedBy\": null, \"lastUpdated\": \"2014-11-15T19:11:28.148\", \"providedElsewhere\": false}, \"ebaJjqltK5N\": {\"value\": \"2\", \"created\": \"2014-11-15T19:11:29.018\", \"storedBy\": null, \"lastUpdated\": \"2014-11-15T19:11:29.018\", \"providedElsewhere\": false}, \"wQLfBvPrXqq\": {\"value\": \"Others\", \"created\": \"2014-11-15T19:11:27.329\", \"storedBy\": null, \"lastUpdated\": \"2014-11-15T19:11:27.329\", \"providedElsewhere\": false}}}, {\"programStageUid\" : \"ZzYYXq4fJie\", \"programStageInstanceUid\" : \"V2f1EhZch8x\", \"executionDate\" : null, \"dueDate\" : \"2001-09-13T02:00:00\", \"eventDataValues\" : {}}, {\"programStageUid\" : \"ZzYYXq4fJie\", \"programStageInstanceUid\" : \"QHde4ozqiNl\", \"executionDate\" : null, \"dueDate\" : \"2020-10-02T00:00:00\", \"eventDataValues\" : {}}]}]",
                "",
                "John",
                "Thomson",
                "Female" ) );

        validateRow( response, 2,
            List.of( "SBjuNw0Xtkn",
                "2014-10-01 12:27:37.837",
                "",
                "",
                "",
                "",
                "",
                "Ngelehun CHC",
                "OU_559",
                "[{\"programUid\" : \"IpHINAT79UW\", \"programInstanceUid\" : \"SAWQe5hyhy0\", \"enrollmentDate\" : \"2022-09-01T02:00:00\", \"incidentDate\" : \"2022-10-01T12:27:37.81\", \"endDate\" : null, \"events\" : [{\"programStageUid\" : \"ZzYYXq4fJie\", \"programStageInstanceUid\" : \"NFKC68Jne6p\", \"executionDate\" : \"2021-10-01T00:00:00\", \"dueDate\" : \"2021-10-01T12:28:04.391\", \"eventDataValues\" : {\"GQY2lXrypjO\": {\"value\": \"12\", \"created\": \"2014-10-01T12:27:51.597\", \"storedBy\": null, \"lastUpdated\": \"2014-10-01T12:27:51.597\", \"providedElsewhere\": false}, \"HLmTEmupdX0\": {\"value\": \"[object Object]\", \"created\": \"2014-10-01T12:28:04.451\", \"storedBy\": null, \"lastUpdated\": \"2014-10-01T12:28:04.451\", \"providedElsewhere\": false}, \"X8zyunlgUfM\": {\"value\": \"[object Object]\", \"created\": \"2014-10-01T12:27:53.271\", \"storedBy\": null, \"lastUpdated\": \"2014-10-01T12:27:53.271\", \"providedElsewhere\": false}, \"bx6fsa0t90x\": {\"value\": \"[object Object]\", \"created\": \"2014-10-01T12:27:54.836\", \"storedBy\": null, \"lastUpdated\": \"2014-10-01T12:27:54.836\", \"providedElsewhere\": false}, \"ebaJjqltK5N\": {\"value\": \"[object Object]\", \"created\": \"2014-10-01T12:27:56.022\", \"storedBy\": null, \"lastUpdated\": \"2014-10-01T12:27:56.022\", \"providedElsewhere\": false}, \"pOe0ogW4OWd\": {\"value\": \"[object Object]\", \"created\": \"2014-10-01T12:27:58.159\", \"storedBy\": null, \"lastUpdated\": \"2014-10-01T12:27:58.159\", \"providedElsewhere\": false}}}, {\"programStageUid\" : \"ZzYYXq4fJie\", \"programStageInstanceUid\" : \"ljdXg3yD3qr\", \"executionDate\" : null, \"dueDate\" : \"2021-10-07T12:27:37.81\", \"eventDataValues\" : {}}, {\"programStageUid\" : \"A03MvHHogjR\", \"programStageInstanceUid\" : \"b0pZPsbHh0R\", \"executionDate\" : null, \"dueDate\" : \"2021-10-01T12:27:37.81\", \"eventDataValues\" : {}}]}]",
                "",
                "Tom",
                "Johson",
                "" ) );
    }

    @Test
    public void queryWithProgramAndPagination()
    {
        // Given
        QueryParamsBuilder params = new QueryParamsBuilder()
            .add( "program=IpHINAT79UW" )
            .add( "pageSize=10" )
            .add( "totalPages=true" )
            .add( "asc=lastupdated" )
            .add( "relativePeriodDate=2022-09-27" );

        // When
        ApiResponse response = analyticsTeiActions.query().get( "nEenWmSyUEp", JSON, JSON, params );

        // Then
        response.validate()
            .statusCode( 200 )
            .body( "headers", hasSize( equalTo( 14 ) ) )
            .body( "rows", hasSize( equalTo( 10 ) ) )
            .body( "metaData.pager.page", equalTo( 1 ) )
            .body( "metaData.pager.pageSize", equalTo( 10 ) )
            .body( "metaData.pager", not( hasKey( "isLastPage" ) ) )
            .body( "metaData.pager.total", equalTo( 19023 ) )
            .body( "metaData.pager.pageCount", equalTo( 1903 ) )
            .body( "metaData.items.ImspTQPwCqd.name", equalTo( null ) )
            .body( "metaData.items.lZGmxYbs97q.name", equalTo( "Unique ID" ) )
            .body( "metaData.items.zDhUuAYrxNC.name", equalTo( "Last name" ) )
            .body( "metaData.items.IpHINAT79UW.name", equalTo( "Child Programme" ) )
            .body( "metaData.items.ZzYYXq4fJie.name", equalTo( "Baby Postnatal" ) )
            .body( "metaData.items.w75KJ2mc4zz.name", equalTo( "First name" ) )
            .body( "metaData.items.A03MvHHogjR.name", equalTo( "Birth" ) )
            .body( "metaData.items.cejWyOfXge6.name", equalTo( "Gender" ) )
            .body( "metaData.items.ou.name", equalTo( null ) )
            .body( "metaData.dimensions", hasKey( "lZGmxYbs97q" ) )
            .body( "metaData.dimensions", hasKey( "zDhUuAYrxNC" ) )
            .body( "metaData.dimensions", hasKey( "pe" ) )
            .body( "metaData.dimensions", hasKey( "w75KJ2mc4zz" ) )
            .body( "metaData.dimensions", hasKey( "cejWyOfXge6" ) )
            .body( "metaData.dimensions", not( hasKey( "ou" ) ) )
            .body( "height", equalTo( 10 ) )
            .body( "width", equalTo( 14 ) )
            .body( "headerWidth", equalTo( 14 ) );

        // Validate headers
        validateHeader( response, 0, "trackedentityinstanceuid", "Tracked Entity Instance", "TEXT", "java.lang.String",
            false, true );
        validateHeader( response, 1, "lastupdated", "Last Updated", "DATETIME", "java.time.LocalDateTime", false,
            true );
        validateHeader( response, 2, "createdbydisplayname", "Created by (display name)", "TEXT", "java.lang.String",
            false, true );
        validateHeader( response, 3, "lastupdatedbydisplayname", "Last updated by (display name)", "TEXT",
            "java.lang.String", false, true );
        validateHeader( response, 4, "geometry", "Geometry", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 5, "longitude", "Longitude", "NUMBER", "java.lang.Double", false, true );
        validateHeader( response, 6, "latitude", "Latitude", "NUMBER", "java.lang.Double", false, true );
        validateHeader( response, 7, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 8, "oucode", "Organisation unit code", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 9, "enrollments", "Enrollments", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 10, "lZGmxYbs97q", "Unique ID", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 11, "w75KJ2mc4zz", "First name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 12, "zDhUuAYrxNC", "Last name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 13, "cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true );

        // Validate the first three rows, as samples.
        validateRow( response, 0,
            List.of( "oi3PMIGYJH8",
                "2014-07-23 12:45:49.787",
                "",
                "",
                "",
                "",
                "",
                "Ngelehun CHC",
                "OU_559",
                "[{\"programUid\" : \"IpHINAT79UW\", \"programInstanceUid\" : \"EbRsJr8LSSO\", \"enrollmentDate\" : \"2022-07-02T02:00:00\", \"incidentDate\" : \"2022-07-08T02:00:00\", \"endDate\" : null, \"events\" : [{\"programStageUid\" : \"ZzYYXq4fJie\", \"programStageInstanceUid\" : \"nwe6hM3ZDJ0\", \"executionDate\" : null, \"dueDate\" : \"2021-07-14T02:00:00\", \"eventDataValues\" : {}}, {\"programStageUid\" : \"A03MvHHogjR\", \"programStageInstanceUid\" : \"zfzS9WeO0uM\", \"executionDate\" : \"2021-07-03T00:00:00\", \"dueDate\" : \"2021-07-23T12:46:11.472\", \"eventDataValues\" : {\"UXz7xuGCEhU\": {\"value\": \"1231\", \"created\": \"2014-07-23T12:46:04.45\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:04.45\", \"providedElsewhere\": false}, \"X8zyunlgUfM\": {\"value\": \"Replacement\", \"created\": \"2014-07-23T12:46:11.485\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:11.485\", \"providedElsewhere\": false}, \"a3kGcGDCuk6\": {\"value\": \"34\", \"created\": \"2014-07-23T12:45:58.15\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:45:58.15\", \"providedElsewhere\": false}, \"bx6fsa0t90x\": {\"value\": \"Yes\", \"created\": \"2014-07-23T12:46:07.795\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:07.795\", \"providedElsewhere\": false}, \"ebaJjqltK5N\": {\"value\": \"1\", \"created\": \"2014-07-23T12:46:09.026\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:09.026\", \"providedElsewhere\": false}, \"wQLfBvPrXqq\": {\"value\": \"NVP only\", \"created\": \"2014-07-23T12:46:06.385\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:06.385\", \"providedElsewhere\": false}}}, {\"programStageUid\" : \"ZzYYXq4fJie\", \"programStageInstanceUid\" : \"hUQEgo9b5dz\", \"executionDate\" : \"2021-07-24T00:00:00\", \"dueDate\" : \"2021-07-23T12:47:03.466\", \"eventDataValues\" : {\"BeynU4L6VCQ\": {\"value\": \"No\", \"created\": \"2014-07-23T12:46:53.337\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:53.337\", \"providedElsewhere\": false}, \"GQY2lXrypjO\": {\"value\": \"1214\", \"created\": \"2014-07-23T12:46:22.459\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:22.459\", \"providedElsewhere\": false}, \"HLmTEmupdX0\": {\"value\": \"No\", \"created\": \"2014-07-23T12:46:30.081\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:30.081\", \"providedElsewhere\": false}, \"OuJ6sgPyAbC\": {\"value\": \"High blood pressure.\", \"created\": \"2014-07-23T12:47:03.479\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:47:03.479\", \"providedElsewhere\": false}, \"X8zyunlgUfM\": {\"value\": \"Replacement\", \"created\": \"2014-07-23T12:46:23.958\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:23.958\", \"providedElsewhere\": false}, \"aei1xRjSU2l\": {\"value\": \"Yes\", \"created\": \"2014-07-23T12:46:52.451\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:52.451\", \"providedElsewhere\": false}, \"bx6fsa0t90x\": {\"value\": \"Yes\", \"created\": \"2014-07-23T12:46:25.026\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:25.026\", \"providedElsewhere\": false}, \"cYGaxwK615G\": {\"value\": \"Positive\", \"created\": \"2014-07-23T12:46:37.473\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:37.473\", \"providedElsewhere\": false}, \"ebaJjqltK5N\": {\"value\": \"1\", \"created\": \"2014-07-23T12:46:26.192\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:26.192\", \"providedElsewhere\": false}, \"hDZbpskhqDd\": {\"value\": \"Blood\", \"created\": \"2014-07-23T12:46:47.84\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:47.84\", \"providedElsewhere\": false}, \"pOe0ogW4OWd\": {\"value\": \"12\", \"created\": \"2014-07-23T12:46:28.192\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:28.192\", \"providedElsewhere\": false}, \"sj3j9Hwc7so\": {\"value\": \"TDF/3TC/EFV - 1\", \"created\": \"2014-07-23T12:46:51.127\", \"storedBy\": null, \"lastUpdated\": \"2014-07-23T12:46:51.127\", \"providedElsewhere\": false}}}]}]",
                "",
                "Evelyn",
                "Jackson",
                "Female" ) );

        validateRow( response, 1,
            List.of( "mYyHxkNAOr2",
                "2014-09-23 20:01:44.961",
                "",
                "",
                "",
                "",
                "",
                "Ngelehun CHC",
                "OU_559",
                "[{\"programUid\" : \"IpHINAT79UW\", \"programInstanceUid\" : \"LZg2cOvrAlU\", \"enrollmentDate\" : \"2022-09-04T02:00:00\", \"incidentDate\" : \"2002-09-07T02:00:00\", \"endDate\" : null, \"events\" : [{\"programStageUid\" : \"A03MvHHogjR\", \"programStageInstanceUid\" : \"ea2QqDBbvMX\", \"executionDate\" : \"2021-05-22T00:00:00\", \"dueDate\" : \"2014-10-02T00:00:00\", \"eventDataValues\" : {\"UXz7xuGCEhU\": {\"value\": \"3500\", \"created\": \"2014-11-15T19:11:25.977\", \"storedBy\": null, \"lastUpdated\": \"2014-11-15T19:11:25.977\", \"providedElsewhere\": false}, \"X8zyunlgUfM\": {\"value\": \"Replacement\", \"created\": \"2014-11-15T19:11:30.088\", \"storedBy\": null, \"lastUpdated\": \"2014-11-15T19:11:30.088\", \"providedElsewhere\": false}, \"a3kGcGDCuk6\": {\"value\": \"5\", \"created\": \"2014-11-15T19:11:22.476\", \"storedBy\": null, \"lastUpdated\": \"2014-11-15T19:11:22.476\", \"providedElsewhere\": false}, \"bx6fsa0t90x\": {\"value\": \"Yes\", \"created\": \"2014-11-15T19:11:28.148\", \"storedBy\": null, \"lastUpdated\": \"2014-11-15T19:11:28.148\", \"providedElsewhere\": false}, \"ebaJjqltK5N\": {\"value\": \"2\", \"created\": \"2014-11-15T19:11:29.018\", \"storedBy\": null, \"lastUpdated\": \"2014-11-15T19:11:29.018\", \"providedElsewhere\": false}, \"wQLfBvPrXqq\": {\"value\": \"Others\", \"created\": \"2014-11-15T19:11:27.329\", \"storedBy\": null, \"lastUpdated\": \"2014-11-15T19:11:27.329\", \"providedElsewhere\": false}}}, {\"programStageUid\" : \"ZzYYXq4fJie\", \"programStageInstanceUid\" : \"V2f1EhZch8x\", \"executionDate\" : null, \"dueDate\" : \"2001-09-13T02:00:00\", \"eventDataValues\" : {}}, {\"programStageUid\" : \"ZzYYXq4fJie\", \"programStageInstanceUid\" : \"QHde4ozqiNl\", \"executionDate\" : null, \"dueDate\" : \"2020-10-02T00:00:00\", \"eventDataValues\" : {}}]}]",
                "",
                "John",
                "Thomson",
                "Female" ) );

        validateRow( response, 2,
            List.of( "SBjuNw0Xtkn",
                "2014-10-01 12:27:37.837",
                "",
                "",
                "",
                "",
                "",
                "Ngelehun CHC",
                "OU_559",
                "[{\"programUid\" : \"IpHINAT79UW\", \"programInstanceUid\" : \"SAWQe5hyhy0\", \"enrollmentDate\" : \"2022-09-01T02:00:00\", \"incidentDate\" : \"2022-10-01T12:27:37.81\", \"endDate\" : null, \"events\" : [{\"programStageUid\" : \"ZzYYXq4fJie\", \"programStageInstanceUid\" : \"NFKC68Jne6p\", \"executionDate\" : \"2021-10-01T00:00:00\", \"dueDate\" : \"2021-10-01T12:28:04.391\", \"eventDataValues\" : {\"GQY2lXrypjO\": {\"value\": \"12\", \"created\": \"2014-10-01T12:27:51.597\", \"storedBy\": null, \"lastUpdated\": \"2014-10-01T12:27:51.597\", \"providedElsewhere\": false}, \"HLmTEmupdX0\": {\"value\": \"[object Object]\", \"created\": \"2014-10-01T12:28:04.451\", \"storedBy\": null, \"lastUpdated\": \"2014-10-01T12:28:04.451\", \"providedElsewhere\": false}, \"X8zyunlgUfM\": {\"value\": \"[object Object]\", \"created\": \"2014-10-01T12:27:53.271\", \"storedBy\": null, \"lastUpdated\": \"2014-10-01T12:27:53.271\", \"providedElsewhere\": false}, \"bx6fsa0t90x\": {\"value\": \"[object Object]\", \"created\": \"2014-10-01T12:27:54.836\", \"storedBy\": null, \"lastUpdated\": \"2014-10-01T12:27:54.836\", \"providedElsewhere\": false}, \"ebaJjqltK5N\": {\"value\": \"[object Object]\", \"created\": \"2014-10-01T12:27:56.022\", \"storedBy\": null, \"lastUpdated\": \"2014-10-01T12:27:56.022\", \"providedElsewhere\": false}, \"pOe0ogW4OWd\": {\"value\": \"[object Object]\", \"created\": \"2014-10-01T12:27:58.159\", \"storedBy\": null, \"lastUpdated\": \"2014-10-01T12:27:58.159\", \"providedElsewhere\": false}}}, {\"programStageUid\" : \"ZzYYXq4fJie\", \"programStageInstanceUid\" : \"ljdXg3yD3qr\", \"executionDate\" : null, \"dueDate\" : \"2021-10-07T12:27:37.81\", \"eventDataValues\" : {}}, {\"programStageUid\" : \"A03MvHHogjR\", \"programStageInstanceUid\" : \"b0pZPsbHh0R\", \"executionDate\" : null, \"dueDate\" : \"2021-10-01T12:27:37.81\", \"eventDataValues\" : {}}]}]",
                "",
                "Tom",
                "Johson",
                "" ) );
    }

    @Test
    public void queryWithProgramAndManyParams()
    {
        // Given
        QueryParamsBuilder params = new QueryParamsBuilder()
            .add( "program=IpHINAT79UW" )
            .add( "displayProperty=NAME" )
            .add( "includeMetadataDetails=true" )
            .add( "showHierarchy=true" )
            .add( "hierarchyMeta=true" )
            .add( "dimension=cejWyOfXge6" )
            .add( "headers=ouname,cejWyOfXge6,w75KJ2mc4zz,trackedentityinstanceuid,lastupdated,oucode" )
            .add( "desc=lastupdated" )
            .add( "relativePeriodDate=2022-09-27" );

        // When
        ApiResponse response = analyticsTeiActions.query().get( "nEenWmSyUEp", JSON, JSON, params );

        // Then
        response.validate()
            .statusCode( 200 )
            .body( "headers", hasSize( equalTo( 6 ) ) )
            .body( "rows", hasSize( equalTo( 50 ) ) )
            .body( "metaData.pager.page", equalTo( 1 ) )
            .body( "metaData.pager.pageSize", equalTo( 50 ) )
            .body( "metaData.pager.isLastPage", is( false ) )
            .body( "metaData.pager", not( hasKey( "total" ) ) )
            .body( "metaData.pager", not( hasKey( "pageCount" ) ) )
            .body( "metaData.items.ImspTQPwCqd.name", equalTo( null ) )
            .body( "metaData.items.lZGmxYbs97q.name", equalTo( "Unique ID" ) )
            .body( "metaData.items.zDhUuAYrxNC.name", equalTo( "Last name" ) )
            .body( "metaData.items.IpHINAT79UW.name", equalTo( "Child Programme" ) )
            .body( "metaData.items.ZzYYXq4fJie.name", equalTo( "Baby Postnatal" ) )
            .body( "metaData.items.w75KJ2mc4zz.name", equalTo( "First name" ) )
            .body( "metaData.items.A03MvHHogjR.name", equalTo( "Birth" ) )
            .body( "metaData.items.cejWyOfXge6.name", equalTo( "Gender" ) )
            .body( "metaData.items.ou.name", equalTo( null ) )
            .body( "metaData.dimensions", hasKey( "lZGmxYbs97q" ) )
            .body( "metaData.dimensions", hasKey( "zDhUuAYrxNC" ) )
            .body( "metaData.dimensions", hasKey( "pe" ) )
            .body( "metaData.dimensions", hasKey( "w75KJ2mc4zz" ) )
            .body( "metaData.dimensions", hasKey( "cejWyOfXge6" ) )
            .body( "metaData.dimensions", not( hasKey( "ou" ) ) )
            .body( "height", equalTo( 50 ) )
            .body( "width", equalTo( 6 ) )
            .body( "headerWidth", equalTo( 6 ) );

        // Validate headers
        validateHeader( response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 1, "cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 2, "w75KJ2mc4zz", "First name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 3, "trackedentityinstanceuid", "Tracked Entity Instance", "TEXT", "java.lang.String",
            false, true );
        validateHeader( response, 4, "lastupdated", "Last Updated", "DATETIME", "java.time.LocalDateTime", false,
            true );
        validateHeader( response, 5, "oucode", "Organisation unit code", "TEXT", "java.lang.String", false, true );

        // Validate the first three rows, as samples.
        validateRow( response, 0,
            List.of( "Ngelehun CHC",
                "Female",
                "Filona",
                "vOxUH373fy5",
                "2017-05-26 11:46:22.372",
                "OU_559" ) );

        validateRow( response, 1,
            List.of( "Ngelehun CHC",
                "Male",
                "Frank",
                "lkuI9OgwfOc",
                "2017-01-20 10:41:45.624",
                "OU_559" ) );

        validateRow( response, 2,
            List.of( "Ngelehun CHC",
                "Female",
                "Gertrude",
                "pybd813kIWx",
                "2017-01-20 10:40:31.913",
                "OU_559" ) );
    }

    @Test
    public void queryWithProgramDimensionAndFilter()
    {
        // Given
        QueryParamsBuilder params = new QueryParamsBuilder()
            .add( "program=IpHINAT79UW" )
            .add( "dimension=ouname,IpHINAT79UW.w75KJ2mc4zz:eq:James" )
            .add( "includeMetadataDetails=false" )
            .add( "headers=ouname,w75KJ2mc4zz,lastupdated" )
            .add( "asc=lastupdated" )
            .add( "relativePeriodDate=2022-09-27" );

        // When
        ApiResponse response = analyticsTeiActions.query().get( "nEenWmSyUEp", JSON, JSON, params );

        // Then
        response.validate()
            .statusCode( 200 )
            .body( "headers", hasSize( equalTo( 3 ) ) )
            .body( "rows", hasSize( equalTo( 50 ) ) )
            .body( "metaData.pager.page", equalTo( 1 ) )
            .body( "metaData.pager.pageSize", equalTo( 50 ) )
            .body( "metaData.pager.isLastPage", is( false ) )
            .body( "metaData.pager", not( hasKey( "total" ) ) )
            .body( "metaData.pager", not( hasKey( "pageCount" ) ) )
            .body( "metaData.items.ImspTQPwCqd.name", equalTo( null ) )
            .body( "metaData.items.lZGmxYbs97q.name", equalTo( "Unique ID" ) )
            .body( "metaData.items.zDhUuAYrxNC.name", equalTo( "Last name" ) )
            .body( "metaData.items.IpHINAT79UW.name", equalTo( "Child Programme" ) )
            .body( "metaData.items.ZzYYXq4fJie.name", equalTo( "Baby Postnatal" ) )
            .body( "metaData.items.w75KJ2mc4zz.name", equalTo( "First name" ) )
            .body( "metaData.items.A03MvHHogjR.name", equalTo( "Birth" ) )
            .body( "metaData.items.cejWyOfXge6.name", equalTo( "Gender" ) )
            .body( "metaData.items.ou.name", equalTo( null ) )
            .body( "metaData.dimensions", hasKey( "lZGmxYbs97q" ) )
            .body( "metaData.dimensions", hasKey( "zDhUuAYrxNC" ) )
            .body( "metaData.dimensions", hasKey( "pe" ) )
            .body( "metaData.dimensions", hasKey( "w75KJ2mc4zz" ) )
            .body( "metaData.dimensions", hasKey( "cejWyOfXge6" ) )
            .body( "metaData.dimensions", not( hasKey( "ouname" ) ) )
            .body( "height", equalTo( 50 ) )
            .body( "width", equalTo( 3 ) )
            .body( "headerWidth", equalTo( 3 ) );

        // Validate headers
        validateHeader( response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 1, "w75KJ2mc4zz", "First name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 2, "lastupdated", "Last Updated", "DATETIME", "java.time.LocalDateTime", false,
            true );

        // Validate the first three rows, as samples.
        validateRow( response, 0,
            List.of( "Ngelehun CHC",
                "James",
                "2014-11-15 21:19:09.35" ) );

        validateRow( response, 1,
            List.of( "Bambara MCHP",
                "James",
                "2015-08-06 21:12:34.395" ) );

        validateRow( response, 2,
            List.of( "Moyollo MCHP",
                "James",
                "2015-08-06 21:12:35.736" ) );
    }

    @Test
    public void queryWithProgramAndMultipleStaticDimOrdering()
    {
        // Given
        QueryParamsBuilder params = new QueryParamsBuilder()
            .add( "program=IpHINAT79UW" )
            .add( "desc=lastupdated,ouname" )
            .add( "headers=ouname,lastupdated" )
            .add( "relativePeriodDate=2022-09-27" );

        // When
        ApiResponse response = analyticsTeiActions.query().get( "nEenWmSyUEp", JSON, JSON, params );

        // Then
        response.validate()
            .statusCode( 200 )
            .body( "headers", hasSize( equalTo( 2 ) ) )
            .body( "rows", hasSize( equalTo( 50 ) ) )
            .body( "height", equalTo( 50 ) )
            .body( "width", equalTo( 2 ) )
            .body( "headerWidth", equalTo( 2 ) );

        // Validate headers
        validateHeader( response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 1, "lastupdated", "Last Updated", "DATETIME", "java.time.LocalDateTime", false,
            true );

        // Validate the first three rows, as samples.
        validateRow( response, 0,
            List.of( "Ngelehun CHC",
                "2017-05-26 11:46:22.372" ) );

        validateRow( response, 1,
            List.of( "Ngelehun CHC",
                "2017-01-20 10:41:45.624" ) );

        validateRow( response, 2,
            List.of( "Ngelehun CHC",
                "2017-01-20 10:40:31.913" ) );
    }

    @Test
    public void queryWithProgramAndMultipleDynamicDimOrdering()
    {
        // Given
        QueryParamsBuilder params = new QueryParamsBuilder()
            .add( "program=IpHINAT79UW" )
            .add( "desc=IpHINAT79UW.w75KJ2mc4zz,IpHINAT79UW.zDhUuAYrxNC" )
            .add( "headers=w75KJ2mc4zz,zDhUuAYrxNC" )
            .add( "relativePeriodDate=2022-09-27" );

        // When
        ApiResponse response = analyticsTeiActions.query().get( "nEenWmSyUEp", JSON, JSON, params );

        // Then
        response.validate()
            .statusCode( 200 )
            .body( "headers", hasSize( equalTo( 2 ) ) )
            .body( "rows", hasSize( equalTo( 50 ) ) )
            .body( "height", equalTo( 50 ) )
            .body( "width", equalTo( 2 ) )
            .body( "headerWidth", equalTo( 2 ) );

        // Validate headers
        validateHeader( response, 0, "w75KJ2mc4zz", "First name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 1, "zDhUuAYrxNC", "Last name", "TEXT", "java.lang.String", false, true );

        // Validate the first three rows, as samples.
        validateRow( response, 0,
            List.of( "Willie",
                "Woods" ) );

        validateRow( response, 1,
            List.of( "Willie",
                "Williams" ) );

        validateRow( response, 2,
            List.of( "Willie",
                "Williams" ) );
    }

    @Test
    public void queryWithProgramAndEnrollmentStaticDimOrdering()
    {
        // Given
        QueryParamsBuilder params = new QueryParamsBuilder()
            .add( "program=IpHINAT79UW" )
            .add( "desc=IpHINAT79UW.A03MvHHogjR.ouname" )
            .add( "headers=ouname,enrollments,lZGmxYbs97q" )
            .add( "relativePeriodDate=2022-09-27" );

        // When
        ApiResponse response = analyticsTeiActions.query().get( "nEenWmSyUEp", JSON, JSON, params );

        // Then
        response.validate()
            .statusCode( 200 )
            .body( "headers", hasSize( equalTo( 3 ) ) )
            .body( "rows", hasSize( equalTo( 50 ) ) )
            .body( "height", equalTo( 50 ) )
            .body( "width", equalTo( 3 ) )
            .body( "headerWidth", equalTo( 3 ) );

        // Validate headers
        validateHeader( response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 1, "enrollments", "Enrollments", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 2, "lZGmxYbs97q", "Unique ID", "TEXT", "java.lang.String", false, true );

        // Validate the first three rows, as samples.
        validateRow( response, 0,
            List.of( "Batkanu CHC",
                "[{\"programUid\" : \"IpHINAT79UW\", \"programInstanceUid\" : \"OLqYNFlxbmO\", \"enrollmentDate\" : \"2022-04-01T12:05:00\", \"incidentDate\" : \"2022-04-01T12:05:00\", \"endDate\" : null, \"events\" : [{\"programStageUid\" : \"A03MvHHogjR\", \"programStageInstanceUid\" : \"YLx9XZhI5Ww\", \"executionDate\" : \"2021-04-01T00:00:00\", \"dueDate\" : \"2021-04-01T12:05:00\", \"eventDataValues\" : {\"UXz7xuGCEhU\": {\"value\": \"3010\", \"created\": \"2014-04-01T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-04-01T12:05:00\", \"providedElsewhere\": false}, \"X8zyunlgUfM\": {\"value\": \"Replacement\", \"created\": \"2014-04-01T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-04-01T12:05:00\", \"providedElsewhere\": false}, \"a3kGcGDCuk6\": {\"value\": \"0\", \"created\": \"2014-04-01T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-04-01T12:05:00\", \"providedElsewhere\": false}, \"bx6fsa0t90x\": {\"value\": \"true\", \"created\": \"2014-04-01T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-04-01T12:05:00\", \"providedElsewhere\": false}, \"ebaJjqltK5N\": {\"value\": \"2\", \"created\": \"2014-04-01T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-04-01T12:05:00\", \"providedElsewhere\": false}, \"wQLfBvPrXqq\": {\"value\": \"NVP only\", \"created\": \"2014-04-01T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-04-01T12:05:00\", \"providedElsewhere\": false}}}, {\"programStageUid\" : \"ZzYYXq4fJie\", \"programStageInstanceUid\" : \"qtinwtzW27a\", \"executionDate\" : \"2021-04-22T00:00:00\", \"dueDate\" : \"2021-04-22T12:05:00\", \"eventDataValues\" : {\"FqlgKAG8HOu\": {\"value\": \"true\", \"created\": \"2014-04-01T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-04-01T12:05:00\", \"providedElsewhere\": false}, \"GQY2lXrypjO\": {\"value\": \"3564\", \"created\": \"2014-04-01T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-04-01T12:05:00\", \"providedElsewhere\": false}, \"HLmTEmupdX0\": {\"value\": \"true\", \"created\": \"2014-04-01T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-04-01T12:05:00\", \"providedElsewhere\": false}, \"X8zyunlgUfM\": {\"value\": \"Exclusive\", \"created\": \"2014-04-01T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-04-01T12:05:00\", \"providedElsewhere\": false}, \"cYGaxwK615G\": {\"value\": \"Postive √\", \"created\": \"2014-04-01T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-04-01T12:05:00\", \"providedElsewhere\": false}, \"hDZbpskhqDd\": {\"value\": \"PCR\", \"created\": \"2014-04-01T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-04-01T12:05:00\", \"providedElsewhere\": false}, \"lNNb3truQoi\": {\"value\": \"IPT 1\", \"created\": \"2014-04-01T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-04-01T12:05:00\", \"providedElsewhere\": false}, \"pOe0ogW4OWd\": {\"value\": \"1\", \"created\": \"2014-04-01T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-04-01T12:05:00\", \"providedElsewhere\": false}, \"rxBfISxXS2U\": {\"value\": \"false\", \"created\": \"2014-04-01T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-04-01T12:05:00\", \"providedElsewhere\": false}, \"sj3j9Hwc7so\": {\"value\": \"AZT/3TC/EFV - 1\", \"created\": \"2014-04-01T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-04-01T12:05:00\", \"providedElsewhere\": false}, \"vTUhAUZFoys\": {\"value\": \"3\", \"created\": \"2014-04-01T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-04-01T12:05:00\", \"providedElsewhere\": false}}}]}]",
                "" ) );

        validateRow( response, 1,
            List.of( "Fatkom Muchendeh Maternity Clinic",
                "[{\"programUid\" : \"IpHINAT79UW\", \"programInstanceUid\" : \"ujgm3JCJJe6\", \"enrollmentDate\" : \"2022-10-20T12:05:00\", \"incidentDate\" : \"2022-10-20T12:05:00\", \"endDate\" : null, \"events\" : [{\"programStageUid\" : \"A03MvHHogjR\", \"programStageInstanceUid\" : \"tYwEr597zdI\", \"executionDate\" : \"2021-10-20T00:00:00\", \"dueDate\" : \"2021-10-20T12:05:00\", \"eventDataValues\" : {\"UXz7xuGCEhU\": {\"value\": \"2649\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"X8zyunlgUfM\": {\"value\": \"Replacement\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"a3kGcGDCuk6\": {\"value\": \"0\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"bx6fsa0t90x\": {\"value\": \"false\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"ebaJjqltK5N\": {\"value\": \"1\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"wQLfBvPrXqq\": {\"value\": \"NVP only\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}}}, {\"programStageUid\" : \"ZzYYXq4fJie\", \"programStageInstanceUid\" : \"g75nKe4alnv\", \"executionDate\" : \"2021-11-10T00:00:00\", \"dueDate\" : \"2021-11-10T12:05:00\", \"eventDataValues\" : {\"FqlgKAG8HOu\": {\"value\": \"true\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"GQY2lXrypjO\": {\"value\": \"3287\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"HLmTEmupdX0\": {\"value\": \"false\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"X8zyunlgUfM\": {\"value\": \"Mixed\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"cYGaxwK615G\": {\"value\": \"Positive\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"hDZbpskhqDd\": {\"value\": \"PCR\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"lNNb3truQoi\": {\"value\": \"On CTX\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"pOe0ogW4OWd\": {\"value\": \"3\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"rxBfISxXS2U\": {\"value\": \"true\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"sj3j9Hwc7so\": {\"value\": \"TDF/3TC/NVP - 1\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"vTUhAUZFoys\": {\"value\": \"2\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}}}]}]",
                "" ) );

        validateRow( response, 2,
            List.of( "Mokotawa CHP",
                "[{\"programUid\" : \"IpHINAT79UW\", \"programInstanceUid\" : \"pe0mBzatZSV\", \"enrollmentDate\" : \"2022-11-18T12:05:00\", \"incidentDate\" : \"2022-11-18T12:05:00\", \"endDate\" : null, \"events\" : [{\"programStageUid\" : \"A03MvHHogjR\", \"programStageInstanceUid\" : \"joGI0xa6JRg\", \"executionDate\" : \"2021-11-18T00:00:00\", \"dueDate\" : \"2021-11-18T12:05:00\", \"eventDataValues\" : {\"UXz7xuGCEhU\": {\"value\": \"2661\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"X8zyunlgUfM\": {\"value\": \"Replacement\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"a3kGcGDCuk6\": {\"value\": \"2\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"bx6fsa0t90x\": {\"value\": \"true\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"ebaJjqltK5N\": {\"value\": \"3\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"wQLfBvPrXqq\": {\"value\": \"NVP only\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}}}, {\"programStageUid\" : \"ZzYYXq4fJie\", \"programStageInstanceUid\" : \"otfIgxEBhed\", \"executionDate\" : \"2021-12-09T00:00:00\", \"dueDate\" : \"2021-12-09T12:05:00\", \"eventDataValues\" : {\"FqlgKAG8HOu\": {\"value\": \"true\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"GQY2lXrypjO\": {\"value\": \"3540\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"HLmTEmupdX0\": {\"value\": \"true\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"X8zyunlgUfM\": {\"value\": \"Exclusive\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"cYGaxwK615G\": {\"value\": \"Postive √\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"hDZbpskhqDd\": {\"value\": \"Rapid\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"lNNb3truQoi\": {\"value\": \"IPT 1\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"pOe0ogW4OWd\": {\"value\": \"1\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"rxBfISxXS2U\": {\"value\": \"false\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"sj3j9Hwc7so\": {\"value\": \"NVP Only\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"vTUhAUZFoys\": {\"value\": \"3\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}}}]}]",
                "" ) );
    }

    @Test
    public void queryWithProgramAndMultipleEventDimOrdering()
    {
        // Given
        QueryParamsBuilder params = new QueryParamsBuilder()
            .add( "program=IpHINAT79UW" )
            .add( "desc=IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6" )
            .add( "headers=ouname,enrollments,lZGmxYbs97q" )
            .add( "relativePeriodDate=2022-09-27" );

        // When
        ApiResponse response = analyticsTeiActions.query().get( "nEenWmSyUEp", JSON, JSON, params );

        // Then
        response.validate()
            .statusCode( 200 )
            .body( "headers", hasSize( equalTo( 3 ) ) )
            .body( "rows", hasSize( equalTo( 50 ) ) )
            .body( "height", equalTo( 50 ) )
            .body( "width", equalTo( 3 ) )
            .body( "headerWidth", equalTo( 3 ) );

        // Validate headers
        validateHeader( response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 1, "enrollments", "Enrollments", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 2, "lZGmxYbs97q", "Unique ID", "TEXT", "java.lang.String", false, true );

        // Validate the first three rows, as samples.
        validateRow( response, 0,
            List.of( "Ngelehun CHC",
                "[{\"programUid\" : \"IpHINAT79UW\", \"programInstanceUid\" : \"KxXkjF6buFN\", \"enrollmentDate\" : \"2022-04-02T02:00:00\", \"incidentDate\" : \"2022-04-02T02:00:00\", \"endDate\" : \"2022-11-16T12:26:42.851\", \"events\" : [{\"programStageUid\" : \"ZzYYXq4fJie\", \"programStageInstanceUid\" : \"v3qObCbW6zn\", \"executionDate\" : null, \"dueDate\" : \"2021-04-08T02:00:00\", \"eventDataValues\" : {}}, {\"programStageUid\" : \"ZzYYXq4fJie\", \"programStageInstanceUid\" : \"vF0YkkC3JD3\", \"executionDate\" : \"2021-05-01T00:00:00\", \"dueDate\" : \"2007-01-07T00:00:00\", \"eventDataValues\" : {\"BeynU4L6VCQ\": {\"value\": \"Unknown\", \"created\": \"2014-11-16T12:26:32.419\", \"storedBy\": null, \"lastUpdated\": \"2014-11-16T12:26:32.419\", \"providedElsewhere\": false}, \"GQY2lXrypjO\": {\"value\": \"2341\", \"created\": \"2014-11-16T12:26:15.31\", \"storedBy\": null, \"lastUpdated\": \"2014-11-16T12:26:15.31\", \"providedElsewhere\": false}, \"HLmTEmupdX0\": {\"value\": \"No\", \"created\": \"2014-11-16T12:26:20.746\", \"storedBy\": null, \"lastUpdated\": \"2014-11-16T12:26:20.746\", \"providedElsewhere\": false}, \"X8zyunlgUfM\": {\"value\": \"Replacement\", \"created\": \"2014-11-16T12:26:17.263\", \"storedBy\": null, \"lastUpdated\": \"2014-11-16T12:26:17.263\", \"providedElsewhere\": false}, \"aei1xRjSU2l\": {\"value\": \"No\", \"created\": \"2014-11-16T12:26:31.642\", \"storedBy\": null, \"lastUpdated\": \"2014-11-16T12:26:31.642\", \"providedElsewhere\": false}, \"bx6fsa0t90x\": {\"value\": \"Yes\", \"created\": \"2014-11-16T12:26:17.972\", \"storedBy\": null, \"lastUpdated\": \"2014-11-16T12:26:17.972\", \"providedElsewhere\": false}, \"cYGaxwK615G\": {\"value\": \"Positive\", \"created\": \"2014-11-16T12:26:21.807\", \"storedBy\": null, \"lastUpdated\": \"2014-11-16T12:26:21.807\", \"providedElsewhere\": false}, \"ebaJjqltK5N\": {\"value\": \"1\", \"created\": \"2014-11-16T12:26:18.782\", \"storedBy\": null, \"lastUpdated\": \"2014-11-16T12:26:18.782\", \"providedElsewhere\": false}, \"hDZbpskhqDd\": {\"value\": \"Rapid\", \"created\": \"2014-11-16T12:26:23.724\", \"storedBy\": null, \"lastUpdated\": \"2014-11-16T12:26:23.724\", \"providedElsewhere\": false}, \"pOe0ogW4OWd\": {\"value\": \"2\", \"created\": \"2014-11-16T12:26:19.482\", \"storedBy\": null, \"lastUpdated\": \"2014-11-16T12:26:19.482\", \"providedElsewhere\": false}, \"sj3j9Hwc7so\": {\"value\": \"TDF/3TC/EFV - 1\", \"created\": \"2014-11-16T12:26:30.686\", \"storedBy\": null, \"lastUpdated\": \"2014-11-16T12:26:30.686\", \"providedElsewhere\": false}}}, {\"programStageUid\" : \"A03MvHHogjR\", \"programStageInstanceUid\" : \"nN7gGLW5Akv\", \"executionDate\" : \"2021-04-02T00:00:00\", \"dueDate\" : \"2007-01-01T00:00:00\", \"eventDataValues\" : {\"UXz7xuGCEhU\": {\"value\": \"2313\", \"created\": \"2014-11-16T12:25:49.243\", \"storedBy\": null, \"lastUpdated\": \"2014-11-16T12:25:49.243\", \"providedElsewhere\": false}, \"X8zyunlgUfM\": {\"value\": \"Replacement\", \"created\": \"2014-11-16T12:25:53.119\", \"storedBy\": null, \"lastUpdated\": \"2014-11-16T12:25:53.119\", \"providedElsewhere\": false}, \"a3kGcGDCuk6\": {\"value\": \"8\", \"created\": \"2014-11-16T12:25:46.981\", \"storedBy\": null, \"lastUpdated\": \"2014-11-16T12:25:46.981\", \"providedElsewhere\": false}, \"bx6fsa0t90x\": {\"value\": \"No\", \"created\": \"2014-11-16T12:25:51.662\", \"storedBy\": null, \"lastUpdated\": \"2014-11-16T12:25:51.662\", \"providedElsewhere\": false}, \"ebaJjqltK5N\": {\"value\": \"1\", \"created\": \"2014-11-16T12:25:52.416\", \"storedBy\": null, \"lastUpdated\": \"2014-11-16T12:25:52.416\", \"providedElsewhere\": false}, \"wQLfBvPrXqq\": {\"value\": \"Others\", \"created\": \"2014-11-16T12:25:50.749\", \"storedBy\": null, \"lastUpdated\": \"2014-11-16T12:25:50.749\", \"providedElsewhere\": false}}}]}, {\"programUid\" : \"IpHINAT79UW\", \"programInstanceUid\" : \"ncHGzdp28S6\", \"enrollmentDate\" : \"2023-05-01T02:00:00\", \"incidentDate\" : \"2023-05-07T02:00:00\", \"endDate\" : null, \"events\" : [{\"programStageUid\" : \"A03MvHHogjR\", \"programStageInstanceUid\" : \"kqICE1Bxlec\", \"executionDate\" : null, \"dueDate\" : \"2022-05-07T02:00:00\", \"eventDataValues\" : {}}, {\"programStageUid\" : \"ZzYYXq4fJie\", \"programStageInstanceUid\" : \"ku5MWWbSE8D\", \"executionDate\" : null, \"dueDate\" : \"2022-05-13T02:00:00\", \"eventDataValues\" : {}}, {\"programStageUid\" : \"ZzYYXq4fJie\", \"programStageInstanceUid\" : \"jh1GKd576CE\", \"executionDate\" : \"2022-09-04T00:00:00\", \"dueDate\" : \"2022-09-14T20:07:33.792\", \"eventDataValues\" : {\"BeynU4L6VCQ\": {\"value\": \"No\", \"created\": \"2015-09-14T20:07:20.457\", \"storedBy\": null, \"lastUpdated\": \"2015-09-14T20:07:20.457\", \"providedElsewhere\": false}, \"HLmTEmupdX0\": {\"value\": \"false\", \"created\": \"2015-09-14T20:07:07.034\", \"storedBy\": null, \"lastUpdated\": \"2015-09-14T20:07:07.034\", \"providedElsewhere\": false}, \"aei1xRjSU2l\": {\"value\": \"No\", \"created\": \"2015-09-14T20:07:18.912\", \"storedBy\": null, \"lastUpdated\": \"2015-09-14T20:07:18.912\", \"providedElsewhere\": false}, \"cYGaxwK615G\": {\"value\": \"Negative\", \"created\": \"2015-09-14T20:07:09.003\", \"storedBy\": null, \"lastUpdated\": \"2015-09-14T20:07:09.003\", \"providedElsewhere\": false}, \"hDZbpskhqDd\": {\"value\": \"PCR\", \"created\": \"2015-09-14T20:07:11.307\", \"storedBy\": null, \"lastUpdated\": \"2015-09-14T20:07:11.307\", \"providedElsewhere\": false}, \"lNNb3truQoi\": {\"value\": \"IPT 2\", \"created\": \"2015-09-14T20:07:00.182\", \"storedBy\": null, \"lastUpdated\": \"2015-09-14T20:07:00.182\", \"providedElsewhere\": false}, \"pOe0ogW4OWd\": {\"value\": \"2\", \"created\": \"2015-09-14T20:07:01.849\", \"storedBy\": null, \"lastUpdated\": \"2015-09-14T20:07:01.849\", \"providedElsewhere\": false}, \"rxBfISxXS2U\": {\"value\": \"false\", \"created\": \"2015-09-14T20:07:33.795\", \"storedBy\": null, \"lastUpdated\": \"2015-09-14T20:07:33.795\", \"providedElsewhere\": false}, \"sj3j9Hwc7so\": {\"value\": \"AZT/3TC/EFV - 1\", \"created\": \"2015-09-14T20:07:14.108\", \"storedBy\": null, \"lastUpdated\": \"2015-09-14T20:07:14.108\", \"providedElsewhere\": false}}}, {\"programStageUid\" : \"A03MvHHogjR\", \"programStageInstanceUid\" : \"lMADe4YpiEd\", \"executionDate\" : \"2022-09-09T00:00:00\", \"dueDate\" : \"2022-09-14T20:05:43.971\", \"eventDataValues\" : {\"bx6fsa0t90x\": {\"value\": \"true\", \"created\": \"2015-09-14T20:05:43.974\", \"storedBy\": null, \"lastUpdated\": \"2015-09-14T20:05:43.974\", \"providedElsewhere\": false}}}]}]",
                "" ) );

        validateRow( response, 1,
            List.of( "Ngelehun CHC",
                "[{\"programUid\" : \"IpHINAT79UW\", \"programInstanceUid\" : \"SAWQe5hyhy0\", \"enrollmentDate\" : \"2022-09-01T02:00:00\", \"incidentDate\" : \"2022-10-01T12:27:37.81\", \"endDate\" : null, \"events\" : [{\"programStageUid\" : \"ZzYYXq4fJie\", \"programStageInstanceUid\" : \"NFKC68Jne6p\", \"executionDate\" : \"2021-10-01T00:00:00\", \"dueDate\" : \"2021-10-01T12:28:04.391\", \"eventDataValues\" : {\"GQY2lXrypjO\": {\"value\": \"12\", \"created\": \"2014-10-01T12:27:51.597\", \"storedBy\": null, \"lastUpdated\": \"2014-10-01T12:27:51.597\", \"providedElsewhere\": false}, \"HLmTEmupdX0\": {\"value\": \"[object Object]\", \"created\": \"2014-10-01T12:28:04.451\", \"storedBy\": null, \"lastUpdated\": \"2014-10-01T12:28:04.451\", \"providedElsewhere\": false}, \"X8zyunlgUfM\": {\"value\": \"[object Object]\", \"created\": \"2014-10-01T12:27:53.271\", \"storedBy\": null, \"lastUpdated\": \"2014-10-01T12:27:53.271\", \"providedElsewhere\": false}, \"bx6fsa0t90x\": {\"value\": \"[object Object]\", \"created\": \"2014-10-01T12:27:54.836\", \"storedBy\": null, \"lastUpdated\": \"2014-10-01T12:27:54.836\", \"providedElsewhere\": false}, \"ebaJjqltK5N\": {\"value\": \"[object Object]\", \"created\": \"2014-10-01T12:27:56.022\", \"storedBy\": null, \"lastUpdated\": \"2014-10-01T12:27:56.022\", \"providedElsewhere\": false}, \"pOe0ogW4OWd\": {\"value\": \"[object Object]\", \"created\": \"2014-10-01T12:27:58.159\", \"storedBy\": null, \"lastUpdated\": \"2014-10-01T12:27:58.159\", \"providedElsewhere\": false}}}, {\"programStageUid\" : \"ZzYYXq4fJie\", \"programStageInstanceUid\" : \"ljdXg3yD3qr\", \"executionDate\" : null, \"dueDate\" : \"2021-10-07T12:27:37.81\", \"eventDataValues\" : {}}, {\"programStageUid\" : \"A03MvHHogjR\", \"programStageInstanceUid\" : \"b0pZPsbHh0R\", \"executionDate\" : null, \"dueDate\" : \"2021-10-01T12:27:37.81\", \"eventDataValues\" : {}}]}]",
                "" ) );

        validateRow( response, 2,
            List.of( "Ngelehun CHC",
                "[{\"programUid\" : \"IpHINAT79UW\", \"programInstanceUid\" : \"JMgRZyeLWOo\", \"enrollmentDate\" : \"2022-03-06T00:00:00\", \"incidentDate\" : \"2022-03-04T00:00:00\", \"endDate\" : null, \"events\" : [{\"programStageUid\" : \"A03MvHHogjR\", \"programStageInstanceUid\" : \"shZUPTRVhG1\", \"executionDate\" : null, \"dueDate\" : \"2021-03-04T00:00:00\", \"eventDataValues\" : {}}, {\"programStageUid\" : \"ZzYYXq4fJie\", \"programStageInstanceUid\" : \"Zq2dg6pTNoj\", \"executionDate\" : null, \"dueDate\" : \"2021-03-10T00:00:00\", \"eventDataValues\" : {}}]}, {\"programUid\" : \"ur1Edk5Oe2n\", \"programInstanceUid\" : \"Yf47yST5FF2\", \"enrollmentDate\" : \"2022-02-09T12:27:48.637\", \"incidentDate\" : \"2022-01-29T12:27:48.637\", \"endDate\" : null, \"events\" : [{\"programStageUid\" : \"ZkbAXlQUYJG\", \"programStageInstanceUid\" : \"blZxytttTqq\", \"executionDate\" : \"2021-11-02T00:00:00\", \"dueDate\" : \"2022-08-05T19:25:49.996\", \"eventDataValues\" : {\"D7m8vpzxHDJ\": {\"value\": \"P+\", \"created\": \"2014-11-03T10:32:21.862\", \"storedBy\": null, \"lastUpdated\": \"2014-11-03T10:32:21.862\", \"providedElsewhere\": false}, \"HmkXnHJxcD1\": {\"value\": \"Relapse\", \"created\": \"2014-11-03T10:32:24.858\", \"storedBy\": null, \"lastUpdated\": \"2014-11-03T10:32:24.858\", \"providedElsewhere\": false}}}, {\"programStageUid\" : \"EPEcjy3FWmI\", \"programStageInstanceUid\" : \"ZdRPhMckeJk\", \"executionDate\" : \"2022-08-02T00:00:00\", \"dueDate\" : \"2022-08-02T19:32:28.998\", \"eventDataValues\" : {\"Vk1tzSQxvOR\": {\"value\": \"true\", \"created\": \"2016-08-02T19:32:27.902\", \"storedBy\": \"system\", \"lastUpdated\": \"2016-08-02T19:32:27.902\", \"providedElsewhere\": false}, \"fCXKBdc27Bt\": {\"value\": \"true\", \"created\": \"2016-08-02T19:32:28.395\", \"storedBy\": \"system\", \"lastUpdated\": \"2016-08-02T19:32:28.395\", \"providedElsewhere\": false}, \"lJTx9EZ1dk1\": {\"value\": \"true\", \"created\": \"2016-08-02T19:32:29.001\", \"storedBy\": \"system\", \"lastUpdated\": \"2016-08-02T19:32:29.001\", \"providedElsewhere\": false}}}, {\"programStageUid\" : \"jdRD35YwbRH\", \"programStageInstanceUid\" : \"DSKhD4VgQPQ\", \"executionDate\" : \"2022-08-02T00:00:00\", \"dueDate\" : \"2022-08-02T19:32:42.022\", \"eventDataValues\" : {\"yLIPuJHRgey\": {\"value\": \"123\", \"created\": \"2016-08-02T19:32:40.837\", \"storedBy\": \"system\", \"lastUpdated\": \"2016-08-02T19:32:40.837\", \"providedElsewhere\": false}, \"zocHNQIQBIN\": {\"value\": \"NEG\", \"created\": \"2016-08-02T19:32:42.029\", \"storedBy\": \"system\", \"lastUpdated\": \"2016-08-02T19:32:42.029\", \"providedElsewhere\": false}}}, {\"programStageUid\" : \"jdRD35YwbRH\", \"programStageInstanceUid\" : \"diebn6sFaCy\", \"executionDate\" : \"2022-08-02T00:00:00\", \"dueDate\" : \"2022-08-02T19:32:14.939\", \"eventDataValues\" : {\"yLIPuJHRgey\": {\"value\": \"123\", \"created\": \"2016-08-02T19:32:11.962\", \"storedBy\": \"system\", \"lastUpdated\": \"2016-08-02T19:32:11.962\", \"providedElsewhere\": false}, \"zocHNQIQBIN\": {\"value\": \"POS\", \"created\": \"2016-08-02T19:32:14.946\", \"storedBy\": \"system\", \"lastUpdated\": \"2016-08-02T19:32:14.946\", \"providedElsewhere\": false}}}, {\"programStageUid\" : \"ZkbAXlQUYJG\", \"programStageInstanceUid\" : \"qEQkB7ceMNh\", \"executionDate\" : null, \"dueDate\" : \"2021-01-29T12:27:48.637\", \"eventDataValues\" : {}}]}]",
                "" ) );
    }

    @Test
    public void queryWithProgramAndProgramIndicatorOrdering()
    {
        // Given
        QueryParamsBuilder params = new QueryParamsBuilder()
            .add( "program=IpHINAT79UW" )
            .add( "desc=IpHINAT79UW.A03MvHHogjR.p2Zxg0wcPQ3" )
            .add( "headers=ouname,enrollments,lZGmxYbs97q" )
            .add( "relativePeriodDate=2022-09-27" );

        // When
        ApiResponse response = analyticsTeiActions.query().get( "nEenWmSyUEp", JSON, JSON, params );

        // Then
        response.validate()
            .statusCode( 200 )
            .body( "headers", hasSize( equalTo( 3 ) ) )
            .body( "rows", hasSize( equalTo( 50 ) ) )
            .body( "height", equalTo( 50 ) )
            .body( "width", equalTo( 3 ) )
            .body( "headerWidth", equalTo( 3 ) );

        // Validate headers
        validateHeader( response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 1, "enrollments", "Enrollments", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 2, "lZGmxYbs97q", "Unique ID", "TEXT", "java.lang.String", false, true );

        // Validate the first three rows, as samples.
        validateRow( response, 0,
            List.of( "Fatkom Muchendeh Maternity Clinic",
                "[{\"programUid\" : \"IpHINAT79UW\", \"programInstanceUid\" : \"ujgm3JCJJe6\", \"enrollmentDate\" : \"2022-10-20T12:05:00\", \"incidentDate\" : \"2022-10-20T12:05:00\", \"endDate\" : null, \"events\" : [{\"programStageUid\" : \"A03MvHHogjR\", \"programStageInstanceUid\" : \"tYwEr597zdI\", \"executionDate\" : \"2021-10-20T00:00:00\", \"dueDate\" : \"2021-10-20T12:05:00\", \"eventDataValues\" : {\"UXz7xuGCEhU\": {\"value\": \"2649\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"X8zyunlgUfM\": {\"value\": \"Replacement\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"a3kGcGDCuk6\": {\"value\": \"0\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"bx6fsa0t90x\": {\"value\": \"false\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"ebaJjqltK5N\": {\"value\": \"1\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"wQLfBvPrXqq\": {\"value\": \"NVP only\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}}}, {\"programStageUid\" : \"ZzYYXq4fJie\", \"programStageInstanceUid\" : \"g75nKe4alnv\", \"executionDate\" : \"2021-11-10T00:00:00\", \"dueDate\" : \"2021-11-10T12:05:00\", \"eventDataValues\" : {\"FqlgKAG8HOu\": {\"value\": \"true\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"GQY2lXrypjO\": {\"value\": \"3287\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"HLmTEmupdX0\": {\"value\": \"false\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"X8zyunlgUfM\": {\"value\": \"Mixed\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"cYGaxwK615G\": {\"value\": \"Positive\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"hDZbpskhqDd\": {\"value\": \"PCR\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"lNNb3truQoi\": {\"value\": \"On CTX\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"pOe0ogW4OWd\": {\"value\": \"3\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"rxBfISxXS2U\": {\"value\": \"true\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"sj3j9Hwc7so\": {\"value\": \"TDF/3TC/NVP - 1\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}, \"vTUhAUZFoys\": {\"value\": \"2\", \"created\": \"2014-10-20T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-10-20T12:05:00\", \"providedElsewhere\": false}}}]}]",
                "" ) );

        validateRow( response, 1,
            List.of( "Mokotawa CHP",
                "[{\"programUid\" : \"IpHINAT79UW\", \"programInstanceUid\" : \"pe0mBzatZSV\", \"enrollmentDate\" : \"2022-11-18T12:05:00\", \"incidentDate\" : \"2022-11-18T12:05:00\", \"endDate\" : null, \"events\" : [{\"programStageUid\" : \"A03MvHHogjR\", \"programStageInstanceUid\" : \"joGI0xa6JRg\", \"executionDate\" : \"2021-11-18T00:00:00\", \"dueDate\" : \"2021-11-18T12:05:00\", \"eventDataValues\" : {\"UXz7xuGCEhU\": {\"value\": \"2661\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"X8zyunlgUfM\": {\"value\": \"Replacement\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"a3kGcGDCuk6\": {\"value\": \"2\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"bx6fsa0t90x\": {\"value\": \"true\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"ebaJjqltK5N\": {\"value\": \"3\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"wQLfBvPrXqq\": {\"value\": \"NVP only\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}}}, {\"programStageUid\" : \"ZzYYXq4fJie\", \"programStageInstanceUid\" : \"otfIgxEBhed\", \"executionDate\" : \"2021-12-09T00:00:00\", \"dueDate\" : \"2021-12-09T12:05:00\", \"eventDataValues\" : {\"FqlgKAG8HOu\": {\"value\": \"true\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"GQY2lXrypjO\": {\"value\": \"3540\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"HLmTEmupdX0\": {\"value\": \"true\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"X8zyunlgUfM\": {\"value\": \"Exclusive\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"cYGaxwK615G\": {\"value\": \"Postive √\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"hDZbpskhqDd\": {\"value\": \"Rapid\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"lNNb3truQoi\": {\"value\": \"IPT 1\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"pOe0ogW4OWd\": {\"value\": \"1\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"rxBfISxXS2U\": {\"value\": \"false\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"sj3j9Hwc7so\": {\"value\": \"NVP Only\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}, \"vTUhAUZFoys\": {\"value\": \"3\", \"created\": \"2014-11-18T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-11-18T12:05:00\", \"providedElsewhere\": false}}}]}]",
                "" ) );

        validateRow( response, 2,
            List.of( "Bai Largo MCHP",
                "[{\"programUid\" : \"IpHINAT79UW\", \"programInstanceUid\" : \"rf5WnZneADI\", \"enrollmentDate\" : \"2022-09-23T12:05:00\", \"incidentDate\" : \"2022-09-23T12:05:00\", \"endDate\" : null, \"events\" : [{\"programStageUid\" : \"A03MvHHogjR\", \"programStageInstanceUid\" : \"VMSB295CiVy\", \"executionDate\" : \"2021-09-23T00:00:00\", \"dueDate\" : \"2021-09-23T12:05:00\", \"eventDataValues\" : {\"UXz7xuGCEhU\": {\"value\": \"3552\", \"created\": \"2014-09-23T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-09-23T12:05:00\", \"providedElsewhere\": false}, \"X8zyunlgUfM\": {\"value\": \"Replacement\", \"created\": \"2014-09-23T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-09-23T12:05:00\", \"providedElsewhere\": false}, \"a3kGcGDCuk6\": {\"value\": \"2\", \"created\": \"2014-09-23T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-09-23T12:05:00\", \"providedElsewhere\": false}, \"bx6fsa0t90x\": {\"value\": \"true\", \"created\": \"2014-09-23T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-09-23T12:05:00\", \"providedElsewhere\": false}, \"ebaJjqltK5N\": {\"value\": \"0\", \"created\": \"2014-09-23T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-09-23T12:05:00\", \"providedElsewhere\": false}, \"wQLfBvPrXqq\": {\"value\": \"Others\", \"created\": \"2014-09-23T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-09-23T12:05:00\", \"providedElsewhere\": false}}}, {\"programStageUid\" : \"ZzYYXq4fJie\", \"programStageInstanceUid\" : \"AiAAWU47Ytm\", \"executionDate\" : \"2021-10-14T00:00:00\", \"dueDate\" : \"2021-10-14T12:05:00\", \"eventDataValues\" : {\"FqlgKAG8HOu\": {\"value\": \"false\", \"created\": \"2014-09-23T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-09-23T12:05:00\", \"providedElsewhere\": false}, \"GQY2lXrypjO\": {\"value\": \"2579\", \"created\": \"2014-09-23T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-09-23T12:05:00\", \"providedElsewhere\": false}, \"HLmTEmupdX0\": {\"value\": \"true\", \"created\": \"2014-09-23T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-09-23T12:05:00\", \"providedElsewhere\": false}, \"X8zyunlgUfM\": {\"value\": \"Replacement\", \"created\": \"2014-09-23T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-09-23T12:05:00\", \"providedElsewhere\": false}, \"cYGaxwK615G\": {\"value\": \"Negative\", \"created\": \"2014-09-23T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-09-23T12:05:00\", \"providedElsewhere\": false}, \"hDZbpskhqDd\": {\"value\": \"Rapid\", \"created\": \"2014-09-23T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-09-23T12:05:00\", \"providedElsewhere\": false}, \"lNNb3truQoi\": {\"value\": \"IPT 1\", \"created\": \"2014-09-23T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-09-23T12:05:00\", \"providedElsewhere\": false}, \"pOe0ogW4OWd\": {\"value\": \"1\", \"created\": \"2014-09-23T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-09-23T12:05:00\", \"providedElsewhere\": false}, \"rxBfISxXS2U\": {\"value\": \"true\", \"created\": \"2014-09-23T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-09-23T12:05:00\", \"providedElsewhere\": false}, \"sj3j9Hwc7so\": {\"value\": \"AZT/3TC/ATV/r - 2\", \"created\": \"2014-09-23T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-09-23T12:05:00\", \"providedElsewhere\": false}, \"vTUhAUZFoys\": {\"value\": \"2\", \"created\": \"2014-09-23T12:05:00\", \"storedBy\": null, \"lastUpdated\": \"2014-09-23T12:05:00\", \"providedElsewhere\": false}}}]}]",
                "" ) );
    }
}
