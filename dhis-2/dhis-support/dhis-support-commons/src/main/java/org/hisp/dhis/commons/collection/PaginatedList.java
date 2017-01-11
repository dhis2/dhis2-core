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
import java.util.List;

/**
 * List implementation that provides a paged view.
 * 
 * @author Lars Helge Overland
 */
public class PaginatedList<T>
    extends ArrayList<T>
{
    /**
     * Determines if a de-serialized file is compatible with this class.
     */
    private static final long serialVersionUID = 6296545460322554660L;

    public static final int DEFAULT_PAGE_SIZE = 10;
    
    private int pageSize;

    private int fromIndex = 0;
    
    public PaginatedList( Collection<T> collection )
    {
        super( collection );
        this.pageSize = DEFAULT_PAGE_SIZE;
    }
    
    /**
     * Sets page size.
     *
     * @param pageSize the page size.
     * @return this PaginatedList.
     */
    public PaginatedList<T> setPageSize( int pageSize )
    {
        this.pageSize = pageSize;
        
        return this;
    }
    
    /**
     * Sets the number of pages. The page size will be calculated and set in
     * order to provide the appropriate total number of pages. The resulting
     * number of pages can be lower than the given argument but not higher.
     *
     * @param pages the number of pages.
     * @return this PaginatedList
     */
    public PaginatedList<T> setNumberOfPages( int pages )
    {
        this.pageSize = (int) Math.ceil( (double) size() / pages );
        
        return this;
    }
    
    /**
     * Returns the next page in the list. The page size is defined by the argument
     * given in the constructor. If there is no more pages, null is returned. The
     * returned page is not guaranteed to have the same size as the page size.
     *
     * @return the next page.
     */
    public List<T> nextPage()
    {
        int size = size();
        
        if ( fromIndex >= size )
        {
            return null;
        }
        
        int toIndex = Math.min( ( fromIndex + pageSize ), size );
        
        List<T> page = subList( fromIndex, toIndex );
        
        fromIndex = toIndex;
        
        return page;
    }
    
    /**
     * Sets the current page position to the first page.
     */
    public void reset()
    {
        fromIndex = 0;
    }
    
    /**
     * Returns the number of pages in the list.
     *
     * @return the number of pages.
     */
    public int pageCount()
    {
        int count = size();        
        int pages = count / pageSize;
        int mod = count % pageSize;
        
        return mod == 0 ? pages : ( pages + 1 );
    }
    
    /**
     * Returns a list of all pages.
     *
     * @return a List of all pages.
     */
    public List<List<T>> getPages()
    {
        List<List<T>> pages = new ArrayList<>();
        
        List<T> page = null;
        
        while ( ( page = nextPage() ) != null )
        {
            pages.add( page );
        }
        
        return pages;
    }
}
