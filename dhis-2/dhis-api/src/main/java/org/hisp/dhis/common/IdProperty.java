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

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
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
public record IdProperty(@Nonnull Name name, @CheckForNull UID attributeId)
    implements Serializable {

  public static final IdProperty UID = new IdProperty(Name.UID, null);
  public static final IdProperty CODE = new IdProperty(Name.CODE, null);
  public static final IdProperty NAME = new IdProperty(Name.NAME, null);

  public IdProperty {
    requireNonNull(name);
    if (name == Name.ATTR && attributeId == null)
      throw new IllegalArgumentException("When property is ATTR the attributeId must be non-null");
    if (name != Name.ATTR && attributeId != null)
      throw new IllegalArgumentException("When property is not ATTR the attributeId must be null");
  }

  public static IdProperty of(@CheckForNull IdentifiableProperty property) {
    if (property == null) return UID;
    return switch (property) {
      case ID, UID -> UID;
      case CODE -> CODE;
      case NAME -> NAME;
      case ATTRIBUTE ->
          throw new IllegalArgumentException("Attribute must be used with the attribute ID");
      case UUID ->
          throw new UnsupportedOperationException("UUID is not supported for this operation");
    };
  }

  @Nonnull
  public static IdProperty of(@Nonnull UID attributeId) {
    return new IdProperty(Name.ATTR, attributeId);
  }

  public static IdProperty of(@CheckForNull IdScheme scheme) {
    if (scheme == null) return UID;
    IdentifiableProperty property = scheme.getIdentifiableProperty();
    if (property == null) return UID;
    return switch (property) {
      case ID, UID -> UID;
      case CODE -> CODE;
      case NAME -> NAME;
      case ATTRIBUTE ->
          new IdProperty(Name.ATTR, org.hisp.dhis.common.UID.of(scheme.getAttribute()));
      case UUID ->
          throw new UnsupportedOperationException("UUID is not supported for this operation");
    };
  }

  /**
   * Parse an ID scheme input with fallback.
   *
   * @param scheme the scheme to use
   * @param orElseScheme the fallback in case the scheme is undefined
   * @return the input as object, {@link #UID} in case both are undefined
   */
  @Nonnull
  public static IdProperty of(@CheckForNull String scheme, @CheckForNull String orElseScheme) {
    IdProperty orElse = of(orElseScheme);
    return scheme == null ? orElse : of(scheme);
  }

  /**
   * Parse an ID scheme input (same format as {@link IdScheme#from(String)})
   *
   * @param scheme user input
   * @return the input as object, {@link UID} in case input is undefined
   */
  @Nonnull
  public static IdProperty of(@CheckForNull String scheme) {
    if (scheme == null) return UID;
    String type = scheme.toUpperCase();
    return switch (type) {
      case "ID", "UID" -> UID;
      case "CODE" -> CODE;
      case "NAME" -> NAME;
      default -> {
        if (type.length() == 21 && type.startsWith("ATTRIBUTE:"))
          yield new IdProperty(Name.ATTR, org.hisp.dhis.common.UID.of(scheme.substring(10)));
        throw new IllegalArgumentException(
            "Invalid ID scheme: %s\n\tUse UID, CODE, NAME, ATTRIBUTE:<uid>".formatted(scheme));
      }
    };
  }

  /**
   * @apiNote When creating an {@link IdProperty} from a nullable {@link String} the output must
   *     also be nullable to allow implementing the fallback logic of a common {@link IdProperty}.
   *     This is why this factory method is used to deserialize from JSON.
   * @param scheme as provided by user input
   * @return input as {@link IdProperty} or null for null input
   */
  @JsonCreator
  @CheckForNull
  public static IdProperty ofNullable(@CheckForNull String scheme) {
    return scheme == null ? null : of(scheme);
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

  @Override
  public String toString() {
    return switch (name) {
      case CODE -> "CODE";
      case NAME -> "NAME";
      case UID -> "ID";
      case ATTR -> "ATTRIBUTE:%s".formatted(attributeId);
    };
  }
}
