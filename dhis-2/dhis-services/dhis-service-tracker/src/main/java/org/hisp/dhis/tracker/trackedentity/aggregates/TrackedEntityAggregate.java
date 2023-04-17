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
package org.hisp.dhis.tracker.trackedentity.aggregates;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.hisp.dhis.tracker.trackedentity.aggregates.ThreadPoolManager.getPool;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.trackedentity.TrackedEntityParams;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * @author Luciano Fiandesio
 */
@Component
@RequiredArgsConstructor
public class TrackedEntityAggregate
    implements
    Aggregate
{
    @Nonnull
    private final TrackedEntityStore trackedEntityStore;

    @Qualifier( "org.hisp.dhis.tracker.trackedentity.aggregates.EnrollmentAggregate" )
    @Nonnull
    private final EnrollmentAggregate enrollmentAggregate;

    @Nonnull
    private final CurrentUserService currentUserService;

    @Qualifier( "org.hisp.dhis.tracker.trackedentity.aggregates.AclStore" )
    @Nonnull
    private final AclStore aclStore;

    @Nonnull
    private final TrackedEntityAttributeService trackedEntityAttributeService;

    @Nonnull
    private final CacheProvider cacheProvider;

    private Cache<Set<TrackedEntityAttribute>> teiAttributesCache;

    private Cache<Map<Program, Set<TrackedEntityAttribute>>> programTeiAttributesCache;

    private Cache<List<String>> userGroupUIDCache;

    private Cache<Context> securityCache;

    @PostConstruct
    protected void init()
    {
        teiAttributesCache = cacheProvider.createTeiAttributesCache();
        programTeiAttributesCache = cacheProvider.createProgramTeiAttributesCache();
        userGroupUIDCache = cacheProvider.createUserGroupUIDCache();
        securityCache = cacheProvider.createSecurityCache();
    }

    /**
     * Fetches a List of {@see TrackedEntityInstance} based on the list of
     * primary keys and search parameters
     *
     * @param ids a List of {@see TrackedEntityInstance} Primary Keys
     * @param params an instance of {@see TrackedEntityParams}
     *
     * @return a List of {@see TrackedEntityInstance} objects
     */
    public List<TrackedEntityInstance> find( List<Long> ids, TrackedEntityParams params,
        TrackedEntityInstanceQueryParams queryParams )
    {
        final Optional<User> user = Optional.ofNullable( currentUserService.getCurrentUser() );

        user.ifPresent( u -> {
            if ( userGroupUIDCache.get( user.get().getUid() ).isEmpty()
                && !CollectionUtils.isEmpty( user.get().getGroups() ) )
            {
                userGroupUIDCache.put( user.get().getUid(),
                    user.get().getGroups().stream().map( BaseIdentifiableObject::getUid )
                        .collect( Collectors.toList() ) );
            }
        } );

        /*
         * Create a context with information which will be used to fetch the
         * entities. Use a superUser context if the user is null.
         */
        Context ctx = user.map( u -> securityCache.get( u.getUid(),
            userUID -> getSecurityContext( userUID, userGroupUIDCache.get( userUID )
                .orElse( Lists.newArrayList() ) ) )
            .toBuilder()
            .userId( u.getId() )
            .userUid( u.getUid() )
            .userGroups( userGroupUIDCache.get( u.getUid() )
                .orElse( Collections.emptyList() ) )
            .superUser( u.isSuper() ) )
            .orElse( new Context.ContextBuilder()
                .superUser( true )
                .trackedEntityTypes( Collections.emptyList() )
                .programs( Collections.emptyList() )
                .programStages( Collections.emptyList() )
                .relationshipTypes( Collections.emptyList() ) )
            .params( params )
            .queryParams( queryParams )
            .build();

        /*
         * Async fetch Relationships for the given TrackedEntityInstance id
         * (only if isIncludeRelationships = true)
         */
        final CompletableFuture<Multimap<String, RelationshipItem>> relationshipsAsync = conditionalAsyncFetch(
            ctx.getParams().isIncludeRelationships(), () -> trackedEntityStore.getRelationships( ids, ctx ),
            getPool() );

        /*
         * Async fetch Enrollments for the given TrackedEntityInstance id (only
         * if isIncludeEnrollments = true)
         */
        final CompletableFuture<Multimap<String, ProgramInstance>> enrollmentsAsync = conditionalAsyncFetch(
            ctx.getParams().isIncludeEnrollments(),
            () -> enrollmentAggregate.findByTrackedEntityInstanceIds( ids, ctx ), getPool() );

        /*
         * Async fetch all ProgramOwner for the given TrackedEntityInstance id
         */
        final CompletableFuture<Multimap<String, TrackedEntityProgramOwner>> programOwnersAsync = conditionalAsyncFetch(
            ctx.getParams().isIncludeProgramOwners(), () -> trackedEntityStore.getProgramOwners( ids ),
            getPool() );

        /*
         * Async Fetch TrackedEntityInstances by id
         */
        final CompletableFuture<Map<String, TrackedEntityInstance>> teisAsync = supplyAsync(
            () -> trackedEntityStore.getTrackedEntityInstances( ids, ctx ), getPool() );

        /*
         * Async fetch TrackedEntityInstance Attributes by TrackedEntityInstance
         * id
         */
        final CompletableFuture<Multimap<String, TrackedEntityAttributeValue>> attributesAsync = supplyAsync(
            () -> trackedEntityStore.getAttributes( ids ), getPool() );

        /*
         * Async fetch Owned Tei mapped to the provided program attributes by
         * TrackedEntityInstance id
         */
        final CompletableFuture<Multimap<String, String>> ownedTeiAsync = conditionalAsyncFetch(
            user.isPresent(),
            () -> trackedEntityStore.getOwnedTeis( ids, ctx ), getPool() );

        /*
         * Execute all queries and merge the results
         */
        return allOf( teisAsync, attributesAsync, relationshipsAsync, enrollmentsAsync, ownedTeiAsync )
            .thenApplyAsync( fn -> {

                Map<String, TrackedEntityInstance> teis = teisAsync.join();

                Multimap<String, TrackedEntityAttributeValue> attributes = attributesAsync.join();
                Multimap<String, RelationshipItem> relationships = relationshipsAsync.join();
                Multimap<String, ProgramInstance> enrollments = enrollmentsAsync.join();
                Multimap<String, TrackedEntityProgramOwner> programOwners = programOwnersAsync.join();
                Multimap<String, String> ownedTeis = ownedTeiAsync.join();

                Stream<String> teiUidStream = teis.keySet().parallelStream();

                if ( user.isPresent() && queryParams.hasProgram() )
                {
                    teiUidStream = teiUidStream.filter( ownedTeis::containsKey );
                }

                return teiUidStream.map( uid -> {

                    TrackedEntityInstance tei = teis.get( uid );
                    tei.setTrackedEntityAttributeValues( filterAttributes( attributes.get( uid ), ownedTeis.get( uid ),
                        teiAttributesCache
                            .get( "ALL_ATTRIBUTES",
                                s -> trackedEntityAttributeService.getTrackedEntityAttributesByTrackedEntityTypes() ),
                        programTeiAttributesCache
                            .get( "ATTRIBUTES_BY_PROGRAM",
                                s -> trackedEntityAttributeService.getTrackedEntityAttributesByProgram() ),
                        ctx ) );
                    tei.setRelationshipItems( new HashSet<>( relationships.get( uid ) ) );
                    tei.setProgramInstances( filterEnrollments( enrollments.get( uid ), ownedTeis.get( uid ), ctx ) );
                    tei.setProgramOwners( new HashSet<>( programOwners.get( uid ) ) );
                    return tei;

                } ).collect( Collectors.toList() );
            }, getPool() ).join();

    }

    /**
     * Filter enrollments based on ownership and super user status.
     *
     */
    private Set<ProgramInstance> filterEnrollments( Collection<ProgramInstance> enrollments,
        Collection<String> programs,
        Context ctx )
    {
        Set<ProgramInstance> enrollmentList = new HashSet<>();

        if ( enrollments.isEmpty() )
        {
            return enrollmentList;
        }

        enrollmentList.addAll( enrollments.stream()
            .filter( e -> (programs.contains( e.getProgram().getUid() ) || ctx.isSuperUser()) )
            .collect( Collectors.toList() ) );

        return enrollmentList;
    }

    /**
     * Filter attributes based on queryParams, ownership and superuser status
     *
     */
    private Set<TrackedEntityAttributeValue> filterAttributes( Collection<TrackedEntityAttributeValue> attributes,
        Collection<String> programs,
        Set<TrackedEntityAttribute> trackedEntityTypeAttributes, Map<Program, Set<TrackedEntityAttribute>> teaByProgram,
        Context ctx )
    {
        Set<TrackedEntityAttributeValue> attributeList = new HashSet<>();

        // Nothing to filter from, return empty
        if ( attributes.isEmpty() )
        {
            return attributeList;
        }

        // Add all tet attributes
        Set<String> allowedAttributeUids = trackedEntityTypeAttributes.stream()
            .map( BaseIdentifiableObject::getUid )
            .collect( Collectors.toSet() );

        for ( Map.Entry<Program, Set<TrackedEntityAttribute>> entry : teaByProgram.entrySet() )
        {
            if ( programs.contains( entry.getKey().getUid() ) || ctx.isSuperUser() )
            {
                allowedAttributeUids.addAll( entry.getValue().stream()
                    .map( BaseIdentifiableObject::getUid )
                    .collect( Collectors.toSet() ) );
            }
        }

        for ( TrackedEntityAttributeValue attributeValue : attributes )
        {
            if ( allowedAttributeUids.contains( attributeValue.getAttribute().getUid() ) )
            {
                attributeList.add( attributeValue );
            }
        }

        return attributeList;
    }

    /**
     * Fetch security related information and add them to the {@see Context}
     *
     * - all Tracked Entity Instance Types this user has READ access to
     *
     * - all Programs Type this user has READ access to
     *
     * - all Program Stages Type this user has READ access to
     *
     * - all Relationship Types this user has READ access to
     *
     * @param userUID the user uid of a {@see User}
     *
     * @return an instance of {@see Context} populated with ACL-related info
     */
    private Context getSecurityContext( String userUID, List<String> userGroupUIDs )
    {
        final CompletableFuture<List<Long>> getTeiTypes = supplyAsync(
            () -> aclStore.getAccessibleTrackedEntityInstanceTypes( userUID, userGroupUIDs ), getPool() );

        final CompletableFuture<List<Long>> getPrograms = supplyAsync(
            () -> aclStore.getAccessiblePrograms( userUID, userGroupUIDs ),
            getPool() );

        final CompletableFuture<List<Long>> getProgramStages = supplyAsync(
            () -> aclStore.getAccessibleProgramStages( userUID, userGroupUIDs ), getPool() );

        final CompletableFuture<List<Long>> getRelationshipTypes = supplyAsync(
            () -> aclStore.getAccessibleRelationshipTypes( userUID, userGroupUIDs ), getPool() );

        return allOf( getTeiTypes, getPrograms, getProgramStages, getRelationshipTypes ).thenApplyAsync(
            fn -> Context.builder()
                .trackedEntityTypes( getTeiTypes.join() )
                .programs( getPrograms.join() )
                .programStages( getProgramStages.join() )
                .relationshipTypes( getRelationshipTypes.join() )
                .build(),
            getPool() )
            .join();
    }
}
