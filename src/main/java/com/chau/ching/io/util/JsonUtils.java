package com.chau.ching.io.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class JsonUtils {

    public static Map<String, Object> getMap(String jsonString)
    {
        try
        {
            JSONObject jsonObject = new JSONObject();
            jsonObject = JSONObject.fromObject(jsonString);

            Iterator keyIter = jsonObject.keys();

            Map valueMap = new HashMap();
            while (keyIter.hasNext()) {
                String key = (String)keyIter.next();
                Object value = jsonObject.get(key);
                valueMap.put(key, value);
            }
            return valueMap;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object jsonToObj(String json, Class clazz)
    {
        return getGson().fromJson(json, clazz);
    }

    public static Gson getGson()
    {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setDateFormat("yyyy-MM-dd HH:mm:ss");
        gsonBuilder.registerTypeAdapter(Timestamp.class,
                new TimestampAdapter());
        Gson GSON = gsonBuilder.create();
        return GSON;
    }

    public static String toJson(Object obj)
    {
        return getGson().toJson(obj);
    }

    public static String toJson(Object obj, Type typeToken)
    {
        return getGson().toJson(obj, typeToken);
    }


}
