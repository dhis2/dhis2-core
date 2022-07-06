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
package org.hisp.dhis.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.vdurmont.semver4j.Semver;

@Slf4j
class SystemUpdateServiceTest
{

    @Test
    void testParseHotfixVersion()
    {
        String value = "2.37.7";
        // String value = "2.37.7-1";

        Semver currentVersion = new Semver( value );

        Map<Semver, Map<String, String>> latestNewerThan = SystemUpdateService.getLatestNewerThan( currentVersion );

        log.info( "latest:" + latestNewerThan );
    }

    @Test
    void testGetNewerPatchVersions_Success()
    {
        // Given
        Semver currentVersion = new Semver( "1.2.2" );
        JsonObject allVersions = new JsonObject();
        allVersions.add( "versions", new JsonArray() );
        JsonObject versionElement = new JsonObject();
        versionElement.add( "name", new JsonPrimitive( "1.2" ) );
        versionElement.add( "version", new JsonPrimitive( 2 ) );
        versionElement.add( "latestPatchVersion", new JsonPrimitive( 3 ) );
        versionElement.add( "patchVersions", new JsonArray() );
        JsonObject patchElement = new JsonObject();
        patchElement.add( "version", new JsonPrimitive( 3 ) );
        versionElement.getAsJsonArray( "patchVersions" ).add( patchElement );
        allVersions.getAsJsonArray( "versions" ).add( versionElement );
        // When
        List<JsonElement> newerPatchVersions = SystemUpdateService.extractNewerPatchHotfixVersions( currentVersion,
            allVersions );
        // Then
        assertNotNull( newerPatchVersions );
        assertEquals( 1, newerPatchVersions.size() );
    }

    @Test
    void testGetNewerPatchVersions_NoNewerPatchVersions()
    {
        Semver currentVersion = new Semver( "1.2.3" );
        JsonObject allVersions = new JsonObject();
        allVersions.add( "versions", new JsonArray() );
        List<JsonElement> newerPatchVersions = SystemUpdateService.extractNewerPatchHotfixVersions( currentVersion,
            allVersions );
        assertTrue( newerPatchVersions.isEmpty() );
    }

    @Test
    void testParseJsonPatchVersions_Success()
    {
        List<JsonElement> newerPatchVersions = new ArrayList<>();
        JsonObject patchJsonObject = new JsonObject();
        patchJsonObject.addProperty( "name", "1.2.3" );
        patchJsonObject.addProperty( "releaseDate", "2018-01-01" );
        patchJsonObject.addProperty( "url", "https://example.com/download/1.2.3" );
        newerPatchVersions.add( patchJsonObject );
        Map<Semver, Map<String, String>> versionsAndMetadata = SystemUpdateService
            .convertJsonToMap( newerPatchVersions );
        assertNotNull( versionsAndMetadata );
        assertEquals( 1, newerPatchVersions.size() );
        Semver semverKey = new Semver( "1.2.3" );
        assertEquals( "1.2.3", versionsAndMetadata.get( semverKey ).get( "version" ) );
        assertEquals( "2018-01-01", versionsAndMetadata.get( semverKey ).get( "releaseDate" ) );
        assertEquals( "https://example.com/download/1.2.3", versionsAndMetadata.get( semverKey ).get( "downloadUrl" ) );
    }

    @Test
    void testParseJsonPatchVersions_Failure()
    {
        List<JsonElement> newerPatchVersions = new ArrayList<>();
        Map<Semver, Map<String, String>> versionsAndMetadata = SystemUpdateService
            .convertJsonToMap( newerPatchVersions );
        assertNotNull( versionsAndMetadata );
        assertTrue( versionsAndMetadata.isEmpty() );
    }
}
