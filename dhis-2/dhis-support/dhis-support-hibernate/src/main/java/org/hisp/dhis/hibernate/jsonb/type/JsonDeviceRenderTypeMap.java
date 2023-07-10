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

import com.fasterxml.jackson.databind.JavaType;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Properties;
import org.hisp.dhis.render.DeviceRenderTypeMap;
import org.hisp.dhis.render.RenderDevice;
import org.hisp.dhis.render.type.RenderingObject;

public class JsonDeviceRenderTypeMap extends JsonBinaryType {
  private Class<? extends RenderingObject> renderType;

  @Override
  protected JavaType getResultingJavaType(Class<?> returnedClass) {
    return MAPPER.getTypeFactory().constructType(DeviceRenderTypeMap.class);
  }

  @Override
  protected Object convertJsonToObject(String content) {
    try {
      LinkedHashMap<RenderDevice, LinkedHashMap<String, String>> map = reader.readValue(content);
      return convertMapToObject(map);
    } catch (IOException | IllegalAccessException | InstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setParameterValues(Properties parameters) {
    super.setParameterValues(parameters);

    final String renderType = (String) parameters.get("renderType");

    if (renderType == null) {
      throw new IllegalArgumentException(
          String.format("Required parameter '%s' is not configured", "renderType"));
    }

    try {
      this.renderType = (Class<? extends RenderingObject>) classForName(renderType);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Class: " + renderType + " is not a known class type.");
    }
  }

  private <T extends RenderingObject<?>> DeviceRenderTypeMap<T> convertMapToObject(
      LinkedHashMap<RenderDevice, LinkedHashMap<String, String>> map)
      throws IllegalAccessException, InstantiationException {
    DeviceRenderTypeMap deviceRenderTypeMap = new DeviceRenderTypeMap<>();
    for (RenderDevice renderDevice : map.keySet()) {
      LinkedHashMap<String, String> renderObjectMap = map.get(renderDevice);
      RenderingObject renderingObject = renderType.newInstance();
      renderingObject.setType(
          Enum.valueOf(
              renderingObject.getRenderTypeClass(), renderObjectMap.get(RenderingObject._TYPE)));
      deviceRenderTypeMap.put(renderDevice, renderingObject);
    }
    return deviceRenderTypeMap;
  }
}
