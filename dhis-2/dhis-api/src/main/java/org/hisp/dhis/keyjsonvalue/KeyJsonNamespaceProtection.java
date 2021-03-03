package org.hisp.dhis.keyjsonvalue;

import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Set;

/**
 * The {@link KeyJsonNamespaceProtection} is a configuration for a particular
 * namespace and the set of {@link KeyJsonValue}s in that namespace.
 *
 * Note that this is configured programmatically only.
 *
 * @author Jan Bernitt
 */
public class KeyJsonNamespaceProtection
{

    /**
     * Protection rules apply to users that do not have at least one of the
     * required {@link #authorities}.
     *
     * All superusers always have read and write access.
     */
    public enum ProtectionType
    {
        /**
         * READ/WRITE: lets any user see or modify (only exists to be used in
         * combination with one of the other two).
         */
        NONE,
        /**
         * READ: The namespace apparently does not exist. Queries come back as
         * empty or undefined.
         *
         * WRITE: Attempts to modify an entry are silently ignored.
         */
        HIDDEN,

        /**
         * READ/WRITE: Attempts to read or modify will throw an exception.
         */
        RESTRICTED
    }

    private final String namespace;

    private final boolean sharingUsed;

    private final ProtectionType reads;

    private final ProtectionType writes;

    private final Set<String> authorities;

    public KeyJsonNamespaceProtection( String namespace, ProtectionType readWrite, boolean sharingUsed,
        String... authorities )
    {
        this( namespace, readWrite, readWrite, sharingUsed, authorities );
    }

    public KeyJsonNamespaceProtection( String namespace, ProtectionType reads, ProtectionType writes,
        boolean sharingUsed, String... authorities )
    {
        this( namespace, reads, writes, sharingUsed, new HashSet<>( asList( authorities ) ) );
    }

    public KeyJsonNamespaceProtection( String namespace, ProtectionType reads, ProtectionType writes,
        boolean sharingUsed, Set<String> authorities )
    {
        this.namespace = namespace;
        this.sharingUsed = sharingUsed;
        this.reads = reads;
        this.writes = writes;
        this.authorities = authorities;
    }

    public String getNamespace()
    {
        return namespace;
    }

    /**
     * @return true when the {@link org.hisp.dhis.user.sharing.Sharing} of a
     *         {@link KeyJsonValue} should be checked for writing operations as
     *         well, when false sharing is ignored and authorities check is the
     *         only check performed.
     */
    public boolean isSharingUsed()
    {
        return sharingUsed;
    }

    public Set<String> getAuthorities()
    {
        return authorities;
    }

    public ProtectionType getReads()
    {
        return reads;
    }

    public ProtectionType getWrites()
    {
        return writes;
    }

    @Override
    public String toString()
    {
        return String.format( "KeyJsonNamespaceProtection{%s r:%s w:%s [%s]%s}",
            namespace, reads, writes, authorities, (sharingUsed ? "!" : "") );
    }
}
