import java.util.*;
import java.io.*;

/* 
Class to simulate the network. System design directions:

- Synchronous communication: each round lasts for 20ms
- At each round the network receives the messages that the nodes want to send and delivers them
- The network should make sure that:
	- A node can only send messages to its neighbours
	- A node can only send one message per neighbour per round
- When a node fails, the network must inform all the node's neighbours about the failure
*/

public class Network {

    private int round;
    private int period = 20;

    // Integer for the id of the sender and String for the message
    private Map<Integer, String> messagesToDeliver;

    private HashMap<Integer, Node> nodes;

    private HashMap<Integer, ArrayList<Action>> specialMessages;

    // Code to call methods for parsing the input file, initiating the system and producing the log can be added here.
    public Network() {

        this.nodes = new HashMap<>();
        this.messagesToDeliver = new HashMap<>();
        this.specialMessages = new HashMap<>();

        try {
            this.parseFile("input_simple.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.prepareInitialMessages();
        this.deliverMessages();
    }

    private boolean nodeExists(List<Node> nodes, int id){
        boolean nodeFound = false;

        for (Node node : nodes) {
            if (node.getNodeId() == id) {
                nodeFound = true;
                break;
            }
        }

        return nodeFound;
    }

    // Notice that the method's descriptor must be defined.
    private void parseFile(String fileName) throws IOException {

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {

            String line;
            Integer round;
            Node node;
            int nodeId;
            Action action;
            Node lastNode = null;
            Node firstNode = null;

            while ((line = br.readLine()) != null) {

                String[] parts = line.split("\\s");

                // Decide how we should process the line
                switch(parts[0]){

                    case "Node_id":
                        break;

                    case "ELECT":

                         action = new Action("ELECT");

                        // Create and/or add each node that are participating in the election
                        for (int i=2; i<parts.length; i++){

                            nodeId = Integer.valueOf(parts[i]);
                            node = this.addOrGetNodeWithId(nodeId);

                            action.addNode(node);
                        }

                        // Add the action to the list of messages for the specific round
                        round = Integer.valueOf(parts[1]);
                        if (this.specialMessages.containsKey(round)){
                            this.specialMessages.get(round).add(action);
                        }else{
                            ArrayList<Action> actions = new ArrayList<>();
                            actions.add(action);
                            this.specialMessages.put(round, actions);
                        }

                        break;

                    case "FAIL":

                        nodeId = Integer.valueOf(parts[1]);
                        node = this.addOrGetNodeWithId(nodeId);

                        action = new Action("FAIL");
                        action.addNode(node);

                        // Add the action to the list of messages for the specific round
                        round = Integer.valueOf(parts[1]);
                        if (this.specialMessages.containsKey(round)){
                            this.specialMessages.get(round).add(action);
                        }else{
                            ArrayList<Action> actions = new ArrayList<>();
                            actions.add(action);
                            this.specialMessages.put(round, actions);
                        }

                        break;

                    default:

                        int mainNodeId = Integer.valueOf(parts[0]);
                        Node mainNode = this.addOrGetNodeWithId(mainNodeId);

                        // For each neighbour, create if doesn't exist yet, then add as neighbour of main node
                        for (int i=1; i<parts.length; i++){

                            nodeId = Integer.valueOf(parts[i]);
                            node = this.addOrGetNodeWithId(nodeId);
                            mainNode.addNeighbour(node);
                        }

                        if (firstNode == null){
                            firstNode = mainNode;
                        }

                        if (lastNode != null){
                            lastNode.setNextNode(mainNode);
                            mainNode.setPreviousNode(lastNode);
                        }

                        lastNode = mainNode;

                        break;
                }
            }

            if (lastNode != null){
                lastNode.setNextNode(firstNode);
                firstNode.setPreviousNode(lastNode);
            }
        }
    }

    private Node addOrGetNodeWithId(Integer id){

        Node node;
        if (!nodes.containsKey(id)){
            nodes.put(id, new Node(id));
            node = nodes.get(id);
        }else{
            node = nodes.get(id);
        }

        return node;
    }

    public synchronized void addMessage(int id, String message) {
		/*
		At each round, the network collects all the messages that the nodes want to send to their neighbours. 
		Implement this logic here.
		*/

        this.messagesToDeliver.put(id, message);
    }

    private void prepareInitialMessages(){

        for (Map.Entry<Integer, Node> entry : this.nodes.entrySet()) {
            Node node = entry.getValue();
            node.incomingMessages.add(MessageCreator.createElectMessage(node.getNodeId(), node.getNodeId()));
        }
    }



    public synchronized void deliverMessages() {
		/*
		At each round, the network delivers all the messages that it has collected from the nodes.
		Implement this logic here.
		The network must ensure that a node can send only to its neighbours, one message per round per neighbour.
		*/

        while (true) {

            // Collect messages from each node
            for (Map.Entry<Integer, Node> entry : this.nodes.entrySet()) {
                Node node = entry.getValue();
                for (String message : node.incomingMessages) {
                    this.addMessage(entry.getKey(), message);
                }
            }

            // Break the loop if there are no more messages to deliver
            if (this.messagesToDeliver.size() == 0){
                break;
            }

            // Send messages to "next" from each node
            Iterator it = this.messagesToDeliver.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry messagesToDeliverPair = (Map.Entry) it.next();
                Integer senderId = (Integer) messagesToDeliverPair.getKey();
                Node sender = this.nodes.get(senderId);
                String message = (String) messagesToDeliverPair.getValue();

                Node neighbour = sender.getNextNode();
                sender.sendMessage(message);
                neighbour.receiveMessage(message);

                it.remove();
            }
        }
    }

    public synchronized void informNodeFailure(int id) {
		/*
		Method to inform the neighbours of a failed node about the event.
		*/
    }

    public static void main(String args[]) throws IOException, InterruptedException {
		/*
		Your main must get the input file as input.
		*/
        Network network = new Network();
    }
}
