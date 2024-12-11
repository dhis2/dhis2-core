package org.hisp.dhis.importexport;

public enum ObjectBundleStatus {
  /** ObjectBundle has been created but not validated or committed. */
  CREATED,

  /** ObjectBundle has been created and validated, but not yet committed. */
  VALIDATED,

  /** ObjectBundle has been created, validated and committed. */
  COMMITTED
}
