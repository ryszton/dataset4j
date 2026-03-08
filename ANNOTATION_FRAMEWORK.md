# Dataset4j Annotation Framework

A comprehensive annotation system for mapping Java record fields to spreadsheet and CSV formats without external dependencies.

## Overview

The annotation framework provides a declarative way to specify how record fields should be mapped, formatted, and validated when exporting to or importing from tabular data formats (spreadsheets, CSV, etc.).

## Core Annotations

### `@Column` - Basic Field Mapping

The base annotation for field mapping with common properties:

```java
public record Employee(
    @Column(name = "Employee ID", order = 1, required = true)
    String employeeId,
    
    @Column(name = "Full Name", order = 2, defaultValue = "Unknown")
    String name
) {}
```

**Properties:**
- `name` - Column header name (defaults to field name)
- `order` - Column position (-1 for default ordering)
- `required` - Whether field is mandatory
- `ignore` - Skip field during export/import
- `description` - Documentation
- `defaultValue` - Default value for empty fields
- `maxWidth` - Maximum column width
- `hidden` - Whether column should be hidden

### `@DataColumn` - Structured Data Formatting

Enhanced structured data formatting with styling and data type options for spreadsheets:

```java
public record SalesRecord(
    @DataColumn(name = "Total Amount", columnIndex = 6,
                cellType = DataColumn.CellType.CURRENCY,
                numberFormat = "$#,##0.00", 
                backgroundColor = "#E6F3FF",
                bold = true, alignment = DataColumn.Alignment.RIGHT)
    BigDecimal totalAmount
) {}
```

**Properties:**
- `columnIndex` - Specific Excel column position (0-based)
- `cellType` - Data type (AUTO, TEXT, NUMBER, DATE, BOOLEAN, CURRENCY, PERCENTAGE, FORMULA)
- `numberFormat` - Standard number format pattern
- `dateFormat` - Date formatting pattern
- `backgroundColor` - Cell background color (hex or standard color name)
- `fontColor` - Font color
- `bold` - Bold text
- `frozen` - Freeze column
- `width` - Column width in characters
- `wrapText` - Text wrapping
- `alignment` - Text alignment (AUTO, LEFT, CENTER, RIGHT)

### `@CsvColumn` - CSV-Specific Options

CSV formatting with parsing and encoding options:

```java
public record Product(
    @CsvColumn(index = 0, quoted = true, trim = true)
    String sku,
    
    @CsvColumn(index = 3, numberFormat = "0.00", nullValue = "")
    BigDecimal price,
    
    @CsvColumn(index = 6, multiline = true, maxLength = 500)
    String description
) {}
```

**Properties:**
- `index` - CSV column position (0-based)
- `quoted` - Quote field in CSV output
- `forceQuoted` - Always quote even if not needed
- `trim` - Trim whitespace during import
- `nullValue` - Value for null fields
- `emptyValue` - Value for empty strings
- `numberFormat` - Number formatting pattern
- `dateFormat` - Date formatting pattern
- `maxLength` - Field length limit
- `multiline` - Allow line breaks
- `escapeChar` - Escape character
- `validateFormat` - Validate format during import
- `pattern` - Validation regex pattern
- `parseOptions` - Custom parsing flags

### `@TableMetadata` - Table-Level Configuration

Configure table-wide settings:

```java
@TableMetadata(
    name = "Employee Report",
    sheetName = "Employees", 
    headers = true,
    freezeHeaders = true,
    autoFilter = true,
    validateOnImport = true
)
public record Employee(...) {}
```

**Properties:**
- `name` - Table/report name
- `description` - Table description
- `sheetName` - Excel sheet name
- `headers` - Include column headers
- `freezeHeaders` - Freeze header row (Excel)
- `autoFilter` - Add auto-filter (Excel)
- `defaultDateFormat` - Default date format
- `defaultNumberFormat` - Default number format
- `validateOnImport` - Validate data during import
- `skipEmptyRows` - Skip empty rows
- `maxRows` - Maximum rows to process

### `@Validate` - Field Validation

Comprehensive validation rules:

```java
public record Customer(
    @Validate(pattern = "CUST-\\d{4}", message = "Invalid customer ID format")
    String customerId,
    
    @Validate(min = 18, max = 120, message = "Age must be between 18 and 120")
    int age,
    
    @Validate(notBlank = true, minLength = 3, maxLength = 50)
    String firstName
) {}
```

**Properties:**
- `pattern` - Regex pattern for validation
- `min/max` - Numeric range validation
- `minLength/maxLength` - String length validation
- `notBlank` - Cannot be null or empty
- `notNull` - Cannot be null
- `allowedValues` - Whitelist of valid values
- `forbiddenValues` - Blacklist of invalid values
- `message` - Custom validation message
- `ignoreErrors` - Continue processing on validation failure

## Core Classes

### `AnnotationProcessor`

Central utility for extracting metadata from annotated record classes:

```java
// Extract all column metadata
List<ColumnMetadata> columns = AnnotationProcessor.extractColumns(Employee.class);

// Find specific column
ColumnMetadata emailColumn = AnnotationProcessor.findColumn(Employee.class, "email");

// Get column headers in order
List<String> headers = AnnotationProcessor.getColumnHeaders(Employee.class);

// Get field-to-column name mapping
Map<String, String> mapping = AnnotationProcessor.getFieldToColumnMapping(Employee.class);
```

### `ColumnMetadata`

Unified metadata container with effective values from all annotations:

```java
ColumnMetadata column = AnnotationProcessor.findColumn(Employee.class, "salary");

String columnName = column.getEffectiveColumnName(); // "Salary"
int order = column.getOrder(); // 6
boolean required = column.isRequired(); // false
String numberFormat = column.getNumberFormat(); // "$#,##0.00"
boolean hasValidation = column.hasValidation(); // true
```

### `FormatProvider`

Format conversion utilities:

```java
// Format values for export
String formatted = FormatProvider.formatValue(75000.0, salaryColumn);
// Result: "$75,000.00"

// Parse values during import  
Object parsed = FormatProvider.parseValue("$75,000.00", salaryColumn);
// Result: 75000.0

// Standalone formatting
String currency = FormatProvider.formatNumber(1234.56, "$#,##0.00");
String date = FormatProvider.formatDate(LocalDate.now(), "MM/dd/yyyy");
```

### `Validator`

Field validation engine:

```java
ColumnMetadata column = AnnotationProcessor.findColumn(Employee.class, "email");
ValidationResult result = Validator.validate("invalid-email", column);

if (!result.isValid()) {
    System.out.println("Errors: " + result.getErrorsAsString());
    System.out.println("Warnings: " + result.getWarningsAsString());
}

// Combine multiple validation results
ValidationResult combined = result1.combine(result2).combine(result3);
```

## Usage Examples

### Complete Employee Record

```java
@TableMetadata(
    name = "Employee Report",
    sheetName = "Employees", 
    headers = true,
    freezeHeaders = true,
    autoFilter = true
)
public record Employee(
    @Column(name = "Employee ID", order = 1, required = true)
    @DataColumn(columnIndex = 0, cellType = DataColumn.CellType.TEXT, frozen = true)
    @CsvColumn(index = 0, quoted = true)
    @Validate(pattern = "EMP-\\d{4}", message = "Employee ID must follow pattern EMP-XXXX")
    String employeeId,
    
    @Column(name = "Email", order = 4, required = true)
    @DataColumn(columnIndex = 3, width = 30)
    @CsvColumn(index = 3, quoted = true)
    @Validate(pattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    String email,
    
    @Column(name = "Salary", order = 6)
    @DataColumn(columnIndex = 5, cellType = DataColumn.CellType.CURRENCY, 
                numberFormat = "$#,##0.00", alignment = DataColumn.Alignment.RIGHT)
    @CsvColumn(index = 5, numberFormat = "0.00")
    @Validate(min = 20000, max = 500000)
    Double salary
) {}
```

### Processing Example

```java
public class ExcelExporter {
    
    public void exportToExcel(Dataset<Employee> employees, String fileName) {
        // Extract column metadata
        List<ColumnMetadata> columns = AnnotationProcessor.extractColumns(Employee.class);
        
        // Create Excel workbook
        // ... Excel creation logic ...
        
        // Write headers
        List<String> headers = AnnotationProcessor.getColumnHeaders(Employee.class);
        // Write header row...
        
        // Write data rows
        for (Employee emp : employees.toList()) {
            for (ColumnMetadata column : columns) {
                Object value = getFieldValue(emp, column.getFieldName());
                
                // Format value
                String formatted = FormatProvider.formatValue(value, column);
                
                // Apply Excel formatting based on metadata
                // ... Excel cell formatting ...
            }
        }
    }
    
    public Dataset<Employee> importFromExcel(String fileName) {
        List<ColumnMetadata> columns = AnnotationProcessor.extractColumns(Employee.class);
        List<Employee> employees = new ArrayList<>();
        
        // Read Excel file
        // ... Excel reading logic ...
        
        for (Row row : excelRows) {
            Map<String, Object> fieldValues = new HashMap<>();
            
            for (ColumnMetadata column : columns) {
                String cellValue = getCellValue(row, column);
                
                // Validate
                ValidationResult validation = Validator.validate(cellValue, column);
                if (!validation.isValid()) {
                    // Handle validation errors
                    continue;
                }
                
                // Parse value
                Object parsed = FormatProvider.parseValue(cellValue, column);
                fieldValues.put(column.getFieldName(), parsed);
            }
            
            // Create Employee record from parsed values
            Employee employee = createEmployee(fieldValues);
            employees.add(employee);
        }
        
        return Dataset.of(employees);
    }
}
```

## Integration with Dataset4j

The annotation framework is designed to work seamlessly with Dataset4j's fluent API:

```java
Dataset<Employee> employees = Dataset.of(employeeList)
    .filter(emp -> emp.isActive())
    .sortBy(Employee::lastName);

// Export with full annotation support
ExcelExporter.export(employees, "employees.xlsx");

// Import with validation
Dataset<Employee> imported = ExcelExporter.importFromExcel("employees.xlsx");
```

## Benefits

1. **Zero Dependencies** - Pure Java with no external libraries required
2. **Type Safety** - Compile-time checking with record classes
3. **Declarative** - Configuration through annotations, not code
4. **Flexible** - Support for multiple formats with format-specific options
5. **Validation** - Built-in validation engine with custom rules
6. **Formatting** - Comprehensive formatting and parsing support
7. **Metadata-Driven** - Runtime introspection for dynamic processing
8. **Extensible** - Easy to add new annotation types and processors

This annotation framework provides a powerful, flexible foundation for mapping Java records to tabular data formats while maintaining type safety and zero external dependencies.