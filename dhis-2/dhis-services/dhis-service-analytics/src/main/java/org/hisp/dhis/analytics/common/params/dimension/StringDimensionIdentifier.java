/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.common.params.dimension;

import static lombok.AccessLevel.PRIVATE;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Triple;

/**
 * This object identifies the structure of dimension item and its associations (program and a
 * program stage).
 */
@RequiredArgsConstructor(access = PRIVATE)
public class StringDimensionIdentifier {
  private final Triple<ElementWithOffset<StringUid>, ElementWithOffset<StringUid>, StringUid>
      triple;

  public static StringDimensionIdentifier of(
      ElementWithOffset<StringUid> program,
      ElementWithOffset<StringUid> programStage,
      StringUid dimension) {
    return new StringDimensionIdentifier(Triple.of(program, programStage, dimension));
  }

  public ElementWithOffset<StringUid> getProgram() {
    return triple.getLeft();
  }

  public ElementWithOffset<StringUid> getProgramStage() {
    return triple.getMiddle();
  }

  public StringUid getDimension() {
    return triple.getRight();
  }

  /**
   * Returns this object as a String in its full representation. The returned value will have the
   * format: programUid.programStageUid.dimensionUid.
   *
   * @return the string representing the full dimension.
   */
  public String toString() {
    return DimensionIdentifierHelper.asText(getProgram(), getProgramStage(), getDimension());
  }
}
