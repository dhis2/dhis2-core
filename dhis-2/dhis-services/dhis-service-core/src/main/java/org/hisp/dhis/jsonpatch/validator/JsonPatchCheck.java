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
package org.hisp.dhis.jsonpatch.validator;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatch;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchException;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchOperation;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;

/**
 * Contains validation method that can be added to {@link
 * org.hisp.dhis.jsonpatch.BulkPatchParameters}
 *
 * <p>which then will be used in {@link org.hisp.dhis.jsonpatch.BulkPatchManager}
 */
@FunctionalInterface
public interface JsonPatchCheck extends Function<JsonPatch, List<ErrorReport>> {
  JsonPatchCheck empty = $ -> Collections.emptyList();

  /**
   * Validate if all {@link JsonPatchOperation} of given {@link JsonPatch} are applied to "sharing"
   * property.
   */
  JsonPatchCheck isSharingPatch =
      checkPath(op -> op.getPath().matchesProperty("sharing"), ErrorCode.E4032);

  static JsonPatchCheck checkPath(
      final Predicate<JsonPatchOperation> predicate, ErrorCode errorCode) {
    return jsonPatch ->
        jsonPatch.getOperations().stream()
            .filter(op -> !predicate.test(op))
            .map(op -> new ErrorReport(JsonPatchException.class, errorCode, op.getPath()))
            .collect(Collectors.toList());
  }
}
