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
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.eventvisualization.EventVisualizationService;
import org.hisp.dhis.minmax.MinMaxDataElement;
import org.hisp.dhis.minmax.MinMaxDataElementService;
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
   * Method retrieving {@link SMSCode}s by source {@link DataElement} references. All retrieved
   * {@link SMSCode}s will have their {@link DataElement} replaced with the target {@link
   * DataElement}.
   *
   * @param sources source {@link DataElement}s used to retrieve {@link SMSCode}s
   * @param target {@link DataElement} which will be set as the {@link DataElement} for an {@link
   *     SMSCode}
   */
  public void handleSmsCode(List<DataElement> sources, DataElement target) {
    List<SMSCode> smsCodes = smsCommandService.getSmsCodesByDataElement(sources);
    smsCodes.forEach(c -> c.setDataElement(target));
  }
}
