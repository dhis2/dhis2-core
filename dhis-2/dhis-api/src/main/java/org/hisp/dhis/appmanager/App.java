package org.hisp.dhis.appmanager;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    @JsonProperty
    private String version;

    @JsonProperty
    private String name;

    @JsonProperty( "launch_path" )
    private String launchPath;

    @JsonProperty( "installs_allowed_from" )
    private String[] installsAllowedFrom;

    @JsonProperty( "default_locale" )
    private String defaultLocale;

    /**
     * Optional.
     */
    @JsonProperty
    private String description;

    @JsonProperty
    private AppIcons icons;

    @JsonProperty
    private AppDeveloper developer;

    @JsonIgnore
    private String locales;

    @JsonProperty
    private AppActivities activities;
    
    @JsonProperty
    private String folderName;

    @JsonProperty
    private String baseUrl;

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------
    
    @JsonProperty
    public String getLaunchUrl()
    {
        if ( baseUrl != null && folderName != null && launchPath != null )
        {
            return baseUrl + "/" + folderName + "/"+ launchPath;
        }
        
        return null;
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
    
    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getLaunchPath()
    {
        return launchPath;
    }

    public void setLaunchPath( String launchPath )
    {
        this.launchPath = launchPath;
    }

    public String[] getInstallsAllowedFrom()
    {
        return installsAllowedFrom;
    }

    public void setInstallsAllowedFrom( String[] installsAllowedFrom )
    {
        this.installsAllowedFrom = installsAllowedFrom;
    }

    public String getDefaultLocale()
    {
        return defaultLocale;
    }

    public void setDefaultLocale( String defaultLocale )
    {
        this.defaultLocale = defaultLocale;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public AppDeveloper getDeveloper()
    {
        return developer;
    }

    public void setDeveloper( AppDeveloper developer )
    {
        this.developer = developer;
    }

    public AppIcons getIcons()
    {
        return icons;
    }

    public void setIcons( AppIcons icons )
    {
        this.icons = icons;
    }

    public String getLocales()
    {
        return locales;
    }

    public void setLocales( String locales )
    {
        this.locales = locales;
    }

    public AppActivities getActivities()
    {
        return activities;
    }

    public void setActivities( AppActivities activities )
    {
        this.activities = activities;
    }

    public String getFolderName()
    {
        return folderName;
    }

    public void setFolderName( String folderName )
    {
        this.folderName = folderName;
    }

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
        return "[" + name + " " + version + "]";
    }
}
