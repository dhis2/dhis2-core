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
package org.hisp.dhis.webapi.common;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.UidObject;

/**
 * UID represents an alphanumeric string of 11 characters starting with a
 * letter.
 */
@Getter
@EqualsAndHashCode
public final class UID
{
    private static final String VALID_UID_FORMAT = "UID must be an alphanumeric string of 11 characters starting with a letter.";

    private final String value;

    private UID( String value )
    {
        if ( !CodeGenerator.isValidUid( value ) )
        {
            throw new IllegalArgumentException( VALID_UID_FORMAT );
        }
        this.value = value;
    }

    @Override
    public String toString()
    {
        return value;
    }

    public static UID of( String value )
    {
        return new UID( value );
    }

    public static UID of( UidObject object )
    {
        return new UID( object.getUid() );
    }

    public static Set<String> toValueSet( Collection<UID> uids )
    {
        return uids.stream().map( UID::getValue ).collect( toUnmodifiableSet() );
    }

    public static List<String> toValueList( Collection<UID> uids )
    {
        return uids.stream().map( UID::getValue ).toList();
    }
}
