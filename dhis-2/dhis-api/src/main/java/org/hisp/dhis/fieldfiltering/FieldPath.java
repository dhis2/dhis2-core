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

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hisp.dhis.common.PropertyPath;
import org.hisp.dhis.schema.Property;

/**
 * @author Morten Olav Hansen
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class FieldPath {

  public static FieldPath of(CharSequence path) {
    return of(PropertyPath.of(path));
  }

  public static FieldPath of(PropertyPath path) {
    return new FieldPath(path, List.of(), null);
  }

  public static final String FIELD_PATH_SEPARATOR = ".";

  @Nonnull private final PropertyPath path;

  /** Transformers to apply to field, can be empty. */
  @Nonnull private final List<FieldPathTransformer> transformers;

  /** Schema Property if present (added by {@link FieldPathHelper}). */
  @CheckForNull @Setter private Property property;

  public FieldPath withTransformers(FieldPathTransformer... transformers) {
    return withTransformers(List.of(transformers));
  }

  public FieldPath withTransformers(@Nonnull List<FieldPathTransformer> transformers) {
    return new FieldPath(path, transformers, property);
  }

  public FieldPath relativeTo(@Nonnull CharSequence parentSegment) {
    PropertyPath relativePath = path.relativeTo(parentSegment);
    if (relativePath == null)
      throw new IllegalArgumentException("Relative path leads to empty path");
    return new FieldPath(relativePath, transformers, property);
  }

  /**
   * @return the name of the property the path points to (tail or leaf of the path)
   */
  public String getPropertyName() {
    return path.property().toString();
  }

  public boolean isPreset() {
    return path.isPreset();
  }

  public boolean isExclude() {
    return path.isExclude();
  }

  /**
   * @return true if we have at least one field path transformer
   */
  public boolean isTransformer() {
    return !transformers.isEmpty();
  }

  @Override
  public String toString() {
    return path.toString();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof FieldPath p && path.equals(p.path);
  }

  @Override
  public int hashCode() {
    return path.hashCode();
  }
}
