package org.hisp.dhis.system;

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

import java.util.Date;

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.springframework.beans.BeanUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "systemInfo", namespace = DxfNamespaces.DXF_2_0 )
public class SystemInfo
{
    // -------------------------------------------------------------------------
    // Transient properties
    // -------------------------------------------------------------------------

    private String contextPath;

    private String userAgent;

    // -------------------------------------------------------------------------
    // Volatile properties
    // -------------------------------------------------------------------------

    private String calendar;

    private String dateFormat;

    private Date serverDate;

    private Date lastAnalyticsTableSuccess;

    private String intervalSinceLastAnalyticsTableSuccess;

    private String lastAnalyticsTableRuntime;

    // -------------------------------------------------------------------------
    // Stable properties
    // -------------------------------------------------------------------------

    private String version;

    private String revision;

    private Date buildTime;

    private String jasperReportsVersion;

    private String environmentVariable;

    private String fileStoreProvider;

    private String cacheProvider;

    private String readOnlyMode;
    
    private String nodeId;

    private String javaVersion;

    private String javaVendor;

    private String javaOpts;

    private String osName;

    private String osArchitecture;

    private String osVersion;

    private String externalDirectory;

    private DatabaseInfo databaseInfo;

    private Integer readReplicaCount;

    private String memoryInfo;

    private Integer cpuCores;

    private boolean encryption;
    
    private String systemId;
    
    private String systemName;

    private String systemMetadataVersion;

    private Boolean isMetadataVersionEnabled;

    private Date lastMetadataVersionSyncAttempt;

    private boolean isMetadataSyncEnabled;

    public SystemInfo instance()
    {
        SystemInfo info = new SystemInfo();
        BeanUtils.copyProperties( this, info );
        return info;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public void clearSensitiveInfo()
    {
        this.fileStoreProvider = null;
        this.cacheProvider = null;
        this.readOnlyMode = null;
        this.nodeId = null;
        this.javaVersion = null;
        this.javaVendor = null;
        this.javaOpts = null;
        this.osName = null;
        this.osArchitecture = null;
        this.osVersion = null;
        this.externalDirectory = null;
        this.cacheProvider = null;
        this.readReplicaCount = null;
        this.memoryInfo = null;
        this.cpuCores = null;

        if ( this.databaseInfo != null )
        {
            this.databaseInfo.clearSensitiveInfo();
        }
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getContextPath()
    {
        return contextPath;
    }

    public void setContextPath( String contextPath )
    {
        this.contextPath = contextPath;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getUserAgent()
    {
        return userAgent;
    }

    public void setUserAgent( String userAgent )
    {
        this.userAgent = userAgent;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getCalendar()
    {
        return calendar;
    }

    public void setCalendar( String calendar )
    {
        this.calendar = calendar;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getDateFormat()
    {
        return dateFormat;
    }

    public void setDateFormat( String dateFormat )
    {
        this.dateFormat = dateFormat;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getServerDate()
    {
        return serverDate;
    }

    public void setServerDate( Date serverDate )
    {
        this.serverDate = serverDate;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getLastAnalyticsTableSuccess()
    {
        return lastAnalyticsTableSuccess;
    }

    public void setLastAnalyticsTableSuccess( Date lastAnalyticsTableSuccess )
    {
        this.lastAnalyticsTableSuccess = lastAnalyticsTableSuccess;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getIntervalSinceLastAnalyticsTableSuccess()
    {
        return intervalSinceLastAnalyticsTableSuccess;
    }

    public void setIntervalSinceLastAnalyticsTableSuccess( String intervalSinceLastAnalyticsTableSuccess )
    {
        this.intervalSinceLastAnalyticsTableSuccess = intervalSinceLastAnalyticsTableSuccess;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getLastAnalyticsTableRuntime()
    {
        return lastAnalyticsTableRuntime;
    }

    public void setLastAnalyticsTableRuntime( String lastAnalyticsTableRuntime )
    {
        this.lastAnalyticsTableRuntime = lastAnalyticsTableRuntime;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getRevision()
    {
        return revision;
    }

    public void setRevision( String revision )
    {
        this.revision = revision;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getBuildTime()
    {
        return buildTime;
    }

    public void setBuildTime( Date buildTime )
    {
        this.buildTime = buildTime;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getJasperReportsVersion()
    {
        return jasperReportsVersion;
    }

    public void setJasperReportsVersion( String jasperReportsVersion )
    {
        this.jasperReportsVersion = jasperReportsVersion;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getEnvironmentVariable()
    {
        return environmentVariable;
    }

    public void setEnvironmentVariable( String environmentVariable )
    {
        this.environmentVariable = environmentVariable;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getFileStoreProvider()
    {
        return fileStoreProvider;
    }

    public void setFileStoreProvider( String fileStoreProvider )
    {
        this.fileStoreProvider = fileStoreProvider;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getCacheProvider()
    {
        return cacheProvider;
    }

    public void setCacheProvider( String cacheProvider )
    {
        this.cacheProvider = cacheProvider;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getReadOnlyMode()
    {
        return readOnlyMode;
    }

    public void setReadOnlyMode( String readOnlyMode )
    {
        this.readOnlyMode = readOnlyMode;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getNodeId()
    {
        return nodeId;
    }

    public void setNodeId( String nodeId )
    {
        this.nodeId = nodeId;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getJavaVersion()
    {
        return javaVersion;
    }

    public void setJavaVersion( String javaVersion )
    {
        this.javaVersion = javaVersion;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getJavaVendor()
    {
        return javaVendor;
    }

    public void setJavaVendor( String javaVendor )
    {
        this.javaVendor = javaVendor;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getJavaOpts()
    {
        return javaOpts;
    }

    public void setJavaOpts( String javaOpts )
    {
        this.javaOpts = javaOpts;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getOsName()
    {
        return osName;
    }

    public void setOsName( String osName )
    {
        this.osName = osName;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getOsArchitecture()
    {
        return osArchitecture;
    }

    public void setOsArchitecture( String osArchitecture )
    {
        this.osArchitecture = osArchitecture;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getOsVersion()
    {
        return osVersion;
    }

    public void setOsVersion( String osVersion )
    {
        this.osVersion = osVersion;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getExternalDirectory()
    {
        return externalDirectory;
    }

    public void setExternalDirectory( String externalDirectory )
    {
        this.externalDirectory = externalDirectory;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DatabaseInfo getDatabaseInfo()
    {
        return databaseInfo;
    }

    public void setDatabaseInfo( DatabaseInfo databaseInfo )
    {
        this.databaseInfo = databaseInfo;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getReadReplicaCount()
    {
        return readReplicaCount;
    }

    public void setReadReplicaCount( Integer readReplicaCount )
    {
        this.readReplicaCount = readReplicaCount;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getMemoryInfo()
    {
        return memoryInfo;
    }

    public void setMemoryInfo( String memoryInfo )
    {
        this.memoryInfo = memoryInfo;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getCpuCores()
    {
        return cpuCores;
    }

    public void setCpuCores( Integer cpuCores )
    {
        this.cpuCores = cpuCores;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isEncryption()
    {
        return encryption;
    }

    public void setEncryption( boolean encryption )
    {
        this.encryption = encryption;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getSystemId()
    {
        return systemId;
    }

    public void setSystemId( String systemId )
    {
        this.systemId = systemId;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getSystemName()
    {
        return systemName;
    }

    public void setSystemName( String systemName )
    {
        this.systemName = systemName;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getSystemMetadataVersion()
    {
        return systemMetadataVersion;
    }

    public void setSystemMetadataVersion( String systemMetadataVersion )
    {
        this.systemMetadataVersion = systemMetadataVersion;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getIsMetadataVersionEnabled()
    {
        return isMetadataVersionEnabled;
    }

    public void setIsMetadataVersionEnabled( Boolean isMetadataVersionEnabled )
    {
        this.isMetadataVersionEnabled = isMetadataVersionEnabled;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getLastMetadataVersionSyncAttempt()
    {
        return lastMetadataVersionSyncAttempt;
    }

    public void setLastMetadataVersionSyncAttempt( Date lastMetadataVersionSyncAttempt )
    {
        this.lastMetadataVersionSyncAttempt = lastMetadataVersionSyncAttempt;
    }

    public void setIsMetadataSyncEnabled( boolean isMetadataSyncEnabled )
    {
        this.isMetadataSyncEnabled = isMetadataSyncEnabled;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean getIsMetadataSyncEnabled()
    {
        return isMetadataSyncEnabled;
    }
}
