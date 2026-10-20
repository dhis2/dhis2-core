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
package org.hisp.dhis.program;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import org.hibernate.annotations.Type;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.attribute.AttributeValuesDeserializer;
import org.hisp.dhis.attribute.AttributeValuesSerializer;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Sortable;
import org.hisp.dhis.common.adapter.DeviceRenderTypeMapSerializer;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.render.DeviceRenderTypeMap;
import org.hisp.dhis.render.type.ValueTypeRenderingObject;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Gist;
import org.hisp.dhis.schema.annotation.Gist.Include;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.Property.Value;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.schema.annotation.PropertyTransformer;
import org.hisp.dhis.schema.transformer.UserPropertyTransformer;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * A data element of a {@link ProgramStage}, with the settings that apply to it in that stage. It is
 * an embedded object owned by {@link ProgramStage#getProgramStageDataElements()}.
 *
 * <p>The programstagedataelement table has no name, userid, sharing, translations or
 * attributevalues column, so this entity implements {@link IdentifiableObject} directly instead of
 * extending {@code BaseMetadataObject} (which maps {@code createdBy} to userid). Translations,
 * {@code href} and {@code access} are kept in memory only; the other unsupported properties are
 * no-op stubs at the end of the class.
 *
 * @author Viet Nguyen
 */
@Entity
@Table(
    name = "programstagedataelement",
    uniqueConstraints =
        @UniqueConstraint(
            name = "programstagedataelement_unique_key",
            columnNames = {"programstageid", "dataelementid"}))
@JacksonXmlRootElement(localName = "programStageDataElement", namespace = DxfNamespaces.DXF_2_0)
public class ProgramStageDataElement implements IdentifiableObject, EmbeddedObject {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "programstagedataelementid")
  private long id;

  @AuditAttribute
  @Column(name = "uid", unique = true, nullable = false, length = 11)
  private String uid;

  @AuditAttribute
  @Column(name = "code", unique = true, length = 50)
  private String code;

  @Column(name = "created", nullable = false, updatable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;

  @Column(name = "lastupdated", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastUpdated;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lastupdatedby")
  private User lastUpdatedBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "programstageid")
  private ProgramStage programStage;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dataelementid", nullable = false)
  private DataElement dataElement;

  /** Whether data element is mandatory for program stage. */
  @Column(name = "compulsory", nullable = false)
  private boolean compulsory = false;

  @Column(name = "allowprovidedelsewhere")
  private Boolean allowProvidedElsewhere = false;

  /** Sort order of data element within program stage. */
  @Column(name = "sort_order")
  private Integer sortOrder;

  @Column(name = "displayinreports")
  private Boolean displayInReports = false;

  /** Whether to allow capture of events in the future. */
  @Column(name = "allowfuturedate")
  private Boolean allowFutureDate = false;

  // Remove this in the future, will be replaced by renderType
  @Column(name = "renderoptionsasradio")
  private Boolean renderOptionsAsRadio = false;

  @Type(type = "jbValueRenderType")
  @Column(name = "rendertype", columnDefinition = "jsonb")
  private DeviceRenderTypeMap<ValueTypeRenderingObject> renderType;

  /** Whether to skip data element in data synchronization. */
  @Column(name = "skipsynchronization")
  private Boolean skipSynchronization = false;

  /** Whether to skip data element in analytics tables and queries, not null. */
  @Column(name = "skipanalytics", nullable = false)
  private Boolean skipAnalytics = false;

  /** Link to this object in the API, set when it is rendered by the web layer. */
  @Transient private transient String href;

  /** Access information for this object. Applies to current user. */
  @Transient private transient Access access;

  /**
   * Not persisted, there is no translations column. Kept in memory so that the translations check
   * still rejects translations on this non-translatable type instead of dropping them.
   */
  @Transient private transient Set<Translation> translations = new HashSet<>();

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public ProgramStageDataElement() {}

  public ProgramStageDataElement(ProgramStage programStage, DataElement dataElement) {
    this.programStage = programStage;
    this.dataElement = dataElement;
  }

  public ProgramStageDataElement(
      ProgramStage programStage, DataElement dataElement, boolean compulsory) {
    this.programStage = programStage;
    this.dataElement = dataElement;
    this.compulsory = compulsory;
  }

  public ProgramStageDataElement(
      ProgramStage programStage, DataElement dataElement, boolean compulsory, Integer sortOrder) {
    this.programStage = programStage;
    this.dataElement = dataElement;
    this.compulsory = compulsory;
    this.sortOrder = sortOrder;
  }

  // -------------------------------------------------------------------------
  // Get and set methods
  // -------------------------------------------------------------------------

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ProgramStage getProgramStage() {
    return programStage;
  }

  public void setProgramStage(ProgramStage programStage) {
    this.programStage = programStage;
  }

  @JsonProperty
  @JsonSerialize(as = IdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public DataElement getDataElement() {
    return dataElement;
  }

  public void setDataElement(DataElement dataElement) {
    this.dataElement = dataElement;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getAllowProvidedElsewhere() {
    return allowProvidedElsewhere;
  }

  public void setAllowProvidedElsewhere(Boolean allowProvidedElsewhere) {
    this.allowProvidedElsewhere = allowProvidedElsewhere;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isCompulsory() {
    return compulsory;
  }

  public void setCompulsory(boolean compulsory) {
    this.compulsory = compulsory;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getDisplayInReports() {
    return displayInReports;
  }

  public void setDisplayInReports(Boolean displayInReports) {
    this.displayInReports = displayInReports;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getAllowFutureDate() {
    return allowFutureDate;
  }

  public void setAllowFutureDate(Boolean allowFutureDate) {
    this.allowFutureDate = allowFutureDate;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getRenderOptionsAsRadio() {
    return renderOptionsAsRadio;
  }

  public void setRenderOptionsAsRadio(Boolean renderOptionsAsRadio) {
    this.renderOptionsAsRadio = renderOptionsAsRadio;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @JsonSerialize(using = DeviceRenderTypeMapSerializer.class)
  public DeviceRenderTypeMap<ValueTypeRenderingObject> getRenderType() {
    return renderType;
  }

  public void setRenderType(DeviceRenderTypeMap<ValueTypeRenderingObject> renderType) {
    this.renderType = renderType;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getSkipSynchronization() {
    return skipSynchronization;
  }

  public void setSkipSynchronization(Boolean skipSynchronization) {
    this.skipSynchronization = skipSynchronization;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getSkipAnalytics() {
    return skipAnalytics;
  }

  public void setSkipAnalytics(Boolean skipAnalytics) {
    this.skipAnalytics = skipAnalytics;
  }

  // -------------------------------------------------------------------------
  // IdentifiableObject implementation
  // -------------------------------------------------------------------------

  @Override
  @JsonIgnore
  public long getId() {
    return id;
  }

  @Override
  public void setId(long id) {
    this.id = id;
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

  @Override
  public void setUid(String uid) {
    this.uid = uid;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The unique code for this Object.")
  @Property(PropertyType.IDENTIFIER)
  public String getCode() {
    return code;
  }

  @Override
  public void setCode(String code) {
    this.code = code;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The date this object was created.")
  @Property(value = PropertyType.DATE, required = Value.FALSE)
  public Date getCreated() {
    return created;
  }

  @Override
  public void setCreated(Date created) {
    this.created = created;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The date this object was last updated.")
  @Property(value = PropertyType.DATE, required = Value.FALSE)
  public Date getLastUpdated() {
    return lastUpdated;
  }

  @Override
  public void setLastUpdated(Date lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  @Override
  @OpenApi.Property(UserPropertyTransformer.UserDto.class)
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  @Override
  public void setLastUpdatedBy(User lastUpdatedBy) {
    this.lastUpdatedBy = lastUpdatedBy;
  }

  @Override
  @Sortable(value = false)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @JacksonXmlProperty(isAttribute = true)
  @Property(PropertyType.URL)
  public String getHref() {
    return href;
  }

  @Override
  public void setHref(String href) {
    this.href = href;
  }

  @Override
  @Sortable(value = false)
  @Gist(included = Include.FALSE)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @JacksonXmlProperty(localName = "access", namespace = DxfNamespaces.DXF_2_0)
  public Access getAccess() {
    return access;
  }

  @Override
  public void setAccess(Access access) {
    this.access = access;
  }

  @Override
  @Gist(included = Include.FALSE)
  @Sortable(value = false)
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "translations", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "translation", namespace = DxfNamespaces.DXF_2_0)
  public Set<Translation> getTranslations() {
    if (translations == null) {
      translations = new HashSet<>();
    }

    return translations;
  }

  @Override
  public void setTranslations(Set<Translation> translations) {
    this.translations = translations;
  }

  @Override
  public String getPropertyValue(IdScheme idScheme) {
    if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
      return uid;
    } else if (idScheme.is(IdentifiableProperty.CODE)) {
      return code;
    } else if (idScheme.is(IdentifiableProperty.ID)) {
      return id > 0 ? String.valueOf(id) : null;
    }
    return null;
  }

  @Override
  public String getDisplayPropertyValue(IdScheme idScheme) {
    return getPropertyValue(idScheme);
  }

  // -------------------------------------------------------------------------
  // hashCode, equals and toString
  // -------------------------------------------------------------------------

  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof ProgramStageDataElement && objectEquals((ProgramStageDataElement) obj);
  }

  private boolean objectEquals(ProgramStageDataElement other) {
    return Objects.equals(dataElement, other.dataElement)
        && Objects.equals(programStage, other.programStage);
  }

  @Override
  public int hashCode() {
    int result = programStage != null ? programStage.hashCode() : 0;
    result = 31 * result + (dataElement != null ? dataElement.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return String.format(
        """
        ProgramStageDataElement{
          programStage=%s,
          dataElement=%s,
          compulsory=%s,
          allowProvidedElsewhere=%s,
          sortOrder=%s,
          displayInReports=%s,
          allowFutureDate=%s,
          renderOptionsAsRadio=%s
        }""",
        programStage,
        dataElement,
        compulsory,
        allowProvidedElsewhere,
        sortOrder,
        displayInReports,
        allowFutureDate,
        renderOptionsAsRadio);
  }

  public static final BiFunction<ProgramStageDataElement, ProgramStage, ProgramStageDataElement>
      copyOf =
          (original, stage) -> {
            ProgramStageDataElement copy = new ProgramStageDataElement();
            copy.setProgramStage(stage);
            copy.setAutoFields();
            setShallowCopyValues(copy, original);
            return copy;
          };

  private static void setShallowCopyValues(
      ProgramStageDataElement copy, ProgramStageDataElement original) {
    copy.setAllowFutureDate(original.getAllowFutureDate());
    copy.setAllowProvidedElsewhere(original.getAllowProvidedElsewhere());
    copy.setCompulsory(original.isCompulsory());
    copy.setDataElement(original.getDataElement());
    copy.setDisplayInReports(original.getDisplayInReports());
    copy.setLastUpdatedBy(original.getLastUpdatedBy());
    copy.setRenderOptionsAsRadio(original.getRenderOptionsAsRadio());
    copy.setRenderType(original.getRenderType());
    copy.setSkipAnalytics(original.getSkipAnalytics());
    copy.setSkipSynchronization(original.getSkipSynchronization());
    copy.setSortOrder(original.getSortOrder());
  }

  // -------------------------------------------------------------------------
  // Unsupported IdentifiableObject methods
  // -------------------------------------------------------------------------

  /** ProgramStageDataElement does not support name. */
  @Override
  @Deprecated(since = "2.44")
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The name of this Object. Required and unique.")
  @PropertyRange(min = 1)
  public String getName() {
    return null;
  }

  /** ProgramStageDataElement does not support name. */
  @Override
  @Deprecated(since = "2.44")
  public void setName(String name) {
    // Not supported
  }

  /** ProgramStageDataElement does not support name, so it has no display name. */
  @Override
  @Deprecated(since = "2.44")
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "name", key = "NAME")
  public String getDisplayName() {
    return null;
  }

  /** ProgramStageDataElement does not support created by. */
  @Override
  @Deprecated(since = "2.44")
  @Gist(included = Include.FALSE)
  @OpenApi.Property(UserPropertyTransformer.UserDto.class)
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getCreatedBy() {
    return null;
  }

  /** ProgramStageDataElement does not support created by. */
  @Override
  @Deprecated(since = "2.44")
  public void setCreatedBy(User createdBy) {
    // Not supported
  }

  /** ProgramStageDataElement does not support created by. */
  @Override
  @Deprecated(since = "2.44")
  @OpenApi.Ignore
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getUser() {
    return null;
  }

  /** ProgramStageDataElement does not support created by or sharing. */
  @Override
  @Deprecated(since = "2.44")
  public void setUser(User user) {
    // Not supported
  }

  /** ProgramStageDataElement does not support sharing. */
  @Override
  @Deprecated(since = "2.44")
  public void setOwner(String owner) {
    // Not supported
  }

  /**
   * ProgramStageDataElement does not support sharing: there is no sharing column, so writes through
   * the returned object are discarded.
   */
  @Override
  @Deprecated(since = "2.44")
  @Sortable(value = false)
  @Gist(included = Include.FALSE)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Sharing getSharing() {
    return Sharing.empty();
  }

  /** ProgramStageDataElement does not support sharing. */
  @Override
  @Deprecated(since = "2.44")
  public void setSharing(Sharing sharing) {
    // Not supported
  }

  /** ProgramStageDataElement does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  @OpenApi.Property(BaseIdentifiableObject.AttributeValue[].class)
  @JsonProperty("attributeValues")
  @JsonDeserialize(using = AttributeValuesDeserializer.class)
  @JsonSerialize(using = AttributeValuesSerializer.class)
  public AttributeValues getAttributeValues() {
    return AttributeValues.empty();
  }

  /** ProgramStageDataElement does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void setAttributeValues(AttributeValues attributeValues) {
    // Not supported
  }

  /** ProgramStageDataElement does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void addAttributeValue(String attributeUid, String value) {
    // Not supported
  }

  /** ProgramStageDataElement does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void removeAttributeValue(String attributeId) {
    // Not supported
  }
}
