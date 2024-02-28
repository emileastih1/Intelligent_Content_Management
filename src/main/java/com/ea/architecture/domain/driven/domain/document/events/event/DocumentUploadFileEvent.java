package com.ea.architecture.domain.driven.domain.document.events.event;

import com.ea.architecture.domain.driven.domain.document.entity.DocumentAttachment;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;

public record DocumentUploadFileEvent(DocumentAggregate aggregate, DocumentAttachment documentAttachment)
{}
