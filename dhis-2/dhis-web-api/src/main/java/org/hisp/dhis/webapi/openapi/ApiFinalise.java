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
package org.hisp.dhis.webapi.openapi;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.OpenApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * {@link Api} finalisation is the second step in the OpenAPI generation process.
 *
 * <p>After the {@link Api} has been analysed by looking at the controller endpoints in the first
 * step the gathered information is completed and consolidated in this second step. This is the
 * preparation to be ready to transform the {@link Api} into the OpenAPI document in the third step.
 *
 * @author Jan Bernitt
 */
@Value
@Slf4j
public class ApiFinalise {
  @Value
  @Builder(toBuilder = true)
  static class Configuration {

    /** When true, the generation fails if there is any name clash. */
    boolean failOnNameClash;

    /**
     * The character(s) used to join the prefix, like {@code Ref} or {@code UID} with the rest of
     * the type name.
     *
     * <p>For example, the {@code -} in the below examples, where simple name is what the Ref/UID
     * refers to:
     *
     * <pre>
     * Ref-SimpleName
     * UID-SimpleName
     * </pre>
     */
    String namePartDelimiter;

    // tags
    // endpoints
    // parameters
    // request bodies
    // responses
    String missingDescription;
    /*
     * .missingDescription( "[no description yet]" ) .namePartDelimiter( "_"
     * )
     */
  }

  Api api;

  Configuration config;

  public static void finaliseApi(Api api, Configuration config) {
    new ApiFinalise(api, config).finaliseApi();
  }

  /**
   * OBS! The order of steps in this is critical to produce a correct and complete {@link Api} model
   * ready for document generation.
   */
  private void finaliseApi() {
    // 1. Set and check shared unique names and create the additional schemas for Refs and UIDs
    nameSharedSchemas();
    nameSharedAdditionalSchemas();
    nameSharedParameters();

    // 2. Add description texts from markdown files to the Api model
    describeTags();
    api.getControllers().forEach(ApiFinalise::describeController);
    api.getSchemas().values().stream()
        .filter(Api.Schema::isShared)
        .forEach(ApiFinalise::describeSchema);

    // 3. Group and merge endpoints by request path and method
    groupAndMergeEndpoints();
  }

  /*
   * 1. Set and check shared unique names and create the additional schemas
   * for Refs and UIDs
   */

  /**
   * Assigns unique names to the shared schema types or fails if {@link
   * Configuration#failOnNameClash} is true.
   *
   * <p>All shared types are transferred to {@link Api.Components#getSchemas()}.
   */
  private void nameSharedSchemas() {
    Map<String, List<Map.Entry<Class<?>, Api.Schema>>> sharedSchemasByName = new HashMap<>();
    api.getSchemas().entrySet().stream()
        .filter(e -> e.getValue().isShared())
        .forEach(
            e ->
                sharedSchemasByName
                    .computeIfAbsent(
                        e.getValue().getSharedName().orElse(e.getKey().getSimpleName()),
                        name -> new ArrayList<>())
                    .add(e));

    if (config.isFailOnNameClash()) {
      checkNoSchemaNameClash(sharedSchemasByName);
    }

    sharedSchemasByName.forEach(
        (name, schemas) -> {
          schemas.get(0).getValue().getSharedName().setValue(name);
          if (schemas.size() > 1) {
            log.warn(createSchemaNameClashMessage(name, schemas));
            for (int i = 1; i < schemas.size(); i++) {
              schemas
                  .get(i)
                  .getValue()
                  .getSharedName()
                  .setValue(name + config.getNamePartDelimiter() + i);
            }
          }
        });

    sharedSchemasByName.values().stream()
        .flatMap(list -> list.stream().map(Map.Entry::getValue))
        .forEach(
            schema ->
                api.getComponents().getSchemas().put(schema.getSharedName().getValue(), schema));
  }

  private record SchemaKey(Api.Schema.Type type, Class<?> of) {}

  /**
   * Traverses the {@link Api} document for all {@link org.hisp.dhis.webapi.openapi.Api.Schema}s to
   * collect the additional schemas.
   */
  private void nameSharedAdditionalSchemas() {
    Map<SchemaKey, Api.Schema> to = new HashMap<>();
    addAdditionalSchemas(to, api.getSchemas().values().stream());
    addAdditionalSchemas(
        to,
        api.getComponents().getParameters().values().stream()
            .flatMap(List::stream)
            .map(Api.Parameter::getType));
    api.getControllers().stream()
        .flatMap(c -> c.getEndpoints().stream())
        .forEach(
            endpoint -> {
              if (endpoint.getRequestBody().isPresent()) {
                addAdditionalSchemas(
                    to, endpoint.getRequestBody().getValue().getConsumes().values().stream());
              }
              addAdditionalSchemas(
                  to, endpoint.getParameters().values().stream().map(Api.Parameter::getType));
              addAdditionalSchemas(
                  to,
                  endpoint.getResponses().values().stream()
                      .flatMap(response -> response.getContent().values().stream()));
            });

    Map<String, Api.Schema> additionalSchemas = api.getComponents().getAdditionalSchemas();
    to.values().forEach(schema -> additionalSchemas.put(getAdditionalTypeName(schema), schema));
  }

  private static void addAdditionalSchemas(Map<SchemaKey, Api.Schema> to, Stream<Api.Schema> from) {
    from.forEach(
        schema -> {
          if (schema.getType().isSharedAsAdditionalSchema()) {
            SchemaKey key = new SchemaKey(schema.getType(), schema.getRawType());
            // OBS! This cannot use putIfAbsent since computing the value can cause more puts
            if (!to.containsKey(key)) {
              to.put(key, schema);
              addAdditionalSchemas(to, schema.getProperties().stream().map(Api.Property::getType));
            }
          }
        });
  }

  /**
   * Parameters generally use the shared name of the parameter defining class combined with the
   * field name of the parameter. This fully qualified shared name must be unique. This still allows
   * to have parameters from different classes of the same name to exist without a conflict as long
   * as none of the parameters of these classes have the same name.
   *
   * <p>Therefore, the all prameters are first grouped by their effective shared name of the
   * parameter class. On second level the parameters are grouped by their defining class.
   */
  private void nameSharedParameters() {
    Map<String, List<Map.Entry<Class<?>, List<Api.Parameter>>>> sharedParametersByName =
        new HashMap<>();
    api.getComponents()
        .getParameters()
        .entrySet()
        .forEach(
            e ->
                sharedParametersByName
                    .computeIfAbsent(
                        e.getValue().get(0).getSharedName().orElse(e.getKey().getSimpleName()),
                        key -> new ArrayList<>())
                    .add(e));

    if (config.isFailOnNameClash()) {
      checkNoParameterNameClash(sharedParametersByName);
    }

    sharedParametersByName.forEach(
        (name, params) -> {
          if (params.size() == 1) {
            // only one class uses the name => always fine
            params.get(0).getValue().forEach(p -> p.getSharedName().setValue(name));
          } else {
            // there might be a clash if a parameter name occurs in more than one
            Set<String> usedNames = new HashSet<>();
            params.stream()
                .flatMap(e -> e.getValue().stream())
                .forEach(
                    p -> {
                      if (usedNames.contains(p.getName())) {
                        p.getSharedName()
                            .setValue(name + config.getNamePartDelimiter() + usedNames.size());
                      } else {
                        usedNames.add(p.getName());
                        p.getSharedName().setValue(name);
                      }
                    });
          }
        });
  }

  /** Any list with more than one entry is a clash. */
  private void checkNoSchemaNameClash(
      Map<String, List<Map.Entry<Class<?>, Api.Schema>>> schemasBySharedName) {
    schemasBySharedName.forEach(
        (name, schemas) -> {
          if (schemas.size() > 1)
            throw new IllegalStateException(createSchemaNameClashMessage(name, schemas));
        });
  }

  /**
   * Parameter types may use the same shared name as long as they do not have the same parameter
   * name. If so this is a clash.
   */
  private void checkNoParameterNameClash(
      Map<String, List<Map.Entry<Class<?>, List<Api.Parameter>>>> paramsBySharedName) {
    paramsBySharedName.forEach(
        (name, params) -> {
          if (params.size() > 1) {
            List<Api.Parameter> paramsInNamespace =
                params.stream().flatMap(e -> e.getValue().stream()).toList();
            Set<String> names =
                paramsInNamespace.stream().map(Api.Parameter::getName).collect(toSet());
            if (paramsInNamespace.size() > names.size()) {
              throw new IllegalStateException(createParamNameClashMessage(name, params));
            }
          }
        });
  }

  private static String createParamNameClashMessage(
      String name, List<? extends Map.Entry<Class<?>, ?>> schemas) {
    return createNameClashMessage(
        name,
        schemas,
        "More than one parameter type uses the shared name `%s` and contains at least one parameter of the same name: \n\t- %s");
  }

  private static String createSchemaNameClashMessage(
      String name, List<? extends Map.Entry<Class<?>, ?>> schemas) {
    return createNameClashMessage(
        name, schemas, "More than one schema type uses the shared name `%s`: \n\t- %s");
  }

  private static String createNameClashMessage(
      String name, List<? extends Map.Entry<Class<?>, ?>> usages, String template) {
    return format(
        template,
        name,
        usages.stream()
            .map(Map.Entry::getKey)
            .map(Class::getCanonicalName)
            .collect(joining("\n\t- ")));
  }

  private static final Pattern VALID_NAME_INFIX = Pattern.compile("^[-_a-zA-Z0-9.]*$");

  private String getAdditionalTypeName(Api.Schema schema) {
    Class<?> of = schema.getRawType();
    Api.Schema.Type type = schema.getType();
    Map<Api.Schema.Type, String> prefixes =
        Map.of(
            Api.Schema.Type.REF, "Ref",
            Api.Schema.Type.UID, "UID",
            Api.Schema.Type.ENUM, ((Class<?>) schema.getSource()).getSimpleName());
    String prefix = prefixes.get(type);
    Api.Schema ofType = api.getSchemas().get(of);
    // why do types exist which do not have the target of type?
    // OPEN do we need to add a schema that is null here?
    if (ofType == null) {
      log.warn("No type for schema: " + schema);
    }
    return prefix
        + config.getNamePartDelimiter()
        + (ofType == null ? of.getSimpleName() : ofType.getSharedName().getValue());
  }

  /*
   * 2. Add description texts from markdown files to the Api model
   */

  private void describeTags() {
    Descriptions tags = Descriptions.of(OpenApi.Tags.class);
    api.getUsedTags()
        .forEach(
            name -> {
              Api.Tag tag = new Api.Tag(name);
              tag.getDescription().setValue(tags.get(name + ".description"));
              tag.getExternalDocsUrl().setValue(tags.get(name + ".externalDocs.url"));
              tag.getExternalDocsDescription()
                  .setValue(tags.get(name + ".externalDocs.description"));
              api.getTags().put(name, tag);
            });
  }

  private static void describeController(Api.Controller controller) {
    Descriptions descriptions = Descriptions.of(controller.getSource());

    controller
        .getEndpoints()
        .forEach(
            endpoint -> {
              String name = endpoint.getSource().getName();
              UnaryOperator<String> subst =
                  desc -> desc.replace("{entityType}", endpoint.getEntityTypeName());
              endpoint
                  .getDescription()
                  .setValue(descriptions.get(subst, format("%s.description", name)));
              endpoint
                  .getParameters()
                  .values()
                  .forEach(
                      parameter -> {
                        List<String> keys =
                            parameter.isShared()
                                ? List.of(
                                    format(
                                        "%s.parameter.%s.description",
                                        name, parameter.getFullName()),
                                    format(
                                        "%s.parameter.%s.description",
                                        name, getSharedNameForDeclaringType(parameter)),
                                    format("*.parameter.%s.description", parameter.getFullName()),
                                    format(
                                        "*.parameter.%s.description",
                                        getSharedNameForDeclaringType(parameter)))
                                : List.of(
                                    format(
                                        "%s.parameter.%s.description", name, parameter.getName()),
                                    format("*.parameter.%s.description", parameter.getName()));
                        parameter.getDescription().setValue(descriptions.get(subst, keys));
                      });
              if (endpoint.getRequestBody().isPresent()) {
                Api.Maybe<String> description =
                    endpoint.getRequestBody().getValue().getDescription();
                String key = format("%s.request.description", name);
                description.setValue(descriptions.get(subst, key));
              }
              endpoint
                  .getResponses()
                  .values()
                  .forEach(
                      response -> {
                        int statusCode = response.getStatus().value();
                        response
                            .getDescription()
                            .setValue(
                                descriptions.get(
                                    subst,
                                    List.of(
                                        format("%s.response.%d.description", name, statusCode),
                                        format("*.response.%d.description", statusCode))));
                        Api.Schema schema = response.getContent().get(MediaType.APPLICATION_JSON);
                        if (schema != null && !schema.isShared()) {
                          schema
                              .getProperties()
                              .forEach(
                                  property ->
                                      property
                                          .getDescription()
                                          .setValue(
                                              descriptions.get(
                                                  subst,
                                                  List.of(
                                                      format(
                                                          "%s.response.%d.%s.description",
                                                          name, statusCode, property.getName()),
                                                      format(
                                                          "*.response.%d.%s.description",
                                                          statusCode, property.getName())))));
                        }
                      });
            });
  }

  private static void describeSchema(Api.Schema schema) {
    Descriptions descriptions = Descriptions.of(schema.getRawType());
    String sharedName = schema.getSharedName().orElse("*");
    schema
        .getProperties()
        .forEach(
            property ->
                property
                    .getDescription()
                    .setValue(
                        descriptions.get(
                            List.of(
                                format("%s.schema.%s.description", sharedName, property.getName()),
                                format("*.schema.%s.description", property.getName()),
                                format("%s.description", property.getName())))));
  }

  private static String getSharedNameForDeclaringType(Api.Parameter p) {
    Class<?> declaringClass = ((Member) p.getSource()).getDeclaringClass();
    return declaringClass.getSimpleName() + "." + p.getName();
  }

  /*
   * 3. Group and merge endpoints by request path and method
   */

  private void groupAndMergeEndpoints() {
    groupEndpointsByAbsolutePath()
        .forEach(
            (path, endpoints) -> {
              EnumMap<RequestMethod, Api.Endpoint> endpointByMethod =
                  new EnumMap<>(RequestMethod.class);
              endpoints.forEach(
                  e ->
                      e.getMethods()
                          .forEach(
                              method ->
                                  endpointByMethod.compute(
                                      method, (k, v) -> mergeEndpoints(v, e, method))));
              api.getEndpoints().put(path, endpointByMethod);
            });
  }

  private Map<String, List<Api.Endpoint>> groupEndpointsByAbsolutePath() {
    // OBS! We use a TreeMap to also get alphabetical order/grouping
    Map<String, List<Api.Endpoint>> endpointsByAbsolutePath = new TreeMap<>();
    for (Api.Controller c : api.getControllers()) {
      if (c.getPaths().isEmpty()) c.getPaths().add("");
      for (String cPath : c.getPaths()) {
        for (Api.Endpoint e : c.getEndpoints()) {
          for (String ePath : e.getPaths()) {
            String absolutePath = cPath + ePath;
            if (absolutePath.isEmpty()) {
              absolutePath = "/";
            }
            endpointsByAbsolutePath.computeIfAbsent(absolutePath, key -> new ArrayList<>()).add(e);
          }
        }
      }
    }
    return endpointsByAbsolutePath;
  }

  /**
   * Merges two endpoints for the same path and request method.
   *
   * <p>A heuristic is used to decide which of the two endpoints is preserved fully.
   */
  private static Api.Endpoint mergeEndpoints(Api.Endpoint a, Api.Endpoint b, RequestMethod method) {
    if (a == null || a.isDeprecated()) return b;
    if (b == null || b.isDeprecated()) return a;
    Api.Endpoint primary = a;
    Api.Endpoint secondary = b;
    if (!a.isSynthetic() && !b.isSynthetic()) {
      long countA = countJsonResponses(a);
      long countB = countJsonResponses(b);
      if (countB > countA) {
        primary = b;
        secondary = a;
      }
    }
    Api.Endpoint merged =
        new Api.Endpoint(
            primary.getIn(),
            null,
            primary.getEntityType(),
            primary.getName() + "+" + secondary.getName(),
            primary.getDeprecated());
    merged.getTags().addAll(primary.getTags());
    merged.getTags().addAll(secondary.getTags());
    merged
        .getDescription()
        .setValue(primary.getDescription().orElse(secondary.getDescription().getValue()));
    merged.getMethods().add(method);
    merged.getRequestBody().setValue(primary.getRequestBody().getValue());
    if (merged.getRequestBody().isPresent() && secondary.getRequestBody().isPresent()) {
      secondary
          .getRequestBody()
          .getValue()
          .getConsumes()
          .forEach((k, v) -> merged.getRequestBody().getValue().getConsumes().putIfAbsent(k, v));
    }
    merged.getParameters().putAll(primary.getParameters());
    Map<HttpStatus, Api.Response> responses = merged.getResponses();
    responses.putAll(primary.getResponses());
    for (Api.Response source : secondary.getResponses().values()) {
      if (!responses.containsKey(source.getStatus())) {
        responses.put(source.getStatus(), source);
      } else {
        Api.Response target = responses.get(source.getStatus());
        source.getContent().entrySet().stream()
            .filter(e -> !target.getContent().containsKey(e.getKey()))
            .forEach(e -> target.getContent().put(e.getKey(), e.getValue()));
      }
    }
    return merged;
  }

  private static long countJsonResponses(Api.Endpoint a) {
    return a.getResponses().values().stream()
        .filter(r -> r.getContent().containsKey(MediaType.APPLICATION_JSON))
        .count();
  }
}
