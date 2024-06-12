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
 * @implNote this type is intentionally default visible to not pollute the global namespace with the
 *     type names as they have fairly common names.
 */
interface OpenApi extends JsonObject {

  @Required
  default String openapi() {
    return getString("openapi").string();
  }

  @Required
  default Info info() {
    return get("info", Info.class);
  }

  default JsonList<Server> servers() {
    return getList("servers", Server.class);
  }

  default JsonMap<PathItem> paths() {
    return getMap("paths", PathItem.class);
  }

  default Components components() {
    return get("components", Components.class);
  }

  default JsonList<Tag> tags() {
    return getList("tags", Tag.class);
  }

  interface Info extends JsonObject {
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

  interface Tag extends JsonObject {
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

  interface Server extends JsonObject {
    @Required
    default String url() {
      return getString("url").string();
    }

    @Language("markdown")
    default String description() {
      return getString("description").string();
    }
  }

  interface PathItem extends JsonObject {

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

    default Operation get() {
      return get("get", Operation.class);
    }

    default Operation put() {
      return get("put", Operation.class);
    }

    default Operation post() {
      return get("post", Operation.class);
    }

    default Operation delete() {
      return get("delete", Operation.class);
    }

    default Operation options() {
      return get("options", Operation.class);
    }

    default Operation head() {
      return get("head", Operation.class);
    }

    default Operation patch() {
      return get("patch", Operation.class);
    }

    default Operation trace() {
      return get("trace", Operation.class);
    }
  }

  interface Components extends JsonObject {

    default JsonMap<Schema> schemas() {
      return getMap("schemas", Schema.class);
    }

    default JsonMap<Parameter> parameters() {
      return getMap("parameters", Parameter.class);
    }
  }

  interface Operation extends JsonObject {

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

    default String operationId() {
      return getString("").string();
    }

    default JsonList<Parameter> parameters() {
      return getList("parameters", Parameter.class);
    }

    default RequestBody requestBody() {
      return get("requestBody", RequestBody.class);
    }

    default JsonMap<Response> responses() {
      return getMap("responses", Response.class);
    }

    default boolean deprecated() {
      return getBoolean("deprecated").booleanValue(false);
    }
  }

  interface Parameter extends JsonObject {
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
  }

  interface RequestBody extends JsonObject {
    @Language("markdown")
    default String description() {
      return getString("description").string();
    }

    @Required
    default JsonMap<MediaType> content() {
      return getMap("content", MediaType.class);
    }

    default boolean required() {
      return getBoolean("required").booleanValue(false);
    }
  }

  interface MediaType extends JsonObject {
    default Schema schema() {
      return get("schema", Schema.class);
    }
  }

  interface Response extends JsonObject {
    @Required
    @Language("markdown")
    default String description() {
      return getString("description").string();
    }

    default JsonMap<MediaType> content() {
      return getMap("content", MediaType.class);
    }
  }

  interface Schema extends JsonObject {

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

    default Schema items() {
      return get("items", Schema.class);
    }

    /*
    object types
     */

    default List<String> required() {
      return getArray("required").stringValues();
    }

    /**
     * @return the {@link Schema} of any property that is not explicitly described in {@link
     *     #properties()}
     */
    default Schema additionalProperties() {
      return get("additionalProperties", Schema.class);
    }

    default JsonMap<Schema> properties() {
      return getMap("properties", Schema.class);
    }

    /*
    type composition (non concrete types)
     */

    default JsonList<Schema> oneOf() {
      return getList("oneOf", Schema.class);
    }
  }
}
