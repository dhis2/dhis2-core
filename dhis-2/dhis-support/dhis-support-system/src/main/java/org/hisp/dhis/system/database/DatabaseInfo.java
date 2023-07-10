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
package org.hisp.dhis.system.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.reflect.InvocationTargetException;
import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.beanutils.BeanUtils;

/**
 * @author Lars Helge Overland
 */
@Getter
@Setter
@NoArgsConstructor
public class DatabaseInfo {
  @JsonProperty private String name;

  @JsonProperty private String user;

  @JsonProperty private String url;

  @JsonProperty private String databaseVersion;

  @JsonProperty private boolean spatialSupport;

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public void clearSensitiveInfo() {
    this.name = null;
    this.user = null;
    this.url = null;
    this.databaseVersion = null;
  }

  /**
   * @return a cloned instance of this object.
   */
  @Nonnull
  public DatabaseInfo instance() {
    final DatabaseInfo cloned = new DatabaseInfo();
    try {
      BeanUtils.copyProperties(cloned, this);
    } catch (IllegalAccessException | InvocationTargetException ex) {
      throw new IllegalStateException(ex);
    }

    return cloned;
  }

  // -------------------------------------------------------------------------
  // toString
  // -------------------------------------------------------------------------

  @Override
  public String toString() {
    return "[Name: " + name + ", User: " + user + ", URL: " + url + "]";
  }
}
