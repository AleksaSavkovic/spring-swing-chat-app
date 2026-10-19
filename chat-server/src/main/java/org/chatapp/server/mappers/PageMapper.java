package org.chatapp.server.mappers;

import org.chatapp.common.model.PageResponse;
import org.springframework.data.domain.Page;

public class PageMapper {

    private PageMapper() {}

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

}
