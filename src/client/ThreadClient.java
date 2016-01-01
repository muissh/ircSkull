package client;

import message.Message;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import server.Server;


/**
 *
 * @author Musish
 */
public class ThreadClient extends Thread{
    

    private String usrIP;
    private static ArrayList<CommunicationThread> contactsList;
    private static ServerSocket serverSocket;

    private boolean connected = false;
    private boolean servSocketUp = false;
    
    //========================================================================//
    //configuration variables
    //========================================================================//
    private final String serverAddress = "127.0.0.1";
    private final int serverPort = 55495;
    private static int threadPort = 55496;
    
    
    ThreadClient() throws IOException{

        try {
            this.usrIP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            System.err.println("Impossible de récupérer l'inetAdresse");
        }
        contactsList = new ArrayList<>();

        /**
         * lancer au départ sur le port 9, doit être 
         */
        //serverSocket = new ServerSocket(9);
        //System.out.println("Server launch on port : " + serverSocket.getLocalPort());
        System.out.println("---------------------------------");
        System.out.println();
    }
    
    @Override
    public void run() {
        Socket s = null;
        while(!servSocketUp)
            try {
                sleep(0);
            } catch (InterruptedException ex) {
                System.err.println("le thread d'écoute a été reveille plus tot que prevu");
            }
        try {
            while (true) {
                s = serverSocket.accept(); //adress 55495
                CommunicationThread tc = new CommunicationThread(s);
                synchronized (contactsList) {
                    contactsList.add(tc);
                    tc.start();
                }
            }
        } catch (IOException ioe) {
            System.err.println(ioe);
        } finally { 
            try {
                s.close();
                serverSocket.close();
            } catch (IOException ioee) {
                System.err.println(ioee);
            }
        }
    }

    protected void login(String name, String pwd){
        try{         
            connected = true;
            System.out.println("\n\nJE SUIS "+name);
            CommunicationThread threadServer = new CommunicationThread(new Socket(serverAddress, serverPort));
            threadServer.start();
            threadServer.setName("server");
            threadServer.identification(name, pwd);
            contactsList.add(threadServer);       
        }catch(Exception e){
            System.err.println(e.getMessage());
        }
    }
    
    protected void send(String s, int i) {
        try {
            contactsList.get(i).output.writeObject(new Message(usrIP,s,usrIP,3));
            contactsList.get(i).output.flush();
        } catch (SocketException  ioe) {
            System.err.println("Connection lost. "+ ioe.getMessage() +"\n");
        }catch(IOException ioe){
            System.err.println("A wild problem appears! "+ ioe.getMessage() +"\n");
        }
    }

    protected class CommunicationThread extends Thread {
        
       public String name;
       protected Socket socket;
       protected ObjectOutputStream output;
       protected ObjectInputStream input;

       CommunicationThread(Socket s){
            socket = s;
            try {              
                output = new ObjectOutputStream(socket.getOutputStream());
                input = new ObjectInputStream(socket.getInputStream());
            } catch (UnknownHostException e) {
                System.err.println(e.getMessage() + "\n");
            } catch (IOException e) {
                System.err.println(e.getMessage() + "\n");
            }
            System.out.println("Je viens de créer un nouveau thread: "+socket.getInetAddress()+"\t port:"+socket.getPort()+"\n");       
       }
           
       
       /**
     * @return the type
     * 0=inscription (demande de contact)
     * 1=login (alex s'est co et initialise la com avec moi)
     * 2=leave (alex dit au revoir)
     * 3=msg (...)
     * 4=ACK from server 
     * 5=contact (le serveur me dit que alexandre se co. je dois me loger pres de lui
     *              port: msg -     - ip : adress -     - nom : pseudo
     * 6=serverSocket
     */
        @Override
        public void run() {
            Message message;
            try {
                System.out.println("Avant la boucle d'écoute");
            loop: while(connected && ((message = (Message) input.readObject()) != null)) {
                    System.out.println("Dans la boucle d'écoute");
                    
                    switch(message.getType()){
                        
                            /**
                             * inscription (demande de contact)
                             */
                        case 0:
                            break loop;
                            
                            /**
                             * login (alex s'est co et initialise la com avec moi)
                             */
                        case 1: 
                            System.out.println("je recois une demande de login de \""+message.getPseudo()+"\"");
                            break;
                            
                            /**
                             * leave (alex dit au revoir)
                             */
                        case 2: 
                            break loop;
                            
                            /**
                             * msg (...)
                             */
                        case 3: 
                            System.out.println("j'ai recu un message de "+message.getPseudo()+" : ");
                            System.out.print(message.getMessage()+'\n');
                            break;
                            
                            /**
                             * ACK from server 
                             */
                        case 4:
                            if(message.getMessage().equalsIgnoreCase("ACK")){
                                System.out.println("Request succesfull.\n");
                            }else{
                                System.err.println("Request failed.\n");
                                
                            }
                            break;
                            
                            /**
                             * contact (le serveur me dit que alexandre se co. je dois me loger pres de lui)
                             */
                        case 5:
                            System.out.println
                                ("contact: "+message.getPseudo()+ " on "+message.getAdress()
                                        +" - "+message.getMessage()+"\n");
                            CommunicationThread ct =
                                    new CommunicationThread(new Socket(message.getAdress(),Integer.parseInt(message.getMessage())));
                            ct.setName(message.getPseudo());
                            contactsList.add(ct);
                            ct.start();
                            ct.identification(name, "pasBesoinDePswdEntreNous");
                            break;
                            
                            /**
                             * remplacement de l'écoute sur le port 9 au run par une écoute sur le port fourni par le serveur
                             */
                        case 6:
                            System.out.println("je viens de recevoir mon port d'écoute: "+message.getOthers());
                            serverSocket = new ServerSocket(message.getOthers());
                            servSocketUp = true;
                            
                            /**
                             * ...
                             */
                        default:
                            System.out.println("message de type default?! "+message.getPseudo() + " : "+message.getMessage() + '\n');
                    }    
                }
            } catch (IOException ioe) {
                System.err.println("Connexion lost with servor EUW4: " + ioe.getMessage() + '\n');      
            } catch (ClassNotFoundException ex) {} 
            finally {
                System.out.println("\nSee you soon!\n");
                try {
                    closeConnexion();
                } catch (IOException ex) {
                    System.err.println("Probleme a la fermeture de la connection");
                }
            }  
        }//fin run
        
        protected void closeConnexion() throws IOException{
            output.close();
            input.close();
            socket.close();
        }
        
        protected void identification(String name, String pwd) throws IOException{
            output.writeObject(new Message(name,pwd,usrIP,1));
            output.flush();
        }
        
        protected void send(String s, int i) {
            try {
                contactsList.get(i).output.writeObject(new Message(usrIP,s,usrIP,3));
                contactsList.get(i).output.flush();
            } catch (SocketException  ioe) {
                System.err.println("Connection lost. "+ ioe.getMessage() +"\n");
            }catch(IOException ioe){
                System.err.println("A wild problem appears! "+ ioe.getMessage() +"\n");
            }
        }
    
    }
    
    
}
