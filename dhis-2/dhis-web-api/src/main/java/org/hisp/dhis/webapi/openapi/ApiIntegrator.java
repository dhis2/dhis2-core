/*
 * Copyright (c) 2004-2023, University of Oslo
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

import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.webapi.openapi.Api.Endpoint;
import org.hisp.dhis.webapi.openapi.Api.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * {@link Api} integration is the second step in the OpenAPI generation process. In simple terms it
 * is an {@link Api} model to an enriched {@link Api} model transformation that uses in place
 * mutation of the existing {@link Api} model. In contrast to the 1st step which is mostly local
 * model building the 2nd step is a global model transformation.
 *
 * <p>After the {@link Api} has been analysed by looking at the controller endpoints in the first
 * step the gathered information is completed and consolidated in this second step. This is the
 * preparation to be ready to transform the {@link Api} into the OpenAPI document in the third step.
 *
 * @author Jan Bernitt
 */
@Value
@Slf4j
public class ApiIntegrator {
  @Value
  @Builder(toBuilder = true)
  public static class Configuration {

    /** When true, the generation fails if there is any name clash. */
    boolean failOnNameClash;

    /**
     * When true, the generation fails if a declaration is declared in an inconsistent way. This
     * usually indicates a programming error.
     *
     * <p>For example, a field/parameter with a default value is marked as required.
     */
    boolean failOnInconsistency;
  }

  Api api;
  Configuration config;

  public static void integrateApi(Api api, Configuration config) {
    new ApiIntegrator(api, config).integrateApi();
  }

  /**
   * OBS! The order of steps in this is critical to produce a correct and complete {@link Api} model
   * ready for document generation.
   */
  private void integrateApi() {
    // 0. validation of the analysis result
    validateParameters();

    // 1. Add input schemas (with shallow object references)
    addSharedInputSchemas();

    // 2. Set and check shared unique names and create the additional schemas for Refs and UIDs
    nameSharedSchemas();
    nameSharedParameters();

    // 3. Add description texts from markdown files to the Api model
    aggregateTags();
    api.getControllers().forEach(ApiIntegrator::describeController);
    api.getSchemas().values().stream()
        .filter(Api.Schema::isShared)
        .forEach(ApiIntegrator::describeSchema);
    api.getGeneratorSchemas().values().stream()
        .flatMap(schemas -> schemas.values().stream())
        .forEach(ApiIntegrator::describeSchema);

    // 4. Group and merge endpoints by request path and method
    groupAndMergeEndpoints();
  }

  /*
  0. validate the Api result of the analysis step
   */

  private void validateParameters() {
    // shared parameters
    api.getComponents()
        .getParameters()
        .values()
        .forEach(params -> params.forEach(this::validateParameter));

    // non shared parameters
    api.getControllers()
        .forEach(
            c ->
                c.getEndpoints()
                    .forEach(e -> e.getParameters().values().forEach(this::validateParameter)));
  }

  private void validateParameter(Parameter p) {
    Api.Maybe<JsonValue> defaultValue = p.getDefaultValue();
    if (p.isRequired() && defaultValue.isPresent() && !defaultValue.getValue().isNull()) {
      String msg =
          "Parameter %s of type %s is both required and has a default value of %s"
              .formatted(
                  p.getFullName(),
                  p.getType().getRawType().getSimpleName(),
                  defaultValue.getValue());
      if (config.failOnInconsistency) throw new IllegalStateException(msg);
      log.warn(msg);
    }
  }

  /*
   * 2. Set and check shared unique names and create the additional schemas
   * for Refs and UIDs
   */

  /**
   * Assigns unique names to the shared schema types or fails if {@link
   * Configuration#failOnNameClash} is true.
   *
   * <p>All shared types are transferred to {@link Api.Components#getSchemas()}.
   */
  private void nameSharedSchemas() {
    Map<String, List<Api.Schema>> sharedSchemasByName = new LinkedHashMap<>();
    Consumer<Api.Schema> addSchema =
        schema ->
            sharedSchemasByName
                .computeIfAbsent(schema.getSharedName().getValue(), name -> new ArrayList<>())
                .add(schema);

    api.getSchemas().values().stream()
        .filter(Api.Schema::isShared)
        .sorted(comparing(e -> e.getSharedName().getValue()))
        .forEach(addSchema);

    sharedSchemasByName.values().stream()
        .flatMap(List::stream)
        .forEach(ApiIntegrator::setSchemaKindName);

    api.getGeneratorSchemas().values().stream()
        .flatMap(schemas -> schemas.values().stream())
        .filter(Api.Schema::isShared)
        .sorted(comparing(e -> e.getSharedName().getValue()))
        .forEach(addSchema);

    if (config.isFailOnNameClash()) {
      checkNoSchemaNameClash(sharedSchemasByName);
    }

    sharedSchemasByName.forEach(
        (name, schemas) -> {
          schemas.get(0).getSharedName().setValue(name);
          if (schemas.size() > 1) {
            log.warn(createSchemaNameClashMessage(name, schemas));
            for (int i = 1; i < schemas.size(); i++) {
              schemas.get(i).getSharedName().setValue(name + "_" + i);
            }
          }
        });

    sharedSchemasByName.values().stream()
        .flatMap(Collection::stream)
        .forEach(
            schema ->
                api.getComponents().getSchemas().put(schema.getSharedName().getValue(), schema));
  }

  /**
   * Parameters generally use the shared name of the parameter defining class combined with the
   * field name of the parameter. This fully qualified shared name must be unique. This still allows
   * to have parameters from different classes of the same name to exist without a conflict as long
   * as none of the parameters of these classes have the same name.
   *
   * <p>Therefore, the all parameters are first grouped by their effective shared name of the
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
                        e.getValue().get(0).getSharedName().getValue(), key -> new ArrayList<>())
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
                        p.getSharedName().setValue(name + "_" + usedNames.size());
                      } else {
                        usedNames.add(p.getName());
                        p.getSharedName().setValue(name);
                      }
                    });
          }
        });
  }

  /** Any list with more than one entry is a clash. */
  private void checkNoSchemaNameClash(Map<String, List<Api.Schema>> schemasBySharedName) {
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
      String name, List<? extends Map.Entry<Class<?>, ?>> parameters) {
    return createNameClashMessage(
        name,
        parameters.stream().map(Map.Entry::getKey).toList(),
        "More than one parameter type uses the shared name `%s` and contains at least one parameter of the same name: \n\t- %s");
  }

  private static String createSchemaNameClashMessage(String name, List<Api.Schema> schemas) {
    return createNameClashMessage(
        name,
        schemas.stream().map(s -> (Class<?>) s.getSource()).toList(),
        "More than one schema type uses the shared name `%s`: \n\t- %s");
  }

  private static String createNameClashMessage(
      String name, Collection<? extends Class<?>> usages, String template) {
    return format(
        template, name, usages.stream().map(Class::getCanonicalName).collect(joining("\n\t- ")));
  }

  private static final Pattern VALID_NAME_INFIX = Pattern.compile("^[-_a-zA-Z0-9.]*$");

  /*
  1. add input schema variants
   */

  private void addSharedInputSchemas() {
    // note this potentially "over-generates" input schemas that are not used anywhere
    // these are removed later again in the generation based on what schemas have
    // actually been referenced when rendering the paths part of the document
    api.getSchemas().values().stream()
        .filter(schema -> !schema.isBidirectional())
        .forEach(this::generateInputSchema);
  }

  private Api.Schema generateInputSchema(Api.Schema output) {
    if (output.isBidirectional()) return output;
    // might already be set due to recursive resolution
    if (output.getInput().isPresent()) return output.getInput().getValue();
    Class<?> schemaType = output.getRawType();
    if (output.getType() == Api.Schema.Type.ARRAY) {
      Api.Schema input = Api.Schema.ofArray(output.getSource(), schemaType);
      output.getInput().setValue(input);
      input.getDirection().setValue(Api.Schema.Direction.IN);
      input.withElements(generateInputReferenceSchema(output.getProperties().get(0)));
      return input;
    }
    Api.Schema input = Api.Schema.ofObject(output.getSource(), schemaType);
    output.getInput().setValue(input);
    if (output.isShared()) {
      String name = output.getSharedName().getValue() + "Params";
      Map<String, Api.Schema> schemas = api.getComponents().getSchemas();
      if (schemas.containsKey(name)) name = output.getSharedName().getValue() + "_Params";
      input.getSharedName().setValue(name);
      api.getGeneratorSchemas()
          .computeIfAbsent(Api.Schema.Direction.class, k -> new ConcurrentHashMap<>())
          .put(schemaType, input);
    }
    input.getDirection().setValue(Api.Schema.Direction.IN);
    output.getProperties().stream()
        .filter(Api.Property::isInput)
        .forEach(p -> input.getProperties().add(p.withType(generateInputReferenceSchema(p))));
    return input;
  }

  private Api.Schema generateInputReferenceSchema(Api.Property property) {
    if (property.getOriginalType().isPresent())
      return generateInputSchema(property.getOriginalType().getValue());
    Api.Schema type = property.getType();
    if (type.isIdentifiable()) return generateIdObject(type);
    return generateInputSchema(type);
  }

  private Api.Schema generateIdObject(Api.Schema of) {
    Class<?> schemaType = of.getIdentifyAs();
    Api.Schema object = Api.Schema.ofObject(of.getSource(), schemaType);
    Map<Class<?>, Api.Schema> idSchemas =
        api.getGeneratorSchemas().computeIfAbsent(UID.class, key -> new ConcurrentHashMap<>());

    Api.Schema idType =
        idSchemas.computeIfAbsent(
            schemaType,
            t -> {
              Api.Schema id = Api.Schema.ofUID(schemaType);
              id.getSharedName().setValue("UID_" + of.getSharedName().getValue());
              return id;
            });
    object.addProperty(new Api.Property(null, "id", true, idType));
    return object;
  }

  /*
   * 3. Add description texts from markdown files to the Api model
   */

  private void aggregateTags() {
    // Note: not tags support for time being
  }

  private static void describeController(Api.Controller controller) {
    ApiDescriptions descriptions = ApiDescriptions.of(controller.getSource());

    controller
        .getEndpoints()
        .forEach(
            endpoint -> {
              String name = endpoint.getSource().getName();
              UnaryOperator<String> subst =
                  desc -> desc.replace("{entityType}", endpoint.getEntityTypeName());
              updateDescription(
                  endpoint.getDescription(),
                  descriptions.get(subst, format("%s.description", name)));
              endpoint
                  .getParameters()
                  .values()
                  .forEach(
                      p ->
                          updateDescription(
                              p.getDescription(),
                              descriptions.get(subst, getParameterKeySequence(name, p))));
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
                        updateDescription(
                            response.getDescription(),
                            descriptions.get(subst, getResponseKeySequence(name, statusCode)));
                        Api.Schema schema = response.getContent().get(MediaType.APPLICATION_JSON);
                        if (schema != null && !schema.isShared()) {
                          schema
                              .getProperties()
                              .forEach(
                                  property ->
                                      updateDescription(
                                          property.getDescription(),
                                          descriptions.get(
                                              subst,
                                              getPropertyKeySequence(name, statusCode, property))));
                        }
                      });
            });
  }

  private static void updateDescription(Api.Maybe<String> target, String desc) {
    String text = target.getValue();
    String merged =
        text != null && text.contains("{md}")
            ? text.replace("{md}", desc == null ? "" : desc)
            : text != null ? text : desc;
    if (merged != null) target.setValue(merged.trim());
  }

  @Nonnull
  private static List<String> getResponseKeySequence(String endpoint, int statusCode) {
    return List.of(
        format("%s.response.%d.description", endpoint, statusCode),
        format("*.response.%d.description", statusCode));
  }

  @Nonnull
  private static List<String> getPropertyKeySequence(
      String endpoint, int statusCode, Api.Property property) {
    return List.of(
        format("%s.response.%d.%s.description", endpoint, statusCode, property.getName()),
        format("*.response.%d.%s.description", statusCode, property.getName()));
  }

  @Nonnull
  private static List<String> getParameterKeySequence(String endpoint, Parameter parameter) {
    return parameter.isShared()
        ? List.of(
            format("%s.parameter.%s.description", endpoint, parameter.getFullName()),
            format(
                "%s.parameter.%s.description", endpoint, getSharedNameForDeclaringType(parameter)),
            format("*.parameter.%s.description", parameter.getFullName()),
            format("*.parameter.%s.description", getSharedNameForDeclaringType(parameter)),
            format("*.parameter.*.%s.description", parameter.getName()))
        : List.of(
            format("%s.parameter.%s.description", endpoint, parameter.getName()),
            format("*.parameter.%s.description", parameter.getName()));
  }

  private static void describeSchema(Api.Schema schema) {
    ApiDescriptions descriptions = ApiDescriptions.of(schema.getRawType());
    String sharedName = schema.getSharedName().orElse("*");
    schema
        .getProperties()
        .forEach(
            property ->
                updateDescription(
                    property.getDescription(),
                    descriptions.get(getSchemaPropertyKeySequence(property, sharedName))));
  }

  @Nonnull
  private static List<String> getSchemaPropertyKeySequence(
      Api.Property property, String sharedName) {
    return List.of(
        format("%s.schema.%s.description", sharedName, property.getName()),
        format("*.schema.%s.description", property.getName()),
        format("%s.description", property.getName()));
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
            (path, pathEndpoints) ->
                groupEndpointsByRequestMethod(pathEndpoints)
                    .forEach(
                        (method, methodEndpoints) ->
                            groupEndpointsByFragment(method, methodEndpoints)
                                .forEach(
                                    (fragment, endpoint) ->
                                        api.getEndpoints()
                                            .computeIfAbsent(
                                                path + fragment,
                                                key -> new EnumMap<>(RequestMethod.class))
                                            .put(method, endpoint))));
  }

  private static Map<RequestMethod, List<Endpoint>> groupEndpointsByRequestMethod(
      List<Endpoint> endpoints) {
    Map<RequestMethod, List<Endpoint>> endpointsByMethod = new EnumMap<>(RequestMethod.class);
    endpoints.forEach(
        e ->
            e.getMethods()
                .forEach(
                    method ->
                        endpointsByMethod
                            .computeIfAbsent(method, key -> new ArrayList<>())
                            .add(e)));
    return endpointsByMethod;
  }

  private Map<String, Api.Endpoint> groupEndpointsByFragment(
      RequestMethod requestMethod, List<Api.Endpoint> endpoints) {
    if (endpoints.size() == 1) return Map.of("", endpoints.get(0));

    Api.Endpoint defaultEndpoint = getDefaultEndpoint(endpoints);
    Map<String, Api.Endpoint> endpointsByFragment = new HashMap<>();
    for (Api.Endpoint endpoint : endpoints) {
      if (!endpoint.equals(defaultEndpoint)) {
        if (isMergeable(endpoint, defaultEndpoint)) {
          defaultEndpoint = mergeEndpoints(defaultEndpoint, endpoint, requestMethod);
        } else {
          endpointsByFragment.put(getFragmentName(endpoint), endpoint);
        }
      }
    }
    endpointsByFragment.put("", defaultEndpoint);
    return endpointsByFragment;
  }

  /**
   * Just by convention the first endpoint handling JSON is considered the default endpoint for that
   * same path and request method. It will have the privilege of not using a fragment in its path to
   * be unique.
   */
  private Api.Endpoint getDefaultEndpoint(List<Api.Endpoint> endpoints) {
    return endpoints.stream()
        .filter(this::consumesOrProducesJson)
        .findFirst()
        .orElse(endpoints.get(0));
  }

  private String getFragmentName(Api.Endpoint endpoint) {
    return "#" + endpoint.getName();
  }

  private boolean isMergeable(Api.Endpoint one, Api.Endpoint other) {
    return one.getParameters().equals(other.getParameters());
  }

  private boolean consumesOrProducesJson(Api.Endpoint endpoint) {
    if (endpoint.getRequestBody().isPresent()
        && endpoint
            .getRequestBody()
            .getValue()
            .getConsumes()
            .containsKey(MediaType.APPLICATION_JSON)) {
      return true;
    }
    return endpoint.getResponses().values().stream()
        .anyMatch(response -> response.getContent().containsKey(MediaType.APPLICATION_JSON));
  }

  private Map<String, List<Api.Endpoint>> groupEndpointsByAbsolutePath() {
    // OBS! We use a TreeMap to also get alphabetical order/grouping
    Map<String, List<Api.Endpoint>> endpointsByAbsolutePath = new TreeMap<>();
    for (Api.Controller c : api.getControllers()) {
      if (c.getPaths().isEmpty()) c.getPaths().add("");
      for (String cPath : c.getPaths()) {
        for (Api.Endpoint e : c.getEndpoints()) {
          for (String ePath : e.getPaths()) {
            String absolutePath =
                (cPath + (cPath.endsWith("/") || ePath.startsWith("/") ? "" : "/") + ePath)
                    .replace("//", "/");
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
            primary.getGroup(),
            primary.getDeprecated(),
            primary.getMaturity());
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

  private static void setSchemaKindName(Api.Schema s) {
    OpenApi.Kind kind = OpenApiAnnotations.getKind(s.getRawType());
    if (kind != null) s.getKind().setValue(kind.value());
  }
}
