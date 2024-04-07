package Server;

import DHMS.DHMS;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import javax.xml.ws.Endpoint;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class HospitalServer {
    public static final int MONTREAL_UDP_PORT = 1111;
    public static final int SHERBROOKE_UDP_PORT = 1112;
    public static final int QUEBEC_UDP_PORT = 1113;

    private int udpPort;
    private String id;
    private String serverName;

    public HospitalServer(String id, String[] args) {
        this.id = id.toUpperCase();

        switch (id) {
            case "MTL":
                udpPort = MONTREAL_UDP_PORT;
                serverName = "MONTREAL";
                break;
            case "QUE":
                udpPort = QUEBEC_UDP_PORT;
                serverName = "QUEBEC";
                break;
            case "SHE":
                udpPort = SHERBROOKE_UDP_PORT;
                serverName = "SHERBROOKE";
                break;
        }


        DHMS dhms = new DHMS(id);
        Endpoint endpoint = Endpoint.publish("http://localhost:8080/" + serverName.toLowerCase(), dhms);
        System.out.println("Service is published: " + endpoint.isPublished());


        Runnable task = () -> {
            listenForRequest(dhms, udpPort, serverName);
        };
        Thread thread = new Thread(task);
        thread.start();
    }

    private static void listenForRequest(DHMS dhms, int udpPort, String serverName) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(udpPort);
            byte[] buffer = new byte[1000];
            System.out.println(serverName + " UDP Server is Up & Running");
            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);
                String requestMessage = new String(request.getData(), 0, request.getLength());
                System.out.println(serverName + " Server received request: " + requestMessage);

                String[] arguments = requestMessage.split(",");

                String command = arguments[0];
                String appointmentType = arguments[1];
                String appointmentID = arguments[2];
                String patientID = arguments[3];

                String response = null;

                switch (command){
                    case "LIST_APPOINTMENT_AVAILABILITY":
                        response = dhms.listAppointmentAvailabilityUDP(appointmentType);
                        break;
                    case "BOOK_APPOINTMENT":
                        response = dhms.bookAppointmentUDP(patientID, appointmentID, appointmentType);
                        break;
                    case "CANCEL_APPOINTMENT":
                        response = dhms.cancelAppointmentUDP(patientID, appointmentID, appointmentType);
                        break;
                    case "REMOVE_APPOINTMENT":
                        response = dhms.removeAppointmentUDP(appointmentType, appointmentID, patientID);
                        break;
                    case "GET_NEXT_AVAILABLE_APPOINTMENT":
                        response = dhms.getNextAvailableAppointmentUDP(appointmentID, appointmentType);
                        break;
                    default:
                        response  = "Invalid Command\n";

                }
                DatagramPacket reply = new DatagramPacket(response.getBytes(), response.length(), request.getAddress(), request.getPort());
                socket.send(reply);

            }
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }


}
