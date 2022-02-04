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

import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonGrid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Tests the {@link EventAnalyticsController}.
 * <p>
 * The main purpose of this test is not to test the correct business logic but
 * to make sure the controller parameters are recognised correctly.
 *
 * @author Jan Bernitt
 */
class EventAnalyticsControllerTest extends DhisControllerConvenienceTest
{

    private String programId;

    private String orgUnitId;

    @BeforeEach
    void setUp()
    {
        orgUnitId = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits/", "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}" ) );
        programId = assertStatus( HttpStatus.CREATED, POST( "/programs/",
            "{'name':'My Program', 'shortName':'MPX1', 'programType': 'WITHOUT_REGISTRATION', 'organisationUnits': [{'id': '"
                + orgUnitId + "'}]}" ) );
    }

    @Test
    void testGetQueryJson()
    {
        JsonGrid grid = GET(
            "/analytics/events/query/{program}?dimension=ou:{unit}&startDate=2019-01-01&endDate=2021-01-01", programId,
            orgUnitId ).content().as( JsonGrid.class );
        assertEquals( grid.getHeaderWidth(), grid.getHeaders().size() );
        assertEquals( "My Program", grid.getMetaData().getItems().get( programId ).getString( "name" ).string() );
        assertEquals( "My Unit", grid.getMetaData().getItems().get( orgUnitId ).getString( "name" ).string() );
        assertEquals( orgUnitId,
            grid.getMetaData().getDimensions().get( "ou" ).get( 0 ).as( JsonString.class ).string() );
    }
}
