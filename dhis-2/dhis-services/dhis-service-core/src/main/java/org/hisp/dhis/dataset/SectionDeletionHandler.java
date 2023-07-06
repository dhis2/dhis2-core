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
package org.hisp.dhis.dataset;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.springframework.stereotype.Component;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
@Component("org.hisp.dhis.dataset.SectionDeletionHandler")
public class SectionDeletionHandler extends DeletionHandler {
  private final SectionService sectionService;

  public SectionDeletionHandler(SectionService sectionService) {
    checkNotNull(sectionService);
    this.sectionService = sectionService;
  }

  @Override
  protected void register() {
    whenDeleting(DataElement.class, this::deleteDataElement);
    whenDeleting(DataSet.class, this::deleteDataSet);
  }

  private void deleteDataElement(DataElement dataElement) {
    for (Section section : sectionService.getAllSections()) {
      List<DataElementOperand> operandsToRemove =
          section.getGreyedFields().stream()
              .filter(operand -> operand.getDataElement().equals(dataElement))
              .collect(Collectors.toList());

      operandsToRemove.stream().forEach(operand -> section.getGreyedFields().remove(operand));

      if (section.getDataElements().remove(dataElement) || !operandsToRemove.isEmpty()) {
        sectionService.updateSection(section);
      }
    }
  }

  private void deleteDataSet(DataSet dataSet) {
    Iterator<Section> iterator = dataSet.getSections().iterator();

    while (iterator.hasNext()) {
      Section section = iterator.next();
      iterator.remove();
      sectionService.deleteSection(section);
    }
  }
}
