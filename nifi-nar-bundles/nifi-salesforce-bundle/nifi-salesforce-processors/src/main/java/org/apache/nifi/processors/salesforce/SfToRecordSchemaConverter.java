package org.apache.nifi.processors.salesforce;

import org.apache.nifi.serialization.record.RecordSchema;

import java.io.IOException;

public interface SfToRecordSchemaConverter {

    RecordSchema convertSchema(final String describeSObject) throws IOException;
}
