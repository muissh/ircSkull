package server;

import java.io.BufferedReader;
import java.io.FileInputStream;
import message.Message;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.StringTokenizer;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * SERVER CLASS
 * 1) création
 * 2) lancement boucle écoute sur socket socketPort(55495)
 * 3) pour chaque connexion:
 *  a) création d'un thread pour gérer le client 
 *  b) envois des contacts
 * @author raphaël
 */
public final class Server extends Thread {

    private ArrayList<ThreadClient> usersList;
    private final ServerSocket serverSocket;
    private final int serverSocketPort = 55495;
    private static int clientPort = 55495;
    
    private static PrivateKey privateKey;
    private static PublicKey publicKey;
 
    /**
     * 
     * @param args 
     */
    public static void main(String[] args) {
        try {
            Server chatServer = new Server();
            chatServer.start();
            AssyKeyGen();
        } catch (Exception ioe) {
            System.err.println("Error while launching server "+ ioe);
        }
    }

    /**
     * 
     * @throws IOException 
     */
    Server() throws IOException { 
        serverSocket = new ServerSocket(serverSocketPort);
        usersList = new ArrayList<>(); // la liste de mes clients
    }

    /**
     * 
     */
    @Override
    public void run() {
        System.out.println("Server launch on port : " + serverSocket.getLocalPort());
        System.out.println("---------------------------------\n");
        
        Socket client = null;
        try {
            while (true) {
                client = serverSocket.accept(); //adress 55495
                ThreadClient tc = new ThreadClient(client);
                tc.start();
            }
        } catch (IOException ioe) {
            System.err.println(ioe);
        } finally { 
            try {
                client.close();
                serverSocket.close();
            } catch (IOException ioee) {
                System.err.println(ioee);
            }
        }
    } 
  
    /**
     * RSA creation keys
     * @throws NoSuchAlgorithmException 
     */
    private static void AssyKeyGen() throws NoSuchAlgorithmException{
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        KeyPair keypair = keyGen.genKeyPair();
           
        privateKey = keypair.getPrivate();       
        publicKey = keypair.getPublic();
    }
    
    /**
     * 
     */
    private void unWrapping( PrivateKey pk, ) {
            try {
                KeyGenerator keygen = KeyGenerator.getInstance("AES");
                Cipher cipherRsa = Cipher.getInstance("RSA");
                Cipher cipherAes = Cipher.getInstance("AES");

                SecureRandom random = new SecureRandom();
                keygen.init(random);
                sessionK = keygen.generateKey();           
                System.out.println("secretKey: "+ sessionK);

                cipherRsa.init(Cipher.UNWRAP_MODE, pk);
                wrappedKey = cipherRsa.unwrap(sessionK);
    //            cipherAes.init(Cipher.ENCRYPT_MODE, sessionK);

            } catch (Exception  ex) {}

        }
    

    //========================================================================//
    //Thread Client
    //========================================================================//
    /**
     * 
     */
    public class ThreadClient extends Thread {

        private final Socket socketClient;
        private final ObjectOutputStream oos;
        private final ObjectInputStream ois;
        private final String iaClient;
        private String nickname;
        
        private SecretKey sessionK;
        private PublicKey clientPK;    

        /**
         * 
         * @param s
         * @throws IOException 
         */
        public ThreadClient(Socket s) throws IOException {
            socketClient = s;
            iaClient = s.getInetAddress().getHostAddress();
            this.oos = new ObjectOutputStream(this.socketClient.getOutputStream());
            this.ois = new ObjectInputStream(this.socketClient.getInputStream());         
        }

        /**
         * 
         */
        @Override
        public void run() {
            Message message;

            try {
                while ((message = ((Message) ois.readObject())) != null) {
                    switch (message.getType()) {
                        
                        /**
                         * demande d'enregistrement
                         */
                        case 0:
//                            if(registration(message.getPseudo(),message.getMessage())){
//                                System.out.println("inscription: "+message.getPseudo()+ " " +message.getPseudo()+"\n");
//                                broadcast(new Message("SEVER","ACK","******",4));
//                            }else{
//                                System.out.println("inscription: "+message.getPseudo()+ " " +message.getPseudo()+" failed\n");
//                                broadcast(new Message("SEVER","KCA","******",4));
//                            }
                            
                            closeClConnection();
                            break;
                            
                            /**
                             * demande de login d'un client
                             */
                        case 1:
                            System.out.println("je recois une demande de login de \""+message.getPseudo()+"\"");
                            if(IDVerif(message.getPseudo(),message.getMessage())){
                                System.out.println("demande acceptée");
                                this.nickname = message.getPseudo();
                                synchronized (usersList) {
                                    usersList.add(this);
                                    broadcast(new Message(message.getPseudo()," connecting.","******",3));  
                                }
                                sendToCl(new Message(message.getPseudo()," you're now online.","******",3));
                                contactSending();
                                
                            }else{
                                System.out.println("demande refusée");
                                sendToCl(new Message(message.getPseudo()," connexion refused.","******",3));
                                closeClConnection();
                            }
                            
                            break;
                           
                            /**
                             * un client se deco
                             */
                        case 2: 
                            broadcast(new Message(nickname, " leaving.","******",message.getType()));
                            return;                  

                            /**
                             * un message a broadcast
                             */
                        case 3:
                            broadcast(new Message(nickname,message.getMessage(),"******", 3));
                            break;
                            
                            /**
                             * réception d'une cle d'un client
                             */
                        case 7:
                            System.out.println("je recois une cle!");
                            if(message.getMessage()=="public") clientPK = message.getPk();// TODO
                            else if(message.getMessage()=="session") sessionK = (PrivateKey)message.getK();//todo
                            break;
                            
                            /**
                             * 
                             */
                        case 8:
                            this.clientPK = message.getK();
                            break;
                                                  
                        default:
                            System.err.println("Unknow command from: "+ socketClient.getInetAddress().getCanonicalHostName());
                            break;
                            
                    } // fin switch
                } // fin while
            } catch (IOException ioe) {
                System.err.println("Problem with " + nickname + " : " + ioe );
            } catch (Exception e){    }
            
        } // fin run
        
        /**
         * 
         * @param message 
         */
        private synchronized void broadcast(Message message) {
            System.out.println(message.getPseudo() + " : " + message.getMessage() + "\n");
            synchronized(usersList){      
                for(ThreadClient tcl : usersList){
                    tcl.sendToCl(message);        
                }
            }
        }
        
        /**
         * pour chaque client, envoie des infos de celui ci au client
         */
        private void contactSending(){
            synchronized(usersList){      
                for (int i = 0; i < usersList.size(); i++) {
                    if(usersList.get(i) != this){
                        sendToCl(new Message(usersList.get(i).nickname,""+(clientPort),usersList.get(i).iaClient,5));
                    }
                }
            }
        }
        
        /**
         * 
         * @param name
         * @param pwd
         * @return 
         */
        private boolean IDVerif(String name,String pwd){
            
            StringTokenizer chaine = null;
            String fichier ="fichiertexte.txt";
            
            //LECTURE DU FICHIER POUR ECRIRE APRES
            try{
                InputStream ips=new FileInputStream(fichier); 
                InputStreamReader ipsr=new InputStreamReader(ips);
                BufferedReader br=new BufferedReader(ipsr);
                String ligne;
                while ((ligne=br.readLine())!=null){
                        System.out.println(ligne);
                        if(ligne.contains(name))
                            chaine = new StringTokenizer(ligne);
                            int i=1;
                            while(chaine.hasMoreTokens()){
                                i++;
                                chaine.nextToken(" ");
                                    if(i==2){
                                        if(chaine.nextToken(" ").equals(pwd)) {
                                            return true;
                                        }else{
                                            return false;
                                        }
                                    }else{
                                        //System.out.println(user.nextToken(" "));
                                    }
                            }
                        //chaine+=ligne+"\n";
                }
                br.close();
            }catch (Exception e){
                System.out.println(e.toString());
            }
            return false;
        }
        
        /**
         * 
         * @param message 
         */
        private synchronized void sendToCl(Message message){
            //TODO crypter avec cle de session
            try{
                oos.writeObject(message);
                oos.flush();
            }catch(IOException ioe){
                System.err.println(ioe);
            }
        }

        /**
         * 
         */
        private void closeClConnection() {
            try {
                this.ois.close();
                this.oos.close();
                this.socketClient.close();
            } catch (IOException ex) {
                System.err.println("probleme à la fermetre du client");
            }                    
        }
        
    }
} 