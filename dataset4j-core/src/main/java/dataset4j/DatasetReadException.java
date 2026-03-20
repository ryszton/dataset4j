package dataset4j;

/**
 * Exception thrown when a read error occurs during dataset parsing.
 * Carries structured location context to help locate the problematic data.
 *
 * <p>Example message:
 * <pre>
 * Failed to parse value 'not-a-date' for field 'hireDate' (java: Employee.hireDate)
 * of type LocalDate at Sheet1!C12 (row 11, column 2)
 * </pre>
 */
public class DatasetReadException extends RuntimeException {

    private final int row;
    private final int column;
    private final String sheetName;
    private final String cellReference;
    private final String fieldName;
    private final Class<?> recordClass;
    private final String rawValue;

    private DatasetReadException(Builder builder) {
        super(builder.buildMessage(), builder.cause);
        this.row = builder.row;
        this.column = builder.column;
        this.sheetName = builder.sheetName;
        this.cellReference = builder.cellReference;
        this.fieldName = builder.fieldName;
        this.recordClass = builder.recordClass;
        this.rawValue = builder.rawValue;
    }

    /** 0-based row index, or -1 if unknown */
    public int getRow() { return row; }

    /** 0-based column index, or -1 if unknown */
    public int getColumn() { return column; }

    /** Sheet name (Excel only), or null */
    public String getSheetName() { return sheetName; }

    /** Human-readable cell reference (e.g. "Sheet1!B5" or "row 3, column 'name'") */
    public String getCellReference() { return cellReference; }

    /** Java record field name */
    public String getFieldName() { return fieldName; }

    /** Target record class */
    public Class<?> getRecordClass() { return recordClass; }

    /** The raw string value that failed to parse */
    public String getRawValue() { return rawValue; }

    /** Qualified Java field name (e.g. "Employee.hireDate") */
    public String getQualifiedFieldName() {
        if (recordClass != null && fieldName != null) {
            return recordClass.getSimpleName() + "." + fieldName;
        }
        return fieldName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int row = -1;
        private int column = -1;
        private String sheetName;
        private String cellReference;
        private String fieldName;
        private Class<?> recordClass;
        private String rawValue;
        private String fieldTypeName;
        private String parseMessage;
        private Throwable cause;

        public Builder row(int row) { this.row = row; return this; }
        public Builder column(int column) { this.column = column; return this; }
        public Builder sheetName(String sheetName) { this.sheetName = sheetName; return this; }
        public Builder cellReference(String cellReference) { this.cellReference = cellReference; return this; }
        public Builder fieldName(String fieldName) { this.fieldName = fieldName; return this; }
        public Builder recordClass(Class<?> recordClass) { this.recordClass = recordClass; return this; }
        public Builder rawValue(String rawValue) { this.rawValue = rawValue; return this; }
        public Builder fieldTypeName(String fieldTypeName) { this.fieldTypeName = fieldTypeName; return this; }
        public Builder parseMessage(String parseMessage) { this.parseMessage = parseMessage; return this; }
        public Builder cause(Throwable cause) { this.cause = cause; return this; }

        public DatasetReadException build() {
            if (cellReference == null) {
                cellReference = buildCellReference();
            }
            return new DatasetReadException(this);
        }

        private String buildCellReference() {
            if (sheetName != null && row >= 0 && column >= 0) {
                return sheetName + "!" + toExcelColumnLetter(column) + (row + 1);
            }
            if (row >= 0 && column >= 0) {
                return toExcelColumnLetter(column) + (row + 1);
            }
            if (row >= 0 && fieldName != null) {
                return "row " + row + ", column '" + fieldName + "'";
            }
            if (row >= 0) {
                return "row " + row;
            }
            return null;
        }

        private String buildMessage() {
            var sb = new StringBuilder();
            sb.append("Failed to parse value '").append(rawValue != null ? rawValue : "null").append("'");

            if (fieldName != null) {
                sb.append(" for field '").append(fieldName).append("'");
                if (recordClass != null) {
                    sb.append(" (java: ").append(recordClass.getSimpleName()).append(".").append(fieldName).append(")");
                }
            }

            if (fieldTypeName != null) {
                sb.append(" of type ").append(fieldTypeName);
            }

            if (cellReference != null) {
                sb.append(" at ").append(cellReference);
            }
            if (row >= 0 && column >= 0 && sheetName != null) {
                sb.append(" (row ").append(row).append(", column ").append(column).append(")");
            }

            if (parseMessage != null) {
                sb.append(": ").append(parseMessage);
            }

            return sb.toString();
        }

        static String toExcelColumnLetter(int columnIndex) {
            var sb = new StringBuilder();
            int col = columnIndex;
            do {
                sb.insert(0, (char) ('A' + col % 26));
                col = col / 26 - 1;
            } while (col >= 0);
            return sb.toString();
        }
    }
}
