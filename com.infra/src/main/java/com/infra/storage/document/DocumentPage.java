package com.infra.storage.document;

import java.util.List;

/**
 * Paged document response returned by infrastructure-backed search operations.
 * The total count comes from the backing store and is not limited to the current page.
 */
public record DocumentPage<T>(List<T> content, long total, int page, int size) {
}
