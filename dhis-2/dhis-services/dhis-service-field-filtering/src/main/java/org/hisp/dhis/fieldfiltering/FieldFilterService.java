/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.fieldfiltering;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableSet;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.PropertyPath;
import org.hisp.dhis.fieldfiltering.transformers.IsEmptyFieldTransformer;
import org.hisp.dhis.fieldfiltering.transformers.IsNotEmptyFieldTransformer;
import org.hisp.dhis.fieldfiltering.transformers.KeyByFieldTransformer;
import org.hisp.dhis.fieldfiltering.transformers.PluckFieldTransformer;
import org.hisp.dhis.fieldfiltering.transformers.RenameFieldTransformer;
import org.hisp.dhis.fieldfiltering.transformers.SizeFieldTransformer;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.OrderComparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen
 */
@Service
public class FieldFilterService {
  private final FieldPathHelper fieldPathHelper;

  @Qualifier("jsonMapper")
  private final ObjectMapper jsonMapper;

  private final SchemaService schemaService;

  private final AclService aclService;

  private final UserGroupService userGroupService;

  private final UserService userService;

  private final AttributeService attributeService;

  public FieldFilterService(
      FieldPathHelper fieldPathHelper,
      ObjectMapper jsonMapper,
      SchemaService schemaService,
      AclService aclService,
      UserGroupService userGroupService,
      UserService userService,
      AttributeService attributeService) {
    this.fieldPathHelper = fieldPathHelper;
    this.jsonMapper = configureFieldFilterObjectMapper(jsonMapper);
    this.schemaService = schemaService;
    this.aclService = aclService;
    this.userGroupService = userGroupService;
    this.userService = userService;
    this.attributeService = attributeService;
  }

  private ObjectMapper configureFieldFilterObjectMapper(ObjectMapper objectMapper) {
    objectMapper = objectMapper.copy();

    SimpleModule module = new SimpleModule();
    module.setMixInAnnotation(Object.class, FieldFilterMixin.class);

    objectMapper.registerModule(module);
    objectMapper.setAnnotationIntrospector(new IgnoreJsonSerializerRefinementAnnotationInspector());

    return objectMapper;
  }

  public static class IgnoreJsonSerializerRefinementAnnotationInspector
      extends JacksonAnnotationIntrospector {
    /**
     * Since the field filter will handle type refinement itself (to avoid recursive loops), we want
     * to ignore any type refinement happening with @JsonSerialize(...). In the future we would want
     * to remove all @JsonSerialize annotations and just use the field filters, but since we still
     * have object mappers without field filtering we can't do this just yet.
     */
    @Override
    public JavaType refineSerializationType(
        MapperConfig<?> config, Annotated a, JavaType baseType) {
      return baseType;
    }
  }

  @Transactional(readOnly = true)
  public <T> ObjectNode toObjectNode(T object, String filters) {
    return toObjectNode(object, FieldFilterParser.parse(filters));
  }

  @Transactional(readOnly = true)
  public <T> ObjectNode toObjectNode(T object, List<FieldPath> filters) {
    List<ObjectNode> objectNodes = toObjectNodes(List.of(object), filters, null, false);

    if (objectNodes.isEmpty()) {
      return null;
    }

    return objectNodes.get(0);
  }

  @Transactional(readOnly = true)
  public List<ObjectNode> toObjectNodes(FieldFilterParams<?> params) {
    List<ObjectNode> objectNodes = new ArrayList<>();

    if (params.getObjects().isEmpty()) {
      return objectNodes;
    }

    List<FieldPath> paths = FieldFilterParser.parse(params.getFields());
    return toObjectNodes(params.getObjects(), paths, params.getUser(), params.isSkipSharing());
  }

  @Transactional(readOnly = true)
  public <T> List<ObjectNode> toObjectNodes(List<T> objects, List<FieldPath> paths) {
    return toObjectNodes(objects, paths, null, false);
  }

  @Transactional(readOnly = true)
  public <T> List<ObjectNode> toObjectNodes(
      List<T> objects, List<FieldPath> paths, UserDetails user, boolean isSkipSharing) {
    List<ObjectNode> objectNodes = new ArrayList<>();

    if (objects.isEmpty()) {
      return objectNodes;
    }

    toObjectNodes(objects, paths, user, isSkipSharing, objectNodes::add);

    return objectNodes;
  }

  /**
   * Method allowing calls without an exclude defaults boolean parameter. This keeps current
   * behaviour as is. If callers require different behaviour regarding excluding defaults, then they
   * can call the toObjectNodes method which takes a value for excluding defaults. This method
   * passes false as the default value for excluding defaults.
   *
   * @param objects objects to render
   * @param filter filters
   * @param user user
   * @param isSkipSharing skip sharing
   * @param consumer consumer action
   * @param <T> type of objects
   */
  private <T> void toObjectNodes(
      List<T> objects,
      List<FieldPath> filter,
      UserDetails user,
      boolean isSkipSharing,
      Consumer<ObjectNode> consumer) {
    toObjectNodes(objects, filter, user, isSkipSharing, false, consumer);
  }

  private <T> void toObjectNodes(
      List<T> objects,
      List<FieldPath> filter,
      UserDetails user,
      boolean isSkipSharing,
      boolean excludeDefaults,
      Consumer<ObjectNode> consumer) {

    UserDetails currentUserDetails =
        Objects.requireNonNullElseGet(user, CurrentUserUtil::getCurrentUserDetails);

    // In case we get a proxied object in we can't just use o.getClass(), we
    // need to figure out the real class name by using HibernateProxyUtils.
    Object firstObject = objects.iterator().next();
    List<FieldPath> paths =
        fieldPathHelper.apply(filter, HibernateProxyUtils.getRealClass(firstObject));

    SimpleFilterProvider filterProvider =
        createSimpleFilterProvider(paths, isSkipSharing, excludeDefaults);

    // only set filter provider on a local copy so that we don't affect
    // other object mappers (running across other threads)
    ObjectMapper objectMapper = jsonMapper.copy().setFilterProvider(filterProvider);

    Map<PropertyPath, List<FieldTransformer>> fieldTransformers = getTransformers(paths);
    List<FieldPath> absoluteAttributePaths = getAttributePropertyPathsInAttributeValues(paths);
    List<FieldPath> relativeAttributePaths =
        absoluteAttributePaths.stream().map(e -> e.relativeTo("attribute")).toList();

    Map<String, ObjectNode> attributeProperties = new HashMap<>();

    for (Object object : objects) {
      applyAccess(object, paths, isSkipSharing, currentUserDetails);
      applySharingDisplayNames(object, paths, isSkipSharing);

      ObjectNode objectNode = objectMapper.valueToTree(object);
      addAttributeFieldsInAttributeValues(
          object, objectNode, relativeAttributePaths, attributeProperties);
      applyAttributeAsPropertyFields(object, objectNode, paths);
      applyTransformers(objectNode, fieldTransformers);

      if (excludeDefaults) removeEmptyObjects(objectNode);

      consumer.accept(objectNode);
    }
  }

  /**
   * Method that removes empty objects from an ObjectNode, at root level.
   *
   * <p>e.g. json input: <br>
   * <code>
   *   {
   *      "name": "dhis2",
   *      "system": {}
   *   }
   * </code>
   *
   * <p><br>
   *
   * <p>resulting output: <br>
   * <code>
   *   {
   *      "name": "dhis2"
   *   }
   * </code>
   *
   * @param objectNode object node to process on
   */
  private void removeEmptyObjects(ObjectNode objectNode) {
    Iterator<JsonNode> elements = objectNode.elements();

    while (elements.hasNext()) {
      JsonNode next = elements.next();
      if (next.isObject() && next.isEmpty()) {
        elements.remove();
      }
    }
  }

  private static final PropertyPath ATTRIBUTE_VALUES_PATH =
      PropertyPath.of("attributeValues.attribute");

  /**
   * @param paths all paths requested
   * @return only paths that belong to a property of {@link Attribute} that is not "id" with the
   *     "attributeValues"
   */
  private static List<FieldPath> getAttributePropertyPathsInAttributeValues(List<FieldPath> paths) {
    return paths.stream()
        .filter(
            path -> {
              PropertyPath p = path.getPath();
              if (p.length() != 3) return false;
              if (!p.isParent(ATTRIBUTE_VALUES_PATH)) return false;
              return !p.segment().contentEquals("id");
            })
        .toList();
  }

  /**
   * Streams filtered object nodes using given JsonGenerator.
   *
   * @param params Filter params to apply
   * @param generator Pre-created json generator
   * @throws IOException if there is either an underlying I/O problem or encoding issue on writing
   *     to the generator
   */
  @Transactional(readOnly = true)
  public void toObjectNodesStream(
      FieldFilterParams<?> params, boolean excludeDefaults, JsonGenerator generator)
      throws IOException {
    if (params.getObjects().isEmpty()) {
      return;
    }
    List<FieldPath> paths = FieldFilterParser.parse(params.getFields());

    try {
      toObjectNodes(
          params.getObjects(),
          paths,
          params.getUser(),
          params.isSkipSharing(),
          excludeDefaults,
          n -> {
            try {
              generator.writeObject(n);
            } catch (IOException e) {
              throw new UncheckedIOException(e);
            }
          });
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }

  /**
   * Adds in properties of {@link Attribute} that are not contained in the serialization of {@link
   * org.hisp.dhis.attribute.AttributeValues} if the {@link FieldPath}s contains a path to an {@link
   * Attribute} property other than "id".
   */
  private void addAttributeFieldsInAttributeValues(
      Object object,
      ObjectNode objectNode,
      List<FieldPath> relativeAttributePaths,
      Map<String, ObjectNode> attributeProperties) {
    if (relativeAttributePaths.isEmpty()) return;
    if (!(object instanceof IdentifiableObject source)) return;
    ArrayNode attributes = objectNode.putArray("attributeValues");
    source
        .getAttributeValues()
        .forEach(
            (key, value) -> {
              String attributeId = key.toString();
              ObjectNode attrValueNode = attributes.addObject();
              attrValueNode.put("value", value.toString());
              attrValueNode.set(
                  "attribute",
                  attributeProperties.computeIfAbsent(
                      attributeId,
                      k -> {
                        Attribute attribute = attributeService.getAttribute(attributeId);
                        List<ObjectNode> res = new ArrayList<>(1);
                        toObjectNodes(
                            List.of(attribute), relativeAttributePaths, null, true, res::add);
                        ObjectNode attr = res.get(0);
                        attr.put("id", attributeId);
                        return attr;
                      }));
            });
  }

  /**
   * Adds in those property paths that end with a UID of an attribute treating attributes as if they
   * were usual properties of the parent object.
   */
  private void applyAttributeAsPropertyFields(
      Object object, ObjectNode node, List<FieldPath> paths) {
    if ((object instanceof IdentifiableObject obj)) {
      for (FieldPath path : paths) {
        applyAttributeAsPropertyField(obj, node, path);
      }
    }
  }

  private void applyAttributeAsPropertyField(
      IdentifiableObject object, ObjectNode node, FieldPath path) {
    if (path.getProperty() != null || !path.getPath().isUID()) return;

    String attributeId = path.getPropertyName();

    String value = object.getAttributeValues().get(attributeId);
    if (value == null) return;

    Attribute attribute = attributeService.getAttribute(attributeId);

    if (!value.isBlank() && attribute.getValueType().isJson()) {
      try {
        node.set(attributeId, jsonMapper.readTree(value));
      } catch (JsonProcessingException e) {
        node.put(attributeId, value);
      }
    } else {
      node.put(attributeId, value);
    }
  }

  private void applyFieldPathVisitor(
      Object object, List<FieldPath> paths, BiConsumer<Object, PropertyPath> visitor) {
    if (object == null) {
      return;
    }

    Schema schema = schemaService.getSchema(HibernateProxyUtils.getRealClass(object));

    if (!schema.isIdentifiableObject()) {
      return;
    }

    fieldPathHelper.visitPaths(object, paths, visitor);
  }

  public ObjectNode createObjectNode() {
    return jsonMapper.createObjectNode();
  }

  public ArrayNode createArrayNode() {
    return jsonMapper.createArrayNode();
  }

  private void applyTransformers(
      JsonNode root, Map<PropertyPath, List<FieldTransformer>> fieldTransformers) {
    if (fieldTransformers.isEmpty()) return;
    fieldTransformers.forEach((path, transformers) -> applyTransformers(root, path, transformers));
  }

  /** Recursively applies FieldTransformers to a Json node. */
  private void applyTransformers(
      JsonNode parent, PropertyPath path, List<FieldTransformer> transformers) {
    if (path == null) return; // just to make null checker happy
    if (path.parent() != null) {
      // need to drill down
      String name = path.head().toString();
      JsonNode node = parent.get(name);
      if (node == null || node.isNull()) return; // done here
      if (node.isObject()) {
        applyTransformers(node, path.dropHead(), transformers);
      } else if (node.isArray()) {
        PropertyPath subPath = path.dropHead();
        Iterator<JsonNode> iter = node.elements();
        while (iter.hasNext()) applyTransformers(iter.next(), subPath, transformers);
      }
    } else {
      // need to apply the transformers
      if (!parent.isObject()) return; // must be an object, otherwise ignore
      String name = path.property().toString();
      JsonNode target = parent.get(name);
      if (target == null || target.isNull()) return; // nothing to do here
      transformers.forEach(t -> t.apply(path, target, parent));
    }
  }

  private SimpleFilterProvider createSimpleFilterProvider(
      List<FieldPath> paths, boolean skipSharing, boolean excludeDefaults) {
    SimpleFilterProvider res = new SimpleFilterProvider();
    Set<String> includePaths =
        paths.stream()
            .map(p -> p.getPath().properties().collect(joining(".")))
            .collect(toUnmodifiableSet());
    Set<String> skipPaths =
        skipSharing
            ? Set.of("user", "publicAccess", "userGroupAccesses", "userAccesses", "sharing")
            : Set.of();
    res.addFilter(
        "field-filter",
        new FieldFilterSimpleBeanPropertyFilter(includePaths, skipPaths, excludeDefaults));

    return res;
  }

  private Map<PropertyPath, List<FieldTransformer>> getTransformers(List<FieldPath> paths) {
    Map<PropertyPath, List<FieldTransformer>> transformerMap = new HashMap<>();

    for (FieldPath path : paths) {
      List<FieldTransformer> fieldTransformers = new ArrayList<>();

      transformerMap.put(path.getPath(), fieldTransformers);

      for (FieldPathTransformer transformer : path.getTransformers()) {
        switch (transformer.name().toLowerCase()) {
          case "rename" -> fieldTransformers.add(new RenameFieldTransformer(transformer));
          case "size" -> fieldTransformers.add(SizeFieldTransformer.INSTANCE);
          case "isempty" -> fieldTransformers.add(IsEmptyFieldTransformer.INSTANCE);
          case "isnotempty" -> fieldTransformers.add(IsNotEmptyFieldTransformer.INSTANCE);
          case "pluck" -> fieldTransformers.add(new PluckFieldTransformer(transformer));
          case "keyby" -> fieldTransformers.add(new KeyByFieldTransformer(transformer));
          default -> {
            // invalid transformer
          }
        }
      }

      fieldTransformers.sort(OrderComparator.INSTANCE);
    }

    return transformerMap;
  }

  private void applySharingDisplayNames(Object root, List<FieldPath> paths, boolean isSkipSharing) {
    if (isSkipSharing) return;
    applyFieldPathVisitor(
        root,
        paths,
        (obj, path) -> {
          if (obj instanceof IdentifiableObject object
              && object.hasSharing()
              && path.segment().contentEquals("sharing")) {
            object
                .getSharing()
                .getUserGroups()
                .values()
                .forEach(uga -> uga.setDisplayName(userGroupService.getDisplayName(uga.getId())));
            object
                .getSharing()
                .getUsers()
                .values()
                .forEach(ua -> ua.setDisplayName(userService.getDisplayName(ua.getId())));
          }
        });
  }

  private void applyAccess(
      Object root, List<FieldPath> paths, boolean isSkipSharing, UserDetails userDetails) {
    if (isSkipSharing) return;
    applyFieldPathVisitor(
        root,
        paths,
        (obj, path) -> {
          if (obj instanceof IdentifiableObject object && path.segment().contentEquals("access")) {
            object.setAccess(aclService.getAccess(object, userDetails));
          }
        });
  }
}
