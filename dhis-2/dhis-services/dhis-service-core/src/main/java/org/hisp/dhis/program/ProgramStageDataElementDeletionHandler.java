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
package org.hisp.dhis.program;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.springframework.stereotype.Component;

/**
 * @author Chau Thu Tran
 */
@Component("org.hisp.dhis.program.ProgramStageDataElementDeletionHandler")
public class ProgramStageDataElementDeletionHandler extends DeletionHandler {
  private final ProgramStageDataElementService programStageDataElementService;

  public ProgramStageDataElementDeletionHandler(
      ProgramStageDataElementService programStageDataElementService) {
    checkNotNull(programStageDataElementService);

    this.programStageDataElementService = programStageDataElementService;
  }

  @Override
  protected void register() {
    whenDeleting(ProgramStage.class, this::deleteProgramStage);
    whenDeleting(DataElement.class, this::deleteDataElement);
  }

  private void deleteProgramStage(ProgramStage programStage) {
    List<ProgramStageDataElement> programStageDataElements =
        new ArrayList<>(programStage.getProgramStageDataElements());

    for (ProgramStageDataElement programStageDataElement : programStageDataElements) {
      programStage.getProgramStageDataElements().remove(programStageDataElement);
      programStageDataElementService.deleteProgramStageDataElement(programStageDataElement);
    }
  }

  private void deleteDataElement(DataElement dataElement) {
    if (DataElementDomain.TRACKER == dataElement.getDomainType()) {
      for (ProgramStageDataElement element :
          programStageDataElementService.getAllProgramStageDataElements()) {
        if (element.getDataElement() != null && element.getDataElement().equals(dataElement)) {
          programStageDataElementService.deleteProgramStageDataElement(element);
        }
      }
    }
  }
}
