/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.category;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.hisp.dhis.common.SystemDefaultMetadataObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link Category}.
 *
 * @author Volker Schmidt
 */
class CategoryTest {

  @Test
  void hasDefault() {
    Assertions.assertTrue(SystemDefaultMetadataObject.class.isAssignableFrom(Category.class));
  }

  @Test
  void isDefault() {
    Category category = new Category();
    category.setName(Category.DEFAULT_NAME);
    Assertions.assertTrue(category.isDefault());
  }

  @Test
  void isNotDefault() {
    Category category = new Category();
    category.setName(Category.DEFAULT_NAME + "x");
    Assertions.assertFalse(category.isDefault());
  }

  @ParameterizedTest
  @MethodSource("categoryEqualsParams")
  @DisplayName("Category equals check has expected result")
  void categoryEqualsTest(
      String name,
      String uid,
      String code,
      String shortName,
      boolean expectedResult) {
    Category cParams = new Category();
    cParams.setName(name);
    cParams.setUid(uid);
    cParams.setCode(code);
    cParams.setShortName(shortName);

    assertEquals(
        expectedResult, getCategory().equals(cParams), "Category equals check has expected result");
  }

  private static Stream<Arguments> categoryEqualsParams() {
    boolean isEqual = true;
    boolean isNotEqual = false;

    return Stream.of(
        Arguments.of("name", "uid", "code", "shortName",  isEqual),
        Arguments.of("name", "uid", "code", "shortName diff", isEqual),
        Arguments.of("name", "uid", "code diff", "shortName", isNotEqual),
        Arguments.of("name", "uid diff", "code", "shortName", isNotEqual),
        Arguments.of("name diff", "uid", "code", "shortName", isNotEqual));
  }

  private Category getCategory() {
    Category c = new Category();
    c.setName("name");
    c.setUid("uid");
    c.setCode("code");
    c.setShortName("shortName");
    return c;
  }
}
