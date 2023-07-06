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
package org.hisp.dhis.dxf2.dataset;

import java.util.function.Supplier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@Getter
final class ImportConfig {
  private final IdScheme dsScheme;

  private final IdScheme ouScheme;

  private final IdScheme aocScheme;

  private final ImportStrategy strategy;

  private final boolean dryRun;

  private final boolean skipExistingCheck;

  private final boolean strictPeriods;

  private final boolean strictAttrOptionCombos;

  private final boolean strictOrgUnits;

  private final boolean requireAttrOptionCombos;

  private final boolean skipNotifications;

  private final CategoryOptionCombo fallbackCatOptCombo;

  ImportConfig(
      SystemSettingManager systemSettingManager,
      CategoryService categoryService,
      CompleteDataSetRegistrations registrations,
      ImportOptions options) {

    IdSchemes idSchemes = options.getIdSchemes();
    dsScheme =
        getIdScheme(
            registrations::getDataSetIdScheme,
            registrations::getIdScheme,
            idSchemes::getDataSetIdScheme,
            idSchemes::getIdScheme);
    ouScheme =
        getIdScheme(
            registrations::getOrgUnitIdScheme,
            registrations::getIdScheme,
            idSchemes::getOrgUnitIdScheme,
            idSchemes::getIdScheme);
    aocScheme =
        getIdScheme(
            registrations::getAttributeOptionComboIdScheme,
            registrations::getIdScheme,
            idSchemes::getAttributeOptionComboIdScheme,
            idSchemes::getIdScheme);

    log.info(
        String.format(
            "Data set scheme: %s, org unit scheme: %s, attribute option combo scheme: %s",
            dsScheme, ouScheme, aocScheme));

    strategy =
        registrations.getStrategy() != null
            ? ImportStrategy.valueOf(registrations.getStrategy())
            : options.getImportStrategy();

    dryRun = registrations.getDryRun() != null ? registrations.getDryRun() : options.isDryRun();

    skipNotifications = options.isSkipNotifications();

    skipExistingCheck = options.isSkipExistingCheck();

    strictPeriods =
        options.isStrictPeriods()
            || systemSettingManager.getBoolSetting(SettingKey.DATA_IMPORT_STRICT_PERIODS);

    strictAttrOptionCombos =
        options.isStrictAttributeOptionCombos()
            || systemSettingManager.getBoolSetting(
                SettingKey.DATA_IMPORT_STRICT_ATTRIBUTE_OPTION_COMBOS);

    strictOrgUnits =
        options.isStrictOrganisationUnits()
            || systemSettingManager.getBoolSetting(
                SettingKey.DATA_IMPORT_STRICT_ORGANISATION_UNITS);

    requireAttrOptionCombos =
        options.isRequireAttributeOptionCombo()
            || systemSettingManager.getBoolSetting(
                SettingKey.DATA_IMPORT_REQUIRE_ATTRIBUTE_OPTION_COMBO);

    fallbackCatOptCombo = categoryService.getDefaultCategoryOptionCombo();
  }

  /**
   * For the effective {@link IdScheme} the explicit value from {@link CompleteDataSetRegistrations}
   * (payload) takes precedence over explicit scheme from {@link ImportOptions} (URL params). If no
   * scheme is set again the default of {@link CompleteDataSetRegistrations#getIdScheme()} takes
   * precedence over the default from {@link ImportOptions#getIdSchemes()} {@link
   * IdSchemes#getIdScheme()}.
   */
  private static IdScheme getIdScheme(
      Supplier<String> primary,
      Supplier<String> primaryDefault,
      Supplier<IdScheme> secondary,
      Supplier<IdScheme> secondaryDefault) {
    String schemeName = primary.get();
    if (schemeName != null) {
      return getIdSchemeIdAsUid(IdScheme.from(schemeName));
    }
    IdScheme scheme = secondary.get();
    if (scheme != null && scheme != IdScheme.NULL) {
      return getIdSchemeIdAsUid(scheme);
    }
    schemeName = primaryDefault.get();
    if (schemeName != null) {
      return getIdSchemeIdAsUid(IdScheme.from(schemeName));
    }
    return getIdSchemeIdAsUid(IdScheme.from(secondaryDefault.get()));
  }

  private static IdScheme getIdSchemeIdAsUid(IdScheme scheme) {
    return scheme == IdScheme.ID ? IdScheme.UID : scheme;
  }
}
