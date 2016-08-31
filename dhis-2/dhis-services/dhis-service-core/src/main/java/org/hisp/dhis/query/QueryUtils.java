package org.hisp.dhis.query;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.hisp.dhis.system.util.DateUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public final class QueryUtils
{
    static public <T> T getValue( Class<T> klass, Object objectValue )
    {
        return getValue( klass, null, objectValue );
    }

    @SuppressWarnings( { "rawtypes", "unchecked" } )
    static public <T> T getValue( Class<T> klass, Class<?> secondaryKlass, Object objectValue )
    {
        if ( klass.isInstance( objectValue ) )
        {
            return (T) objectValue;
        }

        if ( !String.class.isInstance( objectValue ) )
        {
            return (T) objectValue;
        }

        String value = (String) objectValue;

        if ( Boolean.class.isAssignableFrom( klass ) )
        {
            try
            {
                return (T) Boolean.valueOf( value );
            }
            catch ( Exception ignored )
            {
            }
        }
        else if ( Integer.class.isAssignableFrom( klass ) )
        {
            try
            {
                return (T) Integer.valueOf( value );
            }
            catch ( Exception ignored )
            {
            }
        }
        else if ( Float.class.isAssignableFrom( klass ) )
        {
            try
            {
                return (T) Float.valueOf( value );
            }
            catch ( Exception ignored )
            {
            }
        }
        else if ( Double.class.isAssignableFrom( klass ) )
        {
            try
            {
                return (T) Double.valueOf( value );
            }
            catch ( Exception ignored )
            {
            }
        }
        else if ( Date.class.isAssignableFrom( klass ) )
        {
            return (T) DateUtils.parseDate( value );
        }
        else if ( Enum.class.isAssignableFrom( klass ) )
        {
            Optional<? extends Enum<?>> enumValue = (Optional<? extends Enum<?>>) Enums.getIfPresent( (Class<? extends Enum>) klass, value );

            if ( enumValue.isPresent() )
            {
                return (T) enumValue.get();
            }
        }
        else if ( Collection.class.isAssignableFrom( klass ) )
        {
            if ( !value.startsWith( "[" ) || !value.endsWith( "]" ) )
            {
                return null;
            }

            String[] split = value.substring( 1, value.length() - 1 ).split( "," );
            List<String> items = Lists.newArrayList( split );

            if ( secondaryKlass != null )
            {
                List<Object> convertedList = new ArrayList<>();

                for ( String item : items )
                {
                    Object convertedValue = getValue( secondaryKlass, null, item );

                    if ( convertedValue != null )
                    {
                        convertedList.add( convertedValue );
                    }
                }

                return (T) convertedList;
            }

            return (T) items;
        }

        return null;
    }

    private QueryUtils()
    {
    }
}
