import java.util.HashMap;

import ec.util.MersenneTwisterFast;
import sim.engine.*;
import sim.field.continuous.*;
import sim.field.network.*;
import sim.util.Bag;
import sim.util.Double2D;

public class Model extends SimState{

    public static final MersenneTwisterFast EC = new MersenneTwisterFast();

    private static final int STEPS = 1000;
    public static final int WIDTH = 100;
    public static final double DT = 1;
    public static final double CONTROL_DT = 500;

    public HashMap<Agent, Stoppable> scheduleStoppers = new HashMap<>();

    public HashMap<Resource, Double> totals = new HashMap<>();
    public HashMap<Resource, Double> scarcity = new HashMap<>();
    public HashMap<Resource, Double> disparity = new HashMap<>();
    public HashMap<Resource, Double> demand = new HashMap<>();
    public HashMap<Resource, Double> production = new HashMap<>();
    public HashMap<Resource, Double2D> scarcityCentroid = new HashMap<>();
    public HashMap<Resource, Double2D> disparityCentroid = new HashMap<>();
    public HashMap<Resource, Double2D> demandCentroid = new HashMap<>();
    public HashMap<Resource, Double2D> productionCentroid = new HashMap<>();

    public Continuous2D space = new Continuous2D(1.0, WIDTH, WIDTH);
    public Continuous2D ctrds_s = new Continuous2D(1.0, WIDTH, WIDTH);
    public Continuous2D ctrds_u = new Continuous2D(1.0, WIDTH, WIDTH);
    public Continuous2D ctrds_d = new Continuous2D(1.0, WIDTH, WIDTH);
    public Continuous2D ctrds_p = new Continuous2D(1.0, WIDTH, WIDTH);

    public Network village = new Network(false); // Undirected

    public Model(long seed){
        super(seed);
    }

    public void clear(){
        super.start();
        // Clear all data structures
        scarcity.clear();
        disparity.clear();
        demand.clear();
        space.clear();
        ctrds_s.clear();
        ctrds_u.clear();
        ctrds_d.clear();
        ctrds_p.clear();
        village.clear();
        Resource.clear();
    }

    public Agent addAgent(Resource r, Double2D position){
        Agent agent;
        if(position != null){
            agent = new Agent(position);
        }else{
            agent = new Agent();
        }
        agent.assignProduction(r);

        space.setObjectLocation(agent, agent.position);
        village.addNode(agent);

        // Schedule continuous dynamics
        Stoppable s;
        s = schedule.scheduleRepeating(schedule.getTime() + 1, agent, DT); // Need to schedule 1 unit in the future for some reason
        scheduleStoppers.put(agent, s);

        return agent;
    }

    public Agent addAgent(Resource r){
        return addAgent(r, null);
    }

    public void makeEdges(Agent a){
        Bag neighs = space.getNeighborsWithinDistance(a.position, Agent.NEIGH_RADIUS);
        for(Object y: neighs){
            village.addEdge(new Edge(a, y, null));
        }
    }

    public void start(){
        clear();
        
        new BasicResource("Water", true);
        new BasicResource("Food", true);
        //new BasicResource("Hygiene", true);
        new LuxResource("Clothes", true);
        new LuxResource("Jewelry", true);
        new LuxResource("Faith", true);
        new LuxResource("Watches");

        for(Object x: Resource.AllPresentResources){
            Resource r = (Resource) x;
            for(int i = 0; i < 2; i++){
                addAgent(r);
            }
        }
        for(Object x: Agent.AllAgents){
            Agent a = (Agent) x;
            makeEdges(a);
        }

        // Schedule timer for controller
        schedule.scheduleRepeating(CONTROL_DT, new ModelController(), CONTROL_DT);


        calc();
        System.out.println("System set up");
    }

    public void calc(){
        calculateScarcity();
        calculateDisparity();
        calculateDemand();
        calculateProduction();
    }

    public static void main(String[] args) {
        
        Model state = new Model(System.currentTimeMillis());
        state.start();

        do{
            if(!state.schedule.step(state)) 
                break;        
        }while(state.schedule.getSteps() < STEPS);

    }

    public Bag getAllEdges(){
        Bag seenEdges = new Bag();
        for(Object x: village.getAllNodes()){
            Bag edges = village.getEdgesOut(x);
            for(Object y: edges){
                if(seenEdges.contains(y)) continue;
                seenEdges.add(y);
            }
        }
        return seenEdges;
    }

    public void calculateScarcity(){
        scarcityCentroid.clear();
        scarcity.clear();

        for(Object x: BasicResource.AllBasicResources){
            Resource r = (Resource) x;
            Double2D r_centroid = new Double2D();
            double tot_scar = 0;
            for(Object y: Agent.AllAgents){
                Agent a = (Agent) y;
                double scar = Math.max(1./(1. + a.getR(r)), 0.);
                //double scar = Math.max(1. - a.getR(r), 0.);
                tot_scar += scar;
                r_centroid = r_centroid.add(a.position.multiply(scar));
            }
            //r_centroid = r_centroid.multiply(1./getScarcity().getOrDefault(r, 0.));
            r_centroid = r_centroid.multiply(1./tot_scar);
            ctrds_s.setObjectLocation(r, r_centroid);
            
            tot_scar /= Agent.AllAgents.size();
            r.scarcity = tot_scar;
            scarcity.put(r, tot_scar);
            scarcityCentroid.put(r, r_centroid);
        }

    }

    public void calculateDisparity(){
        disparityCentroid.clear();
        disparity.clear();

        for(Object x: Resource.AllResources){
            Resource r = (Resource) x;
            Double2D r_centroid = new Double2D();
            double tot_disp = 0.;
            
            int i = 0;
            for(Object y: getAllEdges()){
                Edge e = (Edge) y;
                Agent a = (Agent) e.from();
                Agent b = (Agent) e.to();
            
                double disparity = Math.abs(a.getR(r) - b.getR(r));
                tot_disp += disparity;

                Double2D p1 = ((Agent)e.from()).position;
                Double2D p2 = ((Agent)e.to()).position;

                Double2D middle = p1.add(p2).multiply(0.5);                
                r_centroid = r_centroid.add(middle.multiply(disparity));
                i++;
            }

            r_centroid = r_centroid.multiply(1./tot_disp);

            tot_disp /= i;
            ctrds_u.setObjectLocation(r, r_centroid);
            disparityCentroid.put(r, r_centroid);
            disparity.put(r, tot_disp);
            r.disparity = tot_disp;
        }

    }

    public void calculateDemand(){
        demandCentroid.clear();
        for(Object x: LuxResource.AllLuxResources){
            Resource r = (Resource) x;
            Double2D r_centroid = new Double2D();
            double tot_demand = 0.;
            
            int i = 0;
            for(Object y: getAllEdges()){
                Edge e = (Edge) y;
                Agent a = (Agent) e.from();
                Agent b = (Agent) e.to();
            
                double demand = Math.abs(a.getRi(r) - b.getRi(r));
                tot_demand += demand;

                Double2D p1 = ((Agent)e.from()).position;
                Double2D p2 = ((Agent)e.to()).position;

                Double2D middle = p1.add(p2).multiply(0.5);                
                r_centroid = r_centroid.add(middle.multiply(demand));
                i++;
            }
            r_centroid = r_centroid.multiply(1./tot_demand);

            tot_demand /= i;
            ctrds_d.setObjectLocation(r, r_centroid);
            demandCentroid.put(r, r_centroid);
            demand.put(r, tot_demand);
            r.demand = tot_demand;
        }

    }

    public void calculateProduction(){
        productionCentroid.clear();
        production.clear();

        for(Object x: Resource.AllResources){
            Resource r = (Resource) x;
            Double2D r_centroid = new Double2D();
            double tot_prod = 0.;
            
            for(Object y: r.productors){
                Agent a = (Agent) y;

                double prod = a.getR(a.prodResource);
                tot_prod += prod;

                r_centroid = r_centroid.add(a.position.multiply(prod));
            }

            r_centroid = r_centroid.multiply(1./tot_prod);

            ctrds_p.setObjectLocation(r, r_centroid);
            productionCentroid.put(r, r_centroid);
            production.put(r, tot_prod);
            r.production = tot_prod;
        }
    }

    //////////////////////////////////////////////////
    ////// GETTERS/SETTERS////////////////////////////
    //////////////////////////////////////////////////

    public HashMap<Resource, Double> getTotals() {
        return totals;
    }

    public void setTotals(HashMap<Resource, Double> totals) {
        this.totals = totals;
    }

    public HashMap<Resource, Double> getScarcity() {
        return scarcity;
    }

    public void setScarcity(HashMap<Resource, Double> scarcity) {
        this.scarcity = scarcity;
    }

    public HashMap<Resource, Double> getDisparity() {
        return disparity;
    }

    public void setDisparity(HashMap<Resource, Double> disparity) {
        this.disparity = disparity;
    }

    public HashMap<Resource, Double> getDemand() {
        return demand;
    }

    public void setDemand(HashMap<Resource, Double> demand) {
        this.demand = demand;
    }

    public HashMap<Resource, Double> getProduction() {
        return production;
    }

    public void setProduction(HashMap<Resource, Double> production) {
        this.production = production;
    }

    public HashMap<Resource, Double2D> getScarcityCentroid() {
        return scarcityCentroid;
    }

    public void setScarcityCentroid(HashMap<Resource, Double2D> scarcityCentroid) {
        this.scarcityCentroid = scarcityCentroid;
    }

    public HashMap<Resource, Double2D> getDisparityCentroid() {
        return disparityCentroid;
    }

    public void setDisparityCentroid(HashMap<Resource, Double2D> disparityCentroid) {
        this.disparityCentroid = disparityCentroid;
    }

    public HashMap<Resource, Double2D> getDemandCentroid() {
        return demandCentroid;
    }

    public void setDemandCentroid(HashMap<Resource, Double2D> demandCentroid) {
        this.demandCentroid = demandCentroid;
    }

    public HashMap<Resource, Double2D> getProductionCentroid() {
        return productionCentroid;
    }

    public void setProductionCentroid(HashMap<Resource, Double2D> productionCentroid) {
        this.productionCentroid = productionCentroid;
    }

}


class Ev_Trade implements Steppable{

    public Agent agent;
    public Resource res;

    public Ev_Trade(Agent agent, Resource res){
        this.agent = agent;
        this.res = res;
    }

    public void step(SimState state){
        Model model = (Model) state;

        // Find a neighbor and try to trade
        // Bag neighbors = this.agent.getNeighbors(model);
        Bag edges = model.village.getEdgesOut(this.agent);
        for(Object x: edges){
            Edge e = (Edge) x;
            Agent n = (Agent) (e.getOtherNode(this));
            if(n.prodResource == this.res && n.resourceAmount.get(n.prodResource) > 2){
                // Unschedule death
                TentativeStep killer = this.agent.isDyingFrom.getOrDefault(this.res, null);
                if(killer != null){
                    killer.stop();
                }
                this.agent.isDyingFrom.remove(this.res);

                // Trade
                this.agent.addResource(model, this.res, 1);
                n.addResource(model, this.res, -1);
                this.agent.isGonnaTrade.put(this.res, false);
                
                return;
            }
        }

        // If failed, reschedule to try later
        model.schedule.scheduleOnceIn(Agent.RETRY_DT, this);
    }

}

class Ev_Death implements Steppable{
    public Agent agent;
    public Resource res;
    public Ev_Death(Agent agent, Resource res){
        this.agent = agent;
        this.res = res;
    }

    public void step(SimState state){
        Model model = (Model) state;
        System.out.println("Agent " + this.agent + " died");
        model.space.remove(this.agent);
        model.village.removeNode(this.agent);
        this.agent.prodResource.productors.remove(this.agent);

        // Stop the agent's repeated schedule
        Stoppable s = model.scheduleStoppers.getOrDefault(this.agent, null);
        if(s != null){
            s.stop();
        }else{
            System.out.println("Failed to stop agent " + this.agent);
        }
    }
}

class ModelController implements Steppable{

    public void step(SimState state){
        Model model = (Model) state;
        for(Object x: Resource.AllPresentResources){
            Resource r = (Resource) x;
            if(r.isBasic() && model.scarcity.get(r) > 0.5){
                Agent a = model.addAgent(r, model.scarcityCentroid.get(r));
                model.makeEdges(a);
            }else if(model.disparity.get(r) > 10){
                //model.addAgent(r, model.disparityCentroid.get(r));
                System.out.println("Was gonna fix disparity");
            }
        }
    }
}