package com.ea.architecture.domain.driven.infrastructure.persistance.document.adapter.utils;

import org.apache.tika.metadata.Metadata;

public class ExtractionUtils {
    public static String metadataToString(Metadata metadata) {
        StringBuilder sb = new StringBuilder();
        for (String name : metadata.names()) {
            sb.append(name).append(": ").append(metadata.get(name)).append("\n");
        }
        return sb.toString();
    }

}
