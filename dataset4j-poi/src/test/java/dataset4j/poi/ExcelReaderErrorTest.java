package dataset4j.poi;

import dataset4j.Dataset;
import dataset4j.DatasetReadException;
import dataset4j.annotations.DataColumn;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ExcelReaderErrorTest {

    @TempDir
    Path tempDir;

    // --- Records for name-based matching ---

    public record EmployeeByName(
        @DataColumn(name = "Employee ID") int id,
        @DataColumn(name = "Full Name") String name,
        @DataColumn(name = "Hire Date", dateFormat = "yyyy-MM-dd") LocalDate hireDate
    ) {}

    public record EmployeeFieldNameOnly(
        @DataColumn int id,
        @DataColumn String name
    ) {}

    // --- Records for error reporting ---

    public record StrictEmployee(
        @DataColumn(name = "ID", order = 1) int id,
        @DataColumn(name = "Salary", order = 2) double salary,
        @DataColumn(name = "Start Date", order = 3, dateFormat = "yyyy-MM-dd") LocalDate startDate
    ) {}

    @Test
    void shouldMatchColumnsByHeaderName() throws IOException {
        Path file = createExcel("name_match.xlsx",
            new String[]{"Employee ID", "Full Name", "Hire Date"},
            new Object[][]{
                {1, "Alice", "2024-01-15"},
                {2, "Bob", "2024-06-01"}
            });

        Dataset<EmployeeByName> ds = ExcelDatasetReader.fromFile(file.toString())
            .readAs(EmployeeByName.class);

        assertEquals(2, ds.size());
        assertEquals(1, ds.get(0).id());
        assertEquals("Alice", ds.get(0).name());
        assertEquals(LocalDate.of(2024, 1, 15), ds.get(0).hireDate());
        assertEquals("Bob", ds.get(1).name());
    }

    @Test
    void shouldMatchColumnsByHeaderNameCaseInsensitive() throws IOException {
        Path file = createExcel("case_match.xlsx",
            new String[]{"employee id", "full name", "hire date"},
            new Object[][]{
                {1, "Alice", "2024-01-15"}
            });

        Dataset<EmployeeByName> ds = ExcelDatasetReader.fromFile(file.toString())
            .readAs(EmployeeByName.class);

        assertEquals(1, ds.size());
        assertEquals("Alice", ds.get(0).name());
    }

    @Test
    void shouldMatchByFieldNameWhenNoNameSpecified() throws IOException {
        Path file = createExcel("field_name.xlsx",
            new String[]{"id", "name"},
            new Object[][]{
                {42, "Charlie"}
            });

        Dataset<EmployeeFieldNameOnly> ds = ExcelDatasetReader.fromFile(file.toString())
            .readAs(EmployeeFieldNameOnly.class);

        assertEquals(1, ds.size());
        assertEquals(42, ds.get(0).id());
        assertEquals("Charlie", ds.get(0).name());
    }

    @Test
    void shouldHandleColumnsInAnyOrder() throws IOException {
        // Headers in reverse order compared to record declaration
        Path file = createExcel("reverse_order.xlsx",
            new String[]{"Hire Date", "Full Name", "Employee ID"},
            new Object[][]{
                {"2024-03-20", "Diana", 7}
            });

        Dataset<EmployeeByName> ds = ExcelDatasetReader.fromFile(file.toString())
            .readAs(EmployeeByName.class);

        assertEquals(1, ds.size());
        assertEquals(7, ds.get(0).id());
        assertEquals("Diana", ds.get(0).name());
        assertEquals(LocalDate.of(2024, 3, 20), ds.get(0).hireDate());
    }

    @Test
    void shouldThrowDatasetReadExceptionWithCellReference() throws IOException {
        Path file = createExcel("bad_data.xlsx",
            new String[]{"ID", "Salary", "Start Date"},
            new Object[][]{
                {1, 50000.0, "2024-01-01"},
                {2, 60000.0, "not-a-date"}
            });

        DatasetReadException ex = assertThrows(DatasetReadException.class, () ->
            ExcelDatasetReader.fromFile(file.toString())
                .readAs(StrictEmployee.class));

        assertEquals(2, ex.getRow());
        assertEquals(2, ex.getColumn());
        assertEquals("startDate", ex.getFieldName());
        assertEquals("StrictEmployee.startDate", ex.getQualifiedFieldName());
        assertNotNull(ex.getSheetName());
        assertTrue(ex.getCellReference().contains("C3"), "Expected cell ref C3 but got: " + ex.getCellReference());
        assertTrue(ex.getMessage().contains("not-a-date"));
        assertTrue(ex.getMessage().contains("startDate"));
        assertTrue(ex.getMessage().contains("StrictEmployee.startDate"));
    }

    @Test
    void shouldIncludeSheetNameInCellReference() throws IOException {
        Path file = createExcelWithSheet("sheet_ref.xlsx", "MyData",
            new String[]{"ID", "Salary", "Start Date"},
            new Object[][]{
                {1, "bad-number", "2024-01-01"}
            });

        DatasetReadException ex = assertThrows(DatasetReadException.class, () ->
            ExcelDatasetReader.fromFile(file.toString())
                .sheet("MyData")
                .readAs(StrictEmployee.class));

        assertEquals("MyData", ex.getSheetName());
        assertTrue(ex.getCellReference().contains("MyData!"), "Expected sheet name in ref: " + ex.getCellReference());
    }

    @Test
    void shouldReportLocationWhenStringCannotBeParsedAsDouble() throws IOException {
        // Row 0 is valid, row 1 has "hello" in the Salary (double) column
        Path file = createExcel("bad_double.xlsx",
            new String[]{"ID", "Salary", "Start Date"},
            new Object[][]{
                {1, 50000.0, "2024-01-01"},
                {2, "hello", "2024-02-01"},
                {3, 70000.0, "2024-03-01"}
            });

        DatasetReadException ex = assertThrows(DatasetReadException.class, () ->
            ExcelDatasetReader.fromFile(file.toString())
                .readAs(StrictEmployee.class));

        // Row 2 (0-based), Column 1 (B) → cell B3
        assertEquals(2, ex.getRow());
        assertEquals(1, ex.getColumn());
        assertEquals("salary", ex.getFieldName());
        assertEquals("StrictEmployee.salary", ex.getQualifiedFieldName());
        assertTrue(ex.getCellReference().contains("B3"), "Expected cell ref B3 but got: " + ex.getCellReference());
        assertTrue(ex.getMessage().contains("hello"), "Message should contain raw value 'hello': " + ex.getMessage());
        assertTrue(ex.getMessage().contains("double") || ex.getMessage().contains("Double"),
            "Message should mention target type: " + ex.getMessage());
    }

    @Test
    void shouldDefaultMissingColumnsWhenNameNotFound() throws IOException {
        // Only provide 'Employee ID' — 'Full Name' and 'Hire Date' are missing
        Path file = createExcel("partial.xlsx",
            new String[]{"Employee ID", "Other Column"},
            new Object[][]{
                {99, "ignored"}
            });

        Dataset<EmployeeByName> ds = ExcelDatasetReader.fromFile(file.toString())
            .readAs(EmployeeByName.class);

        assertEquals(1, ds.size());
        assertEquals(99, ds.get(0).id());
        assertNull(ds.get(0).name()); // default for String
        assertNull(ds.get(0).hireDate());   // default for LocalDate
    }

    // --- Helpers ---

    private Path createExcel(String filename, String[] headers, Object[][] rows) throws IOException {
        return createExcelWithSheet(filename, null, headers, rows);
    }

    private Path createExcelWithSheet(String filename, String sheetName, String[] headers, Object[][] rows) throws IOException {
        Path file = tempDir.resolve(filename);
        try (var workbook = new XSSFWorkbook();
             var fos = new FileOutputStream(file.toFile())) {
            var sheet = sheetName != null ? workbook.createSheet(sheetName) : workbook.createSheet();
            // Headers
            var headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            // Data
            for (int r = 0; r < rows.length; r++) {
                var row = sheet.createRow(r + 1);
                for (int c = 0; c < rows[r].length; c++) {
                    Object val = rows[r][c];
                    if (val instanceof Number n) {
                        row.createCell(c).setCellValue(n.doubleValue());
                    } else if (val instanceof String s) {
                        row.createCell(c).setCellValue(s);
                    } else if (val instanceof Boolean b) {
                        row.createCell(c).setCellValue(b);
                    }
                }
            }
            workbook.write(fos);
        }
        return file;
    }
}
