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
package org.hisp.dhis.datavalue;

import java.util.function.BiFunction;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;

public final class DataValueUtil {

  private DataValueUtil() {}

  /**
   * Creates and returns a new {@link DataValue}. All the old values are used from the supplied old
   * {@link DataValue} except for the {@link DataElement} field, which uses the supplied {@link
   * DataElement}.
   */
  public static final BiFunction<DataValue, BaseIdentifiableObject, DataValue>
      dataValueWithNewDataElement =
          (oldDv, newDataElement) -> {
            DataValue newValue =
                DataValue.builder()
                    .dataElement((DataElement) newDataElement)
                    .period(oldDv.getPeriod())
                    .source(oldDv.getSource())
                    .categoryOptionCombo(oldDv.getCategoryOptionCombo())
                    .attributeOptionCombo(oldDv.getAttributeOptionCombo())
                    .value(oldDv.getValue())
                    .storedBy(oldDv.getStoredBy())
                    .lastUpdated(oldDv.getLastUpdated())
                    .comment(oldDv.getComment())
                    .followup(oldDv.isFollowup())
                    .deleted(oldDv.isDeleted())
                    .build();
            newValue.setCreated(oldDv.getCreated());
            return newValue;
          };

  /**
   * Creates and returns a new {@link DataValue}. All the old values are used from the supplied old
   * {@link DataValue} except for the {@link CategoryOptionCombo} field, which uses the supplied
   * {@link CategoryOptionCombo}.
   */
  public static final BiFunction<DataValue, BaseIdentifiableObject, DataValue>
      dataValueWithNewCatOptionCombo =
          (oldDv, newCoc) -> {
            DataValue newValue =
                DataValue.builder()
                    .dataElement(oldDv.getDataElement())
                    .period(oldDv.getPeriod())
                    .source(oldDv.getSource())
                    .categoryOptionCombo((CategoryOptionCombo) newCoc)
                    .attributeOptionCombo(oldDv.getAttributeOptionCombo())
                    .value(oldDv.getValue())
                    .storedBy(oldDv.getStoredBy())
                    .lastUpdated(oldDv.getLastUpdated())
                    .comment(oldDv.getComment())
                    .followup(oldDv.isFollowup())
                    .deleted(oldDv.isDeleted())
                    .build();
            newValue.setCreated(oldDv.getCreated());
            return newValue;
          };

  /**
   * Creates and returns a new {@link DataValue}. All the old values are used from the supplied old
   * {@link DataValue} except for the attributeOptionCombo} field, which uses the supplied {@link
   * CategoryOptionCombo}.
   */
  public static final BiFunction<DataValue, BaseIdentifiableObject, DataValue>
      dataValueWithNewAttrOptionCombo =
          (oldDv, newAoc) -> {
            DataValue newValue =
                DataValue.builder()
                    .dataElement(oldDv.getDataElement())
                    .period(oldDv.getPeriod())
                    .source(oldDv.getSource())
                    .categoryOptionCombo(oldDv.getCategoryOptionCombo())
                    .attributeOptionCombo((CategoryOptionCombo) newAoc)
                    .value(oldDv.getValue())
                    .storedBy(oldDv.getStoredBy())
                    .lastUpdated(oldDv.getLastUpdated())
                    .comment(oldDv.getComment())
                    .followup(oldDv.isFollowup())
                    .deleted(oldDv.isDeleted())
                    .build();
            newValue.setCreated(oldDv.getCreated());
            return newValue;
          };
}
