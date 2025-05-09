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
package org.hisp.dhis.programrule;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.DxfNamespaces;

/**
 * @author markusbekken
 */
@JacksonXmlRootElement(
    localName = "programRuleVariableSourceType",
    namespace = DxfNamespaces.DXF_2_0)
@RequiredArgsConstructor
public enum ProgramRuleVariableSourceType {
  DATAELEMENT_NEWEST_EVENT_PROGRAM_STAGE("dataelement_newest_event_program_stage"),
  DATAELEMENT_NEWEST_EVENT_PROGRAM("dataelement_newest_event_program"),
  DATAELEMENT_CURRENT_EVENT("dataelement_current_event"),
  DATAELEMENT_PREVIOUS_EVENT("dataelement_previous_event"),
  CALCULATED_VALUE("calculated_value"),
  TEI_ATTRIBUTE("tei_attribute");

  private final String value;

  private static final Set<ProgramRuleVariableSourceType> DATA_TYPES =
      new ImmutableSet.Builder<ProgramRuleVariableSourceType>()
          .add(
              DATAELEMENT_NEWEST_EVENT_PROGRAM_STAGE,
              DATAELEMENT_NEWEST_EVENT_PROGRAM,
              DATAELEMENT_CURRENT_EVENT,
              DATAELEMENT_PREVIOUS_EVENT)
          .build();

  private static final Set<ProgramRuleVariableSourceType> ATTRIBUTE_TYPES =
      new ImmutableSet.Builder<ProgramRuleVariableSourceType>().add(TEI_ATTRIBUTE).build();

  public static ProgramRuleVariableSourceType fromValue(String value) {
    for (ProgramRuleVariableSourceType type : ProgramRuleVariableSourceType.values()) {
      if (type.value.equalsIgnoreCase(value)) {
        return type;
      }
    }

    return null;
  }

  public static Set<ProgramRuleVariableSourceType> getDataTypes() {
    return DATA_TYPES;
  }

  public static Set<ProgramRuleVariableSourceType> getAttributeTypes() {
    return ATTRIBUTE_TYPES;
  }
}
