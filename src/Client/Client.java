package Client;

import DHMS.DHMSInterface;
import User.*;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.URL;
import java.util.Scanner;

public class Client {
    private static final int ACTION_BOOK_APPOINTMENT = 1;
    private static final int ACTION_GET_APPOINTMENT_SCHEDULE = 2;
    private static final int ACTION_CANCEL_APPOINTMENT = 3;
    private static final int SWAP_APPOINTMENT = 4;
    private static final int PATIENT_LOGOUT = 5;

    private static final int ACTION_ADD_APPOINTMENT = 5;
    private static final int ACTION_REMOVE_APPOINTMENT = 6;
    private static final int ACTION_LIST_APPOINTMENT_AVAILABILITY = 7;
    private static final int ADMIN_LOGOUT = 8;


    public static void main(String[] args) {
        start();
    }

    private static void start() {
        try {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Enter your ID: ");
            String userID = scanner.nextLine();

            User user = new User(userID);

            displayMenu(user);
        } catch (Exception e) {
            System.out.println("Exception in Client: " + e);
            start();
        }
    }

    private static void displayMenu(User user) throws Exception {

        Scanner scanner = new Scanner(System.in);
        int response;

        URL url = new URL("http://localhost:8080/"+ user.getCity().toString().toLowerCase()+"?wsdl");
        QName qName = new QName("http://DHMS/", "DHMSService");
        Service service = Service.create(url, qName);
        DHMSInterface dhms = service.getPort(DHMSInterface.class);


        System.out.println(dhms.login(user.getUserID()));
        try {
            if (user.getUserType() == UserType.PATIENT) {
                System.out.println("====================================================================");
                System.out.println("Please choose from the following:");
                System.out.println("1. Book an appointment");
                System.out.println("2. Get appointment schedule");
                System.out.println("3. Cancel an appointment");
                System.out.println("4. Swap an appointment");
                System.out.println("5. Log out");

                response = Integer.parseInt(scanner.next());

                String appointmentID;
                String appointmentType;

                switch (response) {
                    case ACTION_BOOK_APPOINTMENT:
                        System.out.println("please enter the appointment id:");
                        appointmentID = scanner.next().toUpperCase();
                        System.out.println("please enter the appointment type:");
                        appointmentType = scanner.next().toUpperCase();
                        System.out.println(dhms.bookAppointment(user.getUserID(), appointmentID, appointmentType));
                        break;
                    case ACTION_GET_APPOINTMENT_SCHEDULE:
                        System.out.println(dhms.getAppointmentSchedule(user.getUserID()));
                        break;
                    case ACTION_CANCEL_APPOINTMENT:
                        System.out.println("please enter the appointment id:");
                        appointmentID = scanner.next().toUpperCase();
                        System.out.println("please enter the appointment type:");
                        appointmentType = scanner.next().toUpperCase();
                        System.out.println(dhms.cancelAppointment(user.getUserID(), appointmentID, appointmentType));
                        break;
                    case SWAP_APPOINTMENT:
                        System.out.println("please enter the appointment id for the appointment you want to replace:");
                        String oldAppointmentID = scanner.next().toUpperCase();
                        System.out.println("please enter the appointment type for the appointment you want to replace:");
                        String oldAppointmentType = scanner.next().toUpperCase();
                        System.out.println("please enter the appointment id for the new appointment:");
                        String newAppointmentID = scanner.next().toUpperCase();
                        System.out.println("please enter the appointment type for the new appointment:");
                        String newAppointmentType = scanner.next().toUpperCase();
                        System.out.println(dhms.swapAppointment(user.getUserID(), oldAppointmentID, oldAppointmentType, newAppointmentID, newAppointmentType));
                        break;
                    case PATIENT_LOGOUT:
                        start();
                    default:
                        System.out.println("Invalid input");
                        displayMenu(user);
                }

            } else if (user.getUserType() == UserType.ADMIN) {
                System.out.println("====================================================================");
                System.out.println("Please choose from the following:");
                System.out.println("1. Book an appointment for a patient");
                System.out.println("2. Get appointment schedule for a patient");
                System.out.println("3. Cancel an appointment for a patient");
                System.out.println("4. Swap an appointment for a patient");
                System.out.println("5. Add a new appointment");
                System.out.println("6. Remove an appointment");
                System.out.println("7. List appointment availability");
                System.out.println("8. Log out");

                response = Integer.parseInt(scanner.next());

                String appointmentID;
                String appointmentType;
                String patientID;
                int capacity;

                switch (response) {
                    case ACTION_BOOK_APPOINTMENT:
                        System.out.println("Please enter the patient ID:");
                        patientID = scanner.next();
                        System.out.println("please enter the appointment id:");
                        appointmentID = scanner.next();
                        System.out.println("please enter the appointment type:");
                        appointmentType = scanner.next();
                        System.out.println(dhms.bookAppointment(patientID, appointmentID, appointmentType));
                        break;
                    case ACTION_GET_APPOINTMENT_SCHEDULE:
                        System.out.println("Please enter the patient ID:");
                        patientID = scanner.next();
                        System.out.println(dhms.getAppointmentSchedule(patientID));
                        break;
                    case ACTION_CANCEL_APPOINTMENT:
                        System.out.println("Please enter the patient ID:");
                        patientID = scanner.next();
                        System.out.println("please enter the appointment id:");
                        appointmentID = scanner.next().toUpperCase();
                        System.out.println("please enter the appointment type:");
                        appointmentType = scanner.next().toUpperCase();
                        System.out.println(dhms.cancelAppointment(patientID, appointmentID, appointmentType));
                        break;
                    case SWAP_APPOINTMENT:
                        System.out.println("Please enter the patient ID:");
                        patientID = scanner.next();
                        System.out.println("please enter the appointment id for the appointment you want to replace:");
                        String oldAppointmentID = scanner.next().toUpperCase();
                        System.out.println("please enter the appointment type for the appointment you want to replace:");
                        String oldAppointmentType = scanner.next().toUpperCase();
                        System.out.println("please enter the appointment id for the new appointment:");
                        String newAppointmentID = scanner.next().toUpperCase();
                        System.out.println("please enter the appointment type for the new appointment:");
                        String newAppointmentType = scanner.next().toUpperCase();
                        System.out.println(dhms.swapAppointment(patientID, oldAppointmentID, oldAppointmentType, newAppointmentID, newAppointmentType));
                        break;
                    case ACTION_ADD_APPOINTMENT:
                        System.out.println("please enter the appointment id:");
                        appointmentID = scanner.next();
                        System.out.println("please enter the appointment type:");
                        appointmentType = scanner.next();
                        System.out.println("please enter the appointment capacity:");
                        capacity = Integer.parseInt(scanner.next());
                        System.out.println(dhms.addAppointment(appointmentID, appointmentType, capacity));
                        break;
                    case ACTION_REMOVE_APPOINTMENT:
                        System.out.println("please enter the appointment id:");
                        appointmentID = scanner.next();
                        System.out.println("please enter the appointment type:");
                        appointmentType = scanner.next();
                        System.out.println(dhms.removeAppointment(appointmentID, appointmentType));
                        break;
                    case ACTION_LIST_APPOINTMENT_AVAILABILITY:
                        System.out.println("please enter the appointment type:");
                        appointmentType = scanner.next();
                        System.out.println(dhms.listAppointmentAvailability(appointmentType));
                        break;
                    case ADMIN_LOGOUT:
                        start();
                        break;
                    default:
                        System.out.println("Invalid input");
                        displayMenu(user);
                }
            }
            displayMenu(user);
        }catch (Exception e){
            System.out.println("Exception in Client.displayMenu: " + e);
            displayMenu(user);
        }
    }


}
