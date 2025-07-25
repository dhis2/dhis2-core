/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.tracker.imports.preheat;

import static org.hisp.dhis.tracker.imports.util.RelationshipKeySupport.getRelationshipKey;
import static org.hisp.dhis.tracker.imports.util.RelationshipKeySupport.hasRelationshipKey;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipKey;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerOrgUnit;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerDto;
import org.hisp.dhis.user.User;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class TrackerPreheat {
  /**
   * Internal map of all metadata objects mapped by class type => [id] The value of each id can be
   * either the metadata object's uid, code, name or attribute value
   */
  private final Map<Class<? extends IdentifiableObject>, Map<String, IdentifiableObject>> map =
      new HashMap<>();

  /** Internal map of all default object (like category option combo, etc). */
  private final Map<Class<? extends IdentifiableObject>, IdentifiableObject> defaults =
      new HashMap<>();

  /**
   * Internal map of category combo + category options (key) to category option combo (value).
   *
   * <p>Category option combo value will be in the idScheme defined by the user on import.
   */
  private final Map<Pair<String, Set<MetadataIdentifier>>, MetadataIdentifier> cosToCOC =
      new HashMap<>();

  /**
   * Store mapping of category combo + category options identifiers(key) to category option combo
   * identifiers (value).
   *
   * <p>Category options, category option combo identifiers will be in the idScheme defined by the
   * user on import. Note: different idSchemes for category combos are not supported.
   */
  public void putCategoryOptionCombo(
      CategoryCombo categoryCombo,
      Set<CategoryOption> categoryOptions,
      CategoryOptionCombo categoryOptionCombo) {
    if (categoryOptionCombo == null) {
      this.cosToCOC.put(categoryOptionComboCacheKey(categoryCombo, categoryOptions), null);
    }

    this.cosToCOC.put(
        categoryOptionComboCacheKey(categoryCombo, categoryOptions),
        this.getIdSchemes().toMetadataIdentifier(categoryOptionCombo));
    this.put(categoryOptionCombo);
  }

  private Pair<String, Set<MetadataIdentifier>> categoryOptionComboCacheKey(
      CategoryCombo categoryCombo, Set<CategoryOption> categoryOptions) {
    Set<MetadataIdentifier> coIds =
        categoryOptions.stream()
            .map(co -> this.getIdSchemes().toMetadataIdentifier(co))
            .collect(Collectors.toSet());
    return toCategoryOptionComboCacheKey(categoryCombo, coIds);
  }

  private Pair<String, Set<MetadataIdentifier>> toCategoryOptionComboCacheKey(
      CategoryCombo categoryCombo, Set<MetadataIdentifier> categoryOptions) {
    return Pair.of(categoryCombo.getUid(), categoryOptions);
  }

  /**
   * Check if a category option combo for given category combo and category options has been stored
   * using {@link #putCategoryOptionCombo}. Returns true if null and a non-null category option
   * combo have been stored.
   *
   * @param categoryCombo category combo
   * @param categoryOptions category options
   * @return true if category option combo has been stored given both arguments
   */
  public boolean containsCategoryOptionCombo(
      CategoryCombo categoryCombo, Set<CategoryOption> categoryOptions) {
    return this.cosToCOC.containsKey(categoryOptionComboCacheKey(categoryCombo, categoryOptions));
  }

  /**
   * Get the category option combo for given category combo and category options. For the category
   * option combo to exist it has to be stored before using {@link #putCategoryOptionCombo}.
   *
   * @param categoryCombo category combo
   * @param categoryOptions category options
   * @return category option combo identifier
   */
  public CategoryOptionCombo getCategoryOptionCombo(
      CategoryCombo categoryCombo, Set<CategoryOption> categoryOptions) {
    return this.getCategoryOptionCombo(
        cosToCOC.get(categoryOptionComboCacheKey(categoryCombo, categoryOptions)));
  }

  /**
   * Get the identifier of a category option combo for given category combo and category options.
   * For the category option combo to exist it has to be stored before using {@link
   * #putCategoryOptionCombo}.
   *
   * <p>Category option identifiers needs to match the idScheme used when storing the category
   * option combo using {@link #putCategoryOptionCombo}. Category option combo identifier will be in
   * the idScheme defined by the user on import.
   *
   * @param categoryCombo category combo
   * @param categoryOptions category options
   * @return category option combo identifier
   */
  public MetadataIdentifier getCategoryOptionComboIdentifier(
      CategoryCombo categoryCombo, Set<MetadataIdentifier> categoryOptions) {
    CategoryOptionCombo categoryOptionCombo =
        this.getCategoryOptionCombo(
            this.cosToCOC.get(toCategoryOptionComboCacheKey(categoryCombo, categoryOptions)));
    if (categoryOptionCombo == null) {
      return idSchemes.toMetadataIdentifier((CategoryOptionCombo) null);
    }
    return idSchemes.toMetadataIdentifier(categoryOptionCombo);
  }

  /**
   * Internal map of all preheated tracked entities, mainly used for confirming existence for
   * updates, and used for object merging.
   */
  @Getter private final Map<UID, TrackedEntity> trackedEntities = new HashMap<>();

  /**
   * Internal map of all preheated enrollments, mainly used for confirming existence for updates,
   * and used for object merging.
   */
  @Getter private final Map<UID, Enrollment> enrollments = new HashMap<>();

  /**
   * Internal map of all preheated events, mainly used for confirming existence for updates, and
   * used for object merging.
   */
  @Getter private final Map<UID, TrackerEvent> trackerEvents = new HashMap<>();

  /**
   * Internal map of all preheated single events, mainly used for confirming existence for updates,
   * and used for object merging.
   */
  @Getter private final Map<UID, SingleEvent> singleEvents = new HashMap<>();

  /**
   * Internal map of all preheated relationships, mainly used for confirming existence for updates,
   * and used for object merging.
   */
  @Getter private final Map<UID, Relationship> relationships = new HashMap<>();

  /**
   * Internal set of all relationship keys and inverted keys already present in the DB. This is used
   * to validate only newly create relationships as update is not allowed for relationships. The key
   * is a string concatenating the relationshipType uid, the uid of the `from` entity and the uid of
   * the `to` entity. The inverted key is a string concatenating the relationshipType uid, the uid
   * of the `to` entity and the uid of the `from` entity.
   */
  private final Set<String> existingRelationships = new HashSet<>();

  /** Internal set of all preheated notes uids (events and enrollments) */
  private final Set<UID> notes = new HashSet<>();

  /**
   * Internal map of all existing TrackedEntityProgramOwner. Used for ownership validations and
   * updating. The root key of this map is the tracked entity UID. The value of the root map is
   * another map which holds a key-value combination where the key is the program UID and the value
   * is an object of {@link TrackedEntityProgramOwnerOrgUnit} holding the ownership OrganisationUnit
   */
  @Getter
  private final Map<UID, Map<String, TrackedEntityProgramOwnerOrgUnit>> programOwner =
      new HashMap<>();

  /** A Map of trackedEntity uid connected to Enrollments */
  @Getter @Setter private Map<UID, List<Enrollment>> trackedEntityToEnrollmentMap = new HashMap<>();

  /** A Map of program uid and without registration {@see Enrollment}. */
  private final Map<String, Enrollment> enrollmentsWithoutRegistration = new HashMap<>();

  /**
   * A map of valid users by username that are present in the payload. A user not available in this
   * cache means, payload's username or uid is invalid. These users are primarily used to represent
   * the ValueType.USERNAME of tracked entity attributes and assignedUser fields in events used in
   * validation and persistence.
   */
  private final Map<String, User> users = Maps.newHashMap();

  /**
   * A list of all unique attribute values that are both present in the payload and in the database.
   * This is going to be used to validate the uniqueness of attribute values in the Validation
   * phase.
   */
  @Getter @Setter private List<UniqueAttributeValue> uniqueAttributeValues = Lists.newArrayList();

  /** A list of all Enrollment UID having at least one Event that is not deleted. */
  @Getter @Setter private List<UID> enrollmentsWithOneOrMoreNonDeletedEvent = Lists.newArrayList();

  /** A list of Program Stage UID having 1 or more Events */
  private final List<Pair<String, String>> programStageWithEvents = Lists.newArrayList();

  /** idScheme map */
  @Getter @Setter private TrackerIdSchemeParams idSchemes = new TrackerIdSchemeParams();

  /**
   * Map of Program ID (primary key) and List of Org Unit ID associated to each program. Note that
   * the List only contains the Org Unit ID of the Org Units that are specified in the import
   * payload.
   */
  @Getter @Setter private Map<String, List<String>> programWithOrgUnitsMap;

  public TrackerPreheat() {}

  /**
   * Put a default metadata value (i.e. CategoryOption "default") into the preheat.
   *
   * @param defaultClass class of the default metadata
   * @param metadata the default metadata
   * @return the tracker preheat
   */
  public <T extends IdentifiableObject> TrackerPreheat putDefault(
      Class<T> defaultClass, T metadata) {
    if (metadata == null) {
      return this;
    }

    defaults.put(defaultClass, metadata);

    return this;
  }

  /**
   * Get a default value from the preheat
   *
   * @param defaultClass The type of object to retrieve
   * @return The default object of the class provided
   */
  @SuppressWarnings("unchecked")
  public <T extends IdentifiableObject> T getDefault(Class<T> defaultClass) {
    return (T) this.defaults.get(defaultClass);
  }

  /**
   * Fetch a metadata object from the pre-heat, based on the type of the object and the cached
   * identifier.
   *
   * @param klass The metadata class to fetch
   * @param id metadata identifier
   * @return A metadata object or null
   */
  public <T extends IdentifiableObject> T get(
      Class<? extends IdentifiableObject> klass, MetadataIdentifier id) {
    if (id == null) {
      return null;
    }
    if (id.getIdScheme() == TrackerIdScheme.ATTRIBUTE) {
      return this.get(klass, id.getAttributeValue());
    }
    return this.get(klass, id.getIdentifier());
  }

  /**
   * Fetch a metadata object from the pre-heat, based on the type of the object and the cached
   * identifier.
   *
   * @param klass The metadata class to fetch
   * @param key The key used during the pre-heat creation
   * @return A metadata object or null
   */
  @SuppressWarnings("unchecked")
  public <T extends IdentifiableObject> T get(
      Class<? extends IdentifiableObject> klass, String key) {
    return (T) map.getOrDefault(klass, new HashMap<>()).get(key);
  }

  public DataElement getDataElement(MetadataIdentifier id) {
    return get(DataElement.class, id);
  }

  public DataElement getDataElement(String id) {
    return get(DataElement.class, id);
  }

  public CategoryOption getCategoryOption(MetadataIdentifier id) {
    return get(CategoryOption.class, id);
  }

  public CategoryOption getCategoryOption(String id) {
    return get(CategoryOption.class, id);
  }

  public CategoryOptionCombo getCategoryOptionCombo(MetadataIdentifier id) {
    return get(CategoryOptionCombo.class, id);
  }

  public CategoryOptionCombo getCategoryOptionCombo(String id) {
    return get(CategoryOptionCombo.class, id);
  }

  /**
   * Fetch all the metadata objects from the pre-heat, by object type
   *
   * @param klass The metadata class to fetch
   * @return a List of pre-heated object or empty list
   */
  @SuppressWarnings("unchecked")
  public <T extends IdentifiableObject> List<T> getAll(Class<T> klass) {
    return new ArrayList<>(
        (Collection<? extends T>) map.getOrDefault(klass, new HashMap<>()).values());
  }

  public boolean isEmpty() {
    return map.isEmpty();
  }

  @SuppressWarnings("unchecked")
  public <T extends IdentifiableObject> TrackerPreheat put(
      TrackerIdSchemeParam idSchemeParam, T object) {
    if (object == null) {
      return this;
    }

    Class<? extends IdentifiableObject> klass = HibernateProxyUtils.getRealClass(object);

    map.computeIfAbsent(klass, k -> new HashMap<>());

    if (User.class.isAssignableFrom(klass)) {
      User userObject = (User) object;

      Map<String, IdentifiableObject> identifierMap = map.get(User.class);

      if (!StringUtils.isEmpty(idSchemeParam.getIdentifier(userObject))
          && !identifierMap.containsKey(idSchemeParam.getIdentifier(userObject))) {
        identifierMap.put(idSchemeParam.getIdentifier(userObject), userObject);
      }
    }

    Optional.ofNullable(idSchemeParam.getIdentifier(object))
        .ifPresent(k -> map.get(klass).put(k, object));

    return this;
  }

  public <T extends IdentifiableObject> TrackerPreheat put(
      TrackerIdSchemeParam idSchemeParam, Collection<T> objects) {
    for (T object : objects) {
      put(idSchemeParam, object);
    }

    return this;
  }

  public TrackerPreheat put(DataElement dataElement) {
    return this.put(idSchemes.getDataElementIdScheme(), dataElement);
  }

  public TrackerPreheat put(Program program) {
    return this.put(idSchemes.getProgramIdScheme(), program);
  }

  public TrackerPreheat put(ProgramStage programStage) {
    return this.put(idSchemes.getProgramStageIdScheme(), programStage);
  }

  public TrackerPreheat put(CategoryOptionCombo categoryOptionCombo) {
    return this.put(idSchemes.getCategoryOptionComboIdScheme(), categoryOptionCombo);
  }

  public TrackedEntity getTrackedEntity(UID uid) {
    return trackedEntities.get(uid);
  }

  public void putTrackedEntities(List<TrackedEntity> trackedEntities) {

    trackedEntities.forEach(this::putTrackedEntity);
  }

  private void putTrackedEntity(TrackedEntity trackedEntity) {
    trackedEntities.put(UID.of(trackedEntity), trackedEntity);
  }

  public Enrollment getEnrollment(UID uid) {
    return enrollments.get(uid);
  }

  public void putEnrollments(List<Enrollment> enrollments) {
    enrollments.forEach(this::putEnrollment);
  }

  public void putEnrollment(Enrollment enrollment) {
    enrollments.put(UID.of(enrollment), enrollment);
  }

  public TrackerEvent getTrackerEvent(UID uid) {
    return trackerEvents.get(uid);
  }

  public void putTrackerEvents(List<TrackerEvent> events) {
    events.forEach(this::putTrackerEvent);
  }

  public void putTrackerEvent(TrackerEvent event) {
    trackerEvents.put(UID.of(event), event);
  }

  public SingleEvent getSingleEvent(UID uid) {
    return singleEvents.get(uid);
  }

  public void putSingleEvents(List<SingleEvent> events) {
    events.forEach(this::putSingleEvent);
  }

  public void putSingleEvent(SingleEvent event) {
    singleEvents.put(UID.of(event), event);
  }

  public void addNotes(Set<UID> notes) {
    this.notes.addAll(notes);
  }

  public boolean hasNote(UID uid) {
    return notes.contains(uid);
  }

  public RelationshipType getRelationshipType(MetadataIdentifier id) {
    return get(RelationshipType.class, id);
  }

  public Relationship getRelationship(UID relationship) {
    return relationships.get(relationship);
  }

  public Relationship getRelationship(
      org.hisp.dhis.tracker.imports.domain.Relationship relationship) {
    return relationships.get(relationship.getUid());
  }

  public boolean isDuplicate(org.hisp.dhis.tracker.imports.domain.Relationship relationship) {
    RelationshipType relationshipType =
        get(RelationshipType.class, relationship.getRelationshipType());

    if (hasRelationshipKey(relationship, relationshipType)) {
      RelationshipKey relationshipKey = getRelationshipKey(relationship, relationshipType);

      RelationshipKey inverseKey = null;
      if (relationshipType.isBidirectional()) {
        inverseKey = relationshipKey.inverseKey();
      }
      return Stream.of(relationshipKey, inverseKey)
          .filter(Objects::nonNull)
          .anyMatch(key -> existingRelationships.contains(key.asString()));
    }
    return false;
  }

  public void putRelationships(List<Relationship> relationships) {
    relationships.forEach(this::putRelationship);
  }

  public void putRelationship(Relationship relationship) {
    if (Objects.nonNull(relationship)) {
      relationships.put(UID.of(relationship), relationship);
    }
  }

  public void addExistingRelationship(Relationship relationship) {
    existingRelationships.add(relationship.getKey());
    if (relationship.getRelationshipType().isBidirectional()) {
      existingRelationships.add(relationship.getInvertedKey());
    }
  }

  public Enrollment getEnrollmentsWithoutRegistration(String programUid) {
    return enrollmentsWithoutRegistration.get(programUid);
  }

  public void putEnrollmentsWithoutRegistration(String programUid, Enrollment enrollment) {
    this.enrollmentsWithoutRegistration.put(programUid, enrollment);
  }

  public void addProgramOwners(List<TrackedEntityProgramOwnerOrgUnit> tepos) {
    tepos.forEach(
        tepo -> addProgramOwner(UID.of(tepo.getTrackedEntityId()), tepo.getProgramId(), tepo));
  }

  private void addProgramOwner(UID te, String program, TrackedEntityProgramOwnerOrgUnit tepo) {
    programOwner.computeIfAbsent(te, k -> new HashMap<>()).put(program, tepo);
  }

  public void addProgramOwner(UID te, String program, OrganisationUnit orgUnit) {
    programOwner.computeIfAbsent(te, k -> new HashMap<>());
    if (!programOwner.get(te).containsKey(program)) {
      TrackedEntityProgramOwnerOrgUnit tepo =
          new TrackedEntityProgramOwnerOrgUnit(te.getValue(), program, orgUnit);
      programOwner.get(te).put(program, tepo);
    }
  }

  public void addUsers(Set<User> users) {
    Map<String, User> userMap =
        users.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(User::getUsername, Function.identity()));
    this.users.putAll(userMap);
  }

  public Optional<User> getUserByUsername(String username) {
    return Optional.ofNullable(this.users.get(username));
  }

  public Optional<User> getUserByUid(String uid) {
    return this.users.values().stream().filter(u -> Objects.equals(uid, u.getUid())).findAny();
  }

  public OrganisationUnit getOrganisationUnit(MetadataIdentifier id) {
    return get(OrganisationUnit.class, id);
  }

  public OrganisationUnit getOrganisationUnit(String id) {
    return get(OrganisationUnit.class, id);
  }

  public ProgramStage getProgramStage(MetadataIdentifier id) {
    return get(ProgramStage.class, id);
  }

  public ProgramStage getProgramStage(String id) {
    return get(ProgramStage.class, id);
  }

  public Program getProgram(MetadataIdentifier id) {
    return get(Program.class, id);
  }

  public Program getProgram(String id) {
    return get(Program.class, id);
  }

  public TrackedEntityType getTrackedEntityType(MetadataIdentifier id) {
    return get(TrackedEntityType.class, id);
  }

  public TrackedEntityType getTrackedEntityType(String id) {
    return get(TrackedEntityType.class, id);
  }

  public TrackedEntityAttribute getTrackedEntityAttribute(MetadataIdentifier id) {
    return get(TrackedEntityAttribute.class, id);
  }

  public TrackedEntityAttribute getTrackedEntityAttribute(String id) {
    return get(TrackedEntityAttribute.class, id);
  }

  public TrackerPreheat addProgramStageWithEvents(String programStageUid, String enrollmentUid) {
    this.programStageWithEvents.add(Pair.of(programStageUid, enrollmentUid));
    return this;
  }

  public boolean hasProgramStageWithEvents(MetadataIdentifier programStage, String enrollmentUid) {
    ProgramStage ps = this.getProgramStage(programStage);
    return this.programStageWithEvents.contains(Pair.of(ps.getUid(), enrollmentUid));
  }

  /** Checks if an entity exists in the DB. */
  public <T extends TrackerDto> boolean exists(T entity) {
    return exists(entity.getTrackerType(), entity.getUid());
  }

  /**
   * Checks if an entity of given type and UID exists in the DB.
   *
   * @param type tracker type
   * @param uid uid of entity to check
   * @return true if an entity of given type and UID exists in the DB
   */
  public boolean exists(TrackerType type, UID uid) {
    Objects.requireNonNull(type);

    return switch (type) {
      case TRACKED_ENTITY -> getTrackedEntity(uid) != null;
      case ENROLLMENT -> getEnrollment(uid) != null;
      case EVENT -> getTrackerEvent(uid) != null || getSingleEvent(uid) != null;
      case RELATIONSHIP -> getRelationship(uid) != null;
    };
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", TrackerPreheat.class.getSimpleName() + "[", "]")
        .add("map=" + map)
        .toString();
  }
}
