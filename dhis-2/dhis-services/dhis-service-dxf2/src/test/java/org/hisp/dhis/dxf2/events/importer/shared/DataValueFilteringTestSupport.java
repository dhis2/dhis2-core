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
package org.hisp.dhis.dxf2.events.importer.shared;

import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;

public class DataValueFilteringTestSupport {

  public static final String PROGRAMSTAGE = "programstage";

  public static final String DATA_ELEMENT_1 = "de1";

  public static final String DATA_ELEMENT_2 = "de2";

  public static final String PROGRAM = "program";

  private static final String DATA_ELEMENT_3 = "de3";

  public static Map<String, Program> getProgramMap() {
    return Map.of(PROGRAM, getProgram());
  }

  public static Program getProgram() {
    Program program = new Program();
    program.setProgramStages(getProgramStages());
    return program;
  }

  public static Set<ProgramStage> getProgramStages() {
    ProgramStage programStage = new ProgramStage();
    programStage.setProgramStageDataElements(
        getProgramStageDataElements(DATA_ELEMENT_1, DATA_ELEMENT_3));
    programStage.setUid(PROGRAMSTAGE);
    return Sets.newHashSet(programStage);
  }

  public static Set<ProgramStageDataElement> getProgramStageDataElements(String... uids) {
    return Arrays.stream(uids)
        .map(DataValueFilteringTestSupport::getProgramStageDataElement)
        .collect(Collectors.toSet());
  }

  public static ProgramStageDataElement getProgramStageDataElement(String uid) {
    ProgramStageDataElement programStageDataElement = new ProgramStageDataElement();
    DataElement dataElement = new DataElement();
    dataElement.setUid(uid);
    programStageDataElement.setDataElement(dataElement);
    return programStageDataElement;
  }
}
