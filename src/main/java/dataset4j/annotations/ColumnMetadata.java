package dataset4j.annotations;

import java.lang.reflect.RecordComponent;
import java.util.Objects;

/**
 * Metadata container for a single column extracted from record annotations.
 * 
 * <p>This class provides a unified view of column information regardless of the
 * specific annotation type (@Column, @ExcelColumn, @CsvColumn).
 */
public final class ColumnMetadata {
    
    private final RecordComponent recordComponent;
    private final String fieldName;
    private final String columnName;
    private final int order;
    private final boolean required;
    private final boolean ignored;
    private final String description;
    private final String defaultValue;
    private final Class<?> fieldType;
    
    // Format-specific metadata
    private final String numberFormat;
    private final String dateFormat;
    private final int maxLength;
    
    private ColumnMetadata(Builder builder) {
        this.recordComponent = builder.recordComponent;
        this.fieldName = builder.fieldName;
        this.columnName = builder.columnName;
        this.order = builder.order;
        this.required = builder.required;
        this.ignored = builder.ignored;
        this.description = builder.description;
        this.defaultValue = builder.defaultValue;
        this.fieldType = builder.fieldType;
        this.numberFormat = builder.numberFormat;
        this.dateFormat = builder.dateFormat;
        this.maxLength = builder.maxLength;
    }
    
    public RecordComponent getRecordComponent() { return recordComponent; }
    public String getFieldName() { return fieldName; }
    public String getColumnName() { return columnName; }
    public int getOrder() { return order; }
    public boolean isRequired() { return required; }
    public boolean isIgnored() { return ignored; }
    public String getDescription() { return description; }
    public String getDefaultValue() { return defaultValue; }
    public Class<?> getFieldType() { return fieldType; }
    public String getNumberFormat() { return numberFormat; }
    public String getDateFormat() { return dateFormat; }
    public int getMaxLength() { return maxLength; }
    
    /**
     * Get the effective column name, falling back to field name if not specified.
     */
    public String getEffectiveColumnName() {
        return columnName.isEmpty() ? fieldName : columnName;
    }
    
    /**
     * Check if this column has formatting rules.
     */
    public boolean hasFormatting() {
        return !numberFormat.isEmpty() || !dateFormat.equals("yyyy-MM-dd");
    }
    
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ColumnMetadata that = (ColumnMetadata) obj;
        return Objects.equals(fieldName, that.fieldName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(fieldName);
    }
    
    @Override
    public String toString() {
        return String.format("ColumnMetadata[field=%s, column=%s, order=%d, type=%s]", 
                           fieldName, getEffectiveColumnName(), order, fieldType.getSimpleName());
    }
    
    /**
     * Create a builder for ColumnMetadata.
     */
    public static Builder builder(RecordComponent recordComponent) {
        return new Builder(recordComponent);
    }
    
    public static class Builder {
        private final RecordComponent recordComponent;
        private final String fieldName;
        private final Class<?> fieldType;
        private String columnName = "";
        private int order = -1;
        private boolean required = false;
        private boolean ignored = false;
        private String description = "";
        private String defaultValue = "";
        private String numberFormat = "";
        private String dateFormat = "yyyy-MM-dd";
        private int maxLength = -1;
        
        public Builder(RecordComponent recordComponent) {
            this.recordComponent = recordComponent;
            this.fieldName = recordComponent.getName();
            this.fieldType = recordComponent.getType();
        }
        
        public Builder columnName(String columnName) {
            this.columnName = columnName != null ? columnName : "";
            return this;
        }
        
        public Builder order(int order) {
            this.order = order;
            return this;
        }
        
        public Builder required(boolean required) {
            this.required = required;
            return this;
        }
        
        public Builder ignored(boolean ignored) {
            this.ignored = ignored;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description != null ? description : "";
            return this;
        }
        
        public Builder defaultValue(String defaultValue) {
            this.defaultValue = defaultValue != null ? defaultValue : "";
            return this;
        }
        
        public Builder numberFormat(String numberFormat) {
            this.numberFormat = numberFormat != null ? numberFormat : "";
            return this;
        }
        
        public Builder dateFormat(String dateFormat) {
            this.dateFormat = dateFormat != null ? dateFormat : "yyyy-MM-dd";
            return this;
        }
        
        
        public Builder maxLength(int maxLength) {
            this.maxLength = maxLength;
            return this;
        }
        
        public ColumnMetadata build() {
            return new ColumnMetadata(this);
        }
    }
}