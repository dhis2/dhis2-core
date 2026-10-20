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
package org.hisp.dhis.dxf2.metadata.collection;

import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.validateAndThrowErrors;

import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableObjects;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.FeedbackException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.schema.validation.SchemaValidator;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
@RequiredArgsConstructor
public class DefaultCollectionService implements CollectionService {
  private final IdentifiableObjectManager manager;

  private final AclService aclService;

  private final SchemaService schemaService;

  private final SchemaValidator schemaValidator;

  private final EntityManager entityManager;

  private final UserService userService;

  @Override
  @Transactional(rollbackFor = FeedbackException.class)
  public TypeReport addCollectionItems(
      IdentifiableObject object,
      String propertyName,
      Collection<? extends IdentifiableObject> objects)
      throws ForbiddenException, ConflictException, NotFoundException, BadRequestException {
    Property property =
        validateUpdate(
            object, propertyName, "Only identifiable object collections can be added to.");

    Collection<String> itemCodes = getItemCodes(objects);

    validateUserChanges(object, property, itemCodes, List.of());

    return addItems(object, property, itemCodes);
  }

  private TypeReport addItems(
      IdentifiableObject object, Property property, Collection<String> itemCodes)
      throws BadRequestException {
    if (itemCodes.isEmpty()) {
      return TypeReport.empty(property.getItemKlass());
    }

    TypeReport report = new TypeReport(property.getItemKlass());

    if (property.isOwner()) {
      addOwnedCollectionItems(object, property, itemCodes, report);
    } else {
      addNonOwnedCollectionItems(object, property, itemCodes, report);
    }

    return report;
  }

  private void addOwnedCollectionItems(
      IdentifiableObject object, Property property, Collection<String> itemCodes, TypeReport report)
      throws BadRequestException {
    Collection<IdentifiableObject> collection = getCollection(object, property);

    updateCollectionItems(
        property,
        itemCodes,
        report,
        ErrorCode.E1108,
        item -> {
          if (!collection.contains(item)) {
            collection.add(item);
            report.updatedInc(1);
          } else {
            report.ignoredInc(1);
          }
        });
    validateAndThrowErrors(() -> schemaValidator.validateProperty(property, object));
  }

  private void addNonOwnedCollectionItems(
      IdentifiableObject object,
      Property property,
      Collection<String> itemCodes,
      TypeReport report) {
    Schema owningSchema = schemaService.getSchema(property.getItemKlass());
    Property owningProperty = owningSchema.propertyByRole(property.getOwningRole());

    updateCollectionItems(
        property,
        itemCodes,
        report,
        ErrorCode.E1108,
        item -> {
          Collection<IdentifiableObject> collection = getCollection(item, owningProperty);

          if (!collection.contains(object)) {
            validateAndThrowErrors(() -> schemaValidator.validateProperty(property, object));
            collection.add(object);
            manager.update(item);
            report.updatedInc(1);
          } else {
            report.ignoredInc(1);
          }
        });
    entityManager.refresh(object);
  }

  @Override
  @Transactional(rollbackFor = FeedbackException.class)
  public TypeReport delCollectionItems(
      IdentifiableObject object,
      String propertyName,
      Collection<? extends IdentifiableObject> objects)
      throws ForbiddenException, ConflictException, NotFoundException, BadRequestException {
    Property property =
        validateUpdate(
            object, propertyName, "Only identifiable object collections can be removed from.");

    Collection<String> itemCodes = getItemCodes(objects);

    validateUserChanges(object, property, List.of(), itemCodes);

    return delItems(object, property, itemCodes);
  }

  private TypeReport delItems(
      IdentifiableObject object, Property property, Collection<String> itemCodes)
      throws BadRequestException {
    if (itemCodes.isEmpty()) {
      return TypeReport.empty(property.getItemKlass());
    }

    TypeReport report = new TypeReport(property.getItemKlass());

    if (property.isOwner()) {
      delOwnedCollectionItems(object, property, itemCodes, report);
    } else {
      delNonOwnedCollectionItems(object, property, itemCodes, report);
    }

    validateAndThrowErrors(() -> schemaValidator.validateProperty(property, object));
    return report;
  }

  private void delOwnedCollectionItems(
      IdentifiableObject object,
      Property property,
      Collection<String> itemCodes,
      TypeReport report) {
    Collection<IdentifiableObject> collection = getCollection(object, property);

    updateCollectionItems(
        property,
        itemCodes,
        report,
        ErrorCode.E1109,
        item -> {
          if (collection.contains(item)) {
            collection.remove(item);
            report.deletedInc(1);
          } else {
            report.ignoredInc(1);
          }
        });
  }

  private void delNonOwnedCollectionItems(
      IdentifiableObject object,
      Property property,
      Collection<String> itemCodes,
      TypeReport report) {
    Schema owningSchema = schemaService.getSchema(property.getItemKlass());
    Property owningProperty = owningSchema.propertyByRole(property.getOwningRole());

    updateCollectionItems(
        property,
        itemCodes,
        report,
        ErrorCode.E1109,
        item -> {
          Collection<IdentifiableObject> collection = getCollection(item, owningProperty);

          if (collection.contains(object)) {
            validateAndThrowErrors(() -> schemaValidator.validateProperty(owningProperty, item));
            collection.remove(object);
            manager.update(item);
            report.deletedInc(1);
          } else {
            report.ignoredInc(1);
          }
        });
    entityManager.refresh(object);
  }

  /**
   * Replacing validates the whole change up front, so that a rejected addition cannot leave the
   * deletions that precede it in place. Only the net change is validated: items that stay in the
   * collection are removed and added back, which changes nothing.
   */
  @Override
  @Transactional(rollbackFor = FeedbackException.class)
  public TypeReport replaceCollectionItems(
      IdentifiableObject object,
      String propertyName,
      Collection<? extends IdentifiableObject> objects)
      throws ForbiddenException, ConflictException, NotFoundException, BadRequestException {
    Property property =
        validateUpdate(
            object, propertyName, "Only identifiable object collections can be replaced.");

    Collection<String> deletions = getItemCodes(getCollection(object, property));
    Collection<String> additions = getItemCodes(objects);

    Set<String> current = new HashSet<>(deletions);
    Set<String> requested = new HashSet<>(additions);
    validateUserChanges(
        object,
        property,
        additions.stream().filter(uid -> !current.contains(uid)).toList(),
        deletions.stream().filter(uid -> !requested.contains(uid)).toList());

    TypeReport delReport = delItems(object, property, deletions);
    TypeReport addReport = addItems(object, property, additions);
    return delReport.mergeAllowEmpty(addReport);
  }

  /**
   * Merging validates the whole change up front, so that a rejected addition cannot leave the
   * deletions that precede it in place.
   */
  @Override
  @Transactional(rollbackFor = FeedbackException.class)
  public TypeReport mergeCollectionItems(
      IdentifiableObject object, String propertyName, IdentifiableObjects items)
      throws ForbiddenException, ConflictException, NotFoundException, BadRequestException {
    Property property =
        validateUpdate(
            object, propertyName, "Only identifiable object collections can be removed from.");

    Collection<String> deletions = getItemCodes(items.getDeletions());
    Collection<String> additions = getItemCodes(items.getAdditions());

    validateUserChanges(object, property, additions, deletions);

    TypeReport delReport = delItems(object, property, deletions);
    TypeReport addReport = addItems(object, property, additions);
    return delReport.mergeAllowEmpty(addReport);
  }

  private Property validateUpdate(IdentifiableObject object, String propertyName, String message)
      throws ForbiddenException, NotFoundException, ConflictException {
    Schema schema = schemaService.getSchema(HibernateProxyUtils.getRealClass(object));

    if (!aclService.canUpdate(CurrentUserUtil.getCurrentUserDetails(), object)) {
      throw new ForbiddenException("You don't have the proper permissions to update this object.");
    }

    if (!schema.hasProperty(propertyName)) {
      throw new NotFoundException(
          "Property " + propertyName + " does not exist on " + object.getClass().getName());
    }

    Property property = schema.getProperty(propertyName);

    if (!property.isCollection() || !property.isIdentifiableObject()) {
      throw new ConflictException(message);
    }
    return property;
  }

  /**
   * Changes made through the generic collection endpoints to a collection of a user (its roles,
   * groups, organisation units or dimension constraints), or to the users of a user role or of an
   * organisation unit, follow the rules of a user update (see {@link
   * UserService#checkCanChangeUserCollection}): the current user needs rights over every affected
   * user, and may only add what they could give that user by updating it. All of it is checked
   * before anything is changed. The members of a user group are governed by the sharing of the
   * group instead, as with {@link org.hisp.dhis.user.UserGroupService#addUserToGroups}.
   */
  @SuppressWarnings("unchecked")
  private void validateUserChanges(
      IdentifiableObject object,
      Property property,
      Collection<String> additions,
      Collection<String> deletions)
      throws ForbiddenException {
    if (additions.isEmpty() && deletions.isEmpty()) {
      return;
    }

    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    Class<? extends IdentifiableObject> itemType =
        (Class<? extends IdentifiableObject>) property.getItemKlass();

    if (object instanceof User user) {
      userService.checkCanChangeUserCollection(
          List.of(user),
          itemType,
          manager.getByUid(itemType, additions),
          manager.getByUid(itemType, deletions),
          currentUser);
    } else if (User.class == itemType
        && (object instanceof UserRole || object instanceof OrganisationUnit)) {
      Class<?> objectType = object instanceof UserRole ? UserRole.class : OrganisationUnit.class;
      if (!additions.isEmpty()) {
        userService.checkCanChangeUserCollection(
            manager.getByUid(User.class, additions),
            objectType,
            List.of(object),
            List.of(),
            currentUser);
      }
      if (!deletions.isEmpty()) {
        userService.checkCanChangeUserCollection(
            manager.getByUid(User.class, deletions),
            objectType,
            List.of(),
            List.of(object),
            currentUser);
      }
    }
  }

  private Collection<String> getItemCodes(Collection<? extends IdentifiableObject> objects) {
    return objects.stream().map(IdentifiableObject::getUid).collect(toList());
  }

  @SuppressWarnings("unchecked")
  private List<? extends IdentifiableObject> getItems(
      Property property, Collection<String> itemCodes) {
    return manager.getByUid(
        ((Class<? extends IdentifiableObject>) property.getItemKlass()), itemCodes);
  }

  @SuppressWarnings("unchecked")
  private Collection<IdentifiableObject> getCollection(
      IdentifiableObject object, Property property) {
    try {
      return (Collection<IdentifiableObject>) property.getGetterMethod().invoke(object);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  @FunctionalInterface
  private interface CollectionUpdate {
    void applyToItem(IdentifiableObject item) throws Exception;
  }

  private void updateCollectionItems(
      Property property,
      Collection<String> itemCodes,
      TypeReport report,
      ErrorCode errorCode,
      CollectionUpdate update) {
    int index = 0;
    for (IdentifiableObject item : getItems(property, itemCodes)) {
      try {
        update.applyToItem(item);
      } catch (Exception ex) {
        Class<?> itemType = property.getItemKlass();
        ObjectReport objectReport = new ObjectReport(itemType, index, item.getUid());
        objectReport.addErrorReport(new ErrorReport(itemType, errorCode, ex.getMessage()));
        report.addObjectReport(objectReport);
        report.ignoredInc(1);
      }
      index++;
    }
  }
}
