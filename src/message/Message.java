package message;

import java.io.Serializable;

public class Message implements Serializable{
    
    private final String pseudo;
    private final String message;
    private final String adress;
    private final int others;
    private final int type;
    
    public Message(String pseudo, String msg, String adress, int type) {
        this.pseudo = pseudo;
        this.message = msg;
        this.type = type;
        this.adress = adress;
        this.others = 0;
    }
    
    public Message(String pseudo, String msg, String adress, int type, int others) {
        this.pseudo = pseudo;
        this.message = msg;
        this.type = type;
        this.adress = adress;
        this.others = others;
    }
    
    public String getPseudo() {
        return pseudo;
    }

    public String getMessage() {
        return message;
    }
    
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
}