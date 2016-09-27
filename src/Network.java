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

    // Stores elections for each round. Note that multiple actions is allowed for one round.
    private HashMap<Integer, ArrayList<Action>> roundActions;

    private ArrayList<Node> failures;

    private Logger logger;

    // Code to call methods for parsing the input file, initiating the system and producing the log can be added here.
    public Network(String fileName) {

        this.nodes = new HashMap<>();
        this.messagesToDeliver = new HashMap<>();
        this.roundActions = new HashMap<>();
        this.failures = new ArrayList<>();

        try {
            this.parseFile(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.logger = Logger.getInstance();

        this.startNetwork();
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

                        this.failures.add(node);

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

    // Adds node to the hash-map if node doesn't exist, and returns it. Otherwise just returns it.
    private Node addOrGetNodeWithId(Integer id){

        Node node;
        if (!this.nodes.containsKey(id)){
            this.nodes.put(id, new Node(id));
            node = this.nodes.get(id);
        }else{
            node = this.nodes.get(id);
        }

        return node;
    }

    public synchronized void addMessage(int id, String message) {

        this.messagesToDeliver.put(id, message);
    }

    // Starts all nodes, executes actions and failures, and manages the rounds.
    public synchronized void startNetwork(){

        // Fire up all the nodes
        for (Node node : this.nodes.values()){
            node.start();
        }

        while (true) {

            this.round++;
            System.out.println(String.format("\n-- Round %d starting", this.round));

            // Check for stopping conditions
            if (this.messagesToDeliver.size() == 0 && this.roundActions.size() == 0 && this.allNodesFinished()){

                // If there are no failures, then exit. Otherwise, deploy one failure at a time.
                if (this.failures.size() == 0){

                    this.stopAllNodes();
                    this.logger.closeWriter();
                    break;
                }else{

                    // Make a new node fail
                    Node failingNode = this.failures.get(0);
                    this.failures.remove(failingNode);

                    System.out.println(String.format("Node %d FAILED", failingNode.getNodeId()));

                    // Inform all neighbours about the failure
                    for (Node neighbour : failingNode.getNeighbours()){
                        neighbour.receiveMessage(MessageCreator.createFailMessage(failingNode.getNodeId()));
                    }

                    // Kill failed node's thread
                    failingNode.setActive(false);
                }
            }

            // Check if there's any action to take in this round
            this.doActions(this.round);

            // Collect and deliver messages that need to be delivered in this round
            this.deliverMessages();

            // Simulate one round by sleeping
            try {
                Thread.sleep(this.period);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // At each round, the network delivers all the messages that it has collected from the nodes.
    // The network must ensure that a node can send only to its neighbours, one message per round per neighbour.
    public synchronized void deliverMessages() {

        // Collect messages from each node
        for (Map.Entry<Integer, Node> entry : this.nodes.entrySet()) {
            Node node = entry.getValue();
            if (node.outgoingMessages.size() != 0){
                this.addMessage(node.getNodeId(), node.outgoingMessages.get(0));
            }
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

        // Receive the messages that were just sent
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
    }

    private void stopAllNodes(){
        for (Node node : this.nodes.values()){
            node.setActive(false);
        }
    }

    // Checks if all nodes are finished
    private boolean allNodesFinished(){
        boolean finished = true;
        for (Node node : this.nodes.values()){
            finished &= node.finished;
        }

        return finished;
    }

    // Executes all actions in a specific round
    private void doActions(int round){
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
    }

    public static void main(String args[]) throws IOException, InterruptedException {

        String fileName = args[0];
        Network network = new Network(fileName);
    }
}
