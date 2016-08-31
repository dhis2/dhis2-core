package org.hisp.dhis.dxf2.common;

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

import static org.hisp.dhis.common.IdentifiableProperty.UID;

import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.MergeStrategy;
import org.hisp.dhis.importexport.ImportStrategy;

import com.google.common.base.MoreObjects;

/**
 * The idScheme is a general setting which will apply to all objects. The idSchemes
 * can also be defined for specific objects such as dataElementIdScheme. The
 * general setting will override specific settings.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class ImportOptions
{
    private static final ImportOptions DEFAULT_OPTIONS = new ImportOptions().
        setDataElementIdScheme( UID ).setOrgUnitIdScheme( UID ).setImportStrategy( ImportStrategy.NEW_AND_UPDATES );

    private IdentifiableProperty idScheme;

    private IdentifiableProperty dataElementIdScheme;

    private IdentifiableProperty orgUnitIdScheme;

    private boolean dryRun;

    private boolean preheatCache = true;

    private boolean async;

    private ImportStrategy importStrategy = ImportStrategy.CREATE_AND_UPDATE;

    private MergeStrategy mergeStrategy = MergeStrategy.MERGE_IF_NOT_NULL;

    private boolean skipExistingCheck;

    private boolean sharing;
    
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
    
    public ImportOptions( IdentifiableProperty idScheme, IdentifiableProperty dataElementIdScheme, IdentifiableProperty orgUnitIdscheme )
    {
        this.idScheme = idScheme;
        this.dataElementIdScheme = dataElementIdScheme;
        this.orgUnitIdScheme = orgUnitIdscheme;
    }

    //--------------------------------------------------------------------------
    // Logic
    //--------------------------------------------------------------------------

    public static ImportOptions getDefaultImportOptions()
    {
        return DEFAULT_OPTIONS;
    }
    
    //--------------------------------------------------------------------------
    // Get methods
    //--------------------------------------------------------------------------

    public IdentifiableProperty getIdScheme()
    {
        return idScheme != null ? idScheme : IdentifiableProperty.UID;
    }

    public IdentifiableProperty getDataElementIdScheme()
    {
        return dataElementIdScheme != null ? dataElementIdScheme : ( idScheme != null ? idScheme : IdentifiableProperty.UID );
    }

    public IdentifiableProperty getOrgUnitIdScheme()
    {
        return orgUnitIdScheme != null ? orgUnitIdScheme : ( idScheme != null ? idScheme : IdentifiableProperty.UID );
    }

    public boolean isDryRun()
    {
        return dryRun;
    }

    public boolean isPreheatCache()
    {
        return preheatCache;
    }

    public boolean isAsync()
    {
        return async;
    }

    public ImportStrategy getImportStrategy()
    {
        return importStrategy != null ? importStrategy : ImportStrategy.NEW_AND_UPDATES;
    }

    public MergeStrategy getMergeStrategy()
    {
        return mergeStrategy;
    }

    public void setMergeStrategy( MergeStrategy mergeStrategy )
    {
        this.mergeStrategy = mergeStrategy;
    }

    public boolean isSkipExistingCheck()
    {
        return skipExistingCheck;
    }

    public boolean isSharing()
    {
        return sharing;
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

    public ImportOptions setIdScheme( IdentifiableProperty scheme )
    {
        this.idScheme = scheme != null ? scheme : null;
        return this;
    }

    public ImportOptions setDataElementIdScheme( IdentifiableProperty scheme )
    {
        this.dataElementIdScheme = scheme != null ? scheme : null;
        return this;
    }

    public ImportOptions setOrgUnitIdScheme( IdentifiableProperty scheme )
    {
        this.orgUnitIdScheme = scheme != null ? scheme : null;
        return this;
    }

    public ImportOptions setDryRun( boolean dryRun )
    {
        this.dryRun = dryRun;
        return this;
    }

    public ImportOptions setPreheatCache( boolean preheatCache )
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
            add( "Id scheme", idScheme ).
            add( "Data element id scheme", dataElementIdScheme ).
            add( "Org unit id scheme", orgUnitIdScheme ).
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
