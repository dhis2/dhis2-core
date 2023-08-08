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
package org.hisp.dhis.datastore;

import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Set;

/**
 * The {@link DatastoreNamespaceProtection} is a configuration for a particular namespace and the
 * set of {@link DatastoreEntry}s in that namespace.
 *
 * <p>Note that this is configured programmatically only.
 *
 * @author Jan Bernitt
 */
public class DatastoreNamespaceProtection {

  /**
   * Protection rules apply to users that do not have at least one of the required {@link
   * #authorities}.
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

  private final String namespace;

  private final boolean sharingRespected;

  private final ProtectionType reads;

  private final ProtectionType writes;

  private final Set<String> authorities;

  public DatastoreNamespaceProtection(
      String namespace, ProtectionType readWrite, boolean sharingRespected, String... authorities) {
    this(namespace, readWrite, readWrite, sharingRespected, authorities);
  }

  public DatastoreNamespaceProtection(
      String namespace,
      ProtectionType reads,
      ProtectionType writes,
      boolean sharingRespected,
      String... authorities) {
    this(namespace, reads, writes, sharingRespected, new HashSet<>(asList(authorities)));
  }

  public DatastoreNamespaceProtection(
      String namespace,
      ProtectionType reads,
      ProtectionType writes,
      boolean sharingRespected,
      Set<String> authorities) {
    this.namespace = namespace;
    this.sharingRespected = sharingRespected;
    this.reads = reads;
    this.writes = writes;
    this.authorities = authorities;
  }

  public String getNamespace() {
    return namespace;
  }

  /**
   * @return true when the {@link org.hisp.dhis.user.sharing.Sharing} of a {@link DatastoreEntry}
   *     should be checked in addition to authority based checks, else false.
   */
  public boolean isSharingRespected() {
    return sharingRespected;
  }

  public Set<String> getAuthorities() {
    return authorities;
  }

  public ProtectionType getReads() {
    return reads;
  }

  public ProtectionType getWrites() {
    return writes;
  }

  @Override
  public String toString() {
    return String.format(
        "KeyJsonNamespaceProtection{%s r:%s w:%s [%s]%s}",
        namespace, reads, writes, authorities, (sharingRespected ? "!" : ""));
  }
}
