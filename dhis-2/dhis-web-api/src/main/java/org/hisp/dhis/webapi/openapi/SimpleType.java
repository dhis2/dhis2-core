/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.openapi;

import static java.util.Arrays.stream;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.Value;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.node.config.InclusionStrategy;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.tracker.export.FileResourceStream;
import org.hisp.dhis.webmessage.WebMessageResponse;
import org.locationtech.jts.geom.Geometry;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.multipart.MultipartFile;

@Value
@Builder
class SimpleType {
  Class<?> source;

  /*
   * OpenAPI properties below:
   */

  String type;
  String format;
  String pattern;
  Boolean nullable;
  Integer minLength;
  Integer maxLength;
  String[] enums;

  private static final Map<Class<?>, List<SimpleType>> SIMPLE_TYPES = new IdentityHashMap<>();

  public static List<SimpleType> of(Class<?> type) {
    return SIMPLE_TYPES.get(type);
  }

  public static boolean isSimpleType(Class<?> type) {
    return SIMPLE_TYPES.containsKey(type);
  }

  private static void addSimpleType(Class<?> source, Consumer<SimpleTypeBuilder> schema) {
    SimpleType.SimpleTypeBuilder b = new SimpleType.SimpleTypeBuilder();
    b.source(source);
    schema.accept(b);
    SimpleType s = b.build();
    SIMPLE_TYPES.computeIfAbsent(s.getSource(), key -> new ArrayList<>()).add(s);
  }

  public static final String REGEX_UUID =
      "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

  static {
    addSimpleType(byte.class, schema -> schema.type("integer").format("int8").nullable(false));
    addSimpleType(byte[].class, schema -> schema.type("string").format("binary").nullable(false));
    addSimpleType(int.class, schema -> schema.type("integer").format("int32").nullable(false));
    addSimpleType(long.class, schema -> schema.type("integer").format("int64").nullable(false));
    addSimpleType(float.class, schema -> schema.type("number").format("float").nullable(false));
    addSimpleType(double.class, schema -> schema.type("number").format("double").nullable(false));
    addSimpleType(boolean.class, schema -> schema.type("boolean").nullable(false));
    addSimpleType(
        char.class, schema -> schema.type("string").nullable(false).minLength(1).maxLength(1));
    addSimpleType(Integer.class, schema -> schema.type("integer").format("int32").nullable(true));
    addSimpleType(Long.class, schema -> schema.type("integer").format("int64").nullable(true));
    addSimpleType(Float.class, schema -> schema.type("number").format("float").nullable(true));
    addSimpleType(Double.class, schema -> schema.type("number").format("double").nullable(true));
    addSimpleType(Boolean.class, schema -> schema.type("boolean").nullable(true));
    addSimpleType(
        Character.class, schema -> schema.type("string").nullable(true).minLength(1).maxLength(1));
    addSimpleType(String.class, schema -> schema.type("string").nullable(true));
    addSimpleType(FieldPath.class, schema -> schema.type("string"));
    addSimpleType(Class.class, schema -> schema.type("string").format("class").nullable(false));
    addSimpleType(Date.class, schema -> schema.type("string").format("date-time").nullable(true));
    addSimpleType(URI.class, schema -> schema.type("string").format("uri").nullable(true));
    addSimpleType(URL.class, schema -> schema.type("string").format("url").nullable(true));
    addSimpleType(
        UUID.class,
        schema -> schema.type("string").format("uuid").nullable(true).pattern(REGEX_UUID));
    addSimpleType(
        UID.class,
        schema ->
            schema
                .type("string")
                .format("uid")
                .minLength(11)
                .maxLength(11)
                .nullable(true)
                .pattern(CodeGenerator.UID_REGEXP));
    addSimpleType(Locale.class, schema -> schema.type("string").nullable(true));
    addSimpleType(Instant.class, schema -> schema.type("string").format("date-time"));
    addSimpleType(Instant.class, schema -> schema.type("integer").format("int64"));
    addSimpleType(Serializable.class, schema -> schema.type("string"));
    addSimpleType(Serializable.class, schema -> schema.type("number"));
    addSimpleType(Serializable.class, schema -> schema.type("boolean"));

    addSimpleType(Period.class, schema -> schema.type("string").format("period"));
    addSimpleType(
        PeriodType.class,
        schema ->
            schema
                .type("string")
                .enums(
                    stream(PeriodTypeEnum.values())
                        .map(PeriodTypeEnum::getName)
                        .toArray(String[]::new)));
    addSimpleType(
        InclusionStrategy.class,
        schema ->
            schema
                .type("string")
                .enums(
                    stream(InclusionStrategy.Include.values())
                        .map(Enum::name)
                        .toArray(String[]::new)));

    addSimpleType(MultipartFile.class, schema -> schema.type("string").format("binary"));
    addSimpleType(InputStreamResource.class, schema -> schema.type("string").format("binary"));
    addSimpleType(FileResourceStream.class, schema -> schema.type("string").format("binary"));

    addSimpleType(JsonNode.class, schema -> schema.type("object"));
    addSimpleType(ObjectNode.class, schema -> schema.type("object"));
    addSimpleType(ArrayNode.class, schema -> schema.type("array"));
    addSimpleType(RootNode.class, schema -> schema.type("object"));
    addSimpleType(JsonPointer.class, schema -> schema.type("string"));

    addSimpleType(Geometry.class, schema -> schema.type("object"));
    addSimpleType(WebMessageResponse.class, schema -> schema.type("object"));
    addSimpleType(JobParameters.class, schema -> schema.type("object"));
  }
}
