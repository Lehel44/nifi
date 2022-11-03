package org.apache.nifi.processors.salesforce;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.oauth2.OAuth2AccessTokenProvider;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.processors.salesforce.util.SalesforceRestService;
import org.apache.nifi.serialization.RecordReader;
import org.apache.nifi.serialization.RecordReaderFactory;
import org.apache.nifi.serialization.RecordSetWriter;
import org.apache.nifi.serialization.RecordSetWriterFactory;
import org.apache.nifi.serialization.record.Record;
import org.apache.nifi.stream.io.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@Tags({"salesforce", "sobject", "put"})
public class PutSalesforceRecord extends AbstractProcessor {

    static final PropertyDescriptor API_URL = new PropertyDescriptor.Builder()
            .name("salesforce-url")
            .displayName("URL")
            .description("The URL for the Salesforce REST API including the domain without additional path information, such as https://MyDomainName.my.salesforce.com")
            .required(true)
            .addValidator(StandardValidators.URL_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .build();

    static final PropertyDescriptor API_VERSION = new PropertyDescriptor.Builder()
            .name("salesforce-api-version")
            .displayName("API Version")
            .description("The version number of the Salesforce REST API appended to the URL after the services/data path. See Salesforce documentation for supported versions")
            .required(true)
            .addValidator(StandardValidators.NUMBER_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .defaultValue("54.0")
            .build();

    static final PropertyDescriptor SOBJECT_NAME = new PropertyDescriptor.Builder()
            .name("sobject-name")
            .displayName("sObject Name")
            .description("The Salesforce sObject to be queried")
            .required(true)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
            .build();

    static final PropertyDescriptor FIELD_NAMES = new PropertyDescriptor.Builder()
            .name("field-names")
            .displayName("Field Names")
            .description("Comma-separated list of field names requested from the sObject to be queried")
            .required(true)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
            .build();

    static final PropertyDescriptor READ_TIMEOUT = new PropertyDescriptor.Builder()
            .name("read-timeout")
            .displayName("Read Timeout")
            .description("Maximum time allowed for reading a response from the Salesforce REST API")
            .required(true)
            .defaultValue("15 s")
            .addValidator(StandardValidators.TIME_PERIOD_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .build();

    static final PropertyDescriptor TOKEN_PROVIDER = new PropertyDescriptor.Builder()
            .name("oauth2-access-token-provider")
            .displayName("OAuth2 Access Token Provider")
            .description("Service providing OAuth2 Access Tokens for authenticating using the HTTP Authorization Header")
            .identifiesControllerService(OAuth2AccessTokenProvider.class)
            .required(true)
            .build();

    protected static final PropertyDescriptor RECORD_READER_FACTORY = new PropertyDescriptor.Builder()
            .name("record-reader")
            .displayName("Record Reader")
            .description("Specifies the Controller Service to use for parsing incoming data and determining the data's schema")
            .identifiesControllerService(RecordReaderFactory.class)
            .required(true)
            .build();

    protected static final PropertyDescriptor RECORD_WRITER_FACTORY = new PropertyDescriptor.Builder()
            .name("record-writer")
            .displayName("Record Writer")
            .description("Specifies the Controller Service to use for writing out the records.")
            .identifiesControllerService(RecordSetWriterFactory.class)
            .required(true)
            .build();

    static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("For FlowFiles created as a result of a successful query.")
            .build();

    private volatile SalesforceRestService salesforceRestService;

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return Collections.unmodifiableList(Arrays.asList(
                API_URL,
                API_VERSION,
                SOBJECT_NAME,
                FIELD_NAMES,
                READ_TIMEOUT,
                TOKEN_PROVIDER,
                RECORD_READER_FACTORY,
                RECORD_WRITER_FACTORY
        ));
    }

    @Override
    public Set<Relationship> getRelationships() {
        final Set<Relationship> relationships = new HashSet<>();
        relationships.add(REL_SUCCESS);
        return relationships;
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {

        String salesforceVersion = context.getProperty(API_VERSION).getValue();
        String baseUrl = context.getProperty(API_URL).getValue();
        OAuth2AccessTokenProvider accessTokenProvider = context.getProperty(TOKEN_PROVIDER).asControllerService(OAuth2AccessTokenProvider.class);

        salesforceRestService = new SalesforceRestService(
                salesforceVersion,
                baseUrl,
                () -> accessTokenProvider.getAccessDetails().getAccessToken(),
                context.getProperty(READ_TIMEOUT).evaluateAttributeExpressions().asTimePeriod(TimeUnit.MILLISECONDS).intValue()
        );
    }

    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
        final FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }

        final String objectType = flowFile.getAttribute("objectType");
        if (objectType == null) {
            throw new ProcessException("Object type not found");
        }

//        final byte[] buffer = new byte[(int) flowFile.getSize()];
//        session.read(flowFile, in -> StreamUtils.fillBuffer(in, buffer));
//        salesforceRestService.postRecord(objectType, buffer);

        RecordReaderFactory readerFactory = context.getProperty(RECORD_READER_FACTORY).asControllerService(RecordReaderFactory.class);
        RecordSetWriterFactory writerFactory = context.getProperty(RECORD_WRITER_FACTORY).asControllerService(RecordSetWriterFactory.class);

        try (final InputStream in = session.read(flowFile);
             final ByteArrayOutputStream out = new ByteArrayOutputStream(1000);
             final RecordReader reader = readerFactory.createRecordReader(flowFile, in, getLogger());
             final RecordSetWriter writer = writerFactory.createWriter(getLogger(), reader.getSchema(), out, flowFile)) {
            writer.beginRecordSet();
            Record record;
            while ((record = reader.nextRecord()) != null) {
                writer.write(record);
                salesforceRestService.postRecord(objectType, out.toByteArray());
                out.reset();
            }
            writer.finishRecordSet();
        } catch (Exception ex) {
            getLogger().error("Failed to put records to Salesforce.", ex);
        }
    }
}
