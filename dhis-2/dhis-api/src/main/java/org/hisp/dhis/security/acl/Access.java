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
package org.hisp.dhis.security.acl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.EmbeddedObject;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement(localName = "access", namespace = DxfNamespaces.DXF_2_0)
public class Access implements EmbeddedObject {
  private boolean manage;

  private boolean externalize;

  private boolean write;

  private boolean read;

  private boolean update;

  private boolean delete;

  private AccessData data;

  public Access() {}

  public Access(boolean value) {
    this.manage = value;
    this.externalize = value;
    this.write = value;
    this.read = value;
    this.update = value;
    this.delete = value;
  }

  @JsonProperty
  @JacksonXmlProperty(localName = "manage", namespace = DxfNamespaces.DXF_2_0)
  public boolean isManage() {
    return manage;
  }

  public Access setManage(boolean manage) {
    this.manage = manage;
    return this;
  }

  @JsonProperty
  @JacksonXmlProperty(localName = "externalize", namespace = DxfNamespaces.DXF_2_0)
  public boolean isExternalize() {
    return externalize;
  }

  public Access setExternalize(boolean externalize) {
    this.externalize = externalize;
    return this;
  }

  @JsonProperty
  @JacksonXmlProperty(localName = "write", namespace = DxfNamespaces.DXF_2_0)
  public boolean isWrite() {
    return write;
  }

  public Access setWrite(boolean write) {
    this.write = write;
    return this;
  }

  @JsonProperty
  @JacksonXmlProperty(localName = "read", namespace = DxfNamespaces.DXF_2_0)
  public boolean isRead() {
    return read;
  }

  public Access setRead(boolean read) {
    this.read = read;
    return this;
  }

  @JsonProperty
  @JacksonXmlProperty(localName = "update", namespace = DxfNamespaces.DXF_2_0)
  public boolean isUpdate() {
    return update;
  }

  public Access setUpdate(boolean update) {
    this.update = update;
    return this;
  }

  @JsonProperty
  @JacksonXmlProperty(localName = "delete", namespace = DxfNamespaces.DXF_2_0)
  public boolean isDelete() {
    return delete;
  }

  public Access setDelete(boolean delete) {
    this.delete = delete;
    return this;
  }

  @JsonProperty
  @JacksonXmlProperty(localName = "data", namespace = DxfNamespaces.DXF_2_0)
  public AccessData getData() {
    return data;
  }

  public Access setData(AccessData data) {
    this.data = data;
    return this;
  }

  @Override
  public String toString() {
    return "Access{"
        + "manage="
        + manage
        + ", externalize="
        + externalize
        + ", write="
        + write
        + ", read="
        + read
        + ", update="
        + update
        + ", delete="
        + delete
        + ", data="
        + data
        + '}';
  }
}
