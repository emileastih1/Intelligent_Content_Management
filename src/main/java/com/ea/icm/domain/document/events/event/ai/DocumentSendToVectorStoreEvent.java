package com.ea.icm.domain.document.events.event.ai;

import com.ea.icm.domain.document.entity.DocumentAttachment;
import com.ea.icm.domain.document.model.DocumentAggregate;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public final class DocumentSendToVectorStoreEvent {
    private DocumentAggregate aggregate;
    private DocumentAttachment documentAttachment;

}
