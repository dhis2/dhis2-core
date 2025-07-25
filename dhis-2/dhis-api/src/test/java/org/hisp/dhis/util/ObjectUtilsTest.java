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
package org.hisp.dhis.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import java.util.List;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.importexport.ImportStrategy;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class ObjectUtilsTest {

  @Test
  void testJoin() {
    DataElement deA = new DataElement("DataElementA");
    DataElement deB = new DataElement("DataElementB");
    DataElement deC = new DataElement("DataElementC");
    List<DataElement> elements = Lists.newArrayList(deA, deB, deC);
    String actual = ObjectUtils.join(elements, ", ", de -> de.getName());
    assertEquals("DataElementA, DataElementB, DataElementC", actual);
    assertEquals(null, ObjectUtils.join(null, ", ", null));
  }

  @Test
  void testNotNull() {
    assertTrue(ObjectUtils.notNull(ImportStrategy.CREATE_AND_UPDATE));
    assertFalse(ObjectUtils.notNull(null));
  }

  @Test
  void testAnyIsTrue() {
    assertTrue(ObjectUtils.anyIsTrue(true, false, false));
    assertFalse(ObjectUtils.anyIsTrue(false, false, false));
  }

  @Test
  void testAnyIsFalse() {
    assertTrue(ObjectUtils.anyIsFalse(false, true, true));
    assertFalse(ObjectUtils.anyIsFalse(true, true, true));
  }

  @Test
  void testAnyIsNull() {
    assertTrue(ObjectUtils.anyIsNull(null, true, false));
    assertFalse(ObjectUtils.anyIsNull(true, true, false));
  }
}
