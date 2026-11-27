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
package org.hisp.dhis.sqlview;

import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.Setter;
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
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Sortable;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.common.cache.Cacheable;
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
 * @author Dang Duy Hieu
 */
@Entity
@Table(name = "sqlview")
@JacksonXmlRootElement(localName = "sqlView", namespace = DxfNamespaces.DXF_2_0)
public class SqlView extends BaseMetadataObject implements IdentifiableObject, Cacheable {
  public static final String PREFIX_VIEWNAME = "_view";

  public static final Set<String> PROTECTED_TABLES =
      Set.of(
          "users", "userinfo", "trackedentityattributevalue", "oauth_access_token", "oauth2client");

  public static final Set<String> ILLEGAL_KEYWORDS =
      Set.of(
          "alter",
          "commit",
          "create",
          "createdb",
          "createuser",
          "delete",
          "drop",
          "grant",
          "insert",
          "rename",
          "restore",
          "revoke",
          "truncate",
          "update",
          "write");

  public static final String CURRENT_USER_ID_VARIABLE = "_current_user_id";

  public static final String CURRENT_USERNAME_VARIABLE = "_current_username";

  public static final Set<String> STANDARD_VARIABLES =
      Set.of(CURRENT_USER_ID_VARIABLE, CURRENT_USERNAME_VARIABLE);

  private static final String CRITERIA_SEP = ":";

  private static final String REGEX_SEP = "|";

  private static final String QUERY_VALUE_REGEX = "^[\\p{L}\\w\\s\\-]*$";

  private static final String QUERY_NAME_REGEX = "^[-a-zA-Z0-9_]+$";

  // -------------------------------------------------------------------------
  // Properties
  // -------------------------------------------------------------------------

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "sqlviewid")
  @Setter
  private long id;

  @Column(name = "code", unique = true, length = 50)
  @Setter
  @AuditAttribute
  private String code;

  @Column(name = "name", nullable = false, unique = true, length = 230)
  @Setter
  private String name;

  @Type(type = "text")
  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Type(type = "text")
  @Column(name = "sqlquery", nullable = false, columnDefinition = "text")
  private String sqlQuery;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 40)
  private SqlViewType type = SqlViewType.VIEW;

  @Enumerated(EnumType.STRING)
  @Column(name = "cachestrategy", nullable = false, length = 40)
  private CacheStrategy cacheStrategy = CacheStrategy.RESPECT_SYSTEM_SETTING;

  @Type(type = "jsbObjectSharing")
  @Column(name = "sharing")
  @Setter
  private Sharing sharing = new Sharing();

  @Type(type = "jsbAttributeValues")
  @Column(name = "attributevalues")
  @AuditAttribute
  private AttributeValues attributeValues = AttributeValues.empty();

  /**
   * Optional UID of the {@link org.hisp.dhis.scheduling.Job} that is responsible for updating a
   * {@link SqlViewType#MATERIALIZED_VIEW}.
   *
   * <p>This field is only used in the API.
   */
  @Transient private transient String updateJobId;

  /**
   * Not persisted, there is no translations column. Kept in memory so that the translations check
   * still rejects translations on this non-translatable type (E1107) instead of dropping them.
   */
  @Transient private transient Set<Translation> translations = new HashSet<>();

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public SqlView() {}

  public SqlView(String name, String sqlQuery, SqlViewType type) {
    this.name = name;
    this.sqlQuery = sqlQuery;
    this.type = type;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public String getViewName() {
    final Pattern p = Pattern.compile("\\W");

    String[] items = p.split(name.trim().replace("_", ""));

    StringBuilder input = new StringBuilder();

    for (String s : items) {
      input.append(s.isEmpty() ? "" : ("_" + s));
    }

    return PREFIX_VIEWNAME + input.toString().toLowerCase();
  }

  public static Map<String, String> getCriteria(Set<String> params) {
    Map<String, String> map = new HashMap<>();

    if (params != null) {
      for (String param : params) {
        if (param != null && param.split(CRITERIA_SEP).length == 2) {
          String[] criteria = param.split(CRITERIA_SEP);
          String filter = criteria[0];
          String value = criteria[1];

          map.put(filter, value);
        }
      }
    }

    return map;
  }

  public static Set<String> getInvalidQueryParams(Set<String> params) {
    Set<String> invalid = new HashSet<>();

    for (String param : params) {
      if (!isValidQueryParam(param)) {
        invalid.add(param);
      }
    }

    return invalid;
  }

  /** Indicates whether the given query parameter is valid. */
  public static boolean isValidQueryParam(String param) {
    return param.matches(QUERY_NAME_REGEX);
  }

  public static Set<String> getInvalidQueryValues(Collection<String> values) {
    Set<String> invalid = new HashSet<>();

    for (String value : values) {
      if (!isValidQueryValue(value)) {
        invalid.add(value);
      }
    }

    return invalid;
  }

  /** Indicates whether the given query value is valid. */
  public static boolean isValidQueryValue(String value) {
    return value != null && value.matches(QUERY_VALUE_REGEX);
  }

  public static String getProtectedTablesRegex() {
    StringBuilder regex = new StringBuilder("^(.*\\W)?(");

    for (String table : PROTECTED_TABLES) {
      regex.append(table).append(REGEX_SEP);
    }

    regex.delete(regex.length() - 1, regex.length());

    return regex.append(")(\\W.*)?$").toString();
  }

  public static String getIllegalKeywordsRegex() {
    StringBuilder regex = new StringBuilder("^(.*\\W)?(");

    for (String word : ILLEGAL_KEYWORDS) {
      regex.append(word).append(REGEX_SEP);
    }

    regex.delete(regex.length() - 1, regex.length());

    return regex.append(")(\\W.*)?$").toString();
  }

  /** Indicates whether this SQL view is a query. */
  public boolean isQuery() {
    return SqlViewType.QUERY.equals(type);
  }

  /** Indicates whether this SQl view is a view / materialized view. */
  public boolean isView() {
    return SqlViewType.QUERY.equals(type) || isMaterializedView();
  }

  /** Indicates whether this SQL view is a materalized view. */
  public boolean isMaterializedView() {
    return SqlViewType.MATERIALIZED_VIEW.equals(type);
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(min = 2)
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getSqlQuery() {
    return sqlQuery;
  }

  public void setSqlQuery(String sqlQuery) {
    this.sqlQuery = sqlQuery;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public SqlViewType getType() {
    return type;
  }

  public void setType(SqlViewType type) {
    this.type = type;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Override
  public CacheStrategy getCacheStrategy() {
    return cacheStrategy;
  }

  public void setCacheStrategy(CacheStrategy cacheStrategy) {
    this.cacheStrategy = cacheStrategy;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getUpdateJobId() {
    return updateJobId;
  }

  public void setUpdateJobId(String updateJobId) {
    this.updateJobId = updateJobId;
  }

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
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The name of this Object. Required and unique.")
  @PropertyRange(min = 1)
  public String getName() {
    return name;
  }

  /** The schema is not translatable, so the display name is always the name. */
  @Override
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "name", key = "NAME")
  public String getDisplayName() {
    return name;
  }

  @Override
  @OpenApi.Property(BaseIdentifiableObject.AttributeValue[].class)
  @JsonProperty("attributeValues")
  @JsonDeserialize(using = AttributeValuesDeserializer.class)
  @JsonSerialize(using = AttributeValuesSerializer.class)
  public AttributeValues getAttributeValues() {
    if (attributeValues == null) {
      attributeValues = AttributeValues.empty();
    }

    return attributeValues;
  }

  @Override
  public void setAttributeValues(AttributeValues attributeValues) {
    this.attributeValues = attributeValues == null ? AttributeValues.empty() : attributeValues;
  }

  @Override
  public void addAttributeValue(String attributeId, String value) {
    this.attributeValues = getAttributeValues().added(attributeId, value);
  }

  @Override
  public void removeAttributeValue(String attributeId) {
    this.attributeValues = getAttributeValues().removed(attributeId);
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
    } else if (idScheme.is(IdentifiableProperty.ATTRIBUTE)) {
      return getAttributeValues().get(idScheme.getAttribute());
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
  // hashCode, equals and toString
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    int result = getUid() != null ? getUid().hashCode() : 0;
    result = 31 * result + (getCode() != null ? getCode().hashCode() : 0);
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);
    return result;
  }

  /** Class check uses get-methods and the real class to handle proxied objects. */
  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof SqlView other
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
}
