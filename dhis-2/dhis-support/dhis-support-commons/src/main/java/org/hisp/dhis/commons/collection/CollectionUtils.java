package org.hisp.dhis.commons.collection;

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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Utility methods for operations on various collections.
 *
 * @author Morten Olav Hansen
 */
public class CollectionUtils
{
    public static final String[] STRING_ARR = new String[0];

    /**
     * Returns the intersection of the given Collections.
     *
     * @param c1  the first Collection.
     * @param c2  the second Collection.
     * @param <T> the type.
     * @return the intersection of the Collections.
     */
    public static <T> Collection<T> intersection( Collection<T> c1, Collection<T> c2 )
    {
        Set<T> set1 = new HashSet<>( c1 );
        set1.retainAll( new HashSet<>( c2 ) );
        return set1;
    }

    /**
     * Searches for and returns the first string which starts with the given
     * prefix. Removes the match from the collection.
     *
     * @param collection the collection.
     * @param prefix     the string prefix.
     * @return a string, or null if no matches.
     */
    public static String popStartsWith( Collection<String> collection, String prefix )
    {
        Iterator<String> iterator = collection.iterator();

        while ( iterator.hasNext() )
        {
            String element = iterator.next();

            if ( element != null && element.startsWith( prefix ) )
            {
                iterator.remove();
                return element;
            }
        }

        return null;
    }
}
