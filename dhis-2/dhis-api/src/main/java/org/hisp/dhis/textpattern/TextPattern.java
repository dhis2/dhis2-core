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
package org.hisp.dhis.textpattern;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.google.common.collect.ImmutableList;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.Objects;

/**
 * This class represents a TextPattern - A String that is used to generate and validate a
 * user-defined patterns. Example pattern: "Current date: " + CURRENT_DATE("DD-MM-yyyy")
 *
 * <p>Read more about patterns in TextPatternMethod.
 *
 * @author Stian Sandvold
 */
public class TextPattern implements Serializable {
  private ImmutableList<TextPatternSegment> segments;

  private Objects ownerObject;

  private String ownerUid;

  public TextPattern() {
    this.segments = ImmutableList.of();
  }

  public TextPattern(List<TextPatternSegment> segments) {
    this.segments = ImmutableList.copyOf(segments);
  }

  public void setOwnerUid(String ownerUid) {
    this.ownerUid = ownerUid;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getOwnerUid() {
    return ownerUid;
  }

  public void setSegments(ArrayList<TextPatternSegment> segments) {
    this.segments = ImmutableList.copyOf(segments);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Objects getOwnerObject() {
    return ownerObject;
  }

  public void setOwnerObject(Objects ownerObject) {
    this.ownerObject = ownerObject;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public List<TextPatternSegment> getSegments() {
    return this.segments;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TextPattern that = (TextPattern) o;

    return java.util.Objects.equals(segments, that.segments)
        && ownerObject == that.ownerObject
        && java.util.Objects.equals(ownerUid, that.ownerUid);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(segments, ownerObject, ownerUid);
  }
}
