package com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.modules.extractor;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

public interface DocumentExtractor {
    ImmutablePair<String, Metadata> extract(InputStream inputStream) throws TikaException, IOException, SAXException;

    ImmutablePair<String, Metadata> extract(byte[] bytes) throws TikaException, IOException, SAXException;
}
