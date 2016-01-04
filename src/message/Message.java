package message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Message implements Serializable{

    public static byte[] serialize(Message msg) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    private final String pseudo;
    private final String message;
    private final String adress;
    private final int others;
    private final int type;
    private final byte[] key;
    
    /**
     * envois message de base
     * @param pseudo
     * @param msg
     * @param adress
     * @param type 
     */
    public Message(String pseudo, String msg, String adress, int type) {
        this.pseudo = pseudo;
        this.message = msg;
        this.type = type;
        this.adress = adress;
        this.others = 0;
        this.key = null;
    }
    
    /**
     * envois de port
     * @param pseudo
     * @param msg
     * @param adress
     * @param type
     * @param others 
     */
    public Message(String pseudo, String msg, String adress, int type, int others) {
        this.pseudo = pseudo;
        this.message = msg;
        this.type = type;
        this.adress = adress;
        this.others = others;
        this.key = null;
    }
    
    /**
     * envois de clé
     * @param pseudo le login pour la co
     * @param msg le mot de passe
     * @param adress l'ip
     * @param type 7
     * @param key la clé envelopée
     */
    public Message(String pseudo, String msg, String adress, int type, byte[] key) {
        this.pseudo = pseudo;
        this.message = msg;
        this.type = type;
        this.adress = adress;
        this.others = 0;
        this.key = key;
    }
    
    /**
     * 
     * @return 
     */
    public String getPseudo() {
        return pseudo;
    }

    /**
     * 
     * @return 
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * 
     * @return 
     */
    public String getAdress(){
        return this.adress;
    }
    
    /**
     * @return the type
     * 0=inscription
     * 1=login
     * 2=leave
     * 3=msg
     * 4=ACK from server 
     * 5=contact port: msg -- ip : adress -- nom : pseudo
     */
    public int getType() {
        return type;
    }

    /**
     * @return the others
     */
    public int getOthers() {
        return others;
    }
    
    /**
     * 
     * @return 
     */
    public byte[] getK(){
        return this.key;
    }
   
    /**
     * 
     * @param obj
     * @return
     * @throws IOException 
     */
    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }
    
    /**
     * 
     * @param data
     * @return
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }
}