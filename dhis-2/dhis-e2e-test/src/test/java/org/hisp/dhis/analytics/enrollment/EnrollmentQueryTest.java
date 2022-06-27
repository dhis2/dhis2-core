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
package org.hisp.dhis.analytics.enrollment;

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
import org.hisp.dhis.actions.analytics.AnalyticsEnrollmentsActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.Test;

/**
 * Groups e2e tests for Enrollments "/query" endpoint.
 *
 * @author maikel arabori
 */
public class EnrollmentQueryTest extends AnalyticsApiTest
{

    private AnalyticsEnrollmentsActions enrollmentsActions = new AnalyticsEnrollmentsActions();

    @Test
    public void queryWithProgramAndProgramStageWhenTotalPagesIsFalse()
    {
        // Given
        final QueryParamsBuilder params = new QueryParamsBuilder();
        params.add( "dimension=pe:LAST_12_MONTHS,ou:ImspTQPwCqd" )
            .add( "stage=A03MvHHogjR" )
            .add( "displayProperty=NAME" )
            .add( "totalPages=false" )
            .add( "outputType=ENROLLMENT" )
            .add( "desc=enrollmentdate" )
            .add( "pageSize=100" )
            .add( "page=1" );

        // When
        final ApiResponse response = enrollmentsActions.query().get( "IpHINAT79UW", JSON, JSON, params );

        // Then
        response.validate()
            .statusCode( 200 )
            .body( "headers", hasSize( equalTo( 15 ) ) )
            .body( "rows", hasSize( equalTo( 100 ) ) )
            .body( "metaData.pager.page", equalTo( 1 ) )
            .body( "metaData.pager.pageSize", equalTo( 100 ) )
            .body( "metaData.pager.isLastPage", is( false ) )
            .body( "metaData.pager", not( hasKey( "total" ) ) )
            .body( "metaData.pager", not( hasKey( "pageCount" ) ) )
            .body( "metaData.items.ImspTQPwCqd.name", equalTo( "Sierra Leone" ) )
            .body( "metaData.items.IpHINAT79UW.name", equalTo( "Child Programme" ) )
            .body( "metaData.items.ZzYYXq4fJie.name", equalTo( "Baby Postnatal" ) )
            .body( "metaData.items.A03MvHHogjR.name", equalTo( "Birth" ) )
            .body( "metaData.items.ou.name", equalTo( "Organisation unit" ) )
            .body( "metaData.items.LAST_12_MONTHS.name", equalTo( "Last 12 months" ) )
            .body( "metaData.dimensions.pe", hasSize( equalTo( 0 ) ) )
            .body( "metaData.dimensions.ou", hasSize( equalTo( 1 ) ) )
            .body( "metaData.dimensions.ou", hasItem( "ImspTQPwCqd" ) )
            .body( "height", equalTo( 100 ) )
            .body( "width", equalTo( 15 ) )
            .body( "headerWidth", equalTo( 15 ) );

        // Validate headers
        validateHeader( response, 0, "pi", "Enrollment", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 1, "tei", "Tracked entity instance", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 2, "enrollmentdate", "Date of enrollment", "DATE", "java.time.LocalDate", false,
            true );
        validateHeader( response, 3, "incidentdate", "Date of birth", "DATE", "java.time.LocalDate", false, true );
        validateHeader( response, 4, "storedby", "Stored by", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 5, "createdbydisplayname", "Created by (display name)", "TEXT", "java.lang.String",
            false, true );
        validateHeader( response, 6, "lastupdatedbydisplayname", "Last updated by (display name)", "TEXT",
            "java.lang.String", false, true );
        validateHeader( response, 7, "lastupdated", "Last Updated", "DATE", "java.time.LocalDate", false, true );
        validateHeader( response, 8, "geometry", "Geometry", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 9, "longitude", "Longitude", "NUMBER", "java.lang.Double", false, true );
        validateHeader( response, 10, "latitude", "Latitude", "NUMBER", "java.lang.Double", false, true );
        validateHeader( response, 11, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 12, "oucode", "Organisation unit code", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 13, "programstatus", "Program status", "TEXT", "java.lang.String", false, true );
        validateHeader( response, 14, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true );

        // Validate the first three rows, as sample size.
        validateRow( response, 0,
            List.of( "zxBwOY6AJ9W", "p9Ivo6FsNSM", "2022-05-31 12:05:00.0", "2022-05-31 12:05:00.0", "", "", "",
                "2018-08-06 21:20:51.645", "", "0.0", "0.0", "Firawa CHC", "OU_226273", "ACTIVE", "NMcx2jmra3c" ) );
        validateRow( response, 1,
            List.of( "O0KHGiqNgC2", "aIzvohni7g7", "2022-05-31 12:05:00.0", "2022-05-31 12:05:00.0", "", "", "",
                "2018-08-06 21:20:49.373", "", "0.0", "0.0", "Banana Island MCHP", "OU_278384", "ACTIVE",
                "jjtzkzrmG7s" ) );
        validateRow( response, 2,
            List.of( "p43cOkSdtZM", "BDHmCFRRU6H", "2022-05-31 12:05:00.0", "2022-05-31 12:05:00.0", "", "", "",
                "2018-08-06 21:20:48.184", "", "0.0", "0.0", "Roktolon MCHP", "OU_803066", "ACTIVE", "VTtyiYcc6TE" ) );
    }
}
