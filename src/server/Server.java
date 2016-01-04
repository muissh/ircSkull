package server;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import message.Message;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import message.ArrayByteMessage;

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
    private static int clientPort = 55496;
    
    private String name = "server";
    
    private static PrivateKey privateKey;
    private static PublicKey publicKey;
    
     /**
     * 
     * @throws IOException 
     */
    Server() throws IOException { 
        serverSocket = new ServerSocket(serverSocketPort);
        usersList = new ArrayList<>(); // la liste de mes clients
        assymKeysGen();
        exportKey(publicKey, "serverPublicKey.key");
    }
    
    /**
     * 
     */
    @Override
    public void run() {
        System.out.println("Server launch on port : " + serverSocket.getLocalPort());
        System.out.println("---------------------------------\n");
        
        Socket soClient = null;
        try {
            while (true) {
                soClient = serverSocket.accept(); //adress 55495
                ThreadClient tc = new ThreadClient(soClient);
                tc.start();
            }
        } catch (IOException ioe) {
            System.err.println(ioe);
        } finally { 
            try {
                soClient.close();
                serverSocket.close();
            } catch (IOException ioee) {
                System.err.println(ioee);
            }
        }
    } 
    {
        try /**
         * RSA creation keys
         * @throws NoSuchAlgorithmException
         */ {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            KeyPair keypair = keyGen.genKeyPair();
            
            privateKey = keypair.getPrivate();
            publicKey = keypair.getPublic();
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * 
     * @param k
     * @param filename 
     */
    public static void exportKey(PublicKey k , String filename){
        try (ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream (filename))){     
            o.writeObject(k);
            System.out.println("publicKey saved in " + filename);
        } catch (Exception e) {
            System.out.println ("Erreur : "+e.getMessage());
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
            InputStream ips = new FileInputStream(fichier); 
            InputStreamReader ipsr = new InputStreamReader(ips);
            BufferedReader br = new BufferedReader(ipsr);
            String ligne;
            while ((ligne = br.readLine())!=null){
                    System.out.println(ligne);
                    if(ligne.contains(name))
                        chaine = new StringTokenizer(ligne);
                        int i = 1;
                        while(chaine.hasMoreTokens()){
                            i++;
                            chaine.nextToken(" ");
                                if(i == 2){
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
     * @param key
     * @return 
     */
    public String pwdSalting(String name , String pwd){
            String saledPwd = name + pwd;
            byte[] saledPwdBytes = saledPwd.getBytes(), hash = null; 
            
            try{ 
              hash = MessageDigest.getInstance("MD5").digest(saledPwdBytes); //MD2, MD5, SHA-1, SHA-256, SHA-384, SHA-512
            }catch (Exception e) {
               e.printStackTrace();
            }

            StringBuffer hashString = new StringBuffer(); 
            for ( int i = 0; i < hash.length; ++i ) { 
               String hex = Integer.toHexString(hash[i]); 
               if ( hex.length() == 1 ) { 
                    hashString.append('0'); 
                    hashString.append(hex.charAt(hex.length()-1)); 
               } else { 
                    hashString.append(hex.substring(hex.length()-2)); 
               } 
            } 
            return hashString.toString(); 
        }
    
    /**
     * 
     * @throws NoSuchAlgorithmException 
     */
    private void assymKeysGen(){
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);//1024
            KeyPair keypair = keyGen.genKeyPair();
            privateKey = keypair.getPrivate();
            publicKey = keypair.getPublic();
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * 
     * @param abm
     * @param array
     * @return 
     */
    public SecretKey sessKeyUnwrapping(byte [] abm){
        try {
            Cipher cipherRsa = Cipher.getInstance("RSA");
            cipherRsa.init(Cipher.UNWRAP_MODE, privateKey);
            return (SecretKey) cipherRsa.unwrap(abm, "AES", Cipher.SECRET_KEY);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException ex) {}
        return null;
    }
    
    /**
     * 
     * @param abm
     * @param array
     * @return 
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException 
     */
    public PublicKey pubKeyUnwrapping(byte [] abm){
        try {
            Cipher cipherRsa = Cipher.getInstance("RSA");
            cipherRsa.init(Cipher.UNWRAP_MODE, privateKey);
            return (PublicKey) cipherRsa.unwrap(abm, "RSA", Cipher.PUBLIC_KEY);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException ex) {}
        return null;
    }
    
    /**
     * RAS pour com assym
     * AES pour cle de session
     * @param m 
     */
    private byte[] pubKeyWrapping(PublicKey pk,PublicKey wpk) {
        try { 
            Cipher cipherRsa = Cipher.getInstance("RSA");
            cipherRsa.init(Cipher.WRAP_MODE, pk);
            return cipherRsa.wrap(wpk);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException  ex) {}
        return null;
    }
    
    
    //========================================================================//
    //Thread Client
    //========================================================================//
    /**
     * 
     */
    public class ThreadClient extends Thread {

        private final Socket socketClient;
        private final String iaClient;
        private final ObjectOutputStream output;
        private final ObjectInputStream input;
        
        private int cliListPort;

        private String threadName;
        
        private SecretKey sessionK;
        private PublicKey cliPK;
        
        private boolean skReceive = false;
        private boolean pkReceive = false;

        /**
         * 
         * @param s
         * @throws IOException 
         */
        public ThreadClient(Socket s) throws IOException {
            socketClient = s;
            iaClient = s.getInetAddress().getHostAddress();
            this.output = new ObjectOutputStream(this.socketClient.getOutputStream());
            this.input = new ObjectInputStream(this.socketClient.getInputStream());         
        }

        /**
         * 
         */
        @Override
        public void run() {
            Object o;
            Message message;
            ArrayByteMessage messageByte;
         
            try {      
            loop: while(( o = input.readObject()) != null) {
                
                    if(o.getClass().getName().equalsIgnoreCase("Message")){
                        message = (Message)o;
                        
                    }else if(o.getClass().getName().equalsIgnoreCase("MessageByte")){   
                        messageByte = (ArrayByteMessage) o;
                        message = msgUncrypting(messageByte.getArrayByte(), sessionK);
                        
                    }else 
                        message = null;
                    
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
                            
                            closeConnection();
                            break;
                            
                            /**
                             * demande de login d'un client
                             */
                        case 1:
                            System.out.println("je recois une demande de login de \""+message.getPseudo()+"\"");
                            if(!skReceive || !pkReceive) send(new Message("SERVER","KCA","******",4, 10));
                            else{
                                if(IDVerif(message.getPseudo(),message.getMessage())){
                                    System.out.println("demande acceptée");
                                    this.threadName = message.getPseudo();
                                    synchronized (usersList) {
                                        usersList.add(this);
                                        broadcast(new Message(message.getPseudo()," connecting.","******",3));  
                                    }
                                    cryptedSend(new Message(name," you're now online.","******",3));
                                    cryptedSend(new Message(name," your listening port number","******",6, clientPort++));
                                    contactsSending();                              
                                }else{
                                    System.out.println("demande refusée");
                                    cryptedSend(new Message(name," connexion refused.","******",3));
                                    closeConnection();
                                }
                            }
                            
                            break;
                           
                            /**
                             * un client se deco
                             */
                        case 2: 
                            broadcast(new Message(threadName, " leaving.","******",message.getType()));
                            return;                  

                            /**
                             * un message a broadcast
                             */
                        case 3:
                            broadcast(new Message(threadName,message.getMessage(),"******", 3));
                            break;
                            
                            /**
                             * réception de clé de session
                             */
                        case 7:
                            this.sessionK =  sessKeyUnwrapping(message.getK());
                            skReceive = true;
                            break;
                            
                            /**
                             * reception de cle publique par le serveur pour crypter le massage vers le futur client
                             */
                        case 8:
                            this.cliPK = pubKeyUnwrapping(message.getK());
                            pkReceive = true;
                            break;
                                                  
                        default:
                            System.err.println("Unknow command from: "+ socketClient.getInetAddress().getCanonicalHostName());
                            break;
                            
                    } // fin switch
                } // fin while // fin while // fin while // fin while // fin while // fin while // fin while // fin while
            } catch (IOException ioe) {
                System.err.println("Problem with " + threadName + " : " + ioe );
            } catch (Exception e){    }
            
        } // fin run
        
        /**
         * 
         * @param m
         * @param i 
         */
        protected void send(Message m) {
            //TODO crypter avec cle de session
            try {
                output.writeObject(m);
                output.flush();
            } catch (SocketException  ioe) {
                System.err.println("Connection lost. "+ ioe.getMessage() +"\n");
            }catch(IOException ioe){
                System.err.println("A wild problem appears! "+ ioe.getMessage() +"\n");
            }
        }
        
        /**
         * pour chaque client, envoie des infos de celui ci au client
         */
        private void contactsSending(){
            synchronized(usersList){      
                for (int i = 0; i < usersList.size(); i++) {
                    if(usersList.get(i) != this){
                        cryptedSend(new Message
                            (usersList.get(i).threadName
                                    ," a contact to you"
                                    ,usersList.get(i).iaClient
                                    ,5
                                    ,usersList.get(i).cliListPort
                                    ,usersList.get(i).cliPK));
                    }
                }
            }
        }
        
        /**
         * 
         * @param message 
         */
        private synchronized void broadcast(Message message) {
            System.out.println(message.getPseudo() + " : " + message.getMessage() + "\n");
            synchronized(usersList){      
                for(ThreadClient tcl : usersList){
                    tcl.cryptedSend(message);        
                }
            }
        }
                      
        /**
         * 
         * @param m 
         */
        protected void cryptedSend(Message m){
            try {
                output.writeObject(new ArrayByteMessage(msgCrypting( m, sessionK)));
                output.flush();
            } catch (SocketException  ioe) {
                System.err.println("Connection lost. "+ ioe.getMessage() +"\n");
            }catch(IOException ioe){
                System.err.println("A wild problem appears! "+ ioe.getMessage() +"\n");
            }
        }    
        
        /**
        * 
        * @param m
        * @param sessKey
        * @return
        * @throws NoSuchAlgorithmException
        * @throws NoSuchPaddingException
        * @throws InvalidKeyException
        * @throws IllegalBlockSizeException
        * @throws BadPaddingException 
        */
        protected byte[] msgCrypting(Message m, SecretKey sessKey){
           try {
               Cipher cipher = Cipher.getInstance("AES");
               cipher.init(Cipher.ENCRYPT_MODE, sessKey);
               byte[] donnees = null;
               donnees = Message.serialize(m);

               return cipher.doFinal(donnees);
           } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {

           }
           return null;
       }

        /**
        * 
        * @param data
        * @param sessKey
        * @return
        * @throws NoSuchAlgorithmException
        * @throws NoSuchPaddingException
        * @throws InvalidKeyException
        * @throws IllegalBlockSizeException
        * @throws BadPaddingException 
        */
        protected  Message msgUncrypting(byte[] data, SecretKey sessKey){
                try {
                     Message msg = null;
                    Cipher cipher = Cipher.getInstance("AES");
                    cipher.init(Cipher.DECRYPT_MODE, sessKey);
                    return (Message) Message.deserialize(cipher.doFinal(data));
                } catch (IOException | ClassNotFoundException | IllegalBlockSizeException | BadPaddingException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }  
            return null;
           }
        
        /**
         * 
         */
        private void closeConnection() {
            try {
                this.input.close();
                this.output.close();
                this.socketClient.close();
            } catch (IOException ex) {
                System.err.println("probleme à la fermetre du client");
            }                    
        }
        
        /**
         * 
         * @param name 
         */
        protected void setNickName(String name){
            this.threadName = name;
        }
        
        /**
         * 
         * @return 
         */
        public String getNickName(){
            return this.threadName;
        }
    }
} 