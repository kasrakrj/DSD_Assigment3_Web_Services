package Appointment;

public enum AppointmentType {
    PHYSICIAN,
    SURGEON,
    DENTAL;

    public static AppointmentType detectAppointmentType(String input) {
        return AppointmentType.valueOf(input.toUpperCase());
    }
}
