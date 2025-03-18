/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.textpattern.TextPatternParser;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataElementObjectBundleHook extends AbstractObjectBundleHook<DataElement> {

  private final DataValueService dataValueService;

  @Override
  public void validate(
      DataElement dataElement, ObjectBundle bundle, Consumer<ErrorReport> addReports) {
    fieldMaskValidation(dataElement, addReports);
    valueTypeChangeValidation(dataElement, bundle, addReports);
  }

  private void fieldMaskValidation(DataElement dataElement, Consumer<ErrorReport> addReports) {
    if (dataElement.getFieldMask() != null) {
      try {
        TextPatternParser.parse("\"" + dataElement.getFieldMask() + "\"");
      } catch (TextPatternParser.TextPatternParsingException ex) {
        addReports.accept(
            new ErrorReport(
                DataElement.class,
                ErrorCode.E4019,
                dataElement.getFieldMask(),
                "Not a valid TextPattern 'TEXT' segment"));
      }
    }
  }

  /**
   * Performs validation to check if the {@link ValueType} of a {@link DataElement} is being
   * changed. <br>
   * If the {@link ValueType} is not being changed, then no action taken <br>
   * If the {@link ValueType} is being changed, a check for {@link DataValue}s for the {@link
   * DataElement} is performed:
   *
   * <ul>
   *   <li>If there are {@link DataValue}s for the {@link DataElement} then we prohibit the change
   *   <li>If there are no {@link DataValue}s for the {@link DataElement} then the change of {@link
   *       ValueType} is allowed
   * </ul>
   *
   * @param dataElement {@link DataElement} to check
   * @param bundle {@link ObjectBundle} to get existing {@link DataElement}
   * @param addReports reports to add error if validation fails
   */
  private void valueTypeChangeValidation(
      DataElement dataElement, ObjectBundle bundle, Consumer<ErrorReport> addReports) {
    log.debug("checking data element value type validation");

    // existing value
    DataElement dePreheat = bundle.getPreheat().get(PreheatIdentifier.UID, dataElement);
    ValueType existingValueType = dePreheat.getValueType();

    // new value
    ValueType proposedNewValueType = dataElement.getValueType();

    if (existingValueType != proposedNewValueType) {
      log.debug(
          "DataElement {} valueType {} is different from existing valueType {}, checking if any data associated with DataElement",
          dataElement.getUid(),
          proposedNewValueType,
          existingValueType);
      if (dataValueService.dataValueExistsForDataElement(UID.of(dataElement))) {
        log.warn(
            "DataElement {} has associated data values, changing of valueType is prohibited",
            dataElement.getUid());
        addReports.accept(
            new ErrorReport(DataElement.class, ErrorCode.E1121, dataElement.getUid()));
      } else {
        log.debug(
            "DataElement {} has no associated data values, changing of valueType is allowed",
            dataElement.getUid());
      }
    }
  }
}
