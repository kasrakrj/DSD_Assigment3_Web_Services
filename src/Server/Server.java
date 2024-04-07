package Server;

public class Server {
    public static void main(String[] args) {
        Runnable task1 = () -> {
            HospitalServer MontrealServer = new HospitalServer("MTL", args);
        };

        Runnable task2 = () -> {
          HospitalServer QuebecServer = new HospitalServer("QUE", args);
        };

        Runnable task3 = () -> {
          HospitalServer SherbrookeServer = new HospitalServer("SHE", args);
        };

        Thread thread1 = new Thread(task1);
        thread1.start();
        Thread thread2 = new Thread(task2);
        thread2.start();
        Thread thread3 = new Thread(task3);
        thread3.start();
    }
}
