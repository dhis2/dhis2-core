package org.hisp.dhis.appstore;

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

import org.apache.commons.io.FilenameUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Lars Helge Overland
 */
public class WebAppVersion
{
    private String id;
    
    private String version;
    
    private String minPlatformVersion;
    
    private String maxPlatformVersion;
    
    private String downloadUrl;
    
    private String demoUrl;
    
    public WebAppVersion()
    {
    }

    @JsonIgnore
    public String getFilename()
    {
        return FilenameUtils.getName( downloadUrl );
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
    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    @JsonProperty( value = "min_platform_version" )
    public String getMinPlatformVersion()
    {
        return minPlatformVersion;
    }

    public void setMinPlatformVersion( String minPlatformVersion )
    {
        this.minPlatformVersion = minPlatformVersion;
    }

    @JsonProperty( value = "max_platform_version" )
    public String getMaxPlatformVersion()
    {
        return maxPlatformVersion;
    }

    public void setMaxPlatformVersion( String maxPlatformVersion )
    {
        this.maxPlatformVersion = maxPlatformVersion;
    }

    @JsonProperty( value = "download_url" )
    public String getDownloadUrl()
    {
        return downloadUrl;
    }

    public void setDownloadUrl( String downloadUrl )
    {
        this.downloadUrl = downloadUrl;
    }

    @JsonProperty( value = "demo_url" )
    public String getDemoUrl()
    {
        return demoUrl;
    }

    public void setDemoUrl( String demoUrl )
    {
        this.demoUrl = demoUrl;
    }
}
