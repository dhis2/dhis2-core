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
 * Resolves the {@code objects=type:id} references of a multi-object dependency export into the
 * {@link IdentifiableObject} roots to export, collecting every problem rather than throwing the
 * first.
 *
 * @author David Mackessy
 */
@Service
@RequiredArgsConstructor
public class MetadataDependencyRootResolver {

  /**
   * The root types this endpoint accepts, deliberately narrower than {@link
   * MetadataExportService#getDependencyRootTypes()}. The traversal has known N+1s for the other
   * types, one query per category option combo, per data set element, per program stage data
   * element, and a multi-object request multiplies them. {@code OptionSet} has the shallowest
   * closure, its options plus any attributes either carries.
   *
   * <p>Add a type here once its N+1s are fixed, nothing else needs to change.
   */
  public static final Set<Class<? extends IdentifiableObject>> ENABLED_ROOT_TYPES =
      Set.of(OptionSet.class);

  private final SchemaService schemaService;
  private final AclService aclService;
  private final IdentifiableObjectManager manager;
  private final MetadataExportService metadataExportService;

  /**
   * Loads with one batched ACL-aware query per type. An object the user may not read is reported
   * the same way as one that does not exist.
   *
   * @param tokens the raw {@code objects} parameter values
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
   * Classifies in request order, reporting the first failing check per reference. Survivors are
   * de-duplicated by the object they denote, not their spelling, so {@code optionSet:X} and {@code
   * optionSets:X} are walked once.
   */
  private List<TypedReference> classify(
      Collection<MetadataObjectReference> references, List<ErrorReport> errors) {
    Set<Class<? extends IdentifiableObject>> supported =
        metadataExportService.getDependencyRootTypes();
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
   * Singular is canonical, via {@link AclService#classForType}, and tried first so the lookup
   * cannot become ambiguous as schemas are added. Plural is accepted because it is the key in the
   * exported payload.
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

  /** A reference that survived classification, paired with the class its type name resolved to. */
  private record TypedReference(
      MetadataObjectReference reference, Class<? extends IdentifiableObject> type) {}

  /** What a reference denotes, independent of whether it was spelt singular or plural. */
  private record RootKey(Class<? extends IdentifiableObject> type, String id) {}
}
