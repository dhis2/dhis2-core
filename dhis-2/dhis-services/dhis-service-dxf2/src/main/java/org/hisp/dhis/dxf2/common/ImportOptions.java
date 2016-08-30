package org.hisp.dhis.dxf2.common;

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

import com.google.common.base.MoreObjects;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.importexport.ImportStrategy;

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

    private boolean sendNotifications;

    private boolean datasetAllowsPeriods;

    private boolean strictPeriods;

    private boolean strictCategoryOptionCombos;

    private boolean strictAttributeOptionCombos;

    private boolean strictOrganisationUnits;

    private boolean requireCategoryOptionCombo;

    private boolean requireAttributeOptionCombo;

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------

    public ImportOptions()
    {
    }

    //--------------------------------------------------------------------------
    // Logic
    //--------------------------------------------------------------------------

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

    //--------------------------------------------------------------------------
    // Get methods
    //--------------------------------------------------------------------------

    public IdSchemes getIdSchemes()
    {
        return idSchemes;
    }

    public boolean isDryRun()
    {
        return dryRun;
    }

    public Boolean getPreheatCache()
    {
        return preheatCache;
    }

    public boolean isAsync()
    {
        return async;
    }

    public boolean isDatasetAllowsPeriods()
    {
        return datasetAllowsPeriods;
    }

    public ImportStrategy getImportStrategy()
    {
        return importStrategy != null ? importStrategy : ImportStrategy.NEW_AND_UPDATES;
    }

    public MergeMode getMergeMode()
    {
        return mergeMode;
    }

    public void setMergeMode( MergeMode mergeMode )
    {
        this.mergeMode = mergeMode;
    }

    public boolean isSkipExistingCheck()
    {
        return skipExistingCheck;
    }

    public boolean isSharing()
    {
        return sharing;
    }

    public boolean isSendNotifications()
    {
        return sendNotifications;
    }

    public boolean isStrictPeriods()
    {
        return strictPeriods;
    }

    public boolean isStrictCategoryOptionCombos()
    {
        return strictCategoryOptionCombos;
    }

    public boolean isStrictAttributeOptionCombos()
    {
        return strictAttributeOptionCombos;
    }

    public boolean isStrictOrganisationUnits()
    {
        return strictOrganisationUnits;
    }

    public boolean isRequireCategoryOptionCombo()
    {
        return requireCategoryOptionCombo;
    }

    public boolean isRequireAttributeOptionCombo()
    {
        return requireAttributeOptionCombo;
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

    public ImportOptions setSendNotifications( boolean sendNotifications )
    {
        this.sendNotifications = sendNotifications;
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

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this.getClass() ).
            add( "ID Schemes", idSchemes ).
            add( "Dry run", dryRun ).
            add( "Preheat cache", preheatCache ).
            add( "Async", async ).
            add( "Import strategy", importStrategy ).
            add( "Skip existing check", skipExistingCheck ).
            add( "Sharing", sharing ).
            add( "Strict periods", strictPeriods ).
            add( "Strict category option combos", strictCategoryOptionCombos ).
            add( "Strict attr option combos", strictAttributeOptionCombos ).
            add( "Strict org units", strictCategoryOptionCombos ).
            add( "Require category option combo", requireCategoryOptionCombo ).
            add( "Require attribute option combo", requireAttributeOptionCombo ).
            toString();
    }
}
