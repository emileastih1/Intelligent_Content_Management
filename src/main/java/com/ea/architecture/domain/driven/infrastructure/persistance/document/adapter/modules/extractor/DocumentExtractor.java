package com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.modules.extractor;

import com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.modules.extractor.vo.ExtractionResult;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

public interface DocumentExtractor {
    ExtractionResult extract (InputStream inputStream) throws TikaException, IOException, SAXException;
}
