/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.openapi;

import static org.hisp.dhis.common.CodeGenerator.UID_REGEXP;
import static org.hisp.dhis.webapi.openapi.ApiDescriptions.toMarkdown;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonBoolean;
import org.hisp.dhis.jsontree.JsonDate;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonNumber;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.node.config.InclusionStrategy;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.tracker.export.FileResourceStream;
import org.hisp.dhis.webapi.webdomain.EndDateTime;
import org.hisp.dhis.webapi.webdomain.StartDateTime;
import org.intellij.lang.annotations.Language;
import org.locationtech.jts.geom.Geometry;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * A type mapping between a Java type and its OpenAPI form that is specified directly in terms of an
 * OpenAPI type (schema) declaration.
 *
 * <p>As such it is one of the basic building blocks of OpenAPI similar to Java primitives and value
 * types like {@link String}.
 *
 * @author Jan Bernitt
 */
@Value
@Accessors(fluent = true)
@RequiredArgsConstructor
class DirectType {

  Class<?> source;
  Map<String, SimpleType> oneOf;
  boolean shared;

  /** A type declared in terms of OpenAPI schema properties */
  @Value
  @Builder
  static class SimpleType {

    String type;
    String format;
    String pattern;
    Boolean nullable;
    Integer minLength;
    Integer maxLength;
    List<String> enums;
    String description;
  }

  private static final Map<Class<?>, DirectType> TYPES = new IdentityHashMap<>();

  /**
   * @param type a Java type
   * @return {@code oneOf} the OpenAPI types that correspond to the Java type
   */
  @CheckForNull
  public static DirectType of(Class<?> type) {
    return TYPES.get(type);
  }

  public static boolean isDirectType(Class<?> type) {
    return TYPES.containsKey(type);
  }

  /**
   * Adds a OpenAPI {@code oneOf} schema {@link SimpleType} to the {@link DirectType} for the
   * provided source {@link Class}.
   *
   * @param source the Java type to associate with a directly declared {@link SimpleType}
   * @param schema builder used to declare the schema declaration of the simple type to add
   */
  private static void oneOf(Class<?> source, Consumer<SimpleType.SimpleTypeBuilder> schema) {
    SimpleType.SimpleTypeBuilder b = new SimpleType.SimpleTypeBuilder();
    b.enums(List.of()); // default
    boolean autoShare =
        source.isAnnotationPresent(OpenApi.Description.class)
            && source.isAnnotationPresent(OpenApi.Shared.class)
            && source.getAnnotation(OpenApi.Shared.class).value();
    if (autoShare) b.description(toMarkdown(source.getAnnotation(OpenApi.Description.class)));
    schema.accept(b);
    SimpleType type = b.build();
    TYPES.compute(
        source,
        (k, v) ->
            v == null
                ? new DirectType(k, Map.of(type.type(), type), autoShare)
                : new DirectType(k, put(v.oneOf(), type.type(), type), autoShare || v.shared()));
  }

  private static <K, V> Map<K, V> put(Map<K, V> into, K key, V value) {
    if (into.isEmpty()) return Map.of(key, value);
    Map<K, V> res = new HashMap<>(into);
    res.put(key, value);
    return Map.copyOf(res);
  }

  /**
   * Flags the {@link DirectType} associated with the provided Java type as a shared (named) type
   * that is referenced as a named schema in an OpenAPI document.
   *
   * @param source the Java type
   */
  private static void share(Class<?> source) {
    TYPES.compute(
        source,
        (k, v) -> v == null ? new DirectType(k, Map.of(), true) : new DirectType(k, v.oneOf, true));
  }

  @Language("RegExp")
  private static final String REGEX_UUID =
      "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

  /** Note that this is an approximation of a strictly correct locale string :/ */
  @Language("RegExp")
  private static final String REGEX_LOCALE =
      "^(?i)(?<lang>[a-z]{2,8})(?:_(?<script>[a-z]{4}))?(?:_(?<country>[a-z]{2}|[0-9]{3}))?(?:_(?<variant>[0-9a-z]{3,8}))?$";

  static {
    oneOf(byte.class, schema -> schema.type("integer").format("int8").nullable(false));
    oneOf(byte[].class, schema -> schema.type("string").format("binary").nullable(false));
    oneOf(int.class, schema -> schema.type("integer").format("int32").nullable(false));
    oneOf(long.class, schema -> schema.type("integer").format("int64").nullable(false));
    oneOf(float.class, schema -> schema.type("number").format("float").nullable(false));
    oneOf(double.class, schema -> schema.type("number").format("double").nullable(false));
    oneOf(boolean.class, schema -> schema.type("boolean").nullable(false));
    oneOf(char.class, schema -> schema.type("string").nullable(false).minLength(1).maxLength(1));
    oneOf(Integer.class, schema -> schema.type("integer").format("int32").nullable(true));
    oneOf(Long.class, schema -> schema.type("integer").format("int64").nullable(true));
    oneOf(Float.class, schema -> schema.type("number").format("float").nullable(true));
    oneOf(Double.class, schema -> schema.type("number").format("double").nullable(true));
    oneOf(Boolean.class, schema -> schema.type("boolean").nullable(true));
    oneOf(
        Character.class, schema -> schema.type("string").nullable(true).minLength(1).maxLength(1));
    oneOf(String.class, schema -> schema.type("string").nullable(true));
    oneOf(FieldPath.class, schema -> schema.type("string"));
    oneOf(Class.class, schema -> schema.type("string").format("class").nullable(false));
    oneOf(Date.class, schema -> schema.type("string").format("date-time").nullable(true));
    oneOf(StartDateTime.class, schema -> schema.type("string").format("date-time").nullable(true));
    oneOf(EndDateTime.class, schema -> schema.type("string").format("date-time").nullable(true));
    oneOf(URI.class, schema -> schema.type("string").format("uri").nullable(true));
    oneOf(URL.class, schema -> schema.type("string").format("url").nullable(true));
    oneOf(
        UUID.class,
        schema -> schema.type("string").format("uuid").nullable(true).pattern(REGEX_UUID));
    oneOf(
        UID.class,
        schema ->
            schema
                .type("string")
                .format("uid")
                .minLength(11)
                .maxLength(11)
                .nullable(true)
                .pattern(UID_REGEXP));
    oneOf(
        Locale.class,
        schema -> schema.type("string").nullable(true).format("locale").pattern(REGEX_LOCALE));
    oneOf(Instant.class, schema -> schema.type("string").format("date-time"));
    oneOf(Instant.class, schema -> schema.type("integer").format("int64"));
    share(Instant.class);
    oneOf(Serializable.class, schema -> schema.type("string"));
    oneOf(Serializable.class, schema -> schema.type("number"));
    oneOf(Serializable.class, schema -> schema.type("boolean"));

    oneOf(Period.class, schema -> schema.type("string").format("period"));
    oneOf(PeriodDimension.class, schema -> schema.type("string").format("period"));
    oneOf(
        PeriodType.class,
        schema ->
            schema.type("string").enums(enums(PeriodTypeEnum.class, PeriodTypeEnum::getName)));
    oneOf(
        InclusionStrategy.class,
        schema -> schema.type("string").enums(enums(InclusionStrategy.Include.class, Enum::name)));

    oneOf(MultipartFile.class, schema -> schema.type("string").format("binary"));
    oneOf(InputStreamResource.class, schema -> schema.type("string").format("binary"));
    oneOf(FileResourceStream.class, schema -> schema.type("string").format("binary"));

    oneOf(JsonNode.class, schema -> schema.type("any"));
    oneOf(ObjectNode.class, schema -> schema.type("object"));
    oneOf(ArrayNode.class, schema -> schema.type("array"));
    oneOf(RootNode.class, schema -> schema.type("object"));
    oneOf(JsonPointer.class, schema -> schema.type("string"));

    oneOf(JsonValue.class, schema -> schema.type("any"));
    oneOf(JsonMixed.class, schema -> schema.type("any"));
    oneOf(JsonObject.class, schema -> schema.type("object"));
    oneOf(JsonArray.class, schema -> schema.type("array"));
    oneOf(JsonString.class, schema -> schema.type("string"));
    oneOf(JsonNumber.class, schema -> schema.type("number"));
    oneOf(JsonBoolean.class, schema -> schema.type("boolean"));
    oneOf(JsonDate.class, schema -> schema.type("string").format("date-time"));

    oneOf(Geometry.class, schema -> schema.type("object"));
    oneOf(JobParameters.class, schema -> schema.type("object"));

    oneOf(StreamingResponseBody.class, schema -> schema.type("any"));
  }

  private static <T extends Enum<T>> List<String> enums(Class<T> type, Function<T, String> name) {
    return Stream.of(type.getEnumConstants()).map(name).toList();
  }
}
