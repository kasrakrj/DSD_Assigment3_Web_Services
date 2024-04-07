import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    public static void main(String[] args) {
       Map<Integer, String> map = new ConcurrentHashMap<>();

       for (String string: map.values()){
           System.out.println(string);
       }
    }
}
