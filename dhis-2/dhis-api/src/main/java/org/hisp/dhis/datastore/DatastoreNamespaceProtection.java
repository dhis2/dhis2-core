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

import static com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY;
import static org.hisp.dhis.common.collection.CollectionUtils.union;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.Value;

/**
 * The {@link DatastoreNamespaceProtection} is a configuration for a particular namespace and the
 * set of {@link DatastoreEntry}s in that namespace.
 *
 * <p>Note that this is configured programmatically only.
 *
 * @author Jan Bernitt
 */
@Value
public class DatastoreNamespaceProtection {

  /**
   * Protection rules apply to users that do not have at least one of the required authorities.
   *
   * <p>All superusers always have read and write access.
   */
  public enum ProtectionType {
    /**
     * READ/WRITE: lets any user see or modify (only exists to be used in combination with one of
     * the other two).
     */
    NONE,
    /**
     * READ: The namespace apparently does not exist. Queries come back as empty or undefined.
     *
     * <p>WRITE: Attempts to modify an entry are silently ignored.
     */
    HIDDEN,

    /** READ/WRITE: Attempts to read or modify will throw an exception. */
    RESTRICTED
  }

  @JsonProperty(access = READ_ONLY)
  @Nonnull
  String namespace;

  @JsonProperty(access = READ_ONLY)
  @Nonnull
  ProtectionType reads;

  @JsonProperty(access = READ_ONLY)
  @Nonnull
  ProtectionType writes;

  @JsonProperty(access = READ_ONLY)
  @Nonnull
  Set<String> readAuthorities;

  @JsonProperty(access = READ_ONLY)
  @Nonnull
  Set<String> writeAuthorities;

  public DatastoreNamespaceProtection(
      @Nonnull String namespace,
      @Nonnull ProtectionType readWrite,
      @Nonnull String... authorities) {
    this(namespace, readWrite, readWrite, authorities);
  }

  public DatastoreNamespaceProtection(
      @Nonnull String namespace,
      @Nonnull ProtectionType reads,
      @Nonnull ProtectionType writes,
      @Nonnull String... authorities) {
    this(namespace, reads, Set.of(authorities), writes, Set.of(authorities));
  }

  public DatastoreNamespaceProtection(
      @Nonnull String namespace,
      @Nonnull ProtectionType reads,
      @Nonnull Set<String> readAuthorities,
      @Nonnull ProtectionType writes,
      @Nonnull Set<String> writeAuthorities) {
    this.namespace = namespace;
    this.reads = readAuthorities.isEmpty() ? ProtectionType.NONE : reads;
    this.writes = writeAuthorities.isEmpty() ? ProtectionType.NONE : writes;
    this.readAuthorities = readAuthorities;
    this.writeAuthorities = writeAuthorities;
  }

  @Nonnull
  public Set<String> getAllAuthorities() {
    return union(readAuthorities, writeAuthorities);
  }
}
