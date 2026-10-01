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
package org.hisp.dhis.user.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.Locale;
import java.util.stream.Stream;
import javax.management.BadAttributeValueExpException;
import org.hisp.dhis.common.DisplayProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Verifies the {@link java.io.ObjectInputFilter} on {@link
 * HibernateUserSettingStore#fromBinary(String, Object)} accepts known safe types and rejects
 * potential gadget classes.
 *
 * @author Morten Svanaes
 */
class HibernateUserSettingStoreFilterTest {

  static Stream<Arguments> allowedTypes() {
    return Stream.of(
        Arguments.of("String value", "hello", "hello"),
        Arguments.of("Boolean value", Boolean.TRUE, "true"),
        Arguments.of("Integer value", 42, "42"),
        Arguments.of("Long value", 100L, "100"),
        Arguments.of("Double value", 3.14, "3.14"),
        Arguments.of("java.util.Locale", Locale.FRENCH, "fr"),
        Arguments.of("java.util.Date", new Date(1700000000000L), "1700000000000"),
        Arguments.of("DHIS2 Locale", org.hisp.dhis.common.Locale.FRENCH, "fr"),
        Arguments.of("DisplayProperty enum", DisplayProperty.NAME, "NAME"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("allowedTypes")
  void fromBinary_acceptsAllowedType(String label, Serializable input, String expected) {
    byte[] binary = serialize(input);
    assertEquals(expected, HibernateUserSettingStore.fromBinary("testKey", binary));
  }

  @Test
  void fromBinary_rejectsGadgetClass() {
    BadAttributeValueExpException gadget = new BadAttributeValueExpException("exploit");
    byte[] binary = serialize(gadget);
    assertEquals("", HibernateUserSettingStore.fromBinary("testKey", binary));
  }

  @Test
  void fromBinary_returnsEmptyForNull() {
    assertEquals("", HibernateUserSettingStore.fromBinary("testKey", null));
  }

  @Test
  void fromBinary_handlesNonByteArraySerializable() {
    assertEquals("hello", HibernateUserSettingStore.fromBinary("testKey", "hello"));
  }

  private static byte[] serialize(Serializable obj) {
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos)) {
      out.writeObject(obj);
      out.flush();
      return bos.toByteArray();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
