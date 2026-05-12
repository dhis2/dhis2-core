/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.common.validation;

import java.util.function.Function;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.jsontree.JsonObject;

/**
 * Validates input against a schema {@link Class} which has {@link
 * org.hisp.dhis.jsontree.Validation} annotations.
 *
 * <p>Formal input validation is all the validation that can be performed solely based on a target
 * schema. In other words, it is a validation of input within the static (type) context without
 * checking validity in the context the value will exist. Such semantic validation is performed in
 * later stages.
 *
 * @author Jan Bernitt
 * @since 2.44
 */
public interface InputValidationService {

  /**
   * Decodes key-value input such as request parameters into a JSON value based on the target
   * schema.
   *
   * @param schema the target the key-value data should conform to
   * @param values a lookup function to return the values for a given key
   * @return a JSON object with the key-value data found in the given schema as provided by the
   *     values loopup function
   */
  JsonObject decode(Class<? extends Record> schema, Function<String, String[]> values);

  /**
   * Formal validation of the given input against the given schema.
   *
   * @param schema the target type the input should conform to
   * @param input typically user input such as request bodies
   * @throws BadRequestException in case the input is not valid
   */
  void validate(Class<?> schema, JsonObject input) throws BadRequestException;
}
