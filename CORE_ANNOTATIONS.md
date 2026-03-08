# Dataset4j Core Annotations

A lightweight annotation system for mapping Java record fields to tabular data formats without external dependencies.

## Overview

The core annotation framework provides the essential building blocks for declarative field mapping. Format-specific extensions (CSV, Excel, etc.) are implemented in separate modules.

## Core Annotations

### `@Column` - Basic Field Mapping

The foundation annotation for field mapping with essential properties:

```java
public record Employee(
    @Column(name = "Employee ID", order = 1, required = true)
    String employeeId,
    
    @Column(name = "Full Name", order = 2, defaultValue = "Unknown", maxWidth = 50)
    String name,
    
    @Column(ignore = true)  // Skip in exports
    String internalNotes
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

Enhanced formatting for structured tabular data with styling and data type options:

```java
public record SalesRecord(
    @Column(name = "Transaction ID", order = 1)
    @DataColumn(columnIndex = 0, cellType = DataColumn.CellType.TEXT, 
                frozen = true, bold = true)
    String transactionId,
    
    @Column(name = "Total Amount", order = 6)
    @DataColumn(columnIndex = 5, cellType = DataColumn.CellType.CURRENCY,
                numberFormat = "$#,##0.00", 
                backgroundColor = "#E6F3FF",
                alignment = DataColumn.Alignment.RIGHT)
    BigDecimal totalAmount
) {}
```

**Properties:**
- `columnIndex` - Specific column position (0-based)
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

### `@DataTable` - Table-Level Configuration

Configure table-wide settings:

```java
@DataTable(
    name = "Employee Report", 
    description = "Monthly employee data export",
    headers = true,
    validateOnImport = true
)
public record Employee(...) {}
```

**Properties:**
- `name` - Table/report name
- `description` - Table description
- `headers` - Include column headers
- `defaultDateFormat` - Default date format
- `defaultNumberFormat` - Default number format
- `validateOnImport` - Validate data during import
- `skipEmptyRows` - Skip empty rows
- `maxRows` - Maximum rows to process
- `styleClasses` - Custom styling options

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

## Core Processing Classes

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

Unified metadata container:

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

// Parse values during import  
Object parsed = FormatProvider.parseValue("75000.0", salaryColumn);

// Standalone formatting
String currency = FormatProvider.formatNumber(1234.56, "$#,##0.00");
String date = FormatProvider.formatDate(LocalDate.now(), "MM/dd/yyyy");
```

### `Validator`

Field validation engine:

```java
ColumnMetadata column = AnnotationProcessor.findColumn(Employee.class, "email");
ValidationResult result = Validator.validate("test@example.com", column);

if (!result.isValid()) {
    System.out.println("Errors: " + result.getErrorsAsString());
    System.out.println("Warnings: " + result.getWarningsAsString());
}
```

## Example Usage

### Complete Record Definition

```java
@DataTable(
    name = "Employee Report",
    description = "Comprehensive employee data",
    headers = true,
    validateOnImport = true
)
public record Employee(
    @Column(name = "Employee ID", order = 1, required = true)
    @DataColumn(columnIndex = 0, cellType = DataColumn.CellType.TEXT, frozen = true)
    @Validate(pattern = "EMP-\\d{4}", message = "Employee ID must follow pattern EMP-XXXX")
    String employeeId,
    
    @Column(name = "Email", order = 4, required = true)
    @DataColumn(columnIndex = 3, cellType = DataColumn.CellType.TEXT, width = 30)
    @Validate(pattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    String email,
    
    @Column(name = "Salary", order = 6)
    @DataColumn(columnIndex = 5, cellType = DataColumn.CellType.CURRENCY, 
                numberFormat = "$#,##0.00", alignment = DataColumn.Alignment.RIGHT)
    @Validate(min = 20000, max = 500000)
    Double salary
) {}
```

### Processing Example

```java
public class DataExporter {
    
    public void processRecords(Dataset<Employee> employees) {
        // Extract column metadata
        List<ColumnMetadata> columns = AnnotationProcessor.extractColumns(Employee.class);
        
        for (Employee emp : employees.toList()) {
            for (ColumnMetadata column : columns) {
                Object value = getFieldValue(emp, column.getFieldName());
                
                // Validate
                ValidationResult validation = Validator.validate(value, column);
                if (!validation.isValid()) {
                    System.out.println("Validation failed: " + validation.getErrorsAsString());
                    continue;
                }
                
                // Format value
                String formatted = FormatProvider.formatValue(value, column);
                
                // Process formatted value (export to file, display, etc.)
                processValue(column.getEffectiveColumnName(), formatted);
            }
        }
    }
}
```

## Integration with Dataset4j

```java
Dataset<Employee> employees = Dataset.of(employeeList)
    .filter(emp -> emp.isActive())
    .sortBy(Employee::lastName);

// Process with annotations
DataExporter.process(employees);
```

## Extension Architecture

Format-specific annotations extend the core framework:

```java
// In dataset4j-csv module:
@CsvColumn(index = 0, quoted = true, trim = true)

// In dataset4j-excel module:  
@ExcelColumn(columnIndex = 0, cellType = ExcelColumn.CellType.TEXT)
```

These modules implement their own processors that build upon the core `ColumnMetadata` and use the same validation and formatting infrastructure.

## Benefits

1. **Zero Dependencies** - Pure Java with no external libraries
2. **Type Safety** - Compile-time checking with record classes
3. **Modular** - Core annotations with format-specific extensions
4. **Declarative** - Configuration through annotations
5. **Validation** - Built-in validation engine
6. **Formatting** - Comprehensive formatting and parsing
7. **Extensible** - Easy to add new annotation processors

This core framework provides the essential foundation for mapping Java records to tabular data while maintaining complete flexibility for format-specific extensions.