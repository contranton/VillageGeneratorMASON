import sim.util.Bag;

public class LuxResource extends Resource{

    public static Bag AllLuxResources = new Bag();
    public static Bag AllPresentLuxResources = new Bag();

    public LuxResource(String name, boolean present){
        super(name, present);
        AllLuxResources.add(this);
        if(present)
        AllPresentLuxResources.add(this);
    }

    public LuxResource(String name){
        super(name, false);
    }

    public static void clear_(){
        AllLuxResources.clear();
        AllPresentLuxResources.clear();
    }

    public boolean isLux(){
        return true;
    }

}