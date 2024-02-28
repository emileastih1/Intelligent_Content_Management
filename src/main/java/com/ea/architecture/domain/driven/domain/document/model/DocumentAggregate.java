package com.ea.architecture.domain.driven.domain.document.model;

import com.ea.architecture.domain.driven.domain.common.model.UniqueId;
import com.ea.architecture.domain.driven.domain.document.entity.DocumentAttachment;
import com.ea.architecture.domain.driven.domain.document.entity.DocumentResult;
import com.ea.architecture.domain.driven.domain.document.events.event.DocumentUploadFileEvent;
import com.ea.architecture.domain.driven.domain.document.vo.DocumentStatus;
import com.ea.architecture.domain.driven.domain.document.vo.DocumentTypes;
import com.ea.architecture.domain.driven.domain.document.vo.FileSize;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.time.ZonedDateTime;

/**
 * This is our document aggregate root
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentAggregate {
    private UniqueId id;
    private String elasticId;
    private String documentName;
    private DocumentTypes documentType;
    private byte[] file;
    private String owner;
    private FileSize fileSize;
    private String location;
    private ZonedDateTime creationDate;
    private ZonedDateTime modificationDate;
    private DocumentStatus documentStatus;

    public DocumentUploadFileEvent attachDocument(DocumentUploadCommand documentUploadCommand) {
        Assert.notNull(documentUploadCommand.elasticId(), "Document Id cannot be null");
        Assert.notNull(documentUploadCommand.file(), "File cannot be null");

        String documentId = documentUploadCommand.documentId() != null ?
                documentUploadCommand.documentId().getId() : StringUtils.EMPTY;
        DocumentAttachment documentAttachment = new DocumentAttachment(
                documentId,
                documentUploadCommand.documentName(),
                documentUploadCommand.documentType(),
                null,
                documentUploadCommand.file());
        return new DocumentUploadFileEvent(this, documentAttachment);
    }

    public void updateState(DocumentResult documentResult) {
        switch (DocumentStatus.valueOf(documentResult.status())) {
            case CREATED:
                this.documentStatus = DocumentStatus.CREATED;
                this.elasticId = documentResult.id();
                break;
            case UPDATED:
                this.documentStatus = DocumentStatus.UPDATED;
                break;
            case DELETED:
                this.documentStatus = DocumentStatus.DELETED;
                break;
            case NOT_FOUND:
                this.documentStatus = DocumentStatus.NOT_FOUND;
                break;
        }
    }
}
