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

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link DataSetController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
class DataSetControllerTest extends DhisControllerConvenienceTest
{

    private String dsId;

    @BeforeEach
    void setUp()
    {
        dsId = assertStatus( HttpStatus.CREATED,
            POST( "/dataSets/", "{'name':'My data set', 'shortName':'MDS', 'periodType':'Monthly'}" ) );
    }

    @Test
    void testGetVersion()
    {
        JsonObject info = GET( "/dataSets/{id}/version", dsId ).content( HttpStatus.OK );
        assertTrue( info.isObject() );
        assertEquals( 1, info.size() );
        assertEquals( 0, info.getNumber( "version" ).intValue() );
    }

    @Test
    void testGetFormJson()
    {
        String ouId = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits/", "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}" ) );
        JsonObject info = GET( "/dataSets/{id}/form?ou={ou}", dsId, ouId ).content( HttpStatus.OK );
        assertTrue( info.isObject() );
        assertTrue( info.has( "label", "options", "groups" ) );
        assertEquals( "My data set", info.getString( "label" ).string() );
        JsonObject options = info.getObject( "options" );
        assertEquals( 0, options.getNumber( "openPeriodsAfterCoEndDate" ).intValue() );
        assertEquals( 0, options.getNumber( "openFuturePeriods" ).intValue() );
        assertEquals( 0, options.getNumber( "expiryDays" ).intValue() );
        assertEquals( "Monthly", options.getString( "periodType" ).string() );
        JsonArray groups = info.getArray( "groups" );
        assertTrue( groups.isArray() );
        assertEquals( 1, groups.size() );
    }

    @Test
    void testGetWithDependenciesAsZipFile()
    {
        HttpResponse res = GET( "/dataSets/{id}/metadata.json.zip?skipSharing=false&download=true", dsId );
        assertEquals( HttpStatus.OK, res.status() );
        assertEquals( "attachment; filename=metadata.json.zip", res.header( "Content-Disposition" ) );
        assertEquals( "application/json+zip", res.header( "Content-Type" ) );
        assertEquals( "binary", res.header( "Content-Transfer-Encoding" ) );
    }

    @Test
    void testGetWithDependenciesAsUncompressedFile()
    {
        HttpResponse res = GET( "/dataSets/{id}/metadata.json?skipSharing=false&download=true", dsId );
        assertEquals( HttpStatus.OK, res.status() );
        assertEquals( "attachment; filename=metadata.json", res.header( "Content-Disposition" ) );
        assertEquals( "application/json", res.header( "Content-Type" ) );
    }
}
