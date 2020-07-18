import sim.util.Bag;

public class BasicResource extends Resource{

    // Under our assumptions, the following two bags are equivalent
    // We're keeping both for consistency's sake
    public static Bag AllBasicResources = new Bag();
    public static Bag AllPresentBasicResources = new Bag();

    public BasicResource(String name, boolean present){
        super(name, present);
        AllBasicResources.add(this);
        if(present)
        AllPresentBasicResources.add(this);

        t_death = 1000;
    }

    public static void clear_(){
        AllBasicResources.clear();
        AllPresentBasicResources.clear();
    }

    public boolean isBasic(){
        return true;
    }

}