package com.jfrog.ide.idea.ui.jcef.message;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import static com.jfrog.ide.common.utils.Utils.createMapper;

public class MessagePipeSupport {

    public static String Pack(String type, Object data) throws JsonProcessingException {
        ObjectMapper ow = createMapper();
        return ow.writeValueAsString(new PackedMessage(type, data));
    }
}
