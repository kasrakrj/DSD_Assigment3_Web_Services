package DHMS;

import Appointment.*;
import City.City;
import Server.HospitalServer;
import User.*;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static City.City.detectCity;

@WebService(endpointInterface = "DHMS.DHMSInterface")
@SOAPBinding(style = Style.RPC)
public class DHMS implements DHMSInterface{
    private final String serverID;
    private final Map<String, Map<String, Appointment>> database;

    private final Map<String, User> patientsMap;
    private final Map<String, User> adminMap;

    public DHMS(String serverID) {
        this.serverID = serverID;

        patientsMap = new ConcurrentHashMap<>();
        adminMap = new ConcurrentHashMap<>();

        database = new ConcurrentHashMap<>();

        database.put(AppointmentType.PHYSICIAN.toString(), new ConcurrentHashMap<String, Appointment>());
        database.put(AppointmentType.SURGEON.toString(), new ConcurrentHashMap<String, Appointment>());
        database.put(AppointmentType.DENTAL.toString(), new ConcurrentHashMap<String, Appointment>());
    }

    @Override
    public String helloWorld(String name) {
        return "Hello world from " + name;
    }

    @Override
    public String login(String userID){
        try {
            User user = new User(userID);
            if (user.getUserType() == UserType.PATIENT){
                if (!patientsMap.containsKey(user.getUserID())){
                    patientsMap.put(user.getUserID(), user);
                    return "New Patient logged in";
                }else {
                    return "Patient logged in successfully";
                }
            }else {
                if (!adminMap.containsKey(user.getUserID())){
                    adminMap.put(user.getUserID(), user);
                    return "New Admin logged in";
                }else {
                    return "Admin logged in successfully";
                }
            }
        }catch (Exception e){
            return e.getMessage();
        }
    }

    @Override
    public String addAppointment(String appointmentID, String appointmentType, int capacity) {
        appointmentID = appointmentID.toUpperCase();
        appointmentType = appointmentType.toUpperCase();

        if (detectCity(appointmentID) != detectCity(serverID)){
            return "Appointment city does not match server city";
        }

        Map<String, Appointment> appointmentMap = database.get(appointmentType);
        if (appointmentMap != null){
            if (!appointmentMap.containsKey(appointmentID)){
                try {
                    appointmentMap.put(appointmentID, new Appointment(appointmentID, capacity, appointmentType));
                }catch (IllegalArgumentException e){
                    return e.getMessage();
                }
                return "appointment added successfully";
            }else {
                return "appointment already exists";
            }
        }else {
            return "Invalid appointment type: " + appointmentType;
        }
    }

    @Override
    public String removeAppointment(String appointmentID, String appointmentType) {
        appointmentID  = appointmentID.toUpperCase();
        appointmentType = appointmentType.toUpperCase();

        if (detectCity(appointmentID) != detectCity(serverID)){
            return "Appointment city does not match server city";
        }

        if (database.containsKey(appointmentType)) {
            Map<String, Appointment> appointmentMap = database.get(appointmentType);
            if (appointmentMap.containsKey(appointmentID)) {
                Appointment appointment = database.get(appointmentType).get(appointmentID);
                for (String patientID : appointment.getPatientIDs()) {
                    City patientCity = detectCity(patientID);
                    if (patientCity == detectCity(serverID)) {
                        User patient = patientsMap.get(patientID);
                        patient.removeAppointment(appointment.getAppointmentID(), appointmentType);
                        String nextAvailableAppointmentID = getNextAvailableAppointment(appointmentID, appointmentType);
                        if (nextAvailableAppointmentID != null){
                            bookAppointment(patientID, nextAvailableAppointmentID, appointmentType);
                        }
                    } else {
                        switch (patientCity) {
                            case MONTREAL:
                                sendUDPRequest(HospitalServer.MONTREAL_UDP_PORT, "REMOVE_APPOINTMENT", appointmentType, appointmentID, patientID);
                                break;
                            case QUEBEC:
                                sendUDPRequest(HospitalServer.QUEBEC_UDP_PORT, "REMOVE_APPOINTMENT", appointmentType, appointmentID, patientID);
                                break;
                            case SHERBROOKE:
                                sendUDPRequest(HospitalServer.SHERBROOKE_UDP_PORT, "REMOVE_APPOINTMENT", appointmentType, appointmentID, patientID);
                                break;
                        }
                    }
                }
                database.get(appointmentType).remove(appointmentID);
                return "Appointment removed successfully";
            } else {
                return "Appointment does not exist";
            }
        }else {
            return "invalid appointment type";
        }
    }

    @Override
    public String listAppointmentAvailability(String appointmentType) {
        appointmentType = appointmentType.toUpperCase();

        StringBuilder stringBuilder = new StringBuilder();
        if (database.containsKey(appointmentType)){
            stringBuilder.append(listAppointmentAvailabilityUDP(appointmentType));
            switch (serverID){
                case "MTL":
                    stringBuilder.append(sendUDPRequest(HospitalServer.SHERBROOKE_UDP_PORT, "LIST_APPOINTMENT_AVAILABILITY" , appointmentType, null, null));
                    stringBuilder.append(sendUDPRequest(HospitalServer.QUEBEC_UDP_PORT, "LIST_APPOINTMENT_AVAILABILITY" , appointmentType, null, null));
                    break;
                case "QUE":
                    stringBuilder.append(sendUDPRequest(HospitalServer.MONTREAL_UDP_PORT, "LIST_APPOINTMENT_AVAILABILITY" , appointmentType, null, null));
                    stringBuilder.append(sendUDPRequest(HospitalServer.SHERBROOKE_UDP_PORT, "LIST_APPOINTMENT_AVAILABILITY" , appointmentType, null, null));
                    break;
                case "SHE":
                    stringBuilder.append(sendUDPRequest(HospitalServer.MONTREAL_UDP_PORT, "LIST_APPOINTMENT_AVAILABILITY" , appointmentType, null, null));
                    stringBuilder.append(sendUDPRequest(HospitalServer.QUEBEC_UDP_PORT, "LIST_APPOINTMENT_AVAILABILITY" , appointmentType, null, null));
                    break;
                default:
                    break;
            }
            return stringBuilder.toString();


        }else {
            return "Invalid appointment type";
        }
    }

    @Override
    public String bookAppointment(String patientID, String appointmentID, String appointmentType) {
        patientID = patientID.toUpperCase();
        appointmentID = appointmentID.toUpperCase();
        appointmentType = appointmentType.toUpperCase();

        if (!patientsMap.containsKey(patientID)){
            return "Invalid patient ID";
        }

        User patient = patientsMap.get(patientID);

        if(patient.hasConflict(appointmentID)){
            return "Appointment has conflict";
        }

        if(detectCity(appointmentID) != patient.getCity()){
            if (!patient.canBookOutOfTownAppointment()){
                return "Patient has reached out of town appointment limit";
            }
        }

        if (database.containsKey(appointmentType)){
            try {
                if (detectCity(appointmentID) == detectCity(serverID)) {
                    Map<String, Appointment> db = database.get(appointmentType);
                    if (db.containsKey(appointmentID)) {
                        Appointment appointment = db.get(appointmentID);
                        switch (appointment.addPatient(patientID)) {
                            case APPOINTMENT_FULL:
                                return "Appointment is full";
                            case ADD_PATIENT_SUCCESS:
                                patientsMap.get(patientID).bookAppointment(appointmentID, appointmentType);
                                return "Appointment booked successfully";
                            default:
                                return "Unexpected return value from addPatient";
                        }
                    } else {
                        return "Appointment does not exist";
                    }
                } else {
                    String response;
                    switch (detectCity(appointmentID)) {
                        case MONTREAL:
                            response = sendUDPRequest(HospitalServer.MONTREAL_UDP_PORT, "BOOK_APPOINTMENT", appointmentType, appointmentID, patientID);
                            break;
                        case QUEBEC:
                            response = sendUDPRequest(HospitalServer.QUEBEC_UDP_PORT, "BOOK_APPOINTMENT", appointmentType, appointmentID, patientID);
                            break;
                        case SHERBROOKE:
                            response = sendUDPRequest(HospitalServer.SHERBROOKE_UDP_PORT, "BOOK_APPOINTMENT", appointmentType, appointmentID, patientID);
                            break;
                        default:
                            return "Invalid appointment city";
                    }
                    if (response.equals("Appointment booked successfully")) {
                        patientsMap.get(patientID).bookAppointment(appointmentID, appointmentType);
                    }
                    return response;
                }
            }catch (IllegalArgumentException e){
                return e.getMessage();
            }
        }else {
            return "Invalid appointment type";
        }
    }

    @Override
    public String getAppointmentSchedule(String patientID) {
        patientID = patientID.toUpperCase();
        if (patientsMap.containsKey(patientID)){
            User user = patientsMap.get(patientID);

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Patient ID: ").append(patientID).append("\nappointments:\n");

            for (Map.Entry<String, ArrayList<String>> entry: user.getAppointmentsMap().entrySet()){
                if (!entry.getValue().isEmpty()) {
                    stringBuilder.append(entry.getKey()).append(":\n");
                    for (String appointmentID : entry.getValue()) {
                        stringBuilder.append(appointmentID).append("\n");
                    }
                }
            }

            return stringBuilder.toString();
        }else {
            return "Invalid patient ID";
        }
    }

    @Override
    public String cancelAppointment(String patientID, String appointmentID, String appointmentType) {
        patientID = patientID.toUpperCase();
        appointmentID = appointmentID.toUpperCase();
        appointmentType = appointmentType.toUpperCase();

        if (patientsMap.containsKey(patientID)){
            User patient = patientsMap.get(patientID);
            if(detectCity(appointmentID) == detectCity(patientID)){
                if (database.containsKey(appointmentType)){
                    Map db = database.get(appointmentType);
                    if (db.containsKey(appointmentID)){
                        Appointment appointment = (Appointment) db.get(appointmentID);

                        StringBuilder stringBuilder = new StringBuilder();

                        switch(appointment.removePatient(patientID)){
                            case REMOVE_PATIENT_SUCCESS:
                                stringBuilder.append("Patient removed from appointment");
                                break;
                            case PATIENT_ID_NOT_FOUND:
                                stringBuilder.append("Patient not found in appointment");
                                break;
                            default:
                                stringBuilder.append("Unexpected return value from removePatient in appointment");
                        }

                        switch (patient.removeAppointment(appointmentID, appointmentType)){
                            case REMOVE_APPOINTMENT_SUCCESS:
                                stringBuilder.append(" & Appointment removed from patient");
                                break;
                            case APPOINTMENT_NOT_FOUND:
                                stringBuilder.append(" & Appointment not found in patient's schedule");
                                break;
                            default:
                                stringBuilder.append(" & Unexpected return value from removeAppointment in patient");
                        }

                        return stringBuilder.toString();
                    }else {
                        return "Appointment does not exist";
                    }
                }else {
                    return "Invalid appointment type";
                }
            }else {
                StringBuilder stringBuilder = new StringBuilder();
                switch (detectCity(appointmentID)){
                    case MONTREAL:
                        stringBuilder.append(sendUDPRequest(HospitalServer.MONTREAL_UDP_PORT, "CANCEL_APPOINTMENT", appointmentType, appointmentID, patientID));
                        break;
                    case QUEBEC:
                        stringBuilder.append(sendUDPRequest(HospitalServer.QUEBEC_UDP_PORT, "CANCEL_APPOINTMENT", appointmentType, appointmentID, patientID));
                        break;
                    case SHERBROOKE:
                        stringBuilder.append(sendUDPRequest(HospitalServer.SHERBROOKE_UDP_PORT, "CANCEL_APPOINTMENT", appointmentType, appointmentID, patientID));
                        break;
                    default:
                        return "Invalid appointment city";
                }

                switch (patient.removeAppointment(appointmentID, appointmentType)){
                    case REMOVE_APPOINTMENT_SUCCESS:
                        stringBuilder.append(" & Appointment removed from patient");
                        break;
                    case APPOINTMENT_NOT_FOUND:
                        stringBuilder.append(" & Appointment not found in patient's schedule");
                        break;
                    default:
                        stringBuilder.append(" & Unexpected return value from removeAppointment in patient");
                }

                return stringBuilder.toString();
            }

        }else {
            return "Patient ID not found";
        }
    }

    @Override
    public String swapAppointment(String patientID, String oldAppointmentID, String oldAppointmentType, String newAppointmentID, String newAppointmentType) {
        patientID = patientID.toUpperCase();
        oldAppointmentID = oldAppointmentID.toUpperCase();
        oldAppointmentType = oldAppointmentType.toUpperCase();
        newAppointmentID = newAppointmentID.toUpperCase();
        newAppointmentType = newAppointmentType.toUpperCase();

        if(patientsMap.containsKey(patientID)){
            User patient = patientsMap.get(patientID);
            if (patient.getAppointmentsMap().containsKey(oldAppointmentType)) {
                if (patient.getAppointmentsMap().get(oldAppointmentType).contains(oldAppointmentID)) {
                    if (detectCity(newAppointmentID) == detectCity(serverID)) {
                        if (database.containsKey(newAppointmentType)) {
                            if (database.get(newAppointmentType).containsKey(newAppointmentID)) {
                                Appointment newAppointment = database.get(newAppointmentType).get(newAppointmentID);
                                switch (newAppointment.addPatient(patientID)) {
                                    case APPOINTMENT_FULL:
                                        return "New appointment is full!";
                                    case ADD_PATIENT_SUCCESS:
                                        String response = cancelAppointment(patientID, oldAppointmentID, oldAppointmentType);
                                        if (response.equals("Patient removed from appointment & Appointment removed from patient")) {
                                            switch (patient.bookAppointment(newAppointmentID, newAppointmentType)) {
                                                case OUT_OF_TOWN_LIMIT_REACHED:
                                                    switch (patient.bookAppointment(oldAppointmentID, oldAppointmentType)) {
                                                        case OUT_OF_TOWN_LIMIT_REACHED:
                                                            newAppointment.removePatient(patientID);
                                                            return "Could not book new appointment due to out of town appointment limit & old appointment could not be booked back because out of town limit!";
                                                        case APPOINTMENT_HAS_CONFLICT:
                                                            newAppointment.removePatient(patientID);
                                                            return "Could not book new appointment due to to out of town appointment limit & old appointment could not be booked back due to appointment conflict!";
                                                        case ADD_APPOINTMENT_SUCCESS:
                                                            return "Could not book new appointment due to to out of town appointment limit & old appointment booked back successfully!";
                                                        default:
                                                            return "Unexpected return from booking old appointment!";
                                                    }
                                                case APPOINTMENT_HAS_CONFLICT:
                                                    switch (patient.bookAppointment(oldAppointmentID, oldAppointmentType)) {
                                                        case OUT_OF_TOWN_LIMIT_REACHED:
                                                            newAppointment.removePatient(patientID);
                                                            return "Could not book new appointment due to appointment conflict & old appointment could not be booked back because out of town limit!";
                                                        case APPOINTMENT_HAS_CONFLICT:
                                                            newAppointment.removePatient(patientID);
                                                            return "Could not book new appointment due to appointment conflict & old appointment could not be booked back due to appointment conflict!";
                                                        case ADD_APPOINTMENT_SUCCESS:
                                                            return "Could not book new appointment due to appointment conflict & old appointment booked back successfully!";
                                                        default:
                                                            return "Unexpected return from booking old appointment!";
                                                    }
                                                case ADD_APPOINTMENT_SUCCESS:
                                                    return "Appointments swapped successfully";
                                                default:
                                                    return "Unexpected return from booking new appointment!";
                                            }

                                        } else {
                                            return "Response from canceling old appointment: " + response;
                                        }
                                    default:
                                        return "Unexpected return from addPatient in new appointment!";
                                }
                            } else {
                                return "New appointment not found!";
                            }
                        } else {
                            return "Invalid new appointment type!";
                        }
                    } else {
                        String response1;
                        switch (detectCity(newAppointmentID)) {
                            case MONTREAL:
                                response1 = sendUDPRequest(HospitalServer.MONTREAL_UDP_PORT, "BOOK_APPOINTMENT", newAppointmentType, newAppointmentID, patientID);
                                break;
                            case QUEBEC:
                                response1 = sendUDPRequest(HospitalServer.QUEBEC_UDP_PORT, "BOOK_APPOINTMENT", newAppointmentType, newAppointmentID, patientID);
                                break;
                            case SHERBROOKE:
                                response1 = sendUDPRequest(HospitalServer.SHERBROOKE_UDP_PORT, "BOOK_APPOINTMENT", newAppointmentType, newAppointmentID, patientID);
                                break;
                            default:
                                return "Invalid appointment city";
                        }
                        if (response1.equals("Appointment booked successfully")) {
                            String response2;
                            switch (patient.removeAppointment(oldAppointmentID, oldAppointmentType)){
                                case APPOINTMENT_NOT_FOUND:
                                    return "Old appointment not found in patient";
                                case REMOVE_APPOINTMENT_SUCCESS:
                                    switch (patient.bookAppointment(newAppointmentID, newAppointmentType)){
                                        case OUT_OF_TOWN_LIMIT_REACHED:
                                            switch (detectCity(newAppointmentID)){
                                                case MONTREAL:
                                                    response2 = sendUDPRequest(HospitalServer.MONTREAL_UDP_PORT, "CANCEL_APPOINTMENT", newAppointmentType, newAppointmentID, patientID);
                                                    break;
                                                case QUEBEC:
                                                    response2 = sendUDPRequest(HospitalServer.QUEBEC_UDP_PORT, "CANCEL_APPOINTMENT", newAppointmentType, newAppointmentID, patientID);
                                                    break;
                                                case SHERBROOKE:
                                                    response2 = sendUDPRequest(HospitalServer.SHERBROOKE_UDP_PORT, "CANCEL_APPOINTMENT", newAppointmentType, newAppointmentID, patientID);
                                                    break;
                                                default:
                                                    return "Invalid new appointment city";
                                            }
                                            switch (patient.bookAppointment(oldAppointmentID, oldAppointmentType)){
                                                case OUT_OF_TOWN_LIMIT_REACHED:
                                                    if (response2.equals("Patient removed from appointment")){
                                                        return "Could not swap new appointment due to town limit & could not book back old appointment due to town limit";
                                                    }else {
                                                        return "Could not remove patient from new appointment: " + response2 + " & could not book back old appointment due to town limit";
                                                    }
                                                case APPOINTMENT_HAS_CONFLICT:
                                                    if (response2.equals("Patient removed from appointment")){
                                                        return "Could not swap new appointment due to town limit & could not book back old appointment due to conflict";
                                                    }else {
                                                        return "Could not remove patient from new appointment: " + response2 + " & could not book back old appointment due to conflict";
                                                    }
                                                case ADD_APPOINTMENT_SUCCESS:
                                                    if (response2.equals("Patient removed from appointment")){
                                                        return "Could not swap new appointment due to town limit.";
                                                    }else {
                                                        return "Could not remove patient from new appointment: " + response2 + " & patient has the old appointment";
                                                    }
                                                default:
                                                    return "Unexpected return from booking old appointment";
                                            }
                                        case APPOINTMENT_HAS_CONFLICT:
                                            switch (detectCity(newAppointmentID)){
                                                case MONTREAL:
                                                    response2 = sendUDPRequest(HospitalServer.MONTREAL_UDP_PORT, "CANCEL_APPOINTMENT", newAppointmentType, newAppointmentID, patientID);
                                                    break;
                                                case QUEBEC:
                                                    response2 = sendUDPRequest(HospitalServer.QUEBEC_UDP_PORT, "CANCEL_APPOINTMENT", newAppointmentType, newAppointmentID, patientID);
                                                    break;
                                                case SHERBROOKE:
                                                    response2 = sendUDPRequest(HospitalServer.SHERBROOKE_UDP_PORT, "CANCEL_APPOINTMENT", newAppointmentType, newAppointmentID, patientID);
                                                    break;
                                                default:
                                                    return "Invalid new appointment city";
                                            }
                                            switch (patient.bookAppointment(oldAppointmentID, oldAppointmentType)){
                                                case OUT_OF_TOWN_LIMIT_REACHED:
                                                    if (response2.equals("Patient removed from appointment")){
                                                        return "Could not swap new appointment due to conflict & could not book back old appointment due to town limit";
                                                    }else {
                                                        return "Could not remove patient from new appointment: " + response2 + " & could not book back old appointment due to town limit";
                                                    }
                                                case APPOINTMENT_HAS_CONFLICT:
                                                    if (response2.equals("Patient removed from appointment")){
                                                        return "Could not swap new appointment due to conflict & could not book back old appointment due to conflict";
                                                    }else {
                                                        return "Could not remove patient from new appointment: " + response2 + " & could not book back old appointment due to conflict";
                                                    }
                                                case ADD_APPOINTMENT_SUCCESS:
                                                    if (response2.equals("Patient removed from appointment")){
                                                        return "Could not swap new appointment due to conflict.";
                                                    }else {
                                                        return "Could not remove patient from new appointment: " + response2 + " & patient has the old appointment";
                                                    }
                                                default:
                                                    return "Unexpected return from booking old appointment";
                                            }
                                        case ADD_APPOINTMENT_SUCCESS:
                                            if (detectCity(oldAppointmentID) == detectCity(serverID)){
                                                response2 = cancelAppointmentUDP(patientID, oldAppointmentID, oldAppointmentType);
                                                if (response2.equals("Patient removed from appointment")){
                                                    return "Appointments swapped successfully!";
                                                }else {
                                                    return "Could not remove patient from old appointment: " + response2;
                                                }
                                            }else {
                                                switch (detectCity(oldAppointmentID)){
                                                    case MONTREAL:
                                                        response2 = sendUDPRequest(HospitalServer.MONTREAL_UDP_PORT, "CANCEL_APPOINTMENT", oldAppointmentType, oldAppointmentID, patientID);
                                                        break;
                                                    case QUEBEC:
                                                        response2 = sendUDPRequest(HospitalServer.QUEBEC_UDP_PORT, "CANCEL_APPOINTMENT", oldAppointmentType, oldAppointmentID, patientID);
                                                        break;
                                                    case SHERBROOKE:
                                                        response2 = sendUDPRequest(HospitalServer.SHERBROOKE_UDP_PORT, "CANCEL_APPOINTMENT", oldAppointmentType, oldAppointmentID, patientID);
                                                        break;
                                                    default:
                                                        return "Invalid old appointment city";
                                                }
                                                if (response2.equals("Patient removed from appointment")){
                                                    return "Appointments swapped successfully!";
                                                }else {
                                                    return "Could not remove patient from old appointment: " + response2;
                                                }
                                            }
                                    }
                                default:
                                    return "Unexpected return from removeAppointment in patient";
                            }
                        }else {
                            return "Could not book new appointment: " + response1;
                        }
                    }
                } else {
                    return "Patient doesn't have the first appointment!";
                }
            }else {
                return "Invalid old appointment type!";
            }
        }else {
            return "Patient ID not found";
        }
    }

    public String sendUDPRequest(int port, String command, String appointmentType, String appointmentID, String patientID) {
        DatagramSocket socket = null;
        String responseMessage = "";
        try {
            socket = new DatagramSocket();
            byte[] buffer;
            String requestMessage = command + "," + appointmentType + "," + appointmentID + "," + patientID;
            buffer = requestMessage.getBytes();

            InetAddress host = InetAddress.getByName("localhost");
            DatagramPacket request = new DatagramPacket(buffer, buffer.length, host, port);
            socket.send(request);
            System.out.println(serverID + " UDP request sent to " + port + " : " + requestMessage);

            byte[] responseBuffer = new byte[1000];
            DatagramPacket response = new DatagramPacket(responseBuffer, responseBuffer.length);
            socket.receive(response);
            responseMessage = new String(response.getData(), 0, response.getLength());
            System.out.println(serverID + " UDP response received: " + responseMessage);
        } catch (Exception e) {
            System.out.println("Exception in DHMS.sendUDPRequest: " + e);
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
        return responseMessage;
    }

    public String listAppointmentAvailabilityUDP(String appointmentType) {
        appointmentType = appointmentType.toUpperCase();

        if (database.containsKey(appointmentType)) {
            Map<String, Appointment> appointments = database.get(appointmentType);

            StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append(serverID).append(" ").append(appointmentType).append(":\n");

            if (appointments.isEmpty()) {
                stringBuilder.append("No appointments of type "+ appointmentType +" available\n");
            }else {
                for (Appointment appointment: appointments.values()){
                    stringBuilder.append(appointment.toString()).append("\n");
                }
            }
            return stringBuilder.toString();
        }else {
            return "Invalid appointment type\n";
        }
    }

    public String bookAppointmentUDP(String patientID, String appointmentID, String appointmentType) {
        patientID = patientID.toUpperCase();
        appointmentID = appointmentID.toUpperCase();
        appointmentType = appointmentType.toUpperCase();


        if (database.containsKey(appointmentType)){
            Map<String, Appointment> db = database.get(appointmentType);
            if (db.containsKey(appointmentID)) {
                Appointment appointment = db.get(appointmentID);
                switch (appointment.addPatient(patientID)) {
                    case APPOINTMENT_FULL:
                        return "Appointment is full";
                    case ADD_PATIENT_SUCCESS:
                        return "Appointment booked successfully";
                    default:
                        return "Unexpected return value from addPatient";
                }

            } else {
                return "Appointment does not exist";
            }
        }else {
            return "Invalid appointment type";
        }
    }


    public String cancelAppointmentUDP(String patientID,  String appointmentID, String appointmentType) {
        appointmentID = appointmentID.toUpperCase();
        appointmentType = appointmentType.toUpperCase();

        if (database.containsKey(appointmentType)) {
            Map<String, Appointment> db = database.get(appointmentType);
            if (db.containsKey(appointmentID)) {
                Appointment appointment = (Appointment) db.get(appointmentID);
                switch(appointment.removePatient(patientID)){
                    case REMOVE_PATIENT_SUCCESS:
                        return "Patient removed from appointment";
                    case PATIENT_ID_NOT_FOUND:
                        return "Patient not found in appointment";
                    default:
                        return "Unexpected return value from removePatient in appointment";
                }
            } else {
                return "Appointment does not exist";
            }
        }else {
            return "Invalid appointment type";
        }
    }

    public String removeAppointmentUDP(String appointmentType, String appointmentID, String patientID) {
        appointmentID = appointmentID.toUpperCase();
        appointmentType = appointmentType.toUpperCase();
        patientID = patientID.toUpperCase();

        if (patientsMap.containsKey(patientID)){
            User patient = patientsMap.get(patientID);
            switch (patient.removeAppointment(appointmentID, appointmentType)){
                case REMOVE_APPOINTMENT_SUCCESS:
                    String nextAvailableAppointmentID = getNextAvailableAppointment(appointmentID, appointmentType);
                    if (nextAvailableAppointmentID != null){
                        bookAppointment(patientID, nextAvailableAppointmentID, appointmentType);
                    }
                    return "Appointment removed from patient";
                case APPOINTMENT_NOT_FOUND:
                    return "Appointment not found in patient's schedule";
                default:
                    return "Unexpected return value from removeAppointment in patient";
            }
        }else {
            return "patient ID not found";
        }
    }

    public String getNextAvailableAppointmentUDP(String appointmentID, String appointmentType){
        String nextAvailableAppointmentID = null;
        for (Appointment appointment: database.get(appointmentType).values()){
            if (appointment.hasCapacity() && isFirstAppointmentEarlier(appointmentID, appointment.getAppointmentID())){
                if (nextAvailableAppointmentID == null){
                    nextAvailableAppointmentID = appointment.getAppointmentID();
                }else {
                    if (isFirstAppointmentEarlier(appointment.getAppointmentID(), nextAvailableAppointmentID)){
                        nextAvailableAppointmentID = appointment.getAppointmentID();
                    }
                }
            }
        }
        return nextAvailableAppointmentID;
    }

    private String getNextAvailableAppointment(String appointmentID, String appointmentType){
        String nextAvailableAppointmentID = null;
        if(detectCity(appointmentID) == detectCity(serverID)){
            nextAvailableAppointmentID = getNextAvailableAppointmentUDP(appointmentID, appointmentType);
        }else {
            switch (detectCity(appointmentID)){
                case MONTREAL:
                    nextAvailableAppointmentID = sendUDPRequest(HospitalServer.MONTREAL_UDP_PORT, "GET_NEXT_AVAILABLE_APPOINTMENT", appointmentType, appointmentID, null);
                    break;
                case QUEBEC:
                    nextAvailableAppointmentID = sendUDPRequest(HospitalServer.QUEBEC_UDP_PORT, "GET_NEXT_AVAILABLE_APPOINTMENT", appointmentType, appointmentID, null);
                    break;
                case SHERBROOKE:
                    nextAvailableAppointmentID = sendUDPRequest(HospitalServer.SHERBROOKE_UDP_PORT, "GET_NEXT_AVAILABLE_APPOINTMENT", appointmentType, appointmentID, null);
                    break;
            }
        }
        return nextAvailableAppointmentID;
    }

    private boolean isFirstAppointmentEarlier(String appointmentID1, String appointmentID2){
        int year1 = Integer.parseInt(appointmentID1.substring(8,10));
        int year2 = Integer.parseInt(appointmentID2.substring(8,10));
        int month1 = Integer.parseInt(appointmentID1.substring(6,8));
        int month2 = Integer.parseInt(appointmentID2.substring(6,8));
        int day1 = Integer.parseInt(appointmentID1.substring(4,6));
        int day2 = Integer.parseInt(appointmentID1.substring(4,6));

        if(year1 < year2){
            return true;
        }else if (year1 > year2){
            return false;
        }else {
            if (month1 < month2){
                return true;
            }else if (month1 > month2){
                return false;
            }else {
                if (day1 < day2){
                    return true;
                }else if (day1 > day2) {
                    return false;
                }else {
                    switch (appointmentID1.charAt(3)){
                        case 'M':
                            return appointmentID2.charAt(3) != 'M';
                        case 'A':
                            return appointmentID2.charAt(3) == 'E';
                        case 'E':
                            return false;
                    }
                }
            }
        }
        return true;
    }
}
