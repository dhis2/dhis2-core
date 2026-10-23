/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.attribute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.function.Consumer;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.option.OptionSet;
import org.junit.jupiter.api.Test;

class AttributeTest {

  @Test
  void shouldEqualAttributeWithSameIdentityAndValues() {
    Attribute attribute = attribute();
    Attribute other = attribute();

    assertEquals(attribute, other);
    assertEquals(attribute.hashCode(), other.hashCode());
  }

  @Test
  void shouldNotEqualAttributeWithDifferentShortNameOrDescription() {
    assertNotEqualAfter(a -> a.setShortName("Other short name"));
    assertNotEqualAfter(a -> a.setDescription("Other description"));
  }

  @Test
  void shouldNotEqualAttributeWithDifferentUidCodeOrName() {
    assertNotEqualAfter(a -> a.setUid("OtherUid001"));
    assertNotEqualAfter(a -> a.setCode("OTHER"));
    assertNotEqualAfter(a -> a.setName("Other name"));
  }

  @Test
  void shouldNotEqualAttributeWithDifferentAttributeSettings() {
    assertNotEqualAfter(a -> a.setValueType(ValueType.TEXT));
    assertNotEqualAfter(a -> a.setMandatory(false));
    assertNotEqualAfter(a -> a.setUnique(false));
    assertNotEqualAfter(a -> a.setOptionSet(new OptionSet()));
  }

  @Test
  void shouldNotEqualAttributeWithDifferentObjectTypes() {
    assertNotEqualAfter(a -> a.setUserAttribute(true));
    assertNotEqualAfter(a -> a.setDataElementAttribute(false));
  }

  private static void assertNotEqualAfter(Consumer<Attribute> change) {
    Attribute changed = attribute();
    change.accept(changed);

    assertNotEquals(attribute(), changed);
  }

  private static Attribute attribute() {
    Attribute attribute = new Attribute("AttrUid0001");
    attribute.setCode("ATTR");
    attribute.setName("Attribute");
    attribute.setShortName("Attr");
    attribute.setDescription("Description");
    attribute.setValueType(ValueType.INTEGER);
    attribute.setMandatory(true);
    attribute.setUnique(true);
    attribute.setDataElementAttribute(true);
    return attribute;
  }
}
