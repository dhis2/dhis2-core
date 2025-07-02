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
package org.hisp.dhis.program.notification.template.snapshot;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.springframework.stereotype.Service;

@Service("org.hisp.dhis.program.notification.template.snapshot.NotificationTemplateMapper")
public class NotificationTemplateMapper {

  public ProgramNotificationTemplate toProgramNotificationTemplate(
      ProgramNotificationTemplateSnapshot templateSnapshot) {
    return toBaseIdentifiableObject(
        templateSnapshot,
        ProgramNotificationTemplate::new,
        List.of(
            t -> t.setMessageTemplate(templateSnapshot.getMessageTemplate()),
            t -> t.setNotificationRecipient(templateSnapshot.getNotificationRecipient()),
            t ->
                t.setRecipientProgramAttribute(
                    toBaseIdentifiableObject(
                        templateSnapshot.getRecipientProgramAttribute(),
                        TrackedEntityAttribute::new,
                        Collections.emptyList())),
            t -> t.setNotificationTrigger(templateSnapshot.getNotificationTrigger()),
            t -> t.setSubjectTemplate(templateSnapshot.getSubjectTemplate()),
            t -> t.setDeliveryChannels(templateSnapshot.getDeliveryChannels()),
            t ->
                t.setNotifyParentOrganisationUnitOnly(
                    templateSnapshot.getNotifyParentOrganisationUnitOnly()),
            t -> t.setNotifyUsersInHierarchyOnly(templateSnapshot.getNotifyUsersInHierarchyOnly()),
            t ->
                t.setRecipientDataElement(
                    toBaseIdentifiableObject(
                        templateSnapshot.getRecipientDataElement(),
                        DataElement::new,
                        Collections.emptyList())),
            t -> t.setRecipientUserGroup(toUserGroup(templateSnapshot.getRecipientUserGroup()))));
  }

  public ProgramNotificationTemplateSnapshot toProgramNotificationTemplateSnapshot(
      ProgramNotificationTemplate template) {

    return toIdentifiableObjectSnapshot(
        template,
        ProgramNotificationTemplateSnapshot::new,
        List.of(
            t -> t.setMessageTemplate(template.getMessageTemplate()),
            t -> t.setNotificationRecipient(template.getNotificationRecipient()),
            t ->
                t.setRecipientProgramAttribute(
                    toIdentifiableObjectSnapshot(
                        template.getRecipientProgramAttribute(),
                        IdentifiableObjectSnapshot::new,
                        Collections.emptyList())),
            t -> t.setNotificationTrigger(template.getNotificationTrigger()),
            t -> t.setSubjectTemplate(template.getSubjectTemplate()),
            t -> t.setDeliveryChannels(template.getDeliveryChannels()),
            t ->
                t.setNotifyParentOrganisationUnitOnly(
                    template.getNotifyParentOrganisationUnitOnly()),
            t -> t.setNotifyUsersInHierarchyOnly(template.getNotifyUsersInHierarchyOnly()),
            t ->
                t.setRecipientDataElement(
                    toIdentifiableObjectSnapshot(
                        template.getRecipientDataElement(),
                        IdentifiableObjectSnapshot::new,
                        Collections.emptyList())),
            t -> t.setSendRepeatable(t.isSendRepeatable()),
            t -> t.setRecipientUserGroup(toUserGroupSnapshot(template.getRecipientUserGroup()))));
  }

  private UserGroup toUserGroup(UserGroupSnapshot userGroupSnapshot) {
    return toBaseIdentifiableObject(
        userGroupSnapshot,
        UserGroup::new,
        List.of(ug -> ug.setMembers(toUsers(userGroupSnapshot.getMembers()))));
  }

  private UserGroupSnapshot toUserGroupSnapshot(UserGroup userGroup) {
    return toIdentifiableObjectSnapshot(
        userGroup,
        UserGroupSnapshot::new,
        List.of(ug -> ug.setMembers(toUserSnapshot(userGroup.getMembers()))));
  }

  private Set<User> toUsers(Set<UserSnapshot> userSnapshots) {
    Set<User> users = new HashSet<>();

    for (UserSnapshot userSnapshot : userSnapshots) {
      users.add(
          toBaseIdentifiableObject(
              userSnapshot,
              User::new,
              List.of(
                  u -> u.setName(userSnapshot.getName()),
                  u -> u.setUsername(userSnapshot.getUsername()),
                  u -> u.setEmail(userSnapshot.getEmail()),
                  u -> u.setPhoneNumber(userSnapshot.getPhoneNumber()))));
    }
    return users;
  }

  private Set<UserSnapshot> toUserSnapshot(Set<User> users) {
    Set<UserSnapshot> userSnapshots = new HashSet<>();

    for (User user : users) {
      userSnapshots.add(
          toIdentifiableObjectSnapshot(
              user,
              UserSnapshot::new,
              List.of(
                  u -> u.setName(user.getName()),
                  u -> u.setUsername(user.getUsername()),
                  u -> u.setEmail(user.getEmail()),
                  u -> u.setPhoneNumber(user.getPhoneNumber()))));
    }
    return userSnapshots;
  }

  private <T extends IdentifiableObjectSnapshot> T toIdentifiableObjectSnapshot(
      IdentifiableObject from,
      Supplier<T> instanceSupplier,
      Collection<Consumer<T>> instanceTransformers) {
    Optional<T> optionalInstance =
        Optional.ofNullable(from)
            .map(
                baseIdentifiableObject -> {
                  T instance = instanceSupplier.get();
                  instance.setId(baseIdentifiableObject.getId());
                  instance.setUid(baseIdentifiableObject.getUid());
                  instance.setCode(baseIdentifiableObject.getCode());
                  return instance;
                });

    optionalInstance.ifPresent(
        t -> instanceTransformers.forEach(instanceTransformer -> instanceTransformer.accept(t)));

    return optionalInstance.orElse(null);
  }

  private <T extends BaseIdentifiableObject> T toBaseIdentifiableObject(
      IdentifiableObjectSnapshot from,
      Supplier<T> instanceSupplier,
      Collection<Consumer<T>> instanceTransformers) {
    Optional<T> optionalInstance =
        Optional.ofNullable(from)
            .map(
                identifiableObjectSnapshot -> {
                  T instance = instanceSupplier.get();
                  instance.setId(identifiableObjectSnapshot.getId());
                  instance.setUid(identifiableObjectSnapshot.getUid());
                  instance.setCode(identifiableObjectSnapshot.getCode());
                  return instance;
                });

    optionalInstance.ifPresent(
        t -> instanceTransformers.forEach(instanceTransformer -> instanceTransformer.accept(t)));

    return optionalInstance.orElse(null);
  }
}
