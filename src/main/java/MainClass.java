import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.Model;
import utils.Utils;

import java.io.FileNotFoundException;
import java.io.FileReader;

public class MainClass {
    public static void main(String[] args) throws ClassNotFoundException, FileNotFoundException {
        compare();
    }

    public static void compare() throws FileNotFoundException {
//        创建json解析器
        JsonParser parser = new JsonParser();
        // 使用解析器解析json数据，返回值是JsonElement，强制转化为其子类JsonObject类型
        JsonObject object = (JsonObject) parser.parse(new FileReader("json"));

        JsonObject result = Utils.addMatchProperty2JsonObject(object);

        //TODO 可以用包名，然后使用配置文件来进行读取
        Utils.matchClass2Json(result, Model.class);

        System.out.println(result.toString());
        System.out.println("\n\n\n");

        Utils.printUnusedField(result,0);
    }
}
