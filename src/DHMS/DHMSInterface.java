package DHMS;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;
import java.rmi.RemoteException;

@WebService
@SOAPBinding(style= Style.RPC)
public interface DHMSInterface {
    @WebMethod
    public String addAppointment(String appointmentID, String appointmentType, int capacity);
    @WebMethod
    public String removeAppointment(String appointmentID, String appointmentType);
    @WebMethod
    public String listAppointmentAvailability(String appoitmentType);

    // Patient functions
    @WebMethod
    public String bookAppointment(String patientID, String appointmentID, String appointmentType);
    @WebMethod
    public String getAppointmentSchedule(String patientID);
    @WebMethod
    public String cancelAppointment(String patientID, String appointmentID, String appointmentType);

    @WebMethod
    public String login(String userID);

    @WebMethod
    public String swapAppointment(String patientID, String oldAppointmentID, String oldAppointmentType, String newAppointmentID, String newAppointmentType);

    @WebMethod
    public String helloWorld(String name);
}
