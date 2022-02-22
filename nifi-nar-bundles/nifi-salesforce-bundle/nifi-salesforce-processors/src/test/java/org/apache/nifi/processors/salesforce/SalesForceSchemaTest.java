//package org.apache.nifi.processors.salesforce;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.apache.avro.Schema;
//import org.apache.avro.SchemaBuilder;
//import org.apache.avro.generic.GenericDatumReader;
//import org.apache.avro.generic.GenericDatumWriter;
//import org.apache.avro.generic.GenericRecord;
//import org.apache.avro.io.DatumReader;
//import org.apache.avro.io.Decoder;
//import org.apache.avro.io.DecoderFactory;
//import org.apache.avro.io.Encoder;
//import org.apache.avro.io.EncoderFactory;
//import org.apache.camel.component.salesforce.api.dto.SObjectDescription;
//import org.apache.camel.component.salesforce.api.dto.SObjectField;
//import org.apache.camel.component.salesforce.api.utils.JsonUtils;
//import org.apache.nifi.json.*;
//import org.apache.nifi.record.NullSuppression;
//import org.apache.nifi.schema.access.NopSchemaAccessWriter;
//import org.apache.nifi.schema.access.SchemaNotFoundException;
//import org.apache.nifi.serialization.*;
//import org.apache.nifi.serialization.record.*;
//import org.apache.nifi.util.MockComponentLog;
//import org.junit.jupiter.api.Test;
//
//import java.io.*;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.*;
//
//class SalesForceSchemaTest {
//
//    @Test
//    void testSalesforceJsonSchema() throws Exception {
//        final String describeAccount = readFile("src/test/resources/account/describe_account.json");
//        final ObjectMapper objectMapper = JsonUtils.createObjectMapper();
//        final SObjectDescription sObjectDescription = objectMapper.readValue(describeAccount, SObjectDescription.class);
//        System.out.println(JsonUtils.getSObjectJsonSchema(sObjectDescription));
//    }
//
//    @Test
//    void testSchemaValidation() throws Exception {
//        final SchemaBuilder.TypeBuilder<Schema> builder = SchemaBuilder.builder();
//        final SchemaBuilder.FieldAssembler<Schema> fieldAssembler = builder.record("salesforce").fields();
//        fieldAssembler.name("attributes").type().record("attributes2")
//                .fields()
//                .requiredString("type")
//                .requiredString("url")
//                .endRecord()
//                .noDefault();
//
//        fieldAssembler.optionalString("Id");
//        final Schema schema = fieldAssembler.endRecord();
//
//        System.out.println(schema);
//
//        final String test = readFile("src/test/resources/account/test.json");
//        final boolean b = validateJson(test, schema);
//        System.out.println("VALID: " + b);
//    }
//
//    @Test
//    void testSalesForceToJson() throws Exception {
//        final String describeAccount = readFile("src/test/resources/account/describe_account.json");
//        final ObjectMapper objectMapper = JsonUtils.createObjectMapper();
//        final SObjectDescription sObjectDescription = objectMapper.readValue(describeAccount, SObjectDescription.class);
//        final List<SObjectField> fields = sObjectDescription.getFields();
//
//        final SchemaBuilder.TypeBuilder<Schema> builder = SchemaBuilder.builder();
//        final SchemaBuilder.FieldAssembler<Schema> fieldAssembler = builder.record("salesforce").fields();
//        fieldAssembler.name("attributes").type().record("attributes")
//                .fields()
//                .requiredString("type")
//                .requiredString("url")
//                .endRecord()
//                .noDefault();
//
//        for (SObjectField field : fields) {
//
//
//            final String soapType = field.getSoapType();
//
//            switch (soapType.substring(soapType.indexOf(':') + 1)) {
//                case "ID": // mapping for tns:ID SOAP type
//                case "string":
//                case "base64Binary":
//                    // Salesforce maps any types like string, picklist, reference,
//                    // etc. to string
//                case "anyType":
//                    fieldAssembler.optionalString(field.getName());
//                    break;
//
//                case "integer":
//                case "int":
//                case "long":
//                case "short":
//                case "byte":
//                case "unsignedInt":
//                case "unsignedShort":
//                case "unsignedByte":
//                    fieldAssembler.optionalInt(field.getName());
//                    break;
//
//                case "decimal":
//                case "float":
//                case "double":
//                    fieldAssembler.optionalDouble(field.getName());
//                    break;
//
//                case "boolean":
//                    fieldAssembler.optionalBoolean(field.getName());
//                    break;
//
//                case "date":
//                    fieldAssembler.optionalString(field.getName());
//                    break;
//                case "dateTime":
//                case "g":
//                    fieldAssembler.optionalString(field.getName());
//                    break;
//                case "time":
//                    fieldAssembler.optionalString(field.getName());
//                    break;
//
//                case "address":
//                    fieldAssembler.name(field.getName()).type().record(field.getName())
//                            .fields()
//                            .optionalString("city")
//                            .optionalString("country")
//                            .optionalString("countryCode")
//                            .optionalString("postalCode")
//                            .optionalString("state")
//                            .optionalString("stateCode")
//                            .optionalString("street")
//                            .optionalString("geocodeAccuracy")
//                            .endRecord()
//                            .noDefault();
//                    break;
//
//                case "location":
//                    fieldAssembler.name(field.getName()).type().record(field.getName())
//                            .fields()
//                            .optionalString("latitude")
//                            .optionalString("longitude")
//                            .endRecord()
//                            .noDefault();
//                    break;
//
//                default:
//                    throw new IllegalArgumentException("Unsupported type " + soapType);
//            }
//
//
//        }
//
//
//        final Schema schema = fieldAssembler.endRecord();
////        System.out.println(schema);
////
//        final String accountRow = readFile("src/test/resources/account/account_row.json");
////        final boolean b = validateJson(accountRow, schema);
////        System.out.println("VALID: " + b);
//
//        byte[] avroByteArray = fromJsonToAvro(accountRow, schema);
//
//        DatumReader<GenericRecord> reader1 = new GenericDatumReader<>(schema);
//
//        Decoder decoder1 = DecoderFactory.get().binaryDecoder(avroByteArray, null);
//        GenericRecord result = reader1.read(null, decoder1);
//
//    }
//
//    @Test
//    void testSalesforceRecordSchema() throws IOException, SchemaNotFoundException, MalformedRecordException {
//        final MockComponentLog mockComponentLog = new MockComponentLog("logger", new Object());
//        final RecordSchema recordSchema = createSalesforceRecordSchema();
//        final String filePath = "src/test/resources/account/account_row.json";
//        final InputStream in = new FileInputStream(filePath);
//
//        JsonTreeRowRecordReader reader = new JsonTreeRowRecordReader(in, mockComponentLog, recordSchema, null, null, null);
//
//        final OutputStream out = new FileOutputStream("src/test/resources/account/result.json");
//        RecordSetWriter writer = new WriteJsonResult(mockComponentLog, recordSchema, new NopSchemaAccessWriter(), out, true,
//                NullSuppression.NEVER_SUPPRESS, OutputGrouping.OUTPUT_ONELINE, null, null, null);
//
//
//        //Record record;
//        //writer.beginRecordSet();
//        //while((record = reader.nextRecord()) != null) {
//        //    writer.write(record);
//        //}
//        //writer.finishRecordSet();
//
//        Record record = reader.nextRecord();
//
////        WriteResult writeResult = writer.write(RecordSet.of(record.getSchema(), record));
////
////        System.out.println(writeResult);
//
//        MockRecordWriter sad = new MockRecordWriter();
//        final RecordSetWriter writer1 = sad.createWriter(mockComponentLog, recordSchema, out, Collections.emptyMap());
//        writer1.beginRecordSet();
//        writer1.write(record);
//        writer1.finishRecordSet();
//
//
//
//        in.close();
//        out.close();
//    }
//
//    private RecordSchema createSalesforceRecordSchema() throws IOException {
//        final String describeAccount = readFile("src/test/resources/account/describe_account.json");
//        final ObjectMapper objectMapper = JsonUtils.createObjectMapper();
//        final SObjectDescription sObjectDescription = objectMapper.readValue(describeAccount, SObjectDescription.class);
//        final List<SObjectField> fields = sObjectDescription.getFields();
//
//        final List<RecordField> attributesFields = new ArrayList<>();
//        attributesFields.add(new RecordField("type", RecordFieldType.STRING.getDataType(), true));
//        attributesFields.add(new RecordField("url", RecordFieldType.STRING.getDataType(), true));
//        final RecordSchema attributesSchema = new SimpleRecordSchema(attributesFields);
//
//        final List<RecordField> recordFields = new ArrayList<>();
//        recordFields.add(new RecordField("attributes", RecordFieldType.RECORD.getRecordDataType(attributesSchema), false));
//
//        for (SObjectField field : fields) {
//            final String soapType = field.getSoapType();
//
//            switch (soapType.substring(soapType.indexOf(':') + 1)) {
//                case "ID": // mapping for tns:ID SOAP type
//                case "string":
//                case "base64Binary":
//                    // Salesforce maps any types like string, picklist, reference,
//                    // etc. to string
//                case "anyType":
//                    recordFields.add(new RecordField(field.getName(), RecordFieldType.STRING.getDataType(), field.getDefaultValue(), field.isNillable()));
//                    break;
//                case "integer":
//                case "int":
//                case "unsignedInt":
//                    recordFields.add(new RecordField(field.getName(), RecordFieldType.INT.getDataType(), field.getDefaultValue(), field.isNillable()));
//                    break;
//                case "long":
//                    recordFields.add(new RecordField(field.getName(), RecordFieldType.LONG.getDataType(), field.getDefaultValue(), field.isNillable()));
//                    break;
//                case "short":
//                case "unsignedShort":
//                    recordFields.add(new RecordField(field.getName(), RecordFieldType.SHORT.getDataType(), field.getDefaultValue(), field.isNillable()));
//                    break;
//                case "byte":
//                case "unsignedByte":
//                    recordFields.add(new RecordField(field.getName(), RecordFieldType.BYTE.getDataType(), field.getDefaultValue(), field.isNillable()));
//                    break;
//                case "decimal":
//                    recordFields.add(new RecordField(field.getName(), RecordFieldType.DECIMAL.getDataType("asd"), field.getDefaultValue(), field.isNillable()));
//                    break;
//                case "float":
//                    recordFields.add(new RecordField(field.getName(), RecordFieldType.FLOAT.getDataType(), field.getDefaultValue(), field.isNillable()));
//                    break;
//                case "double":
//                    recordFields.add(new RecordField(field.getName(), RecordFieldType.DOUBLE.getDataType(), field.getDefaultValue(), field.isNillable()));
//                    break;
//                case "boolean":
//                    recordFields.add(new RecordField(field.getName(), RecordFieldType.BOOLEAN.getDataType(), field.getDefaultValue(), field.isNillable()));
//                    break;
//                case "date":
//                    recordFields.add(new RecordField(field.getName(), RecordFieldType.DATE.getDataType(dateFormat), field.getDefaultValue(), field.isNillable()));
//                    break;
//                case "dateTime":
//                case "g":
//                    recordFields.add(new RecordField(field.getName(), RecordFieldType.TIMESTAMP.getDataType(dateTimeFormat), field.getDefaultValue(), field.isNillable()));
//                    break;
//                case "time":
//                    recordFields.add(new RecordField(field.getName(), RecordFieldType.TIME.getDataType(timeFormat), field.getDefaultValue(), field.isNillable()));
//                    break;
//                case "address":
//                    final List<RecordField> addressFields = new ArrayList<>();
//                    addressFields.add(new RecordField("city", RecordFieldType.STRING.getDataType(), true));
//                    addressFields.add(new RecordField("country", RecordFieldType.STRING.getDataType(), true));
//                    addressFields.add(new RecordField("countryCode", RecordFieldType.STRING.getDataType(), true));
//                    addressFields.add(new RecordField("postalCode", RecordFieldType.STRING.getDataType(), true));
//                    addressFields.add(new RecordField("state", RecordFieldType.STRING.getDataType(), true));
//                    addressFields.add(new RecordField("stateCode", RecordFieldType.STRING.getDataType(), true));
//                    addressFields.add(new RecordField("street", RecordFieldType.STRING.getDataType(), true));
//                    addressFields.add(new RecordField("geocodeAccuracy", RecordFieldType.STRING.getDataType(), true));
//                    final RecordSchema addressSchema = new SimpleRecordSchema(addressFields);
//                    recordFields.add(new RecordField(field.getName(), RecordFieldType.RECORD.getRecordDataType(addressSchema), field.getDefaultValue(), field.isNillable()));
//                    break;
//                case "location":
//                    final List<RecordField> locationFields = new ArrayList<>();
//                    locationFields.add(new RecordField("latitude", RecordFieldType.STRING.getDataType(), true));
//                    locationFields.add(new RecordField("longitude", RecordFieldType.STRING.getDataType(), true));
//                    final RecordSchema locationSchema = new SimpleRecordSchema(locationFields);
//                    recordFields.add(new RecordField(field.getName(), RecordFieldType.RECORD.getRecordDataType(locationSchema), field.getDefaultValue(), field.isNillable()));
//                    break;
//                case "picklist":
//                    recordFields.add(new RecordField(fi))
//                default:
//                    throw new IllegalArgumentException("Unsupported type " + soapType);
//            }
//        }
//
//        return new SimpleRecordSchema(recordFields);
//    }
//
//    private Set<String> set(final String... values) {
//        return new HashSet<>(Arrays.asList(values));
//    }
//
//    private String readFile(final String path) throws IOException {
//        final byte[] content = Files.readAllBytes(Paths.get(path));
//        return new String(content, StandardCharsets.UTF_8);
//    }
//
//    private boolean validateJson(String json, Schema schema) throws Exception {
//        InputStream input = new ByteArrayInputStream(json.getBytes());
//        DataInputStream din = new DataInputStream(input);
//
//        DatumReader reader = new GenericDatumReader(schema);
//        Decoder decoder = DecoderFactory.get().jsonDecoder(schema, din);
//        reader.read(null, decoder);
//        return true;
//
//    }
//
//    @Test
//    void testValidateJson2() throws Exception {
//        final String json = readFile("src/test/resources/account/test.json");
//
//        final SchemaBuilder.TypeBuilder<Schema> builder = SchemaBuilder.builder();
//        final SchemaBuilder.FieldAssembler<Schema> fieldAssembler = builder.record("salesforce").fields();
//        fieldAssembler.name("attributes").type().record("attributes2")
//                .fields()
//                .requiredString("type")
//                .requiredString("url")
//                .endRecord()
//                .noDefault();
//
//        fieldAssembler.optionalString("Id");
//        final Schema schema = fieldAssembler.endRecord();
//
//        byte[] avroByteArray = fromJsonToAvro(json, schema);
//
//        DatumReader<GenericRecord> reader1 = new GenericDatumReader<>(schema);
//
//        Decoder decoder1 = DecoderFactory.get().binaryDecoder(avroByteArray, null);
//        GenericRecord result = reader1.read(null, decoder1);
//        System.out.println(result);
//    }
//
//    private byte[] fromJsonToAvro(String json, Schema schema) throws Exception {
//        InputStream input = new ByteArrayInputStream(json.getBytes());
//        DataInputStream din = new DataInputStream(input);
//
//        Decoder decoder = DecoderFactory.get().jsonDecoder(schema, din);
//
//        DatumReader<Object> reader = new GenericDatumReader<>(schema);
//        Object datum = reader.read(null, decoder);
//
//        GenericDatumWriter<Object> w = new GenericDatumWriter<Object>(schema);
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//
//        Encoder e = EncoderFactory.get().binaryEncoder(outputStream, null);
//
//        w.write(datum, e);
//        e.flush();
//
//        return outputStream.toByteArray();
//
//    }
//
//}
