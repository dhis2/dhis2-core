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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;

import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonNumber;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonImportConflict;
import org.hisp.dhis.webapi.json.domain.JsonImportCount;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Tests the Geo-JSON import API of the {@link GeoJsonImportController}.
 *
 * @author Jan Bernitt
 */
class GeoJsonImportControllerTest extends DhisControllerConvenienceTest
{
    /**
     * Names of the OUs in the test file:
     * {@code geo-json/sierra-leone-districts.geojson} in the same order as
     * provided in the file
     */
    private static final List<String> NAMES = List.of( "Karene District", "Western Area Urban", "Kono District",
        "Koinadugu District", "Kenema District", "Kailahun District", "Bombali District", "Bo District",
        "Kambia District", "Bonthe District", "Tonkolili District", "Pujehun District", "Eastern Area Rural",
        "Port Loko District", "Moyamba District" );

    @Test
    void testPostImport_NameAsIdentifier()
    {
        Map<Integer, String> ouUids = postOrganisationUnits( IntStream.range( 0, 7 ) );
        JsonWebMessage msg = assertWebMessage( "OK", 200, "OK", null,
            POST( "/organisationUnits/geometry?geoJsonId=false&geoJsonProperty=name&orgUnitProperty=name",
                "geo-json/sierra-leone-districts.geojson" ).content() );
        assertImportReportCounts( msg, 7, 8 );
        assertImportReportError( msg, ErrorCode.E7708, List.of( 7, 8, 9, 10, 11, 12, 13, 14 ) );
        assertGeometryIsSet( ouUids, List.of( 0, 1, 2, 3, 4, 5, 6 ) );
    }

    @Test
    void testPostImport_IdAsIdentifier()
    {
        Map<Integer, String> ouUids = postOrganisationUnits( IntStream.of( 1, 3, 5, 7, 9, 11, 13 ) );
        JsonWebMessage msg = assertWebMessage( "OK", 200, "OK", null,
            POST( "/organisationUnits/geometry", "geo-json/sierra-leone-districts.geojson" ).content() );
        assertImportReportCounts( msg, 4, 11 );
        assertImportReportError( msg, ErrorCode.E7708, List.of( 0, 2, 4, 6, 8, 10, 12, 14 ) );
        assertImportReportError( msg, ErrorCode.E7707, List.of( 9, 11, 13 ) );
        assertGeometryIsSet( ouUids, List.of( 1, 3, 5, 7 ) );
    }

    @Test
    void testPostImport_CodeAsIdentifier()
    {
        Map<Integer, String> ouUids = postOrganisationUnits( IntStream.range( 3, 14 ) );
        JsonWebMessage msg = assertWebMessage( "OK", 200, "OK", null,
            POST( "/organisationUnits/geometry?geoJsonId=false&geoJsonProperty=code&orgUnitProperty=code",
                "geo-json/sierra-leone-districts.geojson" ).content() );
        System.out.println( msg );
        assertImportReportCounts( msg, 6, 9 );
        assertImportReportError( msg, ErrorCode.E7708, List.of( 0, 1, 2, 14 ) );
        assertImportReportError( msg, ErrorCode.E7707, List.of( 8, 9, 11, 12, 13 ) );
        assertGeometryIsSet( ouUids, List.of( 3, 4, 5, 6, 7, 10 ) );
    }

    private void assertImportReportCounts( JsonWebMessage msg, int expectedImported, int expectedIgnored )
    {
        JsonObject report = msg.getResponse();
        JsonImportCount counts = report.get( "importCount", JsonImportCount.class );
        assertEquals( expectedImported, counts.getImported(), "imported" );
        assertEquals( expectedIgnored, counts.getIgnored(), "ignored" );
        assertEquals( expectedIgnored, report.getNumber( "totalConflictOccurrenceCount" ).intValue() );
    }

    private void assertImportReportError( JsonWebMessage msg, ErrorCode code, List<Integer> expectedIndexes )
    {
        JsonObject report = msg.getResponse();
        JsonList<JsonImportConflict> conflicts = report.getList( "conflicts", JsonImportConflict.class );
        JsonImportConflict conflict = conflicts.first( c -> c.getErrorCode() == code );
        assertEquals( expectedIndexes, conflict.getIndexes().toList( JsonNumber::intValue ) );
    }

    private void assertGeometryIsSet( Map<Integer, String> ouUids, List<Integer> indexes )
    {
        indexes.stream().map( ouUids::get ).forEach( this::assertGeometryIsSet );
    }

    private void assertGeometryIsSet( String uid )
    {
        JsonObject unit = GET( "/organisationUnits/{uid}", uid ).content();
        JsonObject geometry = unit.getObject( "geometry" );
        assertTrue( geometry.exists() && geometry.isObject(), uid + " has no geometry" );
        assertEquals( "MultiPolygon", geometry.getString( "type" ).string() );
        JsonArray coordinates = geometry.getArray( "coordinates" ).getArray( 0 ).getArray( 0 );
        assertTrue( coordinates.size() >= 3 );
    }

    private Map<Integer, String> postOrganisationUnits( IntStream nameIndexes )
    {
        Map<Integer, String> ouUidByIndex = new TreeMap<>();
        for ( int i : nameIndexes.toArray() )
        {
            ouUidByIndex.put( i, postOrganisationUnit( NAMES.get( i ) ) );
        }
        return ouUidByIndex;
    }

    private String postOrganisationUnit( String name )
    {
        return assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits/",
                "{"
                    + "'id':'" + name.substring( 0, 4 ) + "5678901', "
                    + "'name':'" + name + "', "
                    + "'shortName':'" + name + "', "
                    + "'code':'" + name.substring( 0, 3 ).toUpperCase() + "', "
                    + "'openingDate':'2021-01-01'}" ) );
    }
}
