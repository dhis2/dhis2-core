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

import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.SetMap;

/**
 * The algebra of metadata dependency closures.
 *
 * <p>A dependency closure -- what {@link
 * MetadataExportService#getMetadataWithDependencies(IdentifiableObject)} returns for one root -- is
 * a map from object type to the set of objects of that type pulled in by that root. Closures form a
 * monoid under set union, with {@link #empty()} as the identity and {@link #combine} as the
 * associative operation.
 *
 * <p>That is what makes a multi-root export composable: exporting N objects is the fold of the
 * single-root closure over those N roots, and de-duplication is a property of the union rather than
 * a separate step. An object reachable from more than one root -- including a root that is itself
 * another root's dependency -- lands in the same set and therefore appears exactly once.
 *
 * <p>The fold must be run sequentially. The accumulator is mutable and the values are Hibernate
 * entities bound to the session that produced them.
 *
 * @author David Mackessy
 */
public final class MetadataDependencies {

  private MetadataDependencies() {
    throw new UnsupportedOperationException("util");
  }

  /** The identity element: a closure containing nothing. */
  public static SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> empty() {
    return new SetMap<>();
  }

  /**
   * Unions {@code source} into {@code target}. Only {@code target} is modified; the sets held by
   * {@code source} are never aliased, since {@link SetMap#putValues} copies into a fresh set.
   */
  public static void mergeInto(
      SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> target,
      Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> source) {
    if (source != null) {
      source.forEach(target::putValues);
    }
  }

  /** The associative operation: the union of two closures, accumulated into the first. */
  public static SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> combine(
      SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> a,
      SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> b) {
    mergeInto(a, b);
    return a;
  }

  /**
   * Collects a stream of dependency closures into their union.
   *
   * <p>A {@link Collector} rather than a {@code reduce} because the accumulator is mutable, which
   * would make a shared identity value unsafe.
   */
  public static Collector<
          Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>>,
          SetMap<Class<? extends IdentifiableObject>, IdentifiableObject>,
          SetMap<Class<? extends IdentifiableObject>, IdentifiableObject>>
      union() {
    return Collector.of(
        MetadataDependencies::empty,
        MetadataDependencies::mergeInto,
        MetadataDependencies::combine);
  }
}
