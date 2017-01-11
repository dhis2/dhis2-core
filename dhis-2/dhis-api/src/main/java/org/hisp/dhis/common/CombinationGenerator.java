package org.hisp.dhis.common;

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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lars Helge Overland
 */
public class CombinationGenerator<T>
{
    private T[][] objects; // Array of object arrays
    private int[] indexes; // Current index for each array
    private int no; // No of arrays
    
    @SafeVarargs
    public CombinationGenerator( final T[]... objects )
    {
        this.objects = objects;
        this.indexes = new int[objects.length];
        this.no = objects.length;
        
        if ( no > 0 )
        {
            indexes[no-1]--; // Rewind last index to simplify looping
        }
    }
    
    /**
     * Returns a List of Lists with combinations of objects.
     */
    public List<List<T>> getCombinations()
    {
        final List<List<T>> combinations = new ArrayList<>();
        
        while ( hasNext() )
        {
            combinations.add( getNext() );
        }
        
        return combinations;
    }
    
    /**
     * Indicates whether there are more combinations to be returned or not.
     */
    public boolean hasNext()
    {
        for ( int i = no - 1; i >= 0; i-- )
        {
            if ( indexes[i] < objects[i].length - 1 ) // Not at last position in array
            {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Returns the next combination. Returns null if there are no more combinations.
     */
    public List<T> getNext()
    {
        List<T> current = null;
        
        for ( int i = no - 1; i >= 0; i-- )
        {
            if ( indexes[i] < objects[i].length - 1 ) // Not at last position in array, increment index and break
            {
                indexes[i]++;
                current = getCurrent();
                break;
            }
            else // At last position in array, reset index to 0 and continue to increment next array
            {
                if ( hasNext() ) // Don't reset if at end
                {
                    indexes[i] = 0;
                }
            }
        }
        
        return current;
    }
    
    /**
     * Returns a List with values from the current index of each List.
     */
    private List<T> getCurrent()
    {
        final List<T> current = new ArrayList<>( no );
        
        for ( int i = 0; i < no; i++ )
        {
            int index = indexes[i];
            
            T[] object = objects[i];
            
            current.add( object[index] );
        }
        
        return current;
    }
}
