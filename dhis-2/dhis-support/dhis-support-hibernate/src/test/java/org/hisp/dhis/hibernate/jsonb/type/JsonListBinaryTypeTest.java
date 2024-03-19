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
package org.hisp.dhis.hibernate.jsonb.type;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.ArrayList;
import java.util.List;
import org.hamcrest.Matchers;
import org.hisp.dhis.translation.Translation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JsonListBinaryType}.
 *
 * @author Volker Schmidt
 */
class JsonListBinaryTypeTest {

  private JsonListBinaryType jsonBinaryType;

  private List<Translation> translations;

  private Translation translation1;

  private Translation translation2;

  @BeforeEach
  void setUp() {
    translation1 = new Translation();
    translation1.setLocale("en");
    translation1.setValue("English Test 1");
    translation2 = new Translation();
    translation2.setLocale("no");
    translation2.setValue("Norwegian Test 1");
    translations = new ArrayList<>();
    translations.add(translation1);
    translations.add(translation2);
    jsonBinaryType = new JsonListBinaryType();
    jsonBinaryType.init(Translation.class);
  }

  @SuppressWarnings("unchecked")
  @Test
  void deepCopy() {
    final List<Translation> result = (List<Translation>) jsonBinaryType.deepCopy(translations);
    assertNotSame(translations, result);
    assertThat(result, Matchers.contains(translation1, translation2));
  }

  @Test
  void deepCopyNull() {
    Assertions.assertNull(jsonBinaryType.deepCopy(null));
  }
}
