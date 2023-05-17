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
package org.hisp.dhis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public class ImportCount
{
    private int ignored;

    private int deleted;

    private int updated;

    private int imported;

    private int created;

    private int total;

    public int getIgnored()
    {
        return ignored;
    }

    public void setIgnored( int ignored )
    {
        this.ignored = ignored;
    }

    public int getDeleted()
    {
        return deleted;
    }

    public void setDeleted( int deleted )
    {
        this.deleted = deleted;
    }

    public int getUpdated()
    {
        return updated;
    }

    public void setUpdated( int updated )
    {
        this.updated = updated;
    }

    public int getImported()
    {
        return imported;
    }

    public void setImported( int imported )
    {
        this.imported = imported;
    }

    public int getCreated()
    {
        return created;
    }

    public void setCreated( int created )
    {
        this.created = created;
    }

    public int getTotal()
    {
        return total;
    }

    public void setTotal( int total )
    {
        this.total = total;
    }
}
