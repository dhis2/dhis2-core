package org.hisp.dhis.commons.collection;

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
import java.util.Collection;

/**
 * List implementation that will only add items which are not already present in
 * the list.
 * 
 * @author Lars Helge Overland
 */
public class UniqueArrayList<E>
    extends ArrayList<E>
{
    public UniqueArrayList()
    {
        super();
    }
    
    public UniqueArrayList( Collection<? extends E> c )
    {
        super();
        addAll( c );
    }
    
    @Override
    public boolean add( E e )
    {
        return super.contains( e ) ? false : super.add( e );
    }
    
    @Override
    public void add( int index, E e )
    {
        if ( !super.contains( e ) )
        {
            super.add( index, e );
        }
    }
    
    @Override
    public boolean addAll( Collection<? extends E> c )
    {
        boolean modified = false;
        
        if ( c != null )
        {        
            for ( E e : c )
            {
                if ( !super.contains( e ) )
                {
                    super.add( e );
                    modified = true;
                }
            }
        }
        
        return modified;
    }
    
    @Override
    public boolean addAll( int index, Collection<? extends E> c )
    {
        boolean modified = false;
        
        if ( c != null )
        {        
            for ( E e : c )
            {
                if ( !super.contains( e ) )
                {
                    super.add( index++, e );
                    modified = true;
                }
            }
        }
        
        return modified;
    }
}
