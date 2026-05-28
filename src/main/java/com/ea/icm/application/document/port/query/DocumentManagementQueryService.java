package com.ea.icm.application.document.port.query;

import com.ea.icm.domain.document.model.DocumentAggregate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public interface DocumentManagementQueryService {
    DocumentAggregate findDocumentById(String id);

    DocumentAggregate findDocumentByName(String documentName);

    DocumentAggregate findDocumentByFilter(DocumentAggregate documentAggregate);

    DocumentAggregate extractDocumentByName(String documentName);

    DocumentAggregate extractDocumentByFilter(DocumentAggregate documentAggregate);

}
