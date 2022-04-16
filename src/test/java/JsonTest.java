import com.cracker.code.cson.*;

import java.util.*;

public class JsonTest {

    static class TypeInner {

        private int age;
        private String name;
        private List<? extends Number> numbers = new ArrayList<>();


        public TypeInner(int age, String name) {
            this.age = age;
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<? extends Number> getNumbers() {
            return numbers;
        }

        public void setNumbers(List<? extends Number> numbers) {
            this.numbers = numbers;
        }

        @Override
        public String toString() {
            return "TypeInner[age=" + this.getAge() +
                    ", name=" + this.getName() + ", numbers=" + this.getNumbers() + "]";
        }
    }

    public static void main(String[] args) {

        // 任意对象(支持任意嵌套)转换为json String. start ------>
        Cson cson = new Cson();

        TypeInner inner = new TypeInner(20, "world");
        List<Integer> paramList = new ArrayList<>();
        paramList.add(1);
        paramList.add(2);
        paramList.add(3);
        inner.setNumbers(paramList);
        // 打印：{"age":20,"name":"world","numbers":[1,2,3]}
        System.out.println(cson.toJson(inner));

        Map<String, List<Integer>> params = new HashMap<>();
        List<Integer> simpleInteger = new ArrayList<>();
        simpleInteger.add(1);
        simpleInteger.add(2);
        params.put("result", simpleInteger);
        // 打印：{"result":[1,2]}
        System.out.println(cson.toJson(params));
        // <------ end. 任意对象(支持任意嵌套)转换为json String



        // json String转换为对象. start ------>
        String jsonStr = "{\"age\":20,\"name\":\"world\",\"numbers\":[1,2,3]}";
        TypeInner typeInner = cson.fromJson(jsonStr, TypeInner.class);
        // 打印：TypeInner[age=20, name=world, numbers=[1, 2, 3]]
        System.out.println(typeInner);
        // <------ end. json String转换为对象.




        // json String转换为JsonObject. start ------>
        String modifiedTarget = "{\"age\":20,\"name\":\"world\",\"numbers\":[1,2,3]}";
        JsonObject jsonObject = new JsonParser().parse(modifiedTarget).getAsJsonObject();
        // 打印：20
        System.out.println(jsonObject.get("age").getAsInt());
        // 打印：world
        System.out.println(jsonObject.get("name").getAsString());

        JsonArray array = jsonObject.get("numbers").getAsJsonArray();
        for (JsonElement element : array) {
            // 依次打印1、2、3
            System.out.println(element.getAsInt());
        }
        // <------ end. json String转换为对象.
    }

}
