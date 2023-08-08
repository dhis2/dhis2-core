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
package org.hisp.dhis.visualization;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.hisp.dhis.common.DimensionalObject.ATTRIBUTEOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.CATEGORYOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hisp.dhis.common.DimensionType;

/**
 * This class is used to hold the association between a dimension and its type. Its main goal is to
 * track the associations across dynamic dimensions and their actual type.
 */
@Getter
@Setter
@AllArgsConstructor
public class DimensionDescriptor {
  private String dimension;

  private DimensionType type;

  public boolean hasDimension(final String dimension) {
    return trimToEmpty(dimension).equalsIgnoreCase(trimToEmpty(getDimension()));
  }

  /**
   * Based on the given dimension and the given DimensionDescriptor list, this method will retrieve
   * the respective dimension identifier. See the examples below.
   *
   * <p>For regular dimensions: a "dimension" `dx` will have a type of {@link DimensionType#DATA_X}.
   * Hence the dimension identifier returned will be `dx`.
   *
   * <p>For dynamic dimensions: a "dimension" `mq4jAnN6fg3` (of an org unit, for example), will have
   * a type of {@link DimensionType#ORGANISATION_UNIT}. Hence the dimension identifier returned by
   * this method will be `ou`.
   *
   * @param dimensionDescriptors the list of descriptors to be compared.
   * @param dimension the value to be retrieved from the list of dimensionDescriptors
   * @return the respective descriptive value
   */
  public static String getDimensionIdentifierFor(
      final String dimension, final List<DimensionDescriptor> dimensionDescriptors) {
    if (isNotEmpty(dimensionDescriptors)) {
      // For each dimension descriptor
      for (final DimensionDescriptor dimensionDescriptor : dimensionDescriptors) {
        if (dimensionDescriptor.hasDimension(dimension)) {
          // Returns the string/value associated with the dimension
          // type.
          return dimensionDescriptor.getDimensionIdentifier();
        }
      }
    }

    return dimension;
  }

  /**
   * This method will return the respective dimension identifier associated with the current
   * dimension {@link #type}.
   *
   * @return the dimension identifier. See {@link org.hisp.dhis.common.DimensionalObject} for the
   *     list of possible dimension identifiers.
   */
  private String getDimensionIdentifier() {
    switch (getType()) {
      case ATTRIBUTE_OPTION_COMBO:
        return ATTRIBUTEOPTIONCOMBO_DIM_ID;
      case CATEGORY_OPTION_COMBO:
        return CATEGORYOPTIONCOMBO_DIM_ID;
      case DATA_X:
        return DATA_X_DIM_ID;
      case ORGANISATION_UNIT:
      case ORGANISATION_UNIT_GROUP_SET:
        return ORGUNIT_DIM_ID;
      case PERIOD:
        return PERIOD_DIM_ID;
      default:
        return EMPTY;
    }
  }
}
