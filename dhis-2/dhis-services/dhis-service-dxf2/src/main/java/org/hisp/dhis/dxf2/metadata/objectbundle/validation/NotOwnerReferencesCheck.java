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
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.AtomicMode;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReportMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.preheat.PreheatErrorReport;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen
 */
@Component
public class NotOwnerReferencesCheck implements ValidationCheck {
  @Override
  public <T extends IdentifiableObject> TypeReport check(
      ObjectBundle bundle,
      Class<T> klass,
      List<T> persistedObjects,
      List<T> nonPersistedObjects,
      ImportStrategy importStrategy,
      ValidationContext ctx) {
    if ((persistedObjects.isEmpty() && nonPersistedObjects.isEmpty())
        || ImportReportMode.ERRORS_NOT_OWNER != bundle.getImportReportMode()) {
      return TypeReport.empty(klass);
    }

    TypeReport typeReport = new TypeReport(klass);

    for (IdentifiableObject object : joinObjects(persistedObjects, nonPersistedObjects)) {
      List<PreheatErrorReport> errorReports =
          checkReferences(object, bundle.getPreheatIdentifier(), ctx);

      if (!errorReports.isEmpty() && object != null) {
        ObjectReport objectReport = new ObjectReport(object, bundle);
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
      IdentifiableObject object, PreheatIdentifier identifier, ValidationContext ctx) {
    if (object == null) {
      return emptyList();
    }

    List<PreheatErrorReport> preheatErrorReports = new ArrayList<>();

    Schema schema =
        ctx.getSchemaService().getDynamicSchema(HibernateProxyUtils.getRealClass(object));

    schema.getProperties().stream()
        .filter(
            p ->
                !p.isOwner()
                    && p.isWritable()
                    && (PropertyType.REFERENCE == p.getPropertyType()
                            && schema.getKlass() != p.getKlass()
                        || PropertyType.REFERENCE == p.getItemPropertyType()
                            && schema.getKlass() != p.getItemKlass()))
        .forEach(
            p -> {
              if (!p.isCollection()) {
                checkReference(object, identifier, preheatErrorReports, p);
              } else {
                checkCollection(object, identifier, preheatErrorReports, p);
              }
            });

    return preheatErrorReports;
  }

  private void checkReference(
      IdentifiableObject object,
      PreheatIdentifier identifier,
      List<PreheatErrorReport> preheatErrorReports,
      Property p) {
    // This is a temporary solution needed since we have overloaded the
    // "user" object in IdObject will be removed when we complete move over
    // to the new sharing payload.
    if ("user".equals(p.getName())) {
      return;
    }

    IdentifiableObject refObject = ReflectionUtils.invokeMethod(object, p.getGetterMethod());

    if (refObject != null) {
      preheatErrorReports.add(
          new PreheatErrorReport(
              identifier,
              object.getClass(),
              ErrorCode.E5006,
              identifier.getIdentifiersWithName(refObject),
              identifier.getIdentifiersWithName(object),
              p.getName()));
    }
  }

  private void checkCollection(
      IdentifiableObject object,
      PreheatIdentifier identifier,
      List<PreheatErrorReport> preheatErrorReports,
      Property p) {
    Collection<IdentifiableObject> refObjects =
        ReflectionUtils.invokeMethod(object, p.getGetterMethod());

    if (refObjects == null) {
      return;
    }

    for (IdentifiableObject refObject : refObjects) {
      if (refObject != null) {
        preheatErrorReports.add(
            new PreheatErrorReport(
                identifier,
                object.getClass(),
                ErrorCode.E5006,
                identifier.getIdentifiersWithName(refObject),
                identifier.getIdentifiersWithName(object),
                p.getCollectionName()));
      }
    }
  }
}
