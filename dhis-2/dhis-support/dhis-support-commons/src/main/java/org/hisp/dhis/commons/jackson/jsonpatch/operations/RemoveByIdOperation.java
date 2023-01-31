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
package org.hisp.dhis.commons.jackson.jsonpatch.operations;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchException;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchOperation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;

public class RemoveByIdOperation extends JsonPatchOperation
{
    protected final String id;

    @JsonCreator
    public RemoveByIdOperation( @JsonProperty( "path" ) JsonPointer path, @JsonProperty( "id" ) String id )
    {
        super( REMOVE_BY_ID_OPERATION, path );
        this.id = id;
    }

    @Override
    public JsonNode apply( JsonNode node )
        throws JsonPatchException
    {
        if ( path == JsonPointer.empty() )
        {
            return MissingNode.getInstance();
        }

        if ( StringUtils.isEmpty( id ) || !CodeGenerator.isValidUid( id ) )
        {
            throw new JsonPatchException( String.format( "Invalid id %s", id ) );
        }

        if ( !nodePathExists( node ) )
        {
            throw new JsonPatchException( String.format( "Invalid path %s", path ) );
        }

        final JsonNode parentNode = node.at( path );

        if ( parentNode.isObject() )
        {
            return node;
        }

        if ( parentNode.isArray() )
        {
            ArrayNode arrayNode = ((ArrayNode) parentNode);
            int removeIndex = -1;
            for ( int i = 0; i < arrayNode.size(); i++ )
            {
                JsonNode item = arrayNode.get( i );
                if ( item.has( "id" ) && id.equals( item.get( "id" ).asText() ) )
                {
                    removeIndex = i;
                    break;
                }
            }

            if ( removeIndex > -1 )
            {
                arrayNode.remove( removeIndex );
            }
        }

        return node;
    }

    private boolean nodePathExists( JsonNode node )
    {
        final JsonNode found = node.at( path );
        return !found.isMissingNode();
    }
}
