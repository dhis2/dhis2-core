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
package org.hisp.dhis.common;

import static lombok.AccessLevel.PRIVATE;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;

/**
 * Helper class to convert from various {@link org.hisp.dhis.common.BaseDimensionalObject} into a
 * {@link org.hisp.dhis.common.PrefixedDimension} object.
 */
@NoArgsConstructor(access = PRIVATE)
public class PrefixedDimensions {
  public static Collection<PrefixedDimension> ofProgramIndicators(
      Set<ProgramIndicator> programIndicators) {
    return programIndicators.stream()
        .map(
            programIndicator ->
                PrefixedDimension.builder()
                    .item(programIndicator)
                    .program(programIndicator.getProgram())
                    .build())
        .collect(Collectors.toList());
  }

  public static Collection<PrefixedDimension> ofDataElements(ProgramStage programStage) {
    return programStage.getDataElements().stream()
        .map(
            dataElement ->
                PrefixedDimension.builder()
                    .item(dataElement)
                    .programStage(programStage)
                    .program(programStage.getProgram())
                    .build())
        .collect(Collectors.toList());
  }

  public static Collection<PrefixedDimension> ofItemsWithProgram(
      Program program, Collection<? extends BaseIdentifiableObject> objects) {
    return objects.stream()
        .map(item -> PrefixedDimension.builder().item(item).program(program).build())
        .collect(Collectors.toList());
  }

  public static Collection<PrefixedDimension> ofProgramStageDataElements(
      Collection<ProgramStageDataElement> programStageDataElements) {
    return programStageDataElements.stream()
        .map(
            programStageDataElement ->
                PrefixedDimension.builder()
                    .programStage(programStageDataElement.getProgramStage())
                    .program(programStageDataElement.getProgramStage().getProgram())
                    .item(programStageDataElement)
                    .build())
        .collect(Collectors.toList());
  }
}
