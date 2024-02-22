package com.ea.architecture.domain.driven.application.document.port.command;

import com.ea.architecture.domain.driven.domain.document.model.DocumentAggregate;
import org.springframework.stereotype.Service;

@Service
public interface DocumentManagementCommandService {
    public String addDocument(DocumentAggregate documentAggregate);
}
