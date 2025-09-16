/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.dimensional;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hisp.dhis.common.DimensionItemKeywords;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.eventvisualization.EventRepetition;
import org.hisp.dhis.option.OptionSet;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DimensionalProperties {

  /**
   * The name of this dimension. For the dynamic dimensions this will be equal to dimension
   * identifier. For the period dimension, this will reflect the period type. For the org unit
   * dimension, this will reflect the level.
   */
  private String dimensionName;

  /** The display name to use for this dimension. */
  private String dimensionDisplayName;

  /** Holds the value type of the parent dimension. */
  private ValueType valueType;

  /** The option set associated with the dimension, if any. */
  private OptionSet optionSet;

  /** The dimensional items for this dimension. */
  private List<DimensionalItemObject> items = new ArrayList<>();

  /** Indicates whether all available items in this dimension are included. */
  private boolean allItems;

  /**
   * Filter. Applicable for events. Contains operator and filter on this format:
   * <operator>:<filter>;<operator>:<filter> Operator and filter pairs can be repeated any number of
   * times.
   */
  private String filter;

  /** Applicable only for events. Holds the indexes relate to the repetition object. */
  private EventRepetition eventRepetition;

  /**
   * A {@link DimensionItemKeywords} defines a pre-defined group of items. For instance, all the OU
   * withing a district
   */
  private DimensionItemKeywords dimensionalKeywords;

  /**
   * Indicates whether this dimension is fixed, meaning that the name of the dimension will be
   * returned as is for all dimension items in the response.
   */
  private boolean fixed;
}
