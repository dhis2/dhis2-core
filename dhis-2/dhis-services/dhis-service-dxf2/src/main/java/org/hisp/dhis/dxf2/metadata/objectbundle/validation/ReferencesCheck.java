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
package org.hisp.dhis.dxf2.metadata.objectbundle.validation;

import static java.util.Collections.emptyList;
import static org.hisp.dhis.dxf2.metadata.objectbundle.validation.ValidationUtils.joinObjects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.dxf2.metadata.AtomicMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.preheat.PreheatErrorReport;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class ReferencesCheck implements ValidationCheck {
  @Override
  public <T extends IdentifiableObject> TypeReport check(
      ObjectBundle bundle,
      Class<T> klass,
      List<T> persistedObjects,
      List<T> nonPersistedObjects,
      ImportStrategy importStrategy,
      ValidationContext ctx) {
    if (persistedObjects.isEmpty() && nonPersistedObjects.isEmpty()) {
      return TypeReport.empty(klass);
    }

    TypeReport typeReport = new TypeReport(klass);

    for (IdentifiableObject object : joinObjects(persistedObjects, nonPersistedObjects)) {
      List<PreheatErrorReport> errorReports =
          checkReferences(
              object,
              bundle.getPreheat(),
              bundle.getPreheatIdentifier(),
              bundle.isSkipSharing(),
              ctx);

      if (!errorReports.isEmpty() && object != null) {
        ObjectReport objectReport = new ObjectReport(object, bundle);
        objectReport.setDisplayName(object.getDisplayName());
        objectReport.addErrorReports(errorReports);
        typeReport.addObjectReport(objectReport);
      }
    }

    if (typeReport.hasErrorReports() && AtomicMode.ALL == bundle.getAtomicMode()) {
      typeReport.getStats().incIgnored();
    }

    return typeReport;
  }

  private List<PreheatErrorReport> checkReferences(
      IdentifiableObject object,
      Preheat preheat,
      PreheatIdentifier identifier,
      boolean skipSharing,
      ValidationContext ctx) {
    if (object == null) {
      return emptyList();
    }

    List<PreheatErrorReport> preheatErrorReports = new ArrayList<>();

    Schema schema =
        ctx.getSchemaService().getDynamicSchema(HibernateProxyUtils.getRealClass(object));
    schema.getProperties().stream()
        .filter(
            p ->
                p.isPersisted()
                    && p.isOwner()
                    && (PropertyType.REFERENCE == p.getPropertyType()
                        || PropertyType.REFERENCE == p.getItemPropertyType()))
        .forEach(
            p -> {
              if (skipCheck(p.getKlass()) || skipCheck(p.getItemKlass())) {
                return;
              }

              if (!p.isCollection()) {
                checkReference(object, preheat, identifier, skipSharing, preheatErrorReports, p);
              } else {
                checkCollection(object, preheat, identifier, preheatErrorReports, p);
              }
            });

    if (schema.havePersistedProperty("attributeValues")) {
      checkAttributeValues(object, preheat, identifier, preheatErrorReports);
    }

    if (schema.havePersistedProperty("sharing") && !skipSharing && object.getSharing() != null) {
      checkSharing(object, preheat, preheatErrorReports);
    }

    return preheatErrorReports;
  }

  private void checkReference(
      IdentifiableObject object,
      Preheat preheat,
      PreheatIdentifier identifier,
      boolean skipSharing,
      List<PreheatErrorReport> preheatErrorReports,
      Property property) {
    IdentifiableObject refObject = ReflectionUtils.invokeMethod(object, property.getGetterMethod());
    IdentifiableObject ref = preheat.get(identifier, refObject);

    if (ref == null && refObject != null && !preheat.isDefault(refObject)) {
      // HACK this needs to be redone when the move to using
      // uuid as user identifiers is ready
      boolean isUserReference =
          User.class.isAssignableFrom(property.getKlass())
              && ("user".equals(property.getName())
                  || "lastUpdatedBy".equals(property.getName())
                  || "createdBy".equals(property.getName()));

      if (!(isUserReference && skipSharing)) {
        PreheatErrorReport error =
            new PreheatErrorReport(
                identifier,
                ErrorCode.E5002,
                object,
                property,
                identifier.getIdentifiersWithName(refObject),
                identifier.getIdentifiersWithName(object),
                property.getName());

        preheatErrorReports.add(error);
      }
    }
  }

  private void checkCollection(
      IdentifiableObject object,
      Preheat preheat,
      PreheatIdentifier identifier,
      List<PreheatErrorReport> preheatErrorReports,
      Property property) {
    Collection<IdentifiableObject> objects =
        ReflectionUtils.newCollectionInstance(property.getKlass());
    Collection<IdentifiableObject> refObjects =
        ReflectionUtils.invokeMethod(object, property.getGetterMethod());

    if (refObjects != null) {
      for (IdentifiableObject refObject : refObjects) {
        if (preheat.isDefault(refObject)) continue;

        IdentifiableObject ref = preheat.get(identifier, refObject);

        if (ref == null && refObject != null) {
          preheatErrorReports.add(
              new PreheatErrorReport(
                  identifier,
                  ErrorCode.E5002,
                  object,
                  property,
                  identifier.getIdentifiersWithName(refObject),
                  identifier.getIdentifiersWithName(object),
                  property.getName()));
        } else {
          objects.add(refObject);
        }
      }

      CollectionUtils.findDuplicates(refObjects)
          .forEach(
              refObject ->
                  preheatErrorReports.add(
                      new PreheatErrorReport(
                          identifier,
                          ErrorCode.E5007,
                          object,
                          property,
                          identifier.getIdentifiersWithName(refObject),
                          identifier.getIdentifiersWithName(object),
                          property.getName())));
    }

    ReflectionUtils.invokeMethod(object, property.getSetterMethod(), objects);
  }

  private void checkAttributeValues(
      IdentifiableObject object,
      Preheat preheat,
      PreheatIdentifier identifier,
      List<PreheatErrorReport> preheatErrorReports) {
    object.getAttributeValues().stream()
        .filter(
            attributeValue ->
                attributeValue.getAttribute() != null
                    && preheat.get(identifier, attributeValue.getAttribute()) == null)
        .forEach(
            attributeValue ->
                preheatErrorReports.add(
                    new PreheatErrorReport(
                        identifier,
                        object.getClass(),
                        ErrorCode.E5002,
                        identifier.getIdentifiersWithName(attributeValue.getAttribute()),
                        identifier.getIdentifiersWithName(object),
                        "attributeValues")));
  }

  private void checkSharing(
      IdentifiableObject object, Preheat preheat, List<PreheatErrorReport> preheatErrorReports) {
    Sharing sharing = object.getSharing();
    if (sharing.hasUserGroupAccesses()) {
      sharing.getUserGroups().values().stream()
          .filter(
              userGroupAccess ->
                  preheat.get(PreheatIdentifier.UID, userGroupAccess.toDtoObject().getUserGroup())
                      == null)
          .forEach(
              userGroupAccess ->
                  preheatErrorReports.add(
                      new PreheatErrorReport(
                          PreheatIdentifier.UID,
                          object.getClass(),
                          ErrorCode.E5002,
                          PreheatIdentifier.UID.getIdentifiersWithName(
                              userGroupAccess.toDtoObject().getUserGroup()),
                          PreheatIdentifier.UID.getIdentifiersWithName(object),
                          "userGroupAccesses")));
    }

    if (sharing.hasUserAccesses()) {
      sharing.getUsers().values().stream()
          .filter(
              userAccess ->
                  preheat.get(PreheatIdentifier.UID, userAccess.toDtoObject().getUser()) == null)
          .forEach(
              userAccesses ->
                  preheatErrorReports.add(
                      new PreheatErrorReport(
                          PreheatIdentifier.UID,
                          object.getClass(),
                          ErrorCode.E5002,
                          PreheatIdentifier.UID.getIdentifiersWithName(
                              userAccesses.toDtoObject().getUser()),
                          PreheatIdentifier.UID.getIdentifiersWithName(object),
                          "userAccesses")));
    }
  }

  private boolean skipCheck(Class<?> klass) {
    return klass != null
        && (EmbeddedObject.class.isAssignableFrom(klass)
            || Period.class.isAssignableFrom(klass)
            || PeriodType.class.isAssignableFrom(klass));
  }
}
