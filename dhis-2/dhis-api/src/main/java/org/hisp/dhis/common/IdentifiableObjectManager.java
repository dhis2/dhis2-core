package org.hisp.dhis.common;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.translation.ObjectTranslation;
import org.hisp.dhis.user.User;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Lars Helge Overland
 */
public interface IdentifiableObjectManager
{
    String ID = IdentifiableObjectManager.class.getName();

    void save( IdentifiableObject object );

    void save( IdentifiableObject object, User user );

    void save( IdentifiableObject object, boolean clearSharing );

    void save( IdentifiableObject object, User user, boolean clearSharing );

    void update( IdentifiableObject object );

    void update( IdentifiableObject object, User user );

    void update( List<IdentifiableObject> objects );

    void update( List<IdentifiableObject> objects, User user );

    void delete( IdentifiableObject object );

    void delete( IdentifiableObject object, User user );

    <T extends IdentifiableObject> T get( String uid );

    <T extends IdentifiableObject> T get( Class<T> clazz, int id );

    <T extends IdentifiableObject> T get( Class<T> clazz, String uid );

    <T extends IdentifiableObject> boolean exists( Class<T> clazz, String uid );

    <T extends IdentifiableObject> T get( Collection<Class<? extends IdentifiableObject>> classes, String uid );

    <T extends IdentifiableObject> T get( Collection<Class<? extends IdentifiableObject>> classes, IdScheme idScheme, String value );

    <T extends IdentifiableObject> T getByCode( Class<T> clazz, String code );

    <T extends IdentifiableObject> List<T> getByCode( Class<T> clazz, Collection<String> codes );

    <T extends IdentifiableObject> T getByName( Class<T> clazz, String name );

    <T extends IdentifiableObject> T getByUniqueAttributeValue( Class<T> clazz, Attribute attribute, String value );

    <T extends IdentifiableObject> T search( Class<T> clazz, String query );

    <T extends IdentifiableObject> List<T> filter( Class<T> clazz, String query );

    <T extends IdentifiableObject> List<T> getAll( Class<T> clazz );

    <T extends IdentifiableObject> List<T> getAllByName( Class<T> clazz, String name );

    <T extends IdentifiableObject> List<T> getAllByNameIgnoreCase( Class<T> clazz, String name );

    <T extends IdentifiableObject> List<T> getAllSorted( Class<T> clazz );

    <T extends IdentifiableObject> List<T> getAllSortedByLastUpdated( Class<T> clazz );

    <T extends IdentifiableObject> List<T> getAllByAttributes( Class<T> klass, List<Attribute> attributes );

    <T extends IdentifiableObject> List<T> getByUid( Class<T> clazz, Collection<String> uids );
    
    <T extends IdentifiableObject> List<T> getById( Class<T> clazz, Collection<Integer> ids );

    <T extends IdentifiableObject> List<T> getByUidOrdered( Class<T> clazz, List<String> uids );

    <T extends IdentifiableObject> List<T> getLikeName( Class<T> clazz, String name );

    <T extends NameableObject> List<T> getLikeShortName( Class<T> clazz, String shortName );

    <T extends IdentifiableObject> List<T> getBetween( Class<T> clazz, int first, int max );

    <T extends IdentifiableObject> List<T> getBetweenSorted( Class<T> clazz, int first, int max );

    <T extends IdentifiableObject> List<T> getBetweenLikeName( Class<T> clazz, String name, int first, int max );

    <T extends IdentifiableObject> List<T> getBetweenLikeName( Class<T> clazz, Set<String> words, int first, int max );

    <T extends IdentifiableObject> List<T> getByLastUpdated( Class<T> clazz, Date lastUpdated );

    <T extends IdentifiableObject> List<T> getByCreated( Class<T> clazz, Date created );

    <T extends IdentifiableObject> List<T> getByLastUpdatedSorted( Class<T> clazz, Date lastUpdated );

    <T extends IdentifiableObject> List<T> getByCreatedSorted( Class<T> clazz, Date created );

    <T extends IdentifiableObject> Date getLastUpdated( Class<T> clazz );

    <T extends IdentifiableObject> Set<Integer> convertToId( Class<T> clazz, Collection<String> uids );

    <T extends IdentifiableObject> Map<String, T> getIdMap( Class<T> clazz, IdentifiableProperty property );

    <T extends IdentifiableObject> Map<String, T> getIdMap( Class<T> clazz, IdScheme idScheme );

    <T extends IdentifiableObject> Map<String, T> getIdMapNoAcl( Class<T> clazz, IdentifiableProperty property );

    <T extends IdentifiableObject> Map<String, T> getIdMapNoAcl( Class<T> clazz, IdScheme idScheme );

    <T extends IdentifiableObject> List<T> getObjects( Class<T> clazz, IdentifiableProperty property, Collection<String> identifiers );

    <T extends IdentifiableObject> List<T> getObjects( Class<T> clazz, Collection<Integer> identifiers );

    <T extends IdentifiableObject> T getObject( Class<T> clazz, IdentifiableProperty property, String value );

    <T extends IdentifiableObject> T getObject( Class<T> clazz, IdScheme idScheme, String value );

    IdentifiableObject getObject( String uid, String simpleClassName );

    IdentifiableObject getObject( int id, String simpleClassName );

    <T extends IdentifiableObject> int getCount( Class<T> clazz );

    <T extends IdentifiableObject> int getCountByName( Class<T> clazz, String name );

    <T extends NameableObject> int getCountByShortName( Class<T> clazz, String shortName );

    <T extends IdentifiableObject> int getCountByCreated( Class<T> clazz, Date created );

    <T extends IdentifiableObject> int getCountByLastUpdated( Class<T> clazz, Date lastUpdated );

    <T extends IdentifiableObject> int getCountLikeName( Class<T> clazz, String name );

    <T extends NameableObject> int getCountLikeShortName( Class<T> clazz, String shortName );

    <T extends DimensionalObject> List<T> getDataDimensions( Class<T> clazz );

    <T extends DimensionalObject> List<T> getDataDimensionsNoAcl( Class<T> clazz );

    void refresh( Object object );

    void flush();

    void evict( Object object );

    <T extends IdentifiableObject> List<AttributeValue> getAttributeValueByAttribute( Class<T> klass, Attribute attribute );

    List<AttributeValue> getAttributeValueByAttributes( Class<? extends IdentifiableObject> klass, List<Attribute> attributes );

    <T extends IdentifiableObject> List<AttributeValue> getAttributeValueByAttributeAndValue( Class<T> klass, Attribute attribute, String value );

    <T extends IdentifiableObject> boolean isAttributeValueUnique( Class<? extends IdentifiableObject> klass, T object, AttributeValue attributeValue );

    <T extends IdentifiableObject> boolean isAttributeValueUnique( Class<? extends IdentifiableObject> klass, T object, Attribute attribute, String value );

    Map<Class<? extends IdentifiableObject>, IdentifiableObject> getDefaults();

    // -------------------------------------------------------------------------
    // NO ACL
    // -------------------------------------------------------------------------

    <T extends IdentifiableObject> T getNoAcl( Class<T> clazz, String uid );

    <T extends IdentifiableObject> T getNoAcl( Class<T> clazz, int id );

    <T extends IdentifiableObject> void updateNoAcl( T object );

    <T extends IdentifiableObject> int getCountNoAcl( Class<T> clazz );

    <T extends IdentifiableObject> List<T> getAllNoAcl( Class<T> clazz );

    <T extends IdentifiableObject> List<T> getBetweenNoAcl( Class<T> clazz, int first, int max );

    void updateTranslations( IdentifiableObject persistedObject, Set<ObjectTranslation> translations );

    <T extends IdentifiableObject> List<T> get( Class<T> clazz, Collection<String> uids );
}
