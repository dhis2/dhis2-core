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
package org.hisp.dhis.tracker.audit;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import org.hisp.dhis.audit.AuditOperationType;

/**
 * @author Abyot Asalefew Gizaw abyota@gmail.com
 */
@Deprecated
@Entity
@Table(name = "trackedentityaudit")
public class TrackedEntityAudit implements Serializable {
  private static final long serialVersionUID = 4260110537887403524L;

  @Id
  @GeneratedValue(
      strategy = GenerationType.SEQUENCE,
      generator = "trackedentityinstanceaudit_sequence")
  @SequenceGenerator(
      name = "trackedentityinstanceaudit_sequence",
      sequenceName = "trackedentityinstanceaudit_sequence",
      allocationSize = 1)
  @Column(name = "trackedentityauditid")
  private long id;

  @Column(name = "trackedentity")
  private String trackedEntity;

  @Column(name = "comment", length = 50000)
  private String comment;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "created")
  private Date created;

  @Column(name = "accessedby")
  private String accessedBy;

  @Enumerated(EnumType.STRING)
  @Column(name = "audittype", length = 100, nullable = false)
  private AuditOperationType auditType;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public TrackedEntityAudit() {}

  public TrackedEntityAudit(String trackedEntity, String accessedBy, AuditOperationType auditType) {
    this.trackedEntity = trackedEntity;
    this.accessedBy = accessedBy;
    this.created = new Date();
    this.auditType = auditType;
  }

  public TrackedEntityAudit(
      String trackedEntity,
      String comment,
      Date created,
      String accessedBy,
      AuditOperationType auditOperationType) {
    this(trackedEntity, accessedBy, auditOperationType);
    this.comment = comment;
    this.created = created;
  }

  @Override
  public int hashCode() {
    return Objects.hash(trackedEntity, comment, created, accessedBy, auditType);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final TrackedEntityAudit other = (TrackedEntityAudit) obj;

    return Objects.equals(this.trackedEntity, other.trackedEntity)
        && Objects.equals(this.comment, other.comment)
        && Objects.equals(this.created, other.created)
        && Objects.equals(this.accessedBy, other.accessedBy)
        && Objects.equals(this.auditType, other.auditType);
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  @JsonProperty
  public String getTrackedEntity() {
    return trackedEntity;
  }

  public void setTrackedEntity(String trackedEntity) {
    this.trackedEntity = trackedEntity;
  }

  @JsonProperty
  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  @JsonProperty
  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  @JsonProperty
  public String getAccessedBy() {
    return accessedBy;
  }

  public void setAccessedBy(String accessedBy) {
    this.accessedBy = accessedBy;
  }

  @JsonProperty
  public AuditOperationType getAuditType() {
    return auditType;
  }

  public void setAuditType(AuditOperationType auditType) {
    this.auditType = auditType;
  }
}
