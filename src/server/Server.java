package server;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import message.Message;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
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
import message.MessageByte;

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
     * @throws IOException 
     */
    Server() throws IOException { 
        serverSocket = new ServerSocket(serverSocketPort);
        usersList = new ArrayList<>(); // la liste de mes clients
    }
    
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
     * @return
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws InvalidKeySpecException 
     */    
    private PublicKey serverPKRecup(){
        FileInputStream keyfis = null;
        try {
            keyfis = new FileInputStream("serverPK.puk");
            byte[] encKey = new byte[keyfis.available()];
            keyfis.read(encKey);
            keyfis.close();
            X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(encKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(pubKeySpec);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                keyfis.close();
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    public void unwrap(final MessageByte msg) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException{
        Cipher cipherRsa = Cipher.getInstance("RSA");
        cipherRsa.init(Cipher.UNWRAP_MODE, privateKey);
        sessKey = (SecretKey) cipherRsa.unwrap(msg.getMyObject(), "AES", Cipher.SECRET_KEY);
        System.out.println(sessKey);
    }
    
    
    //========================================================================//
    //Thread Client
    //========================================================================//
    /**
     * 
     */
    public class ThreadClient extends Thread {

        private final Socket socketClient;
        private final ObjectOutputStream output;
        private final ObjectInputStream input;
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
            MessageByte messageByte;
         
            try {      
            loop: while(( o = input.readObject()) != null) {
                
                    if(o.getClass().getName().equalsIgnoreCase("Message")){
                        message = (Message)o;
                        
                    }else if(o.getClass().getName().equalsIgnoreCase("MessageByte")){   
                        messageByte = (MessageByte) o;
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
                            if(IDVerif(message.getPseudo(),message.getMessage())){
                                System.out.println("demande acceptée");
                                this.nickname = message.getPseudo();
                                synchronized (usersList) {
                                    usersList.add(this);
                                    broadcast(new Message(message.getPseudo()," connecting.","******",3));  
                                }
                                cryptedSend(new Message(message.getPseudo()," you're now online.","******",3));
                                contactSending();
                                
                            }else{
                                System.out.println("demande refusée");
                                cryptedSend(new Message(message.getPseudo()," connexion refused.","******",3));
                                closeConnection();
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
                             * réception de clé de session du contact
                             */
                        case 7:
                            System.out.println("TODO CONVERSION");
                            //this.sessionK =  message.getK();
                            break;
                            
                            /**
                             * reception de cle publique d'un client pour la donner a ses contact
                             */
                        case 8:
                            //this.cliPubKey = message.getK();
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
                    tcl.cryptedSend(message);        
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
                        cryptedSend(new Message(usersList.get(i).nickname,""+(clientPort),usersList.get(i).iaClient,5));
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
         * 
         * @param m 
         */
        protected void cryptedSend(Message m){
            try {
                output.writeObject(new MessageByte(msgCrypting( m, sessionK)));
                output.flush();
            } catch (SocketException  ioe) {
                System.err.println("Connection lost. "+ ioe.getMessage() +"\n");
            }catch(IOException ioe){
                System.err.println("A wild problem appears! "+ ioe.getMessage() +"\n");
            }
        }    
        
        /**
        * 
        * @param msg
        * @param cle
        * @return
        * @throws NoSuchAlgorithmException
        * @throws NoSuchPaddingException
        * @throws InvalidKeyException
        * @throws IllegalBlockSizeException
        * @throws BadPaddingException 
        */
        public byte[] msgCrypting(final Message msg, SecretKey cle){
           try {
               Cipher cipher = Cipher.getInstance("AES");
               cipher.init(Cipher.ENCRYPT_MODE, cle);
               byte[] donnees = null;
               donnees = Message.serialize(msg);

               return cipher.doFinal(donnees);
           } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {

           }
           return null;
       }

        /**
        * 
        * @param donnees
        * @param cle
        * @return
        * @throws NoSuchAlgorithmException
        * @throws NoSuchPaddingException
        * @throws InvalidKeyException
        * @throws IllegalBlockSizeException
        * @throws BadPaddingException 
        */
        public  Message msgUncrypting(final byte[] donnees, SecretKey cle){
               try {
                   Message msg = null;
                   Cipher cipher = Cipher.getInstance("AES");
                   cipher.init(Cipher.DECRYPT_MODE, cle);
                   
                   try {
                       msg = (Message) Message.deserialize(cipher.doFinal(donnees));
                       //return new String(cipher.doFinal(donnees));
                   } catch (IOException | ClassNotFoundException | IllegalBlockSizeException | BadPaddingException ex) {
                       Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                   }
                   return msg;
               } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException ex) {
                   Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
               }
            return null;
           }

    }
} 