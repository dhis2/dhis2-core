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
import static java.util.Collections.emptyIterator;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

/**
 * A description of the metadata model that should be created.
 */
@Getter
public class MetadataSetup implements DataValueGenerator
{
    public static final Consumer<Object> withDefaultSetup = x -> {
    };

    private final Objects<CategorySetup> categories = new Objects<>( CategorySetup::getName );

    private final Objects<CategoryOptionSetup> categoryOptions = new Objects<>( CategoryOptionSetup::getName );

    private final Objects<CategoryComboSetup> categoryCombos = new Objects<>( CategoryComboSetup::getName );

    private final Objects<CategoryOptionComboSetup> categoryOptionCombos = new Objects<>(
        CategoryOptionComboSetup::getName );

    private final Objects<OrganisationUnitSetup> organisationUnits = new Objects<>( OrganisationUnitSetup::getName );

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
            return toName.apply( setup );
        }

        @Override
        public Iterator<T> iterator()
        {
            return objectsByName == null ? emptyIterator() : objectsByName.values().iterator();
        }
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

        @Setter( AccessLevel.NONE )
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

        @Setter( AccessLevel.NONE )
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

        @Setter( AccessLevel.NONE )
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
    }
}
