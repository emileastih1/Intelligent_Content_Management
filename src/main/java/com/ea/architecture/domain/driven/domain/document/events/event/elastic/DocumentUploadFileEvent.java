package com.ea.architecture.domain.driven.domain.document.events.event.elastic;

import com.ea.architecture.domain.driven.domain.document.entity.DocumentAttachment;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public final class DocumentUploadFileEvent {
    private DocumentAggregate aggregate;
    private DocumentAttachment documentAttachment;

}
