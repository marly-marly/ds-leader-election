import java.util.ArrayList;


// Action class, currently only used for storing elections
public class Action {

    // Type of the action. E.g. ELECT
    private String type;

    // List of nodes taking part in the specific action
    private ArrayList<Node> nodes;

    public Action(String type){
        this.type = type;
        this.nodes = new ArrayList<>();
    }

    public void addNode(Node node){
        nodes.add(node);
    }

    public String getType() {
        return type;
    }

    public ArrayList<Node> getNodes() {
        return nodes;
    }
}
