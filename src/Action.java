import java.util.ArrayList;

public class Action {

    private String type;

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
