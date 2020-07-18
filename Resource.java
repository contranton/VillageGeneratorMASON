import ec.util.MersenneTwisterFast;
import sim.util.Bag;

public abstract class Resource{

    public static final MersenneTwisterFast EC = new MersenneTwisterFast();

    public String name;
    public int t_crit;
    public int t_prod;
    public int t_death;
    public Bag productors;
    public boolean present;
    public double totalAmount;
    
    public double scarcity;
    public double disparity;
    public double demand;
    public double production;

    public static Bag AllResources = new Bag();
    public static Bag AllPresentResources = new Bag();

    public static void clear(){
        AllResources.clear();
        AllPresentResources.clear();
        LuxResource.clear_();
        BasicResource.clear_();
    }

    public String toString(){
        return this.name;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public Resource(String name, boolean present){
        this.name = name;
        this.present = present;

        if(this.present){
            AllPresentResources.add(this);
        }
        AllResources.add(this);

        productors = new Bag();

        // TODO: Impact of these ranges?
        //t_crit = EC.nextInt(100)+10;
        //t_prod = EC.nextInt(200)+10;
        t_crit = 600;
        t_prod = 100;
    }

    public Resource(String name){
        this(name, false);
    }

    public void makePresent(){
        if(!this.present){
            AllPresentResources.add(this);
        }
        this.present = true;
    }

    public boolean isBasic(){
        return false;
    }

    public boolean isLux(){
        return false;
    }

    public double getScarcity() {
        return scarcity;
    }

    public void setScarcity(double scarcity) {
        this.scarcity = scarcity;
    }

    public double getDisparity() {
        return disparity;
    }

    public void setDisparity(double disparity) {
        this.disparity = disparity;
    }

    public double getDemand() {
        return demand;
    }

    public void setDemand(double demand) {
        this.demand = demand;
    }

    public double getProduction() {
        return production;
    }

    public void setProduction(double production) {
        this.production = production;
    }

}