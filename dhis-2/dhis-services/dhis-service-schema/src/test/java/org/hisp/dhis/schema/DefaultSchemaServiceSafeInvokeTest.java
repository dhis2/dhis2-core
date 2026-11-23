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
package org.hisp.dhis.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

import java.lang.reflect.Method;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.BaseNameableObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class DefaultSchemaServiceSafeInvokeTest {

  @Test
  void testInterfaceFallbackDoesNotInvokeBaseMethod() throws Exception {
    Method getterMethod = BaseDimensionalItemObject.class.getMethod("getDimensionItem");

    CategoryOption categoryOption = new CategoryOption("Test Option");
    categoryOption.setUid("CatOptUid01");

    try (MockedStatic<ReflectionUtils> reflectionUtils =
        mockStatic(ReflectionUtils.class, CALLS_REAL_METHODS)) {
      String result = DefaultSchemaService.safeInvoke(categoryOption, getterMethod);

      assertEquals("CatOptUid01", result);
      reflectionUtils.verify(
          () -> ReflectionUtils.invokeMethod(categoryOption, getterMethod), never());
    }
  }

  @Test
  void testNullMethodReturnsNull() {
    CategoryOption categoryOption = new CategoryOption("Test Option");

    assertNull(DefaultSchemaService.safeInvoke(categoryOption, null));
  }

  @Test
  void testNameNotNull() throws Exception {
    Method getterMethod = BaseNameableObject.class.getMethod("getDisplayName");

    DataElement dataElement = new DataElement("Test Element");

    String result = DefaultSchemaService.safeInvoke(dataElement, getterMethod);

    assertNotNull(result);
    assertEquals("Test Element", result);
  }

  @Test
  void testNullName() throws Exception {
    Method getterMethod = BaseNameableObject.class.getMethod("getDisplayName");

    DataElement dataElement = new DataElement();

    String result = DefaultSchemaService.safeInvoke(dataElement, getterMethod);

    assertEquals(null, result);
  }

  @Test
  void testDimensionalObject() throws Exception {
    Method getterMethod = BaseNameableObject.class.getMethod("getDisplayName");

    DimensionalObject dimensionalObject = new BaseDimensionalObject();
    dimensionalObject.setName("Test Dimensional Object");

    String result = DefaultSchemaService.safeInvoke(dimensionalObject, getterMethod);

    assertEquals("Test Dimensional Object", result);
  }

  @Test
  void testMethodNotFound() throws Exception {
    Method getterMethod = BaseNameableObject.class.getMethod("getDisplayName");

    Object object = new Object();

    RuntimeException exception =
        assertThrows(
            RuntimeException.class, () -> DefaultSchemaService.safeInvoke(object, getterMethod));
    assertEquals("Failed to invoke method getDisplayName", exception.getMessage());
  }

  @Test
  void testObjectDoesNotHaveProperty() throws Exception {
    Method getterMethod = BaseNameableObject.class.getMethod("getDisplayShortName");
    Legend legend = new Legend();
    legend.setName("test");
    RuntimeException exception =
        assertThrows(
            RuntimeException.class, () -> DefaultSchemaService.safeInvoke(legend, getterMethod));
    assertEquals("Failed to invoke method getDisplayShortName", exception.getMessage());
  }
}
