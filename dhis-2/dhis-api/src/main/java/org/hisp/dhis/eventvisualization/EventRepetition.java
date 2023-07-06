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
package org.hisp.dhis.eventvisualization;

import static org.hisp.dhis.common.DxfNamespaces.DXF_2_0;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hisp.dhis.common.EmbeddedObject;

/**
 * This object represents an event repetition. It encapsulates all attributes needed by the
 * analytics engine during the query of different events (event repetition).
 *
 * @author maikel arabori
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventRepetition implements Serializable, EmbeddedObject {
  /** The attribute which the event repetition belongs to. */
  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  @NotNull
  private Attribute parent;

  /** The dimension associated with the event repedition. */
  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  @NotNull
  private String dimension;

  /**
   * Represents the list of event indexes to be queried. It holds a list of integers that are
   * interpreted as follows:
   *
   * <p>// @formatter:off
   *
   * <p>1 = First event 2 = Second event 3 = Third event ... -2 = Third latest event -1 = Second
   * latest event 0 = Latest event (default)
   *
   * <p>// @formatter:on
   */
  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  @NotNull
  private List<Integer> indexes = new ArrayList<>();
}
