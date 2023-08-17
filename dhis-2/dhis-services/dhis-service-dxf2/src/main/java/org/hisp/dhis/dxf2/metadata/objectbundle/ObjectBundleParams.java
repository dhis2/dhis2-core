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
package org.hisp.dhis.dxf2.metadata.objectbundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.dxf2.metadata.AtomicMode;
import org.hisp.dhis.dxf2.metadata.FlushMode;
import org.hisp.dhis.dxf2.metadata.UserOverrideMode;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReportMode;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.preheat.PreheatMode;
import org.hisp.dhis.preheat.PreheatParams;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.user.User;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString
public class ObjectBundleParams {

  @ToString.Exclude private User user;

  @ToString.Exclude private User overrideUser;

  @ToString.Exclude
  // TODO use MetadataObjects?
  private Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects =
      new HashMap<>();

  private UserOverrideMode userOverrideMode = UserOverrideMode.NONE;
  private ObjectBundleMode objectBundleMode = ObjectBundleMode.COMMIT;
  private PreheatIdentifier preheatIdentifier = PreheatIdentifier.UID;
  private PreheatMode preheatMode = PreheatMode.REFERENCE;
  private ImportStrategy importStrategy = ImportStrategy.CREATE_AND_UPDATE;
  private AtomicMode atomicMode = AtomicMode.ALL;
  private MergeMode mergeMode = MergeMode.REPLACE;
  private FlushMode flushMode = FlushMode.AUTO;
  private ImportReportMode importReportMode = ImportReportMode.ERRORS;

  private boolean skipSharing;
  private boolean skipTranslation;
  private boolean skipValidation;
  private boolean metadataSyncImport;

  // FIXME remove
  private JobConfiguration jobId;

  public JobConfiguration getJobId() {
    return jobId;
  }

  public ObjectBundleParams setJobId(JobConfiguration jobId) {
    this.jobId = jobId;
    return this;
  }

  public Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> getObjects() {
    return objects;
  }

  public ObjectBundleParams setObjects(
      Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects) {
    this.objects = objects;
    return this;
  }

  public ObjectBundleParams addObject(
      Class<? extends IdentifiableObject> klass, IdentifiableObject object) {
    if (object == null) {
      return this;
    }

    if (!objects.containsKey(klass)) {
      objects.put(klass, new ArrayList<>());
    }

    objects.get(klass).add(object);

    return this;
  }

  @SuppressWarnings({"unchecked"})
  public ObjectBundleParams addObject(IdentifiableObject object) {
    if (object == null) {
      return this;
    }

    Class<? extends IdentifiableObject> realClass = HibernateProxyUtils.getRealClass(object);

    objects.computeIfAbsent(realClass, k -> new ArrayList<>()).add(object);

    return this;
  }

  public PreheatParams getPreheatParams() {
    PreheatParams params = new PreheatParams();
    params.setPreheatIdentifier(preheatIdentifier);
    params.setPreheatMode(preheatMode);
    return params;
  }
}
