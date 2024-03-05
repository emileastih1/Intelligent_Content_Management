package com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.modules.extractor.impl;

import com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.modules.extractor.DocumentExtractor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

public class FolioReaderImpl implements DocumentExtractor {
    @Override
    public ImmutablePair<String, Metadata> extract(InputStream inputStream) {
        return null;
    }

    @Override
    public ImmutablePair<String, Metadata> extract(byte[] bytes) throws TikaException, IOException, SAXException {
        return null;
    }
}
