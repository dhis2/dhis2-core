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

import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.common.DxfNamespaces.DXF_2_0;
import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;
import static org.hisp.dhis.util.ObjectUtils.copyOf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Type;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.attribute.AttributeValuesDeserializer;
import org.hisp.dhis.attribute.AttributeValuesSerializer;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseMetadataObject;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Sortable;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.common.ObjectStyle;
import org.hisp.dhis.common.TranslationProperty;
import org.hisp.dhis.common.VersionedObject;
import org.hisp.dhis.common.adapter.JacksonPeriodTypeDeserializer;
import org.hisp.dhis.common.adapter.JacksonPeriodTypeSerializer;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Gist;
import org.hisp.dhis.schema.annotation.Gist.Include;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.setting.UserSettings;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * Programe entity object.
 *
 * <p>Note that "incident date" is superseded by "occurred date".
 *
 * @author Abyot Asalefew
 */
@Entity
@Table(name = "program")
@JacksonXmlRootElement(localName = "program", namespace = DxfNamespaces.DXF_2_0)
public class Program extends BaseMetadataObject
    implements IdentifiableObject, NameableObject, VersionedObject {
  static final String DEFAULT_PREFIX = "Copy of ";

  static final String PREFIX_KEY = "prefix";

  // -------------------------------------------------------------------------
  // Fields from identifiableProperties (not in BaseMetadataObject)
  // -------------------------------------------------------------------------

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "programid")
  private long id;

  @Column(name = "code", unique = true, length = 50)
  private String code;

  // -------------------------------------------------------------------------
  // Fields from Program.hbm.xml
  // -------------------------------------------------------------------------

  @Column(name = "name", nullable = false, length = 230)
  private String name;

  @Column(name = "shortname", nullable = false, length = 50)
  private String shortName;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Column(name = "formname", columnDefinition = "text")
  private String formName;

  @Column(name = "version")
  private int version;

  @Column(name = "enrollmentdatelabel", columnDefinition = "text")
  private String enrollmentDateLabel;

  @Column(name = "incidentdatelabel", columnDefinition = "text")
  private String incidentDateLabel;

  @Column(name = "enrollmentlabel", columnDefinition = "text")
  private String enrollmentLabel;

  @Column(name = "followuplabel", columnDefinition = "text")
  private String followUpLabel;

  @Column(name = "orgunitlabel", columnDefinition = "text")
  private String orgUnitLabel;

  @Column(name = "relationshiplabel", columnDefinition = "text")
  private String relationshipLabel;

  @Column(name = "notelabel", columnDefinition = "text")
  private String noteLabel;

  @Column(name = "trackedentityattributelabel", columnDefinition = "text")
  private String trackedEntityAttributeLabel;

  @Column(name = "programstagelabel", columnDefinition = "text")
  private String programStageLabel;

  @Column(name = "eventlabel", columnDefinition = "text")
  private String eventLabel;

  @ManyToMany
  @JoinTable(
      name = "program_organisationunits",
      joinColumns = @JoinColumn(name = "programid"),
      inverseJoinColumns = @JoinColumn(name = "organisationunitid"))
  private Set<OrganisationUnit> organisationUnits = new HashSet<>();

  @OneToMany
  @JoinColumn(name = "programid")
  @OrderBy("sortOrder")
  private Set<ProgramStage> programStages = new HashSet<>();

  @OneToMany
  @JoinColumn(name = "programid")
  @OrderBy("sortOrder")
  private Set<ProgramSection> programSections = new HashSet<>();

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private ProgramType programType;

  @Column(name = "displayincidentdate")
  private Boolean displayIncidentDate = true;

  @Column(name = "ignoreoverdueevents")
  private Boolean ignoreOverdueEvents = false;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "programid")
  @OrderColumn(name = "sort_order")
  private List<ProgramTrackedEntityAttribute> programAttributes = new ArrayList<>();

  @ManyToMany
  @JoinTable(
      name = "program_userroles",
      joinColumns = @JoinColumn(name = "programid"),
      inverseJoinColumns = @JoinColumn(name = "userroleid"))
  private Set<UserRole> userRoles = new HashSet<>();

  @OneToMany(mappedBy = "program")
  private Set<ProgramIndicator> programIndicators = new HashSet<>();

  @OneToMany(mappedBy = "program")
  private Set<ProgramRuleVariable> programRuleVariables = new HashSet<>();

  @Column(name = "onlyenrollonce")
  private Boolean onlyEnrollOnce = false;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "programid")
  private Set<ProgramNotificationTemplate> notificationTemplates = new HashSet<>();

  @Column(name = "selectenrollmentdatesinfuture")
  private Boolean selectEnrollmentDatesInFuture = false;

  @Column(name = "selectincidentdatesinfuture")
  private Boolean selectIncidentDatesInFuture = false;

  @ManyToOne
  @JoinColumn(name = "relatedprogramid")
  private Program relatedProgram;

  @ManyToOne
  @JoinColumn(name = "trackedentitytypeid")
  private TrackedEntityType trackedEntityType;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "dataentryformid")
  private DataEntryForm dataEntryForm;

  @Type(type = "jbObjectStyle")
  @Column(name = "style")
  private ObjectStyle style;

  /** The CategoryCombo used for tracker and single events. */
  @ManyToOne
  @JoinColumn(name = "categorycomboid", nullable = false)
  private CategoryCombo categoryCombo;

  /** The CategoryCombo used for enrollments. */
  @ManyToOne
  @JoinColumn(name = "enrollmentcategorycomboid", nullable = false)
  private CategoryCombo enrollmentCategoryCombo;

  /** Property indicating whether offline storage is enabled for this program or not */
  @Column(name = "skipoffline", nullable = false)
  private boolean skipOffline;

  @Column(name = "displayfrontpagelist")
  private Boolean displayFrontPageList = false;

  @Column(name = "usefirststageduringregistration")
  private Boolean useFirstStageDuringRegistration = false;

  @Enumerated(EnumType.STRING)
  @Column(name = "featuretype")
  private FeatureType featureType;

  @Column(name = "expirydays")
  private int expiryDays;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "expiryperiodtypeid")
  private PeriodType expiryPeriodType;

  @Column(name = "completeeventsexpirydays")
  private int completeEventsExpiryDays;

  @Column(name = "opendaysaftercoenddate")
  private int openDaysAfterCoEndDate;

  @Column(name = "minattributesrequiredtosearch")
  private int minAttributesRequiredToSearch = 1;

  @Column(name = "maxteicounttoreturn")
  private int maxTeiCountToReturn = 0;

  @Enumerated(EnumType.STRING)
  @Column(name = "accesslevel", length = 100)
  private AccessLevel accessLevel = AccessLevel.OPEN;

  @Type(type = "jsbProgramCategoryMappings")
  @Column(name = "categorymappings")
  private Set<ProgramCategoryMapping> categoryMappings = new HashSet<>();

  @Column(name = "enablechangelog")
  private boolean enableChangeLog;

  // -------------------------------------------------------------------------
  // Shared metadata fields (translations, sharing, attributeValues)
  // -------------------------------------------------------------------------

  @Embedded private TranslationProperty translations = new TranslationProperty();

  @Type(type = "jsbObjectSharing")
  @Column(name = "sharing")
  private Sharing sharing = new Sharing();

  @Type(type = "jsbAttributeValues")
  @Column(name = "attributevalues")
  private AttributeValues attributeValues = AttributeValues.empty();

  /** Translation cache for display properties. */
  private final transient Map<String, String> translationCache = new ConcurrentHashMap<>();

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public Program() {}

  public Program(String name) {
    this.name = name;
  }

  public Program(String name, String description) {
    this.name = name;
    this.description = description;
  }

  // -------------------------------------------------------------------------
  // Logic methods
  // -------------------------------------------------------------------------

  public void addOrganisationUnit(OrganisationUnit organisationUnit) {
    organisationUnits.add(organisationUnit);
    organisationUnit.getPrograms().add(this);
  }

  public void addOrganisationUnits(Set<OrganisationUnit> organisationUnits) {
    organisationUnits.forEach(this::addOrganisationUnit);
  }

  public boolean removeOrganisationUnit(OrganisationUnit organisationUnit) {
    organisationUnits.remove(organisationUnit);
    return organisationUnit.getPrograms().remove(this);
  }

  public void removeOrganisationUnits(Set<OrganisationUnit> organisationUnits) {
    organisationUnits.forEach(this::removeOrganisationUnit);
  }

  /** Returns IDs of searchable TrackedEntityAttributes. */
  public List<String> getSearchableAttributeIds() {
    return programAttributes.stream()
        .filter(pa -> pa.getAttribute().isSystemWideUnique() || pa.isSearchable())
        .map(ProgramTrackedEntityAttribute::getAttribute)
        .map(TrackedEntityAttribute::getUid)
        .collect(Collectors.toList());
  }

  /** Returns display in list TrackedEntityAttributes */
  public List<TrackedEntityAttribute> getDisplayInListAttributes() {
    return programAttributes.stream()
        .filter(pa -> pa.isDisplayInList())
        .map(ProgramTrackedEntityAttribute::getAttribute)
        .collect(Collectors.toList());
  }

  /**
   * Returns the ProgramTrackedEntityAttribute of this Program which contains the given
   * TrackedEntityAttribute.
   */
  public ProgramTrackedEntityAttribute getAttribute(TrackedEntityAttribute attribute) {
    for (ProgramTrackedEntityAttribute programAttribute : programAttributes) {
      if (programAttribute != null && programAttribute.getAttribute().equals(attribute)) {
        return programAttribute;
      }
    }

    return null;
  }

  /** Returns all data elements which are part of the stages of this program. */
  public Set<DataElement> getDataElements() {
    return programStages.stream().flatMap(ps -> ps.getDataElements().stream()).collect(toSet());
  }

  /**
   * Returns all data elements which are part of the stages of this program and is not skipped in
   * analytics.
   */
  public Set<DataElement> getAnalyticsDataElements() {
    return programStages.stream()
        .map(ProgramStage::getProgramStageDataElements)
        .flatMap(Collection::stream)
        .filter(Objects::nonNull)
        .filter(psde -> !psde.getSkipAnalytics())
        .map(ProgramStageDataElement::getDataElement)
        .collect(toSet());
  }

  /**
   * Returns data elements which are part of the stages of this program which have a legend set and
   * is of numeric value type.
   */
  public Set<DataElement> getAnalyticsDataElementsWithLegendSet() {
    return getAnalyticsDataElements().stream()
        .filter(de -> de.hasLegendSet() && de.isNumericType())
        .collect(toSet());
  }

  /**
   * Returns TrackedEntityAttributes from ProgramTrackedEntityAttributes. Use getAttributes() to
   * access the persisted attribute list.
   */
  public List<TrackedEntityAttribute> getTrackedEntityAttributes() {
    return programAttributes.stream()
        .map(ProgramTrackedEntityAttribute::getAttribute)
        .collect(Collectors.toList());
  }

  /**
   * Returns non-confidential TrackedEntityAttributes from ProgramTrackedEntityAttributes. Use
   * getAttributes() to access the persisted attribute list.
   */
  public List<TrackedEntityAttribute> getNonConfidentialTrackedEntityAttributes() {
    return getTrackedEntityAttributes().stream()
        .filter(a -> !a.isConfidentialBool())
        .collect(Collectors.toList());
  }

  /**
   * Returns TrackedEntityAttributes from ProgramTrackedEntityAttributes which have a legend set and
   * is of numeric value type.
   */
  public List<TrackedEntityAttribute> getNonConfidentialTrackedEntityAttributesWithLegendSet() {
    return getTrackedEntityAttributes().stream()
        .filter(a -> !a.isConfidentialBool() && a.hasLegendSet() && a.isNumericType())
        .collect(Collectors.toList());
  }

  /** Indicates whether this program contains the given data element. */
  public boolean containsDataElement(DataElement dataElement) {
    for (ProgramStage stage : programStages) {
      for (ProgramStageDataElement element : stage.getProgramStageDataElements()) {
        if (dataElement.getUid().equals(element.getDataElement().getUid())) {
          return true;
        }
      }
    }

    return false;
  }

  /** Indicates whether this program contains the given tracked entity attribute. */
  public boolean containsAttribute(TrackedEntityAttribute attribute) {
    for (ProgramTrackedEntityAttribute programAttribute : programAttributes) {
      if (attribute.equals(programAttribute.getAttribute())) {
        return true;
      }
    }

    return false;
  }

  public ProgramStage getProgramStageByStage(int stage) {
    int count = 1;

    for (ProgramStage programStage : programStages) {
      if (count == stage) {
        return programStage;
      }

      count++;
    }

    return null;
  }

  public boolean isSingleProgramStage() {
    return programStages != null && programStages.size() == 1;
  }

  // -------------------------------------------------------------------------
  // IdentifiableObject / NameableObject getters and setters
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
  @PropertyRange(min = 1, max = 230)
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
    return getTranslation("NAME", getName());
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @PropertyRange(min = 1, max = 50)
  public String getShortName() {
    return shortName;
  }

  public void setShortName(String shortName) {
    this.shortName = shortName;
  }

  @Override
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "shortName", key = "SHORT_NAME")
  public String getDisplayShortName() {
    return getTranslation("SHORT_NAME", getShortName());
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(min = 1)
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  @Sortable(value = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "description", key = "DESCRIPTION")
  public String getDisplayDescription() {
    return getTranslation("DESCRIPTION", getDescription());
  }

  @Override
  public String getDisplayProperty(DisplayProperty displayProperty) {
    if (DisplayProperty.SHORTNAME == displayProperty && getDisplayShortName() != null) {
      return getDisplayShortName();
    } else {
      return getDisplayName();
    }
  }

  @Override
  @Gist(included = Include.FALSE)
  @Sortable(value = false)
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "translations", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "translation", namespace = DxfNamespaces.DXF_2_0)
  public Set<Translation> getTranslations() {
    if (translations == null) {
      return new HashSet<>();
    }
    return translations.getTranslations();
  }

  @Override
  public void setTranslations(Set<Translation> translations) {
    this.translationCache.clear();
    if (this.translations == null) {
      this.translations = new TranslationProperty();
    }
    this.translations.setTranslations(translations);
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

  public String getPublicAccess() {
    return sharing != null ? sharing.getPublicAccess() : null;
  }

  public void setPublicAccess(String access) {
    if (sharing == null) {
      sharing = new Sharing();
    }
    sharing.setPublicAccess(access);
  }

  @Override
  @OpenApi.Property(BaseIdentifiableObject.AttributeValue[].class)
  @JsonProperty("attributeValues")
  @JsonDeserialize(using = AttributeValuesDeserializer.class)
  @JsonSerialize(using = AttributeValuesSerializer.class)
  public AttributeValues getAttributeValues() {
    return attributeValues;
  }

  @Override
  public void setAttributeValues(AttributeValues attributeValues) {
    this.attributeValues = attributeValues;
  }

  @Override
  public void addAttributeValue(String attributeId, String value) {
    this.attributeValues = attributeValues.added(attributeId, value);
  }

  @Override
  public void removeAttributeValue(String attributeId) {
    this.attributeValues = attributeValues.removed(attributeId);
  }

  @Override
  public void setUser(User user) {
    // TODO remove this after implementing functions for using Owner
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
    } else {
      return getPropertyValue(idScheme);
    }
  }

  // -------------------------------------------------------------------------
  // Translation helper
  // -------------------------------------------------------------------------

  protected String getTranslation(String translationKey, String defaultValue) {
    org.hisp.dhis.common.Locale locale = UserSettings.getCurrentSettings().getUserDbLocale();
    final String defaultTranslation = defaultValue != null ? defaultValue.trim() : null;
    if (locale == null || translationKey == null || CollectionUtils.isEmpty(getTranslations())) {
      return defaultValue;
    }
    return translationCache.computeIfAbsent(
        Translation.getCacheKey(locale.toString(), translationKey),
        key -> getTranslationValue(locale.toString(), translationKey, defaultTranslation));
  }

  private String getTranslationValue(String locale, String translationKey, String defaultValue) {
    for (Translation translation : getTranslations()) {
      if (locale.equals(translation.getLocale())
          && translationKey.equals(translation.getProperty())
          && !StringUtils.isEmpty(translation.getValue())) {
        return translation.getValue();
      }
    }
    return defaultValue;
  }

  // -------------------------------------------------------------------------
  // hashCode, equals
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    int result = getUid() != null ? getUid().hashCode() : 0;
    result = 31 * result + (getCode() != null ? getCode().hashCode() : 0);
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof Program other
            && getRealClass(this) == getRealClass(obj)
            && Objects.equals(getUid(), other.getUid())
            && Objects.equals(getCode(), other.getCode())
            && Objects.equals(getName(), other.getName());
  }

  // -------------------------------------------------------------------------
  // Logic methods
  // -------------------------------------------------------------------------

  @Override
  public int increaseVersion() {
    return ++version;
  }

  public boolean isOpen() {
    return this.accessLevel == AccessLevel.OPEN;
  }

  public boolean isAudited() {
    return this.accessLevel == AccessLevel.AUDITED;
  }

  public boolean isProtected() {
    return this.accessLevel == AccessLevel.PROTECTED;
  }

  public boolean isClosed() {
    return this.accessLevel == AccessLevel.CLOSED;
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public int getVersion() {
    return version;
  }

  @Override
  public void setVersion(int version) {
    this.version = version;
  }

  @JsonProperty("organisationUnits")
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "organisationUnits", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "organisationUnit", namespace = DxfNamespaces.DXF_2_0)
  public Set<OrganisationUnit> getOrganisationUnits() {
    return organisationUnits;
  }

  public void setOrganisationUnits(Set<OrganisationUnit> organisationUnits) {
    this.organisationUnits = organisationUnits;
  }

  @JsonProperty("programStages")
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "programStages", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "programStage", namespace = DxfNamespaces.DXF_2_0)
  public Set<ProgramStage> getProgramStages() {
    return programStages;
  }

  public void setProgramStages(Set<ProgramStage> programStages) {
    this.programStages = programStages;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(min = 2)
  public String getEnrollmentDateLabel() {
    return enrollmentDateLabel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "enrollmentDateLabel", key = "ENROLLMENT_DATE_LABEL")
  public String getDisplayEnrollmentDateLabel() {
    return getTranslation("ENROLLMENT_DATE_LABEL", getEnrollmentDateLabel());
  }

  public void setEnrollmentDateLabel(String enrollmentDateLabel) {
    this.enrollmentDateLabel = enrollmentDateLabel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(min = 2)
  public String getIncidentDateLabel() {
    return incidentDateLabel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "incidentDateLabel", key = "INCIDENT_DATE_LABEL")
  public String getDisplayIncidentDateLabel() {
    return getTranslation("INCIDENT_DATE_LABEL", getIncidentDateLabel());
  }

  public void setIncidentDateLabel(String incidentDateLabel) {
    this.incidentDateLabel = incidentDateLabel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(min = 2)
  public String getEnrollmentLabel() {
    return enrollmentLabel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "enrollmentLabel", key = "ENROLLMENT_LABEL")
  public String getDisplayEnrollmentLabel() {
    return getTranslation("ENROLLMENT_LABEL", getEnrollmentLabel());
  }

  public void setEnrollmentLabel(String enrollmentLabel) {
    this.enrollmentLabel = enrollmentLabel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(min = 2)
  public String getFollowUpLabel() {
    return followUpLabel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "followUpLabel", key = "FOLLOW_UP_LABEL")
  public String getDisplayFollowUpLabel() {
    return getTranslation("FOLLOW_UP_LABEL", getFollowUpLabel());
  }

  public void setFollowUpLabel(String followUpLabel) {
    this.followUpLabel = followUpLabel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(min = 2)
  public String getOrgUnitLabel() {
    return orgUnitLabel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "orgUnitLabel", key = "ORG_UNIT_LABEL")
  public String getDisplayOrgUnitLabel() {
    return getTranslation("ORG_UNIT_LABEL", getOrgUnitLabel());
  }

  public void setOrgUnitLabel(String orgUnitLabel) {
    this.orgUnitLabel = orgUnitLabel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(min = 2)
  public String getRelationshipLabel() {
    return relationshipLabel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "relationshipLabel", key = "RELATIONSHIP_LABEL")
  public String getDisplayRelationshipLabel() {
    return getTranslation("RELATIONSHIP_LABEL", getRelationshipLabel());
  }

  public void setRelationshipLabel(String relationshipLabel) {
    this.relationshipLabel = relationshipLabel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(min = 2)
  public String getNoteLabel() {
    return noteLabel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "noteLabel", key = "NOTE_LABEL")
  public String getDisplayNoteLabel() {
    return getTranslation("NOTE_LABEL", getNoteLabel());
  }

  public void setNoteLabel(String noteLabel) {
    this.noteLabel = noteLabel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(min = 2)
  public String getTrackedEntityAttributeLabel() {
    return trackedEntityAttributeLabel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(
      propertyName = "trackedEntityAttributeLabel",
      key = "TRACKED_ENTITY_ATTRIBUTE_LABEL")
  public String getDisplayTrackedEntityAttributeLabel() {
    return getTranslation("TRACKED_ENTITY_ATTRIBUTE_LABEL", getTrackedEntityAttributeLabel());
  }

  public void setTrackedEntityAttributeLabel(String trackedEntityAttributeLabel) {
    this.trackedEntityAttributeLabel = trackedEntityAttributeLabel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(min = 2)
  public String getProgramStageLabel() {
    return programStageLabel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "programStageLabel", key = "PROGRAM_STAGE_LABEL")
  public String getDisplayProgramStageLabel() {
    return getTranslation("PROGRAM_STAGE_LABEL", getProgramStageLabel());
  }

  public void setProgramStageLabel(String programStageLabel) {
    this.programStageLabel = programStageLabel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(min = 2)
  public String getEventLabel() {
    return eventLabel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "eventLabel", key = "EVENT_LABEL")
  public String getDisplayEventLabel() {
    return getTranslation("EVENT_LABEL", getEventLabel());
  }

  public void setEventLabel(String eventLabel) {
    this.eventLabel = eventLabel;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ProgramType getProgramType() {
    return programType;
  }

  public void setProgramType(ProgramType programType) {
    this.programType = programType;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getDisplayIncidentDate() {
    return displayIncidentDate;
  }

  public void setDisplayIncidentDate(Boolean displayIncidentDate) {
    this.displayIncidentDate = displayIncidentDate;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getIgnoreOverdueEvents() {
    return ignoreOverdueEvents;
  }

  public void setIgnoreOverdueEvents(Boolean ignoreOverdueEvents) {
    this.ignoreOverdueEvents = ignoreOverdueEvents;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isRegistration() {
    return programType == ProgramType.WITH_REGISTRATION;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isWithoutRegistration() {
    return programType == ProgramType.WITHOUT_REGISTRATION;
  }

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "userRoles", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "userRole", namespace = DxfNamespaces.DXF_2_0)
  public Set<UserRole> getUserRoles() {
    return userRoles;
  }

  public void setUserRoles(Set<UserRole> userRoles) {
    this.userRoles = userRoles;
  }

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "programIndicators", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "programIndicator", namespace = DxfNamespaces.DXF_2_0)
  public Set<ProgramIndicator> getProgramIndicators() {
    return programIndicators;
  }

  public void setProgramIndicators(Set<ProgramIndicator> programIndicators) {
    this.programIndicators = programIndicators;
  }

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "programRuleVariables", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "programRuleVariable", namespace = DxfNamespaces.DXF_2_0)
  public Set<ProgramRuleVariable> getProgramRuleVariables() {
    return programRuleVariables;
  }

  public void setProgramRuleVariables(Set<ProgramRuleVariable> programRuleVariables) {
    this.programRuleVariables = programRuleVariables;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getOnlyEnrollOnce() {
    return onlyEnrollOnce;
  }

  public void setOnlyEnrollOnce(Boolean onlyEnrollOnce) {
    this.onlyEnrollOnce = onlyEnrollOnce;
  }

  @JsonProperty
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlProperty(localName = "notificationTemplate", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlElementWrapper(localName = "notificationTemplates", namespace = DxfNamespaces.DXF_2_0)
  public Set<ProgramNotificationTemplate> getNotificationTemplates() {
    return notificationTemplates;
  }

  public void setNotificationTemplates(Set<ProgramNotificationTemplate> notificationTemplates) {
    this.notificationTemplates = notificationTemplates;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getSelectEnrollmentDatesInFuture() {
    return selectEnrollmentDatesInFuture;
  }

  public void setSelectEnrollmentDatesInFuture(Boolean selectEnrollmentDatesInFuture) {
    this.selectEnrollmentDatesInFuture = selectEnrollmentDatesInFuture;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getSelectIncidentDatesInFuture() {
    return selectIncidentDatesInFuture;
  }

  public void setSelectIncidentDatesInFuture(Boolean selectIncidentDatesInFuture) {
    this.selectIncidentDatesInFuture = selectIncidentDatesInFuture;
  }

  @JsonProperty
  @JsonSerialize(as = IdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Program getRelatedProgram() {
    return relatedProgram;
  }

  public void setRelatedProgram(Program relatedProgram) {
    this.relatedProgram = relatedProgram;
  }

  @JsonProperty("programTrackedEntityAttributes")
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(
      localName = "programTrackedEntityAttributes",
      namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(
      localName = "programTrackedEntityAttribute",
      namespace = DxfNamespaces.DXF_2_0)
  public List<ProgramTrackedEntityAttribute> getProgramAttributes() {
    return programAttributes;
  }

  public void setProgramAttributes(List<ProgramTrackedEntityAttribute> programAttributes) {
    this.programAttributes = programAttributes;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "trackedEntityType", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "trackedEntityType", namespace = DxfNamespaces.DXF_2_0)
  public TrackedEntityType getTrackedEntityType() {
    return trackedEntityType;
  }

  public void setTrackedEntityType(TrackedEntityType trackedEntityType) {
    this.trackedEntityType = trackedEntityType;
  }

  @JsonProperty
  @JacksonXmlProperty(localName = "dataEntryForm", namespace = DxfNamespaces.DXF_2_0)
  public DataEntryForm getDataEntryForm() {
    return dataEntryForm;
  }

  public void setDataEntryForm(DataEntryForm dataEntryForm) {
    this.dataEntryForm = dataEntryForm;
  }

  @JsonProperty
  @JsonSerialize(as = IdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public CategoryCombo getCategoryCombo() {
    return categoryCombo;
  }

  public void setCategoryCombo(CategoryCombo categoryCombo) {
    this.categoryCombo = categoryCombo;
  }

  /**
   * Indicates whether this program has a category combination which is different from the default
   * category combination.
   */
  public boolean hasNonDefaultCategoryCombo() {
    return categoryCombo != null
        && !CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME.equals(categoryCombo.getName());
  }

  @JsonProperty
  @JsonSerialize(as = IdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public CategoryCombo getEnrollmentCategoryCombo() {
    return enrollmentCategoryCombo;
  }

  public void setEnrollmentCategoryCombo(CategoryCombo enrollmentCategoryCombo) {
    this.enrollmentCategoryCombo = enrollmentCategoryCombo;
  }

  /**
   * Indicates whether this program has an enrollment category combination which is different from
   * the default category combination.
   */
  public boolean hasNonDefaultEnrollmentCategoryCombo() {
    return enrollmentCategoryCombo != null
        && !CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME.equals(enrollmentCategoryCombo.getName());
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isSkipOffline() {
    return skipOffline;
  }

  public void setSkipOffline(boolean skipOffline) {
    this.skipOffline = skipOffline;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getDisplayFrontPageList() {
    return displayFrontPageList;
  }

  public void setDisplayFrontPageList(Boolean displayFrontPageList) {
    this.displayFrontPageList = displayFrontPageList;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getUseFirstStageDuringRegistration() {
    return useFirstStageDuringRegistration;
  }

  public void setUseFirstStageDuringRegistration(Boolean useFirstStageDuringRegistration) {
    this.useFirstStageDuringRegistration = useFirstStageDuringRegistration;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public FeatureType getFeatureType() {
    return featureType;
  }

  public void setFeatureType(FeatureType featureType) {
    this.featureType = featureType;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public int getExpiryDays() {
    return expiryDays;
  }

  public void setExpiryDays(int expiryDays) {
    this.expiryDays = expiryDays;
  }

  @JsonProperty
  @JsonSerialize(using = JacksonPeriodTypeSerializer.class)
  @JsonDeserialize(using = JacksonPeriodTypeDeserializer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public PeriodType getExpiryPeriodType() {
    return expiryPeriodType;
  }

  public void setExpiryPeriodType(PeriodType expiryPeriodType) {
    this.expiryPeriodType = expiryPeriodType;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public int getCompleteEventsExpiryDays() {
    return completeEventsExpiryDays;
  }

  public void setCompleteEventsExpiryDays(int completeEventsExpiryDays) {
    this.completeEventsExpiryDays = completeEventsExpiryDays;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public int getOpenDaysAfterCoEndDate() {
    return openDaysAfterCoEndDate;
  }

  public void setOpenDaysAfterCoEndDate(int openDaysAfterCoEndDate) {
    this.openDaysAfterCoEndDate = openDaysAfterCoEndDate;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public int getMinAttributesRequiredToSearch() {
    return minAttributesRequiredToSearch;
  }

  public void setMinAttributesRequiredToSearch(int minAttributesRequiredToSearch) {
    this.minAttributesRequiredToSearch = minAttributesRequiredToSearch;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public int getMaxTeiCountToReturn() {
    return maxTeiCountToReturn;
  }

  public void setMaxTeiCountToReturn(int maxTeiCountToReturn) {
    this.maxTeiCountToReturn = maxTeiCountToReturn;
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
  public String getFormName() {
    return formName;
  }

  public void setFormName(String formName) {
    this.formName = formName;
  }

  /** Returns the form name, or the name if it does not exist. */
  public String getFormNameFallback() {
    return formName != null && !formName.isEmpty() ? getFormName() : getDisplayName();
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "formName", key = "FORM_NAME")
  public String getDisplayFormName() {
    return getTranslation("FORM_NAME", getFormNameFallback());
  }

  @JsonProperty("programSections")
  @JsonSerialize(contentAs = BaseIdentifiableObject.class)
  @JacksonXmlElementWrapper(localName = "programSections", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "programSection", namespace = DxfNamespaces.DXF_2_0)
  public Set<ProgramSection> getProgramSections() {
    return programSections;
  }

  public void setProgramSections(Set<ProgramSection> programSections) {
    this.programSections = programSections;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public AccessLevel getAccessLevel() {
    return accessLevel;
  }

  public void setAccessLevel(AccessLevel accessLevel) {
    this.accessLevel = accessLevel;
  }

  @JsonProperty("categoryMappings")
  @JacksonXmlElementWrapper(localName = "categoryMappings", namespace = DXF_2_0)
  @JacksonXmlProperty(localName = "categoryMappings", namespace = DXF_2_0)
  public Set<ProgramCategoryMapping> getCategoryMappings() {
    return categoryMappings;
  }

  public void setCategoryMappings(Set<ProgramCategoryMapping> categoryMappings) {
    this.categoryMappings = categoryMappings;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isEnableChangeLog() {
    return enableChangeLog;
  }

  public void setEnableChangeLog(boolean enableChangeLog) {
    this.enableChangeLog = enableChangeLog;
  }

  public static Program shallowCopy(Program original, Map<String, String> options) {
    Program copy = new Program();
    copy.setAutoFields();
    setShallowCopyValues(copy, original, options);
    return copy;
  }

  private static void setShallowCopyValues(
      Program copy, Program original, Map<String, String> options) {
    String prefix = options.getOrDefault(PREFIX_KEY, DEFAULT_PREFIX);
    copy.setAccessLevel(original.getAccessLevel());
    copy.setProgramAttributes(new ArrayList<>());
    copy.setCategoryCombo(original.getCategoryCombo());
    copy.setEnrollmentCategoryCombo(original.getEnrollmentCategoryCombo());
    copy.setCategoryMappings(copyOf(original.getCategoryMappings()));
    copy.setCompleteEventsExpiryDays(original.getCompleteEventsExpiryDays());
    copy.setDataEntryForm(original.getDataEntryForm());
    copy.setDescription(original.getDescription());
    copy.setDisplayIncidentDate(original.getDisplayIncidentDate());
    copy.setDisplayFrontPageList(original.getDisplayFrontPageList());
    copy.setEnrollmentDateLabel(original.getEnrollmentDateLabel());
    copy.setExpiryDays(original.getExpiryDays());
    copy.setExpiryPeriodType(original.getExpiryPeriodType());
    copy.setFeatureType(original.getFeatureType());
    copy.setFormName(original.getFormName());
    copy.setIgnoreOverdueEvents(original.getIgnoreOverdueEvents());
    copy.setIncidentDateLabel(original.getIncidentDateLabel());
    copy.setMaxTeiCountToReturn(original.getMaxTeiCountToReturn());
    copy.setMinAttributesRequiredToSearch(original.getMinAttributesRequiredToSearch());
    copy.setName(prefix + original.getName());
    copy.setNotificationTemplates(copyOf(original.getNotificationTemplates()));
    copy.setOnlyEnrollOnce(original.getOnlyEnrollOnce());
    copy.setOpenDaysAfterCoEndDate(original.getOpenDaysAfterCoEndDate());
    copy.setOrganisationUnits(copyOf(original.getOrganisationUnits()));
    copy.setProgramType(original.getProgramType());
    copy.setPublicAccess(original.getPublicAccess());
    copy.setRelatedProgram(original.getRelatedProgram());
    copy.setSharing(original.getSharing());
    copy.setShortName(original.getShortName());
    copy.setSelectEnrollmentDatesInFuture(original.getSelectEnrollmentDatesInFuture());
    copy.setSelectIncidentDatesInFuture(original.getSelectIncidentDatesInFuture());
    copy.setSkipOffline(original.isSkipOffline());
    copy.setStyle(original.getStyle());
    copy.setTrackedEntityType(original.getTrackedEntityType());
    copy.setUseFirstStageDuringRegistration(original.getUseFirstStageDuringRegistration());
    copy.setUserRoles(copyOf(original.getUserRoles()));
    copy.setEnrollmentLabel(original.getEnrollmentLabel());
    copy.setNoteLabel(original.getNoteLabel());
    copy.setFollowUpLabel(original.getFollowUpLabel());
    copy.setOrgUnitLabel(original.getOrgUnitLabel());
    copy.setTrackedEntityAttributeLabel(original.getTrackedEntityAttributeLabel());
    copy.setProgramStageLabel(original.getProgramStageLabel());
    copy.setEventLabel(original.getEventLabel());
    copy.setRelationshipLabel(original.getRelationshipLabel());
    copy.setEnableChangeLog(original.isEnableChangeLog());
  }

  public record ProgramStageTuple(ProgramStage original, ProgramStage copy) {}
}
