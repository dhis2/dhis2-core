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
package org.hisp.dhis.common.adapter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import java.io.IOException;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodDimension;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
@AllArgsConstructor
@NoArgsConstructor
public class JacksonExtendedPeriodSerializer extends JsonSerializer<Period>
    implements ContextualSerializer {

  private I18nManager i18nManager;

  @Override
  public JsonSerializer<?> createContextual(SerializerProvider provider, BeanProperty beanProperty)
      throws JsonMappingException {
    Object manager = provider.getAttribute(I18nManager.class);
    if (manager instanceof I18nManager m) return new JacksonExtendedPeriodSerializer(m);
    return this;
  }

  @Override
  public void serialize(Period value, JsonGenerator jgen, SerializerProvider provider)
      throws IOException {
    if (value != null) {
      jgen.writeStartObject();
      jgen.writeStringField("id", value.getIsoDate());
      Object parent = jgen.currentValue();
      String name = null;
      if (parent instanceof AnalyticalObject ao) {
        PeriodDimension period =
            ao.getPeriods().stream().filter(p -> p.getPeriod() == value).findFirst().orElse(null);
        if (period != null) name = period.getName();
      }
      if (name == null && i18nManager != null)
        name = i18nManager.getI18nFormat().formatPeriod(value);
      if (name != null) jgen.writeStringField("name", name);
      jgen.writeEndObject();
    }
  }
}
