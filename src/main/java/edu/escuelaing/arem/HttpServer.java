package edu.escuelaing.arem;

import java.net.*;
import java.io.*;
import java.util.ArrayList;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

public class HttpServer {
    private static final HttpServer _instance = new HttpServer();
    private static final String HTTP_MESSAGE = "HTTP/1.1 200 OK\n" + "Content-Type: #/#\r\n" + "\r\n";

    public static HttpServer getInstance(){
        return _instance;
    }

    private HttpServer(){}

    public void start(String[] args) throws IOException{
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(4567);
        } catch (IOException e) {
            System.err.println("Could not listen on port: 35000.");
            System.exit(1);
        }
        boolean running = true;
        while(running){
            Socket clientSocket = null;
            try {
                System.out.println("Listo para recibir ...");
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                System.err.println("Accept failed.");
                System.exit(1);
            }
            try {
                serverConnection(clientSocket);
            } catch (URISyntaxException e) {
                System.err.println("URI incorrect.");
                System.exit(1);
            }
        }
        serverSocket.close();
    }

    public void serverConnection(Socket clientSocket) throws IOException, URISyntaxException {
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        clientSocket.getInputStream()));
        String inputLine, outputLine;
        ArrayList<String> request = new ArrayList<String>();

        while ((inputLine = in.readLine()) != null) {
            System.out.println("Received: " + inputLine);
            request.add(inputLine);
            if (!in.ready()) {
                break;
            }
        }
        String uriStr = request.get(0).split(" ")[1];
        URI resourceURI = new URI(uriStr);
        outputLine = getResource(resourceURI);
        
        out.println(outputLine);
        out.close();
        in.close();
        clientSocket.close();
    }

    private String[] splitFileName(String filename){
        int splitIndex = 0;
        for(int i=0; i < filename.length(); i++){
            if(filename.charAt(i) == '.') splitIndex = i;
        }
        return new String[]{filename.substring(0, splitIndex), filename.substring(splitIndex+1, filename.length())};
    }

    public String getResource(URI resourceURI) throws URISyntaxException{
        String filename = resourceURI.toString().replaceFirst("/", "");
        return computeHTMLResponse(filename);
    }

    public String computeHTMLResponse(String filename){
        if (filename.equals("")) filename = "page.html";
        String extension = splitFileName(filename)[1];
        String content = "";
        Boolean notFound = false;
        try {
            if (extension.equals("png") || extension.equals("jpeg")) content = getImageFile(filename, extension);
            else content = getFile(filename, extension);
        } catch (FileNotFoundException e) { notFound = true;}
        if (notFound) {
            try { content = getFile("404.html", "html"); } 
            catch (FileNotFoundException e) {}   
        }
        return content;
    }

    public static void makeJSFile(){
        File folder = new File("./src/main/resources/public");
        File[] listOfFiles = folder.listFiles();
        String fileList = "export let fileList = [";
        Boolean toCut = false;
        for(File file : listOfFiles) {
            fileList += '"' + file.getName() + '"' + ",";
            toCut = true;
        }
        if(toCut) fileList = fileList.substring(0, fileList.length()-1);
        fileList += "]";
        try {
            File page = new File("./src/main/resources/public/page.js");
            if(!page.exists()) page.createNewFile();
            FileWriter fileWriter = new FileWriter(page);
            fileWriter.write(fileList);
            fileWriter.close();
        } catch (IOException e) {e.printStackTrace();}
        
    }

    public static String getFile(String filename, String extension) throws FileNotFoundException{
        String content = HTTP_MESSAGE.replaceFirst("#", "text").replaceFirst("#", extension);
        File file = new File("./src/main/resources/public/" + filename);
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while((line =  br.readLine()) != null) content += line+"\n";    
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    private String getImageFile(String filename, String extension) throws FileNotFoundException{
        String content = HTTP_MESSAGE.replaceFirst("#", "image").replaceFirst("#", extension);
        File file = new File("./src/main/resources/public/" + filename);
        try {
            System.out.println(file.toString());
            content += file.toString();
        } catch (/*IO*/Exception e) {
            e.printStackTrace();
        }
        return content;
    }

    public static void main(String[] args) throws IOException {
        HttpServer.makeJSFile();
        HttpServer.getInstance().start(args);
    }
}
