/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.webapi.json.domain;

import java.util.List;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonURL;
import org.hisp.dhis.security.AuthorityType;

/**
 * Web API equivalent of a {@code Schema} as returned by the {@code /schemas} endpoint.
 *
 * @author Jan Bernitt
 */
public interface JsonSchema extends JsonObject {

  default Class<?> getKlass() {
    return getString("klass").parsedClass();
  }

  default List<Class<?>> getReferences() {
    return getArray("references")
        .values(
            klass -> {
              try {
                return Class.forName(klass);
              } catch (ClassNotFoundException ex) {
                throw new IllegalArgumentException(ex);
              }
            });
  }

  default String getRelativeApiEndpoint() {
    return getString("relativeApiEndpoint").string();
  }

  default String getName() {
    return getString("name").string();
  }

  default String getSingular() {
    return getString("singular").string();
  }

  default String getPlural() {
    return getString("plural").string();
  }

  default String getDisplayName() {
    return getString("displayName").string();
  }

  default String getCollectionName() {
    return getString("collectionName").string();
  }

  default JsonURL getHref() {
    return get("href", JsonURL.class);
  }

  default JsonURL getApiEndpoint() {
    return get("apiEndpoint", JsonURL.class);
  }

  default JsonURL getNamespace() {
    return get("namespace", JsonURL.class);
  }

  default boolean isShareable() {
    return getBoolean("shareable").booleanValue();
  }

  default boolean isMetadata() {
    return getBoolean("metadata").booleanValue();
  }

  default boolean isSecondaryMetadata() {
    return getBoolean("secondaryMetadata").booleanValue();
  }

  default boolean isImplicitPrivateAuthority() {
    return getBoolean("implicitPrivateAuthority").booleanValue();
  }

  default boolean isNameableObject() {
    return getBoolean("nameableObject").booleanValue();
  }

  default boolean isSubscribable() {
    return getBoolean("subscribable").booleanValue();
  }

  default int getOrder() {
    return getNumber("order").intValue();
  }

  default boolean isTranslatable() {
    return getBoolean("translatable").booleanValue();
  }

  default boolean isIdentifiableObject() {
    return getBoolean("identifiableObject").booleanValue();
  }

  default boolean isFavoritable() {
    return getBoolean("favoritable").booleanValue();
  }

  default boolean isSubscribableObject() {
    return getBoolean("subscribableObject").booleanValue();
  }

  default boolean isDataShareable() {
    return getBoolean("dataShareable").booleanValue();
  }

  default boolean isEmbeddedObject() {
    return getBoolean("embeddedObject").booleanValue();
  }

  default boolean isDefaultPrivate() {
    return getBoolean("defaultPrivate").booleanValue();
  }

  default boolean isPersisted() {
    return getBoolean("persisted").booleanValue();
  }

  default JsonList<JsonAuthority> getAuthorities() {
    return getList("authorities", JsonAuthority.class);
  }

  interface JsonAuthority extends JsonObject {

    default AuthorityType getType() {
      return getString("type").parsed(AuthorityType::valueOf);
    }

    default List<String> getAuthorities() {
      return getArray("authorities").stringValues();
    }
  }

  default JsonList<JsonProperty> getProperties() {
    return getList("properties", JsonProperty.class);
  }

  default Iterable<JsonProperty> getRequiredProperties() {
    return getProperties().filtered(JsonProperty::isRequired);
  }
}
