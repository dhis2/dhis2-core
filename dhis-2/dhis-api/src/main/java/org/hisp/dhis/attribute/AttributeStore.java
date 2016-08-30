package org.hisp.dhis.attribute;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import com.google.common.collect.ImmutableMap;
import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.dataelement.CategoryOptionGroup;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.sqlview.SqlView;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;

import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface AttributeStore
    extends GenericIdentifiableObjectStore<Attribute>
{
    String ID = AttributeStore.class.getName();

    ImmutableMap<Class<?>, String> CLASS_ATTRIBUTE_MAP = ImmutableMap.<Class<?>, String>builder()
        .put( DataElement.class, "dataElementAttribute" )
        .put( DataElementGroup.class, "dataElementGroupAttribute" )
        .put( Indicator.class, "indicatorAttribute" )
        .put( IndicatorGroup.class, "indicatorGroupAttribute" )
        .put( DataSet.class, "dataSetAttribute" )
        .put( OrganisationUnit.class, "organisationUnitAttribute" )
        .put( OrganisationUnitGroup.class, "organisationUnitGroupAttribute" )
        .put( OrganisationUnitGroupSet.class, "organisationUnitGroupSetAttribute" )
        .put( User.class, "userAttribute" )
        .put( UserGroup.class, "userGroupAttribute" )
        .put( Program.class, "programAttribute" )
        .put( ProgramStage.class, "programStageAttribute" )
        .put( TrackedEntity.class, "trackedEntityAttribute" )
        .put( TrackedEntityAttribute.class, "trackedEntityAttributeAttribute" )
        .put( DataElementCategoryOption.class, "categoryOptionAttribute" )
        .put( CategoryOptionGroup.class, "categoryOptionGroupAttribute" )
        .put( Document.class, "documentAttribute" )
        .put( Option.class, "optionAttribute" )
        .put( OptionSet.class, "optionSetAttribute" )
        .put( Constant.class, "constantAttribute")
        .put( LegendSet.class, "legendSetAttribute")
        .put( ProgramIndicator.class, "programIndicatorAttribute")
        .put( SqlView.class, "sqlViewAttribute")
        .put( Section.class, "sectionAttribute")
        .build();

    /**
     * Get all metadata attributes for a given class, returns empty list for un-supported types.
     *
     * @param klass Class to get metadata attributes for
     * @return List of attributes for this class
     */
    List<Attribute> getAttributes( Class<?> klass );

    /**
     * Get all mandatory metadata attributes for a given class, returns empty list for un-supported types.
     *
     * @param klass Class to get metadata attributes for
     * @return List of mandatory metadata attributes for this class
     */
    List<Attribute> getMandatoryAttributes( Class<?> klass );

    /**
     * Get all unique metadata attributes for a given class, returns empty list for un-supported types.
     *
     * @param klass Class to get metadata attributes for
     * @return List of unique metadata attributes for this class
     */
    List<Attribute> getUniqueAttributes( Class<?> klass );
}
