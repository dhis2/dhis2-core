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
package org.hisp.dhis.webapi.controller;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Arrays;

import org.hisp.dhis.dataanalysis.FollowupAnalysisRequest;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonResponse;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.json.domain.JsonError;
import org.hisp.dhis.webapi.json.domain.JsonFollowupValue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests the
 * {@link DataAnalysisController#performFollowupAnalysis(FollowupAnalysisRequest)}
 * method.
 *
 * @author Jan Bernitt
 */
class FollowupAnalysisControllerTest extends AbstractDataValueControllerTest
{

    @Autowired
    private CurrentUserService currentUserService;

    /**
     * This test makes sure the fields returned by a
     * {@link org.hisp.dhis.dataanalysis.FollowupValue} are mapped correctly.
     */
    @Test
    void testPerformFollowupAnalysis_FieldMapping()
    {
        addDataValue( "2021-03", "5", "Needs_check", true );
        JsonList<JsonFollowupValue> values = GET( "/dataAnalysis/followup?ou={ou}&de={de}&pe={pe}", orgUnitId,
            dataElementId, "2021" ).content().getList( "followupValues", JsonFollowupValue.class );
        assertEquals( 1, values.size() );
        JsonFollowupValue value = values.get( 0 );
        assertEquals( dataElementId, value.getDe() );
        assertEquals( "My data element", value.getDeName() );
        assertEquals( orgUnitId, value.getOu() );
        assertEquals( "My Unit", value.getOuName() );
        assertEquals( "/" + orgUnitId, value.getOuPath() );
        assertEquals( "202103", value.getPe() );
        assertEquals( "Monthly", value.getPeType() );
        assertEquals( LocalDate.of( 2021, 03, 01 ).atStartOfDay(), value.getPeStartDate() );
        assertEquals( LocalDate.of( 2021, 03, 31 ).atStartOfDay(), value.getPeEndDate() );
        assertEquals( categoryOptionComboId, value.getCoc() );
        assertEquals( "default", value.getCocName() );
        assertEquals( categoryOptionComboId, value.getAoc() );
        assertEquals( "default", value.getAocName() );
        assertEquals( "5", value.getValue() );
        assertEquals( "admin", value.getStoredBy() );
        assertEquals( "Needs_check", value.getComment() );
        assertEquals( "March 2021", value.getPeName() );
        assertNotNull( value.getLastUpdated() );
        assertNotNull( value.getCreated() );
    }

    @Test
    void testPerformFollowupAnalysis_PeriodFiltering()
    {
        addDataValue( "2021-01", "13", "Needs_check 1", true );
        addDataValue( "2021-02", "5", "Needs_check 2", true );
        addDataValue( "2021-04", "11", null, false );
        addDataValue( "2021-05", "11", "Needs_check 3", true );
        assertFollowupValues( orgUnitId, dataElementId, "2021", "Needs_check 1", "Needs_check 2", "Needs_check 3" );
        assertFollowupValues( orgUnitId, dataElementId, "2021-01", "Needs_check 1" );
        assertFollowupValues( orgUnitId, dataElementId, "2021Q1", "Needs_check 1", "Needs_check 2" );
    }

    @Test
    void testPerformFollowupAnalysis_StartEndDateFiltering()
    {
        addDataValue( "2021-01", "13", "Needs_check 1", true );
        addDataValue( "2021-02", "5", "Needs_check 2", true );
        addDataValue( "2021-03", "11", null, false );
        addDataValue( "2021-04", "11", "Needs_check 3", true );
        assertFollowupValues( GET( "/dataAnalysis/followup?ou={ou}&de={de}&startDate={start}&endDate={end}", orgUnitId,
            dataElementId, "2021-02-01", "2021-03-28" ), "Needs_check 2" );
    }

    @Test
    void testPerformFollowupAnalysis_OrgUnitFiltering()
    {
        String ouA = assertStatus( HttpStatus.CREATED, POST( "/organisationUnits/",
            "{'name':'A', 'shortName':'A', 'openingDate': '2020-01-01', 'parent': { 'id':'" + orgUnitId + "'}}" ) );
        String ouB = assertStatus( HttpStatus.CREATED, POST( "/organisationUnits/",
            "{'name':'B', 'shortName':'B', 'openingDate': '2020-01-01', 'parent': {'id':'" + orgUnitId + "'}}" ) );
        addDataValue( "2021-01", "13", "Needs_check A", true, dataElementId, ouA );
        addDataValue( "2021-01", "14", "Needs_check B", true, dataElementId, ouB );
        assertFollowupValues( orgUnitId, dataElementId, "2021-01", "Needs_check A", "Needs_check B" );
        assertFollowupValues( ouA, dataElementId, "2021-01", "Needs_check A" );
        assertFollowupValues( ouB, dataElementId, "2021-01", "Needs_check B" );
    }

    @Test
    void testPerformFollowupAnalysis_DataElementFiltering()
    {
        String de2 = assertStatus( HttpStatus.CREATED,
            POST( "/dataElements/",
                "{'name':'Another DE', 'shortName':'DE2', 'code':'DE2', 'valueType':'INTEGER', "
                    + "'aggregationType':'SUM', 'zeroIsSignificant':false, 'domainType':'AGGREGATE', "
                    + "'categoryCombo': {'id': '" + categoryComboId + "'}}" ) );
        addDataValue( "2021-01", "13", "Needs check DE1", true, dataElementId, orgUnitId );
        addDataValue( "2021-01", "14", "Needs check DE2", true, de2, orgUnitId );
        assertFollowupValues( orgUnitId, dataElementId, "2021-01", "Needs check DE1" );
        assertFollowupValues( orgUnitId, de2, "2021-01", "Needs check DE2" );
    }

    @Test
    void testPerformFollowupAnalysis_ValidationMissingDataElement()
    {
        JsonError error = GET( "/dataAnalysis/followup?ou={ou}&pe=2021", orgUnitId ).error( HttpStatus.CONFLICT );
        assertEquals( ErrorCode.E2300, error.getErrorCode() );
        assertEquals( "At least one data element or data set must be specified", error.getMessage() );
    }

    @Test
    void testPerformFollowupAnalysis_ValidationMissingOrganisationUnit()
    {
        JsonError error = GET( "/dataAnalysis/followup?de={de}&pe=2021", dataElementId ).error( HttpStatus.CONFLICT );
        assertEquals( ErrorCode.E2203, error.getErrorCode() );
        assertEquals( "At least one organisation unit must be specified", error.getMessage() );
    }

    @Test
    void testPerformFollowupAnalysis_ValidationMissingStartDate()
    {
        JsonError error = GET( "/dataAnalysis/followup?ou={ou}&de={de}&endDate=2020-01-01", orgUnitId, dataElementId )
            .error( HttpStatus.CONFLICT );
        assertEquals( ErrorCode.E2301, error.getErrorCode() );
        assertEquals( "Start date and end date must be specified directly or indirectly by specifying a period",
            error.getMessage() );
    }

    @Test
    void testPerformFollowupAnalysis_ValidationMissingEndDate()
    {
        JsonError error = GET( "/dataAnalysis/followup?ou={ou}&de={de}&startDate=2020-01-01", orgUnitId, dataElementId )
            .error( HttpStatus.CONFLICT );
        assertEquals( ErrorCode.E2301, error.getErrorCode() );
        assertEquals( "Start date and end date must be specified directly or indirectly by specifying a period",
            error.getMessage() );
    }

    @Test
    void testPerformFollowupAnalysis_ValidationStartDateNotBeforeEndDate()
    {
        JsonError error = GET( "/dataAnalysis/followup?ou={ou}&de={de}&startDate=2020-01-01&endDate=2019-01-01",
            orgUnitId, dataElementId ).error( HttpStatus.CONFLICT );
        assertEquals( ErrorCode.E2202, error.getErrorCode() );
        assertEquals( "Start date must be before end date", error.getMessage() );
    }

    @Test
    void testPerformFollowupAnalysis_ValidationMaxResultsZeroOrNegative()
    {
        JsonError error = GET( "/dataAnalysis/followup?ou={ou}&de={de}&pe=2021&maxResults=0", orgUnitId, dataElementId )
            .error( HttpStatus.CONFLICT );
        assertEquals( ErrorCode.E2205, error.getErrorCode() );
        assertEquals( "Max results must be a positive number", error.getMessage() );
    }

    @Test
    void testPerformFollowupAnalysis_ValidationMaxResultsOverLimit()
    {
        JsonError error = GET( "/dataAnalysis/followup?ou={ou}&de={de}&pe=2021&maxResults=11111", orgUnitId,
            dataElementId ).error( HttpStatus.CONFLICT );
        assertEquals( ErrorCode.E2206, error.getErrorCode() );
        assertEquals( "Max results exceeds the allowed max limit: `" + NumberFormat.getInstance().format( 10000 ) + "`",
            error.getMessage() );
    }

    private void assertFollowupValues( String orgUnitId, String dataElementId, String period,
        String... expectedComments )
    {
        HttpResponse response = GET( "/dataAnalysis/followup?ou={ou}&de={de}&pe={pe}", orgUnitId, dataElementId,
            period );
        assertFollowupValues( response, expectedComments );
    }

    private void assertFollowupValues( HttpResponse response, String... expectedComments )
    {
        JsonResponse body = response.content();
        JsonList<JsonFollowupValue> values = body.getList( "followupValues", JsonFollowupValue.class );
        assertEquals( expectedComments.length, values.size() );
        assertEquals( Arrays.stream( expectedComments ).collect( toSet() ),
            values.stream().map( JsonFollowupValue::getComment ).collect( toSet() ) );
        JsonObject metadata = body.getObject( "metadata" );
        assertTrue( metadata.exists() );
        assertEquals( asList( "de", "coc", "ou", "startDate", "endDate", "maxResults" ), metadata.names() );
    }
}
