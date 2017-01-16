package org.hisp.dhis.appmanager;

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * @author Saptarshi
 */
public class App
    implements Serializable
{
    /**
     * Determines if a de-serialized file is compatible with this class.
     */
    private static final long serialVersionUID = -6638197841892194228L;

    /**
     * Required.
     */
    private String version;

    private String name;

    private AppType appType = AppType.APP;

    private String launchPath;

    private String[] installsAllowedFrom;

    private String defaultLocale;

    /**
     * Optional.
     */
    private String description;

    private AppIcons icons;

    private AppDeveloper developer;

    private String locales;

    private AppActivities activities;

    private String folderName;

    private String launchUrl;

    private String baseUrl;

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    /**
     * Initializes the app. Sets the launchUrl property.
     *
     * @param contextPath the context path of this instance.
     */
    public void init( String contextPath )
    {
        this.baseUrl = contextPath + "/api/apps";

        if ( contextPath != null && folderName != null && launchPath != null )
        {
            launchUrl = baseUrl + "/" + folderName + "/" + launchPath;
        }
    }

    /**
     * Alias for folder name.
     */
    @JsonProperty
    public String getKey()
    {
        return folderName;
    }

    // -------------------------------------------------------------------------
    // Get and set methods
    // -------------------------------------------------------------------------
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
    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
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

    @JsonProperty( "launch_path" )
    public String getLaunchPath()
    {
        return launchPath;
    }

    public void setLaunchPath( String launchPath )
    {
        this.launchPath = launchPath;
    }

    @JsonProperty( "installs_allowed_from" )
    public String[] getInstallsAllowedFrom()
    {
        return installsAllowedFrom;
    }

    public void setInstallsAllowedFrom( String[] installsAllowedFrom )
    {
        this.installsAllowedFrom = installsAllowedFrom;
    }

    @JsonProperty( "default_locale" )
    public String getDefaultLocale()
    {
        return defaultLocale;
    }

    public void setDefaultLocale( String defaultLocale )
    {
        this.defaultLocale = defaultLocale;
    }

    @JsonProperty
    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    @JsonProperty
    public AppDeveloper getDeveloper()
    {
        return developer;
    }

    public void setDeveloper( AppDeveloper developer )
    {
        this.developer = developer;
    }

    @JsonProperty
    public AppIcons getIcons()
    {
        return icons;
    }

    public void setIcons( AppIcons icons )
    {
        this.icons = icons;
    }

    @JsonIgnore
    public String getLocales()
    {
        return locales;
    }

    public void setLocales( String locales )
    {
        this.locales = locales;
    }

    @JsonProperty
    public AppActivities getActivities()
    {
        return activities;
    }

    public void setActivities( AppActivities activities )
    {
        this.activities = activities;
    }

    @JsonProperty
    public String getFolderName()
    {
        return folderName;
    }

    public void setFolderName( String folderName )
    {
        this.folderName = folderName;
    }

    @JsonProperty
    public String getLaunchUrl()
    {
        return launchUrl;
    }

    public void setLaunchUrl( String launchUrl )
    {
        this.launchUrl = launchUrl;
    }

    @JsonIgnore
    public String getBaseUrl()
    {
        return baseUrl;
    }

    public void setBaseUrl( String baseUrl )
    {
        this.baseUrl = baseUrl;
    }

    // -------------------------------------------------------------------------
    // hashCode, equals, toString
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 79 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( obj == null )
        {
            return false;
        }

        if ( getClass() != obj.getClass() )
        {
            return false;
        }

        final App other = (App) obj;

        if ( (this.name == null) ? (other.name != null) : !this.name.equals( other.name ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public String toString()
    {
        return "{" +
            "\"version:\"" + version + "\", " +
            "\"name:\"" + name + "\", " +
            "\"appType:\"" + appType + "\", " +
            "\"launchPath:\"" + launchPath + "\" " +
            "}";
    }
}
