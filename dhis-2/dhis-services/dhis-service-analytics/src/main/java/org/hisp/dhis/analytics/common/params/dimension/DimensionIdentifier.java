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
package org.hisp.dhis.analytics.common.params.dimension;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier.DimensionIdentifierType.ENROLLMENT;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier.DimensionIdentifierType.EVENT;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier.DimensionIdentifierType.TEI;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.DIMENSION_SEPARATOR;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.With;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.common.params.IdentifiableKey;
import org.hisp.dhis.common.UidObject;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;

/**
 * Class that identifies a dimension. A dimension can be composed by up to three elements: Program,
 * Program Stage and a Dimension (the dimension uid).
 */
@Data
@AllArgsConstructor(staticName = "of")
public class DimensionIdentifier<D extends UidObject> implements IdentifiableKey {
  public static final DimensionIdentifier<DimensionParam> EMPTY =
      DimensionIdentifier.of(null, null, null);

  private final ElementWithOffset<Program> program;

  private final ElementWithOffset<ProgramStage> programStage;

  private final D dimension;

  @With private final String groupId;

  /**
   * Creates a dimension identifier for a TEI dimension with empty groupId.
   *
   * @param program the {@link ElementWithOffset<Program>}.
   * @param programStage the {@link ElementWithOffset<ProgramStage>}.
   * @param ofObject the dimension.
   * @return the dimension identifier.
   */
  public static <D extends UidObject> DimensionIdentifier<D> of(
      ElementWithOffset<Program> program,
      ElementWithOffset<ProgramStage> programStage,
      D ofObject) {
    return DimensionIdentifier.of(program, programStage, ofObject, null);
  }

  public String getPrefix() {
    if (isEnrollmentDimension()) {
      return getProgram().toString();
    }
    if (isEventDimension()) {
      return getProgram().toString() + DIMENSION_SEPARATOR + getProgramStage().toString();
    }
    return StringUtils.EMPTY;
  }

  public DimensionIdentifierType getDimensionIdentifierType() {
    if (isEventDimension()) {
      return EVENT;
    }
    if (isEnrollmentDimension()) {
      return ENROLLMENT;
    }
    return TEI;
  }

  public boolean isEmpty() {
    return isBlank(getKey());
  }

  public boolean isEnrollmentDimension() {
    return hasProgram() && !hasProgramStage();
  }

  public boolean isEventDimension() {
    return hasProgram() && hasProgramStage();
  }

  public boolean hasProgram() {
    return program != null && program.isPresent();
  }

  public boolean hasProgramStage() {
    return programStage != null && programStage.isPresent();
  }

  @Override
  public String toString() {
    return DimensionIdentifierHelper.asText(program, programStage, dimension);
  }

  public enum DimensionIdentifierType {
    TEI,
    ENROLLMENT,
    EVENT
  }

  @Override
  public String getKey() {
    List<String> keys = new ArrayList<>();

    if (program != null && program.isPresent()) {
      keys.add(program.getElement().getUid());
    }

    if (programStage != null && programStage.isPresent()) {
      keys.add(programStage.getElement().getUid());
    }

    if (dimension != null) {
      keys.add(dimension.getUid());
    }

    return keys.stream().collect(joining("."));
  }
}
