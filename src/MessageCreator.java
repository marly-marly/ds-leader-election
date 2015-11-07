import java.util.ArrayList;

public class MessageCreator {

    public static final String ELECTION_TAG = "ELECT";

    public static final String LEADER_TAG = "LEADER";

    public static final String FAIL_TAG = "FAIL";

    public static String createElectMessage(int initializerId, int maximumId){
        return String.format("%s %d %d", ELECTION_TAG, initializerId, maximumId);
    }

    public static String createLeaderMessage(int initializerId, int leaderId){
        return String.format("%s %d %d", LEADER_TAG, initializerId, leaderId);
    }

    public static String createFailMessage(int nodeId){
        return String.format("%s %d", FAIL_TAG, nodeId);
    }

    public static String getMessageType(String[] parts){

        return parts[0];
    }

    public static int getInitializerIdFromElectMessage(String[] parts){

        return Integer.valueOf(parts[1]);
    }

    public static int getMaximumIdFromElectMessage(String[] parts){

        return Integer.valueOf(parts[2]);
    }

    public static int getLeaderIdFromLeaderMessage(String[] parts){

        return Integer.valueOf(parts[2]);
    }

    public static int getInitializerIdFromLeaderMessage(String[] parts){

        return Integer.valueOf(parts[1]);
    }

    public static int getFailedNodeIdFromFailMessage(String[] parts){

        return Integer.valueOf(parts[1]);
    }
}
