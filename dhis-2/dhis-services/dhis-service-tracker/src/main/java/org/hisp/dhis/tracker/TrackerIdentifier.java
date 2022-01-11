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
package org.hisp.dhis.tracker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.util.ObjectUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Stian Sandvold
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor( staticName = "of" )
public class TrackerIdentifier
{
    public final static TrackerIdentifier UID = builder().idScheme( TrackerIdScheme.UID ).build();

    public final static TrackerIdentifier CODE = builder().idScheme( TrackerIdScheme.CODE ).build();

    public final static TrackerIdentifier NAME = builder().idScheme( TrackerIdScheme.NAME ).build();

    public final static TrackerIdentifier AUTO = builder().idScheme( TrackerIdScheme.AUTO ).build();

    @JsonProperty
    @Builder.Default
    private TrackerIdScheme idScheme = TrackerIdScheme.UID;

    @JsonProperty
    @Builder.Default
    private String value = null;

    public <T extends IdentifiableObject> String getIdentifier( T object )
    {
        switch ( idScheme )
        {
        case UID:
            return object.getUid();
        case CODE:
            return object.getCode();
        case NAME:
            return object.getName();
        case ATTRIBUTE:
            return object.getAttributeValues()
                .stream()
                .filter( av -> av.getAttribute().getUid().equals( value ) )
                .map( AttributeValue::getValue )
                .findFirst()
                .orElse( null );
        case AUTO:
            return ObjectUtils.firstNonNull( object.getUid(), object.getCode() );
        }

        throw new RuntimeException( "Unhandled identifier type." );
    }

    public <T extends IdentifiableObject> String getIdAndName( T object )
    {
        String identifier = getIdentifier( object );
        return object.getClass().getSimpleName() + " (" + identifier + ")";
    }
}
