package dataset4j.annotations;

import dataset4j.examples.Employee;
import dataset4j.examples.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class FormatProviderTest {
    
    @Test
    void shouldFormatNumbersWithPattern() {
        String result = FormatProvider.formatNumber(1234.56, "#,##0.00");
        assertEquals("1,234.56", result);
        
        result = FormatProvider.formatNumber(1234.56, "$#,##0.00");
        assertEquals("$1,234.56", result);
        
        result = FormatProvider.formatNumber(0.123, "0.00%");
        assertEquals("12.30%", result);
    }
    
    @Test
    void shouldFormatDatesWithPattern() {
        LocalDate date = LocalDate.of(2024, 3, 15);
        
        String result = FormatProvider.formatDate(date, "yyyy-MM-dd");
        assertEquals("2024-03-15", result);
        
        result = FormatProvider.formatDate(date, "MM/dd/yyyy");
        assertEquals("03/15/2024", result);
        
        result = FormatProvider.formatDate(date, "dd-MMM-yyyy");
        assertEquals("15-Mar-2024", result);
    }
    
    @Test
    void shouldFormatValuesWithMetadata() {
        ColumnMetadata salaryColumn = AnnotationProcessor.findColumn(Employee.class, "salary");
        
        String result = FormatProvider.formatValue(75000.0, salaryColumn);
        // Should apply number formatting based on metadata
        assertNotNull(result);
        assertNotEquals("75000.0", result); // Should be formatted
        
        ColumnMetadata hireDateColumn = AnnotationProcessor.findColumn(Employee.class, "hireDate");
        LocalDate testDate = LocalDate.of(2024, 1, 15);
        
        result = FormatProvider.formatValue(testDate, hireDateColumn);
        assertEquals("2024-01-15", result);
    }
    
    @Test
    void shouldHandleNullValues() {
        ColumnMetadata column = ColumnMetadata.builder(Employee.class.getRecordComponents()[0])
            .build();
        
        String result = FormatProvider.formatValue(null, column);
        assertEquals("", result);
    }
    
    @Test
    void shouldTruncateStringsToMaxLength() {
        ColumnMetadata column = ColumnMetadata.builder(Employee.class.getRecordComponents()[0])
            .maxLength(10)
            .build();
        
        String result = FormatProvider.formatValue("This is a very long string", column);
        assertEquals("This is a ", result);
        assertEquals(10, result.length());
    }
    
    @Test
    void shouldParseStringValues() {
        ColumnMetadata firstNameColumn = AnnotationProcessor.findColumn(Employee.class, "firstName");
        
        Object result = FormatProvider.parseValue("John", firstNameColumn);
        assertEquals("John", result);
        
        // Test numeric parsing
        record TestRecord(@DataColumn int number) {}
        ColumnMetadata numberColumn = AnnotationProcessor.findColumn(TestRecord.class, "number");
        
        result = FormatProvider.parseValue("123", numberColumn);
        assertEquals(123, result);
    }
    
    @Test
    void shouldParseNumericTypes() {
        record NumericRecord(
            int intValue,
            long longValue,
            double doubleValue,
            float floatValue,
            boolean booleanValue
        ) {}
        
        ColumnMetadata intColumn = AnnotationProcessor.findColumn(NumericRecord.class, "intValue");
        assertEquals(42, FormatProvider.parseValue("42", intColumn));
        
        ColumnMetadata longColumn = AnnotationProcessor.findColumn(NumericRecord.class, "longValue");
        assertEquals(123456789L, FormatProvider.parseValue("123456789", longColumn));
        
        ColumnMetadata doubleColumn = AnnotationProcessor.findColumn(NumericRecord.class, "doubleValue");
        assertEquals(3.14159, (Double) FormatProvider.parseValue("3.14159", doubleColumn), 0.00001);
        
        ColumnMetadata floatColumn = AnnotationProcessor.findColumn(NumericRecord.class, "floatValue");
        assertEquals(2.5f, (Float) FormatProvider.parseValue("2.5", floatColumn), 0.0001);
        
        ColumnMetadata booleanColumn = AnnotationProcessor.findColumn(NumericRecord.class, "booleanValue");
        assertEquals(true, FormatProvider.parseValue("true", booleanColumn));
        assertEquals(false, FormatProvider.parseValue("false", booleanColumn));
    }
    
    @Test
    void shouldParseDateTypes() {
        record DateRecord(
            LocalDate localDate,
            LocalDateTime localDateTime
        ) {}
        
        ColumnMetadata localDateColumn = AnnotationProcessor.findColumn(DateRecord.class, "localDate");
        LocalDate result = (LocalDate) FormatProvider.parseValue("2024-03-15", localDateColumn);
        assertEquals(LocalDate.of(2024, 3, 15), result);
        
        ColumnMetadata localDateTimeColumn = AnnotationProcessor.findColumn(DateRecord.class, "localDateTime");
        
        // Test with full datetime format
        ColumnMetadata dateTimeColumn = ColumnMetadata.builder(localDateTimeColumn.getRecordComponent())
            .dateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .build();
        LocalDateTime dateTimeResult = (LocalDateTime) FormatProvider.parseValue("2024-03-15T10:30:00", dateTimeColumn);
        assertEquals(LocalDateTime.of(2024, 3, 15, 10, 30, 0), dateTimeResult);
        
        // Test with date-only format (should default to start of day)
        LocalDateTime dateOnlyResult = (LocalDateTime) FormatProvider.parseValue("2024-03-15", localDateTimeColumn);
        assertEquals(LocalDateTime.of(2024, 3, 15, 0, 0, 0), dateOnlyResult);
    }
    
    @Test
    void shouldHandleRequiredFieldValidationDuringParsing() {
        ColumnMetadata employeeIdColumn = AnnotationProcessor.findColumn(Employee.class, "employeeId");
        
        // Required field with empty value should throw exception
        assertThrows(IllegalArgumentException.class, 
            () -> FormatProvider.parseValue("", employeeIdColumn));
        
        assertThrows(IllegalArgumentException.class, 
            () -> FormatProvider.parseValue(null, employeeIdColumn));
    }
    
    @Test
    void shouldReturnDefaultValuesForEmptyOptionalFields() {
        ColumnMetadata column = ColumnMetadata.builder(Employee.class.getRecordComponents()[0])
            .required(false)
            .defaultValue("DEFAULT")
            .build();
        
        Object result = FormatProvider.parseValue("", column);
        assertEquals("DEFAULT", result);
        
        result = FormatProvider.parseValue(null, column);
        assertEquals("DEFAULT", result);
    }
    
    @Test
    void shouldThrowExceptionForInvalidData() {
        record NumericRecord(@DataColumn int number) {}
        ColumnMetadata numberColumn = AnnotationProcessor.findColumn(NumericRecord.class, "number");
        
        assertThrows(IllegalArgumentException.class, 
            () -> FormatProvider.parseValue("not-a-number", numberColumn));
        
        // Exception should include helpful context
        try {
            FormatProvider.parseValue("invalid", numberColumn);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("number"));
            assertTrue(e.getMessage().contains("invalid"));
        }
    }
    
    @Test
    void shouldDetectNumericAndDateTypes() {
        assertTrue(FormatProvider.isNumeric(int.class));
        assertTrue(FormatProvider.isNumeric(Integer.class));
        assertTrue(FormatProvider.isNumeric(double.class));
        assertTrue(FormatProvider.isNumeric(Double.class));
        assertTrue(FormatProvider.isNumeric(long.class));
        assertTrue(FormatProvider.isNumeric(Long.class));
        assertTrue(FormatProvider.isNumeric(float.class));
        assertTrue(FormatProvider.isNumeric(Float.class));
        
        assertFalse(FormatProvider.isNumeric(String.class));
        assertFalse(FormatProvider.isNumeric(boolean.class));
        
        assertTrue(FormatProvider.isDate(LocalDate.class));
        assertTrue(FormatProvider.isDate(LocalDateTime.class));
        assertTrue(FormatProvider.isDate(Date.class));
        
        assertFalse(FormatProvider.isDate(String.class));
        assertFalse(FormatProvider.isDate(int.class));
    }
    
    @Test
    void shouldHandleSpecialNumberFormats() {
        String result = FormatProvider.formatNumber(0.85, "0.0%");
        assertEquals("85.0%", result);
        
        result = FormatProvider.formatNumber(1234567.89, "#,##0.00");
        assertEquals("1,234,567.89", result);
        
        result = FormatProvider.formatNumber(42, "000");
        assertEquals("042", result);
    }
    
    @Test
    void shouldFormatBigDecimalCorrectly() {
        ColumnMetadata priceColumn = AnnotationProcessor.findColumn(Product.class, "price");
        BigDecimal price = new BigDecimal("19.99");
        
        String result = FormatProvider.formatValue(price, priceColumn);
        assertNotNull(result);
        // Should format according to the number format in the column metadata
    }
}