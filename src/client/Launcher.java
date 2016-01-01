package client;

import java.io.IOException;
import java.util.Scanner;

/**
 *
 * @author raphaël
 */
public class Launcher extends Thread{
    
    public static void main(String[] args) throws IOException, InterruptedException {
        
        Scanner clavier = new Scanner(System.in);
        ThreadClient tc = new ThreadClient();
        while(true){
            System.out.println("quel voulez vous faire?"
                    + "\n0: enregistrement"
                    + "\n1: login"
                    + "\n3: message");
            switch(clavier.nextInt()){
                case 0: System.out.println("enregistrement!");
                    break;
                case 1: 
                    System.out.println("pseudo + password");
                    tc.login(clavier.next(), clavier.next());
                    break;
                case 3: 
                    System.out.println("message + id");
                    tc.send(clavier.next(), clavier.nextInt());
            }
          
        }
        
    }
}
