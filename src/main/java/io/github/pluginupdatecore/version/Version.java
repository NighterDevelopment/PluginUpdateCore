package io.github.pluginupdatecore.version;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a semantic version number with support for up to 4 parts (major.minor.patch.build).
 *
 * This class provides version comparison capabilities following semantic versioning principles.
 * It can parse version strings with various formats and handle missing parts by defaulting to 0.
 *
 *
 * Supported formats:
 * <ul>
 *   <li>1.0.0</li>
 *   <li>1.0.0.1</li>
 *   <li>v1.0.0</li>
 *   <li>1.0</li>
 *   <li>1</li>
 * </ul>
 *
 *
 * @author PluginUpdateCore Team
 * @version 1.0.0
 * @since 1.0.0
 *
 *
 * <pre>{@code
 * Version v1 = new Version("1.2.3");
 * Version v2 = new Version("1.2.4");
 *
 * if (v1.compareTo(v2) < 0) {
 *     System.out.println("v1 is older than v2");
 * }
 * }</pre>
 */
public class Version implements Comparable<Version> {
    private final int[] parts;
    private static final int MAX_PARTS = 4; // Support major.minor.patch.build

    /**
     * Constructs a Version object from a version string.
     * <p>
     * The constructor parses the version string and extracts numeric parts.
     * Non-numeric prefixes (like 'v') are automatically removed.
     * Missing parts default to 0.
     * </p>
     *
     * @param version The version string to parse (e.g., "1.2.3" or "v1.2.3.4")
     * @throws NullPointerException if version is null
     *
     *
     * <pre>{@code
     * Version v1 = new Version("1.2.3");     // Parses as 1.2.3.0
     * Version v2 = new Version("v2.0");      // Parses as 2.0.0.0
     * Version v3 = new Version("1.0.0.5");   // Parses as 1.0.0.5
     * }</pre>
     */
    public Version(String version) {
        if (version == null) {
            throw new NullPointerException("Version string cannot be null");
        }

        // Remove any non-numeric prefix (e.g., "v1.0.0" -> "1.0.0")
        version = version.replaceAll("[^0-9.].*$", "")
                .replaceAll("^[^0-9]*", "");

        String[] split = version.split("\\.");
        parts = new int[MAX_PARTS]; // Initialize array with 4 parts

        // Parse each part, defaulting to 0 if not present or not a valid number
        for (int i = 0; i < MAX_PARTS; i++) {
            if (i < split.length) {
                try {
                    parts[i] = Integer.parseInt(split[i]);
                } catch (NumberFormatException e) {
                    parts[i] = 0;
                }
            } else {
                parts[i] = 0; // Fill remaining parts with 0
            }
        }
    }

    /**
     * Compares this version with another version for order.
     * <p>
     * Returns a negative integer, zero, or a positive integer as this version
     * is less than, equal to, or greater than the specified version.
     * Comparison is done part-by-part from major to build version.
     * </p>
     *
     * @param other The version to compare to
     * @return A negative integer, zero, or a positive integer as this version
     *         is less than, equal to, or greater than the specified version
     *
     *
     * <pre>{@code
     * Version v1 = new Version("1.2.3");
     * Version v2 = new Version("1.2.4");
     * Version v3 = new Version("1.2.3");
     *
     * v1.compareTo(v2); // Returns negative (v1 < v2)
     * v2.compareTo(v1); // Returns positive (v2 > v1)
     * v1.compareTo(v3); // Returns 0 (v1 == v3)
     * }</pre>
     */
    @Override
    public int compareTo(@NotNull Version other) {
        // Compare all 4 parts
        for (int i = 0; i < MAX_PARTS; i++) {
            if (parts[i] != other.parts[i]) {
                return parts[i] - other.parts[i];
            }
        }
        return 0;
    }

    /**
     * Returns the string representation of this version.
     * <p>
     * The format is "major.minor.patch" or "major.minor.patch.build" if the build part is non-zero.
     * Trailing zeros in the build part are omitted for cleaner output.
     * </p>
     *
     * @return The version string (e.g., "1.2.3" or "1.2.3.5")
     *
     *
     * <pre>{@code
     * new Version("1.2.3").toString();   // Returns "1.2.3"
     * new Version("1.2.3.0").toString(); // Returns "1.2.3"
     * new Version("1.2.3.5").toString(); // Returns "1.2.3.5"
     * }</pre>
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // Always include the first three parts
        for (int i = 0; i < 3; i++) {
            if (i > 0) sb.append('.');
            sb.append(parts[i]);
        }

        // Only include the fourth part if it's non-zero
        if (parts[3] > 0) {
            sb.append('.').append(parts[3]);
        }

        return sb.toString();
    }

    /**
     * Checks if this version is newer than another version.
     * <p>
     * This is a convenience method equivalent to {@code compareTo(other) > 0}.
     * </p>
     *
     * @param other The version to compare against
     * @return true if this version is newer, false otherwise
     *
     *
     * <pre>{@code
     * Version v1 = new Version("1.2.4");
     * Version v2 = new Version("1.2.3");
     *
     * v1.isNewerThan(v2); // Returns true
     * v2.isNewerThan(v1); // Returns false
     * }</pre>
     */
    public boolean isNewerThan(Version other) {
        return compareTo(other) > 0;
    }

    /**
     * Checks if this version is older than another version.
     * <p>
     * This is a convenience method equivalent to {@code compareTo(other) < 0}.
     * </p>
     *
     * @param other The version to compare against
     * @return true if this version is older, false otherwise
     *
     *
     * <pre>{@code
     * Version v1 = new Version("1.2.3");
     * Version v2 = new Version("1.2.4");
     *
     * v1.isOlderThan(v2); // Returns true
     * v2.isOlderThan(v1); // Returns false
     * }</pre>
     */
    public boolean isOlderThan(Version other) {
        return compareTo(other) < 0;
    }

    /**
     * Checks if this version equals another version.
     * <p>
     * This is a convenience method equivalent to {@code compareTo(other) == 0}.
     * </p>
     *
     * @param other The version to compare against
     * @return true if versions are equal, false otherwise
     *
     *
     * <pre>{@code
     * Version v1 = new Version("1.2.3");
     * Version v2 = new Version("1.2.3.0");
     * Version v3 = new Version("1.2.4");
     *
     * v1.isEqualTo(v2); // Returns true
     * v1.isEqualTo(v3); // Returns false
     * }</pre>
     */
    public boolean isEqualTo(Version other) {
        return compareTo(other) == 0;
    }

    /**
     * Gets the major version number (first part).
     *
     * @return The major version number
     */
    public int getMajor() {
        return parts[0];
    }

    /**
     * Gets the minor version number (second part).
     *
     * @return The minor version number
     */
    public int getMinor() {
        return parts[1];
    }

    /**
     * Gets the patch version number (third part).
     *
     * @return The patch version number
     */
    public int getPatch() {
        return parts[2];
    }

    /**
     * Gets the build version number (fourth part).
     *
     * @return The build version number
     */
    public int getBuild() {
        return parts[3];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Version version = (Version) o;
        return compareTo(version) == 0;
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (int part : parts) {
            result = 31 * result + part;
        }
        return result;
    }
}