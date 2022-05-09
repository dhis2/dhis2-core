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
package org.hisp.dhis.tracker.domain;

import java.util.Objects;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.tracker.TrackerIdScheme;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MetadataIdentifier represents an immutable idScheme aware identifier of
 * metadata.
 *
 * idScheme=ATTRIBUTE uses the {@link #identifier} and {@link #attributeValue}
 * to identify metadata while the other idSchemes only rely on the
 * {@link #identifier} (UID, CODE, NAME).
 */
@Value
@AllArgsConstructor( access = AccessLevel.PRIVATE )
public class MetadataIdentifier
{

    public static MetadataIdentifier EMPTY_UID = MetadataIdentifier.ofUid( (String) null );

    /**
     * Represents the idScheme the {@link #identifier} is in.
     */
    @JsonProperty
    private final TrackerIdScheme idScheme;

    /**
     * Represents the actual identifier of the metadata. UID for idScheme
     * {@code UID} and {@code ATTRIBUTE} in which case its the UID of the
     * metadata attribute, code and name respectively.
     *
     * <strong>CAUTION:</strong> when using the {@code identifier} directly. You
     * loose the context of which idScheme it is in, which was the source of
     * many bugs. If you are doing equality comparisons use
     * {@link #isEqualTo(IdentifiableObject)} instead.
     */
    @JsonProperty
    private final String identifier;

    /**
     * Represents the value of a metadata attribute. It is only non-null if
     * idScheme is {@code ATTRIBUTE}.
     */
    @JsonProperty
    private final String attributeValue;

    /**
     * Creates an identifier for metadata. {@code attributeValue} only needs to
     * be set if idScheme is {@code ATTRIBUTE}. Prefer idScheme specific factory
     * methods {@link #ofUid(String)}, {@link #ofCode(String)},
     * {@link #ofName(String)} and {@link #ofAttribute(String, String)} over
     * this one.
     *
     * @param idScheme idScheme of metadata identifier
     * @param identifier identifier of metadata identifier
     * @param attributeValue attribute value for idScheme ATTRIBUTE
     * @return metadata identifier
     */
    @JsonCreator
    public static MetadataIdentifier of( @JsonProperty( "idScheme" ) TrackerIdScheme idScheme,
        @JsonProperty( "identifier" ) String identifier, @JsonProperty( "attributeValue" ) String attributeValue )
    {
        return new MetadataIdentifier( idScheme, identifier, attributeValue );
    }

    /**
     * Creates an identifier for the given metadata using idScheme UID and its
     * UID.
     *
     * @param metadata identifiable object of which the identifier will be
     *        returned
     * @return metadata identifier representing a UID
     */
    public static MetadataIdentifier ofUid( IdentifiableObject metadata )
    {
        if ( metadata == null )
        {
            return MetadataIdentifier.EMPTY_UID;
        }

        return MetadataIdentifier.ofUid( metadata.getUid() );
    }

    /**
     * Creates an identifier for metadata using idScheme UID and the given UID.
     *
     * @param uid metadata uid
     * @return metadata identifier representing a UID
     */
    public static MetadataIdentifier ofUid( String uid )
    {
        return new MetadataIdentifier( TrackerIdScheme.UID, uid, null );
    }

    /**
     * Creates an identifier for metadata using idScheme CODE and the given
     * code.
     *
     * @param code metadata code
     * @return metadata identifier representing a code
     */
    public static MetadataIdentifier ofCode( String code )
    {
        return new MetadataIdentifier( TrackerIdScheme.CODE, code, null );
    }

    /**
     * Creates an identifier for metadata using idScheme NAME and the given
     * name.
     *
     * @param name metadata name
     * @return metadata identifier representing a name
     */
    public static MetadataIdentifier ofName( String name )
    {
        return new MetadataIdentifier( TrackerIdScheme.NAME, name, null );
    }

    /**
     * Creates an identifier for metadata using idScheme ATTRIBUTE and the given
     * attribute uid and value.
     *
     * @param uid metadata attribute uid
     * @param value metadata attribute value
     * @return metadata identifier representing an attribute
     */
    public static MetadataIdentifier ofAttribute( String uid, String value )
    {
        return new MetadataIdentifier( TrackerIdScheme.ATTRIBUTE, uid, value );
    }

    /**
     * Returns the objects' identifier matching this {@link #idScheme}.
     *
     * @param metadata identifiable object of which the identifier will be
     *        returned
     * @param <T> identifiable object
     * @return identifier of given identifiable object
     */
    public <T extends IdentifiableObject> String identifierOf( T metadata )
    {
        switch ( idScheme )
        {
        case UID:
            return metadata.getUid();
        case CODE:
            return metadata.getCode();
        case NAME:
            return metadata.getName();
        case ATTRIBUTE:
            return metadata.getAttributeValues()
                .stream()
                .filter( av -> av.getAttribute().getUid().equals( this.identifier ) )
                .map( AttributeValue::getValue )
                .findFirst()
                .orElse( null );
        }

        throw new RuntimeException( "Unhandled identifier type." );
    }

    /**
     * Returns the {@link #identifier} for idScheme {@code UID}, {@code CODE},
     * {@code NAME} or {@link #attributeValue} for idScheme {@code ATTRIBUTE}.
     * Used for example as a unique key in the {@code TrackerPreheat} during a
     * tracker import. An attribute is usually only uniquely identified via its
     * UID and value. A tracker import uses the same UID for all metadata
     * identifiers overall or per metadata type (defined by query parameters).
     * Therefore, the {@link #attributeValue} is enough to uniquely identify an
     * entity if partitioned per metadata class (i.e. Program, ProgramStage,
     * ...).
     *
     * @return identifier or attribute value
     */
    public String getIdentifierOrAttributeValue()
    {

        if ( this.idScheme == TrackerIdScheme.ATTRIBUTE )
        {
            return this.attributeValue;
        }
        return this.identifier;
    }

    /**
     * Determines whether given metadata is identified by this metadata
     * identifier.
     *
     * @param metadata to compare to
     * @return true if metadata is identified by this identifier
     */
    public boolean isEqualTo( IdentifiableObject metadata )
    {
        if ( metadata == null )
        {
            return false;
        }

        final String thatId = this.identifierOf( metadata );
        if ( this.idScheme == TrackerIdScheme.ATTRIBUTE )
        {
            return Objects.equals( this.attributeValue, thatId );
        }

        return Objects.equals( this.identifier, thatId );
    }

    /**
     * Determines whether this metadata identifier is blank.
     *
     * @return true if identifier is blank
     */
    public boolean isBlank()
    {
        return StringUtils.isBlank( this.getIdentifierOrAttributeValue() );
    }
}
