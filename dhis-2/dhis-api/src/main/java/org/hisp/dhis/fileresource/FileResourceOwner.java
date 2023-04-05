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
package org.hisp.dhis.fileresource;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Backwards reference from a storage key to the object that is associated with
 * the {@link FileResource}.
 *
 * A {@link FileResourceDomain#DATA_VALUE} uses the 4 data value keys
 * {@link #de}, {@link #ou}, {@link #pe} and {@link #co}, all other types use
 * only the {@link #id}.
 *
 * @author Jan Bernitt
 */
@Value
@AllArgsConstructor( access = AccessLevel.PRIVATE )
public class FileResourceOwner
{
    @JsonProperty
    FileResourceDomain domain;

    // for data values
    @JsonProperty
    String de;

    @JsonProperty
    String ou;

    @JsonProperty
    String pe;

    @JsonProperty
    String co;

    // for any other domain
    @JsonProperty
    String id;

    public FileResourceOwner( FileResourceDomain domain, String id )
    {
        this( domain, null, null, null, null, id );
    }

    public FileResourceOwner( String de, String ou, String pe, String co )
    {
        this( FileResourceDomain.DATA_VALUE, de, ou, pe, co, null );
    }
}
