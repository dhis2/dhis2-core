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

import static org.hisp.dhis.render.type.ValueTypeRenderingType.BAR_CODE;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.hisp.dhis.render.DeviceRenderTypeMap;
import org.hisp.dhis.render.RenderDevice;
import org.hisp.dhis.render.type.ValueTypeRenderingObject;
import org.hisp.dhis.translation.Translation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JsonBinaryType}.
 *
 * @author Volker Schmidt
 */
class JsonBinaryTypeTest {

  private JsonBinaryType jsonBinaryType;

  private Translation translation1;

  @BeforeEach
  void setUp() {
    translation1 = new Translation();
    translation1.setLocale("en");
    translation1.setValue("English Test 1");
    jsonBinaryType = new JsonBinaryType();
    jsonBinaryType.init(Translation.class);
  }

  @Test
  void deepCopy() {
    final Translation result = (Translation) jsonBinaryType.deepCopy(translation1);
    Assertions.assertNotSame(translation1, result);
    Assertions.assertEquals(translation1, result);
  }

  @Test
  void deepCopyNull() {
    Assertions.assertNull(jsonBinaryType.deepCopy(null));
  }

  @Test
  void testEquals() {
    DeviceRenderTypeMap<ValueTypeRenderingObject> objOne =
        getDeviceRenderTypeAs(this::getValueTypeRenderingObject);
    DeviceRenderTypeMap<Map<String, Object>> objTwo = getDeviceRenderTypeAs(this::getMap);
    assertTrue(jsonBinaryType.equals(objOne, objTwo));
  }

  private <T> DeviceRenderTypeMap<T> getDeviceRenderTypeAs(Supplier<T> genericInstanceSupplier) {
    DeviceRenderTypeMap<T> objectDeviceRenderTypeMap = new DeviceRenderTypeMap<>();
    objectDeviceRenderTypeMap.put(RenderDevice.MOBILE, genericInstanceSupplier.get());
    return objectDeviceRenderTypeMap;
  }

  private ValueTypeRenderingObject getValueTypeRenderingObject() {
    ValueTypeRenderingObject valueTypeRenderingObject = new ValueTypeRenderingObject();
    valueTypeRenderingObject.setType(BAR_CODE);
    return valueTypeRenderingObject;
  }

  private Map<String, Object> getMap() {
    Map<String, Object> map = new HashMap<>();
    map.put("type", BAR_CODE.name());
    return map;
  }
}
