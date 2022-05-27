/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "pager", namespace = DxfNamespaces.DXF_2_0 )
public class Pager
{
    public static final int DEFAULT_PAGE_SIZE = 50;

    private int page = 1;

    private long total = 0;

    private int pageSize = Pager.DEFAULT_PAGE_SIZE;

    private String nextPage;

    private String prevPage;

    public Pager()
    {

    }

    public Pager( int page, long total )
    {
        this.total = total;
        this.page = getPageInternal( page );
    }

    public Pager( int page, long total, int pageSize )
    {
        this.total = Math.max( total, 0 );
        this.pageSize = pageSize > 0 ? pageSize : 1;
        this.page = getPageInternal( page );
    }

    @Override
    public String toString()
    {
        return "[Page: " + page + " total: " + total + " size: " + pageSize + " offset: " + getOffset() + "]";
    }

    /**
     * Returns the page, ensuring the value is greater or equal to 1 and less or
     * equal to total page count.
     *
     * @param page the page.
     * @return the page.
     */
    private int getPageInternal( int page )
    {
        page = Math.min( page, getPageCount() );
        return Math.max( page, 1 );
    }

    public boolean pageSizeIsDefault()
    {
        return pageSize == Pager.DEFAULT_PAGE_SIZE;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getPage()
    {
        return page;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public long getTotal()
    {
        return total;
    }

    /**
     * Returns the number of items per page.
     *
     * @return The number of items per page.
     */
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getPageSize()
    {
        return pageSize;
    }

    /**
     * Returns the total number of pages.
     *
     * @return The total number of pages.
     */
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getPageCount()
    {
        return (int) Math.ceil( total / (double) pageSize );
    }

    /**
     * Return the current offset to start at.
     *
     * @return The offset to start at.
     */
    public int getOffset()
    {
        return (page * pageSize) - pageSize;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getNextPage()
    {
        return nextPage;
    }

    public void setNextPage( String nextPage )
    {
        this.nextPage = nextPage;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getPrevPage()
    {
        return prevPage;
    }

    public void setPrevPage( String prevPage )
    {
        this.prevPage = prevPage;
    }

    /**
     * Sets pagination directly.
     *
     * @param page the page.
     * @param pageSize the page size.
     */
    public void force( int page, int pageSize )
    {
        this.page = page;
        this.pageSize = pageSize;
    }
}
