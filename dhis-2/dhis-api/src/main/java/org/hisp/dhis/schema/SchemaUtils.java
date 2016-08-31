package org.hisp.dhis.schema;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import com.google.common.collect.Sets;
import com.google.common.primitives.Primitives;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import static org.hisp.dhis.schema.PropertyType.*;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public final class SchemaUtils
{
    private static final Set<PropertyType> PROPS_IGNORE_MINMAX = Sets.newHashSet( REFERENCE, BOOLEAN, DATE, CONSTANT );
    
    public static void updatePropertyTypes( Property property )
    {
        Assert.notNull( property );
        Assert.notNull( property.getKlass() );

        property.setPropertyType( getPropertyType( property.getKlass() ) );

        if ( property.isCollection() )
        {
            property.setItemPropertyType( getPropertyType( property.getItemKlass() ) );
        }

        if ( property.isWritable() )
        {
            if ( property.getGetterMethod().isAnnotationPresent( org.hisp.dhis.schema.annotation.Property.class ) )
            {
                property.setPropertyType( property.getGetterMethod().getAnnotation( org.hisp.dhis.schema.annotation.Property.class ).value() );
            }

            if ( property.getGetterMethod().isAnnotationPresent( PropertyRange.class ) )
            {
                PropertyRange propertyRange = property.getGetterMethod().getAnnotation( PropertyRange.class );

                if ( property.getMax() == null || propertyRange.max() <= property.getMax() )
                {
                    property.setMax( propertyRange.max() );
                }

                if ( property.getMin() == null || (propertyRange.min() >= property.getMin() && propertyRange.min() <= property.getMax()) )
                {
                    property.setMin( propertyRange.min() > property.getMax() ? property.getMax() : propertyRange.min() );
                }
            }

            if ( property.getMin() == null )
            {
                property.setMin( 0 );
            }

            if ( property.getMax() == null )
            {
                property.setMax( Integer.MAX_VALUE );
            }

            if ( PROPS_IGNORE_MINMAX.contains( property.getPropertyType() ) )
            {
                property.setMin( null );
                property.setMax( null );
            }
        }
        else
        {
            property.setMin( null );
            property.setMax( null );
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
        else if ( isAssignableFrom( klass, Integer.class ) )
        {
            return PropertyType.INTEGER;
        }
        else if ( isAssignableFrom( klass, Boolean.class ) )
        {
            return PropertyType.BOOLEAN;
        }
        else if ( isAssignableFrom( klass, Float.class )
            || isAssignableFrom( klass, Double.class ) )
        {
            return PropertyType.NUMBER;
        }
        else if ( isAssignableFrom( klass, Date.class )
            || isAssignableFrom( klass, java.sql.Date.class ) )
        {
            return PropertyType.DATE;
        }
        else if ( isAssignableFrom( klass, Enum.class ) )
        {
            return PropertyType.CONSTANT;
        }
        else if ( isAssignableFrom( klass, IdentifiableObject.class ) )
        {
            return PropertyType.REFERENCE;
        }
        else if ( isAssignableFrom( klass, Collection.class ) )
        {
            return PropertyType.COLLECTION;
        }

        // if klass is primitive (but unknown), fall back to text, if its not then assume reference
        return Primitives.isWrapperType( klass ) ? PropertyType.TEXT : PropertyType.COMPLEX;
    }

    private static boolean isAssignableFrom( Class<?> propertyKlass, Class<?> klass )
    {
        return klass.isAssignableFrom( propertyKlass );
    }

    private SchemaUtils()
    {
    }
}
