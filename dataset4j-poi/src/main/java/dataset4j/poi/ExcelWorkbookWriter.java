package dataset4j.poi;

import dataset4j.Dataset;
import dataset4j.annotations.*;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes multiple {@link Dataset} instances into a single Excel workbook,
 * one dataset per sheet.
 *
 * <p>Example:
 * <pre>{@code
 * ExcelWorkbookWriter.toFile("output.xlsx")
 *     .addSheet("SheetAScenario", aScenarioDataset)
 *     .addSheet("SheetA",         aResultsDataset)
 *     .addSheet("SheetBScenario", bScenarioDataset)
 *     .write();
 * }</pre>
 *
 * <p>Field selection defaults to all exportable fields derived from
 * {@code @DataColumn} annotations. Supply explicit field names to limit or
 * reorder columns for a specific sheet:
 * <pre>{@code
 * .addSheet("SheetA", aResultsDataset, AResults.Fields.ID, AResults.Fields.VALUE)
 * }</pre>
 *
 * <p>All sheets inherit the workbook-level {@link #headers(boolean)} and
 * {@link #autoSize(boolean)} settings.
 */
public class ExcelWorkbookWriter {

    private final String filePath;
    private boolean defaultHeaders = true;
    private boolean defaultAutoSize = true;

    private final List<SheetEntry<?>> sheets = new ArrayList<>();

    private ExcelWorkbookWriter(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Create a writer targeting {@code filePath}.
     * @param filePath path to the output {@code .xlsx} file
     * @return a new writer instance
     */
    public static ExcelWorkbookWriter toFile(String filePath) {
        Path path = Paths.get(filePath).normalize();
        if (path.toString().contains("..")) {
            throw new SecurityException("Path traversal detected in file path: " + filePath);
        }
        return new ExcelWorkbookWriter(filePath);
    }

    /**
     * Whether to write a header row on every sheet (default: {@code true}).
     */
    public ExcelWorkbookWriter headers(boolean includeHeaders) {
        this.defaultHeaders = includeHeaders;
        return this;
    }

    /**
     * Whether to auto-size columns on every sheet (default: {@code true}).
     */
    public ExcelWorkbookWriter autoSize(boolean autoSize) {
        this.defaultAutoSize = autoSize;
        return this;
    }

    /**
     * Add a sheet using all exportable fields defined by {@code @DataColumn} annotations.
     * @param sheetName name for the Excel sheet tab
     * @param dataset   data to write into this sheet
     */
    public <T> ExcelWorkbookWriter addSheet(String sheetName, Dataset<T> dataset) {
        sheets.add(new SheetEntry<>(sheetName, dataset, null));
        return this;
    }

    /**
     * Add a sheet with explicit field selection (varargs of field name constants).
     * @param sheetName  name for the Excel sheet tab
     * @param dataset    data to write into this sheet
     * @param fieldNames field name constants to include, in order
     *                   (e.g. {@code AScenario.Fields.ID, AScenario.Fields.NAME})
     */
    public <T> ExcelWorkbookWriter addSheet(String sheetName, Dataset<T> dataset, String... fieldNames) {
        sheets.add(new SheetEntry<>(sheetName, dataset, fieldNames));
        return this;
    }

    /**
     * Add a sheet with an array of field name constants (e.g. {@code AScenario.Fields.ALL_FIELDS}).
     * @param sheetName      name for the Excel sheet tab
     * @param dataset        data to write into this sheet
     * @param fieldNamesArray generated field constants array
     */
    public <T> ExcelWorkbookWriter addSheetFieldsArray(String sheetName, Dataset<T> dataset, String[] fieldNamesArray) {
        sheets.add(new SheetEntry<>(sheetName, dataset, fieldNamesArray));
        return this;
    }

    /**
     * Write all sheets to the workbook file.
     * @throws IOException if the file cannot be written
     * @throws IllegalStateException if no sheets were added
     */
    public void write() throws IOException {
        if (sheets.isEmpty()) {
            throw new IllegalStateException("No sheets added — call addSheet() before write()");
        }

        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(filePath)) {

            for (SheetEntry<?> entry : sheets) {
                renderEntry(workbook, entry);
            }

            workbook.write(fos);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void renderEntry(Workbook workbook, SheetEntry<T> entry) {
        Dataset<T> dataset = entry.dataset;

        if (dataset.isEmpty()) {
            workbook.createSheet(entry.sheetName);
            return;
        }

        Class<?> rawClass = dataset.toList().get(0).getClass();
        if (!rawClass.isRecord()) {
            throw new IllegalArgumentException(
                    "Sheet '" + entry.sheetName + "': dataset must contain record types");
        }

        PojoMetadata<T> metadata = MetadataCache.getMetadata((Class<T>) rawClass);

        List<FieldMeta> fieldsToExport;
        if (entry.fieldNames != null && entry.fieldNames.length > 0) {
            fieldsToExport = FieldSelector.from(metadata).fieldsArray(entry.fieldNames).select();
        } else {
            fieldsToExport = metadata.getExportableFields();
        }

        if (fieldsToExport.isEmpty()) {
            throw new IllegalArgumentException(
                    "Sheet '" + entry.sheetName + "': no fields selected for export");
        }

        SheetRenderConfig config = new SheetRenderConfig(
                fieldsToExport,
                defaultHeaders,
                defaultAutoSize,
                null,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap());

        ExcelSheetRenderer.renderSheet(workbook, entry.sheetName, dataset, config);
    }

    // -------------------------------------------------------------------------

    private static final class SheetEntry<T> {
        final String sheetName;
        final Dataset<T> dataset;
        final String[] fieldNames; // null → use all exportable fields

        SheetEntry(String sheetName, Dataset<T> dataset, String[] fieldNames) {
            this.sheetName = sheetName;
            this.dataset = dataset;
            this.fieldNames = fieldNames;
        }
    }
}
