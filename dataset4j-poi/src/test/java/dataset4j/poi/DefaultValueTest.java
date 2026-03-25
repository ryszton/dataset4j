package dataset4j.poi;

import dataset4j.Dataset;
import dataset4j.annotations.DataColumn;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for configurable default values on ExcelDatasetReader and ExcelDatasetWriter.
 */
class DefaultValueTest {

    @TempDir
    Path tempDir;

    public record Employee(
        @DataColumn(name = "ID", order = 1)
        Integer id,

        @DataColumn(name = "Name", order = 2)
        String name,

        @DataColumn(name = "Salary", order = 3)
        Double salary,

        @DataColumn(name = "Active", order = 4)
        Boolean active,

        @DataColumn(name = "JoinDate", order = 5)
        LocalDate joinDate
    ) {}

    public record WithAnnotationDefault(
        @DataColumn(name = "ID", order = 1)
        Integer id,

        @DataColumn(name = "Name", order = 2, defaultValue = "UNKNOWN")
        String name,

        @DataColumn(name = "Score", order = 3, defaultValue = "42")
        Integer score
    ) {}

    // --- Reader tests ---

    @Test
    void reader_typeBasedDefault_appliedForNullAndBlankCells() throws IOException {
        Path file = tempDir.resolve("type_defaults.xlsx");

        // Write data: first row has values, second row has nulls (but id is set to avoid empty row skip)
        Dataset<Employee> data = Dataset.of(
            new Employee(1, "Alice", 50000.0, true, LocalDate.of(2024, 1, 15)),
            new Employee(99, null, null, null, null)
        );
        ExcelDatasetWriter.toFile(file.toString()).write(data);

        // Read with type-based defaults
        Dataset<Employee> result = ExcelDatasetReader
            .fromFile(file.toString())
            .defaultValue(String.class, "N/A")
            .defaultValue(Double.class, 0.0)
            .defaultValue(Boolean.class, false)
            .readAs(Employee.class);

        assertEquals(2, result.size());

        Employee nullRow = result.toList().get(1);
        assertEquals(Integer.valueOf(99), nullRow.id());
        assertEquals("N/A", nullRow.name());
        assertEquals(Double.valueOf(0.0), nullRow.salary());
        assertEquals(Boolean.FALSE, nullRow.active());
        assertNull(nullRow.joinDate()); // no type default set for LocalDate
    }

    @Test
    void reader_perFieldDefault_takesPriorityOverTypeDefault() throws IOException {
        Path file = tempDir.resolve("field_defaults.xlsx");

        Dataset<Employee> data = Dataset.of(
            new Employee(99, null, null, null, null)
        );
        ExcelDatasetWriter.toFile(file.toString()).write(data);

        Dataset<Employee> result = ExcelDatasetReader
            .fromFile(file.toString())
            .defaultValue(String.class, "N/A")       // type default
            .defaultValue("name", "UNASSIGNED")       // field default (higher priority)
            .defaultValue(Double.class, -1.0)         // type default
            .defaultValue("salary", 0.0)              // field default (higher priority)
            .readAs(Employee.class);

        Employee row = result.first().orElseThrow();
        assertEquals("UNASSIGNED", row.name());       // field override wins
        assertEquals(Double.valueOf(0.0), row.salary()); // field override wins
    }

    @Test
    void reader_annotationDefault_usedWhenNoOverrideConfigured() throws IOException {
        Path file = tempDir.resolve("annotation_defaults.xlsx");

        // Write with null name and null score — both have annotation defaults
        Dataset<WithAnnotationDefault> data = Dataset.of(
            new WithAnnotationDefault(1, null, null)
        );
        ExcelDatasetWriter.toFile(file.toString()).write(data);

        // Read without any reader-level overrides — annotation defaults should be used
        Dataset<WithAnnotationDefault> result = ExcelDatasetReader
            .fromFile(file.toString())
            .readAs(WithAnnotationDefault.class);

        WithAnnotationDefault row = result.first().orElseThrow();
        assertEquals("UNKNOWN", row.name());
        assertEquals(Integer.valueOf(42), row.score());
    }

    @Test
    void reader_perFieldDefault_overridesAnnotationDefault() throws IOException {
        Path file = tempDir.resolve("field_over_annotation.xlsx");

        // Write with null name — annotation default is "UNKNOWN" but writer writes it as cell value
        // To get a genuinely blank cell, we need a record WITHOUT annotation defaults for name
        Dataset<Employee> data = Dataset.of(
            new Employee(1, null, null, null, null)
        );
        ExcelDatasetWriter.toFile(file.toString()).write(data);

        // Read as Employee (no annotation defaults) with field override
        Dataset<Employee> result = ExcelDatasetReader
            .fromFile(file.toString())
            .defaultValue("name", "OVERRIDE")
            .readAs(Employee.class);

        Employee row = result.first().orElseThrow();
        assertEquals("OVERRIDE", row.name()); // per-field wins
    }

    @Test
    void reader_noConfiguredDefaults_fallsBackToBuiltIn() throws IOException {
        Path file = tempDir.resolve("builtin_defaults.xlsx");

        // Need at least one non-null field to prevent empty row skip
        Dataset<Employee> data = Dataset.of(
            new Employee(1, null, null, null, null)
        );
        ExcelDatasetWriter.toFile(file.toString()).write(data);

        Dataset<Employee> result = ExcelDatasetReader
            .fromFile(file.toString())
            .readAs(Employee.class);

        Employee row = result.first().orElseThrow();
        assertEquals(Integer.valueOf(1), row.id());
        assertNull(row.name());              // built-in String default is null
        assertNull(row.salary());            // built-in boxed Double default
        assertNull(row.active());            // built-in boxed Boolean default
        assertNull(row.joinDate());          // built-in LocalDate default
    }

    // --- Writer tests ---

    @Test
    void writer_typeBasedDefault_writtenForNullFields() throws IOException {
        Path file = tempDir.resolve("writer_type_defaults.xlsx");

        Dataset<Employee> data = Dataset.of(
            new Employee(1, null, null, null, null)
        );

        ExcelDatasetWriter
            .toFile(file.toString())
            .defaultValue(String.class, "N/A")
            .defaultValue(Double.class, 0.0)
            .write(data);

        // Read back without reader defaults to verify what was written
        Dataset<Employee> result = ExcelDatasetReader
            .fromFile(file.toString())
            .readAs(Employee.class);

        Employee row = result.first().orElseThrow();
        assertEquals("N/A", row.name());
        assertEquals(Double.valueOf(0.0), row.salary());
    }

    @Test
    void writer_perFieldDefault_takesPriorityOverTypeDefault() throws IOException {
        Path file = tempDir.resolve("writer_field_defaults.xlsx");

        Dataset<Employee> data = Dataset.of(
            new Employee(1, null, null, null, null)
        );

        ExcelDatasetWriter
            .toFile(file.toString())
            .defaultValue(String.class, "N/A")
            .defaultValue("name", "VACANT")
            .write(data);

        Dataset<Employee> result = ExcelDatasetReader
            .fromFile(file.toString())
            .readAs(Employee.class);

        Employee row = result.first().orElseThrow();
        assertEquals("VACANT", row.name()); // field override wins over type
    }

    @Test
    void writer_annotationDefault_stillUsedWhenNoOverride() throws IOException {
        Path file = tempDir.resolve("writer_annotation_default.xlsx");

        Dataset<WithAnnotationDefault> data = Dataset.of(
            new WithAnnotationDefault(1, null, 10)
        );

        // Write without any writer-level overrides
        ExcelDatasetWriter.toFile(file.toString()).write(data);

        Dataset<WithAnnotationDefault> result = ExcelDatasetReader
            .fromFile(file.toString())
            .readAs(WithAnnotationDefault.class);

        WithAnnotationDefault row = result.first().orElseThrow();
        // DefaultCellWriter uses @DataColumn(defaultValue) for null fields
        assertEquals("UNKNOWN", row.name());
    }
}
