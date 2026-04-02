package dataset4j.poi;

import dataset4j.annotations.FieldMeta;

import java.util.List;
import java.util.Map;

/**
 * Holds per-sheet rendering configuration for {@link ExcelSheetRenderer}.
 */
final class SheetRenderConfig {

    final List<FieldMeta> fieldsToExport;
    final boolean includeHeaders;
    final boolean autoSizeColumns;
    final CellWriter globalCellWriter;
    final Map<String, CellWriter> fieldCellWriters;
    final Map<Class<?>, Object> typeDefaults;
    final Map<String, Object> fieldDefaults;

    SheetRenderConfig(
            List<FieldMeta> fieldsToExport,
            boolean includeHeaders,
            boolean autoSizeColumns,
            CellWriter globalCellWriter,
            Map<String, CellWriter> fieldCellWriters,
            Map<Class<?>, Object> typeDefaults,
            Map<String, Object> fieldDefaults) {
        this.fieldsToExport = fieldsToExport;
        this.includeHeaders = includeHeaders;
        this.autoSizeColumns = autoSizeColumns;
        this.globalCellWriter = globalCellWriter;
        this.fieldCellWriters = fieldCellWriters;
        this.typeDefaults = typeDefaults;
        this.fieldDefaults = fieldDefaults;
    }
}
