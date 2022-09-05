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
import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.test.setup.MetadataSetup.AbstractSetup;
import org.hisp.dhis.test.setup.MetadataSetup.AttributeSetup;
import org.hisp.dhis.test.setup.MetadataSetup.CategoryComboSetup;
import org.hisp.dhis.test.setup.MetadataSetup.CategoryOptionComboSetup;
import org.hisp.dhis.test.setup.MetadataSetup.CategoryOptionSetup;
import org.hisp.dhis.test.setup.MetadataSetup.CategorySetup;
import org.hisp.dhis.test.setup.MetadataSetup.DataElementSetup;
import org.hisp.dhis.test.setup.MetadataSetup.Objects;
import org.hisp.dhis.test.setup.MetadataSetup.OrganisationUnitSetup;
import org.hisp.dhis.test.setup.MetadataSetup.PeriodSetup;
import org.hisp.dhis.test.setup.MetadataSetup.UserGroupSetup;
import org.hisp.dhis.test.setup.MetadataSetup.UserRoleSetup;
import org.hisp.dhis.test.setup.MetadataSetup.UserSetup;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
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
    /**
     * name of the default category, category combo, ...
     */
    private static final String DEFAULT_NAME = "default";

    private final AttributeService attributeService;

    private final UserService userService;

    private final UserGroupService userGroupService;

    private final PeriodService periodService;

    private final CategoryService categoryService;

    private final OrganisationUnitService organisationUnitService;

    private final DataElementService dataElementService;

    private final IdentifiableObjectManager manager;

    @Override
    public void create( MetadataSetup setup )
    {
        // OBS! order matters! objects are created in such an order that
        // referenced objects are already created
        createEach( setup.getAttributes(), this::createAttribute );

        createEach( setup.getOrganisationUnits(), this::createOrganisationUnit );

        createEach( setup.getUsers(), this::createUser );
        createEach( setup.getUserGroups(), this::createUserGroup );
        createEach( setup.getRoles(), this::createRole );

        createEach( setup.getPeriods(), this::createPeriod );

        createEach( setup.getCategoryOptions(), this::createCategoryOptions );
        createEach( setup.getCategories(), this::createCategory );
        createEach( setup.getCategoryCombos(), this::createCategoryCombo );
        createEach( setup.getCategoryOptionCombos(), this::createCategoryOptionCombo );

        createEach( setup.getDataElements(), this::createDataElement );
    }

    private <S extends AbstractSetup<T>, T extends IdentifiableObject> void createEach(
        Objects<S> objects, Function<S, T> creator )
    {
        objects.forEach( setup -> {
            try
            {
                T obj = creator.apply( setup );
                setup.setObject( obj );
                setup.setUid( obj.getUid() );
                Map<AttributeSetup, String> attributeValues = setup.getAttributeValues();
                if ( !attributeValues.isEmpty() )
                {
                    obj.setAttributeValues( attributeValues.entrySet().stream()
                        .map( e -> new AttributeValue( setup.getContext().getAttribute( e.getKey().getName() ),
                            e.getValue() ) )
                        .collect( toUnmodifiableSet() ) );
                    manager.update( obj );
                }
            }
            catch ( Exception ex )
            {
                log.error( "Failed to create " + setup.getClass().getSimpleName() + " " + setup.getName(), ex );
                throw ex;
            }
        } );
    }

    private <S extends AbstractSetup<T>, T extends IdentifiableObject> void createEach(
        Objects<S> objects, BiFunction<S, Objects<S>, T> creator )
    {
        createEach( objects, setup -> creator.apply( setup, objects ) );
    }

    private Attribute createAttribute( AttributeSetup setup )
    {
        Attribute obj = new Attribute( setup.getName(),
            orElse( setup, AttributeSetup::getValueType, ValueType.TEXT ) );
        obj.setUid( setup.getUid() );
        obj.setUnique( setup.isUnique() );
        obj.setMandatory( setup.isMandatory() );
        setup.getObjectTypes().forEach( type -> obj.setAttribute( type, true ) );
        obj.setAutoFields();
        attributeService.addAttribute( obj );
        return obj;
    }

    private User createUser( UserSetup setup )
    {
        String name = setup.getName();

        User obj = new User();
        obj.setUid( setup.getUid() );
        obj.setCreatedBy( obj );
        obj.setUsername( orElse( setup, UserSetup::getUsername, "username" + name ).toLowerCase() );
        obj.setPassword( orElse( setup, UserSetup::getPassword, "password" + name ) );
        obj.setFirstName( orElse( setup, UserSetup::getFirstName, "FirstName" + name ) );
        obj.setSurname( orElse( setup, UserSetup::getSurname, "Surname" + name ) );
        obj.setEmail( orElse( setup, UserSetup::getEmail, "Email" + name ).toLowerCase() );
        obj.setPhoneNumber( orElse( setup, UserSetup::getPhoneNumber, "PhoneNumber" + name ) );
        obj.setCode( orElse( setup, AbstractSetup::getCode, "UserCode" + name ) );
        obj.setAutoFields();
        userService.addUser( obj );
        return obj;
    }

    private UserGroup createUserGroup( UserGroupSetup setup )
    {
        UserGroup obj = new UserGroup();
        obj.setName( setup.getName() );
        obj.setUid( setup.getUid() );
        obj.setCode( orElse( setup, AbstractSetup::getCode, "UserGroupCode" + setup.getName() ) );
        obj.setMembers( toObjectSet( setup.getMembers(), UserSetup::getObject ) );
        obj.setManagedByGroups( toObjectSet( setup.getManagedByGroups(), UserGroupSetup::getObject ) );
        obj.setManagedGroups( toObjectSet( setup.getManagedGroups(), UserGroupSetup::getObject ) );
        obj.setAutoFields();
        userGroupService.addUserGroup( obj );
        return obj;
    }

    private UserRole createRole( UserRoleSetup setup )
    {
        UserRole obj = new UserRole();
        obj.setName( setup.getName() );
        obj.setUid( setup.getUid() );
        obj.setAuthorities( setup.getAuthorities() );
        obj.setAutoFields();
        obj.setMembers( toObjectSet( setup.getMembers(), UserSetup::getObject ) );
        userService.addUserRole( obj );
        return obj;
    }

    private Period createPeriod( PeriodSetup setup )
    {
        Period obj = PeriodType.getPeriodFromIsoString( setup.getIsoPeriod() );
        periodService.addPeriod( obj );
        return obj;
    }

    private CategoryOption createCategoryOptions( CategoryOptionSetup setup )
    {
        if ( DEFAULT_NAME.equals( setup.getName() ) )
        {
            return categoryService.getDefaultCategoryOption();
        }
        CategoryOption obj = new CategoryOption( setup.getName() );
        obj.setStartDate( setup.getStartDate() );
        obj.setEndDate( setup.getEndDate() );
        obj.setAutoFields();
        categoryService.addCategoryOption( obj );
        return obj;
    }

    private Category createCategory( CategorySetup setup, Objects<CategorySetup> categories )
    {
        if ( "default".equals( setup.getName() ) )
        {
            return categoryService.getDefaultCategory();
        }
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
        if ( "default".equals( setup.getName() ) )
        {
            return categoryService.getDefaultCategoryCombo();
        }
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
        if ( "default".equals( setup.getName() ) )
        {
            return categoryService.getDefaultCategoryOptionCombo();
        }
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

    private static <T, E> T orElse( E entity, Function<E, T> getter, T orElse )
    {
        T value = getter.apply( entity );
        return value == null ? orElse : value;
    }

    private static <T extends AbstractSetup<?>, E> Set<E> toObjectSet( Set<T> set, Function<T, E> toObject )
    {
        return set.stream().map( toObject ).collect( toUnmodifiableSet() );
    }
}
