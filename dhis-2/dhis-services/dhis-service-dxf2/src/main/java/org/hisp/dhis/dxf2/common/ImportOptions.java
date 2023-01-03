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
package org.hisp.dhis.dxf2.common;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReportMode;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.user.User;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The idScheme is a general setting which will apply to all objects. The
 * idSchemes can also be defined for specific objects such as
 * dataElementIdScheme. The general setting will override specific settings.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@OpenApi.Shared
@Getter
@Setter
@Accessors( chain = true )
@Builder( access = AccessLevel.PRIVATE, toBuilder = true )
@ToString( exclude = "user" )
@NoArgsConstructor
@AllArgsConstructor( access = AccessLevel.PRIVATE )
public class ImportOptions
{
    private User user;

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private IdSchemes idSchemes = new IdSchemes();

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private boolean dryRun;

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private Boolean preheatCache;

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private boolean async;

    private ImportStrategy importStrategy = ImportStrategy.CREATE_AND_UPDATE;

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private MergeMode mergeMode = MergeMode.REPLACE;

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private ImportReportMode reportMode = ImportReportMode.FULL;

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private boolean skipExistingCheck;

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private boolean sharing;

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private boolean skipNotifications;

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private boolean skipAudit;

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private boolean datasetAllowsPeriods;

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private boolean strictPeriods;

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private boolean strictDataElements;

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private boolean strictCategoryOptionCombos;

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private boolean strictAttributeOptionCombos;

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private boolean strictOrganisationUnits;

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private boolean strictDataSetApproval;

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private boolean strictDataSetLocking;

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private boolean strictDataSetInputPeriods;

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private boolean requireCategoryOptionCombo;

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private boolean requireAttributeOptionCombo;

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private boolean skipPatternValidation;

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private boolean ignoreEmptyCollection;

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private boolean force;

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private boolean firstRowIsHeader = true;

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private String filename;

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private NotificationLevel notificationLevel;

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private boolean skipLastUpdated;

    /**
     * This flag signals the system that the request contains Event Data Values
     * that have to be merged with the existing Data Values (as opposed to a
     * full replacement)
     */
    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private boolean mergeDataValues;

    /**
     * if true, caches for import are not used. Should only be used for testing
     */
    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private boolean skipCache = false;

    /**
     * Optional field to set the data set ID of the imported values using
     * request parameters
     */
    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    private String dataSet;

    // --------------------------------------------------------------------------
    // Logic
    // --------------------------------------------------------------------------

    public ImportOptions instance()
    {
        return toBuilder().build();
    }

    public static ImportOptions getDefaultImportOptions()
    {
        return new ImportOptions().setImportStrategy( ImportStrategy.NEW_AND_UPDATES );
    }

    /**
     * Indicates whether to heat cache. Default is true.
     */
    public boolean isPreheatCache()
    {
        return preheatCache == null || preheatCache;
    }

    /**
     * Indicates whether to heat cache. Default is false.
     */
    public boolean isPreheatCacheDefaultFalse()
    {
        return preheatCache != null && preheatCache;
    }

    /**
     * Returns the notification level, or if not specified, returns the given
     * default notification level.
     *
     * @param defaultLevel the default notification level.
     * @return the notification level.
     */
    public NotificationLevel getNotificationLevel( NotificationLevel defaultLevel )
    {
        return notificationLevel != null ? notificationLevel : defaultLevel;
    }

    // --------------------------------------------------------------------------
    // Get methods
    // --------------------------------------------------------------------------

    @JsonProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ImportStrategy getImportStrategy()
    {
        return importStrategy != null ? importStrategy : ImportStrategy.NEW_AND_UPDATES;
    }

    // --------------------------------------------------------------------------
    // Set methods
    // --------------------------------------------------------------------------

    public ImportOptions setIdScheme( String idScheme )
    {
        idSchemes.setIdScheme( idScheme );
        return this;
    }

    public ImportOptions setDataElementIdScheme( String idScheme )
    {
        idSchemes.setDataElementIdScheme( idScheme );
        return this;
    }

    public ImportOptions setCategoryOptionComboIdScheme( String idScheme )
    {
        idSchemes.setCategoryOptionComboIdScheme( idScheme );
        return this;
    }

    public ImportOptions setCategoryOptionIdScheme( String idScheme )
    {
        idSchemes.setCategoryOptionIdScheme( idScheme );
        return this;
    }

    public ImportOptions setCategoryIdScheme( String idScheme )
    {
        idSchemes.setCategoryIdScheme( idScheme );
        return this;
    }

    public ImportOptions setOrgUnitIdScheme( String idScheme )
    {
        idSchemes.setOrgUnitIdScheme( idScheme );
        return this;
    }

    public ImportOptions setProgramIdScheme( String idScheme )
    {
        idSchemes.setProgramIdScheme( idScheme );
        return this;
    }

    public ImportOptions setProgramStageIdScheme( String idScheme )
    {
        idSchemes.setProgramStageIdScheme( idScheme );
        return this;
    }

    public ImportOptions setTrackedEntityIdScheme( String idScheme )
    {
        idSchemes.setTrackedEntityIdScheme( idScheme );
        return this;
    }

    public ImportOptions setTrackedEntityAttributeIdScheme( String idScheme )
    {
        idSchemes.setTrackedEntityAttributeIdScheme( idScheme );
        return this;
    }

    public ImportOptions setDataSetIdScheme( String idScheme )
    {
        idSchemes.setDataSetIdScheme( idScheme );
        return this;
    }

    public ImportOptions setEventIdScheme( String idScheme )
    {
        idSchemes.setProgramStageInstanceIdScheme( idScheme );
        return this;
    }

    public ImportOptions setStrategy( ImportStrategy strategy )
    {
        return setImportStrategy( strategy );
    }
}
