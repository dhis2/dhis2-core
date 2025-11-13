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
package org.hisp.dhis.hibernate.jsonb.type;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.NoSuchElementException;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobParameters;

/**
 * @author Henning Håkonsen
 */
public class JsonJobParametersType extends JsonBinaryType {

  static final ObjectMapper MAPPER =
      new ObjectMapper()
          .enableDefaultTyping()
          .setSerializationInclusion(JsonInclude.Include.NON_NULL)
          .setAnnotationIntrospector(
              new IgnoreJsonPropertyWriteOnlyAccessJacksonAnnotationIntrospector());

  /**
   * Note that because of the way the Java class information is encoded in the JSON we do need this
   * work-around to get the exact parsing of the {@link JobParameters} that would occur if the
   * {@link JobConfiguration#getJobParameters()} property was parsed.
   */
  private static final TypeReference<JobParameters> GET_JOB_PARAMETERS =
      MethodTypeReference.fromMethod(JobConfiguration.class, "getJobParameters");

  @Override
  protected ObjectMapper getResultingMapper() {
    return MAPPER;
  }

  private static class MethodTypeReference<T> extends TypeReference<T> {
    public static <T> TypeReference<T> fromMethod(Class<?> clazz, String methodName) {
      try {
        Method method = clazz.getMethod(methodName);
        Type returnType = method.getGenericReturnType();
        return new TypeReference<T>() {
          @Override
          public Type getType() {
            return returnType;
          }
        };
      } catch (NoSuchMethodException e) {
        throw new NoSuchElementException("Method not found: " + methodName, e);
      }
    }
  }

  public static JobParameters fromJson(String json) {
    if (json == null || "null".equals(json) || "[]".equals(json) || "{}".equals(json)) return null;
    try {
      return MAPPER.readValue(json, GET_JOB_PARAMETERS);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
