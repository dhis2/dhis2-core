package org.hisp.dhis.program.hibernate;

import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.program.ProgramSection;
import org.hisp.dhis.program.ProgramSectionStore;

/**
 * @author Henning Håkonsen
 */
public class HibernateProgramSectionStore
    extends HibernateIdentifiableObjectStore<ProgramSection>
    implements ProgramSectionStore
{
}