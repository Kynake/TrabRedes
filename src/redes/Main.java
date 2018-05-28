package redes;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Main {

    private static final int MAX_DATA_LENGTH = 32768;

    private static final int CLIENT_HELLO   = 0;
    private static final int CLIENT_MESSAGE = 1;
    private static final int SERVER_HELLO   = 2;
    private static final int SERVER_MESSAGE = 3;

    private static final int DEFAULT_SERVER_PORT = 9876;

    // server [this ip] - primeiro
    // server [this ip] [other ip] - segundo

    public static void main(String[] args) {
        try{
            if(args[0].equals("server"))
                server(args[1], args.length == 2? null : args[2]);
            else
                client();

        } catch(Exception e){
            e.printStackTrace();
        }
    }

    private static void server(String networkIP, String firstServerIP) throws Exception{
        System.out.println(firstServerIP == null? "Primeiro Servidor" : "Segundo Servidor");

        //Set up
        ArrayList<String> clientList = new ArrayList<>(); // clients ports that are known to exist, since all local clients have the localhost ip

        int  localServerPort = DEFAULT_SERVER_PORT;
        DatagramSocket localServerSocket = new DatagramSocket(localServerPort);

        InetAddress remoteServerAddress = InetAddress.getByName("255.255.255.255");
        int remoteServerPort = DEFAULT_SERVER_PORT;

        byte[] receivedData = new byte[MAX_DATA_LENGTH];
        byte[] sendData;

        if(firstServerIP != null){ //send message to first server
            //get first server address
            remoteServerAddress = InetAddress.getByName(firstServerIP);

            //construct hello message
            Message helloMessage = new Message(
                    SERVER_HELLO,
                    "Hello! I'm a server.",
                    localServerPort,
                    firstServerIP,
                    remoteServerPort
            );
            sendData = Message.toByteArray(helloMessage);

            //send Server Hello message
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, remoteServerAddress, remoteServerPort);
            localServerSocket.send(sendPacket);
            System.out.printf("Mensagem de Server Hello enviada para %s:%d\n", remoteServerAddress.getHostAddress(), remoteServerPort);

            //set up receive packet
            DatagramPacket receivedPacket = new DatagramPacket(receivedData, receivedData.length);
            Message receivedMessage;

            do{ //wait for first server's Server Hello response
                System.out.println("Esperando pela resposta do primeiro servidor na porta " + localServerPort);
                localServerSocket.receive(receivedPacket);
                System.out.println("Datagrama UDP recebido, interpretando...");

                //Get data
                receivedMessage = Message.toMessage(receivedPacket.getData());
                System.out.println(receivedMessage);

                System.out.println(receivedMessage.getType() == SERVER_HELLO? "Resposta Server Hello recebida..." : "Mensagem não é do tipo Server Hello, ignorada...");

            } while(receivedMessage.getType() != SERVER_HELLO);
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); //for closing server after use

        while(!br.ready()){ //main server loop
            //print clients list
            System.out.println("Clientes:");
            for(String client : clientList)
                System.out.printf("%s:%s\n", networkIP, client);


            //set up
            Message serverMessage, replyMessage;
            DatagramPacket sendPacket, receivedPacket;
            int clientReplyPort;

            //set up receive packet
            receivedPacket = new DatagramPacket(receivedData, receivedData.length);

            //wait for message
            System.out.println("Esperando por datagrama UDP na porta " + localServerPort);
            localServerSocket.receive(receivedPacket);
            System.out.println("Datagrama UDP recebido, interpretando...");

            //Get data
            Message receivedMessage = Message.toMessage(receivedPacket.getData());
            System.out.println(receivedMessage);

            //treat message
            switch(receivedMessage.getType()){
                case CLIENT_HELLO: //Client Hello
                    System.out.println("Client Hello received.");

                    //add port to knowns list
                    clientList.add(String.valueOf(receivedMessage.getSourcePort())); //port as string

                    //construct reply message
                    clientReplyPort = receivedMessage.getSourcePort();
                    replyMessage = new Message(
                            SERVER_HELLO,
                            "Hello! i'm a server",
                            localServerPort,
                            "localhost",
                            clientReplyPort
                    );
                    sendData = Message.toByteArray(replyMessage);

                    //send Server Hello reply
                    sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("localhost"), clientReplyPort);
                    localServerSocket.send(sendPacket);
                    System.out.printf("Resposta Server Hello enviada para localhost:%d\n", clientReplyPort);
                    break;

                case CLIENT_MESSAGE: //Client Message
                    System.out.println("Client Message received.");

                    //analyze message
                    if(receivedMessage.getDestinationIP().equals("localhost")){ //same network message
                        if(clientList.contains(String.valueOf(receivedMessage.getDestinationPort()))){ //known client
                            //construct server message
                            serverMessage = new Message(
                                    SERVER_MESSAGE,
                                    receivedMessage.getData(),
                                    receivedMessage.getSourcePort(),
                                    receivedMessage.getDestinationIP(),
                                    receivedMessage.getDestinationPort()
                            );
                            sendData = Message.toByteArray(serverMessage);

                            //send Server Message
                            sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("localhost"), receivedMessage.getDestinationPort());
                            localServerSocket.send(sendPacket);
                            System.out.printf("Mensagem Server Message enviada para localhost:%d\n", receivedMessage.getDestinationPort());

                        } else if(receivedMessage.getDestinationPort() == localServerPort){ //the client sent me, the local server, a message
                            System.out.printf("Mensagem do cliente localhost:%d para este servidor recebida: %s\n", receivedMessage.getSourcePort(), receivedMessage.getData());
                        } else{ //unknown client
                            System.out.printf("O cliente localhost:%d enviou uma mensagem para porta desconhecida. Mandando reply de Destination Unreachable...\n", receivedMessage.getSourcePort());

                            //construct reply message
                            replyMessage = new Message(
                                    SERVER_MESSAGE,
                                    "Destination Unreachable",
                                    localServerPort,
                                    "localhost",
                                    receivedMessage.getSourcePort()
                            );
                            sendData = Message.toByteArray(replyMessage);

                            //send Server Message reply
                            sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("localhost"), receivedMessage.getSourcePort());
                            localServerSocket.send(sendPacket);
                            System.out.printf("Mensagem Server Message enviada para localhost:%d\n", receivedMessage.getSourcePort());

                        }
                    } else if(receivedMessage.getDestinationIP().equals(remoteServerAddress.getHostAddress())){ //the other network
                        System.out.printf("Request for message to %s:%d.\n", remoteServerAddress.getHostAddress(), receivedMessage.getDestinationPort());

                        //construct server message
                        serverMessage = new Message(
                                SERVER_MESSAGE,
                                receivedMessage.getData(),
                                receivedMessage.getSourcePort(),
                                receivedMessage.getDestinationIP(),
                                receivedMessage.getDestinationPort()
                        );
                        sendData = Message.toByteArray(serverMessage);

                        //send Server Message
                        sendPacket = new DatagramPacket(sendData, sendData.length, remoteServerAddress, remoteServerPort);
                        localServerSocket.send(sendPacket);
                        System.out.printf("Resposta Server Message enviada para %s:%d\n", remoteServerAddress.getHostAddress(), remoteServerPort);

                    } else{ //unknown network
                        System.out.printf("O cliente localhost:%d enviou uma mensagem para rede desconhecida. Mandando reply de Destination Unreachable...\n", receivedMessage.getSourcePort());

                        //construct reply message
                        replyMessage = new Message(
                                SERVER_MESSAGE,
                                "Destination Unreachable",
                                localServerPort,
                                "localhost",
                                receivedMessage.getSourcePort()
                        );
                        sendData = Message.toByteArray(replyMessage);

                        //send Server Message reply
                        sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("localhost"), receivedMessage.getSourcePort());
                        localServerSocket.send(sendPacket);
                        System.out.printf("Resposta Server Message enviada para localhost:%d\n", receivedMessage.getSourcePort());
                    }

                    break;

                case SERVER_HELLO: //Server Hello
                    System.out.println("Server Hello received.");
                    remoteServerAddress = receivedPacket.getAddress();

                    //construct reply message
                    replyMessage = new Message(
                            SERVER_HELLO,
                            "Hello back! I'm a server too.",
                            localServerPort,
                            remoteServerAddress.getHostAddress(),
                            remoteServerPort
                    );
                    sendData = Message.toByteArray(replyMessage);

                    //send Server Hello reply
                    sendPacket = new DatagramPacket(sendData, sendData.length, remoteServerAddress, remoteServerPort);
                    localServerSocket.send(sendPacket);
                    System.out.printf("Resposta Server Hello enviada para %s:%d\n", remoteServerAddress.getHostAddress(), remoteServerPort);
                    break;

                case SERVER_MESSAGE: //Server Message
                    System.out.printf("Mensagem Server Message recebida de %s:%d\n", remoteServerAddress.getHostAddress(), remoteServerPort);

                    //interpret message
                    if(receivedMessage.getDestinationIP().equals(networkIP)){ //this network
                        if(clientList.contains(String.valueOf(receivedMessage.getDestinationPort()))){ //known client
                            System.out.printf("Mensagem de %s:%d para localhost:%d\n", remoteServerAddress.getHostAddress(), receivedMessage.getSourcePort(), receivedMessage.getDestinationPort());

                            //construct Server Message
                            serverMessage = new Message (
                                    SERVER_MESSAGE,
                                    receivedMessage.getData(),
                                    receivedMessage.getSourcePort(),
                                    remoteServerAddress.getHostAddress(),
                                    receivedMessage.getSourcePort()
                            );
                            sendData = Message.toByteArray(serverMessage);

                            //send Server Message
                            sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("localhost"), receivedMessage.getDestinationPort());
                            localServerSocket.send(sendPacket);
                            System.out.printf("Mensagem Server Message enviada para localhost:%d\n", receivedMessage.getDestinationPort());

                        } else if(receivedMessage.getDestinationPort() == localServerPort){ //the remote server sent a message to this local server
                            System.out.printf("Mensagem de %s:%d para este servidor recebida: %s\n", remoteServerAddress.getHostAddress(), receivedMessage.getSourcePort(), receivedMessage.getData());
                        } else{ //unknown client
                            System.out.println("Cliente destino desconhecido, mandando reply de Destination Unreachable...");

                            //construct reply message
                            replyMessage = new Message(
                                    SERVER_MESSAGE,
                                    "Destination Unreachable",
                                    localServerPort,
                                    remoteServerAddress.getHostAddress(),
                                    receivedMessage.getSourcePort()
                            );
                            sendData = Message.toByteArray(replyMessage);

                            //send Server Message reply
                            sendPacket = new DatagramPacket(sendData, sendData.length, remoteServerAddress, receivedMessage.getSourcePort());
                            localServerSocket.send(sendPacket);
                            System.out.printf("Resposta Server Message enviada para %s:%d\n", remoteServerAddress.getHostAddress(), receivedMessage.getSourcePort());
                        }
                    } else{ //not this network
                        System.out.println("Rede destino desconhecida, mandando reply de Destination Unreachable...");

                        //construct reply message
                        replyMessage = new Message(
                                SERVER_MESSAGE,
                                "Destination Unreachable",
                                localServerPort,
                                remoteServerAddress.getHostAddress(),
                                receivedMessage.getSourcePort()
                        );
                        sendData = Message.toByteArray(replyMessage);

                        //send Server Message reply
                        sendPacket = new DatagramPacket(sendData, sendData.length, remoteServerAddress, receivedMessage.getSourcePort());
                        localServerSocket.send(sendPacket);
                        System.out.printf("Resposta Server Message enviada para localhost:%d\n", receivedMessage.getSourcePort());
                    }
                    break;

                default:
                    System.out.println("Unknown Message Type...");
            }
        }

        localServerSocket.close();
        System.out.println("Fechando o servidor...");
    }

    private static void client() throws Exception{
        //set up
        byte[] receivedData = new byte[MAX_DATA_LENGTH];
        byte[] sendData;
        int localServerPort = DEFAULT_SERVER_PORT, clientPort;

        Message sendMessage, receivedMessage;
        DatagramPacket sendPacket, receivedPacket = new DatagramPacket(receivedData, receivedData.length);

        String localServerIP = "localhost";
        InetAddress localServerAddress = InetAddress.getByName(localServerIP);
        DatagramSocket clientSocket = new DatagramSocket();
        clientPort = clientSocket.getLocalPort();

        //console print
        System.out.println("Porta local: "+clientPort);
        System.out.println("Conectando-se ao servidor: localhost:"+localServerPort);

        //construct hello message
        sendMessage = new Message(
                CLIENT_HELLO,
                "Hello! I'm a client",
                clientPort,
                localServerAddress.getHostAddress(),
                localServerPort
        );
        sendData = Message.toByteArray(sendMessage);

        //send Client Hello to server
        sendPacket = new DatagramPacket(sendData, sendData.length, localServerAddress, localServerPort);
        clientSocket.send(sendPacket);
        System.out.printf("Mensagem de Client Hello enviada para %s:%d\n", localServerAddress.getHostAddress(), localServerPort);

        //set up receive packet
        receivedPacket = new DatagramPacket(receivedData, receivedData.length);
        System.out.println("Esperando pela resposta do servidor na porta " + clientPort);

        //Get packet
        clientSocket.receive(receivedPacket);
        System.out.println("Datagrama UDP recebido, interpretando...");

        //get message
        receivedMessage = Message.toMessage(receivedPacket.getData());
        System.out.println(receivedMessage);
        if(receivedMessage.getType() != SERVER_HELLO){
            System.out.println("Unexpected message type...");
            return;
        }

        //enter send/receive message cycle
        clientSocket.setSoTimeout(1000); //only receive() for 1 second at a time

        String destinationIP = localServerAddress.getHostAddress();
        int destinationPort = localServerPort;
        String messageContent = "";

        //0 = must type IP
        //1 = has typed IP, must type port
        //2 = has typed both, must type message
        //3 = has typed all three, will send message
        int sendStage = 0; //must type IP

        //declare buffered reader for use in non blocking input reading
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String buffer;

        System.out.print("(Digite '-' para sair) Informe o IP de destino: ");
        while(true){
            //receive
            try { //wait until timeout for packet
                clientSocket.receive(receivedPacket);
                System.out.println("\nDatagrama UDP recebido, interpretando...");

                //get message
                receivedMessage = Message.toMessage(receivedPacket.getData());
                System.out.println(receivedMessage);

                //print message
                System.out.println("Mensagem recebida: "+receivedMessage.getData());

                //return to sending stage
                System.out.print(
                        sendStage < 2? sendStage < 1?
                                "(Digite '-' para sair) Informe o IP de destino: " :
                                "(Digite '-' para sair) Informe a porta de destino: " :
                                "(Digite '-' para sair) Informe a mensagem a ser enviada: "
                );
            } catch(SocketTimeoutException e){} //timed out, continue with cycle


            //send
            if(br.ready()){
                buffer = br.readLine();
                if(buffer.equals("-")){
                    break;
                }
                switch(sendStage){
                    case 0: //must type IP
                        sendStage++; //must now type port
                        destinationIP = buffer;
                        System.out.print("(Digite '-' para sair) Informe a porta de destino: ");
                        break;

                    case 1: //has typed IP, must type port
                        sendStage++; //must now type message
                        destinationPort = Integer.parseInt(buffer);
                        System.out.print("(Digite '-' para sair) Informe a mensagem a ser enviada: ");
                        break;

                    case 2: //has typed both, must type message
                        sendStage++; //will now send message
                        messageContent = buffer;
                        break;

                    default: //SHOULD NEVER HAPPEN. but just in case:
                        System.out.println("Unexpected state detected. aborting attempt...");
                        System.out.print("(Digite '-' para sair) Informe o IP de destino: ");
                        sendStage = 0; //reset sendStage to must type IP
                }
            }

            if(sendStage >= 3){ //will now send message
                sendStage = 0; //reset sendStage to must type IP

                //construct message
                sendMessage = new Message(
                        CLIENT_MESSAGE,
                        messageContent,
                        clientPort,
                        destinationIP,
                        destinationPort
                );
                sendData = Message.toByteArray(sendMessage);

                //send Client Hello to server
                sendPacket = new DatagramPacket(sendData, sendData.length, localServerAddress, localServerPort);
                clientSocket.send(sendPacket);
                System.out.printf("Mensagem de Client Message enviada para %s:%d. Destino: %s:%d\n", localServerAddress.getHostAddress(), localServerPort, destinationIP, destinationPort);
                System.out.print("(Digite '-' para sair) Informe o IP de destino: ");
            }
        }

        clientSocket.close();
        System.out.println("Fechando o cliente...");
    }

    private static void clearScreen(){System.out.print("\n\n\n\n\n\n\n\n\n\n\n\n\n");}
}