package User;

import java.util.HashMap;
import java.util.Map;

public enum UserType {
    ADMIN("A"),
    PATIENT("P");

    private final String code;
    UserType(String code) {
        this.code = code;
    }

    private static final Map<String, UserType> BY_CODE = new HashMap<>();

    static{
        for (UserType userType: values()){
            BY_CODE.put(userType.code, userType);
        }
    }

    public static UserType detectUserType(String userID) {
        String userTypeCode = userID.toUpperCase().substring(3, 4);

        UserType userType = BY_CODE.get(userTypeCode);
        if (userType == null){
            throw new IllegalArgumentException("Invalid user type: " + userTypeCode);
        }
        return userType;
    }


}
