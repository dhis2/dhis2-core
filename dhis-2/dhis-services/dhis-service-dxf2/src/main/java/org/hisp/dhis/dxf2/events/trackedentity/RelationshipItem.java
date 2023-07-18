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
package org.hisp.dhis.dxf2.events.trackedentity;

import static org.hisp.dhis.common.OpenApi.Shared.Pattern.TRACKER;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.event.Event;

/**
 * @author Stian Sandvold
 */
@OpenApi.Shared(pattern = TRACKER)
@JacksonXmlRootElement(localName = "relationshipItem", namespace = DxfNamespaces.DXF_2_0)
public class RelationshipItem {
  private TrackedEntityInstance trackedEntityInstance;

  private Enrollment enrollment;

  private Event event;

  public RelationshipItem() {}

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public TrackedEntityInstance getTrackedEntityInstance() {
    return trackedEntityInstance;
  }

  public void setTrackedEntityInstance(TrackedEntityInstance trackedEntityInstance) {
    this.trackedEntityInstance = trackedEntityInstance;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Enrollment getEnrollment() {
    return enrollment;
  }

  public void setEnrollment(Enrollment enrollment) {
    this.enrollment = enrollment;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Event getEvent() {
    return event;
  }

  public void setEvent(Event event) {
    this.event = event;
  }

  @Override
  public String toString() {
    return "RelationshipItem{"
        + "trackedEntityInstance="
        + StringUtils.defaultIfBlank(trackedEntityInstance.getTrackedEntityInstance(), "")
        + ", enrollment="
        + StringUtils.defaultIfBlank(enrollment.getEnrollment(), "")
        + ", event="
        + StringUtils.defaultIfBlank(event.getEvent(), "")
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RelationshipItem that = (RelationshipItem) o;

    return (trackedEntityInstance != null
            && that.trackedEntityInstance != null
            && Objects.equals(
                trackedEntityInstance.getTrackedEntityInstance(),
                that.trackedEntityInstance.getTrackedEntityInstance()))
        || (enrollment != null
            && that.enrollment != null
            && Objects.equals(enrollment.getEnrollment(), that.enrollment.getEnrollment()))
        || (event != null
            && that.event != null
            && Objects.equals(event.getEvent(), that.event.getEvent()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(trackedEntityInstance, enrollment, event);
  }
}
