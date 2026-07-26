package eu.egm.data.common.lfsa.common;

import java.util.List;

public record CommonPage<T>(List<T> items, long total, int page, int size) {
    public CommonPage {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
