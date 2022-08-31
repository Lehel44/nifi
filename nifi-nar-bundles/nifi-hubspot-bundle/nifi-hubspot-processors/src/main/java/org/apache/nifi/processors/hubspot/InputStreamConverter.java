package org.apache.nifi.processors.hubspot;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class InputStreamConverter {

    private final String json;
    private final long byteSize;

    public InputStreamConverter(String json) {
        this.json = json;
        this.byteSize = json.getBytes(StandardCharsets.UTF_8).length;
    }

    InputStream getInputStreamFromJson() {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    public long getByteSize() {
        return byteSize;
    }
}
