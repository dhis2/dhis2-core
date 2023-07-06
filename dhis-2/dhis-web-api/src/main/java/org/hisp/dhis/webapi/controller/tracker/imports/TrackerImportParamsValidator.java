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

import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.ATOMIC_MODE_KEY;
import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.FLUSH_MODE_KEY;
import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.ID_SCHEME_KEY;
import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.IMPORT_MODE_KEY;
import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.IMPORT_STRATEGY_KEY;
import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.REPORT_MODE;
import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.VALIDATION_MODE_KEY;

import com.google.common.base.Enums;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.tracker.AtomicMode;
import org.hisp.dhis.tracker.FlushMode;
import org.hisp.dhis.tracker.TrackerBundleReportMode;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundleMode;
import org.hisp.dhis.webapi.controller.exception.InvalidEnumValueException;

public class TrackerImportParamsValidator {
  private TrackerImportParamsValidator() {}

  public static void validateRequest(TrackerImportRequest request)
      throws InvalidEnumValueException {
    Map<String, List<String>> parameters = request.getContextService().getParameterValuesMap();

    validateEnum(TrackerBundleMode.class, parameters, IMPORT_MODE_KEY);
    validateEnum(TrackerImportStrategy.class, parameters, IMPORT_STRATEGY_KEY);
    validateEnum(AtomicMode.class, parameters, ATOMIC_MODE_KEY);
    validateEnum(FlushMode.class, parameters, FLUSH_MODE_KEY);
    validateEnum(ValidationMode.class, parameters, VALIDATION_MODE_KEY);
    validateEnum(TrackerIdScheme.class, parameters, ID_SCHEME_KEY, IdScheme::isAttribute);
    validateEnum(TrackerBundleReportMode.class, parameters, REPORT_MODE);
  }

  private static <T extends Enum<T>> void validateEnum(
      Class<T> enumKlass,
      Map<String, List<String>> parameters,
      TrackerImportParamKey trackerImportParamKey)
      throws InvalidEnumValueException {
    if (parameters == null
        || parameters.get(trackerImportParamKey.getKey()) == null
        || parameters.get(trackerImportParamKey.getKey()).isEmpty()) {
      return;
    }

    validateEnumValue(
        enumKlass, trackerImportParamKey, parameters.get(trackerImportParamKey.getKey()).get(0));
  }

  private static <T extends Enum<T>> void validateEnum(
      Class<T> enumKlass,
      Map<String, List<String>> parameters,
      TrackerImportParamKey trackerImportParamKey,
      Predicate<String> predicate)
      throws InvalidEnumValueException {
    if (parameters == null
        || parameters.get(trackerImportParamKey.getKey()) == null
        || parameters.get(trackerImportParamKey.getKey()).isEmpty()) {
      return;
    }

    String value = parameters.get(trackerImportParamKey.getKey()).get(0);

    if (predicate.test(value)) {
      return;
    }

    validateEnumValue(enumKlass, trackerImportParamKey, value);
  }

  private static <T extends Enum<T>> void validateEnumValue(
      Class<T> enumKlass, TrackerImportParamKey trackerImportParamKey, String value)
      throws InvalidEnumValueException {
    Optional<T> optionalEnumValue = Enums.getIfPresent(enumKlass, value).toJavaUtil();
    if (optionalEnumValue.isPresent()) {
      return;
    }

    throw new InvalidEnumValueException(value, trackerImportParamKey.getKey(), enumKlass);
  }
}
