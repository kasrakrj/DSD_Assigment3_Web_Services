package Appointment;

import java.util.HashMap;
import java.util.Map;

public enum AppointmentTime {
    MORNING("M"),
    AFTERNOON("A"),
    EVENING("E");

    private final String code;

    AppointmentTime(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    private static final Map<String, AppointmentTime> BY_CODE = new HashMap<>();

    static{
        for(AppointmentTime appointmentTime : values()){
            BY_CODE.put(appointmentTime.code, appointmentTime);
        }
    }

    public static AppointmentTime detectAppointmentTime(String appointmentID){
        String timeCode = appointmentID.toUpperCase().substring(3,4);

        AppointmentTime appointmentTime = BY_CODE.get(timeCode);
        if (appointmentTime == null){
            throw new IllegalArgumentException("Invalid appointment time: " + timeCode);
        }
        return appointmentTime;
    }

}
