package com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.modules.extractor.impl;

import com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.modules.extractor.DocumentExtractor;
import com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.modules.extractor.vo.ExtractionResult;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class TikaExtractorImpl implements DocumentExtractor {
    @Override
    public ExtractionResult extract(InputStream inputStream) throws TikaException, IOException, SAXException {
        Tika tika = new Tika();
        //extracting metadata using Tika
        Map<String, String> metadata = extractMetadata(tika, inputStream);
        //extracting content using Tika
        String content = tika.parseToString(inputStream);
        return new ExtractionResult(content);
    }

    private Map<String, String> extractMetadata(Tika tika, InputStream inputStream) throws IOException, SAXException, TikaException {
        Metadata metadata = new Metadata();
        Map<String, String> map = new HashMap<>();
        for (String name : metadata.names()) {
            map.put(name, metadata.get(name));
        }
        return map;
    }
}
