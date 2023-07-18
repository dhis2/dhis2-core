/*
 * Copyright (c) 2004-2023, University of Oslo
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dashboard.DashboardItem;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DashboardCheck implements ObjectValidationCheck {
  @Override
  public <T extends IdentifiableObject> void check(
      ObjectBundle bundle,
      Class<T> klass,
      List<T> persistedObjects,
      List<T> nonPersistedObjects,
      ImportStrategy importStrategy,
      ValidationContext context,
      Consumer<ObjectReport> addReports) {
    if (!klass.isAssignableFrom(Dashboard.class) || CollectionUtils.isEmpty(persistedObjects)) {
      return;
    }

    persistedObjects.forEach(
        dashboard -> {
          List<ErrorReport> errors = new ArrayList<>();
          checkDashboardItemHasObject(bundle, ((Dashboard) dashboard).getItems(), errors::add);
          if (!errors.isEmpty()) {
            ObjectReport objectReport = new ObjectReport(klass, 0, dashboard.getUid());
            objectReport.addErrorReports(errors);
            addReports.accept(objectReport);
          }
        });
  }

  /**
   * Check if all objects associate with given {@link DashboardItem} are available in Preheat.
   *
   * @param bundle {@link ObjectBundle}
   * @param items {@link DashboardItem} for checking.
   * @param addError add {@link ErrorCode#E4061} if cannot find associate object of DashboardItem in
   *     Preheat.
   */
  private void checkDashboardItemHasObject(
      ObjectBundle bundle, List<DashboardItem> items, Consumer<ErrorReport> addError) {
    if (CollectionUtils.isEmpty(items)) {
      return;
    }

    items.forEach(
        item -> {
          if (item.getEmbeddedItem() != null
              && bundle.getPreheat().get(bundle.getPreheatIdentifier(), item.getEmbeddedItem())
                  == null) {
            addError.accept(
                new ErrorReport(
                    DashboardItem.class,
                    ErrorCode.E4061,
                    item.getUid(),
                    item.getType(),
                    item.getEmbeddedItem().getUid()));
          }

          if (!CollectionUtils.isEmpty(item.getLinkItems())) {
            item.getLinkItems()
                .forEach(
                    linkItem -> {
                      if (bundle.getPreheat().get(bundle.getPreheatIdentifier(), linkItem)
                          == null) {
                        addError.accept(
                            new ErrorReport(
                                DashboardItem.class,
                                ErrorCode.E4061,
                                item.getUid(),
                                item.getType(),
                                linkItem.getUid()));
                      }
                    });
          }
        });
  }
}
