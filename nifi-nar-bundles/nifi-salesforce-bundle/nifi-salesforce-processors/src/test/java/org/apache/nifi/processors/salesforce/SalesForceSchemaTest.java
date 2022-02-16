package org.apache.nifi.processors.salesforce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.camel.component.salesforce.api.dto.SObjectDescription;
import org.apache.camel.component.salesforce.api.dto.SObjectField;
import org.apache.camel.component.salesforce.api.utils.JsonUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SalesForceSchemaTest {

    @Test
    void testSalesforceJsonSchema() throws Exception {
        final String describeAccount = readFile("src/test/resources/account/describe_account.json");
        final ObjectMapper objectMapper = JsonUtils.createObjectMapper();
        final SObjectDescription sObjectDescription = objectMapper.readValue(describeAccount, SObjectDescription.class);
        System.out.println(JsonUtils.getSObjectJsonSchema(sObjectDescription));
    }

    @Test
    void testSchemaValidation() throws Exception {
        final SchemaBuilder.TypeBuilder<Schema> builder = SchemaBuilder.builder();
        final SchemaBuilder.FieldAssembler<Schema> fieldAssembler = builder.record("salesforce").fields();
        fieldAssembler.name("attributes").type().record("attributes2")
                .fields()
                .requiredString("type")
                .requiredString("url")
                .endRecord()
                .noDefault();

        fieldAssembler.optionalString("Id");
        final Schema schema = fieldAssembler.endRecord();

        System.out.println(schema);

        final String test = readFile("src/test/resources/account/test.json");
        final boolean b = validateJson(test, schema);
        System.out.println("VALID: " + b);
    }

    @Test
    void testSalesForceToJson() throws Exception {
        final String describeAccount = readFile("src/test/resources/account/describe_account.json");
        final ObjectMapper objectMapper = JsonUtils.createObjectMapper();
        final SObjectDescription sObjectDescription = objectMapper.readValue(describeAccount, SObjectDescription.class);
        final List<SObjectField> fields = sObjectDescription.getFields();

        final SchemaBuilder.TypeBuilder<Schema> builder = SchemaBuilder.builder();
        final SchemaBuilder.FieldAssembler<Schema> fieldAssembler = builder.record("salesforce").fields();
        fieldAssembler.name("attributes").type().record("attributes")
                .fields()
                .requiredString("type")
                .requiredString("url")
                .endRecord()
                .noDefault();

        for (SObjectField field : fields) {


            final String soapType = field.getSoapType();

            switch (soapType.substring(soapType.indexOf(':') + 1)) {
                case "ID": // mapping for tns:ID SOAP type
                case "string":
                case "base64Binary":
                    // Salesforce maps any types like string, picklist, reference,
                    // etc. to string
                case "anyType":
                    fieldAssembler.optionalString(field.getName());
                    break;

                case "integer":
                case "int":
                case "long":
                case "short":
                case "byte":
                case "unsignedInt":
                case "unsignedShort":
                case "unsignedByte":
                    fieldAssembler.optionalInt(field.getName());
                    break;

                case "decimal":
                case "float":
                case "double":
                    fieldAssembler.optionalDouble(field.getName());
                    break;

                case "boolean":
                    fieldAssembler.optionalBoolean(field.getName());
                    break;

                case "date":
                    fieldAssembler.optionalString(field.getName());
                    break;
                case "dateTime":
                case "g":
                    fieldAssembler.optionalString(field.getName());
                    break;
                case "time":
                    fieldAssembler.optionalString(field.getName());
                    break;

                case "address":
                    fieldAssembler.name(field.getName()).type().record(field.getName())
                            .fields()
                            .optionalString("city")
                            .optionalString("country")
                            .optionalString("countryCode")
                            .optionalString("postalCode")
                            .optionalString("state")
                            .optionalString("stateCode")
                            .optionalString("street")
                            .optionalString("geocodeAccuracy")
                            .endRecord()
                            .noDefault();
                    break;

                case "location":
                    fieldAssembler.name(field.getName()).type().record(field.getName())
                            .fields()
                            .optionalString("latitude")
                            .optionalString("longitude")
                            .endRecord()
                            .noDefault();
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported type " + soapType);
            }


        }

        final Schema schema = fieldAssembler.endRecord();
//        System.out.println(schema);
//
        final String accountRow = readFile("src/test/resources/account/account_row.json");
//        final boolean b = validateJson(accountRow, schema);
//        System.out.println("VALID: " + b);

        byte[] avroByteArray = fromJsonToAvro(accountRow, schema);

        DatumReader<GenericRecord> reader1 = new GenericDatumReader<>(schema);

        Decoder decoder1 = DecoderFactory.get().binaryDecoder(avroByteArray, null);
        GenericRecord result = reader1.read(null, decoder1);

    }

    private String readFile(final String path) throws IOException {
        final byte[] content = Files.readAllBytes(Paths.get(path));
        return new String(content, StandardCharsets.UTF_8);
    }

    private boolean validateJson(String json, Schema schema) throws Exception {
        InputStream input = new ByteArrayInputStream(json.getBytes());
        DataInputStream din = new DataInputStream(input);

        DatumReader reader = new GenericDatumReader(schema);
        Decoder decoder = DecoderFactory.get().jsonDecoder(schema, din);
        reader.read(null, decoder);
        return true;

    }

    @Test
    void testValidateJson2() throws Exception {
        final String json = readFile("src/test/resources/account/test.json");

        final SchemaBuilder.TypeBuilder<Schema> builder = SchemaBuilder.builder();
        final SchemaBuilder.FieldAssembler<Schema> fieldAssembler = builder.record("salesforce").fields();
        fieldAssembler.name("attributes").type().record("attributes2")
                .fields()
                .requiredString("type")
                .requiredString("url")
                .endRecord()
                .noDefault();

        fieldAssembler.optionalString("Id");
        final Schema schema = fieldAssembler.endRecord();

        byte[] avroByteArray = fromJsonToAvro(json, schema);

        DatumReader<GenericRecord> reader1 = new GenericDatumReader<>(schema);

        Decoder decoder1 = DecoderFactory.get().binaryDecoder(avroByteArray, null);
        GenericRecord result = reader1.read(null, decoder1);
        System.out.println(result);
    }

    private byte[] fromJsonToAvro(String json, Schema schema) throws Exception {
        InputStream input = new ByteArrayInputStream(json.getBytes());
        DataInputStream din = new DataInputStream(input);

        Decoder decoder = DecoderFactory.get().jsonDecoder(schema, din);

        DatumReader<Object> reader = new GenericDatumReader<>(schema);
        Object datum = reader.read(null, decoder);

        GenericDatumWriter<Object> w = new GenericDatumWriter<Object>(schema);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Encoder e = EncoderFactory.get().binaryEncoder(outputStream, null);

        w.write(datum, e);
        e.flush();

        return outputStream.toByteArray();

    }

}
