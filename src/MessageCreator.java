
public class MessageCreator {

    public static final String ELECTION_TAG = "ELECT";

    public static final String LEADER_TAG = "LEADER";

    public static String createElectMessage(int initializerId, int maximumId){
        return String.format("%s %d %d", ELECTION_TAG, initializerId, maximumId);
    }

    public static String createLeaderMessage(int initializerId, int leaderId){
        return String.format("%s %d %d", LEADER_TAG, initializerId, leaderId);
    }

    public static String getMessageType(String message){
        String[] parts = message.split("\\s");

        return parts[0];
    }

    public static int getInitializerIdFromElectMessage(String message){
        String[] parts = message.split("\\s");

        return Integer.valueOf(parts[1]);
    }

    public static int getMaximumIdFromElectMessage(String message){
        String[] parts = message.split("\\s");

        return Integer.valueOf(parts[2]);
    }

    public static int getLeaderIdFromLeaderMessage(String message){
        String[] parts = message.split("\\s");

        return Integer.valueOf(parts[2]);
    }

    public static int getInitializerIdFromLeaderMessage(String message){
        String[] parts = message.split("\\s");

        return Integer.valueOf(parts[1]);
    }
}
