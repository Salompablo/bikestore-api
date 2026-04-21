package com.bikestore.api.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SortResolver")
class SortResolverTest {

    private static final Map<String, String> FIELDS = Map.of(
            "name", "name",
            "price", "price",
            "createdat", "createdAt"
    );
    private static final String DEFAULT_FIELD = "name";
    private static final Sort.Direction DEFAULT_DIRECTION = Sort.Direction.ASC;

    // ── sortField resolution ──────────────────────────────────────────────────

    @Nested
    @DisplayName("sortField resolution")
    class SortFieldResolution {

        @Test
        @DisplayName("valid sortField is mapped to JPA property name")
        void validField_isMapped() {
            Pageable result = SortResolver.resolve(0, 10, "price", null, FIELDS, DEFAULT_FIELD, DEFAULT_DIRECTION);
            assertEquals("price", result.getSort().iterator().next().getProperty());
        }

        @Test
        @DisplayName("sortField lookup is case-insensitive")
        void fieldLookup_isCaseInsensitive() {
            Pageable result = SortResolver.resolve(0, 10, "PRICE", null, FIELDS, DEFAULT_FIELD, DEFAULT_DIRECTION);
            assertEquals("price", result.getSort().iterator().next().getProperty());
        }

        @Test
        @DisplayName("field key maps to a different JPA property name")
        void fieldKey_mapsToJpaProperty() {
            Pageable result = SortResolver.resolve(0, 10, "createdAt", null, FIELDS, DEFAULT_FIELD, DEFAULT_DIRECTION);
            assertEquals("createdAt", result.getSort().iterator().next().getProperty());
        }

        @Test
        @DisplayName("null sortField falls back to default field")
        void nullField_fallsBackToDefault() {
            Pageable result = SortResolver.resolve(0, 10, null, null, FIELDS, DEFAULT_FIELD, DEFAULT_DIRECTION);
            assertEquals(DEFAULT_FIELD, result.getSort().iterator().next().getProperty());
        }

        @Test
        @DisplayName("blank sortField falls back to default field")
        void blankField_fallsBackToDefault() {
            Pageable result = SortResolver.resolve(0, 10, "   ", null, FIELDS, DEFAULT_FIELD, DEFAULT_DIRECTION);
            assertEquals(DEFAULT_FIELD, result.getSort().iterator().next().getProperty());
        }

        @Test
        @DisplayName("invalid sortField falls back to default field (no exception thrown)")
        void invalidField_fallsBackToDefault_noException() {
            Pageable result = SortResolver.resolve(0, 10, "injected_column; DROP TABLE users--", null, FIELDS, DEFAULT_FIELD, DEFAULT_DIRECTION);
            assertEquals(DEFAULT_FIELD, result.getSort().iterator().next().getProperty());
        }

        @Test
        @DisplayName("unknown field name falls back to default without exposing raw input")
        void unknownField_fallsBackToDefault() {
            Pageable result = SortResolver.resolve(0, 10, "unknownField", null, FIELDS, DEFAULT_FIELD, DEFAULT_DIRECTION);
            assertEquals(DEFAULT_FIELD, result.getSort().iterator().next().getProperty());
            assertNotEquals("unknownField", result.getSort().iterator().next().getProperty());
        }
    }

    // ── sortDirection resolution ──────────────────────────────────────────────

    @Nested
    @DisplayName("sortDirection resolution")
    class SortDirectionResolution {

        @Test
        @DisplayName("'asc' resolves to ASCENDING")
        void asc_resolvesToAscending() {
            Pageable result = SortResolver.resolve(0, 10, null, "asc", FIELDS, DEFAULT_FIELD, DEFAULT_DIRECTION);
            assertEquals(Sort.Direction.ASC, result.getSort().iterator().next().getDirection());
        }

        @Test
        @DisplayName("'desc' resolves to DESCENDING")
        void desc_resolvesToDescending() {
            Pageable result = SortResolver.resolve(0, 10, null, "desc", FIELDS, DEFAULT_FIELD, Sort.Direction.DESC);
            assertEquals(Sort.Direction.DESC, result.getSort().iterator().next().getDirection());
        }

        @Test
        @DisplayName("direction lookup is case-insensitive")
        void directionLookup_isCaseInsensitive() {
            Pageable result = SortResolver.resolve(0, 10, null, "DESC", FIELDS, DEFAULT_FIELD, Sort.Direction.ASC);
            assertEquals(Sort.Direction.DESC, result.getSort().iterator().next().getDirection());
        }

        @Test
        @DisplayName("null sortDirection falls back to default direction")
        void nullDirection_fallsBackToDefault() {
            Pageable result = SortResolver.resolve(0, 10, null, null, FIELDS, DEFAULT_FIELD, Sort.Direction.DESC);
            assertEquals(Sort.Direction.DESC, result.getSort().iterator().next().getDirection());
        }

        @Test
        @DisplayName("blank sortDirection falls back to default direction")
        void blankDirection_fallsBackToDefault() {
            Pageable result = SortResolver.resolve(0, 10, null, "  ", FIELDS, DEFAULT_FIELD, Sort.Direction.DESC);
            assertEquals(Sort.Direction.DESC, result.getSort().iterator().next().getDirection());
        }

        @Test
        @DisplayName("invalid sortDirection falls back to default (no exception thrown)")
        void invalidDirection_fallsBackToDefault_noException() {
            Pageable result = SortResolver.resolve(0, 10, null, "sideways", FIELDS, DEFAULT_FIELD, Sort.Direction.DESC);
            assertEquals(Sort.Direction.DESC, result.getSort().iterator().next().getDirection());
        }
    }

    // ── pagination params ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("pagination params")
    class PaginationParams {

        @Test
        @DisplayName("page number and size are preserved")
        void pageAndSize_arePreserved() {
            Pageable result = SortResolver.resolve(3, 25, null, null, FIELDS, DEFAULT_FIELD, DEFAULT_DIRECTION);
            assertEquals(3, result.getPageNumber());
            assertEquals(25, result.getPageSize());
        }

        @Test
        @DisplayName("page 0, size 1 is valid")
        void page0Size1_isValid() {
            Pageable result = SortResolver.resolve(0, 1, null, null, FIELDS, DEFAULT_FIELD, DEFAULT_DIRECTION);
            assertEquals(0, result.getPageNumber());
            assertEquals(1, result.getPageSize());
        }
    }

    // ── combined scenarios ────────────────────────────────────────────────────

    @Nested
    @DisplayName("combined scenarios")
    class Combined {

        @Test
        @DisplayName("valid field and direction are both applied")
        void validFieldAndDirection_applied() {
            Pageable result = SortResolver.resolve(1, 12, "price", "desc", FIELDS, DEFAULT_FIELD, DEFAULT_DIRECTION);
            Sort.Order order = result.getSort().iterator().next();
            assertEquals("price", order.getProperty());
            assertEquals(Sort.Direction.DESC, order.getDirection());
            assertEquals(1, result.getPageNumber());
            assertEquals(12, result.getPageSize());
        }

        @Test
        @DisplayName("invalid field + valid direction => default field + valid direction")
        void invalidField_validDirection_usesDefaultFieldWithGivenDirection() {
            Pageable result = SortResolver.resolve(0, 10, "badField", "desc", FIELDS, DEFAULT_FIELD, DEFAULT_DIRECTION);
            Sort.Order order = result.getSort().iterator().next();
            assertEquals(DEFAULT_FIELD, order.getProperty());
            assertEquals(Sort.Direction.DESC, order.getDirection());
        }

        @Test
        @DisplayName("valid field + invalid direction => valid field + default direction")
        void validField_invalidDirection_usesGivenFieldWithDefaultDirection() {
            Pageable result = SortResolver.resolve(0, 10, "price", "sideways", FIELDS, DEFAULT_FIELD, Sort.Direction.DESC);
            Sort.Order order = result.getSort().iterator().next();
            assertEquals("price", order.getProperty());
            assertEquals(Sort.Direction.DESC, order.getDirection());
        }

        @Test
        @DisplayName("result is a PageRequest with a single sort order")
        void result_isPageRequestWithSingleOrder() {
            Pageable result = SortResolver.resolve(0, 10, null, null, FIELDS, DEFAULT_FIELD, DEFAULT_DIRECTION);
            assertInstanceOf(PageRequest.class, result);
            assertEquals(1, (int) result.getSort().stream().count());
        }
    }
}
