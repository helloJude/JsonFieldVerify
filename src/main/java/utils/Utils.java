package utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 判断后端字段和客户端的差异
 * 只能判断  ------》 后端有的，客户端没用到
 */
public class Utils {
    public static final String HITED = "$isHited";
    public static Logger logger = Logger.getLogger(Utils.class.getName());

    public static HashMap<String, Integer> emptyArrayMap = new HashMap<String, Integer>();
    public static HashMap<String, Integer> unUsedFiedMap = new HashMap<String, Integer>();


    public static JsonObject addMatchProperty2JsonObject(JsonObject object) {
        Iterator iterator = object.keySet().iterator();
        while (iterator.hasNext()) {
            // 获得key
            String key = iterator.next().toString();
            // 根据key获得value, value也可以是JSONObject,JSONArray,使用对应的参数接收即可
            JsonElement value = object.get(key);
            if (value.isJsonObject()) {
                ((JsonObject) value).addProperty(HITED, false);
                addMatchProperty2JsonObject((JsonObject) value);
            } else if (value.isJsonArray()) {
                addMatchProperty2JsonArray((JsonArray) value);
            } else if (value.isJsonPrimitive()) {
                object.addProperty(key, false);
            } else if (value.isJsonNull()) {
                object.addProperty(key, false);
            } else {
                System.out.println("无此类型 ");
            }
        }
        object.addProperty(HITED, false);
        return object;
    }

    public static void addMatchProperty2JsonArray(JsonArray array) {
        for (int i = 0; i < array.size(); i++) {
            JsonElement value = array.get(i);
            if (value.isJsonObject()) {
                addMatchProperty2JsonObject((JsonObject) value);
            } else if (value.isJsonArray()) {
                addMatchProperty2JsonArray((JsonArray) value);
            } else if (value.isJsonPrimitive()) {//当为基本类型或null时字段

            } else if (value.isJsonNull()) {
            } else {
                System.out.println("无此类型 ");
            }
        }
    }

    public static void matchClass2Json(JsonObject jsonObject, Class cls) {
        if (jsonObject == null) {// 如果 dataclass 中有的字段，Json中没有，那么会返回 null
            return;
        }
        jsonObject.addProperty(HITED, true);
        Field[] fields = cls.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Class clazz = fields[i].getType();//获取field对应的class
            if (!clazz.isPrimitive() && !isWrapClass(clazz)) {
                if (List.class.isAssignableFrom(clazz)) {//判断 clazz 是否是 List 的子类
                    Type t = fields[i].getGenericType();
                    if (ParameterizedType.class.isAssignableFrom(t.getClass())) {
                        //这里只取了第一个类型，实际有N个类型
                        Type t1 = ((ParameterizedType) t).getActualTypeArguments()[0];
                        JsonElement jsonElement = jsonObject.get(getModelFieldName(fields[i]));
                        if (jsonElement != null) {
                            if (ParameterizedType.class.isAssignableFrom(t1.getClass())) {
                                matchParameterizedType2Json((JsonArray) jsonObject.get(getModelFieldName(fields[i])), (ParameterizedType) t);
                            } else {
                                if (!((Class) t1).isPrimitive() && !String.class.isAssignableFrom((Class) t1) && !isWrapClass((Class) t1)) {
                                    if (jsonElement.isJsonArray() && ((JsonArray) jsonElement).size() > 0) {
                                        JsonArray temp = ((JsonArray) jsonElement);
                                        for (int j = 0; j < temp.size(); j++) {
                                            matchClass2Json((JsonObject) (temp.get(j)), (Class) t1);
                                        }
                                    } else if (((JsonArray) jsonElement).size() == 0) {//当为[]时应该怎么处理
                                        if (!emptyArrayMap.containsKey(getModelFieldName(fields[i]))) {
                                            logger.warning(getModelFieldName(fields[i]) + "  is hited ,but its [] in json ，please check it---------");
                                        }
                                        emptyArrayMap.put(getModelFieldName(fields[i]), 1);
                                    }
                                } else {//当array为基本类型或String或封装类时，只用比较字段JsonArray名称即可，并置为true
                                    jsonObject.remove(getModelFieldName(fields[i]));
                                    jsonObject.addProperty(getModelFieldName(fields[i]), true);
                                }
                            }
                        } else {
                            logger.warning(getModelFieldName(fields[i]) + "cannot find in json ---------- ");
                        }
                    }
                } else if (Object.class.isAssignableFrom(clazz)) {
                    if (String.class.isAssignableFrom(clazz)) {
                        jsonObject.addProperty(getModelFieldName(fields[i]), true);
                    } else {
                        JsonElement jsonElement = jsonObject.get(getModelFieldName(fields[i]));
                        if (JsonElement.class.isAssignableFrom(clazz)){
                            //当为JsonElement时，直接视为 primitive
                            jsonObject.remove(getModelFieldName(fields[i]));
                            jsonObject.addProperty(getModelFieldName(fields[i]),true);
                        }else {
                            if (jsonElement != null) {
                                ((JsonObject) jsonElement).addProperty(HITED, true);
                            }
                            matchClass2Json((JsonObject) jsonObject.get(getModelFieldName(fields[i])), clazz);
                        }
                    }
                }
            } else {
                jsonObject.addProperty(getModelFieldName(fields[i]), true);
            }
        }

    }

    /**
     * 当接口为 List<List<xxx>>时需要兼容
     *
     * @param jsonArray
     * @param parameterizedType
     */
    public static void matchParameterizedType2Json(JsonArray jsonArray, ParameterizedType parameterizedType) {
        if (ParameterizedType.class.isAssignableFrom(parameterizedType.getClass())) {
            Type t1 = ((ParameterizedType) parameterizedType).getActualTypeArguments()[0];
            if (ParameterizedType.class.isAssignableFrom(t1.getClass())) {
                for (int i = 0; i < jsonArray.size(); i++) {
                    matchParameterizedType2Json((JsonArray) jsonArray.get(i), (ParameterizedType) t1);
                }
            } else {
                for (int i = 0; i < jsonArray.size(); i++) {
                    matchClass2Json((JsonObject) jsonArray.get(i), (Class) t1);
                }
            }
        }
    }

    public static void printUnusedField(JsonObject object, int deep) {
        logger.setLevel(Level.INFO);
        Iterator iterator = object.keySet().iterator();
        while (iterator.hasNext()) {
            // 获得key
            String key = iterator.next().toString();
            // 根据key获得value, value也可以是JSONObject,JSONArray,使用对应的参数接收即可
            JsonElement value = object.get(key);
            if (value.isJsonObject()) {
                JsonElement jsonElement = ((JsonObject) value).get(HITED);
                if (jsonElement.getAsBoolean()) {
                    printUnusedField((JsonObject) value, ++deep);
                    --deep;//递归完成后需要回退，这样才能保持状态正确性
                } else {
                    System.out.println("JsonObject --  " + key + "  -- not find");
                }
            } else if (value.isJsonArray()) {
                boolean  flag = true ;//后端 jsonArray 有字段，本地 model 无字段时

                if (((JsonArray) value).size() == 0) {
                    if (!emptyArrayMap.containsKey(key)) {
                        System.out.println("unused array field   " + key);
                    }
                }else{
                    JsonArray tempArray = ((JsonArray)value);
                    for (int i =0;i < tempArray.size();i++){
                        JsonElement temp = tempArray.get(i);
                        if (temp.isJsonObject()) {
                            if(!((JsonObject)temp).get(HITED).getAsBoolean()){
                                flag = false;
                            }
                        }else if (temp.isJsonPrimitive()){
                            flag = false;
                        }
                    }
                }
                if (flag) {
                    printUnusedFieldInArray((JsonArray) value, ++deep);
                }else{
                    System.out.println("unused array field   " + key);
                }
                --deep;
            } else if (value.isJsonPrimitive()) {
                if (!value.getAsBoolean() && !HITED.equals(key)) {
                    if (!unUsedFiedMap.containsKey(key + "" + deep)) {
                        unUsedFiedMap.put(key + "" + deep, 1);
                        System.out.println("unused field   " + key + "    deep = " + deep);
                    }
                }
            } else if (value.isJsonNull()) {
            } else {
                System.out.println("无此类型 ");
            }
        }
    }

    public static void printUnusedFieldInArray(JsonArray array, int deep) {
        if (array.size() > 0) {
            for (int i = 0; i < array.size(); i++) {
                JsonElement value = array.get(i);
                if (value.isJsonObject()) {
                    printUnusedField((JsonObject) value, ++deep);
                    --deep;
                } else if (value.isJsonArray()) {
                    printUnusedFieldInArray((JsonArray) value, ++deep);
                    --deep;
                } else if (value.isJsonPrimitive()) {//前边处理过，不存在此情况
                } else if (value.isJsonNull()) {////前边处理过，不存在此情况
                } else {
                    System.out.println("无此类型 ");
                }
            }
        }
    }

    public static boolean isWrapClass(Class clz) {
        try {
            return ((Class) clz.getField("TYPE").get(null)).isPrimitive();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 加入对SerializedName的兼容
     * @param field
     * @return
     */
    public static String getModelFieldName(Field field) {
        SerializedName name = field.getAnnotation(SerializedName.class);
        if (name != null) {
            return name.value();
        }
        return field.getName();
    }
}
