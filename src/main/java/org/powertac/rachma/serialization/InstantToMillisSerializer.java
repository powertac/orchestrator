package org.powertac.rachma.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.Instant;

public class InstantToMillisSerializer extends StdSerializer<Instant> {

    public InstantToMillisSerializer() {
        super(Instant.class);
    }

    @Override
    public void serialize(Instant instant, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeNumber(instant.toEpochMilli());
    }
}
