/*
 * Copyright (c) 2004-2024, University of Oslo
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

import static java.util.Comparator.comparing;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * A tool to analyse how many schemas are referenced by each {@link
 * org.hisp.dhis.webapi.openapi.Api.Schema} in an {@link Api} directly and indirectly.
 *
 * <p>The purpose of this is to understand which connections have the largest impact on the API
 * surface in terms of shared schemas.
 *
 * @author Jan Bernitt
 */
class OpenApiComponentsRefs {

  /**
   * Reference information for a {@link Api.Property}.
   *
   * @param name name of the property
   * @param of the property itself this information is about
   * @param to names of all schemas directly or indirectly referenced by the properties type
   * @param exclusive names of the schemas directly or indirectly referenced by the properties type
   *     that are not referenced by any other property in the schema this property belongs to
   */
  record PropertyRefs(String name, Api.Property of, Set<String> to, Set<String> exclusive) {
    /**
     * @return the number of references this property adds that are not referenced by any of the
     *     other properties in the same schema
     */
    int additional() {
      return exclusive.size();
    }

    int impact() {
      return to.size();
    }
  }

  /**
   * Reference information for a {@link Api.Schema}.
   *
   * @param name name of the schema
   * @param of the schema itself this information is about
   * @param to names of all the schemas directly or indirectly referenced by this schema
   * @param properties reference information for each of the schemas properties (that are
   *     referencing to a named schema)
   */
  record SchemaRefs(String name, Api.Schema of, Set<String> to, List<PropertyRefs> properties) {

    int impact() {
      return to.size();
    }
  }

  public static void print(Api api, PrintStream out) {
    List<SchemaRefs> refs = of(api);

    for (SchemaRefs ref : refs) {
      out.printf("%2d %s%n", ref.impact(), ref.name);
      for (PropertyRefs pr : ref.properties) {
        Api.Schema type = pr.of.getType();
        String signature =
            type.getType() == Api.Schema.Type.ARRAY
                ? getSharedName(type) + "[]"
                : getSharedName(type);
        out.printf(
            "\t%2d (+%2d) %s %s %s%n",
            pr.to.size(),
            pr.additional(),
            signature,
            pr.name,
            pr.exclusive.isEmpty() ? "" : pr.exclusive.toString());
      }
    }
  }

  @Nonnull
  static List<SchemaRefs> of(@Nonnull Api api) {
    // compute schema statistics
    Map<String, SchemaRefs> byName =
        api.getComponents().getSchemas().values().stream()
            .map(OpenApiComponentsRefs::of)
            .collect(Collectors.toMap(SchemaRefs::name, Function.identity()));

    // add in the properties statistics (requires all schema statistics exist)
    byName
        .values()
        .forEach(
            refs -> {
              refs.of.getProperties().stream()
                  .filter(p -> isReferencing(p.getType()))
                  .forEach(
                      p -> {
                        Set<String> all = byName.get(getSharedName(p.getType())).to;
                        Set<String> unique = new HashSet<>(all);
                        refs.of.getProperties().stream()
                            .filter(p2 -> isReferencing(p2.getType()))
                            .filter(p2 -> p != p2)
                            .forEach(
                                p2 -> unique.removeAll(byName.get(getSharedName(p2.getType())).to));
                        refs.properties.add(new PropertyRefs(p.getName(), p, all, unique));
                      });
              refs.properties.sort(
                  comparing(PropertyRefs::additional)
                      .reversed()
                      .thenComparing(comparing(PropertyRefs::impact).reversed())
                      .thenComparing(PropertyRefs::name));
            });
    return byName.values().stream()
        .sorted(comparing(SchemaRefs::impact).reversed().thenComparing(SchemaRefs::name))
        .toList();
  }

  private static boolean isReferencing(Api.Schema type) {
    return type.isShared()
        || type.getType() == Api.Schema.Type.ARRAY && isReferencing(type.getElementType());
  }

  private static String getSharedName(Api.Schema type) {
    if (type.isShared()) return type.getSharedName().getValue();
    if (type.getType() == Api.Schema.Type.ARRAY) return getSharedName(type.getElementType());
    return null;
  }

  @Nonnull
  static SchemaRefs of(@Nonnull Api.Schema schema) {
    String name = schema.getSharedName().getValue();
    SchemaRefs refs = new SchemaRefs(name, schema, new HashSet<>(), new ArrayList<>());
    refs.to.add(name); // add self to prevent cyclic endless loop
    schema.getProperties().forEach(p -> addReferences(p.getType(), refs.to));
    refs.to.remove(name); // remove self
    return refs;
  }

  private static void addReferences(Api.Schema referenced, Set<String> to) {
    String name = referenced.getSharedName().getValue();
    if (name != null) {
      if (to.contains(name)) return; // been there, done that
      to.add(name);
    }
    referenced.getProperties().forEach(p -> addReferences(p.getType(), to));
  }
}
