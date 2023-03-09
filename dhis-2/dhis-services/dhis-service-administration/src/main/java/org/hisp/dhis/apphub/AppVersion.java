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
package org.hisp.dhis.apphub;

import java.util.Date;

import org.apache.commons.io.FilenameUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by zubair@dhis2.org on 07.09.17.
 */
public class AppVersion
{
    private String id;

    private String appId;

    private String version;

    private String channel;

    private String minDhisVersion;

    private String maxDhisVersion;

    private String downloadUrl;

    private String demoUrl;

    private Integer downloadCount;

    private Date created;

    private Date lastUpdated;

    public AppVersion()
    {
        // empty constructor
    }

    @JsonIgnore
    public String getFilename()
    {
        return FilenameUtils.getName( downloadUrl );
    }

    @JsonProperty
    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }


    @JsonProperty
    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    @JsonProperty
    public String getMinDhisVersion()
    {
        return minDhisVersion;
    }

    public void setMinDhisVersion( String minDhisVersion )
    {
        this.minDhisVersion = minDhisVersion;
    }

    @JsonProperty
    public String getMaxDhisVersion()
    {
        return maxDhisVersion;
    }

    public void setMaxDhisVersion( String maxDhisVersion )
    {
        this.maxDhisVersion = maxDhisVersion;
    }

    @JsonProperty
    public String getDownloadUrl()
    {
        return downloadUrl;
    }

    public void setDownloadUrl( String downloadUrl )
    {
        this.downloadUrl = downloadUrl;
    }

    @JsonProperty
    public Integer getDownloadCount()
    {
        return downloadCount;
    }

    public void setDownloadCount( Integer downloadCount )
    {
        this.downloadCount = downloadCount;
    }

    @JsonProperty
    public String getDemoUrl()
    {
        return demoUrl;
    }

    public void setDemoUrl( String demoUrl )
    {
        this.demoUrl = demoUrl;
    }

    @JsonProperty
    public Date getCreated()
    {
        return created;
    }

    public void setCreated( Date created )
    {
        this.created = created;
    }

    @JsonProperty
    public String getAppId()
    {
        return appId;
    }

    public void setAppId( String appId )
    {
        this.appId = appId;
    }

    @JsonProperty
    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    @JsonProperty
    public Date getLastUpdated()
    {
        return lastUpdated;
    }

    public void setLastUpdated( Date lastUpdated )
    {
        this.lastUpdated = lastUpdated;
    }
}
