package com.ea.icm.domain.document.model;

import com.ea.icm.domain.document.entity.DocumentResult;
import com.ea.icm.domain.document.vo.DocumentStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ElasticDocument extends DocumentAggregate {
    private String elasticId;
    private DocumentStatus documentStatus;

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
