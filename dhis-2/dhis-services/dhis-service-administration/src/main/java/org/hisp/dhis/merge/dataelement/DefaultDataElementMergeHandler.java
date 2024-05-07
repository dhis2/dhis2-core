/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.merge.dataelement;

import java.util.List;
import java.util.function.BiPredicate;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.eventvisualization.EventVisualizationService;
import org.hisp.dhis.minmax.MinMaxDataElement;
import org.hisp.dhis.minmax.MinMaxDataElementService;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.springframework.stereotype.Service;

/**
 * Merge handler for metadata entities.
 *
 * @author david mackessy
 */
@Service
@RequiredArgsConstructor
public class DefaultDataElementMergeHandler {

  private final DataElementService dataElementService;
  private final MinMaxDataElementService minMaxDataElementService;
  private final EventVisualizationService eventVisualizationService;
  private final SMSCommandService smsCommandService;

  public void handleMinMaxDataElement(List<DataElement> sources, DataElement target) {
    List<MinMaxDataElement> minMaxDataElements =
        minMaxDataElementService.getAllByDataElement(sources);
    minMaxDataElements.forEach(mmde -> mmde.setDataElement(target));
  }

  public void handleEventVisualization(List<DataElement> sources, DataElement target) {
    List<EventVisualization> eventVisualizations =
        eventVisualizationService.getAllByDataElement(sources);
    eventVisualizations.forEach(ev -> ev.setDataElementValueDimension(target));
  }

  /**
   * Handle merging of SMS Codes through its parent SMS Command.
   *
   * @param sources
   * @param target
   */
  public void handleSmsCode(List<DataElement> sources, DataElement target) {
    List<SMSCommand> smsCommands = smsCommandService.getSmsCommandsByCodeDataElement(sources);

    smsCommands.stream()
        .flatMap(smsCommand -> smsCommand.getCodes().stream())
        .filter(smsCode -> codeHasAnyDataElement.test(smsCode, sources))
        .forEach(smsCode -> smsCode.setDataElement(target));
  }

  public BiPredicate<SMSCode, List<DataElement>> codeHasAnyDataElement =
      (smsCode, dataElements) -> {
        DataElement dataElement = smsCode.getDataElement();
        if (dataElement != null) {
          return dataElements.stream()
              .map(BaseIdentifiableObject::getUid)
              .toList()
              .contains(dataElement.getUid());
        }
        return false;
      };
}
