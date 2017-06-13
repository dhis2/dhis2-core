package org.hisp.dhis.commons.filter;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import java.util.Collection;
import java.util.Iterator;

/**
 * Utility class for collection filtering.
 * 
 * @author Lars Helge Overland
 */
public class FilterUtils
{
    /**
     * Filters the given collection using the given {@link Filter}.
     *
     * @param <T> type.
     * @param collection the {@link Collection}.
     * @param filter the filter.
     * @param <V> the type of the collection members.
     * @return the filtered collection, null if any input parameter is null.
     */
    public static <T extends Collection<V>, V> T filter( T collection, Filter<V> filter )
    {
        if ( collection == null || filter == null )
        {
            return null;
        }
        
        final Iterator<V> iterator = collection.iterator();
        
        while ( iterator.hasNext() )
        {
            if ( !filter.retain( iterator.next() ) )
            {
                iterator.remove();
            }
        }
        
        return collection;
    }
    
    /**
     * Filters the given collection using the given {@link Filter} retaining only
     * items which does NOT pass the filter evaluation.
     *
     * @param <T> type.
     * @param collection the {@link Collection}.
     * @param filter the filter.
     * @param <V> the type of the collection members.
     * @return the inverse filtered collection, null if any input parameter is null.
     */
    public static <T extends Collection<V>, V> T inverseFilter( T collection, Filter<V> filter )
    {
        if ( collection == null || filter == null )
        {
            return null;
        }
        
        final Iterator<V> iterator = collection.iterator();
        
        while ( iterator.hasNext() )
        {
            if ( filter.retain( iterator.next() ) )
            {
                iterator.remove();
            }
        }
        
        return collection;
    }    
}
