import com.tomcat.core.HttpServer;

/**
 * @author: myr
 * @date: 2019-09-16
 */
public class Main {

    public static void main(String[] args) {
        HttpServer server = new HttpServer();
        server.acceptWait();
    }
}
