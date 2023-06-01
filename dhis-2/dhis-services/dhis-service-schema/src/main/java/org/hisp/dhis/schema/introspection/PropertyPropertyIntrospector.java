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
package org.hisp.dhis.schema.introspection;

import static org.hisp.dhis.schema.PropertyType.BOOLEAN;
import static org.hisp.dhis.schema.PropertyType.CONSTANT;
import static org.hisp.dhis.schema.PropertyType.DATE;
import static org.hisp.dhis.schema.PropertyType.REFERENCE;

import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property.Access;
import org.hisp.dhis.schema.annotation.Property.Value;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.schema.annotation.PropertyTransformer;
import org.springframework.util.Assert;

import com.google.common.primitives.Primitives;

/**
 * A {@link PropertyIntrospector} adds information to existing {@link Property}
 * values if they are annotated with one of the following annotations:
 *
 * <ul>
 * <li>{@link org.hisp.dhis.schema.annotation.Property}</li>
 * <li>{@link PropertyRange}</li>
 * <li>{@link PropertyTransformer}</li>
 * </ul>
 *
 * It will also initialise the {@link Property#getMin()} and
 * {@link Property#getMax()} based on the overall information available for the
 * {@link Property}.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com> (original author)
 * @author Jan Bernitt (extracted from {@link JacksonPropertyIntrospector})
 */
public class PropertyPropertyIntrospector implements PropertyIntrospector
{
    private static final Set<PropertyType> PROPS_IGNORE_MIN_MAX = EnumSet.of( REFERENCE, BOOLEAN, DATE, CONSTANT );

    @Override
    public void introspect( Class<?> klass, Map<String, Property> properties )
    {
        for ( Property property : properties.values() )
        {
            if ( property.getKlass() != null )
            {
                updatePropertyTypes( property );
            }
        }
    }

    private static void updatePropertyTypes( Property property )
    {
        Assert.notNull( property, "Property cannot be null" );
        Assert.notNull( property.getKlass(), "Property class cannot be null" );

        property.setPropertyType( getPropertyType( property.getKlass() ) );

        if ( property.isCollection() )
        {
            property.setItemPropertyType( getPropertyType( property.getItemKlass() ) );
        }

        if ( property.isWritable() )
        {
            initFromPropertyAnnotation( property );
            initFromPropertyTransformerAnnotation( property );
            initFromPropertyRangeAnnotation( property );
            ensureMinMaxDefaults( property );
        }
        else
        {
            property.setMin( null );
            property.setMax( null );
        }
    }

    private static void ensureMinMaxDefaults( Property property )
    {
        if ( property.getMin() == null )
        {
            property.setMin( 0d );
        }

        if ( property.getMax() == null )
        {
            property.setMax( property.is( PropertyType.INTEGER, PropertyType.TEXT )
                ? (double) Integer.MAX_VALUE
                : Double.MAX_VALUE );
        }

        if ( !property.isCollection() && PROPS_IGNORE_MIN_MAX.contains( property.getPropertyType() ) )
        {
            property.setMin( null );
            property.setMax( null );
        }
    }

    private static void initFromPropertyRangeAnnotation( Property property )
    {
        PropertyRange range = property.getAnnotation( PropertyRange.class );

        if ( range != null )
        {
            double max = range.max();
            double min = range.min();

            if ( max > Integer.MAX_VALUE
                && property.is( PropertyType.INTEGER, PropertyType.TEXT, PropertyType.COLLECTION ) )
            {
                max = Integer.MAX_VALUE;
            }

            if ( property.is( PropertyType.COLLECTION ) && min < 0 )
            {
                min = 0d;
            }

            // Max will be applied from PropertyRange annotation only if it
            // is more restrictive than hibernate max (or its a collection)
            if ( property.getMax() == null || max < property.getMax() || property.is( PropertyType.COLLECTION ) )
            {
                property.setMax( max );
            }

            // Min is not set by hibernate (always 0) hence the min from
            // PropertyRange will always be applied.
            property.setMin( min );
        }
    }

    private static void initFromPropertyTransformerAnnotation( Property property )
    {
        PropertyTransformer transformer = property.getAnnotation( PropertyTransformer.class );

        if ( transformer != null )
        {
            property.setPropertyTransformer( transformer.value() );
        }
    }

    private static void initFromPropertyAnnotation( Property property )
    {
        org.hisp.dhis.schema.annotation.Property propertyAnnotation = property
            .getAnnotation( org.hisp.dhis.schema.annotation.Property.class );

        if ( propertyAnnotation != null )
        {
            if ( propertyAnnotation.value() != PropertyType.DEFAULT )
            {
                property.setPropertyType( propertyAnnotation.value() );
            }

            if ( propertyAnnotation.required() != Value.DEFAULT )
            {
                property.setRequired( propertyAnnotation.required() == Value.TRUE );
            }

            if ( propertyAnnotation.persisted() != Value.DEFAULT )
            {
                property.setPersisted( propertyAnnotation.persisted() == Value.TRUE );
            }

            if ( propertyAnnotation.owner() != Value.DEFAULT )
            {
                property.setOwner( propertyAnnotation.owner() == Value.TRUE );
            }

            if ( Access.READ_ONLY == propertyAnnotation.access() )
            {
                property.setWritable( false );
            }

            if ( Access.WRITE_ONLY == propertyAnnotation.access() )
            {
                property.setReadable( false );
            }

            if ( !propertyAnnotation.persistedAs().isEmpty() )
            {
                property.setFieldName( propertyAnnotation.persistedAs() );
            }
        }
    }

    private static PropertyType getPropertyType( Class<?> klass )
    {
        klass = Primitives.wrap( klass );

        if ( isAssignableFrom( klass, String.class )
            || isAssignableFrom( klass, Character.class )
            || isAssignableFrom( klass, Byte.class )
            || isAssignableFrom( klass, Class.class ) )
        {
            return PropertyType.TEXT;
        }
        if ( isAssignableFrom( klass, Integer.class ) )
        {
            return PropertyType.INTEGER;
        }
        if ( isAssignableFrom( klass, Boolean.class ) )
        {
            return PropertyType.BOOLEAN;
        }
        if ( isAssignableFrom( klass, Float.class )
            || isAssignableFrom( klass, Double.class ) )
        {
            return PropertyType.NUMBER;
        }
        if ( isAssignableFrom( klass, Date.class )
            || isAssignableFrom( klass, java.sql.Date.class ) )
        {
            return PropertyType.DATE;
        }
        if ( isAssignableFrom( klass, Enum.class ) )
        {
            return PropertyType.CONSTANT;
        }
        if ( isAssignableFrom( klass, IdentifiableObject.class ) )
        {
            return PropertyType.REFERENCE;
        }
        if ( isAssignableFrom( klass, Collection.class ) )
        {
            return PropertyType.COLLECTION;
        }

        // if klass is primitive (but unknown), fall back to text, if its not
        // then assume complex
        return Primitives.isWrapperType( klass ) ? PropertyType.TEXT : PropertyType.COMPLEX;
    }

    private static boolean isAssignableFrom( Class<?> propertyKlass, Class<?> klass )
    {
        return klass.isAssignableFrom( propertyKlass );
    }

}
