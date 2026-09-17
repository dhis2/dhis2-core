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
package org.hisp.dhis.dxf2.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the {@code object=type:id} references of a multi-object dependency export into the
 * {@link IdentifiableObject} roots to export.
 *
 * <p>Every reference is classified before any object is returned, and every problem is collected
 * rather than the first one thrown, so a caller with several bad references learns about all of
 * them in one response.
 *
 * @author David Mackessy
 */
@Service
@RequiredArgsConstructor
public class MetadataDependencyRootResolver {

  /**
   * The dependency traversal is not memoised, so N roots means N full walks of the object graph.
   * This bounds the work a single request can ask for.
   */
  public static final int MAX_ROOTS = 50;

  /**
   * The root types this endpoint currently accepts.
   *
   * <p>Deliberately narrower than {@link MetadataExportService#getSupportedDependencyRootTypes()},
   * which is every type the traversal can walk and which the per-type {@code /{uid}/metadata}
   * endpoints continue to serve in full. The dependency traversal has known N+1 query problems for
   * the other root types -- one query per category option combo, one per data set element, one per
   * program stage data element -- and a multi-object export multiplies how much of that a single
   * request can ask for. {@code OptionSet} is the one root type whose closure is flat (its options,
   * and nothing else), so it is the one type enabled to begin with.
   *
   * <p>Add a type here once its N+1s are fixed. Nothing else needs to change: the fold, the
   * merging, the error contract and the serialisation are all type-agnostic.
   */
  public static final Set<Class<? extends IdentifiableObject>> ENABLED_ROOT_TYPES =
      Set.of(OptionSet.class);

  private final SchemaService schemaService;
  private final IdentifiableObjectManager manager;
  private final MetadataExportService metadataExportService;

  /**
   * Resolves the given {@code type:id} tokens.
   *
   * <p>Objects are loaded with one batched, ACL-aware query per type, so an object the current user
   * may not read comes back missing and is reported the same way as one that does not exist -- the
   * response does not distinguish the two.
   *
   * @param tokens the raw {@code object} parameter values
   * @return the resolved roots, or every reason resolution failed
   */
  @Transactional(readOnly = true)
  public MetadataDependencyRoots resolve(@CheckForNull Collection<String> tokens) {
    List<ErrorReport> errors = new ArrayList<>();

    if (tokens == null || tokens.isEmpty()) {
      errors.add(new ErrorReport(IdentifiableObject.class, ErrorCode.E6028));
      return new MetadataDependencyRoots(List.of(), errors);
    }

    // identical tokens would otherwise walk the same closure twice
    List<String> distinct = tokens.stream().distinct().toList();

    if (distinct.size() > MAX_ROOTS) {
      errors.add(
          new ErrorReport(IdentifiableObject.class, ErrorCode.E6027, MAX_ROOTS, distinct.size()));
      return new MetadataDependencyRoots(List.of(), errors);
    }

    List<TypedReference> typed = classify(distinct, errors);
    List<IdentifiableObject> objects = load(typed, errors);

    return new MetadataDependencyRoots(errors.isEmpty() ? objects : List.of(), errors);
  }

  /**
   * Classifies each token in request order, in the order malformed, unknown type, unsupported root
   * type, invalid UID. Tokens that survive are returned for loading.
   */
  private List<TypedReference> classify(List<String> tokens, List<ErrorReport> errors) {
    List<TypedReference> typed = new ArrayList<>();

    for (String token : tokens) {
      MetadataObjectReference reference = MetadataObjectReference.parse(token);

      if (reference == null) {
        errors.add(
            new ErrorReport(IdentifiableObject.class, ErrorCode.E6024, token).setMainId(token));
        continue;
      }

      Class<? extends IdentifiableObject> type = classForType(reference.type());

      if (type == null) {
        errors.add(
            new ErrorReport(IdentifiableObject.class, ErrorCode.E6002, reference.type())
                .setMainId(token));
        continue;
      }

      if (!metadataExportService.getSupportedDependencyRootTypes().contains(type)) {
        errors.add(
            new ErrorReport(type, ErrorCode.E6026, reference.type(), supportedRootTypeNames())
                .setMainId(token));
        continue;
      }

      // a dependency export root, but not yet enabled here -- see ENABLED_ROOT_TYPES
      if (!ENABLED_ROOT_TYPES.contains(type)) {
        errors.add(
            new ErrorReport(type, ErrorCode.E6029, reference.type(), enabledRootTypeNames())
                .setMainId(token));
        continue;
      }

      if (!CodeGenerator.isValidUid(reference.id())) {
        errors.add(
            new ErrorReport(type, ErrorCode.E6025, reference.id(), reference.type())
                .setMainId(token));
        continue;
      }

      typed.add(new TypedReference(reference, type));
    }

    return typed;
  }

  /** Loads the classified references with one batched query per type, preserving request order. */
  private List<IdentifiableObject> load(List<TypedReference> typed, List<ErrorReport> errors) {
    Map<Class<? extends IdentifiableObject>, List<TypedReference>> byType = new LinkedHashMap<>();
    typed.forEach(t -> byType.computeIfAbsent(t.type(), k -> new ArrayList<>()).add(t));

    Map<Class<? extends IdentifiableObject>, Map<String, IdentifiableObject>> found =
        new LinkedHashMap<>();

    byType.forEach(
        (type, references) ->
            found.put(
                type,
                manager
                    .getByUid(type, references.stream().map(t -> t.reference().id()).toList())
                    .stream()
                    .collect(
                        Collectors.toMap(
                            IdentifiableObject::getUid,
                            Function.<IdentifiableObject>identity(),
                            (a, b) -> a))));

    List<IdentifiableObject> objects = new ArrayList<>();

    for (TypedReference t : typed) {
      IdentifiableObject object = found.get(t.type()).get(t.reference().id());

      if (object == null) {
        errors.add(
            new ErrorReport(t.type(), ErrorCode.E6025, t.reference().id(), t.reference().type())
                .setMainId(t.reference().toString()));
      } else {
        objects.add(object);
      }
    }

    return objects;
  }

  /**
   * Resolves a schema name to its class. Singular is tried first so the lookup stays deterministic
   * and cannot become ambiguous as schemas are added.
   */
  @CheckForNull
  @SuppressWarnings("unchecked")
  private Class<? extends IdentifiableObject> classForType(String type) {
    Schema schema = schemaService.getSchemaBySingularName(type);

    if (schema == null) {
      schema = schemaService.getSchemaByPluralName(type);
    }

    return schema != null && schema.isIdentifiableObject()
        ? (Class<? extends IdentifiableObject>) schema.getKlass()
        : null;
  }

  /** The supported root types as singular schema names, sorted, for use in an error message. */
  private String supportedRootTypeNames() {
    return typeNames(metadataExportService.getSupportedDependencyRootTypes());
  }

  /** The currently enabled root types as singular schema names, sorted, for an error message. */
  private String enabledRootTypeNames() {
    return typeNames(ENABLED_ROOT_TYPES);
  }

  private String typeNames(Set<Class<? extends IdentifiableObject>> types) {
    return types.stream()
        .map(schemaService::getSchema)
        .map(Schema::getSingular)
        .sorted(Comparator.naturalOrder())
        .collect(Collectors.joining(", "));
  }

  private record TypedReference(
      MetadataObjectReference reference, Class<? extends IdentifiableObject> type) {}
}
