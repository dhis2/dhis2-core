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
import java.util.Set;

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

    void save( IdentifiableObject object );

    void save( IdentifiableObject object, boolean clearSharing );

    void save( List<IdentifiableObject> objects );

    void update( IdentifiableObject object );

    void update( IdentifiableObject object, User user );

    void update( List<IdentifiableObject> objects );

    void update( List<IdentifiableObject> objects, User user );

    void delete( IdentifiableObject object );

    void delete( IdentifiableObject object, User user );

    <T extends IdentifiableObject> T get( String uid );

    <T extends IdentifiableObject> T get( Class<T> type, long id );

    /**
     * Retrieves the object of the given type and UID, or null if no object
     * exists.
     *
     * @param <T>
     * @param type the object class type.
     * @param uid the UID.
     * @return the object with the given UID.
     */
    <T extends IdentifiableObject> T get( Class<T> type, String uid );

    /**
     * Retrieves the object of the given type and UID, throws exception if no
     * object exists.
     *
     * @param <T>
     * @param type the object class type.
     * @param uid the UID.
     * @return the object with the given UID.
     * @throws IllegalQueryException if no object exists.
     */
    <T extends IdentifiableObject> T load( Class<T> type, String uid )
        throws IllegalQueryException;

    /**
     * Retrieves the object of the given type and UID, throws exception using
     * the given error code if no object exists.
     *
     * @param <T>
     * @param type the object class type.
     * @param errorCode the {@link ErrorCode} to use for the exception.
     * @param uid the UID.
     * @return the object with the given UID.
     * @throws IllegalQueryException if no object exists.
     */
    <T extends IdentifiableObject> T load( Class<T> type, ErrorCode errorCode, String uid )
        throws IllegalQueryException;

    <T extends IdentifiableObject> boolean exists( Class<T> type, String uid );

    <T extends IdentifiableObject> T get( Collection<Class<? extends IdentifiableObject>> types, String uid );

    <T extends IdentifiableObject> T get( Collection<Class<? extends IdentifiableObject>> types, IdScheme idScheme,
        String value );

    /**
     * Retrieves the object of the given type and code, or null if no object
     * exists.
     *
     * @param <T>
     * @param type the object class type.
     * @param code the code.
     * @return the object with the given code.
     */
    <T extends IdentifiableObject> T getByCode( Class<T> type, String code );

    /**
     * Retrieves the object of the given type and code, throws exception if no
     * object exists.
     *
     * @param <T>
     * @param type the object class type.
     * @param code the code.
     * @return the object with the given code.
     * @throws IllegalQueryException if no object exists.
     */
    <T extends IdentifiableObject> T loadByCode( Class<T> type, String code )
        throws IllegalQueryException;

    <T extends IdentifiableObject> List<T> getByCode( Class<T> type, Collection<String> codes );

    <T extends IdentifiableObject> T getByName( Class<T> type, String name );

    <T extends IdentifiableObject> T getByUniqueAttributeValue( Class<T> type, Attribute attribute, String value );

    <T extends IdentifiableObject> T getByUniqueAttributeValue( Class<T> type, Attribute attribute, String value,
        User userInfo );

    <T extends IdentifiableObject> T search( Class<T> type, String query );

    <T extends IdentifiableObject> List<T> filter( Class<T> type, String query );

    <T extends IdentifiableObject> List<T> getAll( Class<T> type );

    <T extends IdentifiableObject> List<T> getDataWriteAll( Class<T> type );

    <T extends IdentifiableObject> List<T> getDataReadAll( Class<T> type );

    <T extends IdentifiableObject> List<T> getAllSorted( Class<T> type );

    <T extends IdentifiableObject> List<T> getAllByAttributes( Class<T> type, List<Attribute> attributes );

    <T extends IdentifiableObject> List<AttributeValue> getAllValuesByAttributes( Class<T> type,
        List<Attribute> attributes );

    <T extends IdentifiableObject> long countAllValuesByAttributes( Class<T> type, List<Attribute> attributes );

    <T extends IdentifiableObject> List<T> getByUid( Class<T> type, Collection<String> uids );

    /**
     * Retrieves the objects of the given type and collection of UIDs, throws
     * exception is any object does not exist.
     *
     * @param <T>
     * @param type the object class type.
     * @param uids the collection of UIDs.
     * @return a list of objects.
     * @throws IllegalQueryException if any object does not exist.
     */
    <T extends IdentifiableObject> List<T> loadByUid( Class<T> type, Collection<String> uids )
        throws IllegalQueryException;

    <T extends IdentifiableObject> List<T> getByUid( Collection<Class<? extends IdentifiableObject>> types,
        Collection<String> uids );

    <T extends IdentifiableObject> List<T> getById( Class<T> type, Collection<Long> ids );

    <T extends IdentifiableObject> List<T> getOrdered( Class<T> type, IdScheme idScheme, Collection<String> values );

    <T extends IdentifiableObject> List<T> getByUidOrdered( Class<T> type, List<String> uids );

    <T extends IdentifiableObject> List<T> getLikeName( Class<T> type, String name );

    <T extends IdentifiableObject> List<T> getLikeName( Class<T> type, String name, boolean caseSensitive );

    <T extends IdentifiableObject> List<T> getBetweenSorted( Class<T> type, int first, int max );

    <T extends IdentifiableObject> List<T> getBetweenLikeName( Class<T> type, Set<String> words, int first, int max );

    <T extends IdentifiableObject> Date getLastUpdated( Class<T> type );

    <T extends IdentifiableObject> Map<String, T> getIdMap( Class<T> type, IdentifiableProperty property );

    <T extends IdentifiableObject> Map<String, T> getIdMap( Class<T> type, IdScheme idScheme );

    <T extends IdentifiableObject> Map<String, T> getIdMapNoAcl( Class<T> type, IdentifiableProperty property );

    <T extends IdentifiableObject> Map<String, T> getIdMapNoAcl( Class<T> type, IdScheme idScheme );

    <T extends IdentifiableObject> List<T> getObjects( Class<T> type, IdentifiableProperty property,
        Collection<String> identifiers );

    <T extends IdentifiableObject> List<T> getObjects( Class<T> type, Collection<Long> identifiers );

    <T extends IdentifiableObject> T getObject( Class<T> type, IdentifiableProperty property, String value );

    <T extends IdentifiableObject> T getObject( Class<T> type, IdScheme idScheme, String value );

    IdentifiableObject getObject( String uid, String simpleClassName );

    IdentifiableObject getObject( long id, String simpleClassName );

    <T extends IdentifiableObject> int getCount( Class<T> type );

    <T extends IdentifiableObject> int getCountByCreated( Class<T> type, Date created );

    <T extends IdentifiableObject> int getCountByLastUpdated( Class<T> type, Date lastUpdated );

    <T extends DimensionalObject> List<T> getDataDimensions( Class<T> type );

    <T extends DimensionalObject> List<T> getDataDimensionsNoAcl( Class<T> type );

    void refresh( Object object );

    /**
     * Resets all properties that are not owned by the object type.
     *
     * @param object object to reset
     */
    void resetNonOwnerProperties( Object object );

    void flush();

    void clear();

    void evict( Object object );

    <T extends IdentifiableObject> List<T> getByAttributeAndValue( Class<T> type, Attribute attribute, String value );

    <T extends IdentifiableObject> boolean isAttributeValueUnique( Class<? extends IdentifiableObject> type, T object,
        AttributeValue attributeValue );

    <T extends IdentifiableObject> boolean isAttributeValueUnique( Class<? extends IdentifiableObject> type, T object,
        Attribute attribute, String value );

    List<? extends IdentifiableObject> getAllByAttributeAndValues( Class<? extends IdentifiableObject> type,
        Attribute attribute, List<String> values );

    Map<Class<? extends IdentifiableObject>, IdentifiableObject> getDefaults();

    void updateTranslations( IdentifiableObject persistedObject, Set<Translation> translations );

    <T extends IdentifiableObject> List<T> getNoAcl( Class<T> type, Collection<String> uids );

    boolean isDefault( IdentifiableObject object );

    List<String> getUidsCreatedBefore( Class<? extends IdentifiableObject> type, Date date );

    /**
     * Remove given UserGroup UID from all sharing records in database
     */
    void removeUserGroupFromSharing( String userGroupUid );

    // -------------------------------------------------------------------------
    // NO ACL
    // -------------------------------------------------------------------------

    <T extends IdentifiableObject> T getNoAcl( Class<T> type, String uid );

    <T extends IdentifiableObject> void updateNoAcl( T object );

    <T extends IdentifiableObject> List<T> getAllNoAcl( Class<T> type );
}
