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
package org.hisp.dhis.program;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.system.deletion.DeletionVeto;
import org.hisp.dhis.system.deletion.IdObjectDeletionHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SingleEventDeletionHandler extends IdObjectDeletionHandler<SingleEvent> {
  @Override
  protected void registerHandler() {
    whenVetoing(ProgramStage.class, this::allowDeleteProgramStage);
    whenVetoing(Program.class, this::allowDeleteProgram);
    whenVetoing(DataElement.class, this::allowDeleteDataElement);
  }

  private DeletionVeto allowDeleteProgramStage(ProgramStage programStage) {
    return vetoIfExists(
        VETO,
        "select 1 from singleevent where programstageid = :id limit 1",
        Map.of("id", programStage.getId()));
  }

  private DeletionVeto allowDeleteProgram(Program program) {
    return vetoIfExists(
        VETO,
        "select 1 from singleevent ev join programstage ps on ps.programstageid=ev.programstageid where ps.programid = :id limit 1",
        Map.of("id", program.getId()));
  }

  private DeletionVeto allowDeleteDataElement(DataElement dataElement) {
    return vetoIfExists(
        VETO,
        "select 1 from singleevent where eventdatavalues ?? :uid limit 1",
        Map.of("uid", dataElement.getUid()));
  }
}
