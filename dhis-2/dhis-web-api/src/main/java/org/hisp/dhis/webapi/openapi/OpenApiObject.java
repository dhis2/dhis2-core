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

import java.util.List;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonMap;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
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

  default JsonMap<PathItemObject> paths() {
    return getMap("paths", PathItemObject.class);
  }

  default ComponentsObject components() {
    return get("components", ComponentsObject.class);
  }

  default JsonList<TagObject> tags() {
    return getList("tags", TagObject.class);
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
      return getString("").string();
    }

    default String x_domain() {
      return getString("x-domain").string();
    }

    default String x_group() {
      return getString("x-group").string();
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
  }

  interface ParameterObject extends JsonObject {
    @Required
    default String name() {
      return getString("name").string();
    }

    @Required
    default Api.Parameter.In in() {
      return getString("in").parsed(str -> Api.Parameter.In.valueOf(str.toUpperCase()));
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

    default boolean allowEmptyValue() {
      return getBoolean("allowEmptyValue").booleanValue(false);
    }

    default ParameterObject resolve() {
      JsonString $ref = getString("$ref");
      if (!$ref.exists()) return this;
      String comp = $ref.string();
      String path = "components.parameters{" + comp.substring(24) + "}";
      return node().getRoot().lift(getAccessStore()).asObject().get(path, ParameterObject.class);
    }

    default boolean isShared() {
      return node().getPath().startsWith("$.components");
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
  }

  interface SchemaObject extends JsonObject {

    // TODO Schema resolve(); always returns the referenced schema if a $ref was used otherwise the
    // inline defined schema

    @Validation(oneOfValues = {"string", "array", "integer", "number", "boolean", "object"})
    default String $type() {
      return getString("type").string();
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

    default List<String> $enum() {
      return getArray("enum").stringValues();
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

    /*
    type composition (non concrete types)
     */

    default JsonList<SchemaObject> oneOf() {
      return getList("oneOf", SchemaObject.class);
    }
  }
}
