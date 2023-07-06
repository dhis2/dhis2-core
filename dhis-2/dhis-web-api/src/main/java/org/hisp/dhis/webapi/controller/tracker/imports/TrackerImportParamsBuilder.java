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
package org.hisp.dhis.webapi.controller.tracker.imports;

import static org.hisp.dhis.tracker.AtomicMode.ALL;
import static org.hisp.dhis.tracker.FlushMode.AUTO;
import static org.hisp.dhis.tracker.TrackerIdScheme.UID;
import static org.hisp.dhis.tracker.TrackerImportStrategy.CREATE_AND_UPDATE;
import static org.hisp.dhis.tracker.ValidationMode.FULL;
import static org.hisp.dhis.tracker.bundle.TrackerBundleMode.COMMIT;
import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.ATOMIC_MODE_KEY;
import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.CATEGORY_OPTION_COMBO_ID_SCHEME_KEY;
import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.CATEGORY_OPTION_ID_SCHEME_KEY;
import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.DATA_ELEMENT_ID_SCHEME_KEY;
import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.FLUSH_MODE_KEY;
import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.ID_SCHEME_KEY;
import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.IMPORT_MODE_KEY;
import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.IMPORT_STRATEGY_KEY;
import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.ORG_UNIT_ID_SCHEME_KEY;
import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.PROGRAM_ID_SCHEME_KEY;
import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.PROGRAM_STAGE_ID_SCHEME_KEY;
import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.SKIP_RULE_ENGINE_KEY;
import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.SKIP_SIDE_EFFECTS;
import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.VALIDATION_MODE_KEY;

import com.google.common.base.Enums;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.BooleanUtils;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.tracker.AtomicMode;
import org.hisp.dhis.tracker.FlushMode;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundleMode;

/**
 * @author Luciano Fiandesio
 */
public class TrackerImportParamsBuilder {
  private TrackerImportParamsBuilder() {}

  public static TrackerImportParams build(Map<String, List<String>> parameters) {
    return builder(parameters).build();
  }

  public static TrackerImportParams.TrackerImportParamsBuilder builder(
      Map<String, List<String>> parameters) {
    return TrackerImportParams.builder()
        .validationMode(
            getEnumWithDefault(ValidationMode.class, parameters, VALIDATION_MODE_KEY, FULL))
        .importMode(
            getEnumWithDefault(TrackerBundleMode.class, parameters, IMPORT_MODE_KEY, COMMIT))
        .idSchemes(getTrackerIdentifiers(parameters))
        .importStrategy(
            getEnumWithDefault(
                TrackerImportStrategy.class, parameters, IMPORT_STRATEGY_KEY, CREATE_AND_UPDATE))
        .atomicMode(getEnumWithDefault(AtomicMode.class, parameters, ATOMIC_MODE_KEY, ALL))
        .flushMode(getEnumWithDefault(FlushMode.class, parameters, FLUSH_MODE_KEY, AUTO))
        .skipSideEffects(getBooleanValueOrDefault(parameters, SKIP_SIDE_EFFECTS))
        .skipRuleEngine(getBooleanValueOrDefault(parameters, SKIP_RULE_ENGINE_KEY));
  }

  private static <T extends Enum<T>> T getEnumWithDefault(
      Class<T> enumKlass,
      Map<String, List<String>> parameters,
      TrackerImportParamKey trackerImportParamKey,
      T defaultValue) {
    if (parameters == null
        || parameters.get(trackerImportParamKey.getKey()) == null
        || parameters.get(trackerImportParamKey.getKey()).isEmpty()) {
      return defaultValue;
    }

    if (TrackerIdScheme.class.equals(enumKlass)
        && IdScheme.isAttribute(parameters.get(trackerImportParamKey.getKey()).get(0))) {
      return Enums.getIfPresent(enumKlass, "ATTRIBUTE").orNull();
    }

    String value = String.valueOf(parameters.get(trackerImportParamKey.getKey()).get(0));

    return Enums.getIfPresent(enumKlass, value).or(defaultValue);
  }

  private static TrackerIdSchemeParams getTrackerIdentifiers(Map<String, List<String>> parameters) {
    TrackerIdScheme idScheme =
        getEnumWithDefault(TrackerIdScheme.class, parameters, ID_SCHEME_KEY, UID);

    return TrackerIdSchemeParams.builder()
        .idScheme(bySchemeAndKey(parameters, ID_SCHEME_KEY, idScheme))
        .orgUnitIdScheme(bySchemeAndKey(parameters, ORG_UNIT_ID_SCHEME_KEY, idScheme))
        .programIdScheme(bySchemeAndKey(parameters, PROGRAM_ID_SCHEME_KEY, idScheme))
        .programStageIdScheme(bySchemeAndKey(parameters, PROGRAM_STAGE_ID_SCHEME_KEY, idScheme))
        .dataElementIdScheme(bySchemeAndKey(parameters, DATA_ELEMENT_ID_SCHEME_KEY, idScheme))
        .categoryOptionComboIdScheme(
            bySchemeAndKey(parameters, CATEGORY_OPTION_COMBO_ID_SCHEME_KEY, idScheme))
        .categoryOptionIdScheme(bySchemeAndKey(parameters, CATEGORY_OPTION_ID_SCHEME_KEY, idScheme))
        .build();
  }

  private static Boolean getBooleanValueOrDefault(
      Map<String, List<String>> parameters, TrackerImportParamKey trackerImportParamKey) {
    if (parameters == null
        || parameters.get(trackerImportParamKey.getKey()) == null
        || parameters.get(trackerImportParamKey.getKey()).isEmpty()) {
      return false;
    }

    return BooleanUtils.toBooleanObject(parameters.get(trackerImportParamKey.getKey()).get(0));
  }

  private static String getAttributeUidOrNull(
      Map<String, List<String>> parameters, TrackerImportParamKey trackerImportParamKey) {
    if (parameters == null
        || parameters.get(trackerImportParamKey.getKey()) == null
        || parameters.get(trackerImportParamKey.getKey()).isEmpty()) {
      return null;
    }

    if (IdScheme.isAttribute(parameters.get(trackerImportParamKey.getKey()).get(0))) {
      String uid = "";

      // Get second half of string, separated by ':'
      String[] splitParam = parameters.get(trackerImportParamKey.getKey()).get(0).split(":");

      if (splitParam.length > 1) {
        uid = splitParam[1];
      }

      if (CodeGenerator.isValidUid(uid)) {
        return uid;
      }
    }

    return null;
  }

  private static TrackerIdSchemeParam bySchemeAndKey(
      Map<String, List<String>> parameters,
      TrackerImportParamKey trackerImportParameterKey,
      TrackerIdScheme defaultIdScheme) {

    TrackerIdScheme trackerIdScheme =
        getEnumWithDefault(
            TrackerIdScheme.class, parameters, trackerImportParameterKey, defaultIdScheme);

    return TrackerIdSchemeParam.of(
        trackerIdScheme, getAttributeUidOrNull(parameters, trackerImportParameterKey));
  }
}
