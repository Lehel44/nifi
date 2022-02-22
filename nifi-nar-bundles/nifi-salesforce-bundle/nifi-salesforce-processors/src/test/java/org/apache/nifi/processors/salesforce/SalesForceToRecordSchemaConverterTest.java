package org.apache.nifi.processors.salesforce;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.apache.nifi.serialization.record.DataType;
import org.apache.nifi.serialization.record.RecordField;
import org.apache.nifi.serialization.record.RecordSchema;
import org.apache.nifi.serialization.record.type.RecordDataType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SalesForceToRecordSchemaConverterTest {

    private static final String TEST_PATH = "src/test/resources/converter/";
    private static final String DATE_FORMAT = "yyyy-mm-dd";
    private static final String DATE_TIME_FORMAT = "yyyy-mm-dd / hh:mm:ss";
    private static final String TIME_FORMAT = "hh:mm:ss";

    private static final String ATTRIBUTES_SCHEMA = "[\"type\" : \"STRING\", \"url\" : \"STRING\"]";
    private static final String ADDRESS_SCHEMA = "[\"city\" : \"STRING\", \"country\" : \"STRING\", \"countryCode\" : \"STRING\"," +
            " \"postalCode\" : \"STRING\", \"state\" : \"STRING\", \"stateCode\" : \"STRING\", \"street\" : \"STRING\", \"geocodeAccuracy\" : \"STRING\"]";
    private static final String LOCATION_SCHEMA = "[\"latitude\" : \"STRING\", \"longitude\" : \"STRING\"]";

    private SfToRecordSchemaConverter converter;

    @ParameterizedTest
    @MethodSource("provideDateTimeSfSchemas")
    void testConvertDate(final String schemaPath, final String expectedFormat, final String fieldName) throws IOException {
        final String sfSchema = readFile(schemaPath);
        converter = new StandardSfToRecordSchemaConverter(DATE_FORMAT, DATE_TIME_FORMAT, TIME_FORMAT);
        final RecordSchema recordSchema = converter.convertSchema(sfSchema);

        assertEquals(2, recordSchema.getFields().size());
        final String fieldType = recordSchema.getField(fieldName)
                .map(RecordField::getDataType)
                .map(DataType::toString)
                .orElseThrow(() -> new RuntimeException("Field not found."));
        assertEquals(expectedFormat, fieldType);
    }

    @Test
    void testConvertEmptyString() {
        final String sfSchema = "";
        converter = new StandardSfToRecordSchemaConverter(DATE_FORMAT, DATE_TIME_FORMAT, TIME_FORMAT);
        assertThrows(MismatchedInputException.class, () -> converter.convertSchema(sfSchema));
    }

    @Test
    void testConvertNullSchema() {
        final String sfSchema = null;
        converter = new StandardSfToRecordSchemaConverter(DATE_FORMAT, DATE_TIME_FORMAT, TIME_FORMAT);
        assertThrows(IllegalArgumentException.class, () -> converter.convertSchema(sfSchema));
    }

    @Test
    void testConvertUnknownDataType() throws IOException {
        final String sfSchema = readFile(TEST_PATH + "unknown_type_sf_schema.json");
        converter = new StandardSfToRecordSchemaConverter(DATE_FORMAT, DATE_TIME_FORMAT, TIME_FORMAT);
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> converter.convertSchema(sfSchema));
        assertEquals("Unsupported type xsd:unknown", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("provideComplexFieldSfSchemas")
    void testComplexFields(final String schemaPath, final String fieldName, final String expectedChildSchema) throws IOException {
        final String sfSchema = readFile(schemaPath);
        converter = new StandardSfToRecordSchemaConverter(DATE_FORMAT, DATE_TIME_FORMAT, TIME_FORMAT);
        final RecordSchema recordSchema = converter.convertSchema(sfSchema);
        final DataType fieldType = recordSchema.getField(fieldName)
                .map(RecordField::getDataType)
                .orElseThrow(() -> new RuntimeException("Field not found."));
        final RecordSchema childSchema = ((RecordDataType) fieldType).getChildSchema();
        assertEquals(2, recordSchema.getFields().size());
        assertNotNull(fieldType);
        assertEquals(expectedChildSchema, childSchema.toString());
    }


    private static Stream<Arguments> provideDateTimeSfSchemas() {
        return Stream.of(
                Arguments.of(TEST_PATH + "date_sf_schema.json", "STRING:" + DATE_FORMAT, "SLAExpirationDate__c"),
                Arguments.of(TEST_PATH + "datetime_sf_schema.json", "STRING:" + DATE_TIME_FORMAT, "CreatedDate"),
                Arguments.of(TEST_PATH + "time_sf_schema.json", "TIME:" + TIME_FORMAT, "FridayEndTime")
        );
    }

    private static Stream<Arguments> provideComplexFieldSfSchemas() {
        return Stream.of(
                Arguments.of(TEST_PATH + "date_sf_schema.json", "attributes", ATTRIBUTES_SCHEMA),
                Arguments.of(TEST_PATH + "address_sf_schema.json", "BillingAddress", ADDRESS_SCHEMA),
                Arguments.of(TEST_PATH + "location_sf_schema.json", "DestinationLocation", LOCATION_SCHEMA)
        );
    }

    private String readFile(final String path) throws IOException {
        final byte[] content = Files.readAllBytes(Paths.get(path));
        return new String(content, StandardCharsets.UTF_8);
    }

}
