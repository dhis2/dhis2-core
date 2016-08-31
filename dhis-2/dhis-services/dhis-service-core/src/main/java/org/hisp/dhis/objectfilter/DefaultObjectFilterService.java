package org.hisp.dhis.objectfilter;

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

import com.google.common.collect.Lists;
import org.hisp.dhis.objectfilter.ops.Op;
import org.hisp.dhis.objectfilter.ops.OpStatus;
import org.hisp.dhis.parser.ParserService;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DefaultObjectFilterService implements ObjectFilterService
{
    @Autowired
    private ParserService parserService;

    @Autowired
    private SchemaService schemaService;

    @Override
    public <T> List<T> filter( List<T> objects, List<String> filters )
    {
        if ( objects == null || objects.isEmpty() )
        {
            return Lists.newArrayList();
        }

        Filters parsed = parserService.parseObjectFilter( filters );

        List<T> list = Lists.newArrayList();

        for ( T object : objects )
        {
            if ( evaluateWithFilters( object, parsed ) )
            {
                list.add( object );
            }
        }

        return list;
    }

    @SuppressWarnings( "unchecked" )
    private <T> boolean evaluateWithFilters( T object, Filters filters )
    {
        if ( object == null )
        {
            return false;
        }

        Schema schema = schemaService.getDynamicSchema( object.getClass() );

        for ( String field : filters.getFilters().keySet() )
        {
            if ( !schema.haveProperty( field ) )
            {
                continue;
            }

            Property descriptor = schema.getProperty( field );

            if ( descriptor == null )
            {
                continue;
            }

            Object value = ReflectionUtils.invokeMethod( object, descriptor.getGetterMethod() );

            Object filter = filters.getFilters().get( field );

            if ( FilterOps.class.isInstance( filter ) )
            {
                if ( evaluateFilterOps( value, (FilterOps) filter ) )
                {
                    return false;
                }
            }
            else
            {
                Map<String, Object> map = (Map<String, Object>) filters.getFilters().get( field );
                Filters f = new Filters();
                f.setFilters( map );

                if ( map.containsKey( "__self__" ) )
                {
                    if ( evaluateFilterOps( value, (FilterOps) map.get( "__self__" ) ) )
                    {
                        return false;
                    }

                    map.remove( "__self__" );
                }

                if ( !descriptor.isCollection() )
                {
                    if ( !evaluateWithFilters( value, f ) )
                    {
                        return false;
                    }
                }
                else
                {
                    Collection<?> objectCollection = (Collection<?>) value;

                    if ( objectCollection.isEmpty() )
                    {
                        return false;
                    }

                    boolean include = false;

                    for ( Object idObject : objectCollection )
                    {
                        if ( evaluateWithFilters( idObject, f ) )
                        {
                            include = true;
                        }
                    }

                    if ( !include )
                    {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean evaluateFilterOps( Object value, FilterOps filterOps )
    {
        for ( Op op : filterOps.getFilters() )
        {
            OpStatus status = op.evaluate( value );

            if ( OpStatus.EXCLUDE.equals( status ) )
            {
                return true;
            }
        }

        return false;
    }
}
