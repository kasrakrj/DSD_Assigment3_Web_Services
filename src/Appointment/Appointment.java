package Appointment;

import City.City;

import java.util.ArrayList;

public class Appointment {
    private final String appointmentID;

    private final City city;
    private final AppointmentTime appointmentTime;
    private final AppointmentType appointmentType;
    private final String date;

    private int capacity;
    private final ArrayList<String> patientIDs;

    public Appointment(String appointmentID, int capacity, String appointmentType){
        appointmentID = appointmentID.toUpperCase();

        this.appointmentID  = appointmentID;

        this.capacity = capacity;
        this.patientIDs = new ArrayList<>();

        this.city = City.detectCity(appointmentID);
        this.appointmentTime = AppointmentTime.detectAppointmentTime(appointmentID);
        this.appointmentType = AppointmentType.detectAppointmentType(appointmentType);
        this.date = appointmentID.substring(4, 10);
    }

    public AppointmentOperationStatus addPatient(String patientID){
        if(0 < capacity){
            patientIDs.add(patientID);
            capacity--;
            return AppointmentOperationStatus.ADD_PATIENT_SUCCESS;
        }
        return AppointmentOperationStatus.APPOINTMENT_FULL;
    }

    public AppointmentOperationStatus removePatient(String patientID){
        if(patientIDs.contains(patientID)){
            patientIDs.remove(patientID);
            capacity++;
            return AppointmentOperationStatus.REMOVE_PATIENT_SUCCESS;
        }
        return AppointmentOperationStatus.PATIENT_ID_NOT_FOUND;
    }

    public boolean hasCapacity(){
        return capacity > 0;
    }

    public ArrayList<String> getPatientIDs() {
        return patientIDs;
    }
    public String getAppointmentID() {
        return appointmentID;
    }

    @Override
    public String toString() {
        return "AppointmentID: " + appointmentID + " Time: " + appointmentTime + " Date: " + date.substring(0,2) + "/" + date.substring(2,4) + "/" + date.substring(4,6) + " Capacity: " + capacity;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Appointment) {
            Appointment appointment = (Appointment) obj;
            return appointmentID.equals(appointment.appointmentID) && appointmentType.equals(appointment.appointmentType);
        }else {
            return false;
        }
    }



}
