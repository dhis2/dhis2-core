package org.hisp.dhis.attribute;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.util.List;

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

    /**
     * Gets attributes which are associated with data elements.
     *
     * @return a set of attributes associated with data elements.
     */
    List<Attribute> getDataElementAttributes();

    /**
     * Gets attributes which are associated with data element groups.
     *
     * @return a set of attributes associated with data element groups.
     */
    List<Attribute> getDataElementGroupAttributes();

    /**
     * Gets attributes which are associated with indicators.
     *
     * @return a set of attributes associated with indicators.
     */
    List<Attribute> getIndicatorAttributes();

    /**
     * Gets attributes which are associated with data elements.
     *
     * @return a set of attributes associated with data elements.
     */
    List<Attribute> getIndicatorGroupAttributes();

    /**
     * Gets attributes which are associated with data sets.
     *
     * @return a set of attributes associated with data sets.
     */
    List<Attribute> getDataSetAttributes();

    /**
     * Gets attributes which are associated with organisation units.
     *
     * @return a set of attributes associated with organisation units.
     */
    List<Attribute> getOrganisationUnitAttributes();

    /**
     * Gets attributes which are associated with organisation unit groups.
     *
     * @return a set of attributes associated with organisation unit groups.
     */
    List<Attribute> getOrganisationUnitGroupAttributes();

    /**
     * Gets attributes which are associated with organisation unit group sets.
     *
     * @return a set of attributes associated with organisation unit group sets.
     */
    List<Attribute> getOrganisationUnitGroupSetAttributes();

    /**
     * Gets attributes which are associated with users.
     *
     * @return a set of attributes which are associated with users.
     */
    List<Attribute> getUserAttributes();

    /**
     * Gets attributes which are associated with user groups.
     *
     * @return a set of attributes which are associated with user groups.
     */
    List<Attribute> getUserGroupAttributes();

    /**
     * Gets attributes which are associated with programs.
     *
     * @return a set of attributes which are associated with programs.
     */
    List<Attribute> getProgramAttributes();

    /**
     * Gets attributes which are associated with program stages.
     *
     * @return a set of attributes which are associated with programs.
     */
    List<Attribute> getProgramStageAttributes();

    /**
     * Gets attributes which are associated with tracked entities.
     *
     * @return a set of attributes which are associated with programs.
     */
    List<Attribute> getTrackedEntityAttributes();

    /**
     * Gets attributes which are associated with tracked entity attributes
     *
     * @return a set of attributes which are associated with tracked entity attributes
     */
    List<Attribute> getTrackedEntityAttributeAttributes();

    /**
     * Gets attributes which are associated with category option attributes
     *
     * @return a set of attributes which are associated with category option attributes
     */
    List<Attribute> getCategoryOptionAttributes();

    /**
     * Gets attributes which are associated with category option group attributes
     *
     * @return a set of attributes which are associated with category option group attributes
     */
    List<Attribute> getCategoryOptionGroupAttributes();

    /**
     * Gets the number of attributes.
     *
     * @return the number of attributes.
     */
    int getAttributeCount();

    /**
     * Gets the number of attributes with the given name.
     *
     * @return the number of attributes with the given name.
     */
    int getAttributeCountByName( String name );

    List<Attribute> getAttributesBetween( int first, int max );

    List<Attribute> getAttributesBetweenByName( String name, int first, int max );

    // -------------------------------------------------------------------------
    // AttributeValue
    // -------------------------------------------------------------------------

    /**
     * Adds an attribute value.
     *
     * @param attributeValue the attribute value.
     */
    void addAttributeValue( AttributeValue attributeValue );

    /**
     * Updates an attribute value.
     *
     * @param attributeValue the attribute value.
     */
    void updateAttributeValue( AttributeValue attributeValue );

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

    /**
     * Gets the number of attribute values.
     *
     * @return the number of attribute values.
     */
    int getAttributeValueCount();
}
