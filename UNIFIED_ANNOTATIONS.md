# Dataset4j Unified Annotation Framework

A streamlined annotation system for mapping Java record fields to tabular data formats with a single unified annotation.

## Overview

The unified framework centers around `@DataColumn` - a comprehensive annotation that combines basic field mapping with advanced formatting options. This eliminates the need for multiple overlapping annotations while maintaining full expressiveness.

## Core Annotations

### `@DataColumn` - Unified Column Mapping

The single annotation for all column mapping needs, from basic field naming to advanced formatting:

```java
public record Employee(
    // Basic field mapping
    @DataColumn(name = "Employee ID", order = 1, required = true)
    String employeeId,
    
    // Advanced formatting
    @DataColumn(name = "Salary", order = 6,
                cellType = DataColumn.CellType.CURRENCY,
                numberFormat = "$#,##0.00", 
                alignment = DataColumn.Alignment.RIGHT)
    Double salary,
    
    // Styling and positioning
    @DataColumn(name = "Email", order = 4, required = true,
                columnIndex = 3, width = 30)
    String email,
    
    // Ignored field
    @DataColumn(ignore = true)
    String internalNotes
) {}
```

**Basic Properties:**
- `name` - Column header name (defaults to field name)
- `order` - Logical column order (-1 for default)
- `required` - Whether field is mandatory
- `ignore` - Skip field during export/import
- `description` - Documentation
- `defaultValue` - Default value for empty fields
- `hidden` - Whether column should be hidden

**Formatting Properties:**
- `cellType` - Data type (AUTO, TEXT, NUMBER, DATE, BOOLEAN, CURRENCY, PERCENTAGE, FORMULA)
- `numberFormat` - Standard number format pattern
- `dateFormat` - Date formatting pattern
- `columnIndex` - Specific column position (0-based)
- `width` - Column width in characters
- `alignment` - Text alignment (AUTO, LEFT, CENTER, RIGHT)

**Styling Properties:**
- `backgroundColor` - Cell background color
- `fontColor` - Font color
- `bold` - Bold text
- `frozen` - Freeze column
- `wrapText` - Text wrapping

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

### `@Validate` - Field Validation

Comprehensive validation rules work seamlessly with `@DataColumn`:

```java
@DataColumn(name = "Customer ID", order = 1, required = true)
@Validate(pattern = "CUST-\\d{4}", message = "Invalid customer ID format")
String customerId
```

## Benefits of Unified Approach

### 1. Simplicity
- **Single annotation** covers all column mapping needs
- **No overlap** between annotation responsibilities
- **Clear semantics** - one annotation per field concept

### 2. Flexibility
- **Progressive enhancement** - start simple, add complexity as needed
- **All properties optional** - only specify what you need
- **Consistent ordering** - logical `order` vs. physical `columnIndex`

### 3. Maintainability
- **Fewer dependencies** between annotations
- **Easier refactoring** - all properties in one place
- **Better tooling support** - IDEs can provide comprehensive completion

## Usage Patterns

### Progressive Enhancement

Start simple and add properties as needed:

```java
// Step 1: Basic mapping
@DataColumn(name = "Product Name")
String name

// Step 2: Add ordering
@DataColumn(name = "Product Name", order = 2)
String name

// Step 3: Add validation
@DataColumn(name = "Product Name", order = 2, required = true)
@Validate(maxLength = 100)
String name

// Step 4: Add formatting
@DataColumn(name = "Product Name", order = 2, required = true, width = 30)
@Validate(maxLength = 100)
String name
```

### Complete Example

```java
@DataTable(
    name = "Sales Report",
    description = "Daily sales transaction data",
    headers = true,
    validateOnImport = true
)
public record SalesRecord(
    
    @DataColumn(name = "Transaction ID", order = 1, required = true,
                columnIndex = 0, cellType = DataColumn.CellType.TEXT, 
                frozen = true, bold = true, width = 15)
    @Validate(pattern = "TXN-\\d{8}")
    String transactionId,
    
    @DataColumn(name = "Sale Date", order = 2, required = true,
                columnIndex = 1, cellType = DataColumn.CellType.DATE,
                dateFormat = "MM/dd/yyyy", alignment = DataColumn.Alignment.CENTER)
    LocalDate saleDate,
    
    @DataColumn(name = "Total Amount", order = 7, required = true,
                columnIndex = 6, cellType = DataColumn.CellType.CURRENCY,
                numberFormat = "$#,##0.00", alignment = DataColumn.Alignment.RIGHT,
                backgroundColor = "#E6F3FF", bold = true)
    @Validate(min = 0.01)
    BigDecimal totalAmount,
    
    @DataColumn(name = "Notes", order = 13,
                columnIndex = 12, cellType = DataColumn.CellType.TEXT,
                width = 30, wrapText = true)
    @Validate(maxLength = 500)
    String notes
) {}
```

## Processing

The unified approach simplifies processing:

```java
public class UnifiedProcessor {
    
    public void exportData(Dataset<SalesRecord> records) {
        // Single annotation type to handle
        List<ColumnMetadata> columns = AnnotationProcessor.extractColumns(SalesRecord.class);
        
        for (SalesRecord record : records.toList()) {
            for (ColumnMetadata column : columns) {
                Object value = getFieldValue(record, column.getFieldName());
                
                // Validate
                ValidationResult result = Validator.validate(value, column);
                if (!result.isValid()) continue;
                
                // Format
                String formatted = FormatProvider.formatValue(value, column);
                
                // All metadata available in single object
                processColumn(
                    column.getEffectiveColumnName(),
                    formatted,
                    column.getCellType(),
                    column.getAlignment(),
                    column.isRequired()
                );
            }
        }
    }
}
```

## Migration from Multiple Annotations

If migrating from a system with separate `@Column` and `@ExcelColumn` annotations:

```java
// Before: Multiple annotations
@Column(name = "Employee ID", order = 1, required = true)
@ExcelColumn(columnIndex = 0, cellType = ExcelColumn.CellType.TEXT, frozen = true)
String employeeId

// After: Unified annotation
@DataColumn(name = "Employee ID", order = 1, required = true,
            columnIndex = 0, cellType = DataColumn.CellType.TEXT, frozen = true)
String employeeId
```

## Integration with Dataset4j

```java
Dataset<Employee> employees = Dataset.of(employeeList)
    .filter(emp -> emp.isActive())
    .sortBy(Employee::lastName);

// Process with unified annotations
UnifiedExporter.export(employees, "employees.xlsx");
```

## Format-Specific Extensions

Format-specific modules can extend or specialize the core `@DataColumn`:

```java
// In dataset4j-csv module (hypothetical):
@CsvColumn // extends @DataColumn with CSV-specific properties
(quoted = true, trim = true, escapeChar = '\\')

// In dataset4j-excel module (hypothetical):
@ExcelColumn // extends @DataColumn with Excel-specific properties  
(formula = "SUM(A1:A10)", conditionalFormatting = "dataBar")
```

## Benefits

1. **Unified Model** - Single annotation covers all needs
2. **Progressive Enhancement** - Start simple, add complexity
3. **Type Safety** - Compile-time checking with records
4. **Zero Dependencies** - Pure Java implementation
5. **Extensible** - Easy to add new properties
6. **Maintainable** - Clear single responsibility
7. **Format Agnostic** - Not tied to specific export formats

This unified approach provides maximum flexibility while maintaining simplicity and clarity in the codebase.