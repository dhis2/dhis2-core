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
import lombok.Value;

import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.IdentifiableObject;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * @author Stian Sandvold
 */
@Value
@Builder
@JsonDeserialize( builder = TrackerIdSchemeParam.TrackerIdSchemeParamBuilder.class )
@AllArgsConstructor( staticName = "of" )
public class TrackerIdSchemeParam
{
    public static final TrackerIdSchemeParam UID = builder().idScheme( TrackerIdScheme.UID ).build();

    public static final TrackerIdSchemeParam CODE = builder().idScheme( TrackerIdScheme.CODE ).build();

    public static final TrackerIdSchemeParam NAME = builder().idScheme( TrackerIdScheme.NAME ).build();

    @JsonProperty
    @Builder.Default
    private final TrackerIdScheme idScheme = TrackerIdScheme.UID;

    @JsonProperty
    @Builder.Default
    private final String value = null;

    /**
     * Creates a TrackerIdentifier of idScheme ATTRIBUTE.
     *
     * @param value the attribute value
     * @return tracker identifier representing an attribute
     */
    public static TrackerIdSchemeParam ofAttribute( String value )
    {
        return new TrackerIdSchemeParam( TrackerIdScheme.ATTRIBUTE, value );
    }

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
        }

        throw new RuntimeException( "Unhandled identifier type." );
    }

    public <T extends IdentifiableObject> String getIdAndName( T object )
    {
        String identifier = getIdentifier( object );
        return object.getClass().getSimpleName() + " (" + identifier + ")";
    }
}
