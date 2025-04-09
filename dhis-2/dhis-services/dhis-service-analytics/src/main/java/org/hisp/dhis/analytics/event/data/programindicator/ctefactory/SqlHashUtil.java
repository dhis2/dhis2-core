package org.hisp.dhis.analytics.event.data.programindicator.ctefactory;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Helper for generating a stable 40-character SHA-1 hash that is
 * already used for CTE keys across multiple factories.
 */
@UtilityClass
@Slf4j
public class SqlHashUtil {

    /**
     * Generates a SHA-1 hash for a given SQL string snippet, suitable for use in CTE keys.
     *
     * @param sql The SQL string to hash.
     * @return A 40-character hexadecimal SHA-1 hash string, "null_sql" if input is null, or a
     *     fallback hash if SHA-1 fails.
     */
    public static String sha1(String sql) {
        if (sql == null) {
            return "null_sql";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] messageDigest = md.digest(sql.getBytes(StandardCharsets.UTF_8));
            BigInteger no = new BigInteger(1, messageDigest);
            // Format to 40 hexadecimal characters, padded with leading zeros
            return String.format("%040x", no);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-1 Algorithm not found for SQL hashing. Falling back.", e);
            // Fallback to a simple hash code
            return "hash_error_" + String.valueOf(sql.hashCode()).replace('-', 'N');
        }
    }
}
