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
package org.hisp.dhis.commons.jackson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatch;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchException;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

class RemoveByIdOperationTest
{
    private final ObjectMapper jsonMapper = JacksonObjectMapperConfig.staticJsonMapper();

    @Test
    void testRemoveInvalidProperty()
        throws JsonProcessingException
    {
        JsonPatch patch = jsonMapper.readValue(
            "[" + "{\"op\": \"remove-by-id\", \"path\": \"/aaa\", \"id\":\"id\"}" + "]",
            JsonPatch.class );
        assertNotNull( patch );
        JsonNode root = jsonMapper.createObjectNode();
        assertFalse( root.has( "organisationUnits" ) );
        assertThrows( JsonPatchException.class, () -> patch.apply( root ) );
    }

    @Test
    void testRemoveNotExistId()
        throws JsonProcessingException,
        JsonPatchException
    {
        JsonPatch patch = jsonMapper.readValue(
            "[" + "{\"op\": \"remove-by-id\", \"path\": \"/organisationUnits\", \"id\":\"pmmYPHSIvaP\"}" + "]",
            JsonPatch.class );
        assertNotNull( patch );
        ObjectNode orgUnit = jsonMapper.createObjectNode();
        orgUnit.set( "id", TextNode.valueOf( "MVrhJ3jWCWm" ) );
        ArrayNode orgUnits = jsonMapper.createArrayNode();
        orgUnits.add( orgUnit );
        ObjectNode root = jsonMapper.createObjectNode();
        root.set( "organisationUnits", orgUnits );
        patch.apply( root );

        // OrgUnit is not removed because the patch id is not the same as the
        // existing one.
        assertEquals( 1, root.get( "organisationUnits" ).size() );
        assertEquals( "MVrhJ3jWCWm", root.get( "organisationUnits" ).get( 0 ).get( "id" ).asText() );
    }

    @Test
    void testRemoveByIdOk()
        throws JsonProcessingException,
        JsonPatchException
    {
        JsonPatch patch = jsonMapper.readValue(
            "[" + "{\"op\": \"remove-by-id\", \"path\": \"/organisationUnits\", \"id\":\"pmmYPHSIvaP\"}" + "]",
            JsonPatch.class );
        assertNotNull( patch );
        ObjectNode orgUnit = jsonMapper.createObjectNode();
        orgUnit.set( "id", TextNode.valueOf( "pmmYPHSIvaP" ) );
        ArrayNode orgUnits = jsonMapper.createArrayNode();
        orgUnits.add( orgUnit );
        ObjectNode root = jsonMapper.createObjectNode();
        root.set( "organisationUnits", orgUnits );
        patch.apply( root );
        assertEquals( 0, root.get( "organisationUnits" ).size() );
    }
}
