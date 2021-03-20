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
package org.hisp.dhis.webapi.controller;

import static java.util.Collections.singletonList;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.Assert.assertEquals;

import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.JsonDocument.JsonNodeType;
import org.hisp.dhis.webapi.json.JsonObject;
import org.hisp.dhis.webapi.json.JsonString;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegrityReport;
import org.junit.Test;
import org.springframework.http.HttpStatus;

/**
 * Tests a selection of data integrity checks.
 *
 * @author Jan Bernitt
 */
public class DataIntegrityControllerTest extends DhisControllerConvenienceTest
{

    @Test
    public void testDataIntegrity_NoErrors()
    {
        // if the report does not have any strings in the JSON there are no
        // errors
        assertEquals( 0, getDataIntegrityReport().node().count( JsonNodeType.STRING ) );
    }

    @Test
    public void testDataIntegrity_OrphanedOrganisationUnits()
    {
        // should match:
        assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits", "{'name':'OrphanedUnit', 'shortName':'test', 'openingDate':'2021'}" ) );

        // should not match:
        String id = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits", "{'name':'root', 'shortName':'root', 'openingDate':'2021'}" ) );
        assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits",
                "{'name':'leaf', 'shortName':'leaf', 'openingDate':'2021', 'parent': { 'id': '" + id + "'}}" ) );

        assertEquals( singletonList( "OrphanedUnit" ),
            getDataIntegrityReport().getOrphanedOrganisationUnits().as( JsonString::string ) );
    }

    @Test
    public void testDataIntegrity_OrganisationUnitsWithoutGroups()
    {
        assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits", "{'name':'noGroupSet', 'shortName':'test', 'openingDate':'2021'}" ) );

        assertEquals( singletonList( "noGroupSet" ),
            getDataIntegrityReport().getOrganisationUnitsWithoutGroups().as( JsonString::string ) );
    }

    private JsonDataIntegrityReport getDataIntegrityReport()
    {
        JsonObject response = POST( "/dataIntegrity" ).content().getObject( "response" );

        String id = response.getString( "id" ).string();
        String jobType = response.getString( "jobType" ).string();

        return GET( "/system/taskSummaries/{type}/{id}", jobType, id )
            .content().as( JsonDataIntegrityReport.class );
    }

}
