package org.hisp.dhis.common;

public interface SoftDeletableEntity extends IdentifiableObject {

  boolean isDeleted();

  void setDeleted(boolean deleted);
}
