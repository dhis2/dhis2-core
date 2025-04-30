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
package org.hisp.dhis.tracker.imports.programrule.executor.event;

import static org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue.error;
import static org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue.warning;
import static org.hisp.dhis.tracker.imports.programrule.executor.RuleActionExecutor.isEqual;

import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue;
import org.hisp.dhis.tracker.imports.programrule.executor.RuleActionExecutor;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;

/**
 * This executor assigns a value to a field if it is empty, otherwise returns an error @Author
 * Enrico Colasante
 */
@RequiredArgsConstructor
public class AssignDataValueExecutor implements RuleActionExecutor<Event> {
  private final SystemSettingsProvider settingsProvider;

  private final UID ruleUid;

  private final String value;

  private final UID dataElementUid;

  private final Set<DataValue> dataValues;

  @Override
  public UID getDataElementUid() {
    return dataElementUid;
  }

  @Override
  public Optional<ProgramRuleIssue> executeRuleAction(TrackerBundle bundle, Event event) {
    Boolean canOverwrite = settingsProvider.getCurrentSettings().getRuleEngineAssignOverwrite();

    DataElement dataElement = bundle.getPreheat().getDataElement(dataElementUid.getValue());

    DataValue payloadDataValue =
        dataValues.stream()
            .filter(dv -> dv.getDataElement().isEqualTo(dataElement))
            .findAny()
            .orElse(null);

    if (payloadDataValue == null
        || Boolean.TRUE.equals(canOverwrite)
        || isEqual(value, payloadDataValue.getValue(), dataElement.getValueType())) {
      addOrOverwriteDataValue(event, bundle, dataElement, payloadDataValue);
      return Optional.of(
          warning(
              ruleUid,
              ValidationCode.E1308,
              dataElementUid.getValue(),
              event.getEvent().getValue()));
    }
    return Optional.of(error(ruleUid, ValidationCode.E1307, dataElementUid.getValue(), value));
  }

  private void addOrOverwriteDataValue(
      Event event, TrackerBundle bundle, DataElement dataElement, DataValue payloadDataValue) {
    if (payloadDataValue != null) {
      payloadDataValue.setValue(value);
    } else {
      event
          .getDataValues()
          .add(
              createDataValue(
                  bundle.getPreheat().getIdSchemes().toMetadataIdentifier(dataElement), value));
    }
  }

  private DataValue createDataValue(MetadataIdentifier dataElement, String newValue) {
    return DataValue.builder().dataElement(dataElement).value(newValue).build();
  }
}
