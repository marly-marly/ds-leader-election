import java.io.*;

public class Logger {

    private static Logger instance = null;

    private Writer writer;

    private Logger(){
        try {
            this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("log.txt"), "utf-8"));
        } catch (IOException ex) {
            System.out.println("Unable to open file writer: " + ex.toString());
        }
    }

    public void closeWriter(){
        try {
            writer.close();
        } catch (Exception ex) {
            System.out.println("Unable to close file writer: " + ex.toString());
        }

        instance = null;
    }

    public void log(String line){
        try {
            this.writer.write(line + "\n");
        } catch (IOException e) {
            System.out.println("Unable to write to file: " + e.toString());
        }
    }

    public static Logger getInstance(){
        if (instance == null){
            instance = new Logger();
        }

        return instance;
    }
}
