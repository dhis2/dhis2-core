package org.hisp.dhis.dataapproval;

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

import org.hisp.dhis.common.DxfNamespaces;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement( localName = "dataApprovalPermissions", namespace = DxfNamespaces.DXF_2_0 )
public class DataApprovalPermissions
{   
    private boolean mayApprove;
    
    private boolean mayUnapprove;
    
    private boolean mayAccept;

    private boolean mayUnaccept;

    private boolean mayReadData;
    
    private transient String state;
    
    public DataApprovalPermissions()
    {
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    public boolean isMayApprove()
    {
        return mayApprove;
    }

    public void setMayApprove( boolean mayApprove )
    {
        this.mayApprove = mayApprove;
    }

    @JsonProperty
    public boolean isMayUnapprove()
    {
        return mayUnapprove;
    }

    public void setMayUnapprove( boolean mayUnapprove )
    {
        this.mayUnapprove = mayUnapprove;
    }

    @JsonProperty
    public boolean isMayAccept()
    {
        return mayAccept;
    }

    public void setMayAccept( boolean mayAccept )
    {
        this.mayAccept = mayAccept;
    }

    @JsonProperty
    public boolean isMayUnaccept()
    {
        return mayUnaccept;
    }

    public void setMayUnaccept( boolean mayUnaccept )
    {
        this.mayUnaccept = mayUnaccept;
    }

    @JsonProperty
    public boolean isMayReadData()
    {
        return mayReadData;
    }

    public void setMayReadData( boolean mayReadData )
    {
        this.mayReadData = mayReadData;
    }

    @JsonProperty
    public String getState()
    {
        return state;
    }

    public void setState( String state )
    {
        this.state = state;
    }

    // ----------------------------------------------------------------------
    // toString
    // ----------------------------------------------------------------------

    @Override
    public String toString()
    {
        return "DataApprovalPermissions{" +
                "mayApprove=" + mayApprove +
                ", mayUnapprove=" + mayUnapprove +
                ", mayAccept=" + mayAccept +
                ", mayUnaccept=" + mayUnaccept +
                ", mayReadData=" + mayReadData +
                '}';
    }
}
