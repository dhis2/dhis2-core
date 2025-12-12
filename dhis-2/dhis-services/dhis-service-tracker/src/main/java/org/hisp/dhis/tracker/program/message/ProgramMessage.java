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
package org.hisp.dhis.tracker.program.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.MoreObjects;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
@Entity
@Table(name = "programmessage")
@Getter
@Setter
@Builder(builderClassName = "ProgramMessageBuilder")
@NoArgsConstructor
@AllArgsConstructor
public class ProgramMessage implements IdentifiableObject, Serializable {
  private static final long serialVersionUID = -5882823752156937730L;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "id")
  private long id;

  @Column(name = "uid", unique = true, nullable = false, length = 11)
  protected String uid;

  @JsonProperty
  @Column(name = "code", length = 50)
  @AuditAttribute
  protected String code;

  @Column(name = "created", nullable = false, updatable = false)
  @Temporal(TemporalType.TIMESTAMP)
  protected Date created;

  @Column(name = "lastUpdated", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  protected Date lastUpdated;

  @JsonProperty
  @JsonSerialize(as = IdentifiableObject.class)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enrollmentid", referencedColumnName = "enrollmentid")
  private Enrollment enrollment;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trackereventid", referencedColumnName = "eventid")
  private TrackerEvent trackerEvent;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "singleeventid", referencedColumnName = "eventid")
  private SingleEvent singleEvent;

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  private transient TrackerEvent event;

  @JsonProperty @Embedded private ProgramMessageRecipients recipients;

  @JsonProperty
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "programmessage_deliverychannels",
      joinColumns = @JoinColumn(name = "programmessagedeliverychannelsid"))
  @Column(name = "deliverychannel")
  @Enumerated(EnumType.STRING)
  private Set<DeliveryChannel> deliveryChannels = new HashSet<>();

  @JsonProperty
  @Enumerated(EnumType.STRING)
  @Column(name = "messagestatus", length = 50)
  private ProgramMessageStatus messageStatus;

  @JsonProperty
  @Column(name = "notificationtemplate")
  private String notificationTemplate;

  @JsonProperty
  @Column(name = "subject", length = 200)
  private String subject;

  @JsonProperty
  @Column(name = "text", nullable = false, length = 500)
  private String text;

  @JsonProperty
  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "processeddate")
  private Date processedDate;

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public boolean hasEnrollment() {
    return this.enrollment != null;
  }

  public boolean hasTrackerEvent() {
    return this.trackerEvent != null;
  }

  public boolean hasSingleEvent() {
    return this.singleEvent != null;
  }

  @Override
  public String getName() {
    return "";
  }

  @Override
  public String getDisplayName() {
    return getName();
  }

  @Override
  public User getLastUpdatedBy() {
    return null;
  }

  @Override
  public AttributeValues getAttributeValues() {
    return null;
  }

  @Override
  public void setAttributeValues(AttributeValues attributeValues) {
    // not supported
  }

  @Override
  public void addAttributeValue(String attributeUid, String value) {
    // not supported
  }

  @Override
  public void removeAttributeValue(String attributeId) {
    // not supported
  }

  @Override
  public Set<Translation> getTranslations() {
    return Set.of();
  }

  @Override
  public void setAccess(Access access) {
    // not supported
  }

  @Override
  public User getCreatedBy() {
    return null;
  }

  @Override
  public User getUser() {
    return null;
  }

  @Override
  public void setCreatedBy(User createdBy) {
    // not supported
  }

  @Override
  public void setUser(User user) {
    // not supported
  }

  @Override
  public Access getAccess() {
    return null;
  }

  @Override
  public Sharing getSharing() {
    return Sharing.empty();
  }

  @Override
  public void setSharing(Sharing sharing) {
    // not supported
  }

  @Override
  public String getPropertyValue(IdScheme idScheme) {
    return "";
  }

  @Override
  public String getDisplayPropertyValue(IdScheme idScheme) {
    return "";
  }

  @Override
  public void setName(String name) {
    // not supported
  }

  @Override
  public void setOwner(String owner) {
    // not supported
  }

  @Override
  public void setTranslations(Set<Translation> translations) {
    // not supported
  }

  @Override
  public void setLastUpdatedBy(User user) {
    // not supported
  }

  @Override
  public String getHref() {
    return "";
  }

  @Override
  public void setHref(String link) {
    // not supported
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static final class ProgramMessageBuilder {}

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("uid", uid)
        .add("tracker trackerEvent", trackerEvent)
        .add("single trackerEvent", singleEvent)
        .add("enrollment", enrollment)
        .add("recipients", recipients)
        .add("delivery channels", deliveryChannels)
        .add("subject", subject)
        .add("text", text)
        .toString();
  }
}
