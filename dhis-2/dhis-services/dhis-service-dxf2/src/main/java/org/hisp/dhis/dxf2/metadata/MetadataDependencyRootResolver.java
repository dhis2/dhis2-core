/*
 * Copyright (c) 2004-2026, University of Oslo
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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.stereotype.Service;

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
   * The root types this endpoint currently accepts.
   *
   * <p>Deliberately narrower than {@link MetadataExportService#getSupportedDependencyRootTypes()},
   * which is every type the traversal can walk and which the per-type {@code /{uid}/metadata}
   * endpoints continue to serve in full. The dependency traversal has known N+1 query problems for
   * the other root types, one query per category option combo, one per data set element, one per
   * program stage data element, and a multi-object export multiplies how much of that a single
   * request can ask for. {@code OptionSet} is the one root type whose closure is flat (its options,
   * and nothing else), so it is the one type enabled to begin with.
   *
   * <p>Add a type here once its N+1s are fixed. Nothing else needs to change: the merging, the
   * error contract and the serialisation are all type-agnostic.
   */
  public static final Set<Class<? extends IdentifiableObject>> ENABLED_ROOT_TYPES =
      Set.of(OptionSet.class);

  private final SchemaService schemaService;
  private final AclService aclService;
  private final IdentifiableObjectManager manager;
  private final MetadataExportService metadataExportService;

  /**
   * Resolves the given {@code type:id} tokens.
   *
   * <p>Objects are loaded with one batched ACL-aware query per type. An object the current user may
   * not read comes back missing and is reported the same way as one that does not exist, the
   * response does not distinguish the two.
   *
   * @param tokens the raw {@code object} parameter values
   * @return the resolved roots, or every reason resolution failed
   */
  public MetadataDependencyRoots resolve(@CheckForNull Collection<String> tokens) {
    List<ErrorReport> errors = new ArrayList<>();

    if (tokens == null || tokens.isEmpty()) {
      errors.add(error(IdentifiableObject.class, null, ErrorCode.E6028));
      return new MetadataDependencyRoots(List.of(), errors);
    }

    // one parameter may name several objects, so expand each token before classifying: how the
    // caller chose to spell a request must not change what it resolves to
    Set<MetadataObjectReference> references = new LinkedHashSet<>();

    for (String token : new LinkedHashSet<>(tokens)) {
      List<MetadataObjectReference> parsed = MetadataObjectReference.parseAll(token);

      if (parsed.isEmpty()) {
        errors.add(error(IdentifiableObject.class, token, ErrorCode.E6024, token));
      } else {
        references.addAll(parsed);
      }
    }

    List<TypedReference> typed = classify(references, errors);
    List<IdentifiableObject> objects = load(typed, errors);

    return new MetadataDependencyRoots(errors.isEmpty() ? objects : List.of(), errors);
  }

  /**
   * Classifies each reference in request order, in the order unknown type, unsupported root type,
   * not yet enabled, invalid UID. References that survive are returned for loading, de-duplicated
   * by the object they denote rather than by their spelling, so {@code optionSet:X} and {@code
   * optionSets:X} are walked once.
   */
  private List<TypedReference> classify(
      Collection<MetadataObjectReference> references, List<ErrorReport> errors) {
    Set<Class<? extends IdentifiableObject>> supported =
        metadataExportService.getSupportedDependencyRootTypes();
    Map<RootKey, TypedReference> typed = new LinkedHashMap<>();

    for (MetadataObjectReference reference : references) {
      String token = reference.toString();
      Class<? extends IdentifiableObject> type = classForType(reference.type());

      if (type == null) {
        errors.add(error(IdentifiableObject.class, token, ErrorCode.E6002, reference.type()));
        continue;
      }

      if (!supported.contains(type)) {
        errors.add(error(type, token, ErrorCode.E6026, reference.type(), typeNames(supported)));
        continue;
      }

      if (!ENABLED_ROOT_TYPES.contains(type)) {
        errors.add(
            error(type, token, ErrorCode.E6029, reference.type(), typeNames(ENABLED_ROOT_TYPES)));
        continue;
      }

      if (!CodeGenerator.isValidUid(reference.id())) {
        errors.add(error(type, token, ErrorCode.E1113, reference.type(), reference.id()));
        continue;
      }

      typed.putIfAbsent(new RootKey(type, reference.id()), new TypedReference(reference, type));
    }

    return List.copyOf(typed.values());
  }

  /** Loads the classified references with one batched query per type, preserving request order. */
  private List<IdentifiableObject> load(List<TypedReference> typed, List<ErrorReport> errors) {
    Map<Class<? extends IdentifiableObject>, List<String>> idsByType =
        typed.stream()
            .collect(groupingBy(TypedReference::type, mapping(t -> t.reference().id(), toList())));

    Map<Class<? extends IdentifiableObject>, Map<String, ? extends IdentifiableObject>> found =
        new HashMap<>();
    idsByType.forEach(
        (type, ids) ->
            found.put(type, IdentifiableObjectUtils.getUidObjectMap(manager.getByUid(type, ids))));

    List<IdentifiableObject> objects = new ArrayList<>();

    for (TypedReference t : typed) {
      IdentifiableObject object = found.get(t.type()).get(t.reference().id());

      if (object == null) {
        errors.add(
            error(
                t.type(),
                t.reference().toString(),
                ErrorCode.E1113,
                t.reference().type(),
                t.reference().id()));
      } else {
        objects.add(object);
      }
    }

    return objects;
  }

  private static ErrorReport error(
      Class<?> klass, @CheckForNull String token, ErrorCode code, Object... args) {
    ErrorReport report = new ErrorReport(klass, code, args);
    return token == null ? report : report.setMainId(token);
  }

  /**
   * Resolves a schema name to its class. The singular form is canonical and delegates to {@link
   * AclService#classForType}; the plural form is accepted as a fallback because it is what appears
   * as the key in the exported payload. Singular is tried first so the lookup stays deterministic
   * and cannot become ambiguous as schemas are added.
   */
  @CheckForNull
  @SuppressWarnings("unchecked")
  private Class<? extends IdentifiableObject> classForType(String type) {
    Class<? extends IdentifiableObject> singular = aclService.classForType(type);

    if (singular != null) {
      return singular;
    }

    Schema schema = schemaService.getSchemaByPluralName(type);

    return schema != null && schema.isIdentifiableObject()
        ? (Class<? extends IdentifiableObject>) schema.getKlass()
        : null;
  }

  /** The given root types as singular schema names, sorted, for use in an error message. */
  private String typeNames(Set<Class<? extends IdentifiableObject>> types) {
    return types.stream()
        .map(schemaService::getSchema)
        .map(Schema::getSingular)
        .sorted()
        .collect(joining(", "));
  }

  /**
   * A reference that survived classification, with the class it names and the token it came from.
   */
  private record TypedReference(
      MetadataObjectReference reference, Class<? extends IdentifiableObject> type) {}

  /** What a reference denotes, independent of whether it was spelt singular or plural. */
  private record RootKey(Class<? extends IdentifiableObject> type, String id) {}
}
