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
package org.hisp.dhis.metadata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.util.Date;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.PrimaryKeyObject;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.Property.Value;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.schema.annotation.PropertyTransformer;
import org.hisp.dhis.schema.transformer.UserPropertyTransformer;
import org.hisp.dhis.user.User;

/**
 * A proposal is a record about a change proposed by a user to add, update or remove a certain
 * metadata object.
 *
 * @author Jan Bernitt
 */
@Entity
@Table(name = "metadataproposal")
@Builder
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JacksonXmlRootElement(localName = "metadataProposal", namespace = DxfNamespaces.DXF_2_0)
public class MetadataProposal implements PrimaryKeyObject {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "proposalid")
  private long id;

  @Column(name = "uid", unique = true, nullable = false, length = 11)
  private String uid;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 6)
  private MetadataProposalType type;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 12)
  private MetadataProposalStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "target", nullable = false, length = 30)
  private MetadataProposalTarget target;

  @Column(name = "targetuid", length = 11)
  private String targetId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "createdby",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_createdby_userid"))
  private User createdBy;

  @Column(name = "created", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;

  // the concrete Jackson node types are Serializable, and the field must stay persistent
  @SuppressWarnings("java:S1948")
  @Type(type = "jbJsonNode")
  @Column(name = "change", nullable = false)
  private JsonNode change;

  @Column(name = "comment")
  private String comment;

  @Column(name = "reason", length = 1024)
  private String reason;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "finalisedby", foreignKey = @ForeignKey(name = "fk_finalisedby_userid"))
  private User finalisedBy;

  @Column(name = "finalised")
  @Temporal(TemporalType.TIMESTAMP)
  private Date finalised;

  @Override
  @JsonIgnore
  public long getId() {
    return id;
  }

  @Override
  @JsonProperty(value = "id")
  @JacksonXmlProperty(localName = "id", isAttribute = true)
  @Description("The Unique Identifier for this Object.")
  @Property(value = PropertyType.IDENTIFIER, required = Value.FALSE)
  @PropertyRange(min = 11, max = 11)
  public String getUid() {
    return uid;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public MetadataProposalType getType() {
    return type;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public MetadataProposalStatus getStatus() {
    return status;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public MetadataProposalTarget getTarget() {
    return target;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getTargetId() {
    return targetId;
  }

  @OpenApi.Property(UserPropertyTransformer.UserDto.class)
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getCreatedBy() {
    return createdBy;
  }

  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The date this object was created.")
  @Property(value = PropertyType.DATE, required = Value.TRUE)
  public Date getCreated() {
    return created;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public JsonNode getChange() {
    return change;
  }

  @JsonProperty
  @Description("An optional comment provided by the creator to explain the proposal")
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getComment() {
    return comment;
  }

  @JsonProperty
  @Description("The reason why the proposal was rejected or why it failed and needs update")
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getReason() {
    return reason;
  }

  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The date this object was either accepted or rejected.")
  @Property(value = PropertyType.DATE)
  public Date getFinalised() {
    return finalised;
  }

  @OpenApi.Property(UserPropertyTransformer.UserDto.class)
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getFinalisedBy() {
    return finalisedBy;
  }

  public void setAutoFields() {
    if (uid == null || uid.length() == 0) {
      uid = CodeGenerator.generateUid();
    }
    if (created == null) {
      created = new Date();
    }
    if (status == null) {
      status = MetadataProposalStatus.PROPOSED;
    }
    if (type == MetadataProposalType.REMOVE) {
      change = NullNode.getInstance();
    }
  }
}
