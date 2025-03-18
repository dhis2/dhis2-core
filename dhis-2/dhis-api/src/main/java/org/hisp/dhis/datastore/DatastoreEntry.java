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
package org.hisp.dhis.datastore;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hisp.dhis.common.BaseIdentifiableObject;

/**
 * @author Stian Sandvold
 */
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@NoArgsConstructor
public class DatastoreEntry extends BaseIdentifiableObject {
  /** A namespace represents a collection of keys */
  @JsonProperty @ToString.Include @EqualsAndHashCode.Include private String namespace;

  /** A key belongs to a namespace, and represent a value */
  @JsonProperty @ToString.Include @EqualsAndHashCode.Include private String key;

  /**
   * A value referenced by a key and namespace, JSON-formatted data stored as a string but in a
   * jsonb column.
   */
  private String jbPlainValue;

  /** Whether this KeyJsonValue is encrypted or not. Default is false. */
  private Boolean encrypted = false;

  /** Encrypted value if encrypted is set to true */
  private String encryptedValue;

  /**
   * Transient variable to hold any new values set during session. Will be made into the correct
   * type when being persisted by the persistence layer (encrypted or plain).
   */
  private transient String value;

  public DatastoreEntry(String namespace, String key) {
    this(namespace, key, null, false);
  }

  public DatastoreEntry(String namespace, String key, String value) {
    this(namespace, key, value, false);
  }

  public DatastoreEntry(String namespace, String key, String value, boolean encrypted) {
    this.namespace = namespace;
    this.key = key;
    this.value = value;
    this.encrypted = encrypted;
  }

  @JsonProperty
  @ToString.Include
  @EqualsAndHashCode.Include
  public String getValue() {
    return isEncryptedInternal() ? getEncryptedValue() : getJbPlainValue();
  }

  public String getJbPlainValue() {
    return !isEncryptedInternal() && value != null ? value : jbPlainValue;
  }

  public String getEncryptedValue() {
    return isEncryptedInternal() && value != null ? value : encryptedValue;
  }

  /** Note: this must use a name that is not also a hibernate mapped property */
  private boolean isEncryptedInternal() {
    return encrypted != null && encrypted;
  }
}
