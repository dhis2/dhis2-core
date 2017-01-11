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
        this.page = page;
        this.total = total;

        if ( this.page > getPageCount() )
        {
            this.page = getPageCount();
        }

        if ( this.page < 1 )
        {
            this.page = 1;
        }
    }

    public Pager( int page, long total, int pageSize )
    {
        this.page = page;
        this.total = total >= 0 ? total : 0;
        this.pageSize = pageSize > 0 ? pageSize : 1;

        if ( this.page > getPageCount() )
        {
            this.page = getPageCount();
        }

        if ( this.page < 1 )
        {
            this.page = 1;
        }
    }

    public String toString()
    {
        return "[Page: " + page + " total: " + total + " size: " + pageSize + " offset: " + getOffset() + "]";
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
     * How many items per page.
     *
     * @return Number of items per page.
     */
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getPageSize()
    {
        return pageSize;
    }

    /**
     * How many pages in total
     *
     * @return Total number of pages
     */
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getPageCount()
    {
        int pageCount = 1;
        long totalTmp = total;

        while ( totalTmp > pageSize )
        {
            totalTmp -= pageSize;
            pageCount++;
        }

        return pageCount;
    }

    /**
     * Return the current offset to start at.
     *
     * @return Offset to start at
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
}
