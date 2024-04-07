package City;

import java.util.HashMap;
import java.util.Map;

public enum City {
    MONTREAL("MTL"),
    QUEBEC("QUE"),
    SHERBROOKE("SHE");


    private final String code;

    City(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    private static final Map<String, City> BY_CODE = new HashMap<>();

    static {
        for (City c : values()) {
            BY_CODE.put(c.code, c);
        }
    }

    public static City detectCity(String code) {
        code = code.toUpperCase().substring(0, 3);

        City city = BY_CODE.get(code);
        if (city == null) {
            throw new IllegalArgumentException("Invalid city code: " + code);
        }
        return city;
    }
}
