package redes;

import java.io.*;

public class Message implements Serializable {
    //Atributos

    /*
        0: client hello
        1: client send message
        2: server hello
        3: server send message
    */
    private int type;
    private String data;
    private String destinationIP;
    private int sourcePort;
    private int destinationPort;
    private int checksum;

    //Construtor
    public Message(int type, String data, int srcPort, String destIP, int destPort){
        this.type = type;
        this.data = data;
        this.destinationIP = destIP;
        this.destinationPort = destPort;
        this.sourcePort = srcPort;
        checksum = calculateChecksum();
    }

    //Getters e Setters
    public int getType(){return type;}
    public void setType(int type){
        this.type = type;
        checksum = calculateChecksum();
    }

    public String getData(){return data;}
    public void setData(String data){
        this.data = data;
        checksum = calculateChecksum();
    }

    public String getDestinationIP(){return destinationIP;}
    public void setDestinationIP(String destinationIP){
        this.destinationIP = destinationIP;
        checksum = calculateChecksum();
    }

    public int getDestinationPort(){return destinationPort;}
    public void setDestinationPort(int destinationPort){
        this.destinationPort = destinationPort;
        checksum = calculateChecksum();
    }

    public int getSourcePort(){return sourcePort;}
    public void setSourcePort(int sourcePort) {
        this.sourcePort = sourcePort;
        checksum = calculateChecksum();
    }

    public int getChecksum(){return checksum;}

    //toString


    @Override
    public String toString() {
        String typeDesc;
        switch(type){
            case 0:
                typeDesc = "Client Hello";
                break;

            case 1:
                typeDesc = "Client Message";
                break;

            case 2:
                typeDesc = "Server Hello";
                break;

            case 3:
                typeDesc = "Server Message";
                break;

            default:
                typeDesc = "Unknown Type";
        }

        String checksumStatus = validChecksum()? "OK" : "NOK";

        return String.format("Mensagem:\n\tTipo: %d (%s)\n\tOrigem: %d\n\tDestino: %s:%d\n\tChecksum: %d (%s)\n\tData:\n\t\t%s\n", type, typeDesc, sourcePort, destinationIP, destinationPort, checksum, checksumStatus, data);
    }

    //Métodos
    public boolean validChecksum(){
        return checksum == calculateChecksum();
    }

    private int calculateChecksum(){
        int stringSum = 0;
        for(int i = 0; i < data.length(); i++){
            stringSum = stringSum*31 + (int)data.charAt(i);
        }

        for(int i = 0; i < destinationIP.length(); i++){
            stringSum = stringSum*31 + (int)destinationIP.charAt(i);
        }

        return type + sourcePort + destinationPort + stringSum;
    }

    public static byte[] toByteArray(Message message) throws IOException {
        byte[] bytes = null;
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;
        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(message);
            oos.flush();
            bytes = bos.toByteArray();
        } finally {
            if (oos != null) {
                oos.close();
            }
            if (bos != null) {
                bos.close();
            }
        }
        return bytes;
    }

    public static Message toMessage(byte[] bytes) throws IOException, ClassNotFoundException {
        Message message;
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        try {
            bis = new ByteArrayInputStream(bytes);
            ois = new ObjectInputStream(bis);
            message = (Message)ois.readObject();
        } finally {
            if (bis != null) {
                bis.close();
            }
            if (ois != null) {
                ois.close();
            }
        }
        return message;
    }
}
