import java.util.HashMap;

import ec.util.MersenneTwisterFast;
import sim.engine.*;
import sim.field.network.*;
import sim.util.Double2D;
import sim.util.Bag;

public class Agent implements Steppable{

    public static final MersenneTwisterFast EC = new MersenneTwisterFast();
    public static final double LOC_SPREAD = 50.0;
    public static final int RETRY_DT = 10;

    public static Bag AllAgents = new Bag();
    public static final int NEIGH_RADIUS = 30;

    public int id;
    public Resource prodResource;
    public Double2D position;
    public HashMap<Resource, Double> resourceAmount = new HashMap<>();
    public HashMap<Resource, Double> resourceImpression = new HashMap<>();

    public HashMap<Resource, TentativeStep> isDyingFrom = new HashMap<>();
    public HashMap<Resource, Boolean> isGonnaTrade = new HashMap<>();
    public Bag neighbors;

    public void clear(){
        AllAgents.clear();
        resourceAmount.clear();
        resourceImpression.clear();
        isDyingFrom.clear();
        neighbors.clear();
    }

    public Agent(Double2D position){
        this.position = position;
        AllAgents.add(this);
    }

    public Agent(){
        this(new Double2D((EC.nextDouble())*LOC_SPREAD+Model.WIDTH/4.,
                          (EC.nextDouble())*LOC_SPREAD+Model.WIDTH/4.));
    }

    public void assignProduction(Resource r){
        r.productors.add(this);
        prodResource = r;
        this.id = r.productors.size();
    }
    public String toString(){
        return this.prodResource.name + " " + this.id;
    }

    public void updateNeighbors(Model model){
        this.neighbors = this.getNeighbors(model);
    }


    public boolean isDying(){
        return !isDyingFrom.isEmpty();
    }

    public static double getAllAgentsResourceAmount(Resource r){
        double total = 0;
        for(Object x: AllAgents){
            total += ((Agent) x).resourceAmount.getOrDefault(r, 0.);
        }
        return total;
    }

    public HashMap<Resource, Double> getResourceAmount() {
        return resourceAmount;
    }

    public HashMap<Resource, Double> getResourceImpression() {
        return resourceImpression;
    }

    public double getR(Resource r){
        return this.resourceAmount.getOrDefault(r, 0.);
    }

    public double getRi(Resource r){
        return this.resourceImpression.getOrDefault(r, 0.);
    }

    public Bag getNeighbors(Model model){
        Bag edges = model.village.getEdgesOut(this);
        Bag neighbors = new Bag();
        for(Object x: edges){
            Agent n = (Agent) ((Edge) x).getOtherNode(this);
            neighbors.add(n);
        }

        return neighbors;
    }

    public double getDisparity(Resource r){
        double disparity = 0;
        for(Object x: neighbors){
            Agent n = (Agent) x;
            disparity += Math.abs(n.getR(r) - this.getR(r));
        }
        return disparity;
    }

    public double getDemand(Resource r){
        double demand = 0;
        for(Object x: neighbors){
            Agent n = (Agent) x;
            demand += Math.abs(n.getRi(r) - this.getRi(r));
        }
        return demand;
    }

    public void addResource(Model model, Resource r, double v){
        double old = this.resourceAmount.getOrDefault(r, 0.);
        r.totalAmount -= old;
        double new_= Math.min(Math.max(old+v, 0), 100000);
        this.resourceAmount.put(r, new_);
        r.totalAmount += new_;
        
        model.totals.put(r, r.totalAmount);
    }

    public void step(SimState state){
        Model model = (Model) state;
        for(Object x: Resource.AllPresentResources){
            Resource r = (Resource) x;

            // Resource growth/decay
            if(r != this.prodResource)
                this.addResource(model, r, -1.*Model.DT/r.t_crit);
            else
                this.addResource(model, r, Model.DT/r.t_prod);

            if(this.resourceAmount.getOrDefault(r, 0.) == 0){
                TentativeStep killer = null;

                // If resource is basic, schedule possible death
                if(r.isBasic() && (this.isDyingFrom.getOrDefault(r, null) == null)){
                    r = (BasicResource) r;
                    killer = new TentativeStep(new Ev_Death(this, r));
                    model.schedule.scheduleOnceIn(r.t_death, killer);
                    isDyingFrom.put(r, killer);
                }
                // Schedule trade
                if(!isGonnaTrade.getOrDefault(r, false)){
                    model.schedule.scheduleOnce(new Ev_Trade(this, r));
                    isGonnaTrade.put(r, true);
                }

            }
        }
        model.calc();
        
    }

}
