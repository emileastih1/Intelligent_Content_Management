package com.ea.architecture.domain.driven.domain.document.model;

import com.ea.architecture.domain.driven.domain.document.entity.DocumentAttachment;
import com.ea.architecture.domain.driven.domain.document.events.event.DocumentUploadFileEvent;
import com.ea.architecture.domain.driven.domain.document.vo.DocumentStatus;
import com.ea.architecture.domain.driven.domain.document.vo.DocumentTypes;
import com.ea.architecture.domain.driven.domain.document.vo.FileSize;
import com.ea.architecture.domain.driven.domain.document.vo.FileSizeConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.util.Assert;

import java.sql.Types;
import java.time.ZonedDateTime;

/**
 * This is our document aggregate root
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "document")
public class DocumentAggregate extends AbstractAggregateRoot<DocumentAggregate> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "NAME", nullable = false, length = 200)
    private String documentName;

    @Transient
    private DocumentTypes documentType;

    @Lob
    @JdbcTypeCode(Types.VARBINARY)
    @Column(name = "CONTENT")
    private byte[] file;

    @Transient
    private String owner;

    @Column(name = "FILE_SIZE")
    @Convert(converter = FileSizeConverter.class)
    private FileSize fileSize;

    @Transient
    private String location;

    @Column(name = "CREATION_DATE")
    private ZonedDateTime creationDate;

    @Column(name = "MODIFICATION_DATE")
    private ZonedDateTime modificationDate;

    @Transient
    private DocumentStatus documentStatus;

    public void indexDocument(DocumentIndexCommand documentIndexCommand) {
        Assert.notNull(documentIndexCommand.file(), "File cannot be null");
        Assert.notNull(documentIndexCommand.documentName(), "Document name cannot be null");
        Assert.notNull(documentIndexCommand.documentType(), "Document Type be null");

        DocumentAttachment documentAttachment = new DocumentAttachment(
                0L,
                documentIndexCommand.documentName(),
                documentIndexCommand.documentType(),
                null,
                documentIndexCommand.file());
        DocumentUploadFileEvent documentUploadFileEvent = new DocumentUploadFileEvent(this, documentAttachment);
        this.registerEvent(documentUploadFileEvent);
    }
}
