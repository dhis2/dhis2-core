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
package org.hisp.dhis.gist;

import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.gist.GistLogic.effectiveTransform;
import static org.hisp.dhis.gist.GistLogic.isAttributePath;
import static org.hisp.dhis.gist.GistLogic.isCollectionSizeFilter;
import static org.hisp.dhis.gist.GistLogic.isIncludedField;
import static org.hisp.dhis.gist.GistLogic.isNonNestedPath;
import static org.hisp.dhis.gist.GistLogic.isPersistentCollectionField;
import static org.hisp.dhis.gist.GistLogic.isPersistentReferenceField;
import static org.hisp.dhis.gist.GistLogic.parentPath;
import static org.hisp.dhis.gist.GistLogic.pathOnSameParent;
import static org.hisp.dhis.schema.PropertyType.COLLECTION;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import lombok.AllArgsConstructor;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.common.PrimaryKeyObject;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.gist.GistQuery.Comparison;
import org.hisp.dhis.gist.GistQuery.Field;
import org.hisp.dhis.gist.GistQuery.Filter;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.RelativePropertyContext;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.annotation.Gist.Transform;
import org.hisp.dhis.user.User;

/**
 * The {@link GistPlanner} is responsible to expand the list of {@link Field}s following the {@link
 * GistQuery} and {@link Property} preferences.
 *
 * @author Jan Bernitt
 */
@AllArgsConstructor
class GistPlanner {
  private final GistQuery query;

  private final RelativePropertyContext context;

  private final GistAccessControl access;

  public GistQuery plan() {
    return query.withFields(planFields()).withFilters(planFilters());
  }

  private List<Field> planFields() {
    List<Field> fields = query.getFields();
    if (fields.isEmpty()) {
      fields = singletonList(Field.ALL);
    }
    fields = withPresetFields(fields); // 1:n
    fields = withAttributeFields(fields); // 1:1
    fields = withDisplayAsTranslatedFields(fields); // 1:1
    fields = withUserNameAsFromTransformedField(fields); // 1:1
    fields = withInnerAsSeparateFields(fields); // 1:n
    fields = withCollectionItemPropertyAsTransformation(fields); // 1:1
    fields = withEffectiveTransformation(fields); // 1:1
    fields = withEndpointsField(fields); // 1:1+1
    return fields;
  }

  private List<Filter> planFilters() {
    List<Filter> filters = query.getFilters();
    filters = withAttributeFilters(filters); // 1:1
    filters = withIdentifiableCollectionAutoIdFilters(filters); // 1:1
    filters = withCurrentUserDefaultForAccessFilters(filters); // 1:1
    return filters;
  }

  private List<Filter> withAttributeFilters(List<Filter> filters) {
    return map1to1(
        filters,
        f -> isAttributePath(f.getPropertyPath()) && context.resolve(f.getPropertyPath()) == null,
        Filter::asAttribute);
  }

  /** Understands {@code collection} property as synonym for {@code collection.id} */
  private List<Filter> withIdentifiableCollectionAutoIdFilters(List<Filter> filters) {
    return map1to1(
        filters,
        this::isCollectionFilterWithoutIdField,
        f -> f.withPropertyPath(f.getPropertyPath() + ".id"));
  }

  private boolean isCollectionFilterWithoutIdField(Filter f) {
    if (f.isAttribute()) {
      return false;
    }
    Property p = context.resolveMandatory(f.getPropertyPath());
    return !f.getOperator().isAccessCompare()
        && p.isCollection()
        && !isCollectionSizeFilter(f, p)
        && PrimaryKeyObject.class.isAssignableFrom(p.getItemKlass());
  }

  private List<Filter> withCurrentUserDefaultForAccessFilters(List<Filter> filters) {
    return map1to1(
        filters,
        GistPlanner::isAccessFilterWithoutUserID,
        f ->
            f.withValue(
                f.getValue().length == 0
                    ? new String[] {access.getCurrentUserUid()}
                    : new String[] {access.getCurrentUserUid(), f.getValue()[0]}));
  }

  private static boolean isAccessFilterWithoutUserID(Filter f) {
    Comparison op = f.getOperator();
    if (!op.isAccessCompare()) {
      return false;
    }
    String[] args = f.getValue();
    return op == Comparison.CAN_ACCESS && args.length <= 1
        || op != Comparison.CAN_ACCESS && args.length == 0;
  }

  private static int propertyTypeOrder(Property a, Property b) {
    if (a.isCollection() && b.isCollection()) {
      return modifiedPropertyTypeOrder(a.getItemPropertyType(), b.getItemPropertyType());
    }
    return modifiedPropertyTypeOrder(a.getPropertyType(), b.getPropertyType());
  }

  private static int modifiedPropertyTypeOrder(PropertyType a, PropertyType b) {
    if (a == COLLECTION && b != COLLECTION) {
      return 1;
    }
    if (b == COLLECTION && a != COLLECTION) {
      return -1;
    }
    return a.compareTo(b);
  }

  private List<Field> withEffectiveTransformation(List<Field> fields) {
    return fields.stream().map(this::withEffectiveTransformation).collect(toList());
  }

  private Field withEffectiveTransformation(Field field) {
    return field.isAttribute()
        ? field.withTransformation(
            field.getTransformation() == Transform.PLUCK ? Transform.PLUCK : Transform.NONE)
        : field.withTransformation(
            effectiveTransform(
                context.resolveMandatory(field.getPropertyPath()),
                query.getDefaultTransformation(),
                field.getTransformation()));
  }

  /**
   * Expands any field presets into individual fields while taking explicitly removed fields into
   * account.
   */
  private List<Field> withPresetFields(List<Field> fields) {
    Set<String> explicit = fields.stream().map(Field::getPropertyPath).collect(toSet());
    List<Field> expanded = new ArrayList<>();
    for (Field f : fields) {
      String path = f.getPropertyPath();
      if (isPresetField(path)) {
        Schema schema = context.getHome();
        Predicate<Property> canRead = getAccessFilter(schema);
        schema.getProperties().stream()
            .filter(getPresetFilter(path))
            .sorted(GistPlanner::propertyTypeOrder)
            .forEach(
                p -> {
                  if (!explicit.contains(p.key())
                      && !explicit.contains("-" + p.key())
                      && !explicit.contains("!" + p.key())) {
                    if (canRead.test(p)) {
                      expanded.add(new Field(p.key(), Transform.AUTO));
                    }
                    addReferenceFields(expanded, path, schema, p);
                  }
                });
      } else if (isExcludeField(path)) {
        expanded.removeIf(field -> field.getPropertyPath().equals(path.substring(1)));
      } else {
        expanded.add(f);
      }
    }
    return expanded;
  }

  private void addReferenceFields(List<Field> expanded, String path, Schema schema, Property p) {
    Schema propertySchema = context.switchedTo(p.getKlass()).getHome();
    if (isPersistentReferenceField(p) && propertySchema.getRelativeApiEndpoint() == null) {
      // reference to an object with no endpoint API
      // => include its fields
      propertySchema.getProperties().stream()
          .filter(getPresetFilter(path))
          .filter(getAccessFilter(propertySchema))
          .sorted(GistPlanner::propertyTypeOrder)
          .forEach(
              rp -> {
                String referencePath = p.key() + "." + rp.key();
                if (canRead(schema, referencePath)) {
                  expanded.add(new Field(referencePath, Transform.AUTO));
                }
              });
    }
  }

  private Predicate<Property> getAccessFilter(Schema schema) {
    return p -> canRead(schema, p.getName());
  }

  @SuppressWarnings("unchecked")
  private boolean canRead(Schema schema, String path) {
    return !schema.isIdentifiableObject()
        || access.canRead((Class<? extends PrimaryKeyObject>) schema.getKlass(), path);
  }

  private List<Field> withAttributeFields(List<Field> fields) {
    return map1to1(
        fields,
        f -> isAttributePath(f.getPropertyPath()) && context.resolve(f.getPropertyPath()) == null,
        Field::asAttribute);
  }

  private List<Field> withDisplayAsTranslatedFields(List<Field> fields) {
    fields =
        map1to1(
            fields,
            f -> isDisplayNameField(f.getPropertyPath()),
            f ->
                f.withAlias(f.getName())
                    .withTranslate()
                    .withPropertyPath(pathOnSameParent(f.getPropertyPath(), "name")));
    return map1to1(
        fields,
        f -> isDisplayShortName(f.getPropertyPath()),
        f ->
            f.withAlias(f.getName())
                .withTranslate()
                .withPropertyPath(pathOnSameParent(f.getPropertyPath(), "shortName")));
  }

  private List<Field> withUserNameAsFromTransformedField(List<Field> fields) {
    return query.getElementType() != User.class
        ? fields
        : map1to1(
            fields,
            f -> f.getPropertyPath().equals("name"),
            f ->
                f.toBuilder()
                    .transformation(Transform.FROM)
                    .transformationArgument("firstName,surname")
                    .build());
  }

  /** Transforms {@code field[a,b]} syntax to {@code field.a,field.b} */
  private List<Field> withInnerAsSeparateFields(List<Field> fields) {
    List<Field> expanded = new ArrayList<>();
    for (Field f : fields) {
      String path = f.getPropertyPath();
      if (path.indexOf('[') >= 0) {
        String outerPath = path.substring(0, path.indexOf('['));
        String innerList = path.substring(path.indexOf('[') + 1, path.lastIndexOf(']'));
        for (String innerFieldName : innerList.split(GistQuery.FIELD_SPLIT)) {
          Field child = Field.parse(innerFieldName);
          expanded.add(
              child
                  .withPropertyPath(outerPath + "." + child.getPropertyPath())
                  .withAlias(
                      (f.getAlias().isEmpty() ? outerPath : f.getAlias()) + "." + child.getName()));
        }
      } else {
        expanded.add(f);
      }
    }
    return expanded;
  }

  private List<Field> withCollectionItemPropertyAsTransformation(List<Field> fields) {
    List<Field> mapped = new ArrayList<>();
    for (Field f : fields) {
      String path = f.getPropertyPath();
      if (!isNonNestedPath(path) && context.resolveMandatory(parentPath(path)).isCollection()) {
        String parentPath = parentPath(path);
        String propertyName = path.substring(path.lastIndexOf('.') + 1);
        Property collection = context.resolveMandatory(parentPath);
        if ("id".equals(propertyName)
            && PrimaryKeyObject.class.isAssignableFrom(collection.getItemKlass())) {
          mapped.add(
              f.withPropertyPath(parentPath).withAlias(path).withTransformation(Transform.IDS));
        } else {
          mapped.add(
              Field.builder()
                  .propertyPath(parentPath)
                  .alias(path)
                  .transformation(Transform.PLUCK)
                  .transformationArgument(propertyName)
                  .build());
        }
      } else {
        mapped.add(f);
      }
    }
    return mapped;
  }

  private List<Field> withEndpointsField(List<Field> fields) {
    if (!query.isReferences()) {
      return fields;
    }
    boolean hasReferences =
        fields.stream()
            .anyMatch(
                field -> {
                  if (field.isAttribute()) {
                    return false;
                  }
                  Property p = context.resolveMandatory(field.getPropertyPath());
                  return isPersistentReferenceField(p) && p.isIdentifiableObject()
                      || isPersistentCollectionField(p);
                });
    if (!hasReferences) {
      return fields;
    }
    ArrayList<Field> extended = new ArrayList<>(fields);
    extended.add(new Field(Field.REFS_PATH, Transform.NONE).withAlias("apiEndpoints"));
    return extended;
  }

  private Predicate<Property> getPresetFilter(String path) {
    if (isAllField(path)) {
      return p -> isIncludedField(p, query.getAutoType());
    }
    if (":identifiable".equals(path)) {
      return getPresetFilter(IdentifiableObject.class);
    }
    if (":nameable".equals(path)) {
      return getPresetFilter(NameableObject.class);
    }
    if (":persisted".equals(path)) {
      return Property::isPersisted;
    }
    if (":owner".equals(path)) {
      return p -> p.isPersisted() && p.isOwner();
    }
    throw new IllegalQueryException(new ErrorMessage(ErrorCode.E2038, path));
  }

  private Predicate<Property> getPresetFilter(Class<?> api) {
    return p ->
        stream(api.getMethods())
            .anyMatch(
                m ->
                    m.getName().equals(p.getGetterMethod().getName())
                        && m.getParameterCount() == 0
                        && p.isPersisted());
  }

  private static boolean isPresetField(String path) {
    return path.startsWith(":") || Field.ALL_PATH.equals(path);
  }

  private static boolean isExcludeField(String path) {
    return path.startsWith("-") || path.startsWith("!");
  }

  private static boolean isAllField(String path) {
    return Field.ALL_PATH.equals(path) || ":*".equals(path) || ":all".equals(path);
  }

  private boolean isDisplayShortName(String path) {
    return path.equals("displayShortName") || path.endsWith(".displayShortName");
  }

  private boolean isDisplayNameField(String path) {
    return path.equals("displayName") || path.endsWith(".displayName");
  }

  private static <T> List<T> map1to1(List<T> from, Predicate<T> when, UnaryOperator<T> then) {
    List<T> mapped = new ArrayList<>(from.size());
    for (T e : from) {
      mapped.add(when.test(e) ? then.apply(e) : e);
    }
    return mapped;
  }
}
