/*
 * Copyright (c) 2004-2004, University of Oslo
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
package org.hisp.dhis.analytics.common;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;

public class AnalyticsDimensionsTestSupport {
  public static TrackedEntityType trackedEntityType() {
    TrackedEntityType trackedEntityType = new TrackedEntityType();
    trackedEntityType.setTrackedEntityTypeAttributes(
        allValueTypeTEAs().stream()
            .map(
                trackedEntityAttribute ->
                    new TrackedEntityTypeAttribute(trackedEntityType, trackedEntityAttribute))
            .toList());
    return trackedEntityType;
  }

  public static List<TrackedEntityAttribute> allValueTypeTEAs() {
    return buildWithAllValueTypes(
            valueType -> {
              TrackedEntityAttribute trackedEntityAttribute = new TrackedEntityAttribute();
              trackedEntityAttribute.setUid("uid" + valueType.name());
              trackedEntityAttribute.setValueType(valueType);
              return trackedEntityAttribute;
            })
        .toList();
  }

  public static Set<DataElement> allValueTypeDataElements() {
    return buildWithAllValueTypes(
            valueType -> {
              DataElement dataElement = new DataElement();
              dataElement.setUid("uid" + valueType.name());
              dataElement.setValueType(valueType);
              return dataElement;
            })
        .collect(Collectors.toSet());
  }

  public static <T> Stream<T> buildWithAllValueTypes(Function<ValueType, T> mapper) {
    return Arrays.stream(ValueType.values()).map(mapper);
  }
}
