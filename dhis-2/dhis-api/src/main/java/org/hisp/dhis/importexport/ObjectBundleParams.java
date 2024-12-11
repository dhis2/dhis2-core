package org.hisp.dhis.importexport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.AtomicMode;
import org.hisp.dhis.dxf2.metadata.FlushMode;
import org.hisp.dhis.dxf2.metadata.UserOverrideMode;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReportMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.preheat.PreheatMode;
import org.hisp.dhis.preheat.PreheatParams;
import org.hisp.dhis.user.User;

@Getter
@Setter
@Accessors(chain = true)
@ToString
public class ObjectBundleParams {

  @ToString.Exclude private User user;

  @ToString.Exclude private User overrideUser;

  @ToString.Exclude
  private Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects =
      new HashMap<>();

  private UserOverrideMode userOverrideMode = UserOverrideMode.NONE;
  private ObjectBundleMode objectBundleMode = ObjectBundleMode.COMMIT;
  private PreheatIdentifier preheatIdentifier = PreheatIdentifier.UID;
  private PreheatMode preheatMode = PreheatMode.REFERENCE;
  private ImportStrategy importStrategy = ImportStrategy.CREATE_AND_UPDATE;
  private AtomicMode atomicMode = AtomicMode.ALL;
  private FlushMode flushMode = FlushMode.AUTO;
  private ImportReportMode importReportMode = ImportReportMode.ERRORS;

  private boolean skipSharing;
  private boolean skipTranslation;
  private boolean skipValidation;
  private boolean metadataSyncImport;

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
