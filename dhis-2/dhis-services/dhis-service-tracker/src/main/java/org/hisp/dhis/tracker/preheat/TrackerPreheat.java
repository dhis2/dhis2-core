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
package org.hisp.dhis.tracker.preheat;

import static org.hisp.dhis.tracker.preheat.RelationshipPreheatKeySupport.getRelationshipKey;
import static org.hisp.dhis.tracker.preheat.RelationshipPreheatKeySupport.hasRelationshipKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipKey;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerOrgUnit;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.user.User;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.scalified.tree.TreeNode;
import com.scalified.tree.multinode.ArrayMultiTreeNode;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class TrackerPreheat
{
    /**
     * User to use for import job (important for threaded imports).
     */
    @Getter
    @Setter
    private User user;

    /**
     * Internal map of all metadata objects mapped by class type => [id] The
     * value of each id can be either the metadata object's uid, code, name or
     * attribute value
     */
    private final Map<Class<? extends IdentifiableObject>, Map<String, IdentifiableObject>> map = new HashMap<>();

    /**
     * List of all payload references by tracker type which are not present in
     * the database. This will be used to create the reference tree that
     * represents the hierarchical structure of the references.
     */
    private final ArrayListMultimap<TrackerType, ReferenceTrackerEntity> referenceTrackerEntities = ArrayListMultimap
        .create();

    /**
     * Internal tree of all payload references which are not present in the
     * database. This map is required to allow the validation stage to reference
     * root objects (TEI, PS, PSI) which are present in the payload but not
     * stored in the pre-heat object (since they do not exist in the db yet).
     */
    private final TreeNode<String> referenceTree = new ArrayMultiTreeNode<>( "ROOT" );

    /**
     * Internal map of all default object (like category option combo, etc).
     */
    private final Map<Class<? extends IdentifiableObject>, IdentifiableObject> defaults = new HashMap<>();

    /**
     * All periods available.
     */
    @Getter
    private final Map<String, Period> periodMap = new HashMap<>();

    /**
     * All periodTypes available.
     */
    @Getter
    private final Map<String, PeriodType> periodTypeMap = new HashMap<>();

    /**
     * Internal map of category combo + category options (key) to category
     * option combo (value).
     *
     * Category option combo value will be in the idScheme defined by the user
     * on import.
     */
    private final Map<Pair<String, Set<String>>, String> cosToCOC = new HashMap<>();

    /**
     * Store mapping of category combo + category options identifiers(key) to
     * category option combo identifiers (value).
     *
     * Category options, category option combo identifiers will be in the
     * idScheme defined by the user on import. Note: different idSchemes for
     * category combos are not supported.
     */
    public void putCategoryOptionCombo( CategoryCombo categoryCombo, Set<CategoryOption> categoryOptions,
        CategoryOptionCombo categoryOptionCombo )
    {
        if ( categoryOptionCombo != null )
        {
            TrackerIdSchemeParam optionComboIdScheme = this.getIdSchemes().getCategoryOptionComboIdScheme();
            this.cosToCOC.put( categoryOptionComboCacheKey( categoryCombo, categoryOptions ),
                optionComboIdScheme.getIdentifier( categoryOptionCombo ) );
            this.put( categoryOptionCombo );
        }
        else
        {
            this.cosToCOC.put( categoryOptionComboCacheKey( categoryCombo, categoryOptions ), null );
        }
    }

    private Pair<String, Set<String>> categoryOptionComboCacheKey( CategoryCombo categoryCombo,
        Set<CategoryOption> categoryOptions )
    {
        Set<String> coIds = categoryOptions.stream()
            .map( this.getIdSchemes().getCategoryOptionIdScheme()::getIdentifier )
            .collect( Collectors.toSet() );
        return Pair.of( categoryCombo.getUid(), coIds );
    }

    /**
     * Check if a category option combo for given category combo and category
     * options has been stored using {@link #putCategoryOptionCombo}. Returns
     * true if null and a non-null category option combo have been stored.
     *
     * @param categoryCombo category combo
     * @param categoryOptions category options
     * @return true if category option combo has been stored given both
     *         arguments
     */
    public boolean containsCategoryOptionCombo( CategoryCombo categoryCombo, Set<CategoryOption> categoryOptions )
    {
        return this.cosToCOC.containsKey( categoryOptionComboCacheKey( categoryCombo, categoryOptions ) );
    }

    /**
     * Get the category option combo for given category combo and category
     * options. For the category option combo to exist it has to be stored
     * before using {@link #putCategoryOptionCombo}.
     *
     * @param categoryCombo category combo
     * @param categoryOptions category options
     * @return category option combo identifier
     */
    public CategoryOptionCombo getCategoryOptionCombo( CategoryCombo categoryCombo,
        Set<CategoryOption> categoryOptions )
    {
        return this.getCategoryOptionCombo(
            cosToCOC.get( categoryOptionComboCacheKey( categoryCombo, categoryOptions ) ) );
    }

    /**
     * Get the identifier of a category option combo for given category combo
     * and semicolon separated list of category options. For the category option
     * combo to exist it has to be stored before using
     * {@link #putCategoryOptionCombo}.
     *
     * Category option identifiers needs to match the idScheme used when storing
     * the category option combo using {@link #putCategoryOptionCombo}. Category
     * option combo identifier will be in the idScheme defined by the user on
     * import.
     *
     * @param categoryCombo category combo
     * @param categoryOptions semicolon separated list of category options
     * @return category option combo identifier
     */
    public MetadataIdentifier getCategoryOptionComboIdentifier( CategoryCombo categoryCombo, String categoryOptions )
    {
        CategoryOptionCombo categoryOptionCombo = this.getCategoryOptionCombo(
            this.cosToCOC.get( categoryOptionComboCacheKey( categoryCombo, categoryOptions ) ) );
        if ( categoryOptionCombo == null )
        {
            return idSchemes.toMetadataIdentifier( (CategoryOptionCombo) null );
        }
        return idSchemes.toMetadataIdentifier( categoryOptionCombo );
    }

    private Pair<String, Set<String>> categoryOptionComboCacheKey( CategoryCombo categoryCombo,
        String categoryOptions )
    {
        return Pair.of( categoryCombo.getUid(), parseCategoryOptions( categoryOptions ) );
    }

    private Set<String> parseCategoryOptions( String categoryOptions )
    {
        String cos = StringUtils.strip( categoryOptions );
        if ( StringUtils.isBlank( cos ) )
        {
            return Collections.emptySet();
        }

        return TextUtils.splitToSet( cos, TextUtils.SEMICOLON );
    }

    /**
     * Internal map of all preheated tracked entities, mainly used for
     * confirming existence for updates, and used for object merging.
     */
    @Getter
    private final Map<String, TrackedEntityInstance> trackedEntities = new HashMap<>();

    /**
     * Internal map of all preheated enrollments, mainly used for confirming
     * existence for updates, and used for object merging.
     */
    @Getter
    private final Map<String, ProgramInstance> enrollments = new HashMap<>();

    /**
     * Internal map of all preheated events, mainly used for confirming
     * existence for updates, and used for object merging.
     */
    @Getter
    private final Map<String, ProgramStageInstance> events = new HashMap<>();

    /**
     * Internal map of all preheated relationships, mainly used for confirming
     * existence for updates, and used for object merging.
     */
    @Getter
    private final Map<String, Relationship> relationships = new HashMap<>();

    /**
     * Internal map of all preheated notes (events and enrollments)
     */
    private final Map<String, TrackedEntityComment> notes = new HashMap<>();

    /**
     * Internal map of all existing TrackedEntityProgramOwner. Used for
     * ownership validations and updating. The root key of this map is the
     * trackedEntityInstance UID. The value of the root map is another map which
     * holds a key-value combination where the key is the program UID and the
     * value is an object of {@link TrackedEntityProgramOwnerOrgUnit} holding
     * the ownership OrganisationUnit
     */
    @Getter
    private final Map<String, Map<String, TrackedEntityProgramOwnerOrgUnit>> programOwner = new HashMap<>();

    /**
     * A Map of trackedEntity uid connected to Program Instances
     */
    @Getter
    @Setter
    private Map<String, List<ProgramInstance>> trackedEntityToProgramInstanceMap = new HashMap<>();

    /**
     * A Map of program uid and without registration {@see ProgramInstance}.
     */
    private final Map<String, ProgramInstance> programInstancesWithoutRegistration = new HashMap<>();

    /**
     * A map of valid users by username that are present in the payload. A user
     * not available in this cache means, payload's username or uid is invalid.
     * These users are primarily used to represent the ValueType.USERNAME of
     * tracked entity attributes and assignedUser fields in events used in
     * validation and persistence.
     */
    private final Map<String, User> users = Maps.newHashMap();

    /**
     * A list of all unique attribute values that are both present in the
     * payload and in the database. This is going to be used to validate the
     * uniqueness of attribute values in the Validation phase.
     */
    @Getter
    @Setter
    private List<UniqueAttributeValue> uniqueAttributeValues = Lists.newArrayList();

    /**
     * A list of all Program Instance UID having at least one Event that is not
     * deleted.
     */
    @Getter
    @Setter
    private List<String> programInstanceWithOneOrMoreNonDeletedEvent = Lists.newArrayList();

    /**
     * A list of Program Stage UID having 1 or more Events
     */
    private List<Pair<String, String>> programStageWithEvents = Lists.newArrayList();

    /**
     * idScheme map
     */
    @Getter
    @Setter
    private TrackerIdSchemeParams idSchemes = new TrackerIdSchemeParams();

    /**
     * Map of Program ID (primary key) and List of Org Unit ID associated to
     * each program. Note that the List only contains the Org Unit ID of the Org
     * Units that are specified in the import payload.
     */
    @Getter
    @Setter
    private Map<String, List<String>> programWithOrgUnitsMap;

    public TrackerPreheat()
    {
    }

    public String getUsername()
    {
        return User.username( user );
    }

    /**
     * Put a default metadata value (i.e. CategoryOption "default") into the
     * preheat.
     *
     * @param defaultClass class of the default metadata
     * @param metadata the default metadata
     * @return the tracker preheat
     */
    public <T extends IdentifiableObject> TrackerPreheat putDefault( Class<T> defaultClass, T metadata )
    {
        if ( metadata == null )
        {
            return this;
        }

        defaults.put( defaultClass, metadata );

        return this;
    }

    /**
     * Get a default value from the preheat
     *
     * @param defaultClass The type of object to retrieve
     * @return The default object of the class provided
     */
    public <T extends IdentifiableObject> T getDefault( Class<T> defaultClass )
    {
        return (T) this.defaults.get( defaultClass );
    }

    /**
     * Fetch a metadata object from the pre-heat, based on the type of the
     * object and the cached identifier.
     *
     * @param klass The metadata class to fetch
     * @param id metadata identifier
     * @return A metadata object or null
     */
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> T get( Class<? extends IdentifiableObject> klass, MetadataIdentifier id )
    {
        if ( id == null )
        {
            return null;
        }
        if ( id.getIdScheme() == TrackerIdScheme.ATTRIBUTE )
        {
            return this.get( klass, id.getAttributeValue() );
        }
        return this.get( klass, id.getIdentifier() );
    }

    /**
     * Fetch a metadata object from the pre-heat, based on the type of the
     * object and the cached identifier.
     *
     * @param klass The metadata class to fetch
     * @param key The key used during the pre-heat creation
     * @return A metadata object or null
     */
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> T get( Class<? extends IdentifiableObject> klass,
        String key )
    {
        return (T) map.getOrDefault( klass, new HashMap<>() ).get( key );
    }

    public CategoryOption getCategoryOption( String id )
    {
        return get( CategoryOption.class, id );
    }

    public CategoryOptionCombo getCategoryOptionCombo( MetadataIdentifier id )
    {
        return get( CategoryOptionCombo.class, id );
    }

    public CategoryOptionCombo getCategoryOptionCombo( String id )
    {
        return get( CategoryOptionCombo.class, id );
    }

    /**
     * Fetch all the metadata objects from the pre-heat, by object type
     *
     * @param klass The metadata class to fetch
     * @return a List of pre-heated object or empty list
     */
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getAll( Class<T> klass )
    {
        return new ArrayList<>( (Collection<? extends T>) map.getOrDefault( klass, new HashMap<>() ).values() );
    }

    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> TrackerPreheat put( TrackerIdSchemeParam idSchemeParam, T object )
    {
        if ( object == null )
        {
            return this;
        }

        Class<? extends IdentifiableObject> klass = HibernateProxyUtils.getRealClass( object );

        map.computeIfAbsent( klass, k -> new HashMap<>() );

        if ( User.class.isAssignableFrom( klass ) )
        {
            User userObject = (User) object;

            Map<String, IdentifiableObject> identifierMap = map.get( User.class );

            if ( !StringUtils.isEmpty( idSchemeParam.getIdentifier( userObject ) ) &&
                !identifierMap.containsKey( idSchemeParam.getIdentifier( userObject ) ) )
            {
                identifierMap.put( idSchemeParam.getIdentifier( userObject ), userObject );
            }
        }

        Optional.ofNullable( idSchemeParam.getIdentifier( object ) )
            .ifPresent( k -> map.get( klass ).put( k, object ) );

        return this;
    }

    public <T extends IdentifiableObject> TrackerPreheat put( TrackerIdSchemeParam idSchemeParam,
        Collection<T> objects )
    {
        for ( T object : objects )
        {
            put( idSchemeParam, object );
        }

        return this;
    }

    public TrackerPreheat put( DataElement dataElement )
    {
        return this.put( idSchemes.getDataElementIdScheme(), dataElement );
    }

    public TrackerPreheat put( Program program )
    {
        return this.put( idSchemes.getProgramIdScheme(), program );
    }

    public TrackerPreheat put( ProgramStage programStage )
    {
        return this.put( idSchemes.getProgramStageIdScheme(), programStage );
    }

    public TrackerPreheat put( CategoryOptionCombo categoryOptionCombo )
    {
        return this.put( idSchemes.getCategoryOptionComboIdScheme(), categoryOptionCombo );
    }

    public TrackedEntityInstance getTrackedEntity( String uid )
    {
        return trackedEntities.get( uid );
    }

    public void putTrackedEntities( List<TrackedEntityInstance> trackedEntityInstances, List<String> allEntities )
    {
        putTrackedEntities( trackedEntityInstances );

        List<String> uidOnDB = trackedEntityInstances.stream()
            .map( BaseIdentifiableObject::getUid )
            .collect( Collectors.toList() );

        allEntities
            .stream()
            .filter( t -> !uidOnDB.contains( t ) )
            .map( t -> new ReferenceTrackerEntity( t, null ) )
            .forEach( u -> this.addReference( TrackerType.TRACKED_ENTITY, u ) );
    }

    public void putTrackedEntities( List<TrackedEntityInstance> trackedEntityInstances )
    {

        trackedEntityInstances.forEach( te -> putTrackedEntity( te.getUid(), te ) );
    }

    private void putTrackedEntity( String uid, TrackedEntityInstance trackedEntityInstance )
    {
        trackedEntities.put( uid, trackedEntityInstance );
    }

    public ProgramInstance getEnrollment( String uid )
    {
        return enrollments.get( uid );
    }

    public void putEnrollments( List<ProgramInstance> programInstances, List<Enrollment> allEntities )
    {
        putEnrollments( programInstances );
        List<String> uidOnDB = programInstances.stream().map( BaseIdentifiableObject::getUid )
            .collect( Collectors.toList() );

        allEntities
            .stream()
            .filter( t -> !uidOnDB.contains( t.getEnrollment() ) )
            .map( t -> new ReferenceTrackerEntity( t.getEnrollment(), t.getTrackedEntity() ) )
            .forEach( pi -> this.addReference( TrackerType.ENROLLMENT, pi ) );
    }

    public void putEnrollments( List<ProgramInstance> programInstances )
    {
        programInstances.forEach( pi -> putEnrollment( pi.getUid(), pi ) );
    }

    public void putEnrollment( String uid, ProgramInstance programInstance )
    {
        enrollments.put( uid, programInstance );
    }

    public ProgramStageInstance getEvent( String uid )
    {
        return events.get( uid );
    }

    public void putEvents( List<ProgramStageInstance> programStageInstances, List<Event> allEntities )
    {
        putEvents( programStageInstances );

        List<String> uidOnDB = programStageInstances.stream().map( BaseIdentifiableObject::getUid )
            .collect( Collectors.toList() );

        allEntities
            .stream()
            .filter( t -> !uidOnDB.contains( t.getEvent() ) )
            .map( t -> new ReferenceTrackerEntity( t.getEvent(), t.getEnrollment() ) )
            .forEach( psi -> this.addReference( TrackerType.EVENT, psi ) );
    }

    public void putEvents( List<ProgramStageInstance> programStageInstances )
    {
        programStageInstances.forEach( psi -> putEvent( psi.getUid(), psi ) );
    }

    public void putEvent( String uid, ProgramStageInstance programStageInstance )
    {
        events.put( uid, programStageInstance );
    }

    public void putNotes( List<TrackedEntityComment> trackedEntityComments )
    {
        trackedEntityComments.forEach( c -> putNote( c.getUid(), c ) );
    }

    public void putNote( String uid, TrackedEntityComment comment )
    {
        notes.put( uid, comment );
    }

    public Optional<TrackedEntityComment> getNote( String uid )
    {
        return Optional.ofNullable( notes.get( uid ) );
    }

    public Relationship getRelationship( String relationshipUid )
    {
        return relationships.get( relationshipUid );
    }

    public Relationship getRelationshipByKey( org.hisp.dhis.tracker.domain.Relationship relationship )
    {
        RelationshipType relationshipType = get( RelationshipType.class, relationship.getRelationshipType() );

        if ( hasRelationshipKey( relationship ) )
        {
            RelationshipKey relationshipKey = getRelationshipKey( relationship );

            RelationshipKey inverseKey = null;
            if ( relationshipType.isBidirectional() )
            {
                inverseKey = relationshipKey.inverseKey();
            }
            return Stream.of( relationshipKey, inverseKey )
                .filter( Objects::nonNull )
                .map( key -> relationships.get( key.asString() ) )
                .filter( Objects::nonNull )
                .findFirst()
                .orElse( null );
        }
        return null;
    }

    public void putRelationships( List<Relationship> relationships )
    {
        relationships.forEach( this::putRelationship );
    }

    public void putRelationship( Relationship relationship )
    {
        if ( Objects.nonNull( relationship ) )
        {
            relationships.put( relationship.getUid(), relationship );
        }
    }

    public void putRelationshipsByKey( List<Relationship> relationships )
    {
        relationships.forEach( this::putRelationshipByKey );
    }

    public void putRelationshipByKey( Relationship relationship )
    {
        if ( Objects.nonNull( relationship ) )
        {
            RelationshipKey relationshipKey = getRelationshipKey( relationship );

            if ( relationship.getRelationshipType().isBidirectional() )
            {
                relationships.put( relationshipKey.inverseKey().asString(), relationship );
            }

            relationships.put( relationshipKey.asString(), relationship );
        }
    }

    public ProgramInstance getProgramInstancesWithoutRegistration( String programUid )
    {
        return programInstancesWithoutRegistration.get( programUid );
    }

    public void putProgramInstancesWithoutRegistration( String programUid, ProgramInstance programInstance )
    {
        this.programInstancesWithoutRegistration.put( programUid, programInstance );
    }

    public void createReferenceTree()
    {
        referenceTrackerEntities.get( TrackerType.TRACKED_ENTITY )
            .forEach( r -> referenceTree.add( new ArrayMultiTreeNode<>( r.getUid() ) ) );

        referenceTrackerEntities.get( TrackerType.ENROLLMENT )
            .forEach( this::addElementInReferenceTree );

        referenceTrackerEntities.get( TrackerType.EVENT )
            .forEach( this::addElementInReferenceTree );
    }

    private void addReference( TrackerType trackerType, ReferenceTrackerEntity referenceTrackerEntity )
    {
        referenceTrackerEntities.put( trackerType, referenceTrackerEntity );
    }

    private void addElementInReferenceTree( ReferenceTrackerEntity referenceTrackerEntity )
    {
        final TreeNode<String> node = referenceTree.find( referenceTrackerEntity.getParentUid() );

        if ( node != null )
        {
            node.add( new ArrayMultiTreeNode<>( referenceTrackerEntity.getUid() ) );
        }
        else
        {
            referenceTree.add( new ArrayMultiTreeNode<>( referenceTrackerEntity.getUid() ) );
        }
    }

    public Optional<ReferenceTrackerEntity> getReference( String uid )
    {
        final TreeNode<String> node = referenceTree.find( uid );
        if ( node != null )
        {
            return Optional.of( new ReferenceTrackerEntity( uid, node.parent().data() ) );
        }
        return Optional.empty();
    }

    public void addProgramOwners( List<TrackedEntityProgramOwnerOrgUnit> tepos )
    {
        tepos.forEach( tepo -> addProgramOwner( tepo.getTrackedEntityInstanceId(), tepo.getProgramId(), tepo ) );

    }

    private void addProgramOwner( String teiUid, String programUid,
        TrackedEntityProgramOwnerOrgUnit tepo )
    {
        programOwner
            .computeIfAbsent( teiUid, k -> new HashMap<>() )
            .put( programUid, tepo );
    }

    public void addProgramOwner( String teiUid, String programUid,
        OrganisationUnit orgUnit )
    {
        programOwner.computeIfAbsent( teiUid, k -> new HashMap<>() );
        if ( !programOwner.get( teiUid ).containsKey( programUid ) )
        {
            TrackedEntityProgramOwnerOrgUnit tepo = new TrackedEntityProgramOwnerOrgUnit( teiUid, programUid,
                orgUnit );
            programOwner.get( teiUid ).put( programUid, tepo );
        }
    }

    public void addUsers( Set<User> users )
    {
        Map<String, User> userMap = users.stream()
            .filter( Objects::nonNull )
            .collect( Collectors.toMap( User::getUsername, Function.identity() ) );
        this.users.putAll( userMap );
    }

    public Optional<User> getUserByUsername( String username )
    {
        return Optional.ofNullable( this.users.get( username ) );
    }

    public Optional<User> getUserByUid( String uid )
    {
        return this.users.values()
            .stream()
            .filter( u -> Objects.equals( uid, u.getUid() ) )
            .findAny();
    }

    public OrganisationUnit getOrganisationUnit( MetadataIdentifier id )
    {
        return get( OrganisationUnit.class, id );
    }

    public OrganisationUnit getOrganisationUnit( String id )
    {
        return get( OrganisationUnit.class, id );
    }

    public ProgramStage getProgramStage( MetadataIdentifier id )
    {
        return get( ProgramStage.class, id );
    }

    public ProgramStage getProgramStage( String id )
    {
        return get( ProgramStage.class, id );
    }

    public Program getProgram( MetadataIdentifier id )
    {
        return get( Program.class, id );
    }

    public Program getProgram( String id )
    {
        return get( Program.class, id );
    }

    public TrackedEntityType getTrackedEntityType( String id )
    {
        return get( TrackedEntityType.class, id );
    }

    public TrackedEntityAttribute getTrackedEntityAttribute( String id )
    {
        return get( TrackedEntityAttribute.class, id );
    }

    public TrackerPreheat addProgramStageWithEvents( String programStageUid, String enrollmentUid )
    {
        this.programStageWithEvents.add( Pair.of( programStageUid, enrollmentUid ) );
        return this;
    }

    public boolean hasProgramStageWithEvents( MetadataIdentifier programStage, String enrollmentUid )
    {
        ProgramStage ps = this.getProgramStage( programStage );
        ProgramInstance pi = this.getEnrollment( enrollmentUid );
        return this.programStageWithEvents.contains( Pair.of( ps.getUid(), pi.getUid() ) );
    }

    @Override
    public String toString()
    {
        return new StringJoiner( ", ", TrackerPreheat.class.getSimpleName() + "[", "]" )
            .add( "map=" + map )
            .toString();
    }
}
