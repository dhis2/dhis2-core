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

import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Input identifiers that can be used as an alternative to {@link UID}s.
 *
 * @param type the property used to identify an object
 * @param attributeId the attribute UID in case an attribute is used, otherwise always null
 */
public record InputId(@Nonnull Type type, @CheckForNull String attributeId) {

  public InputId {
    if (type == Type.ATTR && attributeId == null)
      throw new IllegalArgumentException("When type is ATTR the attribute ID must be non-null");
    if (type != Type.ATTR && attributeId != null)
      throw new IllegalArgumentException("When type is not ATTR the attribute ID must be null");
  }

  public static InputId of(IdScheme scheme) {
    if (scheme == null) return new InputId(Type.ID, null);
    IdentifiableProperty property = scheme.getIdentifiableProperty();
    if (property == null) return new InputId(Type.ID, null);
    return switch (property) {
      case ID, UID -> new InputId(Type.ID, null);
      case CODE -> new InputId(Type.CODE, null);
      case NAME -> new InputId(Type.NAME, null);
      case ATTRIBUTE -> new InputId(Type.ATTR, scheme.getAttribute());
      case UUID ->
          throw new UnsupportedOperationException("UUID is not supported for this operation");
    };
  }

  public enum Type {
    ID,
    CODE,
    NAME,
    ATTR
  }

  public boolean isNotUid() {
    return type != Type.ID;
  }

  /** A decoder that parses or resolves alternative IDs to a UID */
  public interface ToUID {

    /**
     * @param xid the input ID to decode or resolve
     * @return the input resolved to a {@link UID}
     * @throws IllegalArgumentException in case the decoding fails
     */
    UID decode(String xid) throws IllegalArgumentException;

    /**
     * @param xid the ID to use when input ID is null
     * @return a wrapped decoder that uses the fallback ID in case the input id is null
     */
    static ToUID whenNullUse(@CheckForNull String xid, ToUID then) {
      if (xid == null) return then;
      return nullableXid -> then.decode(nullableXid == null ? xid : nullableXid);
    }

    /**
     * @param msg a text describing the type of ID in case an exception is thrown because the input
     *     was null
     * @return a wrapped decoder that requires the input ID to be non-null, otherwise a {@link
     *     IllegalArgumentException} will be thrown
     */
    static ToUID requireNonNull(@Nonnull String msg, ToUID then) {
      return xid -> {
        if (xid == null) throw new IllegalArgumentException("%s id cannot be null".formatted(msg));
        return then.decode(xid);
      };
    }

    /**
     * @param xidToUid a mapping from input to mapped a UID
     * @return a wrapped decoder forwarding a non-null input as mapped ID to the {@link
     *     #decode(String)} function, throws a {@link IllegalArgumentException} in case there is no
     *     mapped ID value
     */
    static ToUID mapBy(@Nonnull Map<String, String> xidToUid, ToUID then) {
      return xid -> {
        if (xid == null) return then.decode(null);
        String mid = xidToUid.get(xid);
        if (mid != null) return then.decode(mid);
        throw new IllegalArgumentException("%s does not map to a UID".formatted(xid));
      };
    }
  }
}
