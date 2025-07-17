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
package org.hisp.dhis.common;

import static java.util.Objects.requireNonNull;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * How to identify objects, or what kind of identifier is used (alternative to {@link UID}s).
 *
 * @param name the property used to identify an object
 * @param attributeId the attribute UID in case an attribute is used, otherwise always null
 * @author Jan Bernitt
 * @since 2.43
 */
public record IdProperty(@Nonnull Name name, @CheckForNull UID attributeId) {

  public IdProperty {
    requireNonNull(name);
    if (name == Name.ATTR && attributeId == null)
      throw new IllegalArgumentException("When property is ATTR the attributeId must be non-null");
    if (name != Name.ATTR && attributeId != null)
      throw new IllegalArgumentException("When property is not ATTR the attributeId must be null");
  }

  public static IdProperty of(IdScheme scheme) {
    if (scheme == null) return new IdProperty(Name.UID, null);
    IdentifiableProperty property = scheme.getIdentifiableProperty();
    if (property == null) return new IdProperty(Name.UID, null);
    return switch (property) {
      case ID, UID -> new IdProperty(Name.UID, null);
      case CODE -> new IdProperty(Name.CODE, null);
      case NAME -> new IdProperty(Name.NAME, null);
      case ATTRIBUTE -> new IdProperty(Name.ATTR, UID.of(scheme.getAttribute()));
      case UUID ->
          throw new UnsupportedOperationException("UUID is not supported for this operation");
    };
  }

  public enum Name {
    UID,
    CODE,
    NAME,
    ATTR
  }

  public boolean isNotUID() {
    return name != Name.UID;
  }
}
