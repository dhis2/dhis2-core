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

import java.util.List;

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;

/**
 * @author Lars Helge Overland
 */
public interface GenericStore<T>
{
    /**
     * Class of the object for this store.
     */
    Class<T> getClazz();

    /**
     * Saves the given object instance, with clear sharing set to true.
     *
     * @param object the object instance.
     */
    void save( T object );

    /**
     * Updates the given object instance.
     *
     * @param object the object instance.
     */
    void update( T object );

    /**
     * Removes the given object instance.
     *
     * @param object the object instance to delete.
     */
    void delete( T object );

    /**
     * Retrieves the object with the given identifier. This method will first
     * look in the current Session, then hit the database if not existing.
     *
     * @param id the object identifier.
     * @return the object identified by the given identifier.
     */
    T get( long id );

    long countAllValuesByAttributes( List<Attribute> attributes );

    List<AttributeValue> getAttributeValueByAttribute( Attribute attribute );

    /**
     * Gets the count of objects.
     *
     * @return the count of objects.
     */
    int getCount();

    /**
     * Retrieves a List of all objects.
     *
     * @return a List of all objects.
     */
    List<T> getAll();

    List<T> getByAttribute( Attribute attribute );

    List<T> getByAttributeAndValue( Attribute attribute, String value );

    List<T> getAllByAttributes( List<Attribute> attributes );

    List<AttributeValue> getAllValuesByAttributes( List<Attribute> attributes );

    List<AttributeValue> getAttributeValueByAttributeAndValue( Attribute attribute, String value );

    List<T> getByAttributeValue( AttributeValue attributeValue );

    <P extends IdentifiableObject> boolean isAttributeValueUnique( P object, AttributeValue attributeValue );

    <P extends IdentifiableObject> boolean isAttributeValueUnique( P object, Attribute attribute, String value );

    List<T> getAllByAttributeAndValues( Attribute attribute, List<String> values );

    /**
     * Update a specific attribute of all objects.
     *
     * This can be used to both set (init) or clear a particular attribute.
     *
     * @param attribute the attribute to update for all objects
     * @param newValue the attribute value to store (might override existing
     *        value for that attribute)
     * @param createMissing true, if a attribute should be created should it not
     *        yet exist, false to skip such objects
     * @return number of objects affected (note that this will not distinguish
     *         between attribute values that changed by this update and those
     *         that stay the same)
     */
    int updateAllAttributeValues( Attribute attribute, String newValue, boolean createMissing );
}
