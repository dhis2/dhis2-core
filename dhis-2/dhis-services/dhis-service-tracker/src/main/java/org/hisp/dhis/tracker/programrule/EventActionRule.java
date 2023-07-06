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
package org.hisp.dhis.tracker.programrule;

import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.rules.models.AttributeType;
import org.hisp.dhis.tracker.domain.DataValue;

@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class EventActionRule implements ActionRule {
  private final String ruleUid;

  private final String event;

  private final String data;

  private final String field;

  private final AttributeType attributeType;

  private String content;

  private Set<DataValue> dataValues;

  public String getValue() {
    StringBuilder stringBuilder = new StringBuilder();
    if (!StringUtils.isEmpty(content)) {
      stringBuilder.append(data);
    }
    if (!StringUtils.isEmpty(stringBuilder.toString())) {
      stringBuilder.append(" ");
    }
    if (!StringUtils.isEmpty(data)) {
      stringBuilder.append(data);
    }
    return stringBuilder.toString();
  }

  public Optional<DataValue> getDataValue() {
    if (attributeType.equals(AttributeType.DATA_ELEMENT)) {
      return getDataValues().stream().filter(dv -> dv.getDataElement().equals(field)).findAny();
    }

    return Optional.empty();
  }
}
