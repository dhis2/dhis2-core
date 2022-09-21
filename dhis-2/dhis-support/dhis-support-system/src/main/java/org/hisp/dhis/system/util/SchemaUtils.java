/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
package org.hisp.dhis.system.util;

import static org.hisp.dhis.schema.PropertyType.*;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.schema.annotation.PropertyTransformer;
import org.springframework.util.Assert;

import com.google.common.collect.Sets;
import com.google.common.primitives.Primitives;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public final class SchemaUtils
{
    private static final Set<PropertyType> PROPS_IGNORE_MINMAX = Sets.newHashSet( REFERENCE, BOOLEAN, DATE, CONSTANT );

    public static void updatePropertyTypes( Property property )
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
            if ( AnnotationUtils.isAnnotationPresent( property.getGetterMethod(),
                org.hisp.dhis.schema.annotation.Property.class ) )
            {
                org.hisp.dhis.schema.annotation.Property pAnnotation = AnnotationUtils
                    .getAnnotation( property.getGetterMethod(), org.hisp.dhis.schema.annotation.Property.class );
                property.setPropertyType( pAnnotation.value() );

                if ( pAnnotation.required() != org.hisp.dhis.schema.annotation.Property.Value.DEFAULT )
                {
                    property
                        .setRequired( pAnnotation.required() == org.hisp.dhis.schema.annotation.Property.Value.TRUE );
                }

                if ( pAnnotation.persisted() != org.hisp.dhis.schema.annotation.Property.Value.DEFAULT )
                {
                    property
                        .setPersisted( pAnnotation.persisted() == org.hisp.dhis.schema.annotation.Property.Value.TRUE );
                }

                if ( pAnnotation.owner() != org.hisp.dhis.schema.annotation.Property.Value.DEFAULT )
                {
                    property.setOwner( pAnnotation.owner() == org.hisp.dhis.schema.annotation.Property.Value.TRUE );
                }

                if ( org.hisp.dhis.schema.annotation.Property.Access.READ_ONLY == pAnnotation.access() )
                {
                    property.setWritable( false );
                }

                if ( org.hisp.dhis.schema.annotation.Property.Access.WRITE_ONLY == pAnnotation.access() )
                {
                    property.setReadable( false );
                }
            }

            if ( AnnotationUtils.isAnnotationPresent( property.getGetterMethod(), PropertyTransformer.class ) )
            {
                PropertyTransformer propertyTransformer = AnnotationUtils.getAnnotation( property.getGetterMethod(),
                    PropertyTransformer.class );
                property.setPropertyTransformer( propertyTransformer.value() );
            }

            if ( AnnotationUtils.isAnnotationPresent( property.getGetterMethod(), PropertyRange.class ) )
            {
                PropertyRange propertyRange = AnnotationUtils.getAnnotation( property.getGetterMethod(),
                    PropertyRange.class );

                double max = propertyRange.max();
                double min = propertyRange.min();

                if ( property.is( PropertyType.INTEGER ) || property.is( PropertyType.TEXT )
                    || property.is( PropertyType.COLLECTION ) )
                {
                    if ( max > Integer.MAX_VALUE )
                    {
                        max = Integer.MAX_VALUE;
                    }
                }

                if ( property.is( PropertyType.COLLECTION ) && min < 0 )
                {
                    min = 0d;
                }

<<<<<<< HEAD
                //Max will be applied from PropertyRange annotation only if it is more restrictive than hibernate max (or its a collection)
=======
                // Max will be applied from PropertyRange annotation only if it
                // is more restrictive than hibernate max (or its a collection)
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
                if ( property.getMax() == null || max < property.getMax() || property.is( PropertyType.COLLECTION ) )
                {
                    property.setMax( max );
                }

<<<<<<< HEAD
                //Min is not set by hibernate (always 0) hence the min from PropertyRange will always be applied.
=======
                // Min is not set by hibernate (always 0) hence the min from
                // PropertyRange will always be applied.
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
                property.setMin( min );
            }

            if ( property.getMin() == null )
            {
                property.setMin( 0d );
            }

            if ( property.getMax() == null )
            {
                if ( property.is( PropertyType.INTEGER ) || property.is( PropertyType.TEXT ) )
                {
                    property.setMax( (double) Integer.MAX_VALUE );
                }
                else
                {
                    property.setMax( Double.MAX_VALUE );
                }
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

        // if klass is primitive (but unknown), fall back to text, if its not
        // then assume reference
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
