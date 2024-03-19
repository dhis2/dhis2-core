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
package org.hisp.dhis.programstageworkinglist;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.translation.Translatable;

@Setter
@EqualsAndHashCode(callSuper = true)
@JacksonXmlRootElement(localName = "programStageWorkingList", namespace = DxfNamespaces.DXF_2_0)
public class ProgramStageWorkingList extends BaseIdentifiableObject implements MetadataObject {

  /** Property indicating the program of the working list */
  private Program program;

  /** Property indicating the program stage of the working list */
  private ProgramStage programStage;

  /** Property indicating the description of the working list */
  private String description;

  /**
   * Criteria object representing selected projections, filtering and sorting criteria in program
   * stages
   */
  private ProgramStageQueryCriteria programStageQueryCriteria = new ProgramStageQueryCriteria();

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  public Program getProgram() {
    return program;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  public ProgramStage getProgramStage() {
    return programStage;
  }

  @JsonProperty
  public String getDescription() {
    return description;
  }

  @JsonProperty
  @Translatable(propertyName = "description", key = "DESCRIPTION")
  public String getDisplayDescription() {
    return getTranslation("DESCRIPTION", getDescription());
  }

  @JsonProperty
  public ProgramStageQueryCriteria getProgramStageQueryCriteria() {
    return programStageQueryCriteria;
  }
}
