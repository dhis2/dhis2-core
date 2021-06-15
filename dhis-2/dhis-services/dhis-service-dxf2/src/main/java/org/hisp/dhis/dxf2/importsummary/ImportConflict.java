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
package org.hisp.dhis.dxf2.importsummary;

import static java.util.Objects.requireNonNull;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.i18n.I18n;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * A {@link ImportConflict} can be used in two ways:
 *
 * <dl>
 * <dt>Informal (legacy)</dt>
 * <dd></dd>Providing {@link #object} and {@link #message} - both are part of
 * the {@link #groupingKey}</dd>
 * <dt>Formal (recommended)</dt>
 * <dd>Providing {@link #objects} and {@link #errorCode} to create the
 * {@link #groupingKey}. Other fields are optional to give more details on the
 * conflict in a structured way.</dd>
 * </dl>
 *
 * @author Jason P. Pickering (original)
 * @author Jan Bernitt (extended)
 */
@JsonInclude( Include.NON_NULL )
@JacksonXmlRootElement( localName = "conflict", namespace = DxfNamespaces.DXF_2_0 )
public final class ImportConflict
{
    public static ImportConflict createConflict( I18n i18n,
        Function<Class<? extends IdentifiableObject>, String> singularNameForType,
        int index, ImportConflictDescriptor descriptor, String... objects )
    {
        Class<?>[] objectTypes = descriptor.getObjectTypes();
        Map<String, String> objectsMap = new LinkedHashMap<>();
        for ( int i = 0; i < objects.length; i++ )
        {
            Class<?> objectType = objectTypes[i];
            String object = objects[i];
            if ( objectType == I18n.class )
            {
                objects[i] = i18n.getString( object );
            }
            else if ( IdentifiableObject.class.isAssignableFrom( objectType ) )
            {
                @SuppressWarnings( "unchecked" )
                Class<? extends IdentifiableObject> type = (Class<? extends IdentifiableObject>) objectType;
                objectsMap.put( singularNameForType.apply( type ), object );
            }
            else
            {
                objectsMap.put( descriptor.getProperty(), object );
            }
        }
        ErrorCode errorCode = descriptor.getErrorCode();
        String message = MessageFormat.format( errorCode.getMessage(), (Object[]) objects );
        return new ImportConflict( objectsMap, message, errorCode, descriptor.getProperty(), index );
    }

    /**
     * Identifies a unique conflict, that is the same type of error occurring
     * for the same reason potentially for different value (but for each of them
     * because of the same conflicting combination of referenced objects)
     */
    private final String groupingKey;

    /**
     * Identifies the type of error
     */
    private final ErrorCode errorCode;

    /**
     * The ID of the object having causing the conflict.
     */
    private final String object;

    /**
     * The error message.
     */
    private final String message;

    /**
     * Optional to refer to a single property of the imported object that is the
     * cause of the conflict.
     */
    private final String property;

    /**
     * What type of object does {@link #object} refer to? Uses the singular from
     * schema.
     */
    private final Map<String, String> objects;

    /**
     * A list of indexes pointing out the index of the conflicting element in
     * the set/list of imported elements.
     */
    private int[] indexes;

    private int indexLength;

    public ImportConflict( String object, String message )
    {
        this( object + ":" + message, object, message, null, null, null, -1 );
        requireNonNull( object );
        requireNonNull( message );
    }

    public ImportConflict( Map<String, String> objects, String message, ErrorCode errorCode, String property,
        int index )
    {
        this( errorCode.name() + ":" + String.join( ":", objects.values() ), objects.values().iterator().next(),
            message, errorCode, objects, property, index );
    }

    private ImportConflict( String groupingKey, String object, String message, ErrorCode errorCode,
        Map<String, String> objects, String property, int index )
    {
        this.groupingKey = groupingKey;
        this.errorCode = errorCode;
        this.object = object;
        this.message = message;
        this.objects = objects;
        this.property = property;
        if ( index >= 0 )
        {
            this.indexes = new int[index];
            this.indexLength = 1;
        }
        else
        {
            this.indexes = null;
            this.indexLength = 0;
        }
    }

    @JsonIgnore
    public String getGroupingKey()
    {
        return groupingKey;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public ErrorCode getErrorCode()
    {
        return errorCode;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getObject()
    {
        return object;
    }

    /**
     * OBS! Note that the property cannot be renamed to maintain backwards
     * compatibility
     *
     * @return A conflict error message text
     */
    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getValue()
    {
        return message;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getProperty()
    {
        return property;
    }

    @JsonAnyGetter
    public Map<String, String> getObjects()
    {
        return objects;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public int[] getIndexes()
    {
        return Arrays.copyOf( indexes, indexLength );
    }

    public ImportConflict mergeWith( ImportConflict other )
    {
        if ( other.errorCode != errorCode || !object.equals( other.object ) )
        {
            throw new IllegalArgumentException( "Only errors of same code and object reference can be merged." );
        }
        if ( other.indexLength == 0 )
        {
            return this;
        }
        int newLength = Math.max( indexLength * 2, indexLength + other.indexLength );
        if ( newLength > indexes.length )
        {
            this.indexes = Arrays.copyOf( indexes, newLength );
        }
        if ( other.indexLength == 1 )
        {
            this.indexes[indexLength++] = other.indexes[0];
        }
        else
        {
            System.arraycopy( other.indexes, 0, this.indexes, indexLength, other.indexLength );
            this.indexLength += other.indexLength;
        }
        return this;
    }

    @Override
    public int hashCode()
    {
        return groupingKey.hashCode() ^ Arrays.hashCode( indexes );
    }

    /**
     * Class check uses isAssignableFrom and get-methods to handle proxied
     * objects.
     */
    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( !(obj instanceof ImportConflict) )
        {
            return false;
        }

        final ImportConflict other = (ImportConflict) obj;
        if ( !Objects.equals( groupingKey, other.groupingKey )
            || indexLength != other.indexLength
            || !Objects.equals( property, other.property ) )
        {
            return false;
        }
        for ( int i = 0; i < indexLength; i++ )
        {
            if ( indexes[i] != other.indexes[i] )
            {
                return false;
            }
        }
        return true;
    }

}
