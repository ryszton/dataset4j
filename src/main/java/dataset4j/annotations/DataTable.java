package dataset4j.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for specifying table-level metadata for record classes.
 * 
 * <pre>
 * @DataTable(
 *     name = "Employee Report",
 *     description = "Monthly employee data export",
 *     headers = true
 * )
 * public record Employee(
 *     @Column(name = "ID", order = 1) String id,
 *     @Column(name = "Name", order = 2) String name
 * ) {}
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataTable {
    
    /**
     * Table/report name.
     */
    String name() default "";
    
    /**
     * Table description.
     */
    String description() default "";
    
    /**
     * Whether to include column headers.
     */
    boolean headers() default true;
    
    /**
     * Default date format for the table.
     */
    String defaultDateFormat() default "yyyy-MM-dd";
    
    /**
     * Default number format for the table.
     */
    String defaultNumberFormat() default "";
    
    /**
     * Whether to validate data during import.
     */
    boolean validateOnImport() default false;
    
    /**
     * Whether to skip empty rows during import.
     */
    boolean skipEmptyRows() default true;
    
    /**
     * Maximum number of rows to process (-1 for unlimited).
     */
    int maxRows() default -1;
    
    /**
     * Custom styling options.
     */
    String[] styleClasses() default {};
}