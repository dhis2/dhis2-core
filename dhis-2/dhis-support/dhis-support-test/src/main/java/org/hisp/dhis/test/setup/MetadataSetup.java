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

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyIterator;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserRole;

/**
 * A description of the metadata model that should be created.
 *
 * @author Jan Bernitt
 */
@Getter
public class MetadataSetup implements TestObjectRegistry, TestValueGenerator
{
    public static final Consumer<Object> withDefaultSetup = x -> {
    };

    private final Objects<AttributeSetup> attributes = new Objects<>( AttributeSetup::getName );

    private final Objects<OrganisationUnitSetup> organisationUnits = new Objects<>( OrganisationUnitSetup::getName );

    private final Objects<UserSetup> users = new Objects<>( UserSetup::getName );

    private final Objects<UserGroupSetup> userGroups = new Objects<>( UserGroupSetup::getName );

    private final Objects<UserRoleSetup> roles = new Objects<>( UserRoleSetup::getName );

    private final Objects<CategorySetup> categories = new Objects<>( CategorySetup::getName );

    private final Objects<CategoryOptionSetup> categoryOptions = new Objects<>( CategoryOptionSetup::getName );

    private final Objects<CategoryComboSetup> categoryCombos = new Objects<>( CategoryComboSetup::getName );

    private final Objects<CategoryOptionComboSetup> categoryOptionCombos = new Objects<>(
        CategoryOptionComboSetup::getName );

    private final Objects<DataElementSetup> dataElements = new Objects<>( DataElementSetup::getName );

    private final Objects<PeriodSetup> periods = new Objects<>( PeriodSetup::getIsoPeriod );

    @RequiredArgsConstructor
    public static final class Objects<T> implements Iterable<T>
    {

        private final Function<T, String> toName;

        private Map<String, T> objectsByName;

        public void add( String name, T obj )
        {
            if ( objectsByName == null )
            {
                objectsByName = new LinkedHashMap<>();
            }
            objectsByName.put( name, obj );
        }

        public T get( String name )
        {
            T obj = objectsByName == null ? null : objectsByName.get( name );
            if ( obj == null )
            {
                throw new IllegalStateException( format( "No object with name %s exists", name ) );
            }
            return obj;
        }

        public boolean contains( String name )
        {
            return objectsByName != null && objectsByName.get( name ) != null;
        }

        public String nextUnique( T setup, Function<T, String> getter )
        {
            String value = getter.apply( setup );
            return value != null ? value : toName.apply( setup );
        }

        @Override
        public Iterator<T> iterator()
        {
            return objectsByName == null ? emptyIterator() : objectsByName.values().iterator();
        }
    }

    public AttributeSetup addAttribute( String name, Consumer<? super AttributeSetup> attribute )
    {
        return add( name, AttributeSetup::new, attributes, attribute );
    }

    public UserSetup addUser( String name, Consumer<? super UserSetup> user )
    {
        return add( name, UserSetup::new, users, user );
    }

    public UserGroupSetup addUserGroup( String name, Consumer<? super UserGroupSetup> group )
    {
        return add( name, UserGroupSetup::new, userGroups, group );
    }

    public UserRoleSetup addUserRole( String name, Consumer<? super UserRoleSetup> role )
    {
        return add( name, UserRoleSetup::new, roles, role );
    }

    public CategorySetup addCategory( String name, Consumer<? super CategorySetup> category )
    {
        return add( name, CategorySetup::new, categories, category );
    }

    public CategoryOptionSetup addCategoryOption( String name, Consumer<? super CategoryOptionSetup> categoryOption )
    {
        return add( name, CategoryOptionSetup::new, categoryOptions, categoryOption );
    }

    public CategoryComboSetup addCategoryCombo( String name, Consumer<? super CategoryComboSetup> categoryCombo )
    {
        return add( name, CategoryComboSetup::new, categoryCombos, categoryCombo );
    }

    public CategoryOptionComboSetup addCategoryOptionCombo( String name,
        Consumer<? super CategoryOptionComboSetup> categoryOptionCombo )
    {
        return add( name, CategoryOptionComboSetup::new, categoryOptionCombos, categoryOptionCombo );
    }

    public OrganisationUnitSetup addOrganisationUnit( String name,
        Consumer<? super OrganisationUnitSetup> organisationUnit )
    {
        return add( name, OrganisationUnitSetup::new, organisationUnits, organisationUnit );
    }

    public DataElementSetup addDataElement( String name, Consumer<? super DataElementSetup> dataElement )
    {
        return add( name, DataElementSetup::new, dataElements, dataElement );
    }

    public PeriodSetup addPeriod( String isoPeriod, Consumer<? super PeriodSetup> period )
    {
        return add( isoPeriod, PeriodSetup::new, periods, period );
    }

    public PeriodSetup addPeriod( int dayOfTheMonth, Consumer<? super PeriodSetup> period )
    {
        ZonedDateTime date = ZonedDateTime.now().withDayOfMonth( dayOfTheMonth ).truncatedTo( ChronoUnit.DAYS );
        return addPeriod( date.format( DateTimeFormatter.ofPattern( "yyyyMMdd" ) ), period );
    }

    private <T> T add( String name, BiFunction<MetadataSetup, String, T> newInstance, Objects<T> pool,
        Consumer<? super T> category )
    {
        if ( pool.contains( name ) )
        {
            throw new IllegalStateException( format( "A instance with name %s is already defined.", name ) );
        }
        T instance = newInstance.apply( this, name );
        category.accept( instance );
        pool.add( name, instance );
        return instance;
    }

    private static <T, E> T useItem( T self, String name, Objects<E> from, List<E> to )
    {
        to.add( from.get( name ) );
        return self;
    }

    private static <T, E, C extends Consumer<? super E>> T addItem( T self, String name, BiFunction<String, C, E> adder,
        C setup, List<E> to )
    {
        to.add( adder.apply( name, setup ) );
        return self;
    }

    @Getter
    @Setter
    @Accessors( chain = true )
    public abstract static class AbstractSetup<T extends IdentifiableObject>
    {
        private String uid;

        private T object;

        private String code;

        private String shortName;

        private final Set<AttributeSetup> attributes = new LinkedHashSet<>();

        private final Map<AttributeSetup, String> attributeValues = new LinkedHashMap<>();

        public abstract String getName();

        public abstract MetadataSetup getContext();

        public AbstractSetup<T> addAttribute( String name, Consumer<? super AttributeSetup> attribute )
        {
            attributes.add( getContext().addAttribute( name, attribute ) );
            return this;
        }

        public AbstractSetup<T> useAttribute( String name )
        {
            attributes.add( getContext().getAttributes().get( name ) );
            return this;
        }

        public AbstractSetup<T> addAttributeValue( String name, String value )
        {
            attributeValues.put( getContext().getAttributes().get( name ), value );
            return this;
        }

        public AbstractSetup<T> addAttributeValue( String name, String value, Consumer<AttributeSetup> attribute )
        {
            attributeValues.put( getContext().addAttribute( name, attribute ), value );
            return this;
        }

        @Override
        public final int hashCode()
        {
            return getClass().hashCode() ^ getName().hashCode();
        }

        @Override
        public final boolean equals( Object obj )
        {
            return obj != null && obj.getClass() == getClass()
                && getName().equals( ((AbstractSetup<?>) obj).getName() );
        }
    }

    @ToString
    @Getter
    @Setter
    @Accessors( chain = true )
    @RequiredArgsConstructor
    public static final class AttributeSetup extends AbstractSetup<Attribute>
    {
        private final MetadataSetup context;

        private final String name;

        private ValueType valueType;

        private EnumSet<Attribute.ObjectType> objectTypes = EnumSet.noneOf( Attribute.ObjectType.class );

        private boolean mandatory;

        private boolean unique;

        public AttributeSetup setTypes( Attribute.ObjectType... types )
        {
            return setObjectTypes( types.length == 0
                ? EnumSet.noneOf( Attribute.ObjectType.class )
                : EnumSet.of( types[0], types ) );
        }
    }

    @ToString
    @Getter
    @Setter
    @Accessors( chain = true )
    @RequiredArgsConstructor
    public static final class UserSetup extends AbstractSetup<User>
    {
        private final MetadataSetup context;

        private final String name;

        private String username;

        private String password;

        private String firstName;

        private String surname;

        private String email;

        private String phoneNumber;

        private final Set<OrganisationUnitSetup> organisationUnits = new LinkedHashSet<>();

        public UserSetup addRole( String name, Consumer<? super UserRoleSetup> role )
        {
            context.addUserRole( name, role ).getMembers().add( this );
            return this;
        }

        public UserSetup addRoles( String... names )
        {
            stream( names ).forEach( n -> context.getRoles().get( n ).getMembers().add( this ) );
            return this;
        }

        public UserSetup addGroup( String name, Consumer<? super UserGroupSetup> group )
        {
            context.addUserGroup( name, group ).getMembers().add( this );
            return this;
        }

        public UserSetup addGroups( String... names )
        {
            stream( names ).forEach( n -> context.getUserGroups().get( n ).getMembers().add( this ) );
            return this;
        }

        public UserSetup addOrganisationUnit( String name, Consumer<? super OrganisationUnitSetup> organisationUnit )
        {
            organisationUnits.add( context.addOrganisationUnit( name, organisationUnit ) );
            return this;
        }

        public UserSetup useOrganisationUnit( String name )
        {
            organisationUnits.add( context.getOrganisationUnits().get( name ) );
            return this;
        }
    }

    @ToString
    @Getter
    @Setter
    @Accessors( chain = true )
    @RequiredArgsConstructor
    public static final class UserGroupSetup extends AbstractSetup<UserGroup>
    {
        private final MetadataSetup context;

        private final String name;

        private UUID uuid;

        private final Set<UserSetup> members = new LinkedHashSet<>();

        private final Set<UserGroupSetup> managedGroups = new LinkedHashSet<>();

        private final Set<UserGroupSetup> managedByGroups = new LinkedHashSet<>();

        public UserGroupSetup addMember( String name, Consumer<UserSetup> user )
        {
            members.add( context.addUser( name, user ) );
            return this;
        }

        public UserGroupSetup addMembers( String... names )
        {
            stream( names ).forEach( n -> members.add( context.getUsers().get( n ) ) );
            return this;
        }

    }

    @ToString
    @Getter
    @Setter
    @Accessors( chain = true )
    @RequiredArgsConstructor
    public static final class UserRoleSetup extends AbstractSetup<UserRole>
    {
        private final MetadataSetup context;

        private final String name;

        private final Set<String> authorities = new LinkedHashSet<>();

        private final Set<UserSetup> members = new LinkedHashSet<>();

        public UserRoleSetup addMember( String name, Consumer<UserSetup> user )
        {
            members.add( context.addUser( name, user ) );
            return this;
        }

        public UserRoleSetup addMembers( String... names )
        {
            stream( names ).forEach( n -> members.add( context.getUsers().get( n ) ) );
            return this;
        }
    }

    @ToString
    @Getter
    @Setter
    @Accessors( chain = true )
    @RequiredArgsConstructor
    public static final class CategorySetup extends AbstractSetup<Category>
    {
        private final MetadataSetup context;

        private final String name;

        private DataDimensionType dataDimensionType = DataDimensionType.DISAGGREGATION;

        private final List<CategoryOptionSetup> options = new ArrayList<>();

        public CategorySetup addOption( String name, Consumer<? super CategoryOptionSetup> option )
        {
            return addItem( this, name, context::addCategoryOption, option, options );
        }

        public CategorySetup addOption( String name )
        {
            return addOption( name, o -> {
            } );
        }

        public CategorySetup useOption( String name )
        {
            return useItem( this, name, context.categoryOptions, options );
        }
    }

    @ToString
    @Getter
    @Setter
    @Accessors( chain = true )
    @RequiredArgsConstructor
    public static final class CategoryOptionSetup extends AbstractSetup<CategoryOption>
    {

        private final MetadataSetup context;

        private final String name;

        private Date startDate;

        private Date endDate;
    }

    @ToString
    @Getter
    @Setter
    @Accessors( chain = true )
    @RequiredArgsConstructor
    public static final class CategoryComboSetup extends AbstractSetup<CategoryCombo>
    {
        private final MetadataSetup context;

        private final String name;

        private final List<CategorySetup> categories = new ArrayList<>();

        private DataDimensionType dataDimensionType = DataDimensionType.DISAGGREGATION;

        public CategoryComboSetup addCategory( String name, Consumer<? super CategorySetup> category )
        {
            return addItem( this, name, context::addCategory, category, categories );
        }

        public CategoryComboSetup useCategory( String name )
        {
            return useItem( this, name, context.categories, categories );
        }
    }

    @ToString
    @Getter
    @Setter
    @Accessors( chain = true )
    @RequiredArgsConstructor
    public static final class CategoryOptionComboSetup extends AbstractSetup<CategoryOptionCombo>
    {
        private final MetadataSetup context;

        private final String name;

        private final List<CategoryOptionSetup> options = new ArrayList<>();

        @Setter( AccessLevel.NONE )
        private CategoryComboSetup combo;

        private boolean ignoreApproval = false;

        public CategoryOptionComboSetup addOption( String name, Consumer<? super CategoryOptionSetup> option )
        {
            return addItem( this, name, context::addCategoryOption, option, options );
        }

        public CategoryOptionComboSetup useOption( String name )
        {
            return useItem( this, name, context.categoryOptions, options );
        }

        public CategoryOptionComboSetup addCombo( String name, Consumer<? super CategoryComboSetup> combo )
        {
            this.combo = context.addCategoryCombo( name, combo );
            return this;
        }

        public CategoryOptionComboSetup useCombo( String name )
        {
            this.combo = context.categoryCombos.get( name );
            return this;
        }
    }

    @ToString
    @Getter
    @Setter
    @Accessors( chain = true )
    @RequiredArgsConstructor
    public static final class OrganisationUnitSetup extends AbstractSetup<OrganisationUnit>
    {
        private static final Date OPENING_DATE = Date.from(
            LocalDate.of( 1970, 1, 1 ).atStartOfDay().toInstant( ZoneOffset.UTC ) );

        private final MetadataSetup context;

        private final String name;

        private Date openingDate = OPENING_DATE;
    }

    @ToString
    @Getter
    @Setter
    @Accessors( chain = true )
    @RequiredArgsConstructor
    public static final class DataElementSetup extends AbstractSetup<DataElement>
    {
        private final MetadataSetup context;

        private final String name;

        private ValueType valueType = ValueType.INTEGER;

        private DataElementDomain domainType = DataElementDomain.AGGREGATE;

        private AggregationType aggregationType = AggregationType.SUM;

        private boolean zeroIsSignificant = false;

        @Setter( AccessLevel.NONE )
        private CategoryComboSetup categoryCombo;

        public DataElementSetup addCombo( String name, Consumer<? super CategoryComboSetup> combo )
        {
            this.categoryCombo = context.addCategoryCombo( name, combo );
            return this;
        }

        public DataElementSetup useCombo( String name )
        {
            this.categoryCombo = context.categoryCombos.get( name );
            return this;
        }
    }

    @ToString
    @Getter
    @Setter
    @Accessors( chain = true )
    @RequiredArgsConstructor
    public static final class PeriodSetup extends AbstractSetup<Period>
    {
        private final MetadataSetup context;

        private final String isoPeriod;

        @Override
        public String getName()
        {
            return getIsoPeriod();
        }
    }
}
