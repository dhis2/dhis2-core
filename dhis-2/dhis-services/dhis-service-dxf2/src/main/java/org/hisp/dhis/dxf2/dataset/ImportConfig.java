package org.hisp.dhis.dxf2.dataset;
/*
 * Copyright (c) 2004-2021, University of Oslo
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

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Lars Helge Overland
 */
@Slf4j
class ImportConfig
{
    private IdScheme dsScheme;

    private IdScheme ouScheme;

    private IdScheme aocScheme;

    private ImportStrategy strategy;

    private boolean dryRun;

    private boolean skipExistingCheck;

    private boolean strictPeriods;

    private boolean strictAttrOptionCombos;

    private boolean strictOrgUnits;

    private boolean requireAttrOptionCombos;

    private boolean skipNotifications;

    private CategoryOptionCombo fallbackCatOptCombo;

    ImportConfig(SystemSettingManager systemSettingManager, CategoryService categoryService,
                 CompleteDataSetRegistrations cdsr, ImportOptions options)
    {
        dsScheme = IdScheme.from( cdsr.getDataSetIdSchemeProperty() );
        ouScheme = IdScheme.from( cdsr.getOrgUnitIdSchemeProperty() );
        aocScheme = IdScheme.from( cdsr.getAttributeOptionComboIdSchemeProperty() );

        log.info( String.format( "Data set scheme: %s, org unit scheme: %s, attribute option combo scheme: %s",
            dsScheme, ouScheme, aocScheme ) );

        strategy = cdsr.getStrategy() != null ? ImportStrategy.valueOf( cdsr.getStrategy() )
            : options.getImportStrategy();

        dryRun = cdsr.getDryRun() != null ? cdsr.getDryRun() : options.isDryRun();

        skipNotifications = options.isSkipNotifications();

        skipExistingCheck = options.isSkipExistingCheck();

        strictPeriods = options.isStrictPeriods()
            || (Boolean) systemSettingManager.getSystemSetting( SettingKey.DATA_IMPORT_STRICT_PERIODS );

        strictAttrOptionCombos = options.isStrictAttributeOptionCombos()
            || (Boolean) systemSettingManager.getSystemSetting( SettingKey.DATA_IMPORT_STRICT_ATTRIBUTE_OPTION_COMBOS );

        strictOrgUnits = options.isStrictOrganisationUnits()
            || (Boolean) systemSettingManager.getSystemSetting( SettingKey.DATA_IMPORT_STRICT_ORGANISATION_UNITS );

        requireAttrOptionCombos = options.isRequireAttributeOptionCombo()
            || (Boolean) systemSettingManager.getSystemSetting( SettingKey.DATA_IMPORT_REQUIRE_ATTRIBUTE_OPTION_COMBO );

        fallbackCatOptCombo = categoryService.getDefaultCategoryOptionCombo();
    }

    IdScheme getDsScheme()
    {
        return dsScheme;
    }

    IdScheme getOuScheme()
    {
        return ouScheme;
    }

    IdScheme getAocScheme()
    {
        return aocScheme;
    }

    ImportStrategy getStrategy()

    {
        return strategy;
    }

    boolean isDryRun()
    {
        return dryRun;
    }

    boolean isSkipExistingCheck()
    {
        return skipExistingCheck;
    }

    boolean isStrictPeriods()
    {
        return strictPeriods;
    }

    boolean isStrictAttrOptionCombos()
    {
        return strictAttrOptionCombos;
    }

    boolean isStrictOrgUnits()
    {
        return strictOrgUnits;
    }

    boolean isRequireAttrOptionCombos()
    {
        return requireAttrOptionCombos;
    }

    boolean isSkipNotifications()
    {
        return skipNotifications;
    }

    CategoryOptionCombo getFallbackCatOptCombo()
    {
        return fallbackCatOptCombo;
    }
}
