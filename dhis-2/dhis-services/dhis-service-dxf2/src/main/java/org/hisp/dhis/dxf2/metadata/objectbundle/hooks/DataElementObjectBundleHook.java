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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
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

  private final DataElementService dataElementService;
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

  private void valueTypeChangeValidation(
      DataElement dataElement, ObjectBundle bundle, Consumer<ErrorReport> addReports) {
    // check value type being changed
    // if no data for DE then allow, otherwise reject
    log.info("checking data element value type validation");

    // preheat value
    DataElement dePreheat = bundle.getPreheat().get(PreheatIdentifier.UID, dataElement);
    ValueType existingValueType = dePreheat.getValueType();
    log.info("PREHEAT: data element value type: " + existingValueType);
    log.info("PREHEAT: data element UID: " + dePreheat.getUid());

    // new value
    ValueType proposedNewValueType = dataElement.getValueType();
    log.info("NEW: data element value type: " + proposedNewValueType);
    log.info("NEW: data element UID: " + dataElement.getUid());

    if (existingValueType != proposedNewValueType) {
      log.info(
          "DataElement {} valueType is different from existing valueType, checking if any data associated with DataElement",
          dataElement.getUid());
      if (dataValueService.dataValueExists(UID.of(dataElement))) {
        log.info(
            "DataElement {} has associated data values, changing of valueType is prohibited",
            dataElement.getUid());
        addReports.accept(new ErrorReport(DataElement.class, ErrorCode.E1121));
      } else {
        log.info(
            "DataElement {} has no associated data values, allow changing of valueType",
            dataElement.getUid());
      }
    }
  }
}
