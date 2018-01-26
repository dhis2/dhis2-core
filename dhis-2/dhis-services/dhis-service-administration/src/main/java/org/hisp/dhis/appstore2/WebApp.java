package org.hisp.dhis.appstore2;

/*
 * Copyright (c) 2004-2018, University of Oslo
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
import org.hisp.dhis.appmanager.AppStatus;
import org.hisp.dhis.appmanager.AppType;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by zubair@dhis2.org on 07.09.17.
 */
public class WebApp
{
    private String id; // uid

    private String name;

    private String description;

    private String owner;

    private Date created;

    private Date lastUpdated;

    private String sourceUrl;

    private AppStatus status;

    private AppType appType;

    private Developer developer;

    private Set<AppVersion> versions = new HashSet<>();

    private Set<Review> reviews = new HashSet<>();

    private Set<ImageResource> images = new HashSet<>();

    public WebApp()
    {
    }

    @JsonProperty
    public AppType getAppType()
    {
        return appType;
    }

    public void setAppType( AppType appType )
    {
        this.appType = appType;
    }

    @JsonProperty
    public String getDescription() {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    @JsonProperty
    public Developer getDeveloper() {
        return developer;
    }

    public void setDeveloper( Developer developer )
    {
        this.developer = developer;
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
    public Set<ImageResource> getImages()
    {
        return images;
    }

    public void setImages( Set<ImageResource> images )
    {
        this.images = images;
    }

    @JsonProperty
    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    @JsonProperty
    public String getOwner()
    {
        return owner;
    }

    public void setOwner( String owner )
    {
        this.owner = owner;
    }

    @JsonProperty
    public Set<Review> getReviews()
    {
        return reviews;
    }

    public void setReviews( Set<Review> reviews )
    {
        this.reviews = reviews;
    }

    @JsonProperty
    public String getSourceUrl()
    {
        return sourceUrl;
    }

    public void setSourceUrl( String sourceUrl )
    {
        this.sourceUrl = sourceUrl;
    }

    @JsonProperty
    public AppStatus getStatus()
    {
        return status;
    }

    public void setStatus( AppStatus status )
    {
        this.status = status;
    }

    @JsonProperty
    public Set<AppVersion> getVersions()
    {
        return versions;
    }

    public void setVersions( Set<AppVersion> versions )
    {
        this.versions = versions;
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
    public Date getLastUpdated()
    {
        return lastUpdated;
    }

    public void setLastUpdated( Date lastUpdated )
    {
        this.lastUpdated = lastUpdated;
    }
}
