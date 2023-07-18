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

import static org.hisp.dhis.dxf2.metadata.objectbundle.validation.ValidationUtils.createObjectReport;

import java.util.List;
import java.util.function.Consumer;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class SecurityCheck implements ObjectValidationCheck {
  @Override
  public <T extends IdentifiableObject> void check(
      ObjectBundle bundle,
      Class<T> klass,
      List<T> persistedObjects,
      List<T> nonPersistedObjects,
      ImportStrategy importStrategy,
      ValidationContext context,
      Consumer<ObjectReport> addReports) {
    if (importStrategy.isUpdate() || importStrategy.isCreateAndUpdate()) {
      runValidationCheck(
          bundle, klass, persistedObjects, ImportStrategy.UPDATE, context, addReports);
    }
    if (importStrategy.isCreate() || importStrategy.isCreateAndUpdate()) {
      runValidationCheck(
          bundle, klass, nonPersistedObjects, ImportStrategy.CREATE, context, addReports);
    }
    if (importStrategy.isDelete()) {
      runValidationCheck(
          bundle, klass, persistedObjects, ImportStrategy.DELETE, context, addReports);
    }
  }

  private <T extends IdentifiableObject> void runValidationCheck(
      ObjectBundle bundle,
      Class<T> klass,
      List<T> objects,
      ImportStrategy importMode,
      ValidationContext ctx,
      Consumer<ObjectReport> addReports) {
    if (objects == null || objects.isEmpty()) {
      return;
    }

    PreheatIdentifier identifier = bundle.getPreheatIdentifier();

    for (T object : objects) {
      if (importMode.isCreate()) {
        if (!ctx.getAclService().canCreate(bundle.getUser(), klass)) {
          ErrorReport errorReport =
              new ErrorReport(
                  klass,
                  ErrorCode.E3000,
                  identifier.getIdentifiersWithName(bundle.getUser()),
                  identifier.getIdentifiersWithName(object));

          addReports.accept(createObjectReport(errorReport, object, bundle));
          ctx.markForRemoval(object);
          continue;
        }
      } else {
        T persistedObject = bundle.getPreheat().get(bundle.getPreheatIdentifier(), object);

        if (importMode.isUpdate()) {
          if (!ctx.getAclService().canUpdate(bundle.getUser(), persistedObject)) {
            ErrorReport errorReport =
                new ErrorReport(
                    klass,
                    ErrorCode.E3001,
                    identifier.getIdentifiersWithName(bundle.getUser()),
                    identifier.getIdentifiersWithName(object));

            addReports.accept(createObjectReport(errorReport, object, bundle));
            ctx.markForRemoval(object);
            continue;
          }
        } else if (importMode.isDelete()
            && !ctx.getAclService().canDelete(bundle.getUser(), persistedObject)) {
          ErrorReport errorReport =
              new ErrorReport(
                  klass,
                  ErrorCode.E3002,
                  identifier.getIdentifiersWithName(bundle.getUser()),
                  identifier.getIdentifiersWithName(object));

          addReports.accept(createObjectReport(errorReport, object, bundle));
          ctx.markForRemoval(object);
          continue;
        }
      }

      if (object instanceof User) {
        User user = (User) object;
        List<ErrorReport> errorReports = ctx.getUserService().validateUser(user, bundle.getUser());

        if (!errorReports.isEmpty()) {
          addReports.accept(createObjectReport(errorReports, object, bundle));
          ctx.markForRemoval(object);
        }
      }

      if (!bundle.isSkipSharing()) {
        List<ErrorReport> sharingErrorReports =
            ctx.getAclService().verifySharing(object, bundle.getUser());
        if (!sharingErrorReports.isEmpty()) {
          addReports.accept(createObjectReport(sharingErrorReports, object, bundle));
          ctx.markForRemoval(object);
        }
      }
    }
  }
}
