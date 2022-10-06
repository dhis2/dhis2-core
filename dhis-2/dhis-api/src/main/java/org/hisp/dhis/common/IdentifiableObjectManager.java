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

import javax.validation.constraints.NotNull;

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;

/**
 * @author Lars Helge Overland
 */
public interface IdentifiableObjectManager
{
    String ID = IdentifiableObjectManager.class.getName();

    void save( @NotNull IdentifiableObject object );

    void save( @NotNull IdentifiableObject object, boolean clearSharing );

    void save( @NotNull List<IdentifiableObject> objects );

    void update( @NotNull IdentifiableObject object );

    void update( @NotNull IdentifiableObject object, User user );

    void update( @NotNull List<IdentifiableObject> objects );

    void update( @NotNull List<IdentifiableObject> objects, User user );

    void delete( @NotNull IdentifiableObject object );

    void delete( @NotNull IdentifiableObject object, User user );

    /**
     * Lookup objects of unknown type.
     *
     * If the type is known at compile time this method should not be used.
     * Instead, use
     * {@link org.hisp.dhis.common.IdentifiableObjectManager#get(Class, String)}.
     *
     * @param uid a UID of an object of unknown type
     * @return The {@link IdentifiableObject} with the given UID
     */
    @NotNull
    Optional<? extends IdentifiableObject> find( @NotNull String uid );

    <T extends IdentifiableObject> @NotNull T get( @NotNull Class<T> type, long id );

    /**
     * Retrieves the object of the given type and UID, or null if no object
     * exists.
     *
     * @param type the object class type.
     * @param uid the UID.
     * @return the object with the given UID.
     */
    <T extends IdentifiableObject> T get( @NotNull Class<T> type, @NotNull String uid );

    /**
     * Retrieves the object of the given type and UID, throws exception if no
     * object exists.
     *
     * @param type the object class type.
     * @param uid the UID.
     * @return the object with the given UID.
     * @throws IllegalQueryException if no object exists.
     */
    <T extends IdentifiableObject> @NotNull T load( @NotNull Class<T> type, @NotNull String uid )
        throws IllegalQueryException;

    /**
     * Retrieves the object of the given type and UID, throws exception using
     * the given error code if no object exists.
     *
     * @param type the object class type.
     * @param errorCode the {@link ErrorCode} to use for the exception.
     * @param uid the UID.
     * @return the object with the given UID.
     * @throws IllegalQueryException if no object exists.
     */
    <T extends IdentifiableObject> @NotNull T load( @NotNull Class<T> type, @NotNull ErrorCode errorCode,
        @NotNull String uid )
        throws IllegalQueryException;

    <T extends IdentifiableObject> boolean exists( @NotNull Class<T> type, @NotNull String uid );

    IdentifiableObject get( @NotNull Collection<Class<? extends IdentifiableObject>> types,
        @NotNull String uid );

    IdentifiableObject get( @NotNull Collection<Class<? extends IdentifiableObject>> types,
        @NotNull IdScheme idScheme,
        String value );

    /**
     * Retrieves the object of the given type and code, or null if no object
     * exists.
     *
     * @param type the object class type.
     * @param code the code.
     * @return the object with the given code.
     */
    <T extends IdentifiableObject> T getByCode( @NotNull Class<T> type, @NotNull String code );

    /**
     * Retrieves the object of the given type and code, throws exception if no
     * object exists.
     *
     * @param type the object class type.
     * @param code the code.
     * @return the object with the given code.
     * @throws IllegalQueryException if no object exists.
     */
    <T extends IdentifiableObject> @NotNull T loadByCode( @NotNull Class<T> type, @NotNull String code )
        throws IllegalQueryException;

    <T extends IdentifiableObject> @NotNull List<T> getByCode( @NotNull Class<T> type,
        @NotNull Collection<String> codes );

    <T extends IdentifiableObject> T getByName( @NotNull Class<T> type, @NotNull String name );

    <T extends IdentifiableObject> T getByUniqueAttributeValue( @NotNull Class<T> type, @NotNull Attribute attribute,
        @NotNull String value );

    <T extends IdentifiableObject> T getByUniqueAttributeValue( @NotNull Class<T> type, @NotNull Attribute attribute,
        @NotNull String value,
        User userInfo );

    <T extends IdentifiableObject> T search( @NotNull Class<T> type, @NotNull String query );

    <T extends IdentifiableObject> @NotNull List<T> filter( @NotNull Class<T> type, @NotNull String query );

    <T extends IdentifiableObject> @NotNull List<T> getAll( @NotNull Class<T> type );

    <T extends IdentifiableObject> @NotNull List<T> getDataWriteAll( @NotNull Class<T> type );

    <T extends IdentifiableObject> @NotNull List<T> getDataReadAll( @NotNull Class<T> type );

    <T extends IdentifiableObject> @NotNull List<T> getAllSorted( @NotNull Class<T> type );

    <T extends IdentifiableObject> @NotNull List<T> getAllByAttributes( @NotNull Class<T> type,
        @NotNull List<Attribute> attributes );

    <T extends IdentifiableObject> @NotNull List<AttributeValue> getAllValuesByAttributes( @NotNull Class<T> type,
        @NotNull List<Attribute> attributes );

    <T extends IdentifiableObject> long countAllValuesByAttributes( @NotNull Class<T> type,
        @NotNull List<Attribute> attributes );

    <T extends IdentifiableObject> @NotNull List<T> getByUid( @NotNull Class<T> type,
        @NotNull Collection<String> uids );

    /**
     * Retrieves the objects of the given type and collection of UIDs, throws
     * exception is any object does not exist.
     *
     * @param type the object class type.
     * @param uids the collection of UIDs.
     * @return a list of objects.
     * @throws IllegalQueryException if any object does not exist.
     */
    <T extends IdentifiableObject> @NotNull List<T> loadByUid( @NotNull Class<T> type,
        @NotNull Collection<String> uids )
        throws IllegalQueryException;

    @NotNull
    List<IdentifiableObject> getByUid(
        @NotNull Collection<Class<? extends IdentifiableObject>> types,
        @NotNull Collection<String> uids );

    <T extends IdentifiableObject> @NotNull List<T> getById( @NotNull Class<T> type, @NotNull Collection<Long> ids );

    <T extends IdentifiableObject> @NotNull List<T> getOrdered( @NotNull Class<T> type, @NotNull IdScheme idScheme,
        @NotNull Collection<String> values );

    <T extends IdentifiableObject> @NotNull List<T> getByUidOrdered( @NotNull Class<T> type,
        @NotNull List<String> uids );

    <T extends IdentifiableObject> @NotNull List<T> getLikeName( @NotNull Class<T> type, @NotNull String name );

    <T extends IdentifiableObject> @NotNull List<T> getLikeName( @NotNull Class<T> type, @NotNull String name,
        boolean caseSensitive );

    <T extends IdentifiableObject> @NotNull List<T> getBetweenSorted( @NotNull Class<T> type, int first, int max );

    <T extends IdentifiableObject> @NotNull List<T> getBetweenLikeName( @NotNull Class<T> type,
        @NotNull Set<String> words, int first, int max );

    <T extends IdentifiableObject> Date getLastUpdated( @NotNull Class<T> type );

    <T extends IdentifiableObject> @NotNull Map<String, T> getIdMap( @NotNull Class<T> type,
        @NotNull IdentifiableProperty property );

    <T extends IdentifiableObject> @NotNull Map<String, T> getIdMap( @NotNull Class<T> type,
        @NotNull IdScheme idScheme );

    <T extends IdentifiableObject> @NotNull Map<String, T> getIdMapNoAcl( @NotNull Class<T> type,
        @NotNull IdentifiableProperty property );

    <T extends IdentifiableObject> @NotNull Map<String, T> getIdMapNoAcl( @NotNull Class<T> type,
        @NotNull IdScheme idScheme );

    <T extends IdentifiableObject> @NotNull List<T> getObjects( @NotNull Class<T> type,
        @NotNull IdentifiableProperty property,
        @NotNull Collection<String> identifiers );

    <T extends IdentifiableObject> @NotNull List<T> getObjects( @NotNull Class<T> type,
        @NotNull Collection<Long> identifiers );

    <T extends IdentifiableObject> T getObject( @NotNull Class<T> type, @NotNull IdentifiableProperty property,
        @NotNull String value );

    <T extends IdentifiableObject> T getObject( @NotNull Class<T> type, @NotNull IdScheme idScheme,
        @NotNull String value );

    IdentifiableObject getObject( @NotNull String uid, @NotNull String simpleClassName );

    IdentifiableObject getObject( long id, @NotNull String simpleClassName );

    <T extends IdentifiableObject> int getCount( @NotNull Class<T> type );

    <T extends IdentifiableObject> int getCountByCreated( @NotNull Class<T> type, @NotNull Date created );

    <T extends IdentifiableObject> int getCountByLastUpdated( @NotNull Class<T> type, @NotNull Date lastUpdated );

    <T extends DimensionalObject> @NotNull List<T> getDataDimensions( @NotNull Class<T> type );

    <T extends DimensionalObject> @NotNull List<T> getDataDimensionsNoAcl( @NotNull Class<T> type );

    void refresh( @NotNull Object object );

    /**
     * Resets all properties that are not owned by the object type.
     *
     * @param object object to reset
     */
    void resetNonOwnerProperties( @NotNull Object object );

    void flush();

    void clear();

    void evict( @NotNull Object object );

    <T extends IdentifiableObject> List<T> getByAttributeAndValue( @NotNull Class<T> type, @NotNull Attribute attribute,
        @NotNull String value );

    <T extends IdentifiableObject> boolean isAttributeValueUnique( @NotNull Class<T> type,
        @NotNull T object,
        @NotNull AttributeValue attributeValue );

    <T extends IdentifiableObject> boolean isAttributeValueUnique( @NotNull Class<T> type,
        @NotNull T object, @NotNull Attribute attribute, @NotNull String value );

    @NotNull
    <T extends IdentifiableObject> List<T> getAllByAttributeAndValues( @NotNull Class<T> type,
        @NotNull Attribute attribute, @NotNull List<String> values );

    @NotNull
    Map<Class<? extends IdentifiableObject>, IdentifiableObject> getDefaults();

    void updateTranslations( @NotNull IdentifiableObject persistedObject, @NotNull Set<Translation> translations );

    @NotNull
    <T extends IdentifiableObject> List<T> getNoAcl( @NotNull Class<T> type, @NotNull Collection<String> uids );

    boolean isDefault( @NotNull IdentifiableObject object );

    @NotNull
    List<String> getUidsCreatedBefore( @NotNull Class<? extends IdentifiableObject> type, @NotNull Date date );

    /**
     * Remove given UserGroup UID from all sharing records in database
     */
    void removeUserGroupFromSharing( @NotNull String userGroupUid );

    // -------------------------------------------------------------------------
    // NO ACL
    // -------------------------------------------------------------------------

    <T extends IdentifiableObject> T getNoAcl( @NotNull Class<T> type, @NotNull String uid );

    <T extends IdentifiableObject> void updateNoAcl( @NotNull T object );

    <T extends IdentifiableObject> @NotNull List<T> getAllNoAcl( @NotNull Class<T> type );
}
