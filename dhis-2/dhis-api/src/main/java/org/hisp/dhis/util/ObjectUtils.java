package org.hisp.dhis.util;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Lars Helge Overland
 */
public class ObjectUtils
{
    /**
     * Returns the first non-null argument. Returns null if all arguments are null.
     * 
     * @param objects the objects.
     * @return the first non-null argument.
     */
    @SafeVarargs
    public static final <T> T firstNonNull( T... objects )
    {
        if ( objects != null )
        {
            for ( T object : objects )
            {
                if ( object != null )
                {
                    return object;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Indicates whether all of the given argument object are not null.
     * 
     * @param objects the objects.
     * @return true if all of the given argument object are not null.
     */
    public static final boolean allNonNull( Object... objects )
    {
        if ( objects == null )
        {
            return false;
        }
       
        for ( Object object : objects )
        {
            if ( object == null )
            {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Indicates whether any of the given conditions are not null and true.
     * 
     * @param conditions the conditions.
     * @return whether any of the given conditions are not null and true.
     */
    public static final boolean anyIsTrue( Boolean... conditions )
    {
        if ( conditions != null )
        {
            for ( Boolean condition : conditions )
            {
                if ( condition != null && condition.booleanValue() )
                {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Returns a list of strings, where the strings are the result of calling
     * String.valueOf( Object ) of each object in the given collection.
     * 
     * @param objects the collection of objects.
     * @return a list of strings.
     */
    public static List<String> asStringList( Collection<? extends Object> objects )
    {
        List<String> list = new ArrayList<>();
        
        for ( Object object : objects )
        {
            list.add( String.valueOf( object ) );
        }
        
        return list;
    }
        
    /**
     * Joins the elements of the provided collection into a string. The 
     * provided string mapping function is used to produce the string for each
     * object. Null is returned if the provided collection is null.
     * 
     * @param collection the collection of elements.
     * @param separator the separator of elements in the returned string.
     * @param stringMapper the function to produce the string for each object.
     * @return the joined string.
     */
    public static <T> String join( Collection<T> collection, String separator, Function<T, String> stringMapper )
    {
        if ( collection == null )
        {
            return null;
        }
        
        List<String> list = collection.stream().map( stringMapper ).collect( Collectors.toList() );
        
        return StringUtils.join( list, separator );
    }
}
