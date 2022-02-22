package org.apache.nifi.processors.salesforce;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.component.salesforce.api.dto.SObjectDescription;
import org.apache.camel.component.salesforce.api.dto.SObjectField;
import org.apache.camel.component.salesforce.api.utils.JsonUtils;
import org.apache.nifi.serialization.SimpleRecordSchema;
import org.apache.nifi.serialization.record.RecordField;
import org.apache.nifi.serialization.record.RecordFieldType;
import org.apache.nifi.serialization.record.RecordSchema;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SalesForceToRecordSchemaConverter {

    public RecordSchema createSalesforceRecordSchema() throws IOException {
        final String describeAccount = readFile("src/test/resources/account/describe_account.json");
        final ObjectMapper objectMapper = JsonUtils.createObjectMapper();
        final SObjectDescription sObjectDescription = objectMapper.readValue(describeAccount, SObjectDescription.class);
        final List<SObjectField> fields = sObjectDescription.getFields();

        final List<RecordField> attributesFields = new ArrayList<>();
        attributesFields.add(new RecordField("latitude", RecordFieldType.STRING.getDataType(), true));
        attributesFields.add(new RecordField("longitude", RecordFieldType.STRING.getDataType(), true));
        final RecordSchema attributesSchema = new SimpleRecordSchema(attributesFields);

        final List<RecordField> recordFields = new ArrayList<>();
        recordFields.add(new RecordField("attributes", RecordFieldType.RECORD.getRecordDataType(attributesSchema), false));

        for (SObjectField field : fields) {
            final String soapType = field.getSoapType();

            switch (soapType.substring(soapType.indexOf(':') + 1)) {
                case "ID": // mapping for tns:ID SOAP type
                case "string":
                case "base64Binary":
                    // Salesforce maps any types like string, picklist, reference,
                    // etc. to string
                case "anyType":
                    recordFields.add(new RecordField(field.getName(), RecordFieldType.STRING.getDataType(), field.getDefaultValue(), field.isNillable()));
                    break;
                case "integer":
                case "int":
                case "long":
                case "short":
                case "byte":
                case "unsignedInt":
                case "unsignedShort":
                case "unsignedByte":
                    recordFields.add(new RecordField(field.getName(), RecordFieldType.INT.getDataType(), field.getDefaultValue(), field.isNillable()));
                    break;
                case "decimal":
                case "float":
                case "double":
                    recordFields.add(new RecordField(field.getName(), RecordFieldType.DOUBLE.getDataType(), field.getDefaultValue(), field.isNillable()));
                    break;
                case "boolean":
                    recordFields.add(new RecordField(field.getName(), RecordFieldType.BOOLEAN.getDataType(), field.getDefaultValue(), field.isNillable()));
                    break;
                case "date":
                    recordFields.add(new RecordField(field.getName(), RecordFieldType.STRING.getDataType(), field.getDefaultValue(), field.isNillable()));
                    break;
                case "dateTime":
                case "g":
                    recordFields.add(new RecordField(field.getName(), RecordFieldType.STRING.getDataType(), field.getDefaultValue(), field.isNillable()));
                    break;
                case "time":
                    recordFields.add(new RecordField(field.getName(), RecordFieldType.TIME.getDataType(), field.getDefaultValue(), field.isNillable()));
                    break;
                case "address":
                    final List<RecordField> addressFields = new ArrayList<>();
                    addressFields.add(new RecordField("city", RecordFieldType.STRING.getDataType(), true));
                    addressFields.add(new RecordField("country", RecordFieldType.STRING.getDataType(), true));
                    addressFields.add(new RecordField("countryCode", RecordFieldType.STRING.getDataType(), true));
                    addressFields.add(new RecordField("postalCode", RecordFieldType.STRING.getDataType(), true));
                    addressFields.add(new RecordField("state", RecordFieldType.STRING.getDataType(), true));
                    addressFields.add(new RecordField("stateCode", RecordFieldType.STRING.getDataType(), true));
                    addressFields.add(new RecordField("street", RecordFieldType.STRING.getDataType(), true));
                    addressFields.add(new RecordField("geocodeAccuracy", RecordFieldType.STRING.getDataType(), true));
                    final RecordSchema addressSchema = new SimpleRecordSchema(addressFields);
                    recordFields.add(new RecordField(field.getName(), RecordFieldType.RECORD.getRecordDataType(addressSchema), field.getDefaultValue(), field.isNillable()));
                    break;
                case "location":
                    final List<RecordField> locationFields = new ArrayList<>();
                    locationFields.add(new RecordField("latitude", RecordFieldType.STRING.getDataType(), true));
                    locationFields.add(new RecordField("longitude", RecordFieldType.STRING.getDataType(), true));
                    final RecordSchema locationSchema = new SimpleRecordSchema(locationFields);
                    recordFields.add(new RecordField(field.getName(), RecordFieldType.RECORD.getRecordDataType(locationSchema), field.getDefaultValue(), field.isNillable()));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported type " + soapType);
            }
        }

        return new SimpleRecordSchema(recordFields);
    }

    private String readFile(final String path) throws IOException {
        final byte[] content = Files.readAllBytes(Paths.get(path));
        return new String(content, StandardCharsets.UTF_8);
    }

}
