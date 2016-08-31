package org.hisp.dhis.security.acl;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.DxfNamespaces;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "access", namespace = DxfNamespaces.DXF_2_0 )
public class Access
{
    private boolean manage;

    private boolean externalize;

    private boolean write;

    private boolean read;

    private boolean update;

    private boolean delete;

    public Access()
    {
    }

    @JsonProperty
    @JacksonXmlProperty( localName = "manage", namespace = DxfNamespaces.DXF_2_0 )
    public boolean isManage()
    {
        return manage;
    }

    public void setManage( boolean manage )
    {
        this.manage = manage;
    }

    @JsonProperty
    @JacksonXmlProperty( localName = "externalize", namespace = DxfNamespaces.DXF_2_0 )
    public boolean isExternalize()
    {
        return externalize;
    }

    public void setExternalize( boolean externalize )
    {
        this.externalize = externalize;
    }

    @JsonProperty
    @JacksonXmlProperty( localName = "write", namespace = DxfNamespaces.DXF_2_0 )
    public boolean isWrite()
    {
        return write;
    }

    public void setWrite( boolean write )
    {
        this.write = write;
    }

    @JsonProperty
    @JacksonXmlProperty( localName = "read", namespace = DxfNamespaces.DXF_2_0 )
    public boolean isRead()
    {
        return read;
    }

    public void setRead( boolean read )
    {
        this.read = read;
    }

    @JsonProperty
    @JacksonXmlProperty( localName = "update", namespace = DxfNamespaces.DXF_2_0 )
    public boolean isUpdate()
    {
        return update;
    }

    public void setUpdate( boolean update )
    {
        this.update = update;
    }

    @JsonProperty
    @JacksonXmlProperty( localName = "delete", namespace = DxfNamespaces.DXF_2_0 )
    public boolean isDelete()
    {
        return delete;
    }

    public void setDelete( boolean delete )
    {
        this.delete = delete;
    }

    @Override
    public String toString()
    {
        return "Access{" +
            "manage=" + manage +
            ", externalize=" + externalize +
            ", write=" + write +
            ", read=" + read +
            ", update=" + update +
            ", delete=" + delete +
            '}';
    }
}
