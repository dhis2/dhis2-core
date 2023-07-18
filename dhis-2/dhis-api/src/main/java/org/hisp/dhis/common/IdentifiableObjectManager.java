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
package org.hisp.dhis.common;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;

/**
 * @author Lars Helge Overland
 */
public interface IdentifiableObjectManager {
  String ID = IdentifiableObjectManager.class.getName();

  void save(@Nonnull IdentifiableObject object);

  void save(@Nonnull IdentifiableObject object, boolean clearSharing);

  void save(@Nonnull List<IdentifiableObject> objects);

  void update(@Nonnull IdentifiableObject object);

  void update(@Nonnull IdentifiableObject object, @CheckForNull User user);

  void update(@Nonnull List<IdentifiableObject> objects);

  void update(@Nonnull List<IdentifiableObject> objects, @CheckForNull User user);

  void delete(@Nonnull IdentifiableObject object);

  void delete(@Nonnull IdentifiableObject object, @CheckForNull User user);

  /**
   * Lookup objects of unknown type.
   *
   * <p>If the type is known at compile time this method should not be used. Instead, use {@link
   * org.hisp.dhis.common.IdentifiableObjectManager#get(Class, String)}.
   *
   * @param uid a UID of an object of unknown type
   * @return The {@link IdentifiableObject} with the given UID
   */
  @Nonnull
  Optional<? extends IdentifiableObject> find(@Nonnull String uid);

  /**
   * Look up objects which have property createdBy or lastUpdatedBy linked to given {@link User}
   *
   * @param type the object class type.
   * @param user the User which is linked to createdBy or lastUpdatedBy property.
   * @return The list of {@link IdentifiableObject} found
   */
  <T extends IdentifiableObject> List<T> findByUser(Class<T> type, @Nonnull User user);

  /**
   * Lookup objects of a specific type by database ID.
   *
   * @param type the object class type.
   * @param id object's database ID
   * @return the found object
   */
  @CheckForNull
  <T extends IdentifiableObject> T get(@Nonnull Class<T> type, long id);

  /**
   * Retrieves the object of the given type and UID, or null if no object exists.
   *
   * @param type the object class type.
   * @param uid the UID.
   * @return the object with the given UID.
   */
  @CheckForNull
  <T extends IdentifiableObject> T get(@Nonnull Class<T> type, @Nonnull String uid);

  /**
   * Retrieves the object of the given type and UID, throws exception if no object exists.
   *
   * @param type the object class type.
   * @param uid the UID.
   * @return the object with the given UID.
   * @throws IllegalQueryException if no object exists.
   */
  @Nonnull
  <T extends IdentifiableObject> T load(@Nonnull Class<T> type, @Nonnull String uid)
      throws IllegalQueryException;

  /**
   * Retrieves the object of the given type and UID, throws exception using the given error code if
   * no object exists.
   *
   * @param type the object class type.
   * @param errorCode the {@link ErrorCode} to use for the exception.
   * @param uid the UID.
   * @return the object with the given UID.
   * @throws IllegalQueryException if no object exists.
   */
  @Nonnull
  <T extends IdentifiableObject> T load(
      @Nonnull Class<T> type, @Nonnull ErrorCode errorCode, @Nonnull String uid)
      throws IllegalQueryException;

  <T extends IdentifiableObject> boolean exists(@Nonnull Class<T> type, @Nonnull String uid);

  @CheckForNull
  <T extends IdentifiableObject> T get(
      @Nonnull Collection<Class<? extends T>> types, @Nonnull String uid);

  @CheckForNull
  <T extends IdentifiableObject> T get(
      @Nonnull Collection<Class<? extends T>> types,
      @Nonnull IdScheme idScheme,
      @Nonnull String value);

  /**
   * Retrieves the object of the given type and code, or null if no object exists.
   *
   * @param type the object class type.
   * @param code the code.
   * @return the object with the given code.
   */
  @CheckForNull
  <T extends IdentifiableObject> T getByCode(@Nonnull Class<T> type, @Nonnull String code);

  /**
   * Retrieves the object of the given type and code, throws exception if no object exists.
   *
   * @param type the object class type.
   * @param code the code.
   * @return the object with the given code.
   * @throws IllegalQueryException if no object exists.
   */
  @Nonnull
  <T extends IdentifiableObject> T loadByCode(@Nonnull Class<T> type, @Nonnull String code)
      throws IllegalQueryException;

  @Nonnull
  <T extends IdentifiableObject> List<T> getByCode(
      @Nonnull Class<T> type, @Nonnull Collection<String> codes);

  @CheckForNull
  <T extends IdentifiableObject> T getByName(@Nonnull Class<T> type, @Nonnull String name);

  @CheckForNull
  <T extends IdentifiableObject> T getByUniqueAttributeValue(
      @Nonnull Class<T> type, @Nonnull Attribute attribute, @Nonnull String value);

  @CheckForNull
  <T extends IdentifiableObject> T getByUniqueAttributeValue(
      @Nonnull Class<T> type,
      @Nonnull Attribute attribute,
      @Nonnull String value,
      @CheckForNull User userInfo);

  @CheckForNull
  <T extends IdentifiableObject> T search(@Nonnull Class<T> type, @Nonnull String query);

  @Nonnull
  <T extends IdentifiableObject> List<T> filter(@Nonnull Class<T> type, @Nonnull String query);

  @Nonnull
  <T extends IdentifiableObject> List<T> getAll(@Nonnull Class<T> type);

  @Nonnull
  <T extends IdentifiableObject> List<T> getDataWriteAll(@Nonnull Class<T> type);

  @Nonnull
  <T extends IdentifiableObject> List<T> getDataReadAll(@Nonnull Class<T> type);

  @Nonnull
  <T extends IdentifiableObject> List<T> getAllSorted(@Nonnull Class<T> type);

  @Nonnull
  <T extends IdentifiableObject> List<T> getAllByAttributes(
      @Nonnull Class<T> type, @Nonnull List<Attribute> attributes);

  @Nonnull
  <T extends IdentifiableObject> List<AttributeValue> getAllValuesByAttributes(
      @Nonnull Class<T> type, @Nonnull List<Attribute> attributes);

  <T extends IdentifiableObject> long countAllValuesByAttributes(
      @Nonnull Class<T> type, @Nonnull List<Attribute> attributes);

  @Nonnull
  <T extends IdentifiableObject> List<T> getByUid(
      @Nonnull Class<T> type, @Nonnull Collection<String> uids);

  /**
   * Retrieves the objects of the given type and collection of UIDs, throws exception is any object
   * does not exist.
   *
   * @param type the object class type.
   * @param uids the collection of UIDs.
   * @return a list of objects.
   * @throws IllegalQueryException if any object does not exist.
   */
  @Nonnull
  <T extends IdentifiableObject> List<T> loadByUid(
      @Nonnull Class<T> type, @CheckForNull Collection<String> uids) throws IllegalQueryException;

  @Nonnull
  <T extends IdentifiableObject> List<T> getByUid(
      @Nonnull Collection<Class<? extends T>> types, @Nonnull Collection<String> uids);

  @Nonnull
  <T extends IdentifiableObject> List<T> getById(
      @Nonnull Class<T> type, @Nonnull Collection<Long> ids);

  @Nonnull
  <T extends IdentifiableObject> List<T> getOrdered(
      @Nonnull Class<T> type, @Nonnull IdScheme idScheme, @Nonnull Collection<String> values);

  @Nonnull
  <T extends IdentifiableObject> List<T> getByUidOrdered(
      @Nonnull Class<T> type, @Nonnull List<String> uids);

  @Nonnull
  <T extends IdentifiableObject> List<T> getLikeName(@Nonnull Class<T> type, @Nonnull String name);

  @Nonnull
  <T extends IdentifiableObject> List<T> getLikeName(
      @Nonnull Class<T> type, @Nonnull String name, boolean caseSensitive);

  @Nonnull
  <T extends IdentifiableObject> List<T> getBetweenSorted(
      @Nonnull Class<T> type, int first, int max);

  @Nonnull
  <T extends IdentifiableObject> List<T> getBetweenLikeName(
      @Nonnull Class<T> type, @Nonnull Set<String> words, int first, int max);

  <T extends IdentifiableObject> Date getLastUpdated(@Nonnull Class<T> type);

  @Nonnull
  <T extends IdentifiableObject> Map<String, T> getIdMap(
      @Nonnull Class<T> type, @Nonnull IdentifiableProperty property);

  @Nonnull
  <T extends IdentifiableObject> Map<String, T> getIdMap(
      @Nonnull Class<T> type, @Nonnull IdScheme idScheme);

  @Nonnull
  <T extends IdentifiableObject> Map<String, T> getIdMapNoAcl(
      @Nonnull Class<T> type, @Nonnull IdentifiableProperty property);

  @Nonnull
  <T extends IdentifiableObject> Map<String, T> getIdMapNoAcl(
      @Nonnull Class<T> type, @Nonnull IdScheme idScheme);

  @Nonnull
  <T extends IdentifiableObject> List<T> getObjects(
      @Nonnull Class<T> type,
      @Nonnull IdentifiableProperty property,
      @Nonnull Collection<String> identifiers);

  @Nonnull
  <T extends IdentifiableObject> List<T> getObjects(
      @Nonnull Class<T> type, @Nonnull Collection<Long> identifiers);

  @CheckForNull
  <T extends IdentifiableObject> T getObject(
      @Nonnull Class<T> type, @Nonnull IdentifiableProperty property, @Nonnull String value);

  @CheckForNull
  <T extends IdentifiableObject> T getObject(
      @Nonnull Class<T> type, @Nonnull IdScheme idScheme, @Nonnull String value);

  @CheckForNull
  IdentifiableObject getObject(@Nonnull String uid, @Nonnull String simpleClassName);

  @CheckForNull
  IdentifiableObject getObject(long id, @Nonnull String simpleClassName);

  <T extends IdentifiableObject> int getCount(@Nonnull Class<T> type);

  <T extends IdentifiableObject> int getCountByCreated(
      @Nonnull Class<T> type, @Nonnull Date created);

  <T extends IdentifiableObject> int getCountByLastUpdated(
      @Nonnull Class<T> type, @Nonnull Date lastUpdated);

  @Nonnull
  <T extends DimensionalObject> List<T> getDataDimensions(@Nonnull Class<T> type);

  @Nonnull
  <T extends DimensionalObject> List<T> getDataDimensionsNoAcl(@Nonnull Class<T> type);

  void refresh(@Nonnull Object object);

  /**
   * Resets all properties that are not owned by the object type.
   *
   * @param object object to reset
   */
  void resetNonOwnerProperties(@Nonnull Object object);

  void flush();

  void clear();

  void evict(@Nonnull Object object);

  @Nonnull
  <T extends IdentifiableObject> List<T> getByAttributeAndValue(
      @Nonnull Class<T> type, @Nonnull Attribute attribute, @Nonnull String value);

  <T extends IdentifiableObject> boolean isAttributeValueUnique(
      @Nonnull Class<T> type, @Nonnull T object, @Nonnull AttributeValue attributeValue);

  <T extends IdentifiableObject> boolean isAttributeValueUnique(
      @Nonnull Class<T> type,
      @Nonnull T object,
      @Nonnull Attribute attribute,
      @Nonnull String value);

  @Nonnull
  <T extends IdentifiableObject> List<T> getAllByAttributeAndValues(
      @Nonnull Class<T> type, @Nonnull Attribute attribute, @Nonnull List<String> values);

  @Nonnull
  Map<Class<? extends IdentifiableObject>, IdentifiableObject> getDefaults();

  void updateTranslations(
      @Nonnull IdentifiableObject persistedObject, @Nonnull Set<Translation> translations);

  @Nonnull
  <T extends IdentifiableObject> List<T> getNoAcl(
      @Nonnull Class<T> type, @Nonnull Collection<String> uids);

  boolean isDefault(@Nonnull IdentifiableObject object);

  @Nonnull
  List<String> getUidsCreatedBefore(
      @Nonnull Class<? extends IdentifiableObject> type, @Nonnull Date date);

  /** Remove given UserGroup UID from all sharing records in database */
  void removeUserGroupFromSharing(@Nonnull String userGroupUid);

  // -------------------------------------------------------------------------
  // NO ACL
  // -------------------------------------------------------------------------

  @CheckForNull
  <T extends IdentifiableObject> T getNoAcl(@Nonnull Class<T> type, @Nonnull String uid);

  <T extends IdentifiableObject> void updateNoAcl(@Nonnull T object);

  @Nonnull
  <T extends IdentifiableObject> List<T> getAllNoAcl(@Nonnull Class<T> type);
}
