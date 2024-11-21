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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.junit.jupiter.api.Test;

class MappingErrorsTest {
  @Test
  void shouldReportErrors() {
    TrackerIdSchemeParams idSchemeParams =
        TrackerIdSchemeParams.builder()
            .idScheme(TrackerIdSchemeParam.NAME)
            .programIdScheme(TrackerIdSchemeParam.CODE)
            .programStageIdScheme(TrackerIdSchemeParam.ofAttribute("i4a244a8341"))
            .build();

    Program program1 = new Program();
    program1.setUid(CodeGenerator.generateUid());

    ProgramStage programStage1 = new ProgramStage();
    programStage1.setUid(CodeGenerator.generateUid());

    DataElement dataElement1 = new DataElement();
    dataElement1.setUid(CodeGenerator.generateUid());
    DataElement dataElement2 = new DataElement();
    dataElement2.setUid(CodeGenerator.generateUid());

    MappingErrors errors = new MappingErrors(idSchemeParams);

    assertFalse(errors.hasErrors());
    assertEquals("", errors.toString());

    errors.add(program1);
    assertTrue(errors.hasErrors());
    errors.add(program1); // ensure no duplicates are reported
    errors.add(programStage1);
    errors.add(dataElement1);
    errors.add(dataElement2);
    assertTrue(errors.hasErrors());

    assertAll(
        () ->
            assertContains(
                "Following metadata listed using their UIDs is missing identifiers for the"
                    + " requested idScheme:",
                errors.toString()),
        () -> assertContains("Program[CODE]=" + program1.getUid(), errors.toString()),
        () ->
            assertContains(
                "ProgramStage[ATTRIBUTE:i4a244a8341]=" + programStage1.getUid(), errors.toString()),
        // the order in which the uids are listed is not deterministic, only assert the uids are
        // present for simplicity.
        () -> assertContains("DataElement[NAME]=", errors.toString()),
        () -> assertContains(dataElement1.getUid(), errors.toString()),
        () -> assertContains(dataElement2.getUid(), errors.toString()));
  }
}
