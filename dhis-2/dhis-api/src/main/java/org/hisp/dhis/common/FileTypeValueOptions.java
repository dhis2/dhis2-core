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
package org.hisp.dhis.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;

/**
 * A {@link ValueTypeOptions} sub class implementing options for a corresponding {@link
 * ValueType}.FILE_RESOURCE or {@link ValueType}.IMAGE
 *
 * <p>This object is saved as a jsonb column and can be used to validate that a FileResource has the
 * wanted properties.
 *
 * <p>This class is used in the {@link
 * org.hisp.dhis.system.util.ValidationUtils#validateFileResource } method.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 * @see ValueTypeOptions
 * @see ValueType
 */
public class FileTypeValueOptions extends ValueTypeOptions implements Serializable {
  private static final long serialVersionUID = 1L;

  private long version = serialVersionUID;

  private long maxFileSize = 0;

  private Set<String> allowedContentTypes = Collections.emptySet();

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(value = PropertyType.NUMBER, required = Property.Value.FALSE)
  public long getMaxFileSize() {
    return maxFileSize;
  }

  public void setMaxFileSize(long maxFileSize) {
    this.maxFileSize = maxFileSize;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Set<String> getAllowedContentTypes() {
    return allowedContentTypes;
  }

  public void setAllowedContentTypes(Set<String> allowedContentTypes) {
    this.allowedContentTypes = allowedContentTypes;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(value = PropertyType.NUMBER, required = Property.Value.FALSE)
  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
  }

  @Override
  public String toString() {
    return "FileTypeValueOptions{"
        + "version="
        + version
        + ", maxFileSize="
        + maxFileSize
        + ", allowedContentTypes="
        + allowedContentTypes
        + '}';
  }
}
