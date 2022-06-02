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

import static java.lang.String.format;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;

import org.hisp.dhis.attribute.Attribute;
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
 * Tests the GeoJSON import API of the {@link GeoJsonImportController}.
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
        Map<Integer, String> ouIds = postNewOrganisationUnits( IntStream.range( 0, 7 ) );

        JsonWebMessage msg = assertWebMessage( "OK", 200, "WARNING", "Import partially successful.",
            POST( "/organisationUnits/geometry?geoJsonId=false&geoJsonProperty=name&orgUnitProperty=name",
                "geo-json/sierra-leone-districts.geojson" ).content() );

        assertImportedAndIgnored( msg, 7, 8 );
        assertReportError( msg, ErrorCode.E7708, List.of( 7, 8, 9, 10, 11, 12, 13, 14 ) );
        assertGeometryIsNotNull( ouIds, List.of( 0, 1, 2, 3, 4, 5, 6 ) );
    }

    @Test
    void testPostImport_NameAsIdentifier_Attribute()
    {
        String attrId = postNewGeoJsonAttribute();
        Map<Integer, String> ouIds = postNewOrganisationUnits( IntStream.range( 0, 7 ) );

        JsonWebMessage msg = assertWebMessage( "OK", 200, "WARNING", "Import partially successful.",
            POST(
                "/organisationUnits/geometry?geoJsonId=false&geoJsonProperty=name&orgUnitProperty=name&attributeId="
                    + attrId,
                "geo-json/sierra-leone-districts.geojson" ).content() );

        assertImportedAndIgnored( msg, 7, 8 );
        assertReportError( msg, ErrorCode.E7708, List.of( 7, 8, 9, 10, 11, 12, 13, 14 ) );
        assertGeometryAttributeIsNotNull( attrId, ouIds, List.of( 0, 1, 2, 3, 4, 5, 6 ) );
    }

    @Test
    void testPostImport_IdAsIdentifier()
    {
        Map<Integer, String> ouIds = postNewOrganisationUnits( IntStream.of( 1, 3, 5, 7, 9, 11, 13 ) );

        JsonWebMessage msg = assertWebMessage( "OK", 200, "WARNING", "Import partially successful.",
            POST( "/organisationUnits/geometry", "geo-json/sierra-leone-districts.geojson" ).content() );

        assertImportedAndIgnored( msg, 4, 11 );
        assertReportError( msg, ErrorCode.E7708, List.of( 0, 2, 4, 6, 8, 10, 12, 14 ) );
        assertReportError( msg, ErrorCode.E7707, List.of( 9, 11, 13 ) );
        assertGeometryIsNotNull( ouIds, List.of( 1, 3, 5, 7 ) );
    }

    @Test
    void testPostImport_IdAsIdentifier_Attribute()
    {
        String attrId = postNewGeoJsonAttribute();
        Map<Integer, String> ouIds = postNewOrganisationUnits( IntStream.of( 1, 3, 5, 7, 9, 11, 13 ) );

        JsonWebMessage msg = assertWebMessage( "OK", 200, "WARNING", "Import partially successful.",
            POST( "/organisationUnits/geometry?attributeId=" + attrId, "geo-json/sierra-leone-districts.geojson" )
                .content() );

        assertImportedAndIgnored( msg, 4, 11 );
        assertReportError( msg, ErrorCode.E7708, List.of( 0, 2, 4, 6, 8, 10, 12, 14 ) );
        assertReportError( msg, ErrorCode.E7707, List.of( 9, 11, 13 ) );
        assertGeometryAttributeIsNotNull( attrId, ouIds, List.of( 1, 3, 5, 7 ) );
    }

    @Test
    void testPostImport_CodeAsIdentifier()
    {
        Map<Integer, String> ouIds = postNewOrganisationUnits( IntStream.range( 3, 14 ) );

        JsonWebMessage msg = assertWebMessage( "OK", 200, "WARNING", "Import partially successful.",
            POST( "/organisationUnits/geometry?geoJsonId=false&geoJsonProperty=code&orgUnitProperty=code",
                "geo-json/sierra-leone-districts.geojson" ).content() );

        assertImportedAndIgnored( msg, 6, 9 );
        assertReportError( msg, ErrorCode.E7708, List.of( 0, 1, 2, 14 ) );
        assertReportError( msg, ErrorCode.E7707, List.of( 8, 9, 11, 12, 13 ) );
        assertGeometryIsNotNull( ouIds, List.of( 3, 4, 5, 6, 7, 10 ) );
    }

    @Test
    void testPostImport_CodeAsIdentifier_Attribute()
    {
        String attrId = postNewGeoJsonAttribute();
        Map<Integer, String> ouIds = postNewOrganisationUnits( IntStream.range( 3, 14 ) );

        JsonWebMessage msg = assertWebMessage( "OK", 200, "WARNING", "Import partially successful.",
            POST(
                "/organisationUnits/geometry?geoJsonId=false&geoJsonProperty=code&orgUnitProperty=code&attributeId="
                    + attrId,
                "geo-json/sierra-leone-districts.geojson" ).content() );

        assertImportedAndIgnored( msg, 6, 9 );
        assertReportError( msg, ErrorCode.E7708, List.of( 0, 1, 2, 14 ) );
        assertReportError( msg, ErrorCode.E7707, List.of( 8, 9, 11, 12, 13 ) );
        assertGeometryAttributeIsNotNull( attrId, ouIds, List.of( 3, 4, 5, 6, 7, 10 ) );
    }

    @Test
    void testPostImport_ErrorInputNotGeoJson()
    {
        JsonWebMessage msg = assertWebMessage( "OK", 200, "ERROR", "Import failed.",
            POST( "/organisationUnits/geometry", "not-valid-geojson" ).content( HttpStatus.OK ) );
        assertReportError( msg, ErrorCode.E7701, List.of() );
    }

    @Test
    void testPostImport_ErrorAttributeDoesNotExist()
    {
        JsonWebMessage msg = assertWebMessage( "OK", 200, "ERROR", "Import failed.",
            POST( "/organisationUnits/geometry?attributeId=fake", "does not matter" ).content( HttpStatus.OK ) );
        assertReportError( msg, ErrorCode.E7702, List.of() );
    }

    @Test
    void testPostImport_ErrorAttributeNotGeoJson()
    {
        String attrId = postNewAttribute( "TEXT", Attribute.ObjectType.ORGANISATION_UNIT );

        JsonWebMessage msg = assertWebMessage( "OK", 200, "ERROR", "Import failed.",
            POST( "/organisationUnits/geometry?attributeId=" + attrId, "does not matter" )
                .content( HttpStatus.OK ) );
        assertReportError( msg, ErrorCode.E7703, List.of() );
    }

    @Test
    void testPostImport_ErrorAttributeNotForOrganisationUnits()
    {
        String attrId = postNewAttribute( "GEOJSON", Attribute.ObjectType.CATEGORY );

        JsonWebMessage msg = assertWebMessage( "OK", 200, "ERROR", "Import failed.",
            POST( "/organisationUnits/geometry?attributeId=" + attrId, "does not matter" )
                .content( HttpStatus.OK ) );
        assertReportError( msg, ErrorCode.E7704, List.of() );
    }

    @Test
    void testPostImport_ErrorFeatureHasNoIdentifier()
    {
        JsonWebMessage msg = assertWebMessage( "OK", 200, "ERROR", "Import failed.",
            POST( "/organisationUnits/geometry",
                "{'features':[{'geometry': {'type':'MultiPolygon', 'coordinates': [ [ [ [ -12, 9 ], [ -13, 10 ], [ -11, 8 ] ] ] ] }}]}" )
                    .content( HttpStatus.OK ) );
        assertReportError( msg, ErrorCode.E7705, List.of( 0 ) );
    }

    @Test
    void testPostImport_ErrorFeatureHasNoGeometry()
    {
        postNewOrganisationUnits( IntStream.of( 0 ) );

        JsonWebMessage msg = assertWebMessage( "OK", 200, "ERROR", "Import failed.",
            POST( "/organisationUnits/geometry", "{'features':[{'id':'Kare5678901'}]}" )
                .content( HttpStatus.OK ) );
        assertReportError( msg, ErrorCode.E7706, List.of( 0 ) );
    }

    @Test
    void testPostImport_ErrorGeometryIsNotValid()
    {
        postNewOrganisationUnits( IntStream.of( 0 ) );

        JsonWebMessage msg = assertWebMessage( "OK", 200, "ERROR", "Import failed.",
            POST( "/organisationUnits/geometry",
                "{'features':[{'id':'Kare5678901', 'geometry': {'type':'Invalid'} }]}" )
                    .content( HttpStatus.OK ) );
        assertReportError( msg, ErrorCode.E7707, List.of( 0 ) );
    }

    @Test
    void testPostImport_ErrorOrgUnitDoesNotExist()
    {
        JsonWebMessage msg = assertWebMessage( "OK", 200, "ERROR", "Import failed.",
            POST( "/organisationUnits/geometry",
                "{'features':[{'id':'foo', 'geometry': {'type':'MultiPolygon', 'coordinates': [ [ [ [ -12, 9 ], [ -13, 10 ], [ -11, 8 ] ] ] ]}}]}" )
                    .content( HttpStatus.OK ) );
        assertReportError( msg, ErrorCode.E7708, List.of( 0 ) );
    }

    @Test
    void testPostImport_ErrorOrgUnitIsNotUnique()
    {
        postNewOrganisationUnit( "Alpha", "Alpha", "ALP" );
        postNewOrganisationUnit( "Alpha", "Beta", "BET" );

        JsonWebMessage msg = assertWebMessage( "OK", 200, "ERROR", "Import failed.",
            POST( "/organisationUnits/geometry?geoJsonId=false&geoJsonProperty=name&orgUnitProperty=name",
                "{'features':[{"
                    + "'properties': { 'name': 'Alpha'}, "
                    + "'geometry': {'type':'MultiPolygon', 'coordinates': [ [ [ [ 1,1 ], [ 2,2 ], [ 1,3 ], [1,1] ] ] ] }"
                    + "}]}" )
                        .content( HttpStatus.OK ) );
        assertReportError( msg, ErrorCode.E7711, List.of( 0 ) );
    }

    @Test
    void testPostImportSingle()
    {
        Map<Integer, String> ouIds = postNewOrganisationUnits( IntStream.of( 0 ) );
        String ouId = ouIds.values().iterator().next();

        JsonWebMessage msg = assertWebMessage( "OK", 200, "OK", "Import successful.",
            POST( "/organisationUnits/" + ouId + "/geometry",
                "{'type':'MultiPolygon', 'coordinates': [ [ [ [ 1,1 ], [ 2,2 ], [ 1,3 ], [1,1] ] ] ] }" )
                    .content( HttpStatus.OK ) );

        assertImportedAndIgnored( msg, 1, 0 );
        assertGeometryIsNotNull( ouId );
    }

    @Test
    void testPostImportSingle_Attribute()
    {
        String attrId = postNewGeoJsonAttribute();
        Map<Integer, String> ouIds = postNewOrganisationUnits( IntStream.of( 0 ) );
        String ouId = ouIds.values().iterator().next();

        JsonWebMessage msg = assertWebMessage( "OK", 200, "OK", "Import successful.",
            POST( "/organisationUnits/" + ouId + "/geometry?attributeId=" + attrId,
                "{'type':'MultiPolygon', 'coordinates': [ [ [ [ 1,1 ], [ 2,2 ], [ 1,3 ], [1,1] ] ] ] }" )
                    .content( HttpStatus.OK ) );

        assertImportedAndIgnored( msg, 1, 0 );
        assertGeometryAttributeIsNotNull( attrId, ouId );
    }

    @Test
    void testDeleteImportSingle()
    {
        Map<Integer, String> ouIds = postNewOrganisationUnits( IntStream.of( 0 ) );
        String ouId = ouIds.values().iterator().next();

        JsonWebMessage msg = assertWebMessage( "OK", 200, "OK", "Import successful.",
            POST( "/organisationUnits/" + ouId + "/geometry",
                "{'type':'MultiPolygon', 'coordinates': [ [ [ [ 1,1 ], [ 2,2 ], [ 1,3 ], [1,1] ] ] ] }" )
                    .content( HttpStatus.OK ) );

        assertImportedAndIgnored( msg, 1, 0 );
        assertGeometryIsNotNull( ouId );

        msg = assertWebMessage( "OK", 200, "OK", "Import successful.",
            DELETE( "/organisationUnits/" + ouId + "/geometry" ).content( HttpStatus.OK ) );

        assertDeletedAndIgnored( msg, 1, 0 );
        assertGeometryIsNull( ouId );
    }

    @Test
    void testDeleteImportSingle_Attribute()
    {
        String attrId = postNewGeoJsonAttribute();
        Map<Integer, String> ouIds = postNewOrganisationUnits( IntStream.of( 0 ) );
        String ouId = ouIds.values().iterator().next();

        JsonWebMessage msg = assertWebMessage( "OK", 200, "OK", "Import successful.",
            POST( "/organisationUnits/" + ouId + "/geometry?attributeId=" + attrId,
                "{'type':'MultiPolygon', 'coordinates': [ [ [ [ 1,1 ], [ 2,2 ], [ 1,3 ], [1,1] ] ] ] }" )
                    .content( HttpStatus.OK ) );

        assertImportedAndIgnored( msg, 1, 0 );
        assertGeometryAttributeIsNotNull( attrId, ouId );

        msg = assertWebMessage( "OK", 200, "OK", "Import successful.",
            DELETE( "/organisationUnits/" + ouId + "/geometry?attributeId=" + attrId ).content( HttpStatus.OK ) );

        assertDeletedAndIgnored( msg, 1, 0 );
        assertGeometryAttributeIsNull( attrId, ouId );
    }

    private void assertImportedAndIgnored( JsonWebMessage msg, int expectedImported, int expectedIgnored )
    {
        JsonObject report = msg.getResponse();
        JsonImportCount counts = report.get( "importCount", JsonImportCount.class );
        assertEquals( expectedImported, counts.getImported(), "imported" );
        assertEquals( expectedIgnored, counts.getIgnored(), "ignored" );
        assertEquals( 0, counts.getDeleted(), "ignored" );
        assertEquals( 0, counts.getUpdated(), "updated" );
        assertEquals( expectedIgnored, report.getNumber( "totalConflictOccurrenceCount" ).intValue() );
    }

    private void assertDeletedAndIgnored( JsonWebMessage msg, int expectedDeleted, int expectedIgnored )
    {
        JsonObject report = msg.getResponse();
        JsonImportCount counts = report.get( "importCount", JsonImportCount.class );
        assertEquals( 0, counts.getImported(), "imported" );
        assertEquals( expectedIgnored, counts.getIgnored(), "ignored" );
        assertEquals( expectedDeleted, counts.getDeleted(), "ignored" );
        assertEquals( 0, counts.getUpdated(), "updated" );
        assertEquals( expectedIgnored, report.getNumber( "totalConflictOccurrenceCount" ).intValue() );
    }

    private void assertReportError( JsonWebMessage msg, ErrorCode code, List<Integer> expectedIndexes )
    {
        JsonObject report = msg.getResponse();
        JsonList<JsonImportConflict> conflicts = report.getList( "conflicts", JsonImportConflict.class );
        JsonImportConflict conflict = conflicts.first( c -> c.getErrorCode() == code );
        assertTrue( conflict.exists(), () -> format( "no conflict of code %s exists but %s", code, conflicts.toList(
            JsonImportConflict::getErrorCode, List.of() ) ) );
        if ( expectedIndexes.isEmpty() )
        {
            assertTrue( conflict.getIndexes().isUndefined() || conflict.getIndexes().isEmpty() );
        }
        else
        {
            assertEquals( expectedIndexes, conflict.getIndexes().toList( JsonNumber::intValue ) );
        }
    }

    private void assertGeometryIsNotNull( Map<Integer, String> ouIds, List<Integer> indexes )
    {
        indexes.stream().map( ouIds::get ).forEach( this::assertGeometryIsNotNull );
    }

    private void assertGeometryIsNotNull( String uid )
    {
        JsonObject unit = GET( "/organisationUnits/{uid}/gist?fields=geometry,attributeValues", uid ).content();
        JsonObject geometry = unit.getObject( "geometry" );
        assertTrue( geometry.exists() && geometry.isObject(), uid + " has no geometry" );
        assertIsGeometryValue( geometry );
        JsonArray attributeValues = unit.getArray( "attributeValues" );
        assertTrue( attributeValues.isUndefined() || attributeValues.isEmpty() );
    }

    private void assertGeometryAttributeIsNotNull( String attributeId, Map<Integer, String> ouIds,
        List<Integer> indexes )
    {
        indexes.stream().map( ouIds::get ).forEach( ouId -> assertGeometryAttributeIsNotNull( attributeId, ouId ) );
    }

    private void assertGeometryAttributeIsNotNull( String attributeId, String uid )
    {
        JsonObject unit = GET( "/organisationUnits/{uid}/gist?fields=geometry,{attr}~rename(geo2)", uid, attributeId )
            .content();
        JsonObject geometry = unit.getObject( "geo2" );
        assertTrue( geometry.exists() && geometry.isObject(), uid + " has no geometry" );
        // need to un-quote the value to make it into a JSON document
        assertIsGeometryValue( geometry );
        assertTrue( unit.getObject( "geometry" ).isUndefined(),
            "unit should not have a geometry value set when an attribute is used" );
    }

    private void assertIsGeometryValue( JsonObject geometry )
    {
        assertEquals( "MultiPolygon", geometry.getString( "type" ).string() );
        JsonArray coordinates = geometry.getArray( "coordinates" ).getArray( 0 ).getArray( 0 );
        assertTrue( coordinates.size() >= 3 );
    }

    private void assertGeometryAttributeIsNull( String attributeId, String uid )
    {
        JsonObject unit = GET( "/organisationUnits/{uid}/gist?fields=geometry,{attr}~rename(geo2)", uid, attributeId )
            .content();
        assertTrue( unit.getString( "geo2" ).isUndefined(), uid + " still has a geometry" );
    }

    private void assertGeometryIsNull( String uid )
    {
        JsonObject unit = GET( "/organisationUnits/{uid}/gist?fields=geometry,attributeValues", uid ).content();
        JsonObject geometry = unit.getObject( "geometry" );
        assertTrue( geometry.isUndefined(), "geometry still set" );
    }

    private Map<Integer, String> postNewOrganisationUnits( IntStream nameIndexes )
    {
        Map<Integer, String> ouIdByIndex = new TreeMap<>();
        for ( int i : nameIndexes.toArray() )
        {
            ouIdByIndex.put( i, postNewOrganisationUnit( NAMES.get( i ) ) );
        }
        return ouIdByIndex;
    }

    private String postNewGeoJsonAttribute()
    {
        return postNewAttribute( "GEOJSON", Attribute.ObjectType.ORGANISATION_UNIT );
    }

    private String postNewAttribute( String valueType, Attribute.ObjectType objType )
    {
        return assertStatus( HttpStatus.CREATED, POST( "/attributes", "{"
            + "'name':'geo2', "
            + "'valueType':'" + valueType + "', " + "'" + objType.getPropertyName() + "':true"
            + "}" ) );
    }

    private String postNewOrganisationUnit( String name )
    {
        return postNewOrganisationUnit( name, name, name.substring( 0, 3 ).toUpperCase() );
    }

    private String postNewOrganisationUnit( String name, String id, String code )
    {
        return assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits/",
                "{"
                    + "'id':'" + id.substring( 0, 4 ) + "5678901', "
                    + "'name':'" + name + "', "
                    + "'shortName':'" + id + "', "
                    + "'code':'" + code + "', "
                    + "'openingDate':'2021-01-01'}" ) );
    }
}
