package org.hisp.dhis.deletedobject;

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
 *
 */

import com.google.common.base.MoreObjects;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.Pager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DeletedObjectQuery
{
    public static final DeletedObjectQuery EMPTY = new DeletedObjectQuery();

    private List<String> klass = new ArrayList<>();

    private List<String> uid = new ArrayList<>();

    private List<String> code = new ArrayList<>();

    private Date deletedAt;

    private boolean skipPaging;

    private int page = 1;

    private int pageSize = Pager.DEFAULT_PAGE_SIZE;

    private int total;

    public DeletedObjectQuery()
    {
    }

    public DeletedObjectQuery( IdentifiableObject identifiableObject )
    {
        Assert.notNull( identifiableObject, "identifiableObject is a required parameter and can not be null." );

        klass.add( ClassUtils.getShortName( identifiableObject.getClass() ) );
        uid.add( identifiableObject.getUid() );

        if ( !StringUtils.isEmpty( identifiableObject.getCode() ) )
        {
            code.add( identifiableObject.getCode() );
        }
    }

    public List<String> getKlass()
    {
        return klass;
    }

    public void setKlass( List<String> klass )
    {
        this.klass = klass;
    }

    public List<String> getUid()
    {
        return uid;
    }

    public void setUid( List<String> uid )
    {
        this.uid = uid;
    }

    public List<String> getCode()
    {
        return code;
    }

    public void setCode( List<String> code )
    {
        this.code = code;
    }

    public Date getDeletedAt()
    {
        return deletedAt;
    }

    public void setDeletedAt( Date deletedAt )
    {
        this.deletedAt = deletedAt;
    }

    public boolean isSkipPaging()
    {
        return skipPaging;
    }

    public void setSkipPaging( boolean skipPaging )
    {
        this.skipPaging = skipPaging;
    }

    public int getPage()
    {
        return page;
    }

    public void setPage( int page )
    {
        this.page = page;
    }

    public int getPageSize()
    {
        return pageSize;
    }

    public void setPageSize( int pageSize )
    {
        this.pageSize = pageSize;
    }

    public int getTotal()
    {
        return total;
    }

    public void setTotal( int total )
    {
        this.total = total;
    }

    public Pager getPager()
    {
        return skipPaging ? null : new Pager( page, total, pageSize );
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "klass", klass )
            .add( "uid", uid )
            .add( "code", code )
            .add( "deletedAt", deletedAt )
            .add( "page", page )
            .add( "pageSize", pageSize )
            .add( "total", total )
            .toString();
    }
}
