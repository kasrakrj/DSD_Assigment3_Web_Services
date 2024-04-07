package User;

import City.City;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class User {
    private final String userID;
    private final City city;
    private final UserType userType;

    private int outOfTownAppointments;

    private final Map<String, ArrayList<String>> appointmentsMap;
    private final ArrayList<String> physicianAppointments;
    private final ArrayList<String> dentalAppointments;
    private final ArrayList<String> surgeonAppointments;

    public User(String userID) throws IllegalArgumentException{
        userID = userID.toUpperCase();

        Integer.parseInt(userID.substring(4,8));

        this.userID = userID;
        this.city = City.detectCity(userID);
        this.userType = UserType.detectUserType(userID);

        outOfTownAppointments = 0;

        this.appointmentsMap = new ConcurrentHashMap<String, ArrayList<String>>();
        this.physicianAppointments = new ArrayList<>();
        this.dentalAppointments = new ArrayList<>();
        this.surgeonAppointments = new ArrayList<>();

        this.appointmentsMap.put("PHYSICIAN", this.physicianAppointments);
        this.appointmentsMap.put("DENTAL", this.dentalAppointments);
        this.appointmentsMap.put("SURGEON", this.surgeonAppointments);
    }

    public UserOperationStatus bookAppointment(String appointmentID, String appointmentType){
        ArrayList<String> appointmentArray = appointmentsMap.get(appointmentType);
        if(!hasConflict(appointmentID)){
            if (City.detectCity(appointmentID) != city){
                if (outOfTownAppointments >= 3){
                    return UserOperationStatus.OUT_OF_TOWN_LIMIT_REACHED;
                }else {
                    outOfTownAppointments ++;
                }
            }
            appointmentArray.add(appointmentID);
            return UserOperationStatus.ADD_APPOINTMENT_SUCCESS;
        }else {
            return UserOperationStatus.APPOINTMENT_HAS_CONFLICT;
        }
    }

    public UserOperationStatus removeAppointment(String appointmentID, String appointmentType){
        ArrayList<String> appointmentArray = appointmentsMap.get(appointmentType);
        if(appointmentArray.contains(appointmentID)){
            if (City.detectCity(appointmentID) != city){
                outOfTownAppointments --;
            }
            appointmentArray.remove(appointmentID);
            return UserOperationStatus.REMOVE_APPOINTMENT_SUCCESS;
        }else {
            return UserOperationStatus.APPOINTMENT_NOT_FOUND;
        }
    }

    public boolean hasConflict(String appointmentID){
        for(ArrayList<String> appointmentArray : appointmentsMap.values()){
            for (String appointment_id: appointmentArray){
                if(appointment_id.substring(3,10).equals(appointmentID.substring(3,10))){
                    return true;
                }
            }
        }
        return false;
    }

    public boolean canBookOutOfTownAppointment(){
        return outOfTownAppointments < 3;
    }

    public String getUserID() {
        return userID;
    }
    public City getCity() {
        return city;
    }
    public UserType getUserType() {
        return userType;
    }
    public Map<String, ArrayList<String>> getAppointmentsMap() {
        return appointmentsMap;
    }
}
