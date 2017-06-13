package org.hisp.dhis.dxf2.common;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.google.common.base.MoreObjects;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.system.notification.NotificationLevel;

/**
 * The idScheme is a general setting which will apply to all objects. The idSchemes
 * can also be defined for specific objects such as dataElementIdScheme. The
 * general setting will override specific settings.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class ImportOptions
{
    private static final ImportOptions DEFAULT_OPTIONS = new ImportOptions().setImportStrategy( ImportStrategy.NEW_AND_UPDATES );

    private IdSchemes idSchemes = new IdSchemes();

    private boolean dryRun;

    private Boolean preheatCache;

    private boolean async;

    private ImportStrategy importStrategy = ImportStrategy.CREATE_AND_UPDATE;

    private MergeMode mergeMode = MergeMode.REPLACE;

    private boolean skipExistingCheck;

    private boolean sharing;

    private boolean skipNotifications;

    private boolean datasetAllowsPeriods;

    private boolean strictPeriods;

    private boolean strictCategoryOptionCombos;

    private boolean strictAttributeOptionCombos;

    private boolean strictOrganisationUnits;

    private boolean requireCategoryOptionCombo;

    private boolean requireAttributeOptionCombo;

    private String filename;

    private NotificationLevel notificationLevel;
    
    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------

    public ImportOptions()
    {
    }

    //--------------------------------------------------------------------------
    // Logic
    //--------------------------------------------------------------------------

    public ImportOptions instance()
    {
        ImportOptions options = new ImportOptions();
        
        options.idSchemes = this.idSchemes;
        options.dryRun = this.dryRun;
        options.preheatCache = this.preheatCache;
        options.async = this.async;
        options.importStrategy = this.importStrategy;
        options.mergeMode = this.mergeMode;
        options.skipExistingCheck = this.skipExistingCheck;
        options.sharing = this.sharing;
        options.skipNotifications = this.skipNotifications;
        options.datasetAllowsPeriods = this.datasetAllowsPeriods;
        options.strictPeriods = this.strictPeriods;
        options.strictCategoryOptionCombos = this.strictCategoryOptionCombos;
        options.strictAttributeOptionCombos = this.strictAttributeOptionCombos;
        options.strictOrganisationUnits = this.strictOrganisationUnits;
        options.requireCategoryOptionCombo = this.requireCategoryOptionCombo;
        options.requireAttributeOptionCombo = this.requireAttributeOptionCombo;
        options.filename = this.filename;
        options.notificationLevel = this.notificationLevel;
        
        return options;
    }
    
    public static ImportOptions getDefaultImportOptions()
    {
        return DEFAULT_OPTIONS;
    }

    /**
     * Indicates whether to heat cache. Default is true.
     */
    public boolean isPreheatCache()
    {
        return preheatCache == null ? true : preheatCache;
    }

    /**
     * Indicates whether to heat cache. Default is false.
     */
    public boolean isPreheatCacheDefaultFalse()
    {
        return preheatCache == null ? false : preheatCache;
    }
    
    /**
     * Returns the notification level, or if not specified, returns the given
     * default notification level.
     * 
     * @param defaultLevel the default notification level.
     * @return the nofication level.
     */
    public NotificationLevel getNotificationLevel( NotificationLevel defaultLevel )
    {
        return notificationLevel != null ? notificationLevel : defaultLevel;
    }

    //--------------------------------------------------------------------------
    // Get methods
    //--------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public IdSchemes getIdSchemes()
    {
        return idSchemes;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isDryRun()
    {
        return dryRun;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getPreheatCache()
    {
        return preheatCache;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isAsync()
    {
        return async;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isDatasetAllowsPeriods()
    {
        return datasetAllowsPeriods;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ImportStrategy getImportStrategy()
    {
        return importStrategy != null ? importStrategy : ImportStrategy.NEW_AND_UPDATES;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public MergeMode getMergeMode()
    {
        return mergeMode;
    }

    public void setMergeMode( MergeMode mergeMode )
    {
        this.mergeMode = mergeMode;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isSkipExistingCheck()
    {
        return skipExistingCheck;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isSharing()
    {
        return sharing;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isSkipNotifications()
    {
        return skipNotifications;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isStrictPeriods()
    {
        return strictPeriods;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isStrictCategoryOptionCombos()
    {
        return strictCategoryOptionCombos;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isStrictAttributeOptionCombos()
    {
        return strictAttributeOptionCombos;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isStrictOrganisationUnits()
    {
        return strictOrganisationUnits;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isRequireCategoryOptionCombo()
    {
        return requireCategoryOptionCombo;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isRequireAttributeOptionCombo()
    {
        return requireAttributeOptionCombo;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getFilename()
    {
        return filename;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public NotificationLevel getNotificationLevel()
    {
        return notificationLevel;
    }

    //--------------------------------------------------------------------------
    // Set methods
    //--------------------------------------------------------------------------

    public ImportOptions setIdSchemes( IdSchemes idSchemes )
    {
        this.idSchemes = idSchemes;
        return this;
    }

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

    public ImportOptions setDatasetAllowsPeriods( boolean datasetAllowsPeriods )
    {
        this.datasetAllowsPeriods = datasetAllowsPeriods;
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

    public ImportOptions setEventIdScheme( String idScheme )
    {
        idSchemes.setProgramStageInstanceIdScheme( idScheme );
        return this;
    }

    public ImportOptions setDryRun( boolean dryRun )
    {
        this.dryRun = dryRun;
        return this;
    }

    public ImportOptions setPreheatCache( Boolean preheatCache )
    {
        this.preheatCache = preheatCache;
        return this;
    }

    public ImportOptions setAsync( boolean async )
    {
        this.async = async;
        return this;
    }

    public ImportOptions setStrategy( ImportStrategy strategy )
    {
        this.importStrategy = strategy != null ? strategy : null;
        return this;
    }

    public ImportOptions setImportStrategy( ImportStrategy strategy )
    {
        this.importStrategy = strategy != null ? strategy : null;
        return this;
    }

    public ImportOptions setSkipExistingCheck( boolean skipExistingCheck )
    {
        this.skipExistingCheck = skipExistingCheck;
        return this;
    }

    public ImportOptions setSharing( boolean sharing )
    {
        this.sharing = sharing;
        return this;
    }

    public ImportOptions setSkipNotifications( boolean skipNotifications )
    {
        this.skipNotifications = skipNotifications;
        return this;
    }

    public ImportOptions setStrictPeriods( boolean strictPeriods )
    {
        this.strictPeriods = strictPeriods;
        return this;
    }

    public ImportOptions setStrictCategoryOptionCombos( boolean strictCategoryOptionCombos )
    {
        this.strictCategoryOptionCombos = strictCategoryOptionCombos;
        return this;
    }

    public ImportOptions setStrictAttributeOptionCombos( boolean strictAttributeOptionCombos )
    {
        this.strictAttributeOptionCombos = strictAttributeOptionCombos;
        return this;
    }

    public ImportOptions setStrictOrganisationUnits( boolean strictOrganisationUnits )
    {
        this.strictOrganisationUnits = strictOrganisationUnits;
        return this;
    }

    public ImportOptions setRequireCategoryOptionCombo( boolean requireCategoryOptionCombo )
    {
        this.requireCategoryOptionCombo = requireCategoryOptionCombo;
        return this;
    }

    public ImportOptions setRequireAttributeOptionCombo( boolean requireAttributeOptionCombo )
    {
        this.requireAttributeOptionCombo = requireAttributeOptionCombo;
        return this;
    }

    public ImportOptions setFilename( String filename )
    {
        this.filename = filename;
        return this;
    }

    public ImportOptions setNotificationLevel( NotificationLevel notificationLevel )
    {
        this.notificationLevel = notificationLevel;
        return this;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "idSchemes", idSchemes )
            .add( "dryRun", dryRun )
            .add( "preheatCache", preheatCache )
            .add( "async", async )
            .add( "importStrategy", importStrategy )
            .add( "mergeMode", mergeMode )
            .add( "skipExistingCheck", skipExistingCheck )
            .add( "sharing", sharing )
            .add( "skipNotifications", skipNotifications )
            .add( "datasetAllowsPeriods", datasetAllowsPeriods )
            .add( "strictPeriods", strictPeriods )
            .add( "strictCategoryOptionCombos", strictCategoryOptionCombos )
            .add( "strictAttributeOptionCombos", strictAttributeOptionCombos )
            .add( "strictOrganisationUnits", strictOrganisationUnits )
            .add( "requireCategoryOptionCombo", requireCategoryOptionCombo )
            .add( "requireAttributeOptionCombo", requireAttributeOptionCombo )
            .toString();
    }
}
