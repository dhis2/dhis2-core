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
package org.hisp.dhis.webapi.webdomain.approval;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.period.Period;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@OpenApi.Shared
@JacksonXmlRootElement( localName = "approvals", namespace = DxfNamespaces.DXF_2_0 )
public class ApprovalsDto
{
    private List<String> wf = new ArrayList<>();

    private List<String> ds = new ArrayList<>();

    private List<String> pe = new ArrayList<>();

    private List<ApprovalDto> approvals = new ArrayList<>();

    public ApprovalsDto()
    {
    }

    @JsonProperty
    @OpenApi.Property( { UID[].class, DataApprovalWorkflow.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public List<String> getWf()
    {
        return wf;
    }

    public void setWf( List<String> wf )
    {
        this.wf = wf;
    }

    @JsonProperty
    @OpenApi.Property( { UID[].class, DataSet.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public List<String> getDs()
    {
        return ds;
    }

    public void setDs( List<String> ds )
    {
        this.ds = ds;
    }

    @JsonProperty
    @OpenApi.Property( Period[].class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public List<String> getPe()
    {
        return pe;
    }

    public void setPe( List<String> pe )
    {
        this.pe = pe;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public List<ApprovalDto> getApprovals()
    {
        return approvals;
    }

    public void setApprovals( List<ApprovalDto> approvals )
    {
        this.approvals = approvals;
    }
}
