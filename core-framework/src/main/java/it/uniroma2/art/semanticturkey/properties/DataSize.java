package it.uniroma2.art.semanticturkey.properties;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonDeserialize(using= DataSize.DataSizeDeserializer.class)
@JsonSerialize(using = DataSize.DataSizeSerializer.class)
public class DataSize implements Comparable<DataSize> {
    public static class DataSizeDeserializer extends StdDeserializer<DataSize> {

        public DataSizeDeserializer(Class<?> vc) {
            super(vc);
        }

        public DataSizeDeserializer() {
            super(DataSize.class);
        }

        @Override
        public DataSize deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return DataSize.valueOf(p.getText());
        }
    }

    public static class DataSizeSerializer extends StdSerializer<DataSize> {

        public DataSizeSerializer(Class<DataSize> t) {
            super(t);
        }

        public DataSizeSerializer() {
            super(DataSize.class);
        }

        @Override
        public void serialize(DataSize value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.stringValue());
        }
    }

    public String stringValue() {
        return value + " " + dataUnit.getSymbol();
    }

    private long value;
    private DataUnit dataUnit;

    public static enum DataUnit {
        B("B", 1l), kB("kB", 1000l), MB("MB", 1000000l), GB("GB", 1000000000l), TB("TB", 1000000000000l),
        KiB("KiB", 2l << 10), MiB("MiB", 2l << 20), GiB("GiB", 2l << 30), TiB("TiB", 2l << 40);

        private final String symbol;

        private final long multiplier;
        DataUnit(String symbol, long multiplier) {
            this.symbol = symbol;
            this.multiplier = multiplier;
        }

        public String getSymbol() {
            return symbol;
        }

        public long getMultiplier() {
            return multiplier;
        }

    }
    public DataSize(long value, DataUnit dataUnit) {
        if (value < 0) throw new IllegalArgumentException("Value may not be negative");
        if (dataUnit == null) throw new IllegalArgumentException("Data unit may not be null");

        this.value = value;
        this.dataUnit = dataUnit;
    }

    public long getValue() {
        return value;
    }

    public DataUnit getDataUnit() {
        return dataUnit;
    }

    public BigInteger toBigIntegerBytes() {
        return BigInteger.valueOf(this.value).multiply(BigInteger.valueOf(this.dataUnit.getMultiplier()));
    }


    @Override
    public int compareTo(DataSize o) {
        return this.toBigIntegerBytes().compareTo(o.toBigIntegerBytes());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSize dataSize = (DataSize) o;
        return value == dataSize.value && dataUnit == dataSize.dataUnit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, dataUnit);
    }

    @Override
    public String toString() {
        return "DataSize{" +
                "value=" + value +
                ", dataUnit=" + dataUnit +
                '}';
    }

    private static Pattern DATA_SIZE_PATTERN = Pattern.compile("^\\s*(?<value>[1-9]\\d*|0)\\s*(?<unit>[A-Za-z]+)\\s*$");

    public static DataSize valueOf(String text) {
        Matcher m = DATA_SIZE_PATTERN.matcher(text);
        if (!m.find()) {
            throw new IllegalArgumentException("Illegal data size: " + text);
        }

        String dataUnitValue = m.group("value");
        String dataUnitSymbol = m.group("unit");

        DataUnit dataUnit = Arrays.stream(DataUnit.values()).filter(du -> du.getSymbol().equals(dataUnitSymbol)).findAny().orElseThrow(() -> new IllegalArgumentException("Illegal data unit: " + dataUnitSymbol));
        return new DataSize(Long.valueOf(dataUnitValue), dataUnit);
    }

    public static void main(String[] args) throws IOException {
        System.out.println(DataSize.valueOf("2000 KiB"));
        System.out.println(DataSize.valueOf("1 MiB"));
        System.out.println(DataSize.valueOf("2000 KiB").compareTo(DataSize.valueOf("1 MiB")));
        ObjectMapper om = new ObjectMapper();
        System.out.println(om.readValue("\"1 KiB\"", DataSize.class));
        System.out.println(om.valueToTree(DataSize.valueOf("2000 KiB")));

    }
}
