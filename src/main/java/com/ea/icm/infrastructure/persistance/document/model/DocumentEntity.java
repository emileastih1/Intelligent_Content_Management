package com.ea.icm.infrastructure.persistance.document.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;

@Getter
@Setter
@Entity
@Table(name = "document")
public class DocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "NAME", nullable = false, length = 200)
    private String name;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "CONTENT")
    private byte[] content;

    @Column(name = "TEXT_CONTENT")
    private String textContent;

    @Column(name = "FILE_SIZE")
    private String fileSize;

    @Column(name = "LOCATION")
    private String location;

    @Column(name = "CREATION_DATE")
    private ZonedDateTime creationDate;

    @Column(name = "MODIFICATION_DATE")
    private ZonedDateTime modificationDate;
}
