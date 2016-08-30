package org.hisp.dhis.pushanalysis;
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
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.user.UserGroup;

import java.util.Date;
import java.util.Set;

/**
 * @author Stian Sandvold
 */
@JacksonXmlRootElement( localName = "pushanalysis", namespace = DxfNamespaces.DXF_2_0 )
public class PushAnalysis
    extends BaseIdentifiableObject
{

    private Dashboard dashboard;

    private Set<UserGroup> receivingUserGroups;

    private String name;

    private String message;

    private boolean enabled;

    private Date lastRun;

    public PushAnalysis()
    {

    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getLastRun()
    {
        return lastRun;
    }

    public void setLastRun( Date lastRun )
    {
        this.lastRun = lastRun;
    }

    public PushAnalysis( String name )
    {
        this.name = name;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Dashboard getDashboard()
    {
        return dashboard;
    }

    public void setDashboard( Dashboard dashboard )
    {
        this.dashboard = dashboard;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Set<UserGroup> getReceivingUserGroups()
    {
        return receivingUserGroups;
    }

    public void setReceivingUserGroups( Set<UserGroup> receivingUserGroups )
    {
        this.receivingUserGroups = receivingUserGroups;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getMessage()
    {
        return message;
    }

    public void setMessage( String message )
    {
        this.message = message;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {

        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {

            PushAnalysis pushAnalysis = (PushAnalysis) other;

            if ( mergeMode.isReplace() )
            {
                dashboard = pushAnalysis.getDashboard();
                receivingUserGroups = pushAnalysis.getReceivingUserGroups();
                name = pushAnalysis.getName();
                message = pushAnalysis.getMessage();
            }

            if ( mergeMode.isMerge() )
            {
                dashboard = pushAnalysis.getDashboard() == null ? dashboard : pushAnalysis.getDashboard();
                receivingUserGroups = pushAnalysis.getReceivingUserGroups() == null ?
                    receivingUserGroups :
                    pushAnalysis.getReceivingUserGroups();
                name = pushAnalysis.getName() == null ? name : pushAnalysis.getName();
                message = pushAnalysis.getMessage() == null ? message : pushAnalysis.getMessage();
            }
        }
    }

    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean getEnabled()
    {
        return this.enabled;
    }


}
