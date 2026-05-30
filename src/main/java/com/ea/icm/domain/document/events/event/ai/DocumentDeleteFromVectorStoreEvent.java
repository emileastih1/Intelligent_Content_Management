package com.ea.icm.domain.document.events.event.ai;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public final class DocumentDeleteFromVectorStoreEvent {
    private long documentId;
}
