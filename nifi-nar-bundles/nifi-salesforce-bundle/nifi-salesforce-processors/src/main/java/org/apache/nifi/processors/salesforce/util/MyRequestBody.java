package org.apache.nifi.processors.salesforce.util;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Sink;
import org.apache.nifi.serialization.RecordSetWriter;
import org.apache.nifi.serialization.RecordSetWriterFactory;
import org.apache.nifi.serialization.RecordWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MyRequestBody extends RequestBody {

    private final RecordSetWriterFactory writerFactory;
    private final MediaType contentType;

    public MyRequestBody(MediaType contentType, RecordSetWriterFactory writerFactory) {
        this.contentType = contentType;
        this.writerFactory = writerFactory;
    }

    @Nullable
    @Override
    public MediaType contentType() {
        return null;
    }

    @Override
    public void writeTo(@NotNull BufferedSink bufferedSink) throws IOException {
        RecordSetWriter writer = writerFactory.createWriter(logger, bufferedSink.outputStream(), schema, flowFile)
        // writer.write records
    }
}
