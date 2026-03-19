package dataset4j.poi;

import dataset4j.Dataset;
import dataset4j.annotations.DataColumn;
import dataset4j.annotations.FieldSelector;
import dataset4j.annotations.MetadataCache;
import dataset4j.annotations.PojoMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CsvDatasetWriter.
 */
class CsvSimpleTest {

    @TempDir
    Path tempDir;

    public record SimpleEmployee(
        @DataColumn(name = "ID", order = 1, required = true)
        Integer id,

        @DataColumn(name = "Name", order = 2, required = true)
        String name,

        @DataColumn(name = "Email", order = 3)
        String email,

        @DataColumn(name = "Salary", order = 4, numberFormat = "$#,##0.00")
        BigDecimal salary
    ) {}

    @Test
    void shouldWriteBasicCsv() throws IOException {
        // Given
        Path csvFile = tempDir.resolve("basic.csv");
        Dataset<SimpleEmployee> data = Dataset.of(
            new SimpleEmployee(1, "John Doe", "john@company.com", new BigDecimal("75000.50")),
            new SimpleEmployee(2, "Jane Smith", "jane@company.com", new BigDecimal("82000.00"))
        );

        // When
        CsvDatasetWriter
            .toFile(csvFile.toString())
            .write(data);

        // Then
        assertTrue(csvFile.toFile().exists());
        List<String> lines = Files.readAllLines(csvFile);
        assertEquals(3, lines.size()); // header + 2 data rows
        assertTrue(lines.get(0).contains("ID"));
        assertTrue(lines.get(0).contains("Name"));
        assertTrue(lines.get(1).contains("John Doe"));
        assertTrue(lines.get(2).contains("Jane Smith"));

        System.out.println("CSV content:");
        lines.forEach(System.out::println);
    }

    @Test
    void shouldWriteWithoutHeaders() throws IOException {
        // Given
        Path csvFile = tempDir.resolve("no_headers.csv");
        Dataset<SimpleEmployee> data = Dataset.of(
            new SimpleEmployee(1, "John Doe", "john@company.com", new BigDecimal("75000.50"))
        );

        // When
        CsvDatasetWriter
            .toFile(csvFile.toString())
            .headers(false)
            .write(data);

        // Then
        List<String> lines = Files.readAllLines(csvFile);
        assertEquals(1, lines.size()); // data only, no header
        assertTrue(lines.get(0).contains("John Doe"));
    }

    @Test
    void shouldWriteSelectedFieldsOnly() throws IOException {
        // Given
        Path csvFile = tempDir.resolve("selected_fields.csv");
        Dataset<SimpleEmployee> data = Dataset.of(
            new SimpleEmployee(1, "John Doe", "john@company.com", new BigDecimal("75000.50")),
            new SimpleEmployee(2, "Jane Smith", "jane@company.com", new BigDecimal("82000.00"))
        );

        PojoMetadata<SimpleEmployee> meta = MetadataCache.getMetadata(SimpleEmployee.class);

        // When - Write only id and name
        CsvDatasetWriter
            .toFile(csvFile.toString())
            .select(meta)
            .fields("id", "name")
            .write(data);

        // Then
        List<String> lines = Files.readAllLines(csvFile);
        assertEquals(3, lines.size());
        // Header should only have ID and Name
        assertTrue(lines.get(0).contains("ID"));
        assertTrue(lines.get(0).contains("Name"));
        assertFalse(lines.get(0).contains("Email"));
        assertFalse(lines.get(0).contains("Salary"));

        System.out.println("Selected fields CSV:");
        lines.forEach(System.out::println);
    }

    @Test
    void shouldWriteWithExcludedFields() throws IOException {
        // Given
        Path csvFile = tempDir.resolve("excluded_fields.csv");
        Dataset<SimpleEmployee> data = Dataset.of(
            new SimpleEmployee(1, "John Doe", "john@company.com", new BigDecimal("75000.50"))
        );

        PojoMetadata<SimpleEmployee> meta = MetadataCache.getMetadata(SimpleEmployee.class);

        // When - Exclude email
        CsvDatasetWriter
            .toFile(csvFile.toString())
            .select(meta)
            .exclude("email")
            .write(data);

        // Then
        List<String> lines = Files.readAllLines(csvFile);
        assertTrue(lines.get(0).contains("ID"));
        assertTrue(lines.get(0).contains("Name"));
        assertFalse(lines.get(0).contains("Email"));
        assertTrue(lines.get(0).contains("Salary"));
    }

    @Test
    void shouldWriteWithCustomSeparator() throws IOException {
        // Given
        Path csvFile = tempDir.resolve("semicolon.csv");
        Dataset<SimpleEmployee> data = Dataset.of(
            new SimpleEmployee(1, "John Doe", "john@company.com", new BigDecimal("75000.50"))
        );

        // When - Use semicolon separator
        CsvDatasetWriter
            .toFile(csvFile.toString())
            .separator(';')
            .write(data);

        // Then
        List<String> lines = Files.readAllLines(csvFile);
        assertTrue(lines.get(0).contains(";"));

        System.out.println("Semicolon-separated CSV:");
        lines.forEach(System.out::println);
    }

    @Test
    void shouldHandleNullValues() throws IOException {
        // Given
        Path csvFile = tempDir.resolve("nulls.csv");
        Dataset<SimpleEmployee> data = Dataset.of(
            new SimpleEmployee(1, "John", null, null),
            new SimpleEmployee(2, "Jane", "jane@company.com", new BigDecimal("50000"))
        );

        // When
        CsvDatasetWriter
            .toFile(csvFile.toString())
            .write(data);

        // Then
        assertTrue(csvFile.toFile().exists());
        List<String> lines = Files.readAllLines(csvFile);
        assertEquals(3, lines.size());
    }

    @Test
    void shouldHandleEmptyDataset() throws IOException {
        // Given
        Path csvFile = tempDir.resolve("empty.csv");
        Dataset<SimpleEmployee> emptyData = Dataset.empty();

        // When
        CsvDatasetWriter
            .toFile(csvFile.toString())
            .write(emptyData);

        // Then
        assertTrue(csvFile.toFile().exists());
    }

    @Test
    void shouldWriteRequiredFieldsOnly() throws IOException {
        // Given
        Path csvFile = tempDir.resolve("required_only.csv");
        Dataset<SimpleEmployee> data = Dataset.of(
            new SimpleEmployee(1, "John Doe", "john@company.com", new BigDecimal("75000.50"))
        );

        FieldSelector<SimpleEmployee> selector = FieldSelector.from(SimpleEmployee.class)
            .requiredOnly();

        // When
        CsvDatasetWriter
            .toFile(csvFile.toString())
            .select(selector)
            .write(data);

        // Then
        List<String> lines = Files.readAllLines(csvFile);
        assertTrue(lines.get(0).contains("ID"));
        assertTrue(lines.get(0).contains("Name"));
        assertFalse(lines.get(0).contains("Email"));
        assertFalse(lines.get(0).contains("Salary"));

        System.out.println("Required fields only CSV:");
        lines.forEach(System.out::println);
    }
}
