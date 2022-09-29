package solution;

import org.graphstream.graph.Node;

import java.util.*;

public class PartialSolution{

    private LinkedHashMap<Node, NodeProperties> nodeStates;
    private List<Node> nodesPath;
    private double costFunction;
    private int idleTime;



    /**
     * Constructor for any subsequent partial solution on the solution tree except the root of the solution tree.
     * @param prevPartial previous PartialSolution immediately prior to the current PartialSolution
     * @param graph diGraph of the Original Tasks Graph
     * @param currentNode The current Task(node) that is going to be scheduled
     * @param processorId Which Processor is the current Task(node) is going to be scheduled on
     */
    public PartialSolution(PartialSolution prevPartial, Digraph graph, Node currentNode, int processorId,int numOfProcessor) {

        initializeFromPrevPartialSolution(prevPartial);
        scheduleTask(graph, currentNode, processorId);
        updateCurrentPartialSolutionStatus(graph, currentNode, processorId);
        //calculate Cost Function for this PartialSolution
        costFunction = calculateCostFunction(graph,currentNode,processorId,numOfProcessor);
    }

    /**
     * Constructor for the root of the solution tree.
     * @param graph diGraph of the Original Tasks Graph
     */
    public PartialSolution(Digraph graph) {
        initializeRootPartialSolution(graph);
    }

    /**
     * @return `List<Node>` a list of next available Tasks(nodes) that can be scheduled immediately.
     */
    public List<Node> getAvailableNextNodes() {
        List<Node> availableNextNodes = new ArrayList<>();
        for (Node node : nodeStates.keySet()) {
            if (nodeStates.get(node).getInDegree() == 0) {
                availableNextNodes.add(node);
            }
        }
        return availableNextNodes;
    }

    public List<Node> getNodesPath() {
        return nodesPath;
    }

    public int getIdleTime() {
        return idleTime;
    }
    public LinkedHashMap<Node, NodeProperties> getNodeStates() {
        return nodeStates;
    }

    public double getCostFunction() {
        return costFunction;
    }


    /**
     * @return a String representation of the current partial solution status on the solution tree.
     *           i.e.  "(nodeId,processorId,startTime);"
     */
    public String getInfo(){

        StringBuffer sb = new StringBuffer();
        for(int i=0;i<nodesPath.size();i++){
            Node node = nodesPath.get(i);
            sb.append("(" + node.getId()+",");
            sb.append(nodeStates.get(node).getProcessorId()+",");
            sb.append(nodeStates.get(node).getStartingTime()+")");
            sb.append(";");
        }
        sb.append(" Finishing Time: ");
        sb.append(getEndScheduleTime());

        return sb.toString();
    }

    /**
     * initialize fields from the previous partial solution.
     * @param prevPartial previous PartialSolution immediately prior to the current PartialSolution
     */
    private void initializeFromPrevPartialSolution(PartialSolution prevPartial){
        // copy nodePath from the previous solution
        this.nodesPath = new ArrayList<>();
        nodesPath.addAll(prevPartial.getNodesPath());

        // copy nodeStates from the previous solution
        this.nodeStates = new LinkedHashMap<>();
        for (Node node : prevPartial.getNodeStates().keySet()) {
            NodeProperties nodeProperties = new NodeProperties();

            NodeProperties prevNodeProperties = prevPartial.getNodeStates().get(node);
            nodeProperties.setInDegree(prevNodeProperties.getInDegree());
            nodeProperties.setProcessorId(prevNodeProperties.getProcessorId());
            nodeProperties.setStartingTime(prevNodeProperties.getStartingTime());

            nodeStates.put(node, nodeProperties);
        }

        idleTime = prevPartial.getIdleTime();
    }

    /**
     * initialize fields for root partial solution of the solution tree.
     * @param graph diGraph of the Original Tasks Graph
     */
    private void initializeRootPartialSolution(Digraph graph){
        this.nodesPath = new ArrayList<>();
        this.nodeStates = new LinkedHashMap<>();
        idleTime = 0;

        // initialize status for all nodes.
        for (Node node : graph.getAllNodes()) {
            NodeProperties nodeProperties = new NodeProperties();
            nodeProperties.setProcessorId(0);//processorId
            nodeProperties.setInDegree(node.getInDegree());// inDegree
            nodeProperties.setStartingTime(0);//startingTime
            nodeStates.put(node, nodeProperties);
        }

    }


    /**
     * logic for schedule task according to supplied task(Node) and processorId.
     * @param graph diGraph of the Original Tasks Graph
     * @param currentNode The current Task(node) that is going to be scheduled
     * @param processorId Which Processor is the current Task(node) is going to be scheduled on
     */
    private void scheduleTask(Digraph graph, Node currentNode, int processorId){
            // set the starting time of the current node.
            nodeStates.get(currentNode).setStartingTime(calculateStartingTime(graph, currentNode, processorId));
    }

    private int calculateStartingTime(Digraph graph, Node currentNode, int processorId){
        // make sure it's not the root partial solution of the solution tree before going to the next step.
        if (!nodesPath.isEmpty()) {
            // find the latest finishing time of previously direct parent(s) of the current node(Task) iteratively.
            int startTime = 0;
            for (Node node : nodesPath) {
                if (node.hasEdgeToward(currentNode)) {
                    // if the current node is on the same processor with the selected its parent node.
                    if (nodeStates.get(node).getProcessorId() == processorId) {
                        // find the finishing time of the parent node of the current node and let it be the starting
                        // time of the current node.
                        startTime = (int) Math.max(startTime, nodeStates.get(node).getStartingTime() + graph.getNodeWeight(node.getId()));
                        // if the current node is on the different processor with the selected its parent node.
                    } else {
                        // find the finishing time of the parent node of the current node and add communication cost,
                        // then let it be the starting time of the current node.
                        int communicationCost = graph.getEdgeWeight(node.getId(), currentNode.getId()).intValue();
                        startTime = (int) Math.max(startTime, nodeStates.get(node).getStartingTime() + graph.getNodeWeight(node.getId()) + communicationCost);
                    }
                }
            }
            // check if the current scheduled processor that may have any Tasks may span over the finishing time of the parents of the
            // current node being calculated above. Select the max value between the two values as the starting time of the current node.
            int finishTimePrev = findLastFinishTime(graph,processorId);
            return Math.max(startTime, finishTimePrev);
        }
        return 0;
    }

    private int futureMinBottomLevel(Digraph graph,int numOfProcessor){
        int MinStartingTime = Integer.MAX_VALUE;
        int bottomLevel = 0;
        for(Node node : getAvailableNextNodes()){
            for(int p = 1; p <= numOfProcessor; p++){
                int startingTime = calculateStartingTime(graph, node, p);
                if(startingTime < MinStartingTime){
                    MinStartingTime = startingTime;
                    bottomLevel = (int) (graph.getBottomLevel(node) + startingTime);
                }
            }
        }

        return bottomLevel;
    }

    /**
     * Get the last finish time of this processor
     * @param processorId processor Id
     * @return last finish time of this processor
     */
    private int findLastFinishTime(Digraph graph,int processorId){

        int lastTime = 0;
        for(Node node:nodesPath){
            if(nodeStates.get(node).getProcessorId()==processorId){
                lastTime = (int)Math.max(lastTime,nodeStates.get(node).getStartingTime()+ graph.getNodeWeight(node.getId()));
            }
        }

        return lastTime;
    }

    /**
     * updating the current partial solution status and save it into fields.
     *
     * @param graph diGraph of the Original Tasks Graph
     * @param currentNode The current Task(node) that is going to be scheduled
     * @param processorId Which Processor is the current Task(node) is going to be scheduled on
     */
    private void updateCurrentPartialSolutionStatus(Digraph graph, Node currentNode, int processorId){
        // add this node to the solution path
        nodesPath.add(currentNode);
        // set current node's processor number.
        nodeStates.get(currentNode).setProcessorId(processorId);
        // set current node's indegree to -1 (all done.)
        nodeStates.get(currentNode).setInDegree(-1);

        // decrease all direct children indegree by 1 (effectively get rid of the in edges from the current node to
        // all its children nodes).
        List<Node> childrenNodes = graph.getAllChildrenNode(currentNode);
        for (Node childNode : childrenNodes) {
            int currentInDegree = nodeStates.get(childNode).getInDegree();
            nodeStates.get(childNode).setInDegree(currentInDegree - 1);
        }
    }

    public double calculateCostFunction(Digraph graph, Node currentNode, int processorId, int numOfProcessor){

        // find idle time ----------------------------------------------------

        idleTime += nodeStates.get(currentNode).getStartingTime() - findLastFinishTime(graph,processorId);

        double bottomLevel = 0;
        for(Node node: nodesPath) {
            bottomLevel = Math.max(bottomLevel, nodeStates.get(node).getStartingTime()+graph.getBottomLevel(node));
        }

        double loadBalance = (graph.getAllNodeWeight()+ idleTime) / (double)numOfProcessor;

        return Math.max(Math.max(bottomLevel,loadBalance),futureMinBottomLevel(graph,numOfProcessor));
    }


    private int getEndScheduleTime(){
        int finishingTime = 0;
        for (Node node: nodesPath){
            int weight = (int) Double.parseDouble(node.getGraph().getNode(node.getId()).getAttribute("Weight").toString());
            int startingTime = nodeStates.get(node).getStartingTime();
            finishingTime = Math.max(finishingTime,weight+startingTime);
        }
        return finishingTime;
    }

}
