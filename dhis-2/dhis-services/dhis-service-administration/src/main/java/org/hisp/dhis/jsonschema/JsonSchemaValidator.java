/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

@Slf4j
public class JsonSchemaValidator {

  private static final String DATA_INTEGRITY_CHECK_DIR = "data-integrity-checks/";
  private static final String DATA_INTEGRITY_CHECK_SCHEMA_FILE = "integrity_check_schema.json";
  private static final String DATA_INTEGRITY_CHECK_SCHEMA =
      DATA_INTEGRITY_CHECK_DIR + DATA_INTEGRITY_CHECK_SCHEMA_FILE;
  private static JsonSchema dataIntegritySchema;

  static {
    JsonSchemaFactory schemaFactory =
        JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    try (InputStream is = new ClassPathResource(DATA_INTEGRITY_CHECK_SCHEMA).getInputStream()) {
      dataIntegritySchema = schemaFactory.getSchema(is);
    } catch (IOException e) {
      log.error(
          "Error loading data integrity check schema at class path location {}. Error message: {}",
          DATA_INTEGRITY_CHECK_SCHEMA,
          e.getMessage());
    }
  }

  private JsonSchemaValidator() {
    throw new UnsupportedOperationException("util");
  }

  public static Set<ValidationMessage> validateDataIntegrityCheck(JsonNode json) {
    return dataIntegritySchema.validate(json);
  }
}
