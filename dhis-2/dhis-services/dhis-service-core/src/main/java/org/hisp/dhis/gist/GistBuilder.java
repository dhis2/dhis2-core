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
package org.hisp.dhis.gist;

import static java.util.Arrays.stream;
import static java.util.Comparator.comparingInt;
import static java.util.Map.entry;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.common.collection.CollectionUtils.merge;
import static org.hisp.dhis.commons.util.TextUtils.replace;
import static org.hisp.dhis.gist.GistLogic.attributePath;
import static org.hisp.dhis.gist.GistLogic.getBaseType;
import static org.hisp.dhis.gist.GistLogic.isAccessProperty;
import static org.hisp.dhis.gist.GistLogic.isAttributeFlagProperty;
import static org.hisp.dhis.gist.GistLogic.isAttributeValuesAttributePropertyPath;
import static org.hisp.dhis.gist.GistLogic.isAttributeValuesProperty;
import static org.hisp.dhis.gist.GistLogic.isCollectionSizeFilter;
import static org.hisp.dhis.gist.GistLogic.isHrefProperty;
import static org.hisp.dhis.gist.GistLogic.isJsonCollectionFilter;
import static org.hisp.dhis.gist.GistLogic.isNestedPath;
import static org.hisp.dhis.gist.GistLogic.isPersistentCollectionField;
import static org.hisp.dhis.gist.GistLogic.isPersistentReferenceField;
import static org.hisp.dhis.gist.GistLogic.isStringLengthFilter;
import static org.hisp.dhis.gist.GistLogic.parentPath;
import static org.hisp.dhis.gist.GistLogic.pathOnSameParent;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.Attribute.ObjectType;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.gist.GistQuery.Comparison;
import org.hisp.dhis.gist.GistQuery.Field;
import org.hisp.dhis.gist.GistQuery.Filter;
import org.hisp.dhis.gist.GistQuery.Owner;
import org.hisp.dhis.jsontree.JsonNode;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.RelativePropertyContext;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.annotation.Gist.Transform;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * Purpose of this helper is to avoid passing around same state while building the HQL query and to
 * setup post processing of results.
 *
 * <p>Usage:
 *
 * <ol>
 *   <li>Use {@link #buildFetchHQL()} to create the HQL query
 *   <li>Use {@link #transform(List)} on the result rows when querying selected columns
 * </ol>
 *
 * <p>Within the HQL naming conventions are:
 *
 * <pre>
 *   o => owner table
 *   e => member collection element table
 * </pre>
 *
 * @author Jan Bernitt
 */
@Slf4j
@RequiredArgsConstructor
final class GistBuilder {
  private static final String GIST_PATH = "/gist";

  /**
   * Defines the functions the builder needs to be able to run that depend on other parts of the
   * system.
   */
  interface GistBuilderSupport {
    List<String> getUserGroupIdsByUserId(String userId);

    Attribute getAttributeById(String attributeId);

    Object getTypedAttributeValue(Attribute attribute, String value);
  }

  /**
   * HQL does not allow plain "null" in select columns list as the type is unknown. Therefore we
   * just cast it to some simple type. Which is not important as the value will be {@code null}
   * anyway.
   */
  private static final String HQL_NULL = "cast(null as char)";

  private static final String TRANSLATIONS_PROPERTY = "translations";

  private static final String ID_PROPERTY = "id";

  private static final String SHARING_PROPERTY = "sharing";

  private static final String ATTRIBUTES_PROPERTY = "attributeValues";

  private static final String OBJECT_TYPES = "objectTypes";

  static GistBuilder createFetchBuilder(
      GistQuery query,
      RelativePropertyContext context,
      GistAccessControl access,
      GistBuilderSupport support) {
    return new GistBuilder(access, addSupportFields(query, context), context, support);
  }

  static GistBuilder createCountBuilder(
      GistQuery query,
      RelativePropertyContext context,
      GistAccessControl access,
      GistBuilderSupport support) {
    return new GistBuilder(access, query, context, support);
  }

  private final GistAccessControl access;

  private final GistQuery query;

  private final RelativePropertyContext context;

  private final GistBuilderSupport support;

  private final List<Consumer<Object[]>> fieldResultTransformers = new ArrayList<>();

  private final Map<String, Integer> fieldIndexByPath = new HashMap<>();

  /**
   * Depending on what fields should be listed other fields are needed to fully compute the
   * requested fields. Such fields are added should they not be present already. This is done only
   * within the builder. While the fields are fetched from the database the caller does not include
   * the added fields as it is still working with the original field list.
   */
  private static GistQuery addSupportFields(GistQuery query, RelativePropertyContext context) {
    GistQuery extended = query;
    for (Field f : query.getFields()) {
      extended = addSupportFields(extended, context, f);
    }
    return extended;
  }

  private static GistQuery addSupportFields(
      GistQuery query, RelativePropertyContext context, Field f) {
    if (Field.REFS_PATH.equals(f.getPropertyPath())) {
      return query;
    }

    // attribute fields? => make sure we have attributeValues
    if (f.isAttribute() && !existsSameParentField(query, f, ATTRIBUTES_PROPERTY)) {
      return f.getTransformation() == Transform.PLUCK
          ? query
          : query.withField(pathOnSameParent(f.getPropertyPath(), ATTRIBUTES_PROPERTY));
    }

    Property p = context.resolveMandatory(f.getPropertyPath());

    // ID column not present but ID column required?
    if ((isPersistentCollectionField(p) || isHrefProperty(p))
        && !existsSameParentField(query, f, ID_PROPERTY)) {
      return query.withField(pathOnSameParent(f.getPropertyPath(), ID_PROPERTY));
    }

    // translatable fields? => make sure we have translations
    if ((query.isTranslate() || f.isTranslate())
        && p.isTranslatable()
        && !existsSameParentField(query, f, TRANSLATIONS_PROPERTY)) {
      return query.withField(pathOnSameParent(f.getPropertyPath(), TRANSLATIONS_PROPERTY));
    }

    // Access based on Sharing
    if (isAccessProperty(p) && !existsSameParentField(query, f, SHARING_PROPERTY)) {
      return query.withField(pathOnSameParent(f.getPropertyPath(), SHARING_PROPERTY));
    }

    // flags on Attribute map to/from objectTypes set
    if (query.getElementType() == Attribute.class
        && isAttributeFlagProperty(p)
        && !existsSameParentField(query, f, OBJECT_TYPES)) {
      return query.withField(pathOnSameParent(f.getPropertyPath(), OBJECT_TYPES));
    }

    return addFromTransformationSupportFields(query, f);
  }

  private static GistQuery addFromTransformationSupportFields(GistQuery query, Field f) {
    if (f.getTransformation() == Transform.FROM) {
      for (String propertyName : f.getTransformationArgument().split(",")) {
        if (!existsSameParentField(query, f, propertyName)) {
          query = query.withField(pathOnSameParent(f.getPropertyPath(), propertyName));
        }
      }
    }
    return query;
  }

  private static boolean existsSameParentField(GistQuery query, Field field, String property) {
    String parentPath = parentPath(field.getPropertyPath());
    String requiredPath = parentPath.isEmpty() ? property : parentPath + "." + property;
    return query.getFields().stream().anyMatch(f -> f.getPropertyPath().equals(requiredPath));
  }

  private String getMemberPath(String property) {
    List<Property> path = context.resolvePath(property);
    return path.size() == 1
        ? path.get(0).getFieldName()
        : path.stream().map(Property::getFieldName).collect(joining("."));
  }

  /*
   * SQL response post processing...
   */

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class IdObject {
    @JsonProperty final String id;
  }

  public List<?> transform(List<?> rows) {
    if (fieldResultTransformers.isEmpty() || rows.isEmpty()) {
      return rows;
    }
    @SuppressWarnings("unchecked")
    List<Object> rowsObjects = (List<Object>) rows;
    for (int i = 0; i < rowsObjects.size(); i++) {
      Object rowValue = rowsObjects.get(i);
      if (rowValue != null && rowValue.getClass() == Object[].class) {
        Object[] row = (Object[]) rowValue;
        for (Consumer<Object[]> transformer : fieldResultTransformers) {
          transformer.accept(row);
        }
      } else if (rowValue != null) {
        Object[] row = new Object[] {rowValue};
        for (Consumer<Object[]> transformer : fieldResultTransformers) {
          transformer.accept(row);
        }
        rowsObjects.set(i, row[0]);
      }
    }
    return rowsObjects;
  }

  private void addTransformer(Consumer<Object[]> transformer) {
    fieldResultTransformers.add(transformer);
  }

  private Object attributeValue(String attributeUid, Object attributeValues, Attribute attribute) {
    AttributeValues values = (AttributeValues) attributeValues;
    String value = values.get(attributeUid);
    return attribute != null ? support.getTypedAttributeValue(attribute, value) : value;
  }

  private Map<String, String> attributeValues(Object attributeValues) {
    return attributeValues instanceof AttributeValues attrs ? attrs.toMap() : Map.of();
  }

  @SuppressWarnings("unchecked")
  private boolean isObjectTypeAttribute(String name, Object objectTypes) {
    Set<String> set = (Set<String>) objectTypes;
    return set != null && set.contains(name);
  }

  private Object translate(Object value, String property, Object translations) {
    @SuppressWarnings("unchecked")
    Set<Translation> list = (Set<Translation>) translations;
    if (list == null || list.isEmpty()) {
      return value;
    }
    String locale = query.getTranslationLocale().toString();
    for (Translation t : list) {
      if (t.getLocale().equalsIgnoreCase(locale)
          && t.getProperty().equalsIgnoreCase(property)
          && !t.getValue().isEmpty()) return t.getValue();
    }
    String lang = query.getTranslationLocale().getLanguage();
    for (Translation t : list) {
      if (t.getLocale().startsWith(lang)
          && t.getProperty().equalsIgnoreCase(property)
          && !t.getValue().isEmpty()) return t.getValue();
    }
    return value;
  }

  /*
   * HQL query building...
   */

  public String buildFetchHQL() {
    Owner owner = query.getOwner();
    Map<String, String> variables =
        Map.ofEntries(
            entry("fields", createFieldsHQL()),
            entry("table", query.getElementType().getSimpleName()),
            entry("filter", createFiltersHQL()),
            entry("access", createAccessFilterHQL(context, "e")),
            entry("order", createOrderByHQL()));
    if (owner == null) {
      return replace(
          "select ${fields} from ${table} e where (${filter}) and (${access}) order by ${order}",
          variables);
    }
    String property =
        context
            .switchedTo(owner.getType())
            .resolveMandatory(owner.getCollectionProperty())
            .getFieldName();
    variables =
        merge(
            variables,
            Map.ofEntries(
                entry("owner", owner.getType().getSimpleName()), entry("property", property)));
    if (!query.isInverse()) {
      return replace(
          "select ${fields} from ${owner} o inner join o.${property} as e where o.uid = :OwnerId and (${filter}) and (${access}) order by ${order}",
          variables);
    }
    return replace(
        "select ${fields} from ${owner} o, ${table} e where o.uid = :OwnerId and e not in elements(o.${property}) and (${filter}) and (${access}) order by ${order}",
        variables);
  }

  public String buildCountHQL() {
    Map<String, String> variables =
        Map.ofEntries(
            entry("table", query.getElementType().getSimpleName()),
            entry("filter", createFiltersHQL()),
            entry("access", createAccessFilterHQL(context, "e")));
    Owner owner = query.getOwner();
    if (owner == null) {
      return replace(
          "select count(*) from ${table} e where (${filter}) and (${access})", variables);
    }
    String property =
        context
            .switchedTo(owner.getType())
            .resolveMandatory(owner.getCollectionProperty())
            .getFieldName();
    variables =
        merge(
            variables,
            Map.ofEntries(
                entry("owner", owner.getType().getSimpleName()), entry("property", property)));
    if (!query.isInverse()) {
      return replace(
          "select count(*) from ${owner} o left join o.${property} as e where o.uid = :OwnerId and (${filter}) and (${access})",
          variables);
    }
    return replace(
        "select count(*) from ${owner} o, ${table} e where o.uid = :OwnerId and e not in elements(o.${property}) and (${filter}) and (${access})",
        variables);
  }

  private String createAccessFilterHQL(RelativePropertyContext context, String alias) {
    if (!isFilterBySharing(context)) {
      return "1=1";
    }
    return access.createAccessFilterHQL(alias);
  }

  private boolean isFilterBySharing(RelativePropertyContext context) {
    Property sharing = context.resolve(SHARING_PROPERTY);
    return sharing != null && sharing.isPersisted() && !access.isSuperuser();
  }

  private String createFieldsHQL() {
    int i = 0;
    for (Field f : query.getFields()) {
      fieldIndexByPath.put(f.getPropertyPath(), i++);
    }
    return join(query.getFields(), ", ", "e", this::createFieldHQL);
  }

  private String createFieldHQL(int index, Field field) {
    String path = field.getPropertyPath();
    if (Field.REFS_PATH.equals(path)) {
      return HQL_NULL;
    }
    if (field.isAttribute()) {
      Attribute attribute = query.isTypedAttributeValues() ? support.getAttributeById(path) : null;
      if (field.getTransformation() == Transform.PLUCK) {
        if (attribute != null) {
          addTransformer(
              row -> row[index] = support.getTypedAttributeValue(attribute, (String) row[index]));
        }
        return "jsonb_extract_path_text(e.attributeValues, '"
            + field.getPropertyPath()
            + "', 'value')";
      }
      int attrValuesFieldIndex = getSameParentFieldIndex("", ATTRIBUTES_PROPERTY);
      addTransformer(
          row -> row[index] = attributeValue(path, row[attrValuesFieldIndex], attribute));
      return HQL_NULL;
    }
    Property property = context.resolveMandatory(path);
    if (isAttributeValuesProperty(property)) {
      addTransformer(row -> row[index] = attributeValues(row[index]));
    }
    if (query.getElementType() == Attribute.class && isAttributeFlagProperty(property)) {
      int objectTypesFieldIndex = getSameParentFieldIndex(path, OBJECT_TYPES);
      String name =
          stream(ObjectType.values())
              .filter(type -> type.getPropertyName().equals(property.getName()))
              .map(ObjectType::name)
              .findFirst()
              .orElse("");
      addTransformer(row -> row[index] = isObjectTypeAttribute(name, row[objectTypesFieldIndex]));
      return HQL_NULL;
    }
    if (query.isTranslate() && property.isTranslatable() && query.getTranslationLocale() != null) {
      int translationsFieldIndex = getSameParentFieldIndex(path, TRANSLATIONS_PROPERTY);
      addTransformer(
          row ->
              row[index] =
                  translate(row[index], property.getTranslationKey(), row[translationsFieldIndex]));
    }
    if (isHrefProperty(property)) {
      String endpointRoot = getSameParentEndpointRoot(path);
      Integer idFieldIndex = getSameParentFieldIndex(path, ID_PROPERTY);
      if (idFieldIndex != null && endpointRoot != null) {
        addTransformer(row -> row[index] = toEndpointURL(endpointRoot, row[idFieldIndex]));
      }
      return HQL_NULL;
    }
    if (isAccessProperty(property)) {
      int sharingFieldIndex = getSameParentFieldIndex(path, SHARING_PROPERTY);
      @SuppressWarnings("unchecked")
      Class<? extends IdentifiableObject> objType =
          (Class<? extends IdentifiableObject>)
              (!isNestedPath(path) ? query.getElementType() : property.getKlass());
      addTransformer(
          row -> row[index] = access.asAccess(objType, (Sharing) row[sharingFieldIndex]));
      return HQL_NULL;
    }
    if (field.getTransformation() == Transform.FROM) {
      createFromTransformedFieldHQL(index, field, path, property);
      return HQL_NULL;
    }
    if (isPersistentReferenceField(property)) {
      if (PeriodType.class.isAssignableFrom(property.getKlass())) {
        addTransformer(
            row -> row[index] = row[index] == null ? null : ((PeriodType) row[index]).getName());
      }
      return createReferenceFieldHQL(index, field);
    }
    if (isPersistentCollectionField(property)) {
      return createCollectionFieldHQL(index, field);
    }
    if (property.isCollection() && property.getOwningRole() != null) {
      return "size(e." + getMemberPath(path) + ")";
    }
    String memberPath = getMemberPath(path);
    return "e." + memberPath;
  }

  private void createFromTransformedFieldHQL(
      int index, Field field, String path, Property property) {
    Object bean = newQueryElementInstance();
    if (bean == null) {
      return;
    }
    String[] sources = field.getTransformationArgument().split(",");
    List<Method> setters =
        stream(sources).map(context::resolveMandatory).map(Property::getSetterMethod).toList();
    int[] indexes =
        stream(sources)
            .mapToInt(srcProperty -> getSameParentFieldIndex(path, srcProperty))
            .toArray();
    Method getter = property.getGetterMethod();
    addTransformer(
        row -> {
          try {
            for (int i = 0; i < indexes.length; i++) {
              setters.get(i).invoke(bean, row[indexes[i]]);
            }
            row[index] = getter.invoke(bean);
          } catch (Exception ex) {
            log.debug("Failed to perform from transformation", ex);
          }
        });
  }

  private String createReferenceFieldHQL(int index, Field field) {
    String path = field.getPropertyPath();
    Property property = context.resolveMandatory(path);
    Class<?> table = property.getKlass();
    RelativePropertyContext fieldContext = context.switchedTo(table);
    String propertyName = determineReferenceProperty(field, fieldContext, false);
    Schema propertySchema = fieldContext.getHome();
    Map<String, String> variables =
        Map.ofEntries(
            entry("alias", alias(table, index)),
            entry("table", table.getSimpleName()),
            entry("path", getMemberPath(path)));
    if (propertyName == null || propertySchema.getRelativeApiEndpoint() == null) {
      // embed the object directly
      if (!property.isRequired()) {
        return replace(
            "(select ${alias} from ${table} ${alias} where ${alias} = e.${path})", variables);
      }
      return replace("e.${path}", variables);
    }
    variables = merge(variables, Map.of("property", propertyName));

    if (property.isIdentifiableObject()) {
      String endpointRoot = getEndpointRoot(property);
      if (endpointRoot != null && query.isReferences()) {
        int refIndex = fieldIndexByPath.get(Field.REFS_PATH);
        addTransformer(
            row ->
                addEndpointURL(
                    row,
                    refIndex,
                    field,
                    isNullOrEmpty(row[index]) ? null : toEndpointURL(endpointRoot, row[index])));
      }
    }

    if (field.getTransformation() == Transform.ID_OBJECTS) {
      addTransformer(row -> row[index] = toIdObject(row[index]));
    }
    if (property.isRequired()) {
      return replace("e.${path}.${property}", variables);
    }
    return replace(
        "(select ${alias}.${property} from ${table} ${alias} where ${alias} = e.${path})",
        variables);
  }

  private String createCollectionFieldHQL(int index, Field field) {
    String path = field.getPropertyPath();
    Property property = context.resolveMandatory(path);
    String endpointRoot = getSameParentEndpointRoot(path);
    if (endpointRoot != null && query.isReferences()) {
      int idFieldIndex = getSameParentFieldIndex(path, ID_PROPERTY);
      int refIndex = fieldIndexByPath.get(Field.REFS_PATH);
      addTransformer(
          row ->
              addEndpointURL(
                  row,
                  refIndex,
                  field,
                  isNullOrEmpty(row[index])
                      ? null
                      : toEndpointURL(endpointRoot, row[idFieldIndex], property)));
    }

    Transform transform = field.getTransformation();
    switch (transform) {
      default:
      case AUTO:
      case NONE:
        return HQL_NULL;
      case SIZE:
        return createSizeTransformerHQL(index, field, property);
      case IS_EMPTY:
        return createIsEmptyTransformerHQL(field);
      case IS_NOT_EMPTY:
        return createIsNotEmptyTransformerHQL(field);
      case NOT_MEMBER:
        return createHasMemberTransformerHQL(index, field, property, "=0");
      case MEMBER:
        return createHasMemberTransformerHQL(index, field, property, ">0");
      case ID_OBJECTS:
        addTransformer(row -> row[index] = toIdObjects(row[index]));
        return createIdsTransformerHQL(index, field, property);
      case IDS:
        return createIdsTransformerHQL(index, field, property);
      case PLUCK:
        return createPluckTransformerHQL(index, field, property);
    }
  }

  private String createSizeTransformerHQL(int index, Field field, Property property) {
    Class<?> table = property.getItemKlass();
    String alias = alias(table, index);
    RelativePropertyContext fieldContext = context.switchedTo(table);

    Map<String, String> variables =
        Map.ofEntries(entry("alias", alias), entry("path", getMemberPath(field.getPropertyPath())));
    if (!isFilterBySharing(fieldContext)) {
      // generates better SQL in case no access control is needed
      return replace("size(e.${path})", variables);
    }
    variables =
        merge(
            variables,
            Map.ofEntries(
                entry("table", table.getSimpleName()),
                entry("access", createAccessFilterHQL(fieldContext, alias))));
    return replace(
        "(select count(*) from ${table} ${alias} where ${alias} in elements(e.${path}) and ${access})",
        variables);
  }

  private String createIsNotEmptyTransformerHQL(Field field) {
    return replace(
        "e.${path} is not empty", Map.of("path", getMemberPath(field.getPropertyPath())));
  }

  private String createIsEmptyTransformerHQL(Field field) {
    return replace("e.${path} is empty", Map.of("path", getMemberPath(field.getPropertyPath())));
  }

  private String createIdsTransformerHQL(int index, Field field, Property property) {
    return createPluckTransformerHQL(index, field, property);
  }

  private String createPluckTransformerHQL(int index, Field field, Property property) {
    String plucked = field.getTransformationArgument();
    Class<?> table = property.getItemKlass();
    RelativePropertyContext itemContext = context.switchedTo(table);
    List<Property> pluckedProperties =
        plucked == null || plucked.isEmpty()
            ? List.of()
            : Stream.of(field.getTransformationArgument().split(","))
                .map(itemContext::resolveMandatory)
                .toList();
    if (pluckedProperties.size() > 1
        || pluckedProperties.stream().anyMatch(p -> p.getKlass() != String.class)) {
      return createMultiPluckTransformerHQL(index, field, property);
    }
    String propertyName = determineReferenceProperty(field, itemContext, true);
    if (propertyName == null || table == Period.class) {
      // give up
      return createSizeTransformerHQL(index, field, property);
    }
    String alias = alias(table, index);
    Map<String, String> variables =
        Map.ofEntries(
            entry("alias", alias),
            entry("table", table.getSimpleName()),
            entry("property", propertyName),
            entry("path", getMemberPath(field.getPropertyPath())),
            entry("access", createAccessFilterHQL(itemContext, alias)));
    return replace(
        "(select array_agg(${alias}.${property}) from ${table} ${alias} where ${alias} in elements(e.${path}) and ${access})",
        variables);
  }

  private String createMultiPluckTransformerHQL(int index, Field field, Property property) {
    Class<?> table = property.getItemKlass();
    RelativePropertyContext itemContext = context.switchedTo(table);
    List<Property> plucked =
        Stream.of(field.getTransformationArgument().split(","))
            .map(itemContext::resolveMandatory)
            .toList();

    Function<Property, String> path =
        p ->
            p.getFieldName()
                + (IdentifiableObject.class.isAssignableFrom(p.getKlass()) ? ".uid" : "");
    String alias = alias(table, index);
    String pluckedObj =
        plucked.stream()
            .map(p -> String.format("'%3$s', %1$s.%2$s", alias, path.apply(p), p.getName()))
            .collect(joining(","));

    addTransformer(
        row ->
            row[index] =
                row[index] == null
                    ? null
                    : Stream.of((String[]) row[index]).map(JsonNode::of).toArray(JsonNode[]::new));

    Map<String, String> variables =
        Map.ofEntries(
            entry("alias", alias),
            entry("table", table.getSimpleName()),
            entry("pluck", pluckedObj),
            entry("path", getMemberPath(field.getPropertyPath())),
            entry("access", createAccessFilterHQL(itemContext, alias)));
    return replace(
        "(select array_agg(json_build_object(${pluck})) from ${table} ${alias} where ${alias} in elements(e.${path}) and ${access})",
        variables);
  }

  private String determineReferenceProperty(
      Field field, RelativePropertyContext fieldContext, boolean forceTextual) {
    Class<?> fieldType = fieldContext.getHome().getKlass();
    if (field.getTransformationArgument() != null) {
      return getPluckPropertyName(field, fieldType, forceTextual);
    }
    if (fieldType == PeriodType.class) {
      // this is how HQL refers to discriminator property, here "name"
      return "class";
    }
    if (existsAsReference(fieldContext, "id")) {
      return fieldContext.resolveMandatory("id").getFieldName();
    }
    if (existsAsReference(fieldContext, "code")) {
      return fieldContext.resolveMandatory("code").getFieldName();
    }
    if (existsAsReference(fieldContext, "name")) {
      return fieldContext.resolveMandatory("name").getFieldName();
    }
    return null;
  }

  private boolean existsAsReference(RelativePropertyContext fieldContext, String id) {
    Property p = fieldContext.resolve(id);
    return p != null && p.isPersisted();
  }

  private String getPluckPropertyName(Field field, Class<?> ownerType, boolean forceTextual) {
    return getPluckPropertyName(field.getTransformationArgument(), ownerType, forceTextual);
  }

  private String getPluckPropertyName(
      String propertyName, Class<?> ownerType, boolean forceTextual) {
    Property property = context.switchedTo(ownerType).resolveMandatory(propertyName);
    if (forceTextual && property.getKlass() != String.class) {
      throw new UnsupportedOperationException(
          "Only textual properties can be plucked, but "
              + propertyName
              + " is a: "
              + property.getKlass());
    }
    return propertyName;
  }

  private String createHasMemberTransformerHQL(
      int index, Field field, Property property, String compare) {
    Class<?> table = property.getItemKlass();
    String alias = alias(table, index);
    Map<String, String> variables =
        Map.ofEntries(
            entry("access", createAccessFilterHQL(context.switchedTo(table), alias)),
            entry("alias", alias),
            entry("table", table.getSimpleName()),
            entry("path", getMemberPath(field.getPropertyPath())),
            entry("property", field.getPropertyPath()),
            entry("compare", compare));
    return replace(
        "(select count(*) ${compare} from ${table} ${alias} where ${alias} in elements(e.${path}) and ${alias}.uid = :p_${property} and ${access})",
        variables);
  }

  @SuppressWarnings("unchecked")
  private void addEndpointURL(Object[] row, int refIndex, Field field, String url) {
    if (url == null || url.isEmpty()) {
      return;
    }
    if (row[refIndex] == null) {
      row[refIndex] = new TreeMap<>();
    }
    ((Map<String, String>) row[refIndex]).put(field.getName(), url);
  }

  private String toEndpointURL(String endpointRoot, Object id) {
    return id == null ? null : endpointRoot + '/' + id + GIST_PATH + getEndpointUrlParams();
  }

  private String toEndpointURL(String endpointRoot, Object id, Property property) {
    return endpointRoot + '/' + id + '/' + property.key() + GIST_PATH + getEndpointUrlParams();
  }

  private String getEndpointUrlParams() {
    return query.isAbsoluteUrls() ? "?absoluteUrls=true" : "";
  }

  private static IdObject toIdObject(Object id) {
    return id == null ? null : new IdObject((String) id);
  }

  private static Object[] toIdObjects(Object ids) {
    return isNullOrEmpty(ids) ? null : Arrays.stream(((String[]) ids)).map(IdObject::new).toArray();
  }

  private static boolean isNullOrEmpty(Object obj) {
    return obj == null
        || obj instanceof Object[] && ((Object[]) obj).length == 0
        || obj instanceof Number && ((Number) obj).intValue() == 0;
  }

  private Integer getSameParentFieldIndex(String path, String property) {
    return fieldIndexByPath.get(pathOnSameParent(path, property));
  }

  private String getSameParentEndpointRoot(String path) {
    return getEndpointRoot(context.switchedTo(path).getHome());
  }

  private String getEndpointRoot(Property property) {
    return getEndpointRoot(context.switchedTo(property.getKlass()).getHome());
  }

  private String getEndpointRoot(Schema schema) {
    String relativeApiEndpoint = schema.getRelativeApiEndpoint();
    return relativeApiEndpoint == null ? null : query.getEndpointRoot() + relativeApiEndpoint;
  }

  private String createFiltersHQL() {
    String rootJunction = query.isAnyFilter() ? " or " : " and ";
    List<Filter> filters = query.getFilters();
    if (!query.hasFilterGroups()) {
      return join(filters, rootJunction, "1=1", this::createFilterHQL);
    }
    String groupJunction = query.isAnyFilter() ? " and " : " or ";
    Map<Integer, List<Filter>> grouped =
        filters.stream().collect(groupingBy(Filter::getGroup, toList()));
    StringBuilder hql = new StringBuilder();
    for (Entry<Integer, List<Filter>> group : grouped.entrySet()) {
      if (!group.getValue().isEmpty()) {
        hql.append('(');
        for (Filter f : group.getValue()) {
          int index = filters.indexOf(f);
          hql.append(createFilterHQL(index, f));
          hql.append(group.getKey() >= 0 ? groupJunction : rootJunction);
        }
        hql.append("1=1");
        hql.append(')');
      }
      hql.append(rootJunction);
    }
    hql.append("1=1");
    return hql.toString().replaceAll("(?: and | or )1=1", "");
  }

  private String createFilterHQL(int index, Filter filter) {
    String propertyPath = filter.getPropertyPath();
    if (isAttributeValuesAttributePropertyPath(propertyPath)) {
      filter = filter.withPropertyPath(attributePath(propertyPath));
      return replace(
          "jsonb_exists_any(e.attributeValues, (select array_agg(uid) from Attribute a where ${filter})) = true",
          Map.of("filter", createFilterHQL(index, filter, "a." + filter.getPropertyPath())));
    }
    if (isNestedPath(propertyPath)) {
      List<Property> path = context.resolvePath(propertyPath);
      if (filter.isSubSelect()) {
        return createSubSelectFilterHQL(index, filter, path);
      }
      if (isExistsInCollectionFilter(path)) {
        return createExistsFilterHQL(index, filter, path);
      }
    }
    String memberPath = filter.isAttribute() ? ATTRIBUTES_PROPERTY : getMemberPath(propertyPath);
    return createFilterHQL(index, filter, "e." + memberPath);
  }

  private String createSubSelectFilterHQL(int index, Filter filter, List<Property> path) {
    Property relation = path.get(0);
    Class<?> table = relation.getKlass();
    String alias = alias(table, index);
    Map<String, String> variables =
        Map.ofEntries(
            entry("alias", alias),
            entry("property", relation.getFieldName()),
            entry("table", table.getSimpleName()),
            entry(
                "filter",
                createFilterHQL(index, filter, alias + "." + path.get(1).getFieldName())));
    if (relation.isCollection())
      return replace("exists (select 1 from e.${property} ${alias} where ${filter})", variables);
    return replace(
        "exists (select 1 from ${table} ${alias} where ${alias} = e.${property} and ${filter})",
        variables);
  }

  private boolean isExistsInCollectionFilter(List<Property> path) {
    return path.size() == 2 && isPersistentCollectionField(path.get(0))
        || path.size() == 3
            && isPersistentReferenceField(path.get(0))
            && isPersistentCollectionField(path.get(1));
  }

  private String createExistsFilterHQL(int index, Filter filter, List<Property> path) {
    Property compared = path.get(path.size() - 1);
    Property collection = path.get(path.size() - 2);
    Class<?> table = collection.getItemKlass();
    String alias = alias(table, index);
    String pathToCollection =
        path.size() == 2
            ? path.get(0).getFieldName()
            : path.get(0).getFieldName() + "." + path.get(1).getFieldName();
    Map<String, String> variables =
        Map.ofEntries(
            entry("table", table.getSimpleName()),
            entry("alias", alias),
            entry("path", pathToCollection),
            entry("filter", createFilterHQL(index, filter, alias + "." + compared.getFieldName())));
    return replace(
        "exists (select 1 from ${table} ${alias} where ${alias} in elements(e.${path}) and ${filter})",
        variables);
  }

  private String createFilterHQL(int index, Filter filter, String field) {
    Comparison operator = filter.getOperator();
    if (operator.isAccessCompare()) {
      return createAccessFilterHQL(index, filter, field);
    }
    String template = "${property}";
    if (filter.isAttribute()) {
      template =
          "jsonb_extract_path_text(${property}, '" + filter.getPropertyPath() + "', 'value')";
    } else {
      Property property = context.resolveMandatory(filter.getPropertyPath());
      if (property.getPropertyType() == PropertyType.PASSWORD)
        throw new IllegalQueryException("Filter not allowed: " + filter);
      if (isStringLengthFilter(filter, property)) {
        template = "length(${property})";
      } else if (isCollectionSizeFilter(filter, property)) {
        template = "size(${property})";
      } else if (isJsonCollectionFilter(filter, property)) {
        return createJsonCollectionFilterHQL(filter, field);
      } else if (operator.isCaseInsensitive()) {
        template = "lower(${property})";
      }
    }
    String param = ":f_" + index;
    Map<String, String> variables =
        Map.ofEntries(
            entry("left", replace(template, Map.of("property", field))),
            entry("op", operator.getSql()),
            entry("right", operator.isMultiValue() ? "(" + param + ")" : param));
    return operator.isUnary()
        ? replace("${left} ${op}", variables)
        : replace("${left} ${op} ${right}", variables);
  }

  private String createJsonCollectionFilterHQL(Filter filter, String property) {
    Map<String, String> variables = Map.of("property", property);
    return switch (filter.getOperator()) {
      case EMPTY ->
          replace(
              "(${property} is null or ${property} = '{}' or ${property} = '[]' or ${property} = 'null')",
              variables);
      case NOT_EMPTY ->
          replace(
              "(${property} is not null and ${property} != 'null' and not (${property} = '[]' or ${property} = '{}'))",
              variables);
      default ->
          throw new UnsupportedOperationException(
              "Filter %s not supported for property %s since it is stored as JSONB"
                  .formatted(filter.getOperator(), filter.getPropertyPath()));
    };
  }

  private String createAccessFilterHQL(int index, Filter filter, String property) {
    String path = filter.getPropertyPath();
    Property p = context.resolveMandatory(path);
    Class<?> table = p.getItemKlass();
    String alias = alias(table, index);
    Map<String, String> variables =
        Map.ofEntries(
            entry("alias", alias),
            entry("table", table.getSimpleName()),
            entry("property", property),
            entry("filter", createAccessFilterHQL(filter, alias)));
    if (isPersistentCollectionField(p) && IdentifiableObject.class.isAssignableFrom(table)) {
      return replace(
          "exists (select ${alias} from ${table} ${alias} where ${alias} in elements(${property}) and ${filter})",
          variables);
    }
    if (isPersistentReferenceField(p) && p.isIdentifiableObject()) {
      variables = merge(variables, Map.of("table", p.getKlass().getSimpleName()));
      return replace(
          "${property} in (select ${alias} from ${table} ${alias} where ${filter})", variables);
    }
    if (isNestedPath(path)) {
      throw new UnsupportedOperationException("Access filter not supported for property: " + path);
    }
    // trivial case: the filter property is a non nested non-identifiable
    // property => access check applied to gist item element
    return createAccessFilterHQL(filter, "e");
  }

  private static String alias(Class<?> table, int uniqueId) {
    return table
            .getSimpleName()
            .chars()
            .filter(Character::isUpperCase)
            .collect(StringBuilder::new, (sb, ch) -> sb.append((char) ch), StringBuilder::append)
            .toString()
        + uniqueId;
  }

  private String createAccessFilterHQL(Filter filter, String alias) {
    String userId = filter.getValue()[0];
    return JpaQueryUtils.generateHqlQueryForSharingCheck(
        alias, getAccessPattern(filter), userId, support.getUserGroupIdsByUserId(userId));
  }

  private String getAccessPattern(Filter filter) {
    switch (filter.getOperator()) {
      default:
      case CAN_READ:
        return AclService.LIKE_READ_METADATA;
      case CAN_WRITE:
        return AclService.LIKE_WRITE_METADATA;
      case CAN_DATA_READ:
        return AclService.LIKE_READ_DATA;
      case CAN_DATA_WRITE:
        return AclService.LIKE_WRITE_DATA;
      case CAN_ACCESS:
        return filter.getValue()[1];
    }
  }

  private String createOrderByHQL() {
    if (query.getOrders() == null || query.getOrders().isEmpty()) {
      List<Property> orderBy =
          context.getHome().getProperties().stream()
              .filter(p -> p.getGistPreferences().getOrder() != null)
              .sorted(comparingInt(a -> a.getGistPreferences().getOrder()))
              .toList();
      if (!orderBy.isEmpty()) {
        return orderBy.stream().map(p -> p.getFieldName() + " asc").collect(joining(", "));
      }
    }
    return join(
        query.getOrders(),
        ",",
        "e.id asc",
        (index, order) ->
            " e."
                + getMemberPath(order.getPropertyPath())
                + " "
                + order.getDirection().name().toLowerCase());
  }

  private <T> String join(
      Collection<T> elements,
      String delimiter,
      String empty,
      BiFunction<Integer, T, String> elementFactory) {
    if (elements == null || elements.isEmpty()) {
      return empty;
    }
    StringBuilder str = new StringBuilder();
    int i = 0;
    for (T e : elements) {
      if (!str.isEmpty()) {
        str.append(delimiter);
      }
      str.append(elementFactory.apply(i++, e));
    }
    return str.toString();
  }

  /*
   * HQL query parameter mapping...
   */

  public void addFetchParameters(
      BiConsumer<String, Object> dest, BiFunction<String, Class<?>, Object> argumentParser) {
    for (Field field : query.getFields()) {
      Transform transformation = field.getTransformation();
      if (field.getTransformationArgument() != null
          && transformation != Transform.PLUCK
          && transformation != Transform.FROM) {
        dest.accept("p_" + field.getPropertyPath(), field.getTransformationArgument());
      }
    }
    addCountParameters(dest, argumentParser);
  }

  public void addCountParameters(
      BiConsumer<String, Object> dest, BiFunction<String, Class<?>, Object> argumentParser) {
    Owner owner = query.getOwner();
    if (owner != null) {
      dest.accept("OwnerId", owner.getId());
    }
    int i = 0;
    for (Filter filter : query.getFilters()) {
      Comparison operator = filter.getOperator();
      if (!operator.isUnary() && !operator.isAccessCompare()) {
        Object value =
            filter.isAttribute() || isAttributeValuesAttributePropertyPath(filter.getPropertyPath())
                ? filter.getValue()[0]
                : getParameterValue(
                    context.resolveMandatory(filter.getPropertyPath()), filter, argumentParser);
        dest.accept(
            "f_" + i,
            operator.isStringCompare()
                ? completeLikeExpression(operator, stringParameterValue(operator, value))
                : value);
      }
      i++;
    }
  }

  private String stringParameterValue(Comparison operator, Object value) {
    return value == null
        ? null
        : operator.isCaseInsensitive() ? value.toString().toLowerCase() : (String) value;
  }

  private Object getParameterValue(
      Property property, Filter filter, BiFunction<String, Class<?>, Object> argumentParser) {
    String[] value = filter.getValue();
    if (value.length == 0) {
      return "";
    }
    if (value.length == 1) {
      return getParameterValue(property, filter, value[0], argumentParser);
    }
    return stream(value)
        .map(e -> getParameterValue(property, filter, e, argumentParser))
        .collect(toList());
  }

  private Object getParameterValue(
      Property property,
      Filter filter,
      String value,
      BiFunction<String, Class<?>, Object> argumentParser) {
    if (isStringLengthFilter(filter, property)) {
      return argumentParser.apply(value, Integer.class);
    }
    if (value == null || property.getKlass() == String.class) {
      return value;
    }
    if (isCollectionSizeFilter(filter, property)) {
      return argumentParser.apply(value, Integer.class);
    }
    Class<?> itemType = getBaseType(property);
    return argumentParser.apply(value, itemType);
  }

  private static Object completeLikeExpression(Comparison operator, String value) {
    switch (operator) {
      case LIKE:
      case ILIKE:
      case NOT_ILIKE:
      case NOT_LIKE:
        return sqlLikeExpressionOf(value);
      case STARTS_LIKE:
      case STARTS_WITH:
      case NOT_STARTS_LIKE:
      case NOT_STARTS_WITH:
        return value + "%";
      case ENDS_LIKE:
      case ENDS_WITH:
      case NOT_ENDS_LIKE:
      case NOT_ENDS_WITH:
        return "%" + value;
      default:
        return value;
    }
  }

  /**
   * Converts the user input of a like pattern matching to the SQL like expression.
   *
   * <p>Like (pattern matching) allows for two modes:
   *
   * <p>1. providing a pattern with wild-card placeholders (* is any string, ? any character)
   *
   * <p>2. providing a string without placeholders to match anywhere
   *
   * @param value user input for like {@link Filter}
   * @return The SQL like expression
   */
  private static String sqlLikeExpressionOf(String value) {
    return value != null && (value.contains("*") || value.contains("?"))
        ? value.replace("*", "%").replace("?", "_")
        : "%" + value + "%";
  }

  private Object newQueryElementInstance() {
    try {
      return context.getHome().getKlass().getConstructor().newInstance();
    } catch (Exception ex) {
      log.warn("Failed to construct from transformation transfer bean instance");
      return null;
    }
  }
}
