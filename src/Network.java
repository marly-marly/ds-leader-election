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

    private HashMap<Integer, ArrayList<Action>> roundActions;

    private Logger logger;

    // Code to call methods for parsing the input file, initiating the system and producing the log can be added here.
    public Network() {

        this.nodes = new HashMap<>();
        this.messagesToDeliver = new HashMap<>();
        this.roundActions = new HashMap<>();

        try {
            this.parseFile("input.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.logger = Logger.getInstance();

        // this.prepareInitialMessages();
        this.deliverMessages();
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

                // Decide how we should process the line
                String[] parts = line.split("\\s");

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
                        if (this.roundActions.containsKey(round)){
                            this.roundActions.get(round).add(action);
                        }else{
                            ArrayList<Action> actions = new ArrayList<>();
                            actions.add(action);
                            this.roundActions.put(round, actions);
                        }

                        break;

                    case "FAIL":

                        nodeId = Integer.valueOf(parts[1]);
                        node = this.addOrGetNodeWithId(nodeId);

                        action = new Action("FAIL");
                        action.addNode(node);

                        // Add the action to the list of messages for the specific round
                        round = Integer.valueOf(parts[1]);
                        if (this.roundActions.containsKey(round)){
                            this.roundActions.get(round).add(action);
                        }else{
                            ArrayList<Action> actions = new ArrayList<>();
                            actions.add(action);
                            this.roundActions.put(round, actions);
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
                        }

                        lastNode = mainNode;

                        break;
                }
            }

            if (lastNode != null){
                lastNode.setNextNode(firstNode);
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

    public synchronized void deliverMessages() {
		/*
		At each round, the network delivers all the messages that it has collected from the nodes.
		Implement this logic here.
		The network must ensure that a node can send only to its neighbours, one message per round per neighbour.
		*/

        for (Node node : this.nodes.values()){
            node.start();
        }

        while (true) {

            this.round++;
            this.logger.log(String.format("-- Round %d starting", this.round));

            // Check if there's any action to take in this round
            ArrayList<Action> actions = this.roundActions.get(round);
            if (actions != null){
                for (Action action : actions){

                    switch(action.getType()){

                        case MessageCreator.ELECTION_TAG:
                            for (Node node : action.getNodes()){
                                node.startLeaderElection();
                            }

                            break;
                    }
                }

                this.roundActions.remove(round);
            }

            // Collect messages from each node
            for (Map.Entry<Integer, Node> entry : this.nodes.entrySet()) {
                Node node = entry.getValue();
                if (node.outgoingMessages.size() != 0){
                    this.addMessage(node.getNodeId(), node.outgoingMessages.get(0));
                }
            }

            // Break the loop if there are no more messages to deliver
            if (this.messagesToDeliver.size() == 0 && this.roundActions.size() == 0 && this.allNodesFinished()){
                this.stopAllNodes();
                this.logger.closeWriter();
                break;
            }

            // Send messages to "next" from each node
            Iterator iterator = this.messagesToDeliver.entrySet().iterator();
            while(iterator.hasNext()) {
                Map.Entry messagesToDeliverPair = (Map.Entry) iterator.next();
                Integer senderId = (Integer) messagesToDeliverPair.getKey();
                Node sender = this.nodes.get(senderId);
                String message = (String) messagesToDeliverPair.getValue();

                sender.sendMessage(message);
            }

            // Receive messages
            iterator = this.messagesToDeliver.entrySet().iterator();
            while(iterator.hasNext()) {
                Map.Entry messagesToDeliverPair = (Map.Entry) iterator.next();
                Integer senderId = (Integer) messagesToDeliverPair.getKey();
                Node sender = this.nodes.get(senderId);
                String message = (String) messagesToDeliverPair.getValue();

                Node nextNeighbour = sender.getNextNode();
                nextNeighbour.receiveMessage(message);

                iterator.remove();
            }

            try {
                Thread.sleep(this.period);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopAllNodes(){
        for (Node node : this.nodes.values()){
            node.setActive(false);
        }
    }

    private boolean allNodesFinished(){
        boolean finished = true;
        for (Node node : this.nodes.values()){
            finished &= node.finished;
        }

        return finished;
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
