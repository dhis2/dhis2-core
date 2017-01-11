package org.hisp.dhis.attribute;

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

import org.hisp.dhis.attribute.exception.NonUniqueAttributeValueException;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.feedback.ErrorReport;

import java.util.List;
import java.util.Set;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface AttributeService
{
    String ID = AttributeService.class.getName();

    // -------------------------------------------------------------------------
    // Attribute
    // -------------------------------------------------------------------------

    /**
     * Adds an attribute.
     *
     * @param attribute the attribute.
     */
    void addAttribute( Attribute attribute );

    /**
     * Updates an attribute.
     *
     * @param attribute the attribute.
     */
    void updateAttribute( Attribute attribute );

    /**
     * Deletes an attribute.
     *
     * @param attribute the attribute.
     */
    void deleteAttribute( Attribute attribute );

    /**
     * Gets the attribute with the given id.
     *
     * @param id the attribute id.
     * @return the attribute with the given id.
     */
    Attribute getAttribute( int id );

    /**
     * Gets the attribute with the given uid.
     *
     * @param id the attribute uid.
     * @return the attribute with the given uid.
     */
    Attribute getAttribute( String uid );

    /**
     * Gets the attribute with the given name.
     *
     * @param name the name.
     * @return the attribute with the given name.
     */
    Attribute getAttributeByName( String name );

    /**
     * Gets the attribute with the given code.
     *
     * @param code the code.
     * @return the attribute with the given code.
     */
    Attribute getAttributeByCode( String code );

    /**
     * Gets all attributes.
     *
     * @return a set of all attributes.
     */
    List<Attribute> getAllAttributes();

    List<Attribute> getAttributes( Class<?> klass );

    List<Attribute> getMandatoryAttributes( Class<?> klass );

    List<Attribute> getUniqueAttributes( Class<?> klass );

    // -------------------------------------------------------------------------
    // AttributeValue
    // -------------------------------------------------------------------------

    /**
     * Adds an attribute value.
     *
     * @param attributeValue the attribute value.
     */
    <T extends IdentifiableObject> void addAttributeValue( T object, AttributeValue attributeValue ) throws NonUniqueAttributeValueException;

    /**
     * Updates an attribute value.
     *
     * @param attributeValue the attribute value.
     */
    <T extends IdentifiableObject> void updateAttributeValue( T object, AttributeValue attributeValue ) throws NonUniqueAttributeValueException;

    /**
     * Deletes an attribute value.
     *
     * @param attributeValue the attribute value.
     */
    void deleteAttributeValue( AttributeValue attributeValue );

    /**
     * Gets the attribute value with the given id.
     *
     * @param id the id.
     * @return the attribute value with the given id.
     */
    AttributeValue getAttributeValue( int id );

    /**
     * Gets all attribute values.
     *
     * @return a set with all attribute values.
     */
    List<AttributeValue> getAllAttributeValues();

    List<AttributeValue> getAllAttributeValuesByAttributes( List<Attribute> attributes );

    List<AttributeValue> getAllAttributeValuesByAttribute( Attribute attribute );

    List<AttributeValue> getAllAttributeValuesByAttributeAndValue( Attribute attribute, String value );

    <T extends IdentifiableObject> boolean isAttributeValueUnique( T object, AttributeValue attributeValue );

    /**
     * Gets the number of attribute values.
     *
     * @return the number of attribute values.
     */
    int getAttributeValueCount();

    <T extends IdentifiableObject> List<ErrorReport> validateAttributeValues( T object, Set<AttributeValue> attributeValues );

    <T extends IdentifiableObject> void updateAttributeValues( T object, List<String> jsonAttributeValues ) throws Exception;

    <T extends IdentifiableObject> void updateAttributeValues( T object, Set<AttributeValue> attributeValues ) throws Exception;
}
