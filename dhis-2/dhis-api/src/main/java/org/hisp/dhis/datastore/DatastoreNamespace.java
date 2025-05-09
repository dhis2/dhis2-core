/*
 * Copyright (c) 2004-2024, University of Oslo
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

import static org.hisp.dhis.common.collection.CollectionUtils.union;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.Data;

/**
 * A {@link DatastoreNamespace} is the input declaration for a {@link DatastoreNamespaceProtection}
 * object.
 *
 * <p>Note that this type needs to be {@link Serializable} as it is put in a cache as part of the
 * {@link org.hisp.dhis.appmanager.App}.
 *
 * @author Jan Bernitt
 */
@Data
public class DatastoreNamespace implements Serializable {

  private static final long serialVersionUID = -1653792127753819375L;

  /** The namespace name an app wants to use in a protected manner */
  @JsonProperty @CheckForNull private String namespace;

  /** A user must have one of these authorities to be able to read/write the namespace */
  @JsonProperty @CheckForNull private Set<String> authorities;

  /** A user must have one of these authorities to be able to read the namespace */
  @JsonProperty @CheckForNull private Set<String> readOnlyAuthorities;

  @Nonnull
  public Set<String> getAllAuthorities() {
    return union(authorities, readOnlyAuthorities);
  }
}
