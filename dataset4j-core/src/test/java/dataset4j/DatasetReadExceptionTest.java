package dataset4j;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DatasetReadExceptionTest {

    record Employee(String name, int age) {}

    @Test
    void shouldBuildExcelStyleCellReference() {
        var ex = DatasetReadException.builder()
            .row(4).column(2).sheetName("Sheet1")
            .fieldName("hireDate").recordClass(Employee.class)
            .rawValue("bad").fieldTypeName("LocalDate")
            .build();

        assertEquals("Sheet1!C5", ex.getCellReference());
        assertEquals(4, ex.getRow());
        assertEquals(2, ex.getColumn());
        assertEquals("Sheet1", ex.getSheetName());
        assertEquals("hireDate", ex.getFieldName());
        assertEquals("Employee.hireDate", ex.getQualifiedFieldName());
        assertEquals("bad", ex.getRawValue());
        assertTrue(ex.getMessage().contains("Sheet1!C5"));
        assertTrue(ex.getMessage().contains("Employee.hireDate"));
        assertTrue(ex.getMessage().contains("LocalDate"));
    }

    @Test
    void shouldBuildCellReferenceWithoutSheet() {
        var ex = DatasetReadException.builder()
            .row(0).column(0).fieldName("id").rawValue("x")
            .build();

        assertEquals("A1", ex.getCellReference());
    }

    @Test
    void shouldBuildRowOnlyReference() {
        var ex = DatasetReadException.builder()
            .row(5).fieldName("salary").rawValue("abc")
            .build();

        assertEquals("row 5, column 'salary'", ex.getCellReference());
    }

    @Test
    void shouldConvertColumnLettersCorrectly() {
        assertEquals("A", DatasetReadException.Builder.toExcelColumnLetter(0));
        assertEquals("B", DatasetReadException.Builder.toExcelColumnLetter(1));
        assertEquals("Z", DatasetReadException.Builder.toExcelColumnLetter(25));
        assertEquals("AA", DatasetReadException.Builder.toExcelColumnLetter(26));
        assertEquals("AZ", DatasetReadException.Builder.toExcelColumnLetter(51));
        assertEquals("BA", DatasetReadException.Builder.toExcelColumnLetter(52));
    }

    @Test
    void qualifiedFieldNameWithoutRecordClass() {
        var ex = DatasetReadException.builder()
            .fieldName("foo").rawValue("bar").build();

        assertEquals("foo", ex.getQualifiedFieldName());
    }
}
