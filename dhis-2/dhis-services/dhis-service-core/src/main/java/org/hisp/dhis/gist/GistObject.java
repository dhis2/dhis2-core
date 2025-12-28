/*
 * Copyright (c) 2004-2025, University of Oslo
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

import static java.util.Objects.requireNonNull;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.PrimaryKeyObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.object.ObjectOutput.Property;

/**
 * Data about a single object given as key-value pairs of {@link #properties()} and {@link
 * #values()}.
 *
 * @author Jan Bernitt
 * @param properties the properties contained in the object (scope)
 * @param values of the properties
 */
public record GistObject(@Nonnull List<Property> properties, @CheckForNull Object[] values) {

  public GistObject {
    requireNonNull(properties);
  }

  /**
   * User input to export details of a single object identified by its {@link #id()}.
   *
   * @param objectType the type of object to export
   * @param id the UID of the target object to find
   * @param params further query options on the lookup, mainly what properties to export
   */
  public record Input(
      @Nonnull Class<? extends PrimaryKeyObject> objectType,
      @Nonnull UID id,
      @Nonnull GistObjectParams params) {

    public Input {
      requireNonNull(objectType);
      requireNonNull(id);
      requireNonNull(params);
    }
  }

  /**
   * Data on a single object in form of it's {@link #properties()} and {@link #values()} as used to
   * serialize them as JSON or CSV.
   *
   * @param properties the scope contained in the data
   * @param values the values for the properties in matching order
   */
  public record Output(@Nonnull List<Property> properties, @Nonnull Object[] values) {

    public Output {
      requireNonNull(properties);
      requireNonNull(values);
    }

    public List<String> paths() {
      return properties.stream().map(Property::path).toList();
    }
  }
}
