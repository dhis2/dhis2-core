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
package org.hisp.dhis.webapi.controller.dataitem;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;

import java.util.Set;

public class Filter {
  public enum Attribute {
    DIMENSION_TYPE("dimensionItemType"),
    VALUE_TYPE("valueType"),
    NAME("name"),
    DISPLAY_NAME("displayName"),
    SHORT_NAME("shortName"),
    DISPLAY_SHORT_NAME("displayShortName"),
    PROGRAM_ID("programId"),
    ID("id");

    private String name;

    Attribute(final String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public static Set<String> getNames() {
      return of(Attribute.values()).map(Attribute::getName).collect(toSet());
    }
  }

  public enum Operation {
    EQ("eq"),
    IN("in"),
    ILIKE("ilike"),
    TOKEN("token");

    private String abbreviation;

    Operation(final String abbreviation) {
      this.abbreviation = abbreviation;
    }

    public String getAbbreviation() {
      return this.abbreviation;
    }

    public static Set<String> getAbbreviations() {
      return of(Operation.values()).map(Operation::getAbbreviation).collect(toSet());
    }
  }

  public enum Custom {
    IDENTIFIABLE("identifiable");

    private String propertyName;

    Custom(final String propertyName) {
      this.propertyName = propertyName;
    }

    public String getPropertyName() {
      return this.propertyName;
    }

    public static Set<String> getPropertyNames() {
      return of(Custom.values()).map(Custom::getPropertyName).collect(toSet());
    }
  }

  public enum Combination {
    DIMENSION_TYPE_IN("dimensionItemType:in:"),
    DIMENSION_TYPE_EQUAL("dimensionItemType:eq:"),
    VALUE_TYPE_IN("valueType:in:"),
    VALUE_TYPE_EQUAL("valueType:eq:"),
    NAME_ILIKE("name:ilike:"),
    DISPLAY_NAME_ILIKE("displayName:ilike:"),
    SHORT_NAME_ILIKE("shortName:ilike:"),
    DISPLAY_SHORT_NAME_ILIKE("displayShortName:ilike:"),
    PROGRAM_ID_EQUAL("programId:eq:"),
    ID_EQUAL("id:eq:"),
    IDENTIFIABLE_TOKEN("identifiable:token:");

    private String combination;

    Combination(final String combination) {
      this.combination = combination;
    }

    public String getCombination() {
      return this.combination;
    }

    public static Set<String> getCombinations() {
      return of(Combination.values()).map(Combination::getCombination).collect(toSet());
    }
  }
}
