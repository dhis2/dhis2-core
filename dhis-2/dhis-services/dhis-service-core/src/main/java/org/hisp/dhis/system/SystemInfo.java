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
package org.hisp.dhis.system;

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

    private String serverTimeZoneId;

    private String serverTimeZoneDisplayName;

    private Date lastAnalyticsTableSuccess;

    private String intervalSinceLastAnalyticsTableSuccess;

    private String lastAnalyticsTableRuntime;

    private Date lastSystemMonitoringSuccess;

    private Date lastAnalyticsTablePartitionSuccess;

    private String intervalSinceLastAnalyticsTablePartitionSuccess;

    private String lastAnalyticsTablePartitionRuntime;

    // -------------------------------------------------------------------------
    // Stable properties
    // -------------------------------------------------------------------------

    private String version;

    private String revision;

    private Date buildTime;

    private String jasperReportsVersion;

    private String environmentVariable;

    private String fileStoreProvider;

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

    private boolean emailConfigured;

    private boolean redisEnabled;

    private String redisHostname;

    private String systemId;

    private String systemName;

    private String systemMetadataVersion;

    private String instanceBaseUrl;

    private String systemMonitoringUrl;

    private String clusterHostname;

    private Boolean isMetadataVersionEnabled;

    private Date lastMetadataVersionSyncAttempt;

    private boolean isMetadataSyncEnabled;

    public SystemInfo instance()
    {
        SystemInfo info = new SystemInfo();
        BeanUtils.copyProperties( this, info );
        // clear sensitive info may reset the data
        info.setDatabaseInfo( databaseInfo == null ? null : databaseInfo.instance() );
        return info;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public void clearSensitiveInfo()
    {
        this.fileStoreProvider = null;
        this.readOnlyMode = null;
        this.nodeId = null;
        this.javaVersion = null;
        this.javaVendor = null;
        this.javaOpts = null;
        this.osName = null;
        this.osArchitecture = null;
        this.osVersion = null;
        this.externalDirectory = null;
        this.readReplicaCount = null;
        this.memoryInfo = null;
        this.cpuCores = null;
        this.systemMonitoringUrl = null;

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
    public String getServerTimeZoneId()
    {
        return serverTimeZoneId;
    }

    public void setServerTimeZoneId( String serverTimeZoneId )
    {
        this.serverTimeZoneId = serverTimeZoneId;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getServerTimeZoneDisplayName()
    {
        return serverTimeZoneDisplayName;
    }

    public void setServerTimeZoneDisplayName( String serverTimeZoneDisplayName )
    {
        this.serverTimeZoneDisplayName = serverTimeZoneDisplayName;
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
    public Date getLastAnalyticsTablePartitionSuccess()
    {
        return lastAnalyticsTablePartitionSuccess;
    }

    public void setLastAnalyticsTablePartitionSuccess( Date lastAnalyticsTablePartitionSuccess )
    {
        this.lastAnalyticsTablePartitionSuccess = lastAnalyticsTablePartitionSuccess;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getIntervalSinceLastAnalyticsTablePartitionSuccess()
    {
        return intervalSinceLastAnalyticsTablePartitionSuccess;
    }

    public void setIntervalSinceLastAnalyticsTablePartitionSuccess(
        String intervalSinceLastAnalyticsTablePartitionSuccess )
    {
        this.intervalSinceLastAnalyticsTablePartitionSuccess = intervalSinceLastAnalyticsTablePartitionSuccess;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getLastAnalyticsTablePartitionRuntime()
    {
        return lastAnalyticsTablePartitionRuntime;
    }

    public void setLastAnalyticsTablePartitionRuntime( String lastAnalyticsTablePartitionRuntime )
    {
        this.lastAnalyticsTablePartitionRuntime = lastAnalyticsTablePartitionRuntime;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getLastSystemMonitoringSuccess()
    {
        return lastSystemMonitoringSuccess;
    }

    public void setLastSystemMonitoringSuccess( Date lastSystemMonitoringSuccess )
    {
        this.lastSystemMonitoringSuccess = lastSystemMonitoringSuccess;
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
    public boolean isEmailConfigured()
    {
        return emailConfigured;
    }

    public void setEmailConfigured( boolean emailConfigured )
    {
        this.emailConfigured = emailConfigured;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isRedisEnabled()
    {
        return redisEnabled;
    }

    public void setRedisEnabled( boolean redisEnabled )
    {
        this.redisEnabled = redisEnabled;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getRedisHostname()
    {
        return redisHostname;
    }

    public void setRedisHostname( String redisHostname )
    {
        this.redisHostname = redisHostname;
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
    public String getInstanceBaseUrl()
    {
        return instanceBaseUrl;
    }

    public void setInstanceBaseUrl( String instanceBaseUrl )
    {
        this.instanceBaseUrl = instanceBaseUrl;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getSystemMonitoringUrl()
    {
        return systemMonitoringUrl;
    }

    public void setSystemMonitoringUrl( String systemMonitoringUrl )
    {
        this.systemMonitoringUrl = systemMonitoringUrl;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getClusterHostname()
    {
        return clusterHostname;
    }

    public void setClusterHostname( String clusterHostname )
    {
        this.clusterHostname = clusterHostname;
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

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isMetadataSyncEnabled()
    {
        return isMetadataSyncEnabled;
    }

    public void setMetadataSyncEnabled( boolean isMetadataSyncEnabled )
    {
        this.isMetadataSyncEnabled = isMetadataSyncEnabled;
    }
}
