import java.util.*;

// Class to represent a node. Each node must run on its own thread.
public class Node extends Thread {

    private int id;
    private boolean participant = false;
    private boolean leader = false;

    private boolean active = true;

    public boolean finished;

    private Logger logger;

    private Node nextNode;

    // Neighbouring nodes
    private List<Node> neighbours;

    // Queue for the incoming messages
    public final List<String> incomingMessages;

    // Queue for the outgoing messages
    public final List<String> outgoingMessages;

    public Node(int id){

        this.id = id;

        this.neighbours = new ArrayList<>();
        this.incomingMessages = Collections.synchronizedList(new ArrayList<String>());
        this.outgoingMessages = new ArrayList<>();

        this.logger = Logger.getInstance();
    }

    public int getNodeId() {
        return this.id;
    }

    public boolean isNodeLeader() {
        return this.leader;
    }

    public List<Node> getNeighbours() {
        return this.neighbours;
    }

    public void addNeighbour(Node node) {
        this.neighbours.add(node);
    }

    public Node getNextNode() {
        return nextNode;
    }

    public void setNextNode(Node nextNode) {
        this.nextNode = nextNode;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void run(){

        while(this.active){

            synchronized (this.incomingMessages){

                Iterator iterator = this.incomingMessages.iterator();
                while (iterator.hasNext()){

                    String message = (String) iterator.next();
                    String[] parts = message.split("\\s");
                    String messageType = MessageCreator.getMessageType(parts);

                    switch (messageType){
                        case MessageCreator.ELECTION_TAG:

                            int electionInitializerId = MessageCreator.getInitializerIdFromElectMessage(parts);
                            int incomingId = MessageCreator.getMaximumIdFromElectMessage(parts);

                            this.logger.log(String.format("Node %d received election message with id %d. Initailizer: %d", this.id, incomingId, electionInitializerId));

                            if (!this.participant){

                                // Send the larger ID
                                if (this.id > incomingId){
                                    this.outgoingMessages.add(MessageCreator.createElectMessage(electionInitializerId, this.id));
                                }else{
                                    this.outgoingMessages.add(message);
                                }

                                this.participant = true;
                            }else{

                                // If we find out that we are the leader, signal Leader message
                                if (incomingId == this.id){
                                    this.participant = false;
                                    this.leader = true;
                                    this.outgoingMessages.add(MessageCreator.createLeaderMessage(electionInitializerId, this.id));

                                    this.logger.log(String.format("Node %d is ELECTED as leader. Initailizer: %d", this.id, electionInitializerId));
                                }

                                // If incoming ID is larger than ours, then send it
                                if (incomingId > this.id){
                                    this.outgoingMessages.add(message);
                                }
                            }

                            break;

                        case MessageCreator.LEADER_TAG:

                            int initializerId = MessageCreator.getInitializerIdFromLeaderMessage(parts);
                            int leaderId = MessageCreator.getLeaderIdFromLeaderMessage(parts);

                            this.logger.log(String.format("Node %d received leader message with id %d. Initailizer: %d", this.id, leaderId, initializerId));

                            // If the leader message hasn't made a full round yet, forward it
                            if(this.id != leaderId){
                                this.participant = false;
                                this.outgoingMessages.add(message);
                            }

                            break;
                    }

                    iterator.remove();
                }
            }

            this.finished = this.incomingMessages.size() == 0 && this.outgoingMessages.size() == 0;
        }

        System.out.println((String.format("Node %d finished!", this.id)));
    }

    // Method that implements the reception of an incoming message by a node
    public void receiveMessage(String message) {

        this.finished = false;
        synchronized (this.incomingMessages){
            this.incomingMessages.add(message);
        }
    }

    public void sendMessage(String message) {
		/*
		Method that implements the sending of a message by a node. 
		The message must be delivered to its recipients through the network.
		This method need only implement the logic of the network receiving an outgoing message from a node.
		The remainder of the logic will be implemented in the network class.
		*/
        this.outgoingMessages.remove(message);

        String[] parts = message.split("\\s");
        String messageType = MessageCreator.getMessageType(parts);
        if (messageType.equals(MessageCreator.ELECTION_TAG)){
            this.participant = true;
        }
    }

    public void startLeaderElection(){
        this.outgoingMessages.add(String.format("%s %d %d", MessageCreator.ELECTION_TAG, this.id, this.id));
    }
}
