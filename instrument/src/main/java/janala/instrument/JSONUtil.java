package janala.instrument;

import java.util.HashMap;
import java.util.List;
import java.io.FileNotFoundException;
import java.io.FileReader;

import java.io.IOException;
import java.nio.file.Path;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class JSONUtil {

    public static Map<String, List<Map.Entry<String, String>>> loadFunctoinFromStaticAnalysis(Path fileName) {
        JSONParser jsonParser = new JSONParser();
        Map<String, List<Map.Entry<String, String>>> funcToInst = new HashMap<>();

        try (FileReader reader = new FileReader(fileName.toFile()))
        {
            Object obj = jsonParser.parse(reader);
            JSONObject jsonObject = (JSONObject) obj;
            Set<Map.Entry<String, String>> entrySet = jsonObject.entrySet();

            for (Map.Entry<String, String> e : entrySet) {              
                String[] ret = e.getValue().split(e.getKey().replace("$", "\\$") + "\\(");
                assert(ret.length == 2);
                String ClassName = ret[0].substring(0, ret[0].length() - 1);
                String MethodName = e.getKey();
                String ParamDesc = "(" + ret[1];
                if (funcToInst.containsKey(ClassName)) {
                    funcToInst.get(ClassName).add(new AbstractMap.SimpleEntry<>(MethodName, ParamDesc));
                } else {
                    List<Map.Entry<String,String>> list = new ArrayList<>();
                    list.add(new AbstractMap.SimpleEntry<>(MethodName, ParamDesc));
                    funcToInst.put(ClassName, list);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return funcToInst;
    }
    
}
