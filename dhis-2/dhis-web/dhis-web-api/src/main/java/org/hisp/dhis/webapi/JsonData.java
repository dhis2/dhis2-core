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
package org.hisp.dhis.webapi;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Jan Bernitt
 */
public final class JsonData
{
    public enum Preference
    {
        SKIP_NULL_MEMBERS,
        SKIP_NULL_ELEMENTS,
        SKIP_EMPTY_ARRAYS
    }

    private final ObjectMapper jackson;

    private final EnumSet<Preference> preferences = EnumSet.noneOf( Preference.class );

    public JsonData( ObjectMapper jackson )
    {
        this.jackson = jackson;
    }

    public JsonData with( Preference preference )
    {
        preferences.add( preference );
        return this;
    }

    public JsonData not( Preference preference )
    {
        preferences.remove( preference );
        return this;
    }

    public boolean is( Preference preference )
    {
        return preferences.contains( preference );
    }

    public JsonData skipNullMembers()
    {
        return with( Preference.SKIP_NULL_MEMBERS );
    }

    public JsonData skipNullElements()
    {
        return with( Preference.SKIP_NULL_ELEMENTS );
    }

    public JsonData skipNulls()
    {
        return skipNullElements().skipNullMembers();
    }

    public JsonData skipEmptyArrays()
    {
        return with( Preference.SKIP_EMPTY_ARRAYS );
    }

    public JsonData skipNullOrEmpty()
    {
        return skipNulls().skipEmptyArrays();
    }

    public ArrayNode toArray( List<String> fields, List<Object[]> values )
    {
        ArrayNode arr = jackson.createArrayNode();
        for ( Object[] e : values )
        {
            arr.add( toObject( fields, e ) );
        }
        return arr;
    }

    public ObjectNode toObject( List<String> fields, Object[] values )
    {
        ObjectNode obj = jackson.createObjectNode();
        int i = 0;
        Map<String, ObjectNode> memberObjectsByName = new HashMap<>();
        for ( String field : fields )
        {
            Object value = values[i++];
            if ( !skipValue( value ) )
            {
                JsonNode node = jackson.valueToTree( cleanValue( value ) );
                if ( field.contains( "." ) )
                {
                    String member = field.substring( 0, field.indexOf( '.' ) );
                    ObjectNode memberNode = memberObjectsByName.computeIfAbsent( member,
                        key -> jackson.createObjectNode() );
                    obj.set( member, memberNode );
                    memberNode.set( field.substring( field.indexOf( '.' ) + 1 ), node );
                }
                else
                {
                    obj.set( field, node );
                }
            }
        }
        return obj;
    }

    private Object cleanValue( Object value )
    {
        if ( value instanceof Object[] && is( Preference.SKIP_NULL_ELEMENTS ) )
        {
            long nulls = Arrays.stream( (Object[]) value ).filter( Objects::isNull ).count();
            if ( nulls > 0 )
            {
                return Arrays.stream( (Object[]) value ).filter( Objects::isNull ).toArray();
            }
        }
        return value;
    }

    private boolean skipValue( Object value )
    {
        if ( value == null )
        {
            return is( Preference.SKIP_NULL_MEMBERS );
        }
        if ( value instanceof Object[] )
        {
            return ((Object[]) value).length == 0 && is( Preference.SKIP_EMPTY_ARRAYS );
        }
        return false;
    }
}
