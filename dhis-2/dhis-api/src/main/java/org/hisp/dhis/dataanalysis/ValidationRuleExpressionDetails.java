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
package org.hisp.dhis.dataanalysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JacksonXmlRootElement
public class ValidationRuleExpressionDetails {
  private static final String NAME_PROPERTY_KEY = "name";

  private static final String VALUE_PROPERTY_KEY = "value";

  private List<Map<String, String>> leftSide = new ArrayList<>();

  private List<Map<String, String>> rightSide = new ArrayList<>();

  public static void addDetailTo(String name, String value, List<Map<String, String>> side) {
    if (!Strings.isNullOrEmpty(name)) {
      Map<String, String> map = new HashMap<>();
      map.put(NAME_PROPERTY_KEY, name);
      map.put(VALUE_PROPERTY_KEY, value);

      side.add(map);
    }
  }

  private Comparator<Map<String, String>> mapComparator =
      (m1, m2) -> m1.get("name").compareTo(m2.get("name"));

  public void sortByName() {
    Collections.sort(leftSide, mapComparator);

    Collections.sort(rightSide, mapComparator);
  }

  @JsonProperty
  public List<Map<String, String>> getLeftSide() {
    return leftSide;
  }

  public void setLeftSide(List<Map<String, String>> leftSide) {
    this.leftSide = leftSide;
  }

  @JsonProperty
  public List<Map<String, String>> getRightSide() {
    return rightSide;
  }

  public void setRightSide(List<Map<String, String>> rightSide) {
    this.rightSide = rightSide;
  }

  @Override
  public String toString() {
    return "ValidationRuleExpressionDetails{"
        + "leftSide="
        + leftSide
        + ", rightSide="
        + rightSide
        + '}';
  }
}
