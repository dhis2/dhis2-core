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
package org.hisp.dhis.test.setup;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.function.BiFunction;
import java.util.function.Function;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.test.setup.MetadataSetup.CategoryComboSetup;
import org.hisp.dhis.test.setup.MetadataSetup.CategoryOptionComboSetup;
import org.hisp.dhis.test.setup.MetadataSetup.CategoryOptionSetup;
import org.hisp.dhis.test.setup.MetadataSetup.CategorySetup;
import org.hisp.dhis.test.setup.MetadataSetup.DataElementSetup;
import org.hisp.dhis.test.setup.MetadataSetup.Objects;
import org.hisp.dhis.test.setup.MetadataSetup.OrganisationUnitSetup;
import org.springframework.stereotype.Component;

/**
 * A {@link MetadataSetupExecutor} that uses services directly to create the
 * target {@link MetadataSetup}.
 */
@Slf4j
@Component
@AllArgsConstructor
public class MetadataSetupServiceExecutor implements MetadataSetupExecutor
{
    private final PeriodService periodService;

    private final CategoryService categoryService;

    private final OrganisationUnitService organisationUnitService;

    private final DataElementService dataElementService;

    @Override
    public void create( MetadataSetup setup )
    {
        // OBS! order matters! objects are created in such an order that
        // referenced objects are already created
        createEach( setup.getPeriods(), this::createPeriod );

        createEach( setup.getCategoryOptions(), this::createCategoryOptions );
        createEach( setup.getCategories(), this::createCategory );
        createEach( setup.getCategoryCombos(), this::createCategoryCombo );
        createEach( setup.getCategoryOptionCombos(), this::createCategoryOptionCombo );

        createEach( setup.getOrganisationUnits(), this::createOrganisationUnit );
        createEach( setup.getDataElements(), this::createDataElement );
    }

    private <S extends MetadataSetup.AbstractSetup<T>, T extends IdentifiableObject> void createEach(
        Objects<S> objects, Function<S, T> creator )
    {
        objects.forEach( setup -> {
            try
            {
                T obj = creator.apply( setup );
                setup.setObject( obj );
                setup.setUid( obj.getUid() );
            }
            catch ( Exception ex )
            {
                log.error( "Failed to create " + setup.getClass().getSimpleName() + " " + setup.getName(), ex );
                throw ex;
            }
        } );
    }

    private <S extends MetadataSetup.AbstractSetup<T>, T extends IdentifiableObject> void createEach(
        Objects<S> objects, BiFunction<S, Objects<S>, T> creator )
    {
        createEach( objects, setup -> creator.apply( setup, objects ) );
    }

    private Period createPeriod( MetadataSetup.PeriodSetup setup )
    {
        Period obj = PeriodType.getPeriodFromIsoString( setup.getIsoPeriod() );
        periodService.addPeriod( obj );
        return obj;
    }

    private CategoryOption createCategoryOptions( CategoryOptionSetup setup )
    {
        CategoryOption obj = new CategoryOption( setup.getName() );
        obj.setStartDate( setup.getStartDate() );
        obj.setEndDate( setup.getEndDate() );
        obj.setAutoFields();
        categoryService.addCategoryOption( obj );
        return obj;
    }

    private Category createCategory( CategorySetup setup, Objects<CategorySetup> categories )
    {
        Category obj = new Category( setup.getName(), setup.getDataDimensionType() );
        obj.setUid( setup.getUid() );
        obj.setShortName( categories.nextUnique( setup, CategorySetup::getShortName ) );
        setup.getOptions().forEach( option -> obj.addCategoryOption( option.getObject() ) );
        obj.setAutoFields();
        categoryService.addCategory( obj );
        return obj;
    }

    private CategoryCombo createCategoryCombo( CategoryComboSetup setup )
    {
        CategoryCombo obj = new CategoryCombo( setup.getName(), setup.getDataDimensionType(),
            setup.getCategories().stream().map( CategorySetup::getObject ).collect( toList() ) );
        obj.setUid( setup.getUid() );
        obj.setAutoFields();
        categoryService.addCategoryCombo( obj );
        return obj;
    }

    private CategoryOptionCombo createCategoryOptionCombo( CategoryOptionComboSetup setup,
        Objects<CategoryOptionComboSetup> optionCombos )
    {
        CategoryOptionCombo obj = new CategoryOptionCombo();
        obj.setUid( setup.getUid() );
        obj.setName( setup.getName() );
        obj.setCode( optionCombos.nextUnique( setup, CategoryOptionComboSetup::getCode ) );
        obj.setCategoryCombo( setup.getCombo().getObject() );
        obj.setCategoryOptions( setup.getOptions().stream().map( CategoryOptionSetup::getObject ).collect( toSet() ) );
        obj.setAutoFields();
        categoryService.addCategoryOptionCombo( obj );
        return obj;
    }

    private OrganisationUnit createOrganisationUnit( OrganisationUnitSetup setup,
        Objects<OrganisationUnitSetup> organisationUnits )
    {
        OrganisationUnit obj = new OrganisationUnit();
        obj.setUid( setup.getUid() );
        obj.setName( setup.getName() );
        obj.setShortName( organisationUnits.nextUnique( setup, OrganisationUnitSetup::getShortName ) );
        obj.setCode( organisationUnits.nextUnique( setup, OrganisationUnitSetup::getCode ) );
        obj.setOpeningDate( setup.getOpeningDate() );
        obj.setComment( setup.getName() + "Comment" );
        obj.setAutoFields();
        organisationUnitService.addOrganisationUnit( obj );
        return obj;
    }

    private DataElement createDataElement( DataElementSetup setup, Objects<DataElementSetup> dataElements )
    {
        DataElement obj = new DataElement();
        obj.setUid( setup.getUid() );
        obj.setName( setup.getName() );
        obj.setCode( dataElements.nextUnique( setup, DataElementSetup::getCode ) );
        obj.setShortName( dataElements.nextUnique( setup, DataElementSetup::getShortName ) );
        obj.setZeroIsSignificant( setup.isZeroIsSignificant() );
        obj.setValueType( setup.getValueType() );
        obj.setAggregationType( setup.getAggregationType() );
        obj.setDomainType( setup.getDomainType() );
        obj.setDescription( setup.getName() + "Description" );
        CategoryComboSetup categoryCombo = setup.getCategoryCombo();
        obj.setCategoryCombo( categoryCombo != null
            ? categoryCombo.getObject()
            : categoryService.getDefaultCategoryCombo() );
        obj.setAutoFields();
        dataElementService.addDataElement( obj );
        return obj;
    }
}
