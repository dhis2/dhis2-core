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

import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;
import static org.hisp.dhis.schema.DefaultSchemaService.safeInvoke;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.PropertyPath;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.jsontree.Text;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen
 */
@Component
@RequiredArgsConstructor
public class FieldPathHelper {
  private final SchemaService schemaService;

  public List<FieldPath> apply(List<FieldPath> paths, Class<?> rootKlass) {
    if (paths.isEmpty() || rootKlass == null) {
      return List.of();
    }

    Map<PropertyPath, FieldPath> inclusions =
        paths.stream()
            .filter(not(FieldPath::isPreset).and(not(FieldPath::isExclude)))
            .collect(Collectors.toMap(FieldPath::getPath, Function.identity()));

    applyProperties(inclusions.values(), rootKlass);

    List<FieldPath> presets = paths.stream().filter(FieldPath::isPreset).toList();
    applyPresets(presets, inclusions, rootKlass);

    calculatePathCount(inclusions.values())
        .forEach(
            (path, count) -> {
              if (count <= 1) {
                applyDefaults(inclusions.get(path), rootKlass, inclusions);
              }
            });

    List<FieldPath> exclusions = paths.stream().filter(FieldPath::isExclude).toList();
    applyExclusions(exclusions, inclusions);

    return List.copyOf(inclusions.values());
  }

  /**
   * Applies (recursively) default expansion on the remaining field paths, for example the path
   * 'dataElements.dataElementGroups' would get expanded to 'dataElements.dataElementGroups.id' so
   * that we expose the reference identifier.
   */
  private void applyDefaults(FieldPath path, Class<?> klass, Map<PropertyPath, FieldPath> paths) {
    Schema schema = getSchemaByPath(path.getPath(), klass);

    if (schema == null) return;

    Property property = path.getProperty();
    if (property == null) return;

    if (isComplex(property)) {
      expandComplex(paths, path.getPath(), schema);
    } else if (isReference(property)) {
      expandReference(paths, path.getPath(), schema);
    }
  }

  private void applyDefault(FieldPath path, Map<PropertyPath, FieldPath> paths) {
    paths.put(path.getPath(), path);

    Property property = path.getProperty();
    if (property == null || property.isSimple()) return;

    Schema schema =
        schemaService.getSchema(
            property.isCollection() ? property.getItemKlass() : property.getKlass());

    if (schema == null) return;

    if (isComplex(property)) {
      expandComplex(paths, path.getPath(), schema);
    } else if (isReference(property)) {
      expandReference(paths, path.getPath(), schema);
    }
  }

  private void expandReference(
      Map<PropertyPath, FieldPath> paths, PropertyPath parent, Schema schema) {
    Property idProperty = schema.getProperty("id");

    String name = idProperty.isCollection() ? idProperty.getCollectionName() : idProperty.getName();
    FieldPath id = FieldPath.of(parent.concat(name));
    id.setProperty(idProperty);

    paths.put(id.getPath(), id);
  }

  private void expandComplex(
      Map<PropertyPath, FieldPath> paths, PropertyPath parent, Schema schema) {
    schema
        .getProperties()
        .forEach(
            property -> {
              String name =
                  property.isCollection() ? property.getCollectionName() : property.getName();
              FieldPath path = FieldPath.of(parent.concat(name));
              path.setProperty(property);

              // check if anything else needs to be expanded
              applyDefault(path, paths);
            });
  }

  private void applyProperties(Collection<FieldPath> paths, Class<?> rootKlass) {
    paths.stream()
        .filter(path -> path.getProperty() == null)
        .forEach(
            path -> {
              Schema schema = getSchemaByPath(path.getPath().parent(), rootKlass);

              if (schema != null) {
                path.setProperty(schema.getProperty(path.getPropertyName()));
              }
            });
  }

  /**
   * Applies field presets. See {@link FieldPreset}.
   *
   * @param presets the list of {@link FieldPath}.
   * @param paths mapping of full path and {@link FieldPath} to be populated.
   * @param rootKlass the root class type of the entity.
   */
  public void applyPresets(
      List<FieldPath> presets, Map<PropertyPath, FieldPath> paths, Class<?> rootKlass) {
    Consumer<FieldPath> add = path -> paths.putIfAbsent(path.getPath(), path);
    for (FieldPath preset : presets) {
      PropertyPath parent = preset.getPath().parent();
      Schema schema = getSchemaByPath(parent, rootKlass);

      if (schema == null) {
        continue;
      }
      Text presetName = preset.getPath().segment();
      if (presetName.contentEquals(":all")) {
        schema.getProperties().forEach(p -> add.accept(toFieldPath(parent, p)));
      } else if (presetName.contentEquals(":owner")) {
        schema.getProperties().stream()
            .filter(Property::isOwner)
            .forEach(p -> add.accept(toFieldPath(parent, p)));
      } else if (presetName.contentEquals(":persisted")) {
        schema.getProperties().stream()
            .filter(Property::isPersisted)
            .forEach(p -> add.accept(toFieldPath(parent, p)));
      } else if (presetName.contentEquals(":identifiable")) {
        schema.getProperties().stream()
            .filter(p -> FieldPreset.IDENTIFIABLE.getFields().contains(p.getName()))
            .forEach(p -> add.accept(toFieldPath(parent, p)));
      } else if (presetName.contentEquals(":simple")) {
        schema.getProperties().stream()
            .filter(p -> p.getPropertyType().isSimple())
            .forEach(p -> add.accept(toFieldPath(parent, p)));
      } else if (presetName.contentEquals(":nameable")) {
        schema.getProperties().stream()
            .filter(p -> FieldPreset.NAMEABLE.getFields().contains(p.getName()))
            .forEach(p -> add.accept(toFieldPath(parent, p)));
      }
    }
  }

  public void visitPaths(
      Object root, List<FieldPath> paths, BiConsumer<Object, PropertyPath> visitor) {
    if (root == null || paths.isEmpty()) return;

    Schema schema = schemaService.getSchema(HibernateProxyUtils.getRealClass(root));

    if (!schema.isIdentifiableObject()) return;

    paths.forEach(path -> visitFieldPath(root, path.getPath(), visitor));
  }

  private void visitFieldPath(
      Object object, PropertyPath path, BiConsumer<Object, PropertyPath> visitor) {
    if (object == null) return;

    if (path.parent() == null) {
      visitor.accept(object, path);
      return;
    }

    Schema schema = schemaService.getSchema(HibernateProxyUtils.getRealClass(object));
    Property property = schema.getProperty(path.head().toString());

    if (property == null) return;

    if (property.isCollection()) {
      Collection<?> c = safeInvoke(object, property.getGetterMethod());
      if (c == null || c.isEmpty()) return;
      PropertyPath cPath = path.dropHead();
      for (Object e : c) visitFieldPath(e, cPath, visitor);
    } else {
      Object currentObject = safeInvoke(object, property.getGetterMethod());
      visitFieldPath(currentObject, path.dropHead(), visitor);
    }
  }

  /** Modifies the passed in fieldPathMap by removing any matching exclusions. */
  private void applyExclusions(
      List<FieldPath> exclusions, Map<PropertyPath, FieldPath> fieldPathMap) {
    for (FieldPath exclusion : exclusions) {
      fieldPathMap.keySet().removeIf(path -> fieldShouldBeExcluded(path, exclusion.getPath()));
    }
  }

  // ----------------------------------------------------------------------------------------------------------------
  // Helpers
  // ----------------------------------------------------------------------------------------------------------------

  /**
   * Method that checks whether a field should be excluded or not. <br>
   * Examples: <br>
   *
   * <pre>
   * fullFieldPath   | fullExclusionPath   | outcome
   * ----------------|---------------------|--------------
   * root            | root                | true
   * root.name       | root                | true
   * root.name       | root.name           | true
   * root.name       | name.root           | false
   * root.name       | root.name.last      | false
   * root.name       | name                | false
   * root.name.last  | root.name           | false
   * username        | user                | false
   *
   * </pre>
   *
   * @param fullFieldPath field path that represents the full path of a given field(in dot
   *     notation). It might look like any of the following examples: <br>
   *     <ul>
   *       <li>root
   *       <li>root.name
   *       <li>root.name.last
   *     </ul>
   *
   * @param fullExclusionPath is the actual full exclusion path which should be checked against. It
   *     might look like any of the following examples: <br>
   *     <ul>
   *       <li>root
   *       <li>root.name
   *       <li>root.name.last
   *     </ul>
   *
   * @return true if the field should be excluded <br>
   */
  public static boolean fieldShouldBeExcluded(
      PropertyPath fullFieldPath, PropertyPath fullExclusionPath) {
    if (fullFieldPath == null || fullExclusionPath == null) return false;
    return fullExclusionPath.isExcluded(fullFieldPath);
  }

  private boolean isReference(Property property) {
    return property.is(PropertyType.REFERENCE) || property.itemIs(PropertyType.REFERENCE);
  }

  private boolean isComplex(Property property) {
    return property.is(PropertyType.COMPLEX)
        || property.itemIs(PropertyType.COMPLEX)
        || property.isEmbeddedObject()
        || Sharing.class.isAssignableFrom(property.getKlass())
        || Access.class.isAssignableFrom(property.getKlass())
        || UserAccess.class.isAssignableFrom(property.getKlass())
        || UserGroupAccess.class.isAssignableFrom(property.getKlass());
  }

  /** Calculates a weighted map of paths to find candidates for default expansion. */
  private Map<PropertyPath, Integer> calculatePathCount(Collection<FieldPath> paths) {
    Map<PropertyPath, Integer> pathCount = new HashMap<>();

    for (FieldPath fieldPath : paths) {
      Property property = fieldPath.getProperty();

      if (property == null) {
        continue;
      }

      PropertyPath path = fieldPath.getPath().parent();
      while (path != null) {
        pathCount.compute(path, (key, count) -> count == null ? 1 : count + 1);
        path = path.parent();
      }

      if (isReference(property) || isComplex(property)) {
        pathCount.compute(fieldPath.getPath(), (key, count) -> count == null ? 1 : count + 1);
      }
    }

    return pathCount;
  }

  private FieldPath toFieldPath(@CheckForNull PropertyPath parent, Property property) {
    String name = property.isCollection() ? property.getCollectionName() : property.getName();
    FieldPath res = FieldPath.of(PropertyPath.concat(parent, name));
    res.setProperty(property);
    return res;
  }

  private Schema getSchemaByPath(@CheckForNull PropertyPath path, Class<?> klass) {
    requireNonNull(klass);

    // get root schema
    Schema schema = schemaService.getSchema(klass);

    if (path == null) return schema;

    Property currentProperty;

    Iterator<String> iter = path.properties().map(Text::toString).iterator();
    while (iter.hasNext()) {
      currentProperty = schema.getProperty(iter.next());

      if (currentProperty == null) {
        return null; // invalid path
      }

      if (currentProperty.isCollection()) {
        schema = schemaService.getSchema(currentProperty.getItemKlass());
      } else {
        schema = schemaService.getSchema(currentProperty.getKlass());
      }
    }

    return schema;
  }
}
