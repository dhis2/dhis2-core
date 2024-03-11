package org.hisp.dhis.common;

/*
 * Copyright (c) 2004-2018, University of Oslo
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
import java.util.List;

/**
 * Class which wraps an object to make it easy to sort. This class provides a
 * numeric for sorting and implements the Comparable interface.
 * 
 * @author Lars Helge Overland
 */
public class NumericSortWrapper<T>
    implements Comparable<NumericSortWrapper<T>>
{
    private T object;

    private Double number;
    
    private int sortOrder;
    
    /**
     * @param object the object to wrap.
     * @param number the number to use as basis for sorting.
     * @param sortOrder the sort order, negative
     */
    public NumericSortWrapper( T object, Double number, int sortOrder )
    {
        this.object = object;
        this.number = number;
        this.sortOrder = sortOrder;
    }
    
    @Override
    public int compareTo( NumericSortWrapper<T> other )
    {
        if ( sortOrder < 0 )
        {
            return number != null ? other != null ? number.compareTo( other.getNumber() ) : 1 : -1;
        }
        else
        {
            return other != null && other.getNumber() != null ? number != null ? other.getNumber().compareTo( number ) : 1 : -1;
        }
    }
    
    public T getObject()
    {
        return object;
    }
    
    public Double getNumber()
    {
        return number;
    }
    
    public static <T> List<T> getObjectList( List<NumericSortWrapper<T>> wrapperList )
    {
        List<T> list = new ArrayList<>();
        
        for ( NumericSortWrapper<T> wrapper : wrapperList )
        {
            list.add( wrapper.getObject() );
        }
        
        return list;
    }
    
    public String toString()
    {
        return "[Number: " + number + ", object: " + object + "]"; 
    }
}
