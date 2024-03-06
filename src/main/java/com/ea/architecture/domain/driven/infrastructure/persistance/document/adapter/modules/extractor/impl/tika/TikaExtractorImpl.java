package com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.modules.extractor.impl.tika;

import com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.modules.extractor.DocumentExtractor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TikaExtractorImpl implements DocumentExtractor {
    @Override
    public ImmutablePair<String, Metadata> extract(InputStream inputStream) throws TikaException, IOException, SAXException {
//        Tika tika = new Tika();
//        //extracting metadata using Tika
//        Map<String, String> metadata = extractMetadata(tika, inputStream);
//        //extracting content using Tika
//        String content = tika.parseToString(inputStream);
        return TikaUtils.extractContentAndMetadata(inputStream);
    }

    @Override
    public ImmutablePair<String, Metadata> extract(byte[] bytes) throws TikaException, IOException, SAXException {
        return this.extract(new ByteArrayInputStream(bytes));
    }
}
