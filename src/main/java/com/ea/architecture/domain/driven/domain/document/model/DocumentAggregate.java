package com.ea.architecture.domain.driven.domain.document.model;

import com.ea.architecture.domain.driven.domain.common.model.UniqueId;
import com.ea.architecture.domain.driven.domain.document.vo.FileSize;
import lombok.*;
import org.springframework.data.domain.AbstractAggregateRoot;
import java.time.ZonedDateTime;

/**
 * This is our document aggregate root
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentAggregate extends AbstractAggregateRoot<DocumentAggregate> {
    private UniqueId id;
    private String name;
    private String owner;
    private FileSize fileSize;
    private String location;
    private ZonedDateTime creationDate;
    private ZonedDateTime modificationDate;

}
