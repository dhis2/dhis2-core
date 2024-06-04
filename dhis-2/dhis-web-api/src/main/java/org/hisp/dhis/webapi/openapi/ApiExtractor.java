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

import static java.util.Arrays.copyOfRange;
import static java.util.Arrays.stream;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.hisp.dhis.webapi.openapi.ApiDescriptions.toMarkdown;
import static org.hisp.dhis.webapi.openapi.DirectType.isDirectType;
import static org.hisp.dhis.webapi.openapi.OpenApiAnnotations.getAnnotated;
import static org.hisp.dhis.webapi.openapi.OpenApiAnnotations.getAnnotations;
import static org.hisp.dhis.webapi.openapi.OpenApiAnnotations.whenAnnotated;
import static org.hisp.dhis.webapi.openapi.Property.getProperties;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.OpenApi.Document.Group;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.webapi.openapi.Api.Parameter.In;
import org.hisp.dhis.webmessage.WebMessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

/**
 * Given a set of controller {@link Class}es this creates a {@link Api} model that describes all
 * relevant {@link Api.Endpoint}s and {@link Api.Schema}s.
 *
 * <p>In simple terms it is a {@link Class} (reflection) information to {@link Api} model
 * transformation.
 *
 * @author Jan Bernitt
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ApiExtractor {
  /**
   * The included classes can be filtered based on REST API resource path or {@link
   * OpenApi.Document#domain()} present on the controller class level. Method level path and tags
   * will not be considered for this filter.
   *
   * @param controllers controllers all potential controllers
   * @param paths filter based on resource path (empty includes all)
   * @param domains filter based on {@link OpenApi.Document#domain()} (empty includes all)
   */
  record Scope(
      @Nonnull Set<Class<?>> controllers,
      @Nonnull Set<String> paths,
      @Nonnull Set<String> domains) {

    boolean includes(Class<?> controller) {
      if (!isControllerType(controller)) return false;
      if (!controllers.contains(controller)) return false;
      if (paths.isEmpty() && domains.isEmpty()) return true;
      if (!paths.isEmpty() && paths(controller).noneMatch(paths::contains)) return false;
      Class<?> domain = domain(controller);
      return domains.isEmpty()
          || domains.contains(domain.getName())
          || domains.contains(domain.getSimpleName());
    }

    private static boolean isControllerType(Class<?> source) {
      return (source.isAnnotationPresent(RestController.class)
              || source.isAnnotationPresent(Controller.class))
          && !source.isAnnotationPresent(OpenApi.Ignore.class);
    }

    private static Stream<String> paths(Class<?> controller) {
      RequestMapping a = controller.getAnnotation(RequestMapping.class);
      return a == null
          ? Stream.empty()
          : stream(firstNonEmpty(a.value(), a.path()))
              .flatMap(path -> Stream.of(path, normalisePath(path)));
    }

    @Nonnull
    private static String normalisePath(@Nonnull String path) {
      return path.startsWith("/api/") ? path.substring(4) : path;
    }

    private static Class<?> domain(Class<?> controller) {
      OpenApi.Document doc = controller.getAnnotation(OpenApi.Document.class);
      Class<?> domain = OpenApi.EntityType.class;
      if (doc != null) {
        domain = doc.domain();
      }
      if (domain == OpenApi.EntityType.class) {
        domain = OpenApiAnnotations.getEntityType(controller);
      }
      return domain == null ? Object.class : domain;
    }
  }

  private static final Map<Class<?>, Api.SchemaGenerator> GENERATORS = new ConcurrentHashMap<>();

  public static void register(Class<?> type, Api.SchemaGenerator generator) {
    GENERATORS.put(type, generator);
  }

  static {
    register(UID.class, SchemaGenerators.UID);
    register(Api.PropertyNames.class, SchemaGenerators.PROPERTY_NAMES);
  }

  /**
   * A mapping annotation might have an empty array for the paths which is identical to just the
   * root path o the controller. This array is used to reflect the presence of this root path as the
   * path mapped.
   */
  private static final String[] ROOT_PATH = {""};

  /**
   * Create an {@link Api} model from controller {@link Class}.
   *
   * @return the {@link Api} for all controllers matching both of the filters
   */
  public static Api extractApi(Scope scope) {
    Api api = new Api();
    scope.controllers.stream()
        .filter(scope::includes)
        .forEach(source -> api.getControllers().add(extractController(api, source)));
    return api;
  }

  private static Api.Controller extractController(Api api, Class<?> source) {
    String name =
        getAnnotated(
            source,
            RequestMapping.class,
            RequestMapping::name,
            n -> !n.isEmpty(),
            () -> source.getSimpleName().replace("Controller", ""));
    Class<?> entityClass = OpenApiAnnotations.getEntityType(source);
    Api.Controller controller = new Api.Controller(api, source, entityClass, name);
    whenAnnotated(
        source, RequestMapping.class, a -> controller.getPaths().addAll(List.of(a.value())));

    methodsIn(source)
        .map(ApiExtractor::getMapping)
        .filter(Objects::nonNull)
        .map(mapping -> extractEndpoint(controller, mapping))
        .filter(Objects::nonNull)
        .forEach(endpoint -> controller.getEndpoints().add(endpoint));

    return controller;
  }

  private static Stream<Method> methodsIn(Class<?> source) {
    return source == null || source == Object.class
        ? Stream.empty()
        : Stream.concat(stream(source.getDeclaredMethods()), methodsIn(source.getSuperclass()));
  }

  private static Api.Endpoint extractEndpoint(Api.Controller controller, EndpointMapping mapping) {
    Method source = mapping.source();
    String name = mapping.name().isEmpty() ? source.getName() : mapping.name();
    Class<?> entityType = OpenApiAnnotations.getEntityType(source, controller.getSource());

    if (source.isAnnotationPresent(OpenApi.Filter.class)) {
      // when the entity type is filtered return null to ignore
      OpenApi.Filter filter = source.getAnnotation(OpenApi.Filter.class);
      Set<Class<?>> includes = Set.of(filter.includes());
      if (!includes.isEmpty() && !includes.contains(entityType)) return null;
      Set<Class<?>> excludes = Set.of(filter.excludes());
      if (!excludes.isEmpty() && excludes.contains(entityType)) return null;
    }

    // request media types
    Set<MediaType> consumes =
        mapping.consumes().stream().map(MediaType::parseMediaType).collect(toSet());
    if (consumes.isEmpty()) {
      // assume JSON if nothing is set explicitly
      consumes.add(MediaType.APPLICATION_JSON);
    }

    Boolean deprecated =
        ConsistentAnnotatedElement.of(source).isAnnotationPresent(Deprecated.class)
            ? Boolean.TRUE
            : null;

    Group group = getEndpointGroup(source);

    Api.Endpoint endpoint =
        new Api.Endpoint(controller, source, entityType, name, group, deprecated);

    endpoint.getDescription().setIfAbsent(extractDescription(source));

    mapping.path().stream()
        .map(path -> path.endsWith("/") ? path.substring(0, path.length() - 1) : path)
        .forEach(path -> endpoint.getPaths().add(path));
    endpoint.getMethods().addAll(mapping.method);

    // request:
    extractParameters(endpoint, consumes);

    // response:
    endpoint.getResponses().putAll(extractResponses(endpoint, mapping, consumes));

    return endpoint;
  }

  private static Group getEndpointGroup(Method source) {
    Group group = Group.DEFAULT;
    if (source.getDeclaringClass().isAnnotationPresent(OpenApi.Document.class))
      group = source.getDeclaringClass().getAnnotation(OpenApi.Document.class).group();
    if (source.isAnnotationPresent(OpenApi.Document.class)) {
      Group overlay = source.getAnnotation(OpenApi.Document.class).group();
      if (overlay != Group.DEFAULT) group = overlay;
    }
    return group == Group.DEFAULT ? Group.MISC : group;
  }

  private static Map<HttpStatus, Api.Response> extractResponses(
      Api.Endpoint endpoint, EndpointMapping mapping, Set<MediaType> consumes) {
    Method source = mapping.source();
    Set<MediaType> produces =
        mapping.produces().stream().map(MediaType::parseMediaType).collect(toSet());
    if (produces.isEmpty()) {
      // either make symmetric or assume JSON as standard
      if (consumes.contains(MediaType.APPLICATION_JSON)
          || consumes.contains(MediaType.APPLICATION_XML)) {
        produces.addAll(consumes); // make symmetric
      } else {
        produces.add(MediaType.APPLICATION_JSON);
      }
    }

    HttpStatus signatureStatus =
        getAnnotated(
            source,
            ResponseStatus.class,
            a -> firstNonEqual(HttpStatus.INTERNAL_SERVER_ERROR, a.value(), a.code()),
            s -> s != HttpStatus.INTERNAL_SERVER_ERROR,
            () -> HttpStatus.OK);

    Map<HttpStatus, Api.Response> res = new LinkedHashMap<>();
    // response(s) declared via annotation(s)
    getAnnotations(source, OpenApi.Response.class)
        .forEach(
            a -> res.putAll(newAdditionalResponse(endpoint, source, a, signatureStatus, produces)));

    // response from method signature
    res.computeIfAbsent(
        signatureStatus, status -> newSuccessResponse(endpoint, source, status, produces));

    // error response(s) from annotated exception types in method signature and
    // error response(s) from annotations on exceptions in method signature
    for (AnnotatedType error : source.getAnnotatedExceptionTypes()) {
      OpenApi.Response response = error.getAnnotation(OpenApi.Response.class);
      if (response == null && error.getType() instanceof Class<?> t)
        response = t.getAnnotation(OpenApi.Response.class);
      if (response != null) {
        res.putAll(newErrorResponse(endpoint, error, response, produces));
      }
    }
    return res;
  }

  @Nonnull
  private static Api.Response newSuccessResponse(
      Api.Endpoint endpoint, Method source, HttpStatus status, Set<MediaType> produces) {
    Class<?> type = source.getReturnType();
    Api.Response response = new Api.Response(status);
    if (type != void.class && type != Void.class && type != ModelAndView.class) {
      response.add(produces, extractResponseSchema(endpoint, source.getGenericReturnType()));
    }
    response.getDescription().setIfAbsent(extractDescription(source.getAnnotatedReturnType()));
    return response;
  }

  private static Map<HttpStatus, Api.Response> newAdditionalResponse(
      Api.Endpoint endpoint,
      Method source,
      OpenApi.Response response,
      HttpStatus status,
      Set<MediaType> produces) {
    return extractResponses(
        endpoint, response, produces, List.of(status), source.getGenericReturnType());
  }

  @Nonnull
  private static Map<HttpStatus, Api.Response> newErrorResponse(
      Api.Endpoint endpoint,
      AnnotatedType source,
      OpenApi.Response response,
      Set<MediaType> produces) {
    Map<HttpStatus, Api.Response> responses =
        extractResponses(endpoint, response, produces, List.of(), null);
    if (responses.size() == 1) {
      Api.Response error = responses.values().iterator().next();
      Type exType = source.getType();
      error
          .getDescription()
          .setIfAbsent(extractDescription(source, exType instanceof Class<?> ex ? ex : null));
    }
    return responses;
  }

  private static String extractDescription(AnnotatedElement source) {
    return extractDescription(source, null);
  }

  private static String extractDescription(
      @Nonnull AnnotatedElement source, @CheckForNull Class<?> type) {
    OpenApi.Description desc0 = source.getAnnotation(OpenApi.Description.class);
    OpenApi.Description desc1 = type == null ? null : type.getAnnotation(OpenApi.Description.class);
    if (desc0 == null && desc1 == null) return null;
    String text0 = desc0 == null ? "" : toMarkdown(desc0);
    String text1 = desc1 == null ? "" : toMarkdown(desc1);
    if (desc0 == null)
      return (desc1.ignoreFileDescription() || text1.contains("{md}") ? "" : "{md}\n") + text1;
    boolean noPlaceholder =
        desc0.ignoreFileDescription() || text0.contains("{md}") || text1.contains("{md}");
    String placeholder = noPlaceholder ? "" : "\n{md}\n";
    return (desc1 == null || desc0.ignoreTypeDescription())
        ? text0 + placeholder
        : text0 + placeholder + text1;
  }

  private static Map<HttpStatus, Api.Response> extractResponses(
      Api.Endpoint endpoint,
      OpenApi.Response response,
      Set<MediaType> defaultProduces,
      List<HttpStatus> defaultStatuses,
      Type defaultResponseType) {
    List<HttpStatus> statuses =
        response.status().length == 0
            ? defaultStatuses
            : stream(response.status()).map(s -> HttpStatus.resolve(s.getCode())).toList();
    Set<Api.Header> headers =
        stream(response.headers())
            .map(
                header ->
                    new Api.Header(
                        header.name(),
                        header.description(),
                        extractParamSchema(endpoint, null, response.value())))
            .collect(toSet());
    Set<MediaType> produces =
        response.mediaTypes().length == 0
            ? defaultProduces
            : stream(response.mediaTypes()).map(MediaType::valueOf).collect(toUnmodifiableSet());
    return statuses.stream()
        .collect(
            toMap(
                identity(),
                status ->
                    new Api.Response(status)
                        .add(
                            produces,
                            extractResponseSchema(endpoint, defaultResponseType, response.value()))
                        .add(headers)));
  }

  private static void extractParameters(Api.Endpoint endpoint, Set<MediaType> consumes) {
    Method source = endpoint.getSource();
    // request parameter(s) declared via annotation(s)
    getAnnotations(source, OpenApi.Param.class).forEach(p -> extractParam(endpoint, p, consumes));
    getAnnotations(source, OpenApi.Params.class).forEach(p -> extractParams(endpoint, p));

    // request parameters from method signature
    for (Parameter p : source.getParameters()) {
      if (p.isAnnotationPresent(OpenApi.Ignore.class)) {
        continue;
      }
      Map<String, Api.Parameter> parameters = endpoint.getParameters();
      if (p.isAnnotationPresent(OpenApi.Param.class)) {
        OpenApi.Param a = p.getAnnotation(OpenApi.Param.class);
        ParameterDetails details = getParameterDetails(a, p);
        Api.Schema type = extractParamSchema(endpoint, p.getParameterizedType(), a.value());
        if (details.in != Api.Parameter.In.BODY) {
          parameters.computeIfAbsent(
              details.name(), key -> newGenericParameter(p, key, details, type));
        } else {
          Api.RequestBody requestBody =
              endpoint.getRequestBody().init(() -> new Api.RequestBody(p, details.required()));
          requestBody.getDescription().setIfAbsent(extractDescription(p, p.getType()));
          consumes.forEach(mediaType -> requestBody.getConsumes().putIfAbsent(mediaType, type));
        }
      } else if (p.isAnnotationPresent(PathVariable.class)) {
        PathVariable a = p.getAnnotation(PathVariable.class);
        String name = firstNonEmpty(a.name(), a.value(), p.getName());
        parameters.computeIfAbsent(
            name, key -> newPathParameter(endpoint, p, key, getParameterDetails(a)));
      } else if (p.isAnnotationPresent(RequestParam.class) && p.getType() != Map.class) {
        RequestParam a = p.getAnnotation(RequestParam.class);
        String name = firstNonEmpty(a.name(), a.value(), p.getName());
        parameters.computeIfAbsent(
            name, key -> newQueryParameter(endpoint, p, key, getParameterDetails(a)));
      } else if (p.isAnnotationPresent(RequestBody.class)) {
        RequestBody a = p.getAnnotation(RequestBody.class);
        Api.RequestBody requestBody =
            endpoint.getRequestBody().init(() -> new Api.RequestBody(p, a.required()));
        requestBody.getDescription().setIfAbsent(extractDescription(p, p.getType()));
        Api.Schema type = extractParamSchema(endpoint, p.getParameterizedType());
        consumes.forEach(mediaType -> requestBody.getConsumes().putIfAbsent(mediaType, type));
      } else if (isParams(p)) {
        extractParams(endpoint, p.getType());
      }
    }
  }

  @Nonnull
  private static Api.Parameter newGenericParameter(
      Parameter source, String key, ParameterDetails details, Api.Schema type) {
    boolean deprecated = source.isAnnotationPresent(Deprecated.class);
    Api.Parameter parameter =
        new Api.Parameter(source, key, details.in(), details.required(), type, deprecated);
    parameter.getDefaultValue().setValue(details.defaultValue());
    parameter.getDescription().setIfAbsent(extractDescription(source, source.getType()));
    return parameter;
  }

  @Nonnull
  private static Api.Parameter newPathParameter(
      Api.Endpoint endpoint, Parameter source, String name, ParameterDetails details) {
    Api.Schema type = extractInputSchema(endpoint, source.getParameterizedType());
    boolean deprecated = source.isAnnotationPresent(Deprecated.class);
    Api.Parameter res =
        new Api.Parameter(source, name, In.PATH, details.required(), type, deprecated);
    res.getDescription().setIfAbsent(extractDescription(source, source.getType()));
    return res;
  }

  @Nonnull
  private static Api.Parameter newQueryParameter(
      Api.Endpoint endpoint, Parameter source, String name, ParameterDetails details) {
    Api.Schema type = extractInputSchema(endpoint, source.getParameterizedType());
    boolean deprecated = source.isAnnotationPresent(Deprecated.class);
    Api.Parameter res =
        new Api.Parameter(source, name, In.QUERY, details.required(), type, deprecated);
    res.getDefaultValue().setValue(details.defaultValue());
    res.getDescription().setIfAbsent(extractDescription(source, source.getType()));
    return res;
  }

  private static void extractParam(
      Api.Endpoint endpoint, OpenApi.Param param, Set<MediaType> consumes) {
    String name = param.name();
    Api.Schema type = extractParamSchema(endpoint, null, param.value());
    Api.Schema wrapped =
        param.asProperty().isEmpty()
            ? type
            : Api.Schema.ofObject(Object.class)
                .addProperty(new Api.Property(param.asProperty(), true, type));
    boolean required = param.required();
    if (name.isEmpty()) {
      Api.RequestBody requestBody = new Api.RequestBody(endpoint.getSource(), required);
      consumes.forEach(mediaType -> requestBody.getConsumes().put(mediaType, wrapped));
      endpoint.getRequestBody().setValue(requestBody);
      return;
    }
    boolean deprecated = param.deprecated();
    Api.Parameter parameter =
        new Api.Parameter(endpoint.getSource(), name, In.QUERY, required, wrapped, deprecated);
    endpoint.getParameters().put(name, parameter);
  }

  private static void extractParams(Api.Endpoint endpoint, OpenApi.Params params) {
    extractParams(endpoint, params.value());
  }

  private static void extractParams(Api.Endpoint endpoint, Class<?> paramsObject) {
    Collection<Property> properties = getProperties(paramsObject);
    OpenApi.Shared shared = paramsObject.getAnnotation(OpenApi.Shared.class);
    String sharedName = getSharedName(paramsObject, shared, null);
    if (sharedName != null) {
      Api api = endpoint.getIn().getIn();
      Map<Class<?>, List<Api.Parameter>> sharedParameters = api.getComponents().getParameters();
      properties.forEach(
          property -> {
            Api.Parameter parameter = extractParameter(endpoint, property);
            parameter.getSharedName().setValue(sharedName);
            sharedParameters.computeIfAbsent(paramsObject, e -> new ArrayList<>()).add(parameter);
            endpoint.getParameters().put(parameter.getName(), parameter);
          });
    } else {
      properties.forEach(
          property ->
              endpoint
                  .getParameters()
                  .computeIfAbsent(
                      property.getName(), name -> extractParameter(endpoint, property)));
    }
  }

  @CheckForNull
  private static String getSharedName(Class<?> type, OpenApi.Shared shared, String defaultName) {
    DirectType directType = DirectType.of(type);
    if (directType != null && !directType.shared()) return null;
    if (shared == null) return defaultName;
    if (!shared.value()) return null;
    if (!shared.name().isEmpty()) return shared.name();
    if (shared.pattern() != OpenApi.Shared.Pattern.DEFAULT)
      return String.format(shared.pattern().getTemplate(), type.getSimpleName());
    return type.getSimpleName();
  }

  private static Api.Parameter extractParameter(Api.Endpoint endpoint, Property property) {
    AnnotatedElement source = (AnnotatedElement) property.getSource();
    Type type = property.getType();
    OpenApi.Property annotated = source.getAnnotation(OpenApi.Property.class);
    Api.Schema schema =
        type instanceof Class && isGeneratorType((Class<?>) type) && annotated != null
            ? extractGeneratorSchema(endpoint, type, annotated.value())
            : extractInputSchema(endpoint, getSubstitutedType(endpoint, property, source));
    boolean deprecated = source.isAnnotationPresent(Deprecated.class);
    Api.Parameter param =
        new Api.Parameter(source, property.getName(), In.QUERY, false, schema, deprecated);
    Object defaultValue = property.getDefaultValue();
    if (defaultValue != null) param.getDefaultValue().setValue(defaultValue.toString());
    param
        .getDescription()
        .setValue(extractDescription(source, type instanceof Class<?> c ? c : null));
    return param;
  }

  private static Api.Schema extractParamSchema(
      Api.Endpoint endpoint, Type source, Class<?>... oneOf) {
    if (oneOf.length == 0 && source != null) {
      return extractInputSchema(endpoint, source);
    }
    if (isGeneratorType(oneOf[0])) {
      return extractGeneratorSchema(endpoint, source, oneOf);
    }
    return Api.Schema.ofOneOf(
        List.of(oneOf), type -> extractInputSchema(endpoint, getSubstitutedType(endpoint, type)));
  }

  private static Api.Schema extractResponseSchema(
      Api.Endpoint endpoint, Type source, Class<?>... oneOf) {
    if (oneOf.length == 0 && source != null) {
      return extractOutputSchema(endpoint, source);
    }
    if (isGeneratorType(oneOf[0])) {
      return extractGeneratorSchema(endpoint, source, oneOf);
    }
    return Api.Schema.ofOneOf(
        List.of(oneOf), type -> extractOutputSchema(endpoint, getSubstitutedType(endpoint, type)));
  }

  private static boolean isGeneratorType(Class<?> type) {
    return Api.SchemaGenerator.class.isAssignableFrom(type)
        || Api.SchemaGenerator[].class.isAssignableFrom(type)
        || GENERATORS.containsKey(type)
        || type.isArray() && isGeneratorType(type.getComponentType());
  }

  private static Api.Schema extractGeneratorSchema(
      Api.Endpoint endpoint, Type source, Class<?>... oneOf) {
    Class<?> type = oneOf[0];
    Class<?> genType = Object[].class.isAssignableFrom(type) ? type.getComponentType() : type;
    Api.Schema schema =
        newGenerator(genType).generate(endpoint, source, copyOfRange(oneOf, 1, oneOf.length));
    Class<?> ofType = schema.getRawType();
    Map<Class<?>, Api.Schema> genTypes =
        endpoint
            .getIn()
            .getIn()
            .getGeneratorSchemas()
            .computeIfAbsent(genType, key -> new ConcurrentHashMap<>());
    schema.getSharedName().setValue(getGeneratorTypeSharedName(genType, schema));
    if (schema.isShared()) {
      Api.Schema shared = genTypes.putIfAbsent(ofType, schema);
      if (shared != null) schema = shared; // this makes sure the same instance is reused
    }
    return type == genType ? schema : Api.Schema.ofArray(type, type).withElements(schema);
  }

  private static String getGeneratorTypeSharedName(Class<?> genType, Api.Schema schema) {
    Class<?> of = schema.getRawType();
    Api.Schema.Type type = schema.getType();
    String sharedBaseName =
        getSharedName(of, of.getAnnotation(OpenApi.Shared.class), of.getSimpleName());
    return switch (type) {
      case UID -> "UID_" + sharedBaseName;
      case ENUM -> sharedBaseName + genType.getSimpleName();
      default -> sharedBaseName;
    };
  }

  private static Api.Schema extractInputSchema(Api.Endpoint endpoint, Type source) {
    return extractTypeSchema(endpoint, source);
  }

  private static Api.Schema extractOutputSchema(Api.Endpoint endpoint, Type source) {
    return extractTypeSchema(endpoint, source);
  }

  /**
   * The centerpiece of the type analysis/extraction.
   *
   * <p>Some important aspects to understand:
   *
   * <ul>
   *   <li>Only {@link Class} types (named types in Java) that never transform to different schemas
   *       depending on their context may end up in {@link Api#getSchemas()}. Otherwise one (the
   *       first) transformation would wrongly be used for all possible transformations.
   *   <li>While resolving the schema of a {@link Class} type the resulting {@link Api.Schema} is
   *       added to the resolving context map before any properties of that schema are recursively
   *       resolved. This is necessary so that recursive type structures do not end up in endless
   *       loops or stack overflows. Instead the context already knows the {@link Api.Schema}
   *       instance for the type (even if some of its properties might still be missing) and the
   *       instance can be returned from the resolving context.
   * </ul>
   *
   * @return a schema describing a complex "record-like" or "bean" object
   */
  private static Api.Schema extractClassSchema(Api.Endpoint endpoint, Class<?> type) {
    Api api = endpoint.getIn().getIn();
    Api.Schema s = api.getSchemas().get(type);
    if (s != null) {
      return s;
    }
    UnaryOperator<Api.Schema> addShared =
        schema -> {
          schema
              .getSharedName()
              .setValue(
                  getSharedName(
                      type, type.getAnnotation(OpenApi.Shared.class), type.getSimpleName()));
          if (schema.isShared() || isDirectType(type)) {
            api.getSchemas().put(type, schema);
          }
          return schema;
        };
    if (type.isArray()) {
      Api.Schema schema = Api.Schema.ofArray(type);
      // eventually this will resolve the simple element type
      schema.withElements(extractClassSchema(endpoint, type.getComponentType()));
      return schema;
    }
    if (type.isEnum()) {
      List<String> values =
          Stream.of(type.getEnumConstants()).map(e -> ((Enum<?>) e).name()).toList();
      return addShared.apply(Api.Schema.ofEnum(type, type, values));
    }
    if (type.isAnnotationPresent(JsonSubTypes.class)) {
      Api.Schema schema = extractSubTypeSchema(endpoint, type);
      return schema.getSource() == type ? addShared.apply(schema) : schema;
    }
    Collection<Property> properties = isDirectType(type) ? List.of() : getProperties(type);
    if (properties.isEmpty()) {
      return addShared.apply(Api.Schema.ofSimple(type));
    }
    Api.Schema schema = addShared.apply(Api.Schema.ofObject(type));
    // OOBS! It is important that at this point the schema for the current type is in
    // the schemas map so recursive types do resolve (unless they are inlined)
    for (Property p : properties) {
      Function<Api.Schema, Api.Property> toProperty =
          t -> new Api.Property(getPropertyName(endpoint, p), p.getRequired(), t);
      schema.addProperty(extractObjectProperty(endpoint, p, toProperty));
    }
    return schema;
  }

  private static Api.Property extractObjectProperty(
      Api.Endpoint endpoint, Property property, Function<Api.Schema, Api.Property> toProperty) {
    AnnotatedElement member = (AnnotatedElement) property.getSource();
    if (member.isAnnotationPresent(JsonSubTypes.class)) {
      return toProperty.apply(extractSubTypeSchema(endpoint, member));
    }
    Type type = getSubstitutedType(endpoint, property, member);
    OpenApi.Property annotated = member.getAnnotation(OpenApi.Property.class);
    if (type instanceof Class && isGeneratorType((Class<?>) type) && annotated != null) {
      return toProperty.apply(extractGeneratorSchema(endpoint, type, annotated.value()));
    }
    JsonSerialize serialize = member.getAnnotation(JsonSerialize.class);
    if (serialize != null && serialize.as() != Void.class) {
      Class<?> as = serialize.as();
      Api.Property res = toProperty.apply(extractClassSchema(endpoint, as));
      res.getOriginalType().setValue(extractTypeSchema(endpoint, type));
      return res;
    }
    if (serialize != null
        && serialize.contentAs() != Void.class
        && type instanceof ParameterizedType pt) {
      Class<?> contentAs = serialize.contentAs();
      Api.Property res =
          toProperty.apply(
              Api.Schema.ofArray(type, (Class<?>) pt.getRawType())
                  .withElements(extractClassSchema(endpoint, contentAs)));
      res.getOriginalType().setValue(extractTypeSchema(endpoint, type));
      return res;
    }
    return toProperty.apply(extractTypeSchema(endpoint, type));
  }

  private static Api.Schema extractSubTypeSchema(Api.Endpoint endpoint, AnnotatedElement baseType) {
    List<Class<?>> types =
        Stream.of(baseType.getAnnotation(JsonSubTypes.class).value())
            .map(JsonSubTypes.Type::value)
            .collect(toList());
    return Api.Schema.ofOneOf(types, subType -> extractClassSchema(endpoint, subType));
  }

  private static Api.Schema extractTypeSchema(Api.Endpoint endpoint, Type source) {
    if (source instanceof Class<?> type) {
      return extractClassSchema(endpoint, type);
    }
    if (source instanceof ParameterizedType pt) {
      Class<?> rawType = (Class<?>) pt.getRawType();
      if (rawType == Class.class) {
        return Api.Schema.ofSimple(rawType);
      }
      Type typeArg0 = pt.getActualTypeArguments()[0];
      if (Collection.class.isAssignableFrom(rawType) && rawType.isInterface()
          || rawType == Iterable.class) {
        if (typeArg0 instanceof Class<?>)
          return extractTypeSchema(endpoint, Array.newInstance((Class<?>) typeArg0, 0).getClass());
        return Api.Schema.ofArray(source, rawType)
            .withElements(extractTypeSchema(endpoint, typeArg0));
      }
      if (Map.class.isAssignableFrom(rawType) && rawType.isInterface()) {
        return Api.Schema.ofObject(source, rawType)
            .withEntries(
                extractTypeSchema(endpoint, typeArg0),
                extractTypeSchema(endpoint, pt.getActualTypeArguments()[1]));
      }
      if (rawType == ResponseEntity.class) {
        // just unpack, presents of ResponseEntity is hidden
        return extractTypeSchema(endpoint, typeArg0);
      }
      return Api.Schema.ofUnsupported(source);
    }
    if (source instanceof WildcardType wt) {
      if (wt.getLowerBounds().length == 0
          && Arrays.equals(wt.getUpperBounds(), new Type[] {Object.class}))
        return Api.Schema.ofUnsupported(wt);
      // simplification: <? extends X> => <X>
      return extractTypeSchema(endpoint, wt.getUpperBounds()[0]);
    }
    return Api.Schema.ofUnsupported(source);
  }

  /*
   * OpenAPI "business" helper methods
   */

  private static String getPropertyName(Api.Endpoint endpoint, Property property) {
    return "path$".equals(property.getName())
        ? endpoint.getIn().getPaths().get(0).replace("/", "")
        : property.getName();
  }

  private static Type getSubstitutedType(
      Api.Endpoint endpoint, Property property, AnnotatedElement member) {
    Type type = property.getType();
    if (member.isAnnotationPresent(OpenApi.EntityType.class)) {
      return getSubstitutedType(endpoint, member.getAnnotation(OpenApi.EntityType.class).value());
    }
    if (type instanceof Class<?>) {
      return getSubstitutedType(endpoint, (Class<?>) type);
    }
    return type;
  }

  /**
   * @return the type referred to by the type found in an annotation.
   */
  private static Class<?> getSubstitutedType(Api.Endpoint endpoint, Class<?> type) {
    if (type == OpenApi.EntityType.class && endpoint.getEntityType() != null) {
      return endpoint.getEntityType();
    }
    if (type == OpenApi.EntityType[].class && endpoint.getEntityType() != null) {
      return Array.newInstance(endpoint.getEntityType(), 0).getClass();
    }
    if (type == WebMessageResponse.class) {
      return WebMessage.class;
    }
    return type;
  }

  /**
   * @return is this a parameter objects with properties which are parameters?
   */
  private static boolean isParams(Parameter source) {
    Class<?> type = source.getType();
    if (type.isAnnotationPresent(OpenApi.Params.class)) {
      return true;
    }
    if (type.isInterface()
        || type.isEnum()
        || IdentifiableObject.class.isAssignableFrom(type)
        || source.getAnnotations().length > 0
        || source.isAnnotationPresent(OpenApi.Ignore.class)
        || !(source.getParameterizedType() instanceof Class)) return false;
    return stream(type.getDeclaredConstructors()).anyMatch(c -> c.getParameterCount() == 0);
  }

  private static EndpointMapping getMapping(Method source) {
    if (ConsistentAnnotatedElement.of(source).isAnnotationPresent(OpenApi.Ignore.class)) {
      return null; // ignore this
    }
    if (source.isAnnotationPresent(RequestMapping.class)) {
      RequestMapping a = source.getAnnotation(RequestMapping.class);
      return new EndpointMapping(
          source,
          a.name(),
          firstNonEmpty(a.value(), a.path(), ROOT_PATH),
          a.method(),
          a.params(),
          a.headers(),
          a.consumes(),
          a.produces());
    }
    if (source.isAnnotationPresent(GetMapping.class)) {
      GetMapping a = source.getAnnotation(GetMapping.class);
      return new EndpointMapping(
          source,
          a.name(),
          firstNonEmpty(a.value(), a.path(), ROOT_PATH),
          new RequestMethod[] {RequestMethod.GET},
          a.params(),
          a.headers(),
          a.consumes(),
          a.produces());
    }
    if (source.isAnnotationPresent(PutMapping.class)) {
      PutMapping a = source.getAnnotation(PutMapping.class);
      return new EndpointMapping(
          source,
          a.name(),
          firstNonEmpty(a.value(), a.path(), ROOT_PATH),
          new RequestMethod[] {RequestMethod.PUT},
          a.params(),
          a.headers(),
          a.consumes(),
          a.produces());
    }
    if (source.isAnnotationPresent(PostMapping.class)) {
      PostMapping a = source.getAnnotation(PostMapping.class);
      return new EndpointMapping(
          source,
          a.name(),
          firstNonEmpty(a.value(), a.path(), ROOT_PATH),
          new RequestMethod[] {RequestMethod.POST},
          a.params(),
          a.headers(),
          a.consumes(),
          a.produces());
    }
    if (source.isAnnotationPresent(PatchMapping.class)) {
      PatchMapping a = source.getAnnotation(PatchMapping.class);
      return new EndpointMapping(
          source,
          a.name(),
          firstNonEmpty(a.value(), a.path(), ROOT_PATH),
          new RequestMethod[] {RequestMethod.PATCH},
          a.params(),
          a.headers(),
          a.consumes(),
          a.produces());
    }
    if (source.isAnnotationPresent(DeleteMapping.class)) {
      DeleteMapping a = source.getAnnotation(DeleteMapping.class);
      return new EndpointMapping(
          source,
          a.name(),
          firstNonEmpty(a.value(), a.path(), ROOT_PATH),
          new RequestMethod[] {RequestMethod.DELETE},
          a.params(),
          a.headers(),
          a.consumes(),
          a.produces());
    }
    return null;
  }

  private static ParameterDetails getParameterDetails(Parameter source) {
    if (source.isAnnotationPresent(PathVariable.class))
      return getParameterDetails(source.getAnnotation(PathVariable.class));
    if (source.isAnnotationPresent(RequestParam.class))
      return getParameterDetails(source.getAnnotation(RequestParam.class));
    if (source.isAnnotationPresent(RequestBody.class))
      return getParameterDetails(source.getAnnotation(RequestBody.class));
    return null;
  }

  @Nonnull
  private static ParameterDetails getParameterDetails(OpenApi.Param a, Parameter source) {
    ParameterDetails details = getParameterDetails(source);
    String name = firstNonEmpty(a.name(), details == null ? "" : details.name(), source.getName());
    Api.Parameter.In in = details == null ? Api.Parameter.In.QUERY : details.in();
    boolean required = details == null ? a.required() : details.required();
    String fallbackDefaultValue = details != null ? details.defaultValue() : null;
    String defaultValue = !a.defaultValue().isEmpty() ? a.defaultValue() : fallbackDefaultValue;
    return new ParameterDetails(in, name, required, defaultValue);
  }

  @Nonnull
  private static ParameterDetails getParameterDetails(RequestBody a) {
    return new ParameterDetails(In.BODY, "", a.required(), null);
  }

  @Nonnull
  private static ParameterDetails getParameterDetails(PathVariable a) {
    return new ParameterDetails(In.PATH, firstNonEmpty(a.name(), a.value()), a.required(), null);
  }

  @Nonnull
  private static ParameterDetails getParameterDetails(RequestParam a) {
    boolean hasDefault = !a.defaultValue().equals("\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n");
    boolean required = a.required() && !hasDefault;
    String defaultValue = hasDefault ? a.defaultValue() : null;
    return new ParameterDetails(
        In.QUERY, firstNonEmpty(a.name(), a.value()), required, defaultValue);
  }

  /*
   * Basic helper methods
   */

  private static Api.SchemaGenerator newGenerator(Class<?> type) {
    Api.SchemaGenerator generator = GENERATORS.get(type);
    if (generator == null) {
      throw new IllegalStateException("No generator for type: " + type);
    }
    return generator;
  }

  private static String[] firstNonEmpty(String[] a, String[] b, String[] c) {
    return firstNonEmpty(firstNonEmpty(a, b), c);
  }

  private static String[] firstNonEmpty(String[] a, String[] b) {
    return a.length == 0 ? b : a;
  }

  private static String firstNonEmpty(String a, String b) {
    return a.isEmpty() ? b : a;
  }

  private static String firstNonEmpty(String a, String b, String c) {
    String ab = firstNonEmpty(a, b);
    return !ab.isEmpty() ? ab : c;
  }

  @SafeVarargs
  private static <E extends Enum<E>> E firstNonEqual(E to, E... samples) {
    return stream(samples).filter(e -> e != to).findFirst().orElse(samples[0]);
  }

  record EndpointMapping(
      Method source,
      String name,
      List<String> path,
      Set<RequestMethod> method,
      List<String> params,
      List<String> headers,
      List<String> consumes,
      List<String> produces) {
    EndpointMapping(
        Method source,
        String name,
        String[] path,
        RequestMethod[] method,
        String[] params,
        String[] headers,
        String[] consumes,
        String[] produces) {
      this(
          source,
          name,
          List.of(path),
          Set.of(method),
          List.of(params),
          List.of(headers),
          List.of(consumes),
          List.of(produces));
    }
  }

  record ParameterDetails(
      Api.Parameter.In in, String name, boolean required, String defaultValue) {}
}
