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
package org.hisp.dhis.common;

import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Immutable;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.attribute.AttributeValuesDeserializer;
import org.hisp.dhis.attribute.AttributeValuesSerializer;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Gist;
import org.hisp.dhis.schema.annotation.Gist.Include;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.Property.Value;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.schema.annotation.PropertyTransformer;
import org.hisp.dhis.schema.transformer.UserPropertyTransformer;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.setting.UserSettings;
import org.hisp.dhis.translation.Translatable;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;

/**
 * @author Bob Jolliffe
 */
@JacksonXmlRootElement(localName = "identifiableObject", namespace = DxfNamespaces.DXF_2_0)
public class BaseIdentifiableObject extends BaseLinkableObject
    implements IdentifiableObject, FavoritableObject, AttributeObject {
  /** The database internal identifier for this Object. */
  @Setter protected long id;

  /** The unique identifier for this object. */
  @Setter @AuditAttribute protected String uid;

  /** The unique code for this object. */
  @Setter @AuditAttribute protected String code;

  /** The name of this object. Required and unique. */
  @Setter protected String name;

  /** The date this object was created. */
  @Setter protected Date created;

  /** The date this object was last updated. */
  @Setter protected Date lastUpdated;

  /** Set of the dynamic attributes values that belong to this data element. */
  @AuditAttribute private AttributeValues attributeValues = AttributeValues.empty();

  /** Set of available object translation, normally filtered by locale. */
  protected Set<Translation> translations = new HashSet<>();

  /**
   * Cache for object translations, where the cache key is a combination of locale and translation
   * property, and value is the translated value.
   */
  private final Map<String, String> translationCache = new ConcurrentHashMap<>();

  /** User who created this object. This field is immutable and must not be updated. */
  @Immutable protected User createdBy;

  /** Access information for this object. Applies to current user. */
  protected transient Access access;

  /** Users who have marked this object as a favorite. */
  @Setter protected Set<String> favorites = new HashSet<>();

  /** Last user updated this object. */
  @Setter protected User lastUpdatedBy;

  /** Object sharing (JSONB). */
  @Setter protected Sharing sharing = new Sharing();

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public BaseIdentifiableObject() {}

  public BaseIdentifiableObject(long id, String uid, String name) {
    this.id = id;
    this.uid = uid;
    this.name = name;
  }

  public BaseIdentifiableObject(String uid, String code, String name) {
    this.uid = uid;
    this.code = code;
    this.name = name;
  }

  public BaseIdentifiableObject(IdentifiableObject identifiableObject) {
    this.id = identifiableObject.getId();
    this.uid = identifiableObject.getUid();
    this.name = identifiableObject.getName();
    this.created = identifiableObject.getCreated();
    this.lastUpdated = identifiableObject.getLastUpdated();
  }

  // -------------------------------------------------------------------------
  // Comparable implementation
  // -------------------------------------------------------------------------

  /**
   * Compares objects based on display name. A null display name is ordered after a non-null display
   * name.
   */
  @Override
  public int compareTo(@Nonnull IdentifiableObject object) {
    if (this.getDisplayName() == null) {
      return object.getDisplayName() == null ? 0 : 1;
    }

    return object.getDisplayName() == null
        ? -1
        : this.getDisplayName().compareToIgnoreCase(object.getDisplayName());
  }

  // -------------------------------------------------------------------------
  // Setters and getters
  // -------------------------------------------------------------------------

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
  @JsonIgnore
  public long getId() {
    return id;
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
  @Description("The date this object was created.")
  @Property(value = PropertyType.DATE, required = Value.FALSE)
  public Date getCreated() {
    return created;
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
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The date this object was last updated.")
  @Property(value = PropertyType.DATE, required = Value.FALSE)
  public Date getLastUpdated() {
    return lastUpdated;
  }

  public record AttributeValue(@JsonProperty Attribute attribute, @JsonProperty String value) {}

  @Override
  @OpenApi.Property(AttributeValue[].class)
  @JsonProperty("attributeValues")
  @JsonDeserialize(using = AttributeValuesDeserializer.class)
  @JsonSerialize(using = AttributeValuesSerializer.class)
  public AttributeValues getAttributeValues() {
    return attributeValues;
  }

  @Override
  public void setAttributeValues(AttributeValues attributeValues) {
    this.attributeValues = attributeValues == null ? AttributeValues.empty() : attributeValues;
  }

  @Override
  public void addAttributeValue(String attributeId, String value) {
    this.attributeValues = attributeValues.added(attributeId, value);
  }

  @Override
  public void removeAttributeValue(String attributeId) {
    this.attributeValues = attributeValues.removed(attributeId);
  }

  @JsonIgnore
  public String getAttributeValue(String attributeUid) {
    return attributeValues.get(attributeUid);
  }

  @Gist(included = Include.FALSE)
  @Override
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

  /** Clears out cache when setting translations. */
  @Override
  public void setTranslations(Set<Translation> translations) {
    this.translationCache.clear();
    this.translations = translations;
  }

  /**
   * Returns a translated value for this object for the given property. The current locale is read
   * from the user context.
   *
   * @param translationKey the translation key.
   * @param defaultValue the value to use if there are no translations.
   * @return a translated value.
   */
  protected String getTranslation(String translationKey, String defaultValue) {
    Locale locale = UserSettings.getCurrentSettings().getUserDbLocale();

    final String defaultTranslation = defaultValue != null ? defaultValue.trim() : null;

    if (locale == null || translationKey == null || CollectionUtils.isEmpty(translations)) {
      return defaultValue;
    }

    return translationCache.computeIfAbsent(
        Translation.getCacheKey(locale.toString(), translationKey),
        key -> getTranslationValue(locale.toString(), translationKey, defaultTranslation));
  }

  @Override
  @Gist(included = Include.FALSE)
  @OpenApi.Property(UserPropertyTransformer.UserDto.class)
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getCreatedBy() {
    return createdBy;
  }

  @Override
  @OpenApi.Ignore
  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getUser() {
    return createdBy;
  }

  @Override
  public void setCreatedBy(User createdBy) {
    this.createdBy = createdBy;
  }

  @Override
  public void setUser(User user) {
    // TODO remove this after implementing functions for using Owner
    setCreatedBy(createdBy == null ? user : createdBy);
    setOwner(user != null ? user.getUid() : null);
  }

  public void setOwner(String userId) {
    getSharing().setOwner(userId);
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
  @JsonProperty
  @JacksonXmlElementWrapper(localName = "favorites", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "favorite", namespace = DxfNamespaces.DXF_2_0)
  public Set<String> getFavorites() {
    return favorites;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isFavorite() {
    if (favorites == null || !CurrentUserUtil.hasCurrentUser()) {
      return false;
    }
    return favorites.contains(CurrentUserUtil.getCurrentUserDetails().getUid());
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
  public boolean setAsFavorite(UserDetails user) {
    if (this.favorites == null) {
      this.favorites = new HashSet<>();
    }

    return this.favorites.add(user.getUid());
  }

  @Override
  public boolean removeAsFavorite(UserDetails user) {
    if (this.favorites == null) {
      this.favorites = new HashSet<>();
    }

    return this.favorites.remove(user.getUid());
  }

  // -------------------------------------------------------------------------
  // hashCode and equals
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    int result = getUid() != null ? getUid().hashCode() : 0;
    result = 31 * result + (getCode() != null ? getCode().hashCode() : 0);
    result = 31 * result + (getName() != null ? getName().hashCode() : 0);

    return result;
  }

  /** Class check uses isAssignableFrom and get-methods to handle proxied objects. */
  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof BaseIdentifiableObject other
            && getRealClass(this) == getRealClass(obj)
            && typedEquals(other);
  }

  /**
   * Equality check against typed identifiable object. This method is not vulnerable to proxy
   * issues, where an uninitialized object class type fails comparison to a real class.
   *
   * @param other the identifiable object to compare this object against.
   * @return true if equal.
   */
  public final boolean typedEquals(IdentifiableObject other) {
    if (other == null) {
      return false;
    }
    return Objects.equals(getUid(), other.getUid())
        && Objects.equals(getCode(), other.getCode())
        && Objects.equals(getName(), other.getName());
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  /** Set auto-generated fields on save or update */
  @Override
  public void setAutoFields() {
    if (uid == null || uid.isEmpty()) {
      setUid(CodeGenerator.generateUid());
    }

    Date date = new Date();

    if (created == null) {
      created = date;
    }

    setLastUpdated(date);
  }

  /**
   * Returns the value of the property referred to by the given {@link IdScheme}.
   *
   * @param idScheme the {@link IdScheme}.
   * @return the value of the property referred to by the {@link IdScheme}.
   */
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
      return attributeValues.get(idScheme.getAttribute());
    }
    return null;
  }

  /**
   * Returns the value of the property referred to by the given {@link IdScheme}. If this happens to
   * refer to NAME, it returns the translatable/display version.
   *
   * @param idScheme the {@link IdScheme}.
   * @return the value of the property referred to by the {@link IdScheme}.
   */
  @Override
  public String getDisplayPropertyValue(IdScheme idScheme) {
    if (idScheme.is(IdentifiableProperty.NAME)) {
      return getDisplayName();
    } else {
      return getPropertyValue(idScheme);
    }
  }

  // -------------------------------------------------------------------------
  // Sharing helpers
  // -------------------------------------------------------------------------

  public void setExternalAccess(boolean externalAccess) {
    if (sharing == null) {
      sharing = new Sharing();
    }

    sharing.setExternal(externalAccess);
  }

  public void setPublicAccess(String access) {
    if (sharing == null) {
      sharing = new Sharing();
    }

    sharing.setPublicAccess(access);
  }

  public String getPublicAccess() {
    if (sharing != null) {
      return sharing.getPublicAccess();
    }

    return null;
  }

  public Collection<UserAccess> getUserAccesses() {
    if (sharing == null || getSharing().getUsers() == null) {
      return Collections.emptyList();
    }

    return getSharing().getUsers().values();
  }

  public Collection<UserGroupAccess> getUserGroupAccesses() {
    if (sharing == null || getSharing().getUserGroups() == null) {
      return Collections.emptyList();
    }

    return getSharing().getUserGroups().values();
  }

  @Override
  public String toString() {
    return "{"
        + "\"class\":\""
        + getClass()
        + "\", "
        + "\"id\":\""
        + getId()
        + "\", "
        + "\"uid\":\""
        + getUid()
        + "\", "
        + "\"code\":\""
        + getCode()
        + "\", "
        + "\"name\":\""
        + getName()
        + "\", "
        + "\"created\":\""
        + getCreated()
        + "\", "
        + "\"lastUpdated\":\""
        + getLastUpdated()
        + "\" "
        + "}";
  }

  /**
   * Get Translation value from {@code Set<Translation>} by given locale and translationKey
   *
   * @return Translation value if exists, otherwise return default value.
   */
  private String getTranslationValue(String locale, String translationKey, String defaultValue) {
    for (Translation translation : translations) {
      if (locale.equals(translation.getLocale())
          && translationKey.equals(translation.getProperty())
          && !StringUtils.isEmpty(translation.getValue())) {
        return translation.getValue();
      }
    }

    return defaultValue;
  }

  /**
   * Method that allows copying of a Collection which requires a parent object of each element to be
   * used in the copying logic.
   *
   * @param parent Object to be used as part of the copying logic
   * @param original Collection to be copied
   * @param copy BiFunction which applies the copying logic
   * @return Copied Set
   * @param <T> parent
   * @param <E> element
   */
  public static <T, E> Set<E> copySet(T parent, Collection<E> original, BiFunction<E, T, E> copy) {
    return original == null
        ? Stream.<E>empty().collect(toSet())
        : original.stream()
            .filter(Objects::nonNull)
            .map(e -> copy.apply(e, parent))
            .collect(toSet());
  }

  /**
   * Method that allows copying of a Collection which requires a parent object of each element to be
   * used in the copying logic.
   *
   * @param parent Object to be used as part of the copying logic
   * @param original Collection to be copied
   * @param copy BiFunction which applies the copying logic
   * @return Copied List
   * @param <T> parent
   * @param <E> element
   */
  public static <T, E> List<E> copyList(
      T parent, Collection<E> original, BiFunction<E, T, E> copy) {
    return original == null
        ? Stream.<E>empty().toList()
        : original.stream().filter(Objects::nonNull).map(e -> copy.apply(e, parent)).toList();
  }
}
