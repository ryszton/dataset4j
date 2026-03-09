# Dataset4J

A modular, lightweight DataFrame-like library for Java records. Port your pandas data processing pipelines to Java with strong typing, fluent APIs, and comprehensive format support.

## 🚀 **Quick Start**

### Maven Dependency

**Option 1: Complete Library** (Recommended)
```xml
<dependency>
    <groupId>io.github.amah</groupId>
    <artifactId>dataset4j</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Option 2: Modular Dependencies**
```xml
<!-- Core functionality only -->
<dependency>
    <groupId>io.github.amah</groupId>
    <artifactId>dataset4j-core</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Add Excel/CSV support -->
<dependency>
    <groupId>io.github.amah</groupId>
    <artifactId>dataset4j-poi</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Add Parquet support (lightweight, no Hadoop) -->
<dependency>
    <groupId>io.github.amah</groupId>
    <artifactId>dataset4j-parquet</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Basic Usage

```java
// Define your data structure with annotations
@GenerateFields(className = "Fields", columnsClassName = "Cols")
public record Employee(
    @DataColumn(name = "Employee ID", order = 1, required = true)
    String id,
    @DataColumn(name = "Full Name", order = 2)
    String fullName,
    @DataColumn(name = "Email", order = 3)
    String email,
    @DataColumn(name = "Department", order = 4)
    String department
) {}

// Create and process datasets with pandas-like operations
Dataset<Employee> employees = Dataset.of(
    new Employee("001", "John Doe", "john@company.com", "Engineering"),
    new Employee("002", "Jane Smith", "jane@company.com", "Marketing")
);

// Filter, transform, and aggregate data
Dataset<String> engineerEmails = employees
    .filter(emp -> emp.department().equals("Engineering"))
    .map(Employee::email);

// Export to Excel with type-safe field selection
ExcelDatasetWriter
    .toFile("report.xlsx")
    .fields(Employee.Fields.ID, Employee.Fields.FULL_NAME, Employee.Fields.EMAIL)
    .write(employees);

// Read from Excel
Dataset<Employee> fromExcel = ExcelDatasetReader
    .fromFile("employees.xlsx")
    .read(Employee.class);

// High-performance Parquet I/O
ParquetDatasetWriter
    .toFile("data.parquet")
    .compression(ParquetCompressionCodec.SNAPPY)
    .write(employees);
```

## ✨ **Key Features**

### 🎯 **Type Safety**
- **Java Records**: Strong typing with compile-time validation
- **Generated Constants**: Auto-generated field constants eliminate magic strings
- **IDE Support**: Full auto-completion and refactoring support

### 🐼 **Pandas-Like API**
- **Immutable Operations**: All operations return new Dataset instances
- **Fluent Interface**: Chain operations naturally
- **Familiar Methods**: `filter()`, `map()`, `groupBy()`, `sortBy()`, etc.

### 📊 **Format Support**
- **Excel**: Read/write with Apache POI integration
- **Parquet**: Lightweight implementation (96% size reduction vs Hadoop)
- **CSV**: Coming soon
- **JSON**: Planned

### 🏗️ **Modular Architecture**
- **dataset4j-core**: Zero dependencies (50KB)
- **dataset4j-poi**: Excel/CSV support (15MB)
- **dataset4j-parquet**: Parquet support (5.6MB vs 145MB traditional)
- **dataset4j**: All-in-one convenience module

### 🔒 **Security First**
- **Path Validation**: Automatic protection against path traversal
- **Safe Dependencies**: Vetted compression libraries
- **File Size Limits**: Built-in DoS protection

## 📖 **Documentation**

- **[Core Concepts](docs/ai-instructions/dataset4j-ai-instructions-core-concepts.md)** - Understanding Dataset4J fundamentals
- **[Usage Patterns](docs/ai-instructions/dataset4j-ai-instructions-usage-patterns.md)** - Common operations and examples
- **[Pandas Migration](docs/ai-instructions/dataset4j-ai-instructions-pandas-mapping.md)** - Complete pandas to Dataset4J mapping
- **[Architecture](docs/architecture/MODULAR_ARCHITECTURE.md)** - Module structure and deployment options
- **[AI Instructions](docs/ai-instructions/)** - Complete guide for AI coding assistants

## 🔧 **Advanced Features**

### Generated Field Constants
```java
@GenerateFields
public record Product(
    @DataColumn(name = "Product ID") String id,
    @DataColumn(name = "Price") double price
) {}

// Auto-generated at compile time:
// Product.Fields.ID = "id"
// Product.Fields.PRICE = "price"  
// Product.Cols.COL_ID = "Product ID"
// Product.Cols.COL_PRICE = "Price"

// Use in field selection
FieldSelector.from(metadata)
    .fields(Product.Fields.ID, Product.Fields.PRICE)
    .select();
```

### Flexible Field Selection
```java
// Conditional field selection
List<FieldMeta> exportFields = FieldSelector.from(metadata)
    .fieldsArray(Employee.Fields.ALL_FIELDS)
    .exclude(Employee.Fields.INTERNAL_NOTES)
    .requiredOnly()
    .exportableOnly()
    .orderByAnnotation()
    .select();

// Use with Excel export
ExcelDatasetWriter
    .toFile("filtered_report.xlsx")
    .select(FieldSelector.from(metadata).where(exportFields::contains))
    .write(dataset);
```

### High-Performance Parquet
```java
// Multiple compression options
ParquetDatasetWriter
    .toFile("data.parquet")
    .compression(ParquetCompressionCodec.SNAPPY)  // Fast & efficient
    .compression(ParquetCompressionCodec.LZ4)     // Fastest
    .compression(ParquetCompressionCodec.GZIP)    // Best compression
    .write(largeDataset);
```

## 🤝 **Contributing**

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

## 📄 **License**

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🔗 **Links**

- **Maven Central**: https://central.sonatype.com/artifact/io.github.amah/dataset4j
- **GitHub**: https://github.com/amah/dataset4j
- **Issues**: https://github.com/amah/dataset4j/issues

---

**Dataset4J v1.0.0** - Bringing pandas-like data processing to Java with type safety and performance.