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

public class Order {
  public enum Attribute {
    NAME("name"),
    DISPLAY_NAME("displayName"),
    SHORT_NAME("shortName"),
    DISPLAY_SHORT_NAME("displayShortName");

    private String name;

    Attribute(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public static Set<String> getNames() {
      return of(Order.Attribute.values()).map(Order.Attribute::getName).collect(toSet());
    }
  }

  public enum Nature {
    ASC("asc"),
    DESC("desc");

    private String value;

    Nature(final String value) {
      this.value = value;
    }

    public String getValue() {
      return this.value;
    }

    public static Set<String> getValues() {
      return of(Order.Nature.values()).map(Order.Nature::getValue).collect(toSet());
    }
  }
}
