package org.powertac.orchestrator.user;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.powertac.orchestrator.user.domain.User;

import java.io.IOException;

public class UserIdSerializer extends StdSerializer<User> {

    public UserIdSerializer() {
        super(User.class);
    }

    @Override
    public void serialize(User user, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(user != null ? user.getId() : null);
    }

}
