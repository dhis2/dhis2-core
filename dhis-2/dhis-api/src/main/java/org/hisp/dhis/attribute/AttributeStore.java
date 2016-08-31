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

import org.hisp.dhis.common.GenericIdentifiableObjectStore;

/**
 * @author mortenoh
 */
public interface AttributeStore
    extends GenericIdentifiableObjectStore<Attribute>
{
    String ID = AttributeStore.class.getName();

    /**
     * Get all attributes that are enabled for data elements.
     *
     * @return All attributes with attribute.dataElement = true
     */
    List<Attribute> getDataElementAttributes();

    /**
     * Get all attributes that are enabled for data element groups.
     *
     * @return All attributes with attribute.dataElementGroup = true
     */
    List<Attribute> getDataElementGroupAttributes();

    /**
     * Get all attributes that are enabled for indicators.
     *
     * @return All attributes with attribute.indicator = true
     */
    List<Attribute> getIndicatorAttributes();

    /**
     * Get all attributes that are enabled for indicator groups.
     *
     * @return All attributes with attribute.indicatorGroup = true
     */
    List<Attribute> getIndicatorGroupAttributes();

    /**
     * Get all attributes that are enabled for data sets.
     *
     * @return All attributes with attribute.dataSet = true
     */
    List<Attribute> getDataSetAttributes();

    /**
     * Get all attributes that are enabled for organisation units.
     *
     * @return All attributes with attribute.organisationUnit = true
     */
    List<Attribute> getOrganisationUnitAttributes();

    /**
     * Get all attributes that are enabled for organisation unit groups.
     *
     * @return All attributes with attribute.organisationUnitGroup = true
     */
    List<Attribute> getOrganisationUnitGroupAttributes();

    /**
     * Get all attributes that are enabled for organisation unit group sets.
     *
     * @return All attributes with attribute.organisationUnitGroupSet = true
     */
    List<Attribute> getOrganisationUnitGroupSetAttributes();

    /**
     * Get all attributes that are enabled for users.
     *
     * @return All attributes with attribute.organisationUnit = true
     */
    List<Attribute> getUserAttributes();

    /**
     * Get all attributes that are enabled for user group.
     *
     * @return All attributes with attribute.organisationUnitGroup = true
     */
    List<Attribute> getUserGroupAttributes();

    /**
     * Get all attributes that are enabled for program.
     *
     * @return All attributes with attribute.program = true
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
}
