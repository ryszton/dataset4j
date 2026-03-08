package dataset4j.annotations;

import dataset4j.examples.Employee;
import dataset4j.examples.Product;
import dataset4j.examples.SalesRecord;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnnotationProcessorTest {
    
    @Test
    void shouldExtractColumnsFromEmployeeRecord() {
        List<ColumnMetadata> columns = AnnotationProcessor.extractColumns(Employee.class);
        
        // Should extract all non-ignored columns
        assertEquals(10, columns.size());
        
        // Check that columns are ordered correctly
        ColumnMetadata firstColumn = columns.get(0);
        assertEquals("employeeId", firstColumn.getFieldName());
        assertEquals("Employee ID", firstColumn.getEffectiveColumnName());
        assertEquals(1, firstColumn.getOrder());
        assertTrue(firstColumn.isRequired());
        
        // Check last visible column
        ColumnMetadata lastColumn = columns.get(9);
        assertEquals("yearsOfExperience", lastColumn.getFieldName());
        assertEquals("Years of Experience", lastColumn.getEffectiveColumnName());
        assertEquals(10, lastColumn.getOrder());
    }
    
    @Test
    void shouldRespectIgnoredFields() {
        List<ColumnMetadata> columns = AnnotationProcessor.extractColumns(Employee.class);
        
        // internalNotes should be ignored
        boolean hasInternalNotes = columns.stream()
            .anyMatch(col -> "internalNotes".equals(col.getFieldName()));
        
        assertFalse(hasInternalNotes, "Ignored fields should not be included");
    }
    
    @Test
    void shouldFindSpecificColumn() {
        ColumnMetadata emailColumn = AnnotationProcessor.findColumn(Employee.class, "email");
        
        assertNotNull(emailColumn);
        assertEquals("email", emailColumn.getFieldName());
        assertEquals("Email", emailColumn.getEffectiveColumnName());
        assertEquals(4, emailColumn.getOrder());
        assertTrue(emailColumn.isRequired());
    }
    
    @Test
    void shouldReturnNullForNonExistentField() {
        ColumnMetadata nonExistent = AnnotationProcessor.findColumn(Employee.class, "nonExistentField");
        
        assertNull(nonExistent);
    }
    
    @Test
    void shouldExtractColumnHeaders() {
        List<String> headers = AnnotationProcessor.getColumnHeaders(Product.class);
        
        assertNotNull(headers);
        assertEquals(12, headers.size());
        assertEquals("SKU", headers.get(0));
        assertEquals("Product Name", headers.get(1));
        assertEquals("Tags", headers.get(11));
    }
    
    @Test
    void shouldCreateFieldToColumnMapping() {
        Map<String, String> mapping = AnnotationProcessor.getFieldToColumnMapping(SalesRecord.class);
        
        assertNotNull(mapping);
        assertEquals(13, mapping.size());
        assertEquals("Transaction ID", mapping.get("transactionId"));
        assertEquals("Sale Date", mapping.get("saleDate"));
        assertEquals("Notes", mapping.get("notes"));
    }
    
    @Test
    void shouldDetectColumnAnnotations() {
        assertTrue(AnnotationProcessor.hasColumnAnnotations(Employee.class));
        assertTrue(AnnotationProcessor.hasColumnAnnotations(Product.class));
        assertTrue(AnnotationProcessor.hasColumnAnnotations(SalesRecord.class));
    }
    
    @Test
    void shouldHandleRecordWithoutAnnotations() {
        record SimpleRecord(String name, int value) {}
        
        assertFalse(AnnotationProcessor.hasColumnAnnotations(SimpleRecord.class));
        
        List<ColumnMetadata> columns = AnnotationProcessor.extractColumns(SimpleRecord.class);
        assertEquals(0, columns.size()); // No @DataColumn annotations = no columns extracted
    }
    
    @Test
    void shouldThrowForNonRecordClass() {
        class NotARecord {
            public String field;
        }
        
        assertThrows(IllegalArgumentException.class, 
            () -> AnnotationProcessor.extractColumns(NotARecord.class));
        
        assertThrows(IllegalArgumentException.class, 
            () -> AnnotationProcessor.findColumn(NotARecord.class, "field"));
    }
    
    @Test
    void shouldHandleOrderingCorrectly() {
        record TestRecord(
            @DataColumn(order = 3) String third,
            @DataColumn(order = 1) String first,
            String unordered,
            @DataColumn(order = 2) String second
        ) {}
        
        List<ColumnMetadata> columns = AnnotationProcessor.extractColumns(TestRecord.class);
        
        assertEquals(3, columns.size()); // Only annotated fields
        assertEquals("first", columns.get(0).getFieldName());
        assertEquals("second", columns.get(1).getFieldName());
        assertEquals("third", columns.get(2).getFieldName());
    }
    
    @Test
    void shouldExtractFormattingOptions() {
        ColumnMetadata salaryColumn = AnnotationProcessor.findColumn(Employee.class, "salary");
        
        assertNotNull(salaryColumn);
        assertTrue(salaryColumn.hasFormatting());
        assertEquals("$#,##0.00", salaryColumn.getNumberFormat());
        
        ColumnMetadata hireDateColumn = AnnotationProcessor.findColumn(Employee.class, "hireDate");
        
        assertNotNull(hireDateColumn);
        assertEquals("yyyy-MM-dd", hireDateColumn.getDateFormat());
    }
    
    @Test
    void shouldDetectRequiredFields() {
        ColumnMetadata employeeIdColumn = AnnotationProcessor.findColumn(Employee.class, "employeeId");
        
        assertNotNull(employeeIdColumn);
        assertTrue(employeeIdColumn.isRequired());
    }
    
    @Test
    void shouldHandleMultipleAnnotationTypes() {
        // Test that @DataColumn contains all necessary properties
        ColumnMetadata salaryColumn = AnnotationProcessor.findColumn(Employee.class, "salary");
        
        assertNotNull(salaryColumn);
        assertEquals("Salary", salaryColumn.getEffectiveColumnName()); // From @DataColumn name
        assertEquals(6, salaryColumn.getOrder()); // From @DataColumn order
        assertEquals("$#,##0.00", salaryColumn.getNumberFormat()); // From @DataColumn numberFormat
    }
}