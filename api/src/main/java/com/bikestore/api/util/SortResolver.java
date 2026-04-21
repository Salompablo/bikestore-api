package com.bikestore.api.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

/**
 * Utility that converts raw {@code sortField} / {@code sortDirection} query parameters into a
 * safe {@link Pageable}, applying a per-resource whitelist and falling back to a secure default
 * when either parameter is absent or invalid.
 *
 * <p>The whitelist maps <em>public API keys</em> (lower-cased) to <em>JPA entity property names</em>.
 * A sortField that is not present in the whitelist is silently replaced by the supplied default,
 * so no raw client input ever reaches a JPQL/SQL ORDER BY clause.
 */
@Slf4j
public final class SortResolver {

    private SortResolver() {
    }

    /**
     * Builds a validated {@link Pageable} from request parameters.
     *
     * @param page             0-based page index
     * @param size             page size
     * @param sortField        raw sort field from query param (may be null or invalid)
     * @param sortDirection    raw sort direction from query param ("asc" / "desc", case-insensitive, may be null)
     * @param allowedFields    whitelist mapping lower-cased public keys to JPA property names
     * @param defaultField     JPA property name to use when {@code sortField} is absent/invalid
     * @param defaultDirection direction to use when {@code sortDirection} is absent/invalid
     * @return a validated {@link PageRequest}
     */
    public static Pageable resolve(int page, int size,
                                   String sortField, String sortDirection,
                                   Map<String, String> allowedFields,
                                   String defaultField, Sort.Direction defaultDirection) {

        Sort.Direction direction = resolveDirection(sortDirection, defaultDirection);
        String field = resolveField(sortField, allowedFields, defaultField);

        return PageRequest.of(page, size, Sort.by(direction, field));
    }

    private static Sort.Direction resolveDirection(String sortDirection, Sort.Direction defaultDirection) {
        if (sortDirection == null || sortDirection.isBlank()) {
            return defaultDirection;
        }
        try {
            return Sort.Direction.fromString(sortDirection);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid sortDirection '{}', falling back to '{}'", sortDirection, defaultDirection);
            return defaultDirection;
        }
    }

    private static String resolveField(String sortField, Map<String, String> allowedFields, String defaultField) {
        if (sortField == null || sortField.isBlank()) {
            return defaultField;
        }
        String mapped = allowedFields.get(sortField.toLowerCase());
        if (mapped == null) {
            log.warn("Invalid sortField '{}', falling back to '{}'", sortField, defaultField);
            return defaultField;
        }
        return mapped;
    }
}
