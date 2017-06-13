package org.hisp.dhis.preheat;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import com.google.common.collect.Lists;
import org.hisp.dhis.common.IdentifiableObject;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public enum PreheatIdentifier
{
    /**
     * Preheat using UID identifiers.
     */
    UID,

    /**
     * Preheat using CODE identifiers.
     */
    CODE,

    /**
     * Find first non-null identifier in order: UID, CODE
     */
    AUTO;

    @SuppressWarnings( "incomplete-switch" )
    public <T extends IdentifiableObject> String getIdentifier( T object )
    {
        switch ( this )
        {
            case UID:
                return object.getUid();
            case CODE:
                return object.getCode();
        }

        throw new RuntimeException( "Unhandled identifier type." );
    }

    public <T extends IdentifiableObject> List<String> getIdentifiers( T object )
    {
        switch ( this )
        {
            case UID:
            {
                return Lists.newArrayList( object.getUid() );
            }
            case CODE:
            {
                return Lists.newArrayList( object.getCode() );
            }
            case AUTO:
            {
                return Lists.newArrayList( object.getUid(), object.getCode() );
            }
        }

        return new ArrayList<>();
    }

    public <T extends IdentifiableObject> String getIdentifiersWithName( T object )
    {
        List<String> identifiers = getIdentifiers( object );
        String name = StringUtils.isEmpty( object.getDisplayName() ) ? null : object.getDisplayName();

        if ( name == null )
        {
            return identifiers.toString() + " (" + object.getClass().getSimpleName() + ")";
        }

        return name + " " + identifiers.toString() + " (" + object.getClass().getSimpleName() + ")";
    }
}
