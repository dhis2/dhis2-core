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
package org.hisp.dhis.jsonpatch;

import static org.hisp.dhis.schema.DefaultSchemaService.safeInvoke;
import static org.hisp.dhis.util.JsonUtils.jsonToObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.collection.CollectionUtils;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatch;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchException;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchOperation;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Small manager class to handle the JSON patch processing, handles converting from/to JSON nodes,
 * and then applying the JSON patch.
 *
 * @author Morten Olav Hansen
 */
@Service
public class JsonPatchManager {

  private final ObjectMapper jsonMapper;

  /**
   * Cache of per-entity-type {@code ObjectMapper} copies with {@link JsonPatchFilterMixin} bound to
   * that type only -- NOT {@code Object.class}. Binding the mixin globally would apply the
   * exclusion filter to every nested object in the serialized graph too: {@code Sharing} (present
   * on every {@code IdentifiableObject} via {@code BaseIdentifiableObject.getSharing()}) has its
   * own {@code @JsonProperty} field literally named {@code users}, so a global binding would
   * silently strip {@code sharing.users}/{@code sharing.userGroups} whenever a same-named top-level
   * collection (e.g. {@code UserRole.users}) is excluded -- confirmed as a real bug during review,
   * not a hypothetical. Scoping to {@code realClass} makes that specific cross-type collision
   * impossible: only {@code realClass} carries {@code @JsonFilter}, so a differently-typed nested
   * object (e.g. {@code Sharing}) is never affected by another type's exclusions. (A
   * self-referential {@code realClass}, e.g. a nested {@code OrganisationUnit} under another {@code
   * OrganisationUnit}, does re-carry the filter -- but the excluded set only ever names {@code
   * realClass}'s own non-owner collections, so re-applying it at any depth is safe by the same
   * owner/non-owner invariant that makes dropping them at the root safe.) Built lazily, once per
   * distinct {@code realClass} ever patched, then reused -- mirrors {@code
   * org.hisp.dhis.fieldfiltering.FieldFilterSimpleBeanPropertyFilter}'s own {@code
   * ALWAYS_EXPAND_CACHE} pattern for the same "compute once per Class" idiom. Per-call code (see
   * {@link #toJsonNode}) only attaches a fresh {@link SimpleFilterProvider}; it does not rebuild
   * the cached entry on every patch.
   */
  private final Map<Class<?>, ObjectMapper> patchMapperCache = new ConcurrentHashMap<>();

  private final SchemaService schemaService;

  public JsonPatchManager(ObjectMapper jsonMapper, SchemaService schemaService) {
    this.jsonMapper = jsonMapper;
    this.schemaService = schemaService;
  }

  /**
   * Applies a JSON patch to any valid jackson java object. It uses valueToTree to convert from the
   * object into a tree like node structure, and this is where the patch will be applied. This means
   * that any property renaming etc will be followed.
   *
   * <p>Non-owner collection properties that are not referenced by any patch path are omitted from
   * serialization. Non-owner collections are ignored by metadata import UPDATE, so omitting them is
   * free; omitting owner collections would clear them on import. Non-persisted derived properties
   * (for example {@code OrganisationUnit.leaf}) are also omitted when unreferenced, because their
   * getters can force-initialize inverse lazy collections. Skipping avoids initializing lazy
   * Hibernate collections such as {@code UserRole.members} during scalar PATCH /userRoles (slow
   * PATCH /userRoles). A patch path that explicitly targets an otherwise-excludable property (e.g.
   * {@code /users}) removes it from the excluded set, so that property still falls back to full
   * (slower, but correct) hydration -- it is never silently dropped.
   *
   * @param patch JsonPatch object with the operations it should apply.
   * @param object Jackson Object to apply the patch to.
   * @return New instance of the object with the patch applied.
   */
  @Transactional(readOnly = true)
  @SuppressWarnings("unchecked")
  public <T> T apply(JsonPatch patch, T object) throws JsonPatchException {
    if (patch == null || object == null) {
      return null;
    }

    Class<?> realClass = HibernateProxyUtils.getRealClass(object);
    Schema schema = schemaService.getSchema(realClass);

    Set<String> patchedPaths =
        patch.getOperations().stream()
            .map(op -> op.getPath().getMatchingProperty())
            .collect(Collectors.toSet());
    Set<String> excluded = findExcludableProperties(schema, patchedPaths);

    JsonNode node = toJsonNode(object, realClass, excluded);

    // since valueToTree does not properly handle our deeply nested classes,
    // we need to make another trip to make sure all collections are
    // correctly made into json nodes.
    handleCollectionUpdates(object, schema, (ObjectNode) node, excluded);

    validatePatchPath(patch, schema);

    node = patch.apply(node);
    return (T)
        jsonToObject(node, realClass, jsonMapper, ex -> new JsonPatchException(ex.getMessage()));
  }

  /**
   * Collect JSON field names that are safe to omit during patch serialization.
   *
   * <p>Include when no patch path references {@link Property#getName()} or {@link
   * Property#getCollectionName()}, and either:
   *
   * <ul>
   *   <li>{@code isCollection() && !isOwner()} - non-owner collections are ignored by metadata
   *       import UPDATE
   *   <li>{@code !isPersisted()} - derived getters must not run (e.g. {@code leaf} calling {@code
   *       children.isEmpty()})
   * </ul>
   *
   * <p>Owner collections must always remain serialized (metadata import UPDATE would wipe them if
   * omitted). Properties referenced by a patch path keep today's behavior.
   */
  private static Set<String> findExcludableProperties(Schema schema, Set<String> patchedPaths) {
    Set<String> excluded = new HashSet<>();
    for (Property property : schema.getProperties()) {
      if (isReferencedByPatch(property, patchedPaths)) {
        continue;
      }
      if (property.isCollection() && !property.isOwner()) {
        // collectionName is the JSON name Jackson serializes (e.g. "users")
        excluded.add(property.getCollectionName());
      } else if (!property.isPersisted()) {
        String jsonName =
            property.isCollection() ? property.getCollectionName() : property.getName();
        if (jsonName != null) {
          excluded.add(jsonName);
        }
      }
    }
    return excluded;
  }

  private static boolean isReferencedByPatch(Property property, Set<String> patchedPaths) {
    return patchedPaths.contains(property.getName())
        || (property.getCollectionName() != null
            && patchedPaths.contains(property.getCollectionName()));
  }

  /**
   * Builds the JSON tree for {@code object}, skipping getters for properties in {@code excluded}.
   * Looks up (or lazily builds) {@code realClass}'s cached mapper from {@link #patchMapperCache};
   * only attaches a fresh, per-call {@link SimpleFilterProvider} -- the expensive
   * mixin-binding/{@code copy()} happens at most once per distinct {@code realClass}, not on every
   * patch.
   */
  private JsonNode toJsonNode(Object object, Class<?> realClass, Set<String> excluded) {
    if (excluded.isEmpty()) {
      return jsonMapper.valueToTree(object);
    }

    ObjectMapper patchMapper =
        patchMapperCache.computeIfAbsent(
            realClass, cls -> jsonMapper.copy().addMixIn(cls, JsonPatchFilterMixin.class));

    SimpleFilterProvider filterProvider =
        new SimpleFilterProvider()
            .addFilter(
                JsonPatchExcludedPropertyFilter.ID, new JsonPatchExcludedPropertyFilter(excluded));
    return patchMapper.copy().setFilterProvider(filterProvider).valueToTree(object);
  }

  private <T> void handleCollectionUpdates(
      T object, Schema schema, ObjectNode node, Set<String> excluded) {
    for (Property property : schema.getProperties()) {

      if (property.isCollection()) {
        // Skip before safeInvoke so excluded lazy collections stay uninitialized.
        if (excluded.contains(property.getCollectionName())) {
          continue;
        }

        Object data = safeInvoke(object, property.getGetterMethod());

        Collection<?> collection = (Collection<?>) data;

        if (CollectionUtils.isEmpty(collection)) {
          continue;
        }

        if (IdentifiableObject.class.isAssignableFrom(property.getItemKlass())
            && !EmbeddedObject.class.isAssignableFrom(property.getItemKlass())) {
          ArrayNode arrayNode = jsonMapper.createArrayNode();

          collection.forEach(
              item ->
                  arrayNode.add(
                      jsonMapper.valueToTree(
                          shallowCopyIdentifiableObject((IdentifiableObject) item))));

          node.set(property.getCollectionName(), arrayNode);
        } else {
          node.set(property.getCollectionName(), jsonMapper.valueToTree(data));
        }
      }
    }
  }

  /**
   * Create a copy of given {@link BaseIdentifiableObject} but only with two properties: {@link
   * BaseIdentifiableObject#setId(long)} and {@link BaseIdentifiableObject#setUid(String)}. No other
   * properties will be copied.
   *
   * @param source the BaseIdentifiableObject to be cloned.
   * @return a new BaseIdentifiableObject with id and uid properties.
   */
  private BaseIdentifiableObject shallowCopyIdentifiableObject(IdentifiableObject source) {
    BaseIdentifiableObject clone = new BaseIdentifiableObject();
    clone.setId(source.getId());
    clone.setUid(source.getUid());
    return clone;
  }

  /** Check if all patch paths are valid for the given schema. */
  private void validatePatchPath(JsonPatch patch, Schema schema) throws JsonPatchException {
    for (JsonPatchOperation op : patch.getOperations()) {
      if (!schema.hasProperty(op.getPath().getMatchingProperty())) {
        throw new JsonPatchException(
            "Property "
                + op.getPath().getMatchingProperty()
                + " does not exist on "
                + schema.getClass().getSimpleName());
      }
    }
  }
}
