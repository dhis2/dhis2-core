package org.hisp.dhis.tracker.converter;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hisp.dhis.tracker.domain.TrackerDto;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import lombok.SneakyThrows;

/**
 * @author Luciano Fiandesio
 */
public class ConverterUtils
{
    private ConverterUtils()
    {
    }

    /*
    Memoize the TrackeDto fields
     */
    private final static LoadingCache<Class<? extends TrackerDto>, Set<Field>> memoizedFields = CacheBuilder
        .newBuilder()
        .build( CacheLoader.from( ConverterUtils::getFields ) );

    /**
     * Extract all the {@link Field} from a given object having the following
     * characteristics:
     *
     * - the field is not a Collection
     * 
     * - the field value is not null or empty String
     * 
     * @param clazz the class of the object to analyze
     * @param entity the object from which to extract the Fields
     * @return a List of {@link Field}
     */
    @SneakyThrows
    public static List<Field> getPatchFields( Class<? extends TrackerDto> clazz, Object entity )
    {
        // Get all the fields on the "patchable" entity
        final Set<Field> fields = memoizedFields.get( clazz );

        Predicate<Field> isFromDeclaringClass = f -> f.getDeclaringClass().equals( clazz );
        Predicate<Field> isNotCollection = f -> !Collection.class.isAssignableFrom( f.getType() );
        Predicate<Field> isNotEmpty = f -> isEmpty( f, entity );

        return fields.stream()
            .filter( isFromDeclaringClass )
            .filter( isNotCollection )
            .peek( ReflectionUtils::makeAccessible )
            .filter( isNotEmpty )
            .collect( Collectors.toList() );
    }

    private static boolean isEmpty( Field f, Object entity )
    {
        if ( String.class.isAssignableFrom( f.getType() ) )
        {
            return !StringUtils.isEmpty( ReflectionUtils.getField( f, entity ) );
        }
        else
        {
            return ReflectionUtils.getField( f, entity ) != null;
        }
    }

    private static Set<Field> getFields( Class<? extends TrackerDto> clazz )
    {
        return new Reflections( clazz, new FieldAnnotationsScanner() )
            .getFieldsAnnotatedWith( JsonProperty.class );
    }
}
