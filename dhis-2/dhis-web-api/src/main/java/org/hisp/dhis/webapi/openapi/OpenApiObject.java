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

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonMap;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.jsontree.Required;
import org.hisp.dhis.jsontree.Validation;
import org.intellij.lang.annotations.Language;

/**
 * JSON structure of an OpenAPI document as defined by the OpenAPI standard.
 *
 * <p>Please not that this is just an extract of the complete standard that does only concern itself
 * with the properties that are used. However, the names of types, fields and methods follow those
 * given by the <a href="https://swagger.io/specification">OpenAPI specification</a> as close as
 * possible (unless this is in conflict with the Java language specification).
 *
 * @author Jan Bernitt
 * @since 2.42
 */
@OpenApi.Kind("OpenAPI document")
public interface OpenApiObject extends JsonObject {

  @Required
  default String openapi() {
    return getString("openapi").string();
  }

  @Required
  default InfoObject info() {
    return get("info", InfoObject.class);
  }

  default JsonList<ServerObject> servers() {
    return getList("servers", ServerObject.class);
  }

  default JsonMap<PathItemObject> $paths() {
    return getMap("paths", PathItemObject.class);
  }

  default ComponentsObject components() {
    return get("components", ComponentsObject.class);
  }

  default JsonList<TagObject> tags() {
    return getList("tags", TagObject.class);
  }

  default Stream<OperationObject> operations() {
    return $paths()
        .values()
        .flatMap(
            item ->
                Stream.of(
                        item.get(),
                        item.post(),
                        item.put(),
                        item.patch(),
                        item.delete(),
                        item.head(),
                        item.trace(),
                        item.options())
                    .filter(JsonValue::exists));
  }

  interface InfoObject extends JsonObject {
    @Required
    default String title() {
      return getString("title").string();
    }

    default String summary() {
      return getString("summary").string();
    }

    @Language("markdown")
    default String description() {
      return getString("description").string();
    }

    @Required
    default String version() {
      return getString("version").string();
    }
  }

  interface TagObject extends JsonObject {
    @Required
    default String name() {
      return getString("name").string();
    }

    @Language("markdown")
    default String description() {
      return getString("description").string();
    }

    default String externalDocs() {
      return getString("externalDocs").string();
    }
  }

  interface ServerObject extends JsonObject {
    @Required
    default String url() {
      return getString("url").string();
    }

    @Language("markdown")
    default String description() {
      return getString("description").string();
    }
  }

  interface PathItemObject extends JsonObject {

    default String $ref() {
      return getString("$ref").string();
    }

    default String summary() {
      return getString("summary").string();
    }

    @Language("markdown")
    default String description() {
      return getString("description").string();
    }

    default OperationObject get() {
      return get("get", OperationObject.class);
    }

    default OperationObject put() {
      return get("put", OperationObject.class);
    }

    default OperationObject post() {
      return get("post", OperationObject.class);
    }

    default OperationObject delete() {
      return get("delete", OperationObject.class);
    }

    default OperationObject options() {
      return get("options", OperationObject.class);
    }

    default OperationObject head() {
      return get("head", OperationObject.class);
    }

    default OperationObject patch() {
      return get("patch", OperationObject.class);
    }

    default OperationObject trace() {
      return get("trace", OperationObject.class);
    }
  }

  interface ComponentsObject extends JsonObject {

    default JsonMap<SchemaObject> schemas() {
      return getMap("schemas", SchemaObject.class);
    }

    default JsonMap<ParameterObject> parameters() {
      return getMap("parameters", ParameterObject.class);
    }
  }

  interface OperationObject extends JsonObject {

    default String operationId() {
      return getString("operationId").string();
    }

    default String operationMethod() {
      return node().getPath().segments().get(2).substring(1);
    }

    default String operationPath() {
      String path = node().getPath().segments().get(1);
      return path.startsWith("{") && path.endsWith("}")
          ? path.substring(1, path.length() - 1)
          : path.substring(1);
    }

    default Map<String, String> x_classifiers() {
      return getMap("x-classifiers", JsonString.class).toMap(JsonString::string);
    }

    /**
     * @return the Java class name of the controller declaring the operation
     */
    default String x_class() {
      return getString("x-class").string();
    }

    default String x_entity() {
      return x_classifiers().get("entity");
    }

    default String x_group() {
      return getString("x-group").string("misc");
    }

    default String x_maturity() {
      return getString("x-maturity").string();
    }

    default String x_since() {
      return getString("x-since").string();
    }

    default List<String> x_auth() {
      JsonArray auth = getArray("x-auth");
      return auth.isUndefined() || auth.isEmpty() ? List.of() : auth.stringValues();
    }

    default List<String> tags() {
      return getArray("tags").stringValues();
    }

    default String summary() {
      return getString("summary").string();
    }

    @Language("markdown")
    default String description() {
      return getString("description").string();
    }

    default JsonList<ParameterObject> parameters() {
      return getList("parameters", ParameterObject.class);
    }

    default RequestBodyObject requestBody() {
      return get("requestBody", RequestBodyObject.class);
    }

    default JsonMap<ResponseObject> responses() {
      return getMap("responses", ResponseObject.class);
    }

    default boolean deprecated() {
      return getBoolean("deprecated").booleanValue(false);
    }

    /*
    Utility methods based on the essential properties
     */

    default List<ParameterObject> parameters(ParameterObject.In in) {
      return parameters().stream().filter(p -> p.resolve().in() == in).toList();
    }

    default Set<String> parameterNames() {
      return parameters().stream()
          .map(ParameterObject::resolve)
          .map(ParameterObject::name)
          .filter(Objects::nonNull)
          .collect(toUnmodifiableSet());
    }

    default String responseSuccessCode() {
      return responses().keys().filter(code -> code.startsWith("2")).findFirst().orElse(null);
    }

    default List<String> responseCodes() {
      return responses().keys().distinct().sorted().toList();
    }

    default List<String> responseMediaSubTypes() {
      return responses()
          .values()
          .flatMap(r -> r.content().keys())
          .map(type -> type.substring(type.indexOf('/') + 1).toLowerCase())
          .distinct()
          .sorted()
          .toList();
    }

    default List<SchemaObject> requestSchemas() {
      if (requestBody().isUndefined()) return List.of();
      return toListOfSchemas(requestBody().content());
    }

    /**
     * @return the {@link SchemaObject}s necessary to accurately describe the success response of
     *     this operation. This will be as few {@link SchemaObject}s as possible avoiding duplicates
     *     from different mime types. If more than one {@link SchemaObject} is returned the schemas
     *     have to be understood as a possible response each depending on the mime type. So they are
     *     alternatives outcomes of the operation.
     */
    default List<SchemaObject> responseSuccessSchemas() {
      if (responses().isUndefined() || responses().isEmpty()) return List.of();
      String successCode = responseSuccessCode();
      if (successCode == null) return List.of();
      return toListOfSchemas(responses().get(successCode).content());
    }

    private static List<SchemaObject> toListOfSchemas(JsonMap<MediaTypeObject> content) {
      if (content.isUndefined() || content.isEmpty()) return List.of();
      List<SchemaObject> schemas = content.values().map(MediaTypeObject::schema).toList();
      if (content.size() == 1) return schemas;
      if (MediaTypeObject.isUniform(content)) return List.of(schemas.get(0));
      return schemas;
    }
  }

  interface ParameterObject extends JsonObject {

    enum In {
      query,
      path,
      header,
      cookie
    }

    @Required
    default String name() {
      return getString("name").string();
    }

    @Required
    default In in() {
      return getString("in").parsed(In::valueOf);
    }

    @Language("markdown")
    default String description() {
      return getString("description").string();
    }

    default boolean required() {
      return getBoolean("required").booleanValue(false);
    }

    default boolean deprecated() {
      return getBoolean("deprecated").booleanValue(false);
    }

    default String x_since() {
      return getString("x-since").string();
    }

    default SchemaObject schema() {
      return get("schema", SchemaObject.class).resolve();
    }

    default ParameterObject resolve() {
      JsonString $ref = getString("$ref");
      if (!$ref.exists()) return this;
      String comp = $ref.string();
      String path = "components.parameters{" + comp.substring(24) + "}";
      return node().getRoot().lift(getAccessStore()).asObject().get(path, ParameterObject.class);
    }

    default boolean isShared() {
      String path = node().getPath().toString();
      return path.substring(0, path.lastIndexOf('.')).equals(".components.parameters");
    }

    default String getSharedName() {
      return isShared() ? node().getPath().toString().substring(24) : null;
    }

    /**
     * For a parameter the default might be defined in the schema directly or by the referenced
     * schema.
     *
     * @return the parameters default if there is any defined
     */
    default JsonValue $default() {
      JsonValue directlyDefinedDefault = get("schema.default");
      if (directlyDefinedDefault.exists()) return directlyDefinedDefault;
      return schema().$default();
    }
  }

  interface RequestBodyObject extends JsonObject {
    @Language("markdown")
    default String description() {
      return getString("description").string();
    }

    @Required
    default JsonMap<MediaTypeObject> content() {
      return getMap("content", MediaTypeObject.class);
    }

    default boolean required() {
      return getBoolean("required").booleanValue(false);
    }
  }

  interface MediaTypeObject extends JsonObject {

    default SchemaObject schema() {
      return get("schema", SchemaObject.class);
    }

    static boolean isUniform(JsonMap<MediaTypeObject> content) {
      if (content.isUndefined()) return false;
      if (content.size() == 1) return true;
      List<SchemaObject> types =
          content.values().map(MediaTypeObject::schema).map(SchemaObject::resolve).toList();
      SchemaObject type0 = types.get(0);
      if (type0.isShared())
        return types.stream()
            .allMatch(t -> t.isShared() && type0.getSharedName().equals(t.getSharedName()));
      return types.stream().allMatch(t -> t.toJson().equals(type0.toJson()));
    }
  }

  interface ResponseObject extends JsonObject {
    @Required
    @Language("markdown")
    default String description() {
      return getString("description").string();
    }

    default JsonMap<MediaTypeObject> content() {
      return getMap("content", MediaTypeObject.class);
    }

    default boolean isUniform() {
      return MediaTypeObject.isUniform(content());
    }
  }

  interface SchemaObject extends JsonObject {

    default SchemaObject resolve() {
      JsonString $ref = getString("$ref");
      if (!$ref.exists()) return this;
      String comp = $ref.string();
      String path = "components.schemas{" + comp.substring(21) + "}";
      return node().getRoot().lift(getAccessStore()).asObject().get(path, SchemaObject.class);
    }

    default boolean isRef() {
      return get("$ref").exists();
    }

    default boolean isShared() {
      if (!exists()) return false;
      String path = node().getPath().toString();
      return path.substring(0, path.lastIndexOf('.')).equals(".components.schemas");
    }

    default String getSharedName() {
      return isShared() ? node().getPath().toString().substring(20) : null;
    }

    default String x_kind() {
      JsonString kind = getString("x-kind");
      if (kind.exists()) return kind.string();
      if (get("enum").exists()) return "enum";
      String type = $type();
      if (type != null) return type;
      return "other";
    }

    default String x_since() {
      return getString("x-since").string();
    }

    /*
    From JSON schema spec...
     */

    @Validation(oneOfValues = {"string", "array", "integer", "number", "boolean", "object"})
    default String $type() {
      return getString("type").string();
    }

    default boolean isReadOnly() {
      return getBoolean("readOnly").booleanValue(false);
    }

    default boolean isAnyType() {
      return "any".equalsIgnoreCase($type());
    }

    default boolean isObjectType() {
      return "object".equalsIgnoreCase($type());
    }

    default boolean isArrayType() {
      return "array".equalsIgnoreCase($type());
    }

    default boolean isStringType() {
      return "string".equalsIgnoreCase($type());
    }

    @Language("markdown")
    default String description() {
      return getString("description").string();
    }

    /*
    string types
     */

    default String format() {
      return getString("format").string();
    }

    default Integer minLength() {
      return getNumber("minLength").integer();
    }

    default Integer maxLength() {
      return getNumber("maxLength").integer();
    }

    default String pattern() {
      return getString("pattern").string();
    }

    default JsonValue $default() {
      return get("default");
    }

    default List<String> $enum() {
      return getArray("enum").stringValues();
    }

    default boolean isEnum() {
      return has("enum");
    }

    /*
    array types
     */

    default SchemaObject items() {
      return get("items", SchemaObject.class);
    }

    /*
    object types
     */

    default List<String> required() {
      return getArray("required").stringValues();
    }

    /**
     * @return the {@link SchemaObject} of any property that is not explicitly described in {@link
     *     #properties()}
     */
    default SchemaObject additionalProperties() {
      return get("additionalProperties", SchemaObject.class);
    }

    default JsonMap<SchemaObject> properties() {
      return getMap("properties", SchemaObject.class);
    }

    /**
     * @return an object with no explicitly defined properties and where all additional properties
     *     have the same type is essentially a map with {@link String} keys and the {@link
     *     #additionalProperties()} {@link SchemaObject} values.
     */
    default boolean isMap() {
      if (!isObjectType()) return false;
      if (additionalProperties().isUndefined()) return false;
      return properties().isUndefined() || properties().isEmpty();
    }

    /**
     * @return an object which only has a single property is called a wrapper
     */
    default boolean isWrapper() {
      return isObjectType() && properties().exists() && properties().size() == 1;
    }

    /**
     * @return an object which has a header object and a content list is consider an envelope
     */
    default boolean isEnvelope() {
      if (!isObjectType()) return false;
      JsonMap<SchemaObject> properties = properties();
      if (properties.isUndefined()) return false;
      if (properties.size() != 2) return false;
      if (properties.values().noneMatch(SchemaObject::isArrayType)) return false;
      return properties
          .values()
          .anyMatch(s -> s.isObjectType() || s.isRef() && s.resolve().isObjectType());
    }

    /**
     * @return a schema is flat if it can be printed without needed multiple lines
     */
    default boolean isFlat() {
      if (isRef()) return true;
      String type = $type();
      if (type != null)
        return switch (type) {
          case "array" -> items().isFlat();
          case "object" -> isMap() && additionalProperties().isFlat();
          default -> size() == 1; // "type" is the only member
        };
      if (oneOf().exists()) return oneOf().stream().allMatch(SchemaObject::isFlat);
      if (allOf().exists()) return allOf().stream().allMatch(SchemaObject::isFlat);
      if (anyOf().exists()) return anyOf().stream().allMatch(SchemaObject::isFlat);
      if (not().exists()) return not().isFlat();
      return false; // IDK, probably not
    }

    /*
    type composition (non-concrete types)
    (they do not have a value for the "type" field!)
     */

    /**
     * @return type must match one and only one of the specified schema elements
     */
    default JsonList<SchemaObject> oneOf() {
      return getList("oneOf", SchemaObject.class);
    }

    /**
     * @return type must match all of the specified schema elements
     */
    default JsonList<SchemaObject> allOf() {
      return getList("allOf", SchemaObject.class);
    }

    /**
     * @return type must match at least one of the specified schema elements
     */
    default JsonList<SchemaObject> anyOf() {
      return getList("anyOf", SchemaObject.class);
    }

    /**
     * @return type must not match the specified schema
     */
    default SchemaObject not() {
      return get("not", SchemaObject.class);
    }
  }
}
