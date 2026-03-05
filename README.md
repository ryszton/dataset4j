# dataset4j

A lightweight, zero-dependency DataFrame-like library for Java 21+.

**dataset4j** lets you port Pandas data processing pipelines to Java using records, streams, and a fluent API. No heavy frameworks, no runtime dependencies — just your data and the JDK.

## Quick Start

```java
record Employee(String name, int age, String dept) {}
record Department(String dept, String location) {}

var employees = Dataset.of(
    new Employee("Alice",   30, "Eng"),
    new Employee("Bob",     25, "Sales"),
    new Employee("Charlie", 35, "Eng")
);

// Filter + sort + limit  (df.query("age > 25").sort_values("age").head(2))
var seniors = employees
    .filter(e -> e.age() > 25)
    .sortByInt(Employee::age)
    .head(2);

// GroupBy + aggregation  (df.groupby("dept")["age"].mean())
Map<String, Double> avgByDept = employees
    .groupBy(Employee::dept)
    .meanInt(Employee::age);

// Join with Pair — no custom record needed
//   pd.merge(employees, departments, on="dept")
var departments = Dataset.of(
    new Department("Eng", "SF"),
    new Department("Sales", "NYC")
);

employees.innerJoin(departments, Employee::dept, Department::dept)
    .filter(p -> p.left().age() > 25)
    .map(p -> p.left().name() + " works in " + p.right().location());
```

## Features

| Pandas | dataset4j |
|--------|-----------|
| `df[df["age"] > 25]` | `.filter(e -> e.age() > 25)` |
| `df.sort_values("age")` | `.sortByInt(Employee::age)` |
| `df.head(5)` | `.head(5)` |
| `df["name"].tolist()` | `.column(Employee::name)` |
| `df["age"].sum()` | `.sumInt(Employee::age)` |
| `df.groupby("dept")` | `.groupBy(Employee::dept)` |
| `pd.merge(a, b, on="k")` | `.innerJoin(b, A::k, B::k)` |
| `pd.merge(a, b, how="left")` | `.leftJoin(b, A::k, B::k)` |
| `df.drop_duplicates("col")` | `.distinctBy(Employee::col)` |
| `df.apply(fn, axis=1)` | `.map(e -> ...)` |
| `df.melt(...)` | `.flatMap(e -> List.of(...))` |
| `df["age"].cumsum()` | `.cumSumInt(Employee::age)` |
| `df["age"].rolling(3).mean()` | `.rollingMeanInt(Employee::age, 3)` |
| `df.pipe(fn)` | `.pipe(ds -> ...)` |

### Join Types

```java
// Inner join → Dataset<Pair<L, R>>
ds.innerJoin(other, L::key, R::key)

// Left join (right may be null)
ds.leftJoin(other, L::key, R::key)

// Right join (left may be null)
ds.rightJoin(other, L::key, R::key)

// Cross join (cartesian product)
ds.crossJoin(other)

// Three-way join → Dataset<Triplet<A, B, C>>
ds.innerJoin3(
    table2, A::key1, B::key1,
    table3, B::key2, C::key2)
```

### Pair & Triplet

`Pair<L, R>` and `Triplet<A, B, C>` are generic containers that eliminate the need to define a custom record for every join:

```java
// Join, filter, and process — all without a custom record
employees.innerJoin(departments, Employee::dept, Department::dept)
    .filter(p -> p.left().age() > 30)
    .map(p -> p.left().name() + " @ " + p.right().location());

// Promote Pair to Triplet
pair.withThird(computedValue)

// Transform one side
pair.mapLeft(e -> new Employee(e.name().toUpperCase(), e.age(), e.dept()))

// Convert to a proper record when you need one
joined.map(p -> new MyRecord(p.left().name(), p.right().location()));
```

## Installation

### Maven
```xml
<dependency>
    <groupId>io.github.dataset4j</groupId>
    <artifactId>dataset4j</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Gradle
```groovy
implementation 'io.github.dataset4j:dataset4j:0.1.0-SNAPSHOT'
```

### Manual
Copy `Dataset.java`, `Pair.java`, and `Triplet.java` into your project. Zero dependencies.

## Requirements

- Java 21+

## Design Principles

1. **Zero dependencies** — only the JDK
2. **Immutable** — every operation returns a new `Dataset<T>`
3. **Type-safe** — Java records as rows, generics everywhere
4. **Fluent** — method chaining that mirrors Pandas pipelines
5. **Minimal** — three files, no framework, no magic

## Building

```bash
mvn clean test
```

## License

MIT
