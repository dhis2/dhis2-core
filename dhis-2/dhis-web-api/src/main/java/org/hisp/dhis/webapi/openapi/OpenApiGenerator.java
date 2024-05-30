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
package org.hisp.dhis.webapi.openapi;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparing;
import static org.hisp.dhis.webapi.openapi.Api.Schema.Direction.IN;
import static org.hisp.dhis.webapi.openapi.Api.Schema.Direction.OUT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.webapi.openapi.Api.Schema.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Generates a <a href= "https://github.com/OAI/OpenAPI-Specification/blob/main/versions/">OpenAPI
 * 3.x</a> version JSON document from an {@link Api} model.
 *
 * <p>The generation offers a dozen configuration options which concern both the {@link
 * org.hisp.dhis.webapi.openapi.JsonGenerator.Format} of the generated JSON as well as the semantic
 * {@link Info} content.
 *
 * <p>Alongside the input {@link Api} model there is a pool of known {@link DirectType}s. This is
 * the core translation of primitives, wrapper, {@link String}s but also used as a "correction" for
 * seemingly complex types which in their serialized form become simple ones, like a period that
 * uses its ISO string form.
 *
 * @author Jan Bernit
 */
@Slf4j
public class OpenApiGenerator extends JsonGenerator {
  @Value
  @Builder(toBuilder = true)
  public static class Info {
    public static final Info DEFAULT =
        Info.builder()
            .title("DHIS2 API")
            .version("2.42")
            .serverUrl("https://play.dhis2.org/dev/api")
            .licenseName("BSD 3-Clause \"New\" or \"Revised\" License")
            .licenseUrl("https://raw.githubusercontent.com/dhis2/dhis2-core/master/LICENSE")
            .build();

    String title;
    String version;
    String serverUrl;
    String licenseName;
    String licenseUrl;
    String contactName;
    String contactUrl;
    String contactEmail;
  }

  public static String generateJson(Api api, String serverUrl) {
    return generateJson(
        api, Format.PRETTY_PRINT, Info.DEFAULT.toBuilder().serverUrl(serverUrl).build());
  }

  public static String generateJson(Api api, Format format, Info info) {
    return generate(api, format, Language.JSON, info);
  }

  public static String generateYaml(Api api, String serverUrl) {
    return generateYaml(
        api, Format.PRETTY_PRINT, Info.DEFAULT.toBuilder().serverUrl(serverUrl).build());
  }

  public static String generateYaml(Api api, Format format, Info info) {
    return generate(api, format, Language.YAML, info);
  }

  private static String generate(Api api, Format format, Language language, Info info) {
    int endpoints = 0;
    for (Api.Controller c : api.getControllers()) endpoints += c.getEndpoints().size();
    int capacity = endpoints * 256 + api.getSchemas().size() * 512;
    OpenApiGenerator gen =
        new OpenApiGenerator(
            api,
            language.getAdjustFormat().apply(format),
            language,
            info,
            new StringBuilder(capacity));
    gen.generateDocument();
    return gen.toString();
  }

  /**
   * By default, if no description is provided no such element should be added to the resulting
   * document therefore the default is {@code null}.
   */
  private static final String NO_DESCRIPTION = null;

  private final Api api;
  private final Info info;
  private final Map<String, List<Api.Endpoint>> endpointsByBaseOperationId = new HashMap<>();

  /**
   * Accumulates the schema references used during generation of the OpenAPI {@code paths} part.
   * This is later used to ensure no schema is added to the output that isn't actually referenced.
   */
  private final Set<String> pathSchemaRefs = new HashSet<>();

  private OpenApiGenerator(
      Api api, Format format, Language language, Info info, StringBuilder out) {
    super(out, format, language);
    this.api = api;
    this.info = info;
  }

  private void generateDocument() {
    addRootObject(
        () -> {
          addStringMember("openapi", "3.0.0");
          addObjectMember(
              "info",
              () -> {
                addStringMember("title", info.title);
                addStringMember("version", info.version);
                addObjectMember(
                    "license",
                    () -> {
                      addStringMember("name", info.licenseName);
                      addStringMember("url", info.licenseUrl);
                    });
                addObjectMember(
                    "contact",
                    () -> {
                      addStringMember("name", info.contactName);
                      addStringMember("url", info.contactUrl);
                      addStringMember("email", info.contactEmail);
                    });
              });
          addArrayMember(
              "tags",
              api.getTags().values(),
              tag ->
                  addObjectMember(
                      null,
                      () -> {
                        addStringMember("name", tag.getName());
                        addStringMultilineMember(
                            "description", tag.getDescription().orElse(NO_DESCRIPTION));
                        addObjectMember(
                            "externalDocs",
                            tag.getExternalDocsUrl().isPresent(),
                            () -> {
                              addStringMember("url", tag.getExternalDocsUrl().getValue());
                              addStringMultilineMember(
                                  "description", tag.getExternalDocsDescription().getValue());
                            });
                      }));
          addArrayMember(
              "servers", () -> addObjectMember(null, () -> addStringMember("url", info.serverUrl)));
          addArrayMember(
              "security",
              () ->
                  addObjectMember(
                      null,
                      () -> addArrayMember("basicAuth", () -> addArrayMember(null, List.of()))));
          addObjectMember("paths", this::generatePaths);
          addObjectMember(
              "components",
              () -> {
                addObjectMember("securitySchemes", this::generateSecuritySchemes);
                addObjectMember("parameters", this::generateSharedParameters);
                addObjectMember("schemas", this::generateSharedSchemas);
              });
        });
    log.info(
        format(
            "OpenAPI document generated for %d controllers with %d named schemas",
            api.getControllers().size(), api.getSchemas().size()));
  }

  private void generatePaths() {
    api.getEndpoints().forEach(this::generatePath);
  }

  private void generatePath(String path, Map<RequestMethod, Api.Endpoint> endpoints) {
    addObjectMember(path, () -> endpoints.forEach(this::generatePathMethod));
  }

  private void generatePathMethod(RequestMethod method, Api.Endpoint endpoint) {
    Set<String> tags = Set.of(endpoint.getGroup().tag());
    addObjectMember(
        method.name().toLowerCase(),
        () -> {
          addTrueMember("deprecated", endpoint.getDeprecated());
          addStringMultilineMember("description", endpoint.getDescription().orElse(NO_DESCRIPTION));
          addStringMember("operationId", getUniqueOperationId(endpoint));
          addInlineArrayMember("tags", List.copyOf(tags));
          addArrayMember(
              "parameters", endpoint.getParameters().values(), this::generateParameterOrRef);
          if (endpoint.getRequestBody().isPresent()) {
            addObjectMember(
                "requestBody", () -> generateRequestBody(endpoint.getRequestBody().getValue()));
          }
          addObjectMember("responses", endpoint.getResponses().values(), this::generateResponse);
        });
  }

  private void generateSharedParameters() {
    Map<String, Api.Parameter> paramBySharedName = new TreeMap<>();
    for (List<Api.Parameter> params : api.getComponents().getParameters().values())
      params.forEach(p -> paramBySharedName.put(p.getFullName(), p));
    // use map to sort parameter by name
    paramBySharedName.forEach(this::generateParameter);
  }

  private void generateParameterOrRef(Api.Parameter parameter) {
    if (parameter.isShared()) {
      // shared parameter usage: => reference object
      addObjectMember(
          null,
          () -> addStringMember("$ref", "#/components/parameters/" + parameter.getFullName()));
    } else {
      generateParameter(null, parameter);
    }
  }

  private void generateParameter(String name, Api.Parameter parameter) {
    // parameter definition (both shared and non-shared):
    addObjectMember(
        name,
        () -> {
          addStringMember("name", parameter.getName());
          addStringMember("in", parameter.getIn().name().toLowerCase());
          addStringMultilineMember(
              "description", parameter.getDescription().orElse(NO_DESCRIPTION));
          addTrueMember("required", parameter.isRequired());
          addTrueMember("deprecated", parameter.getDeprecated());
          String defaultValue = parameter.getDefaultValue().orElse(null);
          addObjectMember(
              "schema", () -> generateSchemaOrRef(parameter.getType(), IN, defaultValue));
        });
  }

  private void generateRequestBody(Api.RequestBody requestBody) {
    addStringMultilineMember("description", requestBody.getDescription().orElse(NO_DESCRIPTION));
    addTrueMember("required", requestBody.isRequired());
    addObjectMember(
        "content",
        () ->
            requestBody
                .getConsumes()
                .forEach(
                    (key, value) ->
                        addObjectMember(
                            key.toString(),
                            () ->
                                addObjectMember("schema", () -> generateSchemaOrRef(value, IN)))));
  }

  private void generateResponse(Api.Response response) {
    addObjectMember(
        String.valueOf(response.getStatus().value()),
        () -> {
          // note: for a response the description is required, therefore the default is ?
          addStringMultilineMember("description", response.getDescription().orElse("?"));
          addObjectMember(
              "headers",
              response.getHeaders().values(),
              header ->
                  addObjectMember(
                      header.getName(),
                      () -> {
                        addStringMultilineMember("description", header.getDescription());
                        addObjectMember("schema", () -> generateSchema(header.getType(), OUT));
                      }));
          boolean hasContent =
              !response.getContent().isEmpty() && response.getStatus() != HttpStatus.NO_CONTENT;
          addObjectMember(
              "content",
              hasContent,
              () ->
                  response
                      .getContent()
                      .forEach(
                          (produces, body) ->
                              addObjectMember(
                                  produces.toString(),
                                  () ->
                                      addObjectMember(
                                          "schema", () -> generateSchemaOrRef(body, OUT)))));
        });
  }

  private void generateSecuritySchemes() {
    addObjectMember(
        "basicAuth",
        () -> {
          addStringMember("type", "http");
          addStringMember("scheme", "basic");
        });
  }

  private void generateSharedSchemas() {
    // remove schemas that are not referenced by any of the schemas used so far
    retainUsedSchemasOnly();

    if (api.getDebug().orElse(false)) OpenApiComponentsRefs.print(api, System.out);

    // now only the actually used schemas remain in component/schemas
    api.getComponents().getSchemas().values().stream()
        .sorted(comparing(Api.Schema::getType).thenComparing(s -> s.getSharedName().getValue()))
        .forEach(
            schema -> {
              String name = schema.getSharedName().getValue();
              addObjectMember(
                  name, () -> generateSchema(schema, schema.getDirection().orElse(OUT)));
            });
  }

  private void generateSchemaOrRef(Api.Schema schema, Direction direction) {
    generateSchemaOrRef(schema, direction, null);
  }

  private void generateSchemaOrRef(Api.Schema schema, Direction direction, String defaultValue) {
    if (schema == null) return;
    if (direction == IN) schema = schema.getInput().orElse(schema);
    if (schema.getSharedName().isPresent()) {
      String name = schema.getSharedName().getValue();
      pathSchemaRefs.add(name);
      addStringMember("$ref", "#/components/schemas/" + name);
      if (defaultValue != null && schema.getType() == Api.Schema.Type.ENUM)
        addDefaultMember(schema.getRawType(), defaultValue, "string");
    } else {
      generateSchema(schema, direction, defaultValue);
    }
  }

  private void generateSchema(Api.Schema schema, Direction direction) {
    generateSchema(schema, direction, null);
  }

  private void generateSchema(Api.Schema schema, Direction direction, String defaultValue) {
    if (direction == IN) schema = schema.getInput().orElse(schema);
    Class<?> type = schema.getRawType();
    DirectType directType = DirectType.of(type);
    if (directType != null) {
      Class<?> source = directType.source();
      Collection<DirectType.SimpleType> oneOf = directType.oneOf().values();
      if (oneOf.size() == 1) {
        generateSimpleTypeSchema(source, oneOf.iterator().next(), defaultValue);
      } else {
        addArrayMember(
            "oneOf",
            () ->
                oneOf.forEach(
                    t -> addObjectMember(null, () -> generateSimpleTypeSchema(source, t, null))));
      }
      return;
    }
    Api.Schema.Type schemaType = schema.getType();
    if (schemaType == Api.Schema.Type.UID) {
      generateUidSchema(schema);
      return;
    }
    if (schemaType == Api.Schema.Type.UNSUPPORTED) {
      addStringMultilineMember(
          "description",
          "The exact type is unknown.  \n(Java type was: `"
              + schema.getSource().getTypeName()
              + "`)");
      return;
    }
    if (schemaType == Api.Schema.Type.ONE_OF) {
      addArrayMember(
          "oneOf",
          schema.getProperties(),
          property ->
              addObjectMember(null, () -> generateSchemaOrRef(property.getType(), direction)));
      return;
    }
    if (schemaType == Api.Schema.Type.ENUM) {
      addStringMember("type", "string");
      if (defaultValue != null) addStringMember("default", defaultValue);
      addInlineArrayMember("enum", schema.getValues());
      return;
    }
    if (type.isEnum()) {
      addStringMember("type", "string");
      if (defaultValue != null) addStringMember("default", defaultValue);
      addInlineArrayMember(
          "enum", stream(type.getEnumConstants()).map(e -> ((Enum<?>) e).name()).toList());
      return;
    }
    if (type.isArray() || schemaType == Api.Schema.Type.ARRAY) {
      Api.Schema elements =
          schema.getProperties().isEmpty()
              ? api.getSchemas().get(type.getComponentType())
              : schema.getElementType();
      addStringMember("type", "array");
      addObjectMember("items", () -> generateSchemaOrRef(elements, direction));
      return;
    }
    // best guess: it is an object type
    addStringMember("type", "object");
    if (!schema.getProperties().isEmpty()) {
      if (Map.class.isAssignableFrom(type)) {
        Api.Property key = schema.getProperties().get(0);
        Api.Property value = schema.getProperties().get(1);
        addObjectMember(
            "additionalProperties", () -> generateSchemaOrRef(value.getType(), direction));
        if (key.getType().getRawType() != String.class)
          addStringMultilineMember(
              "description", "keys are " + schema.getProperties().get(0).getType().getRawType());
        return;
      }
      if (!schema.getRequiredProperties().isEmpty())
        addInlineArrayMember("required", schema.getRequiredProperties());
      addObjectMember(
          "properties",
          schema.getProperties(),
          property ->
              addObjectMember(
                  property.getName(),
                  () -> {
                    generateSchemaOrRef(property.getType(), direction);
                    addStringMember(
                        "description", property.getDescription().orElse(NO_DESCRIPTION));
                  }));
    } else {
      addStringMultilineMember(
          "description", "The actual type is unknown.  \n(Java type was: `" + type + "`)");
      if (type != Object.class) {
        log.warn(schema + " " + schema.getSource());
      }
    }
  }

  private void generateSimpleTypeSchema(
      Class<?> source, DirectType.SimpleType simpleType, String defaultValue) {
    String type = simpleType.type();
    addStringMember("type", type);
    if ("array".equals(type)) {
      addObjectMember("items", () -> {});
    }
    addStringMember("format", simpleType.format());
    addNumberMember("minLength", simpleType.minLength());
    addNumberMember("maxLength", simpleType.maxLength());
    addStringMember("pattern", simpleType.pattern());
    addDefaultMember(source, defaultValue, type);
    if (!simpleType.enums().isEmpty()) {
      addInlineArrayMember("enum", simpleType.enums());
    }
    addStringMultilineMember("description", simpleType.description());
  }

  private void addDefaultMember(Class<?> source, String defaultValue, String type) {
    if (defaultValue != null) {
      switch (type) {
        case "string" -> addStringMember("default", defaultValue);
        case "integer" -> addNumberMember("default", parseInt(defaultValue));
        case "number" -> addNumberMember("default", parseDouble(defaultValue));
        case "boolean" -> addBooleanMember("default", parseBoolean(defaultValue));
        default ->
            log.warn(
                "Unsupported default value provided for type %s of %s: %s"
                    .formatted(type, source.getSimpleName(), defaultValue));
      }
    }
  }

  private void generateUidSchema(Api.Schema schema) {
    addStringMember("type", "string");
    addStringMember("format", "uid");
    addStringMember("pattern", "^[0-9a-zA-Z]{11}$");
    addNumberMember("minLength", 11);
    addNumberMember("maxLength", 11);
    addStringMultilineMember("example", generateUid(schema.getRawType()));
    if (schema.getType() == Api.Schema.Type.UID) {
      addTypeDescriptionMember(schema.getRawType(), "A UID for an %s object  \n(Java name `%s`)");
    }
  }

  private void addTypeDescriptionMember(Class<?> type, String template) {
    String name = "any type of object";
    if (type != BaseIdentifiableObject.class && type != IdentifiableObject.class) {
      Api.Schema schema = api.getSchemas().get(type);
      name = schema == null ? type.getSimpleName() : schema.getSharedName().getValue();
    }
    addStringMultilineMember("description", String.format(template, name, type.getTypeName()));
  }

  /*
   * Open API document generation helpers
   */

  private String getUniqueOperationId(Api.Endpoint endpoint) {
    String baseOperationId = endpoint.getIn().getName() + "." + endpoint.getName();
    List<Api.Endpoint> endpoints =
        endpointsByBaseOperationId.computeIfAbsent(baseOperationId, key -> new ArrayList<>());
    endpoints.add(endpoint);
    return endpoints.size() == 1 ? baseOperationId : baseOperationId + endpoints.size();
  }

  /**
   * Generates an 11 character UID based on the target type. This is so each UID type gets its
   * unique but stable example.
   */
  private static String generateUid(Class<?> fromType) {
    char[] chars = CodeGenerator.ALPHANUMERIC_CHARS.toCharArray();
    String key = fromType.getSimpleName();
    key = key.repeat((11 / key.length()) + 1);
    StringBuilder uid = new StringBuilder(11);
    int offset = fromType.getSimpleName().length();
    int letters = 0;
    for (int i = 0; i < 11; i++) {
      int index = key.charAt(i) + offset;
      char c = letters >= 2 ? (char) ('0' + (offset % 10)) : chars[index % chars.length];
      letters = Character.isDigit(c) ? 0 : letters + 1;
      uid.append(c);
      // this is just to get more realistic character distribution
      // 13 because it is about half the alphabet
      offset += 13;
    }
    return uid.toString();
  }

  /**
   * When this is called the schemas directly referenced by and of the endpoints rendered in the
   * {@code paths} part of the document have been collected in {@link #pathSchemaRefs}. Now we add
   * all the named schemas which are referenced by these schemas to the set and then remove all
   * named schemas that are not referenced as they are not needed in the document.
   */
  private void retainUsedSchemasOnly() {
    // this needs a different set to pathSchemaRefs since we have not yet expanded those collected
    // in that set
    Set<String> expanded = new HashSet<>();
    // needs a copy as we modify the iterated collection in the forEach
    Set.copyOf(pathSchemaRefs).forEach(name -> addPathSchemaRefs(name, expanded));
    // now remove those schemas that are not referenced
    api.getComponents().getSchemas().keySet().removeIf(name -> !pathSchemaRefs.contains(name));
  }

  private void addPathSchemaRefs(String name, Set<String> expanded) {
    if (expanded.contains(name)) return; // done that
    pathSchemaRefs.add(name);
    expanded.add(name);
    Api.Schema schema = api.getComponents().getSchemas().get(name);
    schema.getProperties().forEach(p -> addPathSchemaRefs(p.getType(), expanded));
  }

  private void addPathSchemaRefs(Api.Schema schema, Set<String> expanded) {
    if (schema.isShared()) {
      String name = schema.getSharedName().getValue();
      addPathSchemaRefs(name, expanded);
      if (expanded.contains(name)) return;
    }
    schema.getProperties().forEach(p -> addPathSchemaRefs(p.getType(), expanded));
  }
}
