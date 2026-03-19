package dataset4j.parquet;

import dataset4j.Dataset;
import dataset4j.annotations.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.RecordComponent;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Lightweight Parquet writer with minimal dependencies.
 * Custom implementation avoiding heavy Hadoop dependencies.
 * 
 * Example usage:
 * {@code
 * ParquetDatasetWriter
 *     .toFile("employees.parquet")
 *     .withCompression(ParquetCompressionCodec.SNAPPY)
 *     .withRowGroupSize(100000)
 *     .write(employees);
 * }
 */
public class ParquetDatasetWriter {
    
    private final Path filePath;
    private ParquetCompressionCodec compressionCodec = ParquetCompressionCodec.SNAPPY;
    private int rowGroupSize = 50000;
    private boolean enableDictionary = true;
    private Map<String, Object> metadata = new HashMap<>();

    // Field selection support
    private PojoMetadata<?> pojoMetadata;
    private FieldSelector<?> fieldSelector;
    private List<FieldMeta> selectedFields;
    
    private ParquetDatasetWriter(String filePath) {
        this.filePath = Paths.get(filePath);
    }
    
    /**
     * Create writer for Parquet file.
     * @param filePath path to output Parquet file
     * @return new writer instance
     */
    public static ParquetDatasetWriter toFile(String filePath) {
        // Validate file path to prevent directory traversal attacks
        Path path = Paths.get(filePath).normalize();
        if (path.toString().contains("..")) {
            throw new SecurityException("Path traversal detected in file path: " + filePath);
        }
        return new ParquetDatasetWriter(filePath);
    }
    
    /**
     * Set compression codec for writing.
     * @param codec compression codec
     * @return this writer for chaining
     */
    public ParquetDatasetWriter withCompression(ParquetCompressionCodec codec) {
        this.compressionCodec = codec;
        return this;
    }
    
    /**
     * Set row group size (number of rows per row group).
     * @param size row group size
     * @return this writer for chaining
     */
    public ParquetDatasetWriter withRowGroupSize(int size) {
        this.rowGroupSize = size;
        return this;
    }
    
    /**
     * Enable or disable dictionary encoding.
     * @param enable true to enable dictionary encoding
     * @return this writer for chaining
     */
    public ParquetDatasetWriter withDictionary(boolean enable) {
        this.enableDictionary = enable;
        return this;
    }
    
    /**
     * Add custom metadata to the file.
     * @param key metadata key
     * @param value metadata value
     * @return this writer for chaining
     */
    public ParquetDatasetWriter withMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }
    
    /**
     * Select specific fields to export by field names.
     * @param fieldNames field names to include
     * @return this writer for chaining
     */
    public ParquetDatasetWriter fields(String... fieldNames) {
        if (pojoMetadata != null) {
            this.fieldSelector = FieldSelector.from(pojoMetadata).fields(fieldNames);
        }
        return this;
    }

    /**
     * Select specific fields to export by column names.
     * @param columnNames column names to include
     * @return this writer for chaining
     */
    public ParquetDatasetWriter columns(String... columnNames) {
        if (pojoMetadata != null) {
            this.fieldSelector = FieldSelector.from(pojoMetadata).columns(columnNames);
        }
        return this;
    }

    /**
     * Select fields using generated field constants array.
     * @param fieldConstants array of field name constants
     * @return this writer for chaining
     */
    public ParquetDatasetWriter fieldsArray(String[] fieldConstants) {
        if (pojoMetadata != null) {
            this.fieldSelector = FieldSelector.from(pojoMetadata).fieldsArray(fieldConstants);
        }
        return this;
    }

    /**
     * Select fields using generated column constants array.
     * @param columnConstants array of column name constants
     * @return this writer for chaining
     */
    public ParquetDatasetWriter columnsArray(String[] columnConstants) {
        if (pojoMetadata != null) {
            this.fieldSelector = FieldSelector.from(pojoMetadata).columnsArray(columnConstants);
        }
        return this;
    }

    /**
     * Exclude specific fields from export.
     * @param fieldNames field names to exclude
     * @return this writer for chaining
     */
    public ParquetDatasetWriter exclude(String... fieldNames) {
        if (pojoMetadata != null) {
            if (this.fieldSelector == null) {
                this.fieldSelector = FieldSelector.from(pojoMetadata);
            }
            this.fieldSelector = this.fieldSelector.exclude(fieldNames);
        }
        return this;
    }

    /**
     * Select only required fields for export.
     * @return this writer for chaining
     */
    public ParquetDatasetWriter requiredOnly() {
        if (pojoMetadata != null) {
            this.fieldSelector = FieldSelector.from(pojoMetadata).requiredOnly();
        }
        return this;
    }

    /**
     * Select only exportable fields (not ignored or hidden).
     * @return this writer for chaining
     */
    public ParquetDatasetWriter exportableOnly() {
        if (pojoMetadata != null) {
            this.fieldSelector = FieldSelector.from(pojoMetadata).exportableOnly();
        }
        return this;
    }

    /**
     * Use pre-built metadata for field selection.
     * @param <T> record type
     * @param metadata POJO metadata
     * @return this writer for chaining
     */
    @SuppressWarnings("unchecked")
    public <T> ParquetDatasetWriter select(PojoMetadata<T> metadata) {
        this.pojoMetadata = metadata;
        return this;
    }

    /**
     * Use custom field selector for advanced field selection.
     * @param <T> record type
     * @param selector field selector
     * @return this writer for chaining
     */
    @SuppressWarnings("unchecked")
    public <T> ParquetDatasetWriter select(FieldSelector<T> selector) {
        this.fieldSelector = selector;
        return this;
    }

    /**
     * Write Dataset to Parquet file.
     * @param <T> record type
     * @param dataset dataset to write
     * @throws IOException if file cannot be written
     */
    public <T> void write(Dataset<T> dataset) throws IOException {
        if (dataset.isEmpty()) {
            throw new IllegalArgumentException("Cannot write empty dataset");
        }

        Class<?> recordClass = dataset.toList().get(0).getClass();
        if (!recordClass.isRecord()) {
            throw new IllegalArgumentException("Dataset must contain record types");
        }

        // Initialize metadata if not set
        if (pojoMetadata == null) {
            @SuppressWarnings("unchecked")
            Class<Object> typedClass = (Class<Object>) recordClass;
            pojoMetadata = MetadataCache.getMetadata(typedClass);
        }

        // Determine which fields to export
        List<FieldMeta> fieldsToExport;
        if (fieldSelector != null) {
            fieldsToExport = fieldSelector.select();
        } else if (selectedFields != null) {
            fieldsToExport = selectedFields;
        } else {
            fieldsToExport = pojoMetadata.getExportableFields();
        }

        if (fieldsToExport.isEmpty()) {
            throw new IllegalArgumentException("No fields selected for export. Ensure record has @DataColumn annotations.");
        }

        RecordComponent[] components = recordClass.getRecordComponents();

        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "rw");
             FileChannel channel = raf.getChannel()) {

            // Create schema from field metadata
            ParquetSchema schema = createSchema(fieldsToExport);

            // Write magic number at start
            writeMagicNumber(channel);

            // Write data in row groups
            List<ParquetRowGroup> rowGroups = writeRowGroups(channel, dataset.toList(), schema, fieldsToExport, components);

            // Write footer with metadata
            long footerOffset = channel.position();
            writeFooter(channel, schema, rowGroups);

            // Write footer length and magic number at end
            writeFooterLength(channel, (int) (channel.position() - footerOffset));
            writeMagicNumber(channel);
        }
    }

    // Private implementation methods

    private ParquetSchema createSchema(List<FieldMeta> fields) {
        ParquetSchema schema = new ParquetSchema();

        for (FieldMeta field : fields) {
            if (!field.isIgnored()) {
                ParquetDataType dataType = ParquetDataType.fromJavaClass(field.getFieldType());
                ParquetColumn column = new ParquetColumn(
                    field.getFieldName(),
                    dataType,
                    field.isRequired()
                );
                schema.addColumn(column);
            }
        }

        return schema;
    }

    private void writeMagicNumber(FileChannel channel) throws IOException {
        ByteBuffer magic = ByteBuffer.wrap("PAR1".getBytes());
        channel.write(magic);
    }

    private <T> List<ParquetRowGroup> writeRowGroups(FileChannel channel, List<T> records,
                                                   ParquetSchema schema, List<FieldMeta> fields,
                                                   RecordComponent[] components)
            throws IOException {

        List<ParquetRowGroup> rowGroups = new ArrayList<>();

        // Process records in chunks (row groups)
        for (int startIndex = 0; startIndex < records.size(); startIndex += rowGroupSize) {
            int endIndex = Math.min(startIndex + rowGroupSize, records.size());
            List<T> rowGroupRecords = records.subList(startIndex, endIndex);

            ParquetRowGroup rowGroup = writeRowGroup(channel, rowGroupRecords, schema, fields, components);
            rowGroups.add(rowGroup);
        }

        return rowGroups;
    }

    private <T> ParquetRowGroup writeRowGroup(FileChannel channel, List<T> records,
                                            ParquetSchema schema, List<FieldMeta> fields,
                                            RecordComponent[] components)
            throws IOException {

        long rowGroupStart = channel.position();
        List<ParquetColumnChunk> columnChunks = new ArrayList<>();

        // Write each column as a separate chunk
        for (ParquetColumn schemaColumn : schema.getColumns()) {
            RecordComponent component = findComponent(components, schemaColumn.getName());
            if (component != null) {
                ParquetColumnChunk chunk = writeColumnChunk(channel, records, schemaColumn, component);
                columnChunks.add(chunk);
            }
        }

        long totalSize = channel.position() - rowGroupStart;

        return new ParquetRowGroup(columnChunks, records.size(), totalSize);
    }

    private RecordComponent findComponent(RecordComponent[] components, String fieldName) {
        for (RecordComponent component : components) {
            if (component.getName().equals(fieldName)) {
                return component;
            }
        }
        return null;
    }

    private <T> ParquetColumnChunk writeColumnChunk(FileChannel channel, List<T> records,
                                                  ParquetColumn schemaColumn, RecordComponent component)
            throws IOException {

        long chunkStart = channel.position();

        // Extract column values from all records
        List<Object> columnValues = extractColumnValues(records, component);

        // Encode values (simplified - real implementation would support multiple encodings)
        ByteBuffer encodedData = encodeColumnValues(columnValues, schemaColumn.getDataType());

        // Compress if needed
        ByteBuffer compressedData = compress(encodedData, compressionCodec);

        // Write page header (simplified)
        writePageHeader(channel, compressedData.remaining(), encodedData.remaining(), columnValues.size());

        // Write compressed data
        channel.write(compressedData);

        long chunkEnd = channel.position();
        int compressedSize = (int) (chunkEnd - chunkStart);

        return new ParquetColumnChunk(
            schemaColumn.getName(),
            schemaColumn.getDataType(),
            chunkStart,
            compressedSize,
            encodedData.remaining(),
            compressionCodec,
            columnValues.size()
        );
    }

    private <T> List<Object> extractColumnValues(List<T> records, RecordComponent component) {
        List<Object> values = new ArrayList<>();

        for (T record : records) {
            try {
                Object value = component.getAccessor().invoke(record);
                values.add(value);
            } catch (Exception e) {
                values.add(null);
            }
        }

        return values;
    }
    
    private ByteBuffer encodeColumnValues(List<Object> values, ParquetDataType dataType) throws IOException {
        // This is a simplified PLAIN encoding implementation
        // Real Parquet supports multiple encodings: PLAIN, DICTIONARY, RLE, DELTA, etc.
        
        ByteBuffer buffer = ByteBuffer.allocate(estimateEncodedSize(values, dataType));
        
        for (Object value : values) {
            if (value == null) {
                // Handle null values (would need proper definition levels in real implementation)
                continue;
            }
            
            switch (dataType) {
                case BOOLEAN -> buffer.put((byte) (((Boolean) value) ? 1 : 0));
                case INT32 -> buffer.putInt((Integer) value);
                case INT64 -> buffer.putLong((Long) value);
                case FLOAT -> buffer.putFloat((Float) value);
                case DOUBLE -> buffer.putDouble((Double) value);
                case BYTE_ARRAY -> {
                    byte[] bytes = value.toString().getBytes();
                    buffer.putInt(bytes.length);
                    buffer.put(bytes);
                }
                default -> throw new UnsupportedOperationException("Data type not supported: " + dataType);
            }
        }
        
        buffer.flip();
        return buffer;
    }
    
    private int estimateEncodedSize(List<Object> values, ParquetDataType dataType) {
        // Rough estimation for buffer allocation
        int baseSize = switch (dataType) {
            case BOOLEAN -> 1;
            case INT32, FLOAT -> 4;
            case INT64, DOUBLE -> 8;
            case BYTE_ARRAY -> 50; // Rough estimate
            default -> 10;
        };
        
        return values.size() * baseSize + 1024; // Extra padding
    }
    
    private ByteBuffer compress(ByteBuffer data, ParquetCompressionCodec codec) throws IOException {
        if (codec == ParquetCompressionCodec.UNCOMPRESSED) {
            return data;
        }
        
        byte[] uncompressed = new byte[data.remaining()];
        data.get(uncompressed);
        
        byte[] compressed = switch (codec) {
            case SNAPPY -> {
                try {
                    yield org.xerial.snappy.Snappy.compress(uncompressed);
                } catch (Exception e) {
                    throw new IOException("Failed to compress with SNAPPY", e);
                }
            }
            case GZIP -> {
                try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                     java.util.zip.GZIPOutputStream gzipOut = new java.util.zip.GZIPOutputStream(baos)) {
                    gzipOut.write(uncompressed);
                    gzipOut.finish();
                    yield baos.toByteArray();
                } catch (Exception e) {
                    throw new IOException("Failed to compress with GZIP", e);
                }
            }
            case LZ4 -> {
                try {
                    net.jpountz.lz4.LZ4Factory factory = net.jpountz.lz4.LZ4Factory.fastestInstance();
                    net.jpountz.lz4.LZ4Compressor compressor = factory.fastCompressor();
                    int maxCompressedLength = compressor.maxCompressedLength(uncompressed.length);
                    byte[] compressedBuffer = new byte[maxCompressedLength];
                    int compressedLength = compressor.compress(uncompressed, 0, uncompressed.length, 
                                                             compressedBuffer, 0, maxCompressedLength);
                    byte[] result = new byte[compressedLength];
                    System.arraycopy(compressedBuffer, 0, result, 0, compressedLength);
                    yield result;
                } catch (Exception e) {
                    throw new IOException("Failed to compress with LZ4", e);
                }
            }
            default -> throw new UnsupportedOperationException("Compression codec not supported: " + codec);
        };
        
        return ByteBuffer.wrap(compressed);
    }
    
    private void writePageHeader(FileChannel channel, int compressedPageSize, 
                               int uncompressedPageSize, int numValues) throws IOException {
        
        // Simplified page header (real implementation would use Thrift)
        ByteBuffer header = ByteBuffer.allocate(16);
        header.putInt(compressedPageSize);
        header.putInt(uncompressedPageSize);
        header.putInt(numValues);
        header.putInt(0); // Padding
        header.flip();
        
        channel.write(header);
    }
    
    private void writeFooter(FileChannel channel, ParquetSchema schema, List<ParquetRowGroup> rowGroups)
            throws IOException {
        
        // This is a simplified footer implementation
        // Real Parquet uses Thrift serialization for the footer
        
        ByteBuffer footer = ByteBuffer.allocate(1024);
        
        // Write schema info
        footer.putInt(schema.getColumnCount());
        for (ParquetColumn column : schema.getColumns()) {
            writeString(footer, column.getName());
            footer.putInt(column.getDataType().ordinal());
        }
        
        // Write row group info
        footer.putInt(rowGroups.size());
        for (ParquetRowGroup rowGroup : rowGroups) {
            footer.putLong(rowGroup.getNumRows());
            footer.putLong(rowGroup.getTotalByteSize());
            footer.putInt(rowGroup.getColumnChunks().size());
            
            for (ParquetColumnChunk chunk : rowGroup.getColumnChunks()) {
                writeString(footer, chunk.getColumnPath());
                footer.putLong(chunk.getFileOffset());
                footer.putInt(chunk.getCompressedSize());
                footer.putInt(chunk.getUncompressedSize());
                footer.putLong(chunk.getNumValues());
            }
        }
        
        // Write custom metadata
        footer.putInt(metadata.size());
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            writeString(footer, entry.getKey());
            writeString(footer, entry.getValue().toString());
        }
        
        footer.flip();
        channel.write(footer);
    }
    
    private void writeString(ByteBuffer buffer, String str) {
        byte[] bytes = str.getBytes();
        buffer.putInt(bytes.length);
        buffer.put(bytes);
    }
    
    private void writeFooterLength(FileChannel channel, int footerLength) throws IOException {
        ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
        lengthBuffer.putInt(footerLength);
        lengthBuffer.flip();
        channel.write(lengthBuffer);
    }
}