package org.hisp.dhis.validation;

/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.calendar.DateUnitType;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.validation.comparator.ValidationResultQuery;
import org.hisp.dhis.validation.hibernate.HibernateValidationResultStore;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Simple unit tests for {@link HibernateValidationResultStore} that mocks the
 * actual hibernate part to verify that the SQL query generated from a
 * {@link ValidationResultQuery} is reasonable.
 *
 * This test also verifies that other methods that use the same part of the
 * implementation are not affected by changes related to the
 * {@link ValidationResultQuery}.
 *
 * @author Jan Bernitt
 */
public class ValidationResultStoreHqlTest
{

    private final List<String> hqlQueries = new ArrayList<>();
    private final Map<String, Map<String, Object>> parametersByQueryAndName = new HashMap<>();

    private ValidationResultStore store;

    private CurrentUserService currentUserService;

    @Before
    public void setUp()
    {
        SessionFactory sessionFactory = mock( SessionFactory.class );
        JdbcTemplate jdbcTemplate = new JdbcTemplate();
        ApplicationEventPublisher publisher = mock( ApplicationEventPublisher.class );
        currentUserService = mock( CurrentUserService.class );
        store = new HibernateValidationResultStore( sessionFactory, jdbcTemplate, publisher, currentUserService );
        Session session = mock( Session.class );
        when( sessionFactory.getCurrentSession() ).thenReturn( session );
        when( session.createQuery( anyString() ) ).then( createQueryInvocation -> {
            String hql = createQueryInvocation.getArgument(0);
            hqlQueries.add(hql);
            @SuppressWarnings( "rawtypes" )
            Query query = mock( Query.class );
            when( query.setCacheable( anyBoolean() ) ).thenReturn( query );
            when( query.setHint( anyString(), any() ) ).thenReturn( query );
            when( query.setParameter( anyString(), any() ) ).then( setParameterInvocation -> {
                String parameter = setParameterInvocation.getArgument( 0 );
                Object value = setParameterInvocation.getArgument( 1 );
                parametersByQueryAndName.computeIfAbsent( hql, key -> new HashMap<>() ).put( parameter, value );
                return query;
            } );
            when( query.getResultList() ).thenReturn( emptyList() );
            when( query.getSingleResult() ).thenReturn( 0 );
            return query;
        } );
    }

    private void setUpUser(String orgUnitUid, Category category, CategoryOptionGroupSet groupSet )
    {
        User user = new User();
        when( currentUserService.getCurrentUser() ).thenReturn( user );
        user.setGroups( emptySet() );
        OrganisationUnit unit = new OrganisationUnit();
        unit.setUid( orgUnitUid );
        user.setDataViewOrganisationUnits( singleton( unit ) );
        UserCredentials credentials = new UserCredentials();
        user.setUserCredentials( credentials );

        // categories
        Set<Category> categories = category == null ? emptySet() : singleton(category);
        credentials.setCatDimensionConstraints( categories );

        // option groups
        Set<CategoryOptionGroupSet> options = groupSet == null ? emptySet() : singleton(groupSet);
        credentials.setCogsDimensionConstraints(options);
    }

    @Test
    public void getById()
    {
        store.getById( 13L );
        assertHQLMatches( "from ValidationResult vr where vr.id = :id" );
    }

    @Test
    public void getAllUnreportedValidationResults()
    {
        store.getAllUnreportedValidationResults();
        assertHQLMatches( "from ValidationResult vr where vr.notificationSent = false" );
    }

    @Test
    public void queryDefaultQuery()
    {
        store.query( new ValidationResultQuery() );
        assertHQLMatches( "from ValidationResult vr" );
    }

    @Test
    public void queryWithUser()
    {
        setUpUser( "uid", null, null );
        store.query( new ValidationResultQuery() );
        assertHQLMatches( "from ValidationResult vr where (locate('uid',vr.organisationUnit.path) <> 0)" );
    }

    @Test
    public void queryWithUserWithCategory()
    {
        Category category = new Category();
        category.setId(42L);
        setUpUser( "orgUid", category, null);
        store.query( new ValidationResultQuery() );
        assertHQLMatches( "from ValidationResult vr where (locate('orgUid',vr.organisationUnit.path) <> 0) and 1 = ...", 523 );
    }

    @Test
    public void queryWithUserWithCategoryOptionGroupSet()
    {
        CategoryOptionGroupSet groupSet = new CategoryOptionGroupSet();
        groupSet.setId(42L);
        setUpUser( "orgUid", null, groupSet);
        store.query( new ValidationResultQuery() );
        assertHQLMatches( "from ValidationResult vr where (locate('orgUid',vr.organisationUnit.path) <> 0) and 1 = ...", 544 );
    }

    @Test
    public void queryWithUserWithCategoryAndCategoryOptionGroupSet()
    {
        Category category = new Category();
        category.setId(42L);
        CategoryOptionGroupSet groupSet = new CategoryOptionGroupSet();
        groupSet.setId(42L);
        setUpUser( "orgUid", category, groupSet);
        store.query( new ValidationResultQuery() );
        assertHQLMatches( "from ValidationResult vr where (locate('orgUid',vr.organisationUnit.path) <> 0) and 1 = ...", 988 );
    }

    @Test
    public void queryWithOrgUnitFilter()
    {
        ValidationResultQuery query = new ValidationResultQuery();
        query.setOu(asList("uid1", "uid2"));
        store.query(query);
        assertHQLMatches("from ValidationResult vr where vr.organisationUnit.uid in :orgUnitsUids ");
        assertHQLParameter("orgUnitsUids", asList("uid1", "uid2"));
        assertHQLParameterCount( 1 );
    }

    @Test
    public void queryWithValidationRuleFilter()
    {
        ValidationResultQuery query = new ValidationResultQuery();
        query.setVr(asList("uid1", "uid2"));
        store.query(query);
        assertHQLMatches("from ValidationResult vr where vr.validationRule.uid in :validationRulesUids ");
        assertHQLParameter("validationRulesUids", asList("uid1", "uid2"));
        assertHQLParameterCount( 1 );
    }

    @Test
    public void queryWithOrgUnitAndValidationRuleFilter()
    {
        ValidationResultQuery query = new ValidationResultQuery();
        query.setOu(asList("uid1", "uid2"));
        query.setVr(asList("uid3", "uid4"));
        store.query(query);
        assertHQLMatches("from ValidationResult vr where vr.organisationUnit.uid in :orgUnitsUids  and vr.validationRule.uid in :validationRulesUids ");
        assertHQLParameter("orgUnitsUids", asList("uid1", "uid2"));
        assertHQLParameter("validationRulesUids", asList("uid3", "uid4"));
        assertHQLParameterCount( 2 );
    }

    @Test
    public void queryWithIsoPeriodFilter()
    {
        ValidationResultQuery query = new ValidationResultQuery();
        query.setPe( singletonList( "2017Q1" ) );
        store.query( query );
        assertHQLMatches( "from ValidationResult vr where( ((vr.period.startDate <= :periodId1End ) and (vr.period.endDate >= :periodId1Start )))" );
        PeriodType quarterly = PeriodType.getByNameIgnoreCase( DateUnitType.QUARTERLY.getName() );
        Period q1_2017 = quarterly.createPeriod( "2017Q1" );
        assertNotNull( q1_2017 );
        assertHQLParameter( "periodId1Start", q1_2017.getStartDate() );
        assertHQLParameter( "periodId1End", q1_2017.getEndDate() );
        assertHQLParameterCount( 2 );
    }

    private void assertHQLMatches(String expected )
    {
        assertHQLMatches( expected, -1 );
    }

    private void assertHQLMatches(String expected, int expectedLength )
    {
        assertEquals( 1, hqlQueries.size() );
        String actual = hqlQueries.get( 0 );
        if ( expected.endsWith( "..." ) )
        {
            int len = expected.length() - 3;
            assertEquals( expected.substring( 0, len ), actual.substring( 0, len ) );
            if ( expectedLength >= 0 )
            {
                assertEquals( expectedLength, actual.length() );
            }
        }
        else
        {
            assertEquals( expected, actual );
        }
    }

    private void assertHQLParameterCount( int expected )
    {
        String hql = hqlQueries.get( hqlQueries.size() - 1 );
        assertEquals( expected, parametersByQueryAndName.get( hql ).size() );
    }

    private void assertHQLParameter( String parameterName, Object expectedValue )
    {
        String hql = hqlQueries.get( hqlQueries.size() - 1 );
        Map<String, Object> parameters = parametersByQueryAndName.get( hql );
        assertNotNull( "No parameters were set", parameters );
        assertTrue( "No parameter of name " + parameterName + " was set", parameters.containsKey( parameterName ) );
        assertEquals( "Unexpected parameter value: ", expectedValue, parameters.get( parameterName ) );
    }

}
