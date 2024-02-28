package com.ea.architecture.domain.driven.application.document.port.command;

import com.ea.architecture.domain.driven.domain.document.entity.DocumentResult;
import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public interface DocumentManagementCommandService {
    public DocumentResult addOrUpdateDocument(DocumentAggregate documentAggregate);
}
