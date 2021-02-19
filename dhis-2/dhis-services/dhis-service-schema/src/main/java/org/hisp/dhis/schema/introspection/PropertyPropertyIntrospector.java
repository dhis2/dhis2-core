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
package org.hisp.dhis.schema.introspection;

import static org.hisp.dhis.schema.PropertyType.BOOLEAN;
import static org.hisp.dhis.schema.PropertyType.CONSTANT;
import static org.hisp.dhis.schema.PropertyType.DATE;
import static org.hisp.dhis.schema.PropertyType.REFERENCE;
import static org.hisp.dhis.system.util.AnnotationUtils.getAnnotation;
import static org.hisp.dhis.system.util.AnnotationUtils.isAnnotationPresent;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
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

import com.google.common.collect.Sets;
import com.google.common.primitives.Primitives;

/**
 * A {@link PropertyIntrospector} that reads
 * {@link org.hisp.dhis.schema.annotation.Property} annotation (if present) to
 * update existing {@link Property} values in the map.
 *
 * It will not add {@link Property} values to the map.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com> (original author)
 * @author Jan Bernitt (extracted from {@link JacksonPropertyIntrospector})
 */
public class PropertyPropertyIntrospector implements PropertyIntrospector
{
    private static final Set<PropertyType> PROPS_IGNORE_MIN_MAX = Sets.newHashSet( REFERENCE, BOOLEAN, DATE, CONSTANT );

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
        if ( isAnnotationPresent( property.getGetterMethod(), PropertyRange.class ) )
        {
            PropertyRange range = getAnnotation( property.getGetterMethod(), PropertyRange.class );

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
        Method getter = property.getGetterMethod();
        if ( isAnnotationPresent( getter, PropertyTransformer.class ) )
        {
            property.setPropertyTransformer( getAnnotation( getter, PropertyTransformer.class ).value() );
        }
    }

    private static void initFromPropertyAnnotation( Property property )
    {
        Method getter = property.getGetterMethod();
        if ( isAnnotationPresent( getter, org.hisp.dhis.schema.annotation.Property.class ) )
        {
            org.hisp.dhis.schema.annotation.Property pAnnotation = getAnnotation( getter,
                org.hisp.dhis.schema.annotation.Property.class );
            property.setPropertyType( pAnnotation.value() );

            if ( pAnnotation.required() != Value.DEFAULT )
            {
                property.setRequired( pAnnotation.required() == Value.TRUE );
            }

            if ( pAnnotation.persisted() != Value.DEFAULT )
            {
                property.setPersisted( pAnnotation.persisted() == Value.TRUE );
            }

            if ( pAnnotation.owner() != Value.DEFAULT )
            {
                property.setOwner( pAnnotation.owner() == Value.TRUE );
            }

            if ( Access.READ_ONLY == pAnnotation.access() )
            {
                property.setWritable( false );
            }

            if ( Access.WRITE_ONLY == pAnnotation.access() )
            {
                property.setReadable( false );
            }
        }
    }

    private static PropertyType getPropertyType( Class<?> klass )
    {
        if ( isAssignableFrom( klass, String.class )
            || isAssignableFrom( klass, Character.class )
            || isAssignableFrom( klass, Byte.class ) )
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
        // then assume reference
        return Primitives.isWrapperType( klass ) ? PropertyType.TEXT : PropertyType.COMPLEX;
    }

    private static boolean isAssignableFrom( Class<?> propertyKlass, Class<?> klass )
    {
        return klass.isAssignableFrom( propertyKlass );
    }

}
