import sun.plugin2.message.Message;

import java.util.*;

// Class to represent a node. Each node must run on its own thread.
public class Node extends Thread {

    private int id;
    private boolean participant = false;
    private boolean leader = false;

    private Node nextNode;

    private Node previousNode;

    // Neighbouring nodes
    private List<Node> neighbours;

    // Queue for the incoming messages
    public List<String> incomingMessages;

    public Node(int id){

        this.id = id;

        this.neighbours = new ArrayList<>();
        this.incomingMessages = new ArrayList<>();
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

    public Node getPreviousNode() {
        return previousNode;
    }

    public void setPreviousNode(Node previousNode) {
        this.previousNode = previousNode;
    }

    // Method that implements the reception of an incoming message by a node
    public void receiveMessage(String message) {

        String[] parts = message.split("\\s");
        String messageType = parts[0];

        switch (messageType){
            case MessageCreator.ELECTION_TAG:

                int electionInitializerId = Integer.valueOf(parts[1]);
                int incomingId = Integer.valueOf(parts[2]);

                if (!this.participant){

                    // Send the larger ID
                    if (this.id > incomingId){
                        this.incomingMessages.add(MessageCreator.createElectMessage(electionInitializerId, this.id));
                    }else{
                        this.incomingMessages.add(message);
                    }

                    this.participant = true;
                }else{

                    // If we find out that we are the leader, signal Leader message
                    if (incomingId == this.id){
                        this.participant = false;
                        this.leader = true;
                        this.incomingMessages.add(MessageCreator.createLeaderMessage(electionInitializerId, this.id));

                        System.out.println(String.format("Leader with id %d elected", this.id));
                        return;
                    }

                    // If incoming ID is larger than ours, then send it
                    if (incomingId > this.id){
                        this.incomingMessages.add(message);
                    }
                }

                break;

            case MessageCreator.LEADER_TAG:

                int leaderId = MessageCreator.getLeaderIdFromLeaderMessage(message);

                // If the leader message hasn't made a full round yet, forward it
                if(this.id != leaderId){
                    this.participant = false;
                    this.incomingMessages.add(message);
                }

                break;
        }
    }

    public void sendMessage(String message) {
		/*
		Method that implements the sending of a message by a node. 
		The message must be delivered to its recipients through the network.
		This method need only implement the logic of the network receiving an outgoing message from a node.
		The remainder of the logic will be implemented in the network class.
		*/
        this.incomingMessages.remove(message);
    }
}