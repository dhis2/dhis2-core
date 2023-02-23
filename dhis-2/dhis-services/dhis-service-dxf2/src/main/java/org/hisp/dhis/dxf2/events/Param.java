/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.dxf2.events;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author Luca Cambi <luca@dhis2.org>
 */
public enum Param
{
    ATTRIBUTES( null, "attributes" ),
    RELATIONSHIPS( null, "relationships" ),
    PROGRAM_OWNERS( null, "programOwners" ),
    DELETED( null, "deleted" ),
    ENROLLMENTS( null, "enrollments" ),
    EVENTS( null, "events" ),
    EVENTS_RELATIONSHIPS( EVENTS, RELATIONSHIPS.getFieldPath() ),
    ENROLLMENTS_EVENTS( ENROLLMENTS, EVENTS.getFieldPath() ),
    ENROLLMENTS_RELATIONSHIPS( ENROLLMENTS, RELATIONSHIPS.getFieldPath() ),
    ENROLLMENTS_ATTRIBUTES( ENROLLMENTS, ATTRIBUTES.getFieldPath() ),
    ENROLLMENTS_EVENTS_RELATIONSHIPS( ENROLLMENTS, EVENTS_RELATIONSHIPS.getFieldPath() ),

    RELATIONSHIP_FROM( null, "from" ),
    RELATIONSHIP_TO( null, "to" );

    private final Optional<Param> prefix;

    private final String field;

    Param( Param prefix, String field )
    {
        this.prefix = null == prefix ? Optional.empty() : Optional.of( prefix );
        this.field = field;
    }

    Optional<Param> getPrefix()
    {
        return this.prefix;
    }

    String getField()
    {
        return this.field;
    }

    public String getFieldPath()
    {
        return this.prefix.isPresent() ? String.join( ".", this.getPrefix().get().getField(),
            this.field ) : this.field;
    }

    static Param fromFieldPath( String fieldPath )
    {
        return Arrays.stream( values() ).filter( p -> p.getFieldPath().equals( fieldPath ) ).findFirst().orElse( null );
    }
}
