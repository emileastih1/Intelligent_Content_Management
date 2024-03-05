package com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.modules.extractor.impl.tika;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

public class TikaUtils {

    /**
     * Extracts content and metadata from the given input stream.
     *
     * @param stream
     * @return
     * @throws IOException
     * @throws TikaException
     * @throws SAXException
     */
    public static ImmutablePair<String, Metadata> extractContentAndMetadata(InputStream stream)
            throws IOException, TikaException, SAXException {

        Parser parser = new AutoDetectParser();
        ContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();

        parser.parse(stream, handler, metadata, context);
        return ImmutablePair.of(handler.toString(), metadata);
    }
}
