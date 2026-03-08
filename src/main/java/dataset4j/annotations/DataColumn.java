package dataset4j.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Unified data column mapping annotation for structured tabular formats.
 * 
 * <pre>
 * public record Employee(
 *     @DataColumn(name = "Employee ID", order = 1, required = true,
 *                 cellType = DataColumn.CellType.TEXT, columnIndex = 0)
 *     String id,
 *     
 *     @DataColumn(name = "Salary", order = 6,
 *                 cellType = DataColumn.CellType.CURRENCY,
 *                 numberFormat = "$#,##0.00")
 *     double salary,
 *     
 *     @DataColumn(name = "Join Date", order = 7,
 *                 cellType = DataColumn.CellType.DATE,
 *                 dateFormat = "yyyy-MM-dd")
 *     String joinDate,
 *     
 *     @DataColumn(ignore = true)
 *     String internalNotes
 * ) {}
 * </pre>
 */
@Target(ElementType.RECORD_COMPONENT)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataColumn {
    
    /**
     * Column header name (defaults to field name).
     */
    String name() default "";
    
    /**
     * Column order/position for logical ordering. Lower numbers appear first.
     * Use -1 for default ordering (field declaration order).
     */
    int order() default -1;
    
    /**
     * Specific column index (A=0, B=1, etc.). Use -1 for auto-assignment.
     */
    int columnIndex() default -1;
    
    /**
     * Whether this column is required (non-null/non-empty).
     */
    boolean required() default false;
    
    /**
     * Whether to ignore this field during export/import.
     */
    boolean ignore() default false;
    
    /**
     * Optional description for documentation.
     */
    String description() default "";
    
    /**
     * Default value to use if field is null/empty during export.
     */
    String defaultValue() default "";
    
    /**
     * Whether the column should be hidden by default.
     */
    boolean hidden() default false;
    
    /**
     * Cell type for proper data formatting.
     */
    CellType cellType() default CellType.AUTO;
    
    /**
     * Number format pattern (standard format string).
     * Examples: "#,##0.00", "$#,##0.00", "0.00%"
     */
    String numberFormat() default "";
    
    /**
     * Date format pattern for date fields.
     * Examples: "yyyy-MM-dd", "MM/dd/yyyy", "dd-MMM-yyyy"
     */
    String dateFormat() default "yyyy-MM-dd";
    
    /**
     * Background color (hex format: #RRGGBB or standard color name).
     */
    String backgroundColor() default "";
    
    /**
     * Font color (hex format: #RRGGBB or standard color name).
     */
    String fontColor() default "";
    
    /**
     * Whether the column should be bold.
     */
    boolean bold() default false;
    
    /**
     * Whether the column should be frozen (freeze panes).
     */
    boolean frozen() default false;
    
    /**
     * Column width in characters. Use -1 for auto-width.
     */
    int width() default -1;
    
    /**
     * Whether to wrap text in the cell.
     */
    boolean wrapText() default false;
    
    /**
     * Text alignment in the cell.
     */
    Alignment alignment() default Alignment.AUTO;
    
    /**
     * Cell types for proper data handling.
     */
    enum CellType {
        AUTO,       // Infer from field type
        TEXT,       // String
        NUMBER,     // Numeric values
        DATE,       // Date/time
        BOOLEAN,    // Boolean
        CURRENCY,   // Currency formatting
        PERCENTAGE, // Percentage formatting
        FORMULA     // Formula/calculated field
    }
    
    /**
     * Text alignment options.
     */
    enum Alignment {
        AUTO,    // Default alignment based on data type
        LEFT,    // Left-aligned
        CENTER,  // Center-aligned
        RIGHT    // Right-aligned
    }
}