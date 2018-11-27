package com.chau.ching.io.util;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.sql.Timestamp;

public class TimestampAdapter implements JsonSerializer<Timestamp>, JsonDeserializer<Timestamp> {

@Override
public Timestamp deserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) throws JsonParseException {
        if(json == null){
        return null;
        } else {
        try {
        return new Timestamp(json.getAsLong());
        } catch (Exception e) {
        return null;
        }
        }
        }

@Override
public JsonElement serialize(Timestamp src, Type typeOfSrc,
        JsonSerializationContext context) {
        String value = "";
        if(src != null){
        value = String.valueOf(src.getTime());
        }
        return new JsonPrimitive(value);
        }
        }
