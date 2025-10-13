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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
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

  private static final ObjectReader CONFIG_PARAMS_READER =
      MAPPER
          .readerFor(JobConfiguration.class)
          .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  @Override
  protected ObjectMapper getResultingMapper() {
    return MAPPER;
  }

  public static JobParameters fromJson(String json) {
    if (json == null || "null".equals(json) || "[]".equals(json) || "{}".equals(json)) return null;
    // The idea here is to reuse the jackson mapping
    // based on the annotations present on JobConfiguration
    // for that we wrap the parameters in a shallow job object
    String config =
        """
      {
        "jobParameters": %s
      }
      """
            .formatted(json);
    try {
      JobConfiguration c = CONFIG_PARAMS_READER.readValue(config);
      return c.getJobParameters();
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
