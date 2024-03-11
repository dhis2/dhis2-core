/*
 * Copyright (c) 2004-2018, University of Oslo
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

package org.hisp.dhis;

import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TestRunStorage
{
    private static LinkedHashMap<String, String> createdEntities;

    public static void addCreatedEntity( final String resource, final String id )
    {
        if ( createdEntities == null )
        {
            createdEntities = new LinkedHashMap<>();
        }

        createdEntities.put( id, resource );
    }

    public static Map<String, String> getCreatedEntities()
    {
        if ( createdEntities == null )
        {
            return new LinkedHashMap<>();
        }

        return new LinkedHashMap<>( createdEntities );
    }

    public static List<String> getCreatedEntities( String resource )
    {
        if ( createdEntities == null )
        {
            return new ArrayList<>();
        }

        return getCreatedEntities()
            .entrySet().stream()
            .filter( entrySet -> resource.equals( entrySet.getValue() ) )
            .map( entry -> entry.getKey() )
            .collect( toList() );
    }

    public static void removeEntity( final String resource, final String id )
    {
        if ( createdEntities == null )
        {
            return;
        }

        createdEntities.remove( id, resource );
    }

    public static void removeAllEntities()
    {
        if ( createdEntities == null )
        {
            return;
        }

        createdEntities.clear();
    }
}
