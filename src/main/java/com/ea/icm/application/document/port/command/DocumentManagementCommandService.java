package com.ea.icm.application.document.port.command;

import com.ea.icm.domain.document.entity.DocumentResult;
import com.ea.icm.domain.document.model.DocumentAggregate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public interface DocumentManagementCommandService {
    DocumentResult addDocument(DocumentAggregate documentAggregate);

    DocumentAggregate updateDocument(DocumentAggregate documentAggregate);
}
