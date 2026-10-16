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
package org.hisp.dhis.trackedentityfilter;

import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Type;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.attribute.AttributeValuesDeserializer;
import org.hisp.dhis.attribute.AttributeValuesSerializer;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseMetadataObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.ObjectStyle;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Sortable;
import org.hisp.dhis.common.TranslationProperty;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.programstagefilter.DateFilterPeriod;
import org.hisp.dhis.programstagefilter.DatePeriodType;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Gist;
import org.hisp.dhis.schema.annotation.Gist.Include;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.Property.Value;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 */
@Entity
@Table(name = "trackedentityfilter")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JacksonXmlRootElement(localName = "trackedEntityFilter", namespace = DxfNamespaces.DXF_2_0)
public class TrackedEntityFilter extends BaseMetadataObject
    implements IdentifiableObject, MetadataObject {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "trackedentityfilterid")
  private long id;

  @Column(name = "code", unique = true, length = 50)
  @AuditAttribute
  private String code;

  @Column(name = "name", nullable = false, length = 230)
  private String name;

  /** Property indicating program's of trackedEntityFilter */
  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @Fetch(FetchMode.SELECT)
  @JoinColumn(name = "programid", nullable = false)
  private Program program;

  /** Property indicating description of trackedEntityFilter */
  @Column(name = "description")
  private String description;

  /** Property indicating the filter's order in tracked entity search UI */
  @Column(name = "sortorder")
  private int sortOrder;

  /** Property indicating the filter's rendering style */
  @Type(type = "jbObjectStyle")
  @Column(name = "style")
  private ObjectStyle style;

  /** Property to filter tracked entities based on event dates and statues */
  @Type(type = "jlbEventFilter")
  @Column(name = "eventfilters")
  private List<EventFilter> eventFilters = new ArrayList<>();

  @Type(type = "jbEntityQueryCriteria")
  @Column(name = "entityquerycriteria")
  private EntityQueryCriteria entityQueryCriteria;

  @Embedded private TranslationProperty translations = new TranslationProperty();

  @Type(type = "jsbObjectSharing")
  @Column(name = "sharing")
  private Sharing sharing = new Sharing();

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public TrackedEntityFilter() {}

  // -------------------------------------------------------------------------
  // hashCode, equals and toString
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    int result = getUid() != null ? getUid().hashCode() : 0;
    result = 31 * result + (getCode() != null ? getCode().hashCode() : 0);
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);

    return result;
  }

  /** Compares uid, code and name. Class check uses the real class to handle proxied objects. */
  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof TrackedEntityFilter other
            && getRealClass(this) == getRealClass(obj)
            && Objects.equals(getUid(), other.getUid())
            && Objects.equals(getCode(), other.getCode())
            && Objects.equals(getName(), other.getName());
  }

  @Override
  public String toString() {
    return ("{\"class\":\"%s\", \"id\":\"%s\", \"uid\":\"%s\", \"code\":\"%s\", \"name\":\"%s\","
            + " \"created\":\"%s\", \"lastUpdated\":\"%s\" }")
        .formatted(
            getClass(), getId(), getUid(), getCode(), getName(), getCreated(), getLastUpdated());
  }

  // -------------------------------------------------------------------------
  // Identifiable object getters and setters
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
  @Sortable(value = false)
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @JacksonXmlProperty(isAttribute = true)
  @Property(PropertyType.URL)
  public String getHref() {
    return href;
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
  @Description("The name of this Object. Required and unique.")
  @PropertyRange(min = 1)
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "name", key = "NAME")
  public String getDisplayName() {
    return translationProperty().getTranslation("NAME", getName());
  }

  @Override
  @Gist(included = Include.FALSE)
  @Sortable(value = false)
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "translations", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "translation", namespace = DxfNamespaces.DXF_2_0)
  public Set<Translation> getTranslations() {
    return translationProperty().getTranslations();
  }

  /** Clears out the translation cache. A null set is stored as an empty set. */
  @Override
  public void setTranslations(Set<Translation> translations) {
    translationProperty().setTranslations(translations != null ? translations : new HashSet<>());
  }

  /**
   * Returns the embedded translations, which Hibernate loads as null when the translations column
   * is null.
   */
  private TranslationProperty translationProperty() {
    if (translations == null) {
      translations = new TranslationProperty();
    }

    return translations;
  }

  @Override
  @Sortable(value = false)
  @Gist(included = Include.FALSE)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Sharing getSharing() {
    if (sharing == null) {
      sharing = new Sharing();
    }

    return sharing;
  }

  @Override
  public void setSharing(Sharing sharing) {
    this.sharing = sharing;
  }

  @Override
  public void setUser(User user) {
    setCreatedBy(createdBy == null ? user : createdBy);
    setOwner(user != null ? user.getUid() : null);
  }

  @Override
  public void setOwner(String userId) {
    getSharing().setOwner(userId);
  }

  @Override
  public String getPropertyValue(IdScheme idScheme) {
    if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
      return uid;
    } else if (idScheme.is(IdentifiableProperty.CODE)) {
      return code;
    } else if (idScheme.is(IdentifiableProperty.NAME)) {
      return name;
    } else if (idScheme.is(IdentifiableProperty.ID)) {
      return id > 0 ? String.valueOf(id) : null;
    }
    return null;
  }

  @Override
  public String getDisplayPropertyValue(IdScheme idScheme) {
    if (idScheme.is(IdentifiableProperty.NAME)) {
      return getDisplayName();
    }
    return getPropertyValue(idScheme);
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JsonSerialize(as = IdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Program getProgram() {
    return program;
  }

  public void setProgram(Program program) {
    this.program = program;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "description", key = "DESCRIPTION")
  public String getDisplayDescription() {
    return translationProperty().getTranslation("DESCRIPTION", getDescription());
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ObjectStyle getStyle() {
    return style;
  }

  public void setStyle(ObjectStyle style) {
    this.style = style;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public int getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(int sortOrder) {
    this.sortOrder = sortOrder;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public EnrollmentStatus getEnrollmentStatus() {
    if (this.entityQueryCriteria != null) {
      return entityQueryCriteria.getEnrollmentStatus();
    }
    return null;
  }

  public void setEnrollmentStatus(EnrollmentStatus enrollmentStatus) {
    if (this.entityQueryCriteria == null) {
      this.entityQueryCriteria = new EntityQueryCriteria();
    }
    this.entityQueryCriteria.setEnrollmentStatus(enrollmentStatus);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean isFollowup() {
    if (this.entityQueryCriteria != null) {
      return entityQueryCriteria.getFollowUp();
    }
    return false;
  }

  public void setFollowup(Boolean followup) {
    if (this.entityQueryCriteria == null) {
      this.entityQueryCriteria = new EntityQueryCriteria();
    }
    this.entityQueryCriteria.setFollowUp(followup);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public FilterPeriod getEnrollmentCreatedPeriod() {
    if (this.entityQueryCriteria != null
        && this.entityQueryCriteria.getEnrollmentCreatedDate() != null
        && this.entityQueryCriteria.getEnrollmentCreatedDate().getType()
            == DatePeriodType.RELATIVE) {
      DateFilterPeriod enrollmentCreatedDate = this.entityQueryCriteria.getEnrollmentCreatedDate();
      FilterPeriod enrollmentCreatedPeriod = new FilterPeriod();
      enrollmentCreatedPeriod.setPeriodFrom(enrollmentCreatedDate.getStartBuffer());
      enrollmentCreatedPeriod.setPeriodTo(enrollmentCreatedDate.getEndBuffer());
      return enrollmentCreatedPeriod;
    }
    return null;
  }

  public void setEnrollmentCreatedPeriod(FilterPeriod enrollmentCreatedPeriod) {
    if (enrollmentCreatedPeriod == null) {
      return;
    }

    if (this.entityQueryCriteria == null) {
      this.entityQueryCriteria = new EntityQueryCriteria();
    }

    DateFilterPeriod enrollmentCreatedDate = new DateFilterPeriod();
    enrollmentCreatedDate.setStartBuffer(enrollmentCreatedPeriod.getPeriodFrom());
    enrollmentCreatedDate.setEndBuffer(enrollmentCreatedPeriod.getPeriodTo());
    enrollmentCreatedDate.setType(DatePeriodType.RELATIVE);
    enrollmentCreatedDate.setPeriod(RelativePeriodEnum.TODAY);
    this.entityQueryCriteria.setEnrollmentCreatedDate(enrollmentCreatedDate);
  }

  @JsonProperty("eventFilters")
  @JacksonXmlElementWrapper(localName = "eventFilters", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "eventFilters", namespace = DxfNamespaces.DXF_2_0)
  public List<EventFilter> getEventFilters() {
    return eventFilters;
  }

  public void setEventFilters(List<EventFilter> eventFilters) {
    this.eventFilters = eventFilters;
  }

  @JsonProperty("entityQueryCriteria")
  @JacksonXmlElementWrapper(localName = "entityQueryCriteria", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "entityQueryCriteria", namespace = DxfNamespaces.DXF_2_0)
  public EntityQueryCriteria getEntityQueryCriteria() {
    return entityQueryCriteria;
  }

  public void setEntityQueryCriteria(EntityQueryCriteria entityQueryCriteria) {
    this.entityQueryCriteria = entityQueryCriteria;
  }

  // -------------------------------------------------------------------------
  // Unsupported IdentifiableObject methods
  // -------------------------------------------------------------------------

  /** TrackedEntityFilter does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  @OpenApi.Property(BaseIdentifiableObject.AttributeValue[].class)
  @JsonProperty("attributeValues")
  @JsonDeserialize(using = AttributeValuesDeserializer.class)
  @JsonSerialize(using = AttributeValuesSerializer.class)
  public AttributeValues getAttributeValues() {
    return AttributeValues.empty();
  }

  /** TrackedEntityFilter does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void setAttributeValues(AttributeValues attributeValues) {
    // Not supported
  }

  /** TrackedEntityFilter does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void addAttributeValue(String attributeUid, String value) {
    // Not supported
  }

  /** TrackedEntityFilter does not support attribute values. */
  @Override
  @Deprecated(since = "2.44")
  public void removeAttributeValue(String attributeId) {
    // Not supported
  }
}
