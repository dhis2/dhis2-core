package org.hisp.dhis.organisationunit;

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
public class CoordinatesTuple
{
    private List<String> coordinatesTuple = new ArrayList<>();

    public void addCoordinates( String coordinates )
    {
        this.coordinatesTuple.add( coordinates );
    }
    
    public long getNumberOfCoordinates()
    {
        return this.coordinatesTuple.size();
    }
    
    public List<String> getCoordinatesTuple()
    {
        return coordinatesTuple;
    }
    
    public boolean hasCoordinates()
    {
        return this.coordinatesTuple != null && this.coordinatesTuple.size() > 0;
    }
    
    public static boolean hasCoordinates( List<CoordinatesTuple> list )
    {
        if  ( list != null && list.size() > 0 )
        {
            for ( CoordinatesTuple tuple : list )
            {
                if ( tuple.hasCoordinates() )
                {
                    return true;
                }
            }
        }
        
        return false;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        
        for ( String c : coordinatesTuple )
        {
            result = prime * result + c.hashCode();
        }
        
        return result;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        
        if ( o == null )
        {
            return false;
        }
        
        if ( getClass() != o.getClass() )
        {
            return false;
        }
        
        final CoordinatesTuple other = (CoordinatesTuple) o;

        if ( coordinatesTuple.size() != other.getCoordinatesTuple().size() )
        {
            return false;
        }
        
        int size = coordinatesTuple.size();
        
        for ( int i = 0; i < size; i++ )
        {
            if ( !coordinatesTuple.get( i ).equals( other.getCoordinatesTuple().get( i ) ) )
            {
                return false;
            }
        }
        
        return true;
    }    
}
