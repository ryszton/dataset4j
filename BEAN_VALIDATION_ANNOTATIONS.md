# Dataset4j with Bean Validation

A streamlined annotation framework combining unified column mapping with standard Bean Validation for field validation.

## Overview

This framework leverages `@DataColumn` for comprehensive column mapping and Jakarta Bean Validation (JSR-380) for standardized field validation, providing excellent integration with the Java ecosystem.

## Core Architecture

### `@DataColumn` - Unified Column Mapping

Single annotation handling all column mapping aspects:

```java
@DataColumn(
    name = "Employee ID",           // Column header
    order = 1,                      // Logical ordering
    required = true,                // Required field
    columnIndex = 0,               // Physical position (0-based)
    cellType = DataColumn.CellType.TEXT,  // Data type
    frozen = true,                  // Freeze column
    width = 15                      // Column width
)
```

### Bean Validation Integration

Standard Jakarta validation annotations work seamlessly:

```java
@DataColumn(name = "Email", order = 4, required = true, width = 30)
@NotBlank
@Email(message = "Please enter a valid email address")
String email
```

## Complete Example

```java
@DataTable(
    name = "Employee Report",
    description = "Comprehensive employee data",
    headers = true,
    validateOnImport = true
)
public record Employee(
    
    @DataColumn(name = "Employee ID", order = 1, required = true,
                columnIndex = 0, cellType = DataColumn.CellType.TEXT, frozen = true)
    @NotBlank
    @Pattern(regexp = "EMP-\\d{4}", message = "Employee ID must follow pattern EMP-XXXX")
    String employeeId,
    
    @DataColumn(name = "First Name", order = 2, required = true,
                columnIndex = 1, cellType = DataColumn.CellType.TEXT)
    @NotBlank
    @Size(min = 2, max = 50)
    String firstName,
    
    @DataColumn(name = "Email", order = 4, required = true,
                columnIndex = 3, cellType = DataColumn.CellType.TEXT, width = 30)
    @NotBlank
    @Email(message = "Please enter a valid email address")
    String email,
    
    @DataColumn(name = "Department", order = 5,
                columnIndex = 4, cellType = DataColumn.CellType.TEXT)
    @Pattern(regexp = "IT|HR|Finance|Marketing|Operations|Sales", 
             message = "Department must be one of: IT, HR, Finance, Marketing, Operations, Sales")
    String department,
    
    @DataColumn(name = "Salary", order = 6,
                columnIndex = 5, cellType = DataColumn.CellType.CURRENCY, 
                numberFormat = "$#,##0.00", alignment = DataColumn.Alignment.RIGHT)
    @DecimalMin(value = "20000.0", message = "Salary must be at least $20,000")
    @DecimalMax(value = "500000.0", message = "Salary cannot exceed $500,000")
    Double salary,
    
    @DataColumn(name = "Hire Date", order = 7,
                columnIndex = 6, cellType = DataColumn.CellType.DATE, 
                dateFormat = "yyyy-MM-dd", alignment = DataColumn.Alignment.CENTER)
    @NotNull
    LocalDate hireDate,
    
    @DataColumn(name = "Years of Experience", order = 10,
                columnIndex = 9, cellType = DataColumn.CellType.NUMBER, 
                numberFormat = "0", alignment = DataColumn.Alignment.RIGHT)
    @Min(value = 0, message = "Years of experience cannot be negative")
    @Max(value = 50, message = "Years of experience cannot exceed 50")
    Integer yearsOfExperience,
    
    // Ignored field - not exported
    @DataColumn(ignore = true)
    String internalNotes
) {}
```

## Bean Validation Annotations Used

### Standard Validation
- `@NotNull` - Field cannot be null
- `@NotBlank` - String cannot be null, empty, or whitespace-only
- `@Size(min, max)` - String/collection length constraints
- `@Min(value)` - Minimum numeric value
- `@Max(value)` - Maximum numeric value
- `@DecimalMin(value)` - Minimum decimal value
- `@DecimalMax(value)` - Maximum decimal value
- `@Pattern(regexp)` - Regular expression validation
- `@Email` - Email format validation

### Usage Patterns

```java
// String validation
@NotBlank
@Size(min = 3, max = 100)
String productName

// Numeric validation
@NotNull
@DecimalMin(value = "0.01", message = "Price must be positive")
@DecimalMax(value = "9999.99", message = "Price too high")
BigDecimal price

// Pattern validation for enums
@Pattern(regexp = "Electronics|Clothing|Books|Home|Sports|Beauty", 
         message = "Category must be one of: Electronics, Clothing, Books, Home, Sports, Beauty")
String category

// ID validation
@NotBlank
@Pattern(regexp = "SKU-[A-Z0-9]{6}", message = "SKU must follow format SKU-XXXXXX")
String sku
```

## Processing Integration

The framework provides validation through standard Bean Validation:

```java
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;

public class DataProcessor {
    
    private final Validator validator;
    
    public DataProcessor() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }
    
    public void processEmployee(Employee employee) {
        // Extract column metadata
        List<ColumnMetadata> columns = AnnotationProcessor.extractColumns(Employee.class);
        
        // Validate using Bean Validation
        Set<ConstraintViolation<Employee>> violations = validator.validate(employee);
        if (!violations.isEmpty()) {
            for (ConstraintViolation<Employee> violation : violations) {
                System.out.println("Validation error: " + violation.getMessage() 
                                 + " (field: " + violation.getPropertyPath() + ")");
            }
            return;
        }
        
        // Process valid employee
        for (ColumnMetadata column : columns) {
            Object value = getFieldValue(employee, column.getFieldName());
            String formatted = FormatProvider.formatValue(value, column);
            
            // Export to file, database, etc.
            exportColumn(column.getEffectiveColumnName(), formatted, column);
        }
    }
}
```

## Benefits of Bean Validation Integration

### 1. Standard Ecosystem
- **Industry Standard** - JSR-380 is the Java validation standard
- **Tool Support** - IDEs, frameworks, and libraries understand Bean Validation
- **Documentation** - Well-documented with extensive examples

### 2. Rich Validation Features
- **Comprehensive** - Covers all common validation scenarios
- **Extensible** - Easy to create custom validators
- **Internationalization** - Built-in i18n support for error messages

### 3. Framework Integration
- **Spring Boot** - Automatic validation integration
- **JPA/Hibernate** - Database validation
- **JAX-RS** - REST API validation
- **Testing** - Validation testing utilities

### 4. Zero Custom Code
- **No custom validation framework** needed
- **Standard annotations** everyone understands
- **Proven reliability** from years of production use

## Dependencies

```xml
<dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
    <version>3.0.2</version>
</dependency>
```

For runtime validation, add an implementation:

```xml
<!-- For full validation support -->
<dependency>
    <groupId>org.hibernate.validator</groupId>
    <artifactId>hibernate-validator</artifactId>
    <version>8.0.0.Final</version>
    <scope>runtime</scope>
</dependency>
```

## Validation Examples

### Product Catalog

```java
@DataTable(name = "Product Catalog")
public record Product(
    @DataColumn(name = "SKU", order = 1, required = true)
    @NotBlank
    @Pattern(regexp = "SKU-[A-Z0-9]{6}", message = "SKU must follow format SKU-XXXXXX")
    String sku,
    
    @DataColumn(name = "Product Name", order = 2, required = true, width = 100)
    @NotBlank
    @Size(min = 3, max = 100)
    String name,
    
    @DataColumn(name = "Category", order = 3)
    @Pattern(regexp = "Electronics|Clothing|Books|Home|Sports|Beauty", 
             message = "Category must be one of: Electronics, Clothing, Books, Home, Sports, Beauty")
    String category,
    
    @DataColumn(name = "Price", order = 4, required = true)
    @NotNull
    @DecimalMin(value = "0.01", message = "Price must be at least $0.01")
    @DecimalMax(value = "9999.99", message = "Price cannot exceed $9,999.99")
    BigDecimal price,
    
    @DataColumn(name = "Stock Quantity", order = 6, defaultValue = "0")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Max(value = 99999, message = "Stock quantity cannot exceed 99,999")
    Integer stockQuantity
) {}
```

### Sales Transaction

```java
@DataTable(name = "Sales Report")
public record SalesRecord(
    @DataColumn(name = "Transaction ID", order = 1, required = true,
                cellType = DataColumn.CellType.TEXT, frozen = true, bold = true)
    @NotBlank
    @Pattern(regexp = "TXN-\\d{8}", message = "Transaction ID must follow format TXN-XXXXXXXX")
    String transactionId,
    
    @DataColumn(name = "Total Amount", order = 7, required = true,
                cellType = DataColumn.CellType.CURRENCY, numberFormat = "$#,##0.00",
                backgroundColor = "#E6F3FF", bold = true)
    @NotNull
    @DecimalMin(value = "0.01", message = "Total amount must be positive")
    BigDecimal totalAmount,
    
    @DataColumn(name = "Payment Method", order = 11,
                cellType = DataColumn.CellType.TEXT, alignment = DataColumn.Alignment.CENTER)
    @Pattern(regexp = "Cash|Credit Card|Debit Card|Check|Gift Card|Store Credit",
             message = "Payment method must be one of: Cash, Credit Card, Debit Card, Check, Gift Card, Store Credit")
    String paymentMethod
) {}
```

## Migration from Custom Validation

```java
// Before: Custom validation
@DataColumn(name = "Email", order = 4, required = true)
@Validate(pattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", 
          message = "Please enter a valid email address")
String email

// After: Bean Validation
@DataColumn(name = "Email", order = 4, required = true)
@NotBlank
@Email(message = "Please enter a valid email address")
String email
```

This approach leverages the mature Bean Validation ecosystem while maintaining the simplicity and power of the unified `@DataColumn` annotation for column mapping.