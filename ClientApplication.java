import ch.qos.logback.classic.Level;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.QuoteRequest;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.StringTokenizer;

public class ClientApplication implements Application {


   private String username;
   private String password;
   private String symbols;

   private static volatile SessionID sessionID;


   public void onCreate(SessionID sessionID) {
       System.out.println("OnCreate");
   }


   public void onLogon(SessionID sessionID) {
       System.out.println("OnLogon");
       ClientApplication.sessionID = sessionID;
       Session s = Session.lookupSession(sessionID);
       StringTokenizer symbolsTokens = new StringTokenizer(symbols, " ");
       while(symbolsTokens.hasMoreTokens()){
           QuoteRequest qr = new QuoteRequest();
           qr.setString(Symbol.FIELD, symbolsTokens.nextToken());
           s.send(qr);
       }
   }


   public void onLogout(SessionID sessionID) {
       System.out.println("OnLogout");
   }

   public void toAdmin(Message message, SessionID sessionID) {
       System.out.println("ToAdmin");
       if (message instanceof quickfix.fix44.Logon) {
           try {
               System.out.println(" Login " + username + " " + password);
               message.setString(quickfix.field.Username.FIELD, username);
               message.setString(quickfix.field.Password.FIELD, password);
               System.out.println(" Logon " + message.toString());
           }
           catch (Exception ex) {
               throw new RuntimeException();
           }
       }else if (message instanceof quickfix.fix44.QuoteRequest) {
           System.out.println(" Sent Quote Request ");
       }
   }

   public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
       System.out.println("FromAdmin");
   }

   public void toApp(Message message, SessionID sessionID) throws DoNotSend {
       System.out.println("ToApp: " + message);
   }

   public void fromApp(Message message, SessionID sessionID) {
       try {
           String symbol = message.getString(Symbol.FIELD);
               System.out.println(" FromApp " + message);
               message.getString(TransactTime.FIELD);
               double bid = message.getDouble(BidPx.FIELD);
               double ask = message.getDouble(OfferPx.FIELD);
       } catch (FieldNotFound fieldNotFound) {
           fieldNotFound.printStackTrace();
       }
   }


   public ClientApplication(String configFile) {
       try {
           System.out.println(" Config File " + configFile);
           Properties props = new Properties();
           try {
               props.load(new FileInputStream(configFile));
           } catch (IOException e) {
               e.printStackTrace();
           }
           username = props.getProperty("Username");
           password = props.getProperty("Password");
           symbols = props.getProperty("Symbols");
           System.out.println(" FIX Port " + props.getProperty("SocketConnectPort"));
           System.out.println(" FIX IP " + props.getProperty("SocketConnectHost"));
           System.out.println(" Username " + username + " Password " + password);
           SessionSettings settings = new SessionSettings(configFile);
           MessageStoreFactory messageStoreFactory = new FileStoreFactory(settings);
           LogFactory logFactory = new FileLogFactory(settings);
           MessageFactory messageFactory = new DefaultMessageFactory();
           Initiator initiator = new SocketInitiator(this, messageStoreFactory, settings, logFactory, messageFactory);
           initiator.start();

           String serverIp = props.getProperty("SocketConnectHost");
           String serverPort = props.getProperty("SocketConnectPort");
           System.out.println(" Data Server IP " + serverIp);
           System.out.println(" Data Server Port " + serverPort);
           ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)
                        org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
           root.setLevel(Level.OFF);
//            root.setLevel(Level.DEBUG);

           while (sessionID == null) {
               Thread.sleep(1000);
           }

           Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
               public void run() {
                   System.out.print("Logout");
                   Session.lookupSession(sessionID).logout();
               }
           }));
       }catch (Exception e){
           e.printStackTrace();
       }

   }

   public static void main(String[] args){
       String configFile = args[0];
       new ClientApplication(configFile);
   }
}

