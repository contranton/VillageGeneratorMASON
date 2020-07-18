import sim.engine.*;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.Inspector;
import sim.portrayal.SimpleInspector;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.network.NetworkPortrayal2D;
import sim.portrayal.network.SimpleEdgePortrayal2D;
import sim.portrayal.network.SpatialNetwork2D;
import sim.portrayal.simple.LabelledPortrayal2D;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.util.Bag;
import sim.util.media.chart.*;

import java.awt.*;
import java.awt.Color;
import javax.swing.*;

import sim.display.*;

public class ModelUI extends GUIState{

    public static final boolean DO_PLOT = true;

    public Display2D display;
    public JFrame displayFrame;
    ContinuousPortrayal2D spacePortrayal = new ContinuousPortrayal2D();
    ContinuousPortrayal2D ctrds_sPortrayal = new ContinuousPortrayal2D();
    ContinuousPortrayal2D ctrds_uPortrayal = new ContinuousPortrayal2D();
    ContinuousPortrayal2D ctrds_dPortrayal = new ContinuousPortrayal2D();
    ContinuousPortrayal2D ctrds_pPortrayal = new ContinuousPortrayal2D();

    NetworkPortrayal2D netPortrayal = new NetworkPortrayal2D();

    public Bag scarcityChartAttributes = new Bag(); //TimeSeriesAttributes
    public TimeSeriesChartGenerator scarcityChart;

    public Bag disparityChartAttributes = new Bag(); //TimeSeriesAttributes
    public TimeSeriesChartGenerator disparityChart;

    public Bag demandChartAttributes = new Bag(); //TimeSeriesAttributes
    public TimeSeriesChartGenerator demandChart;


    public static void main(String[] args) {
        ModelUI vid = new ModelUI();
        Console c = new Console(vid);
        c.setVisible(true);
    }

    public ModelUI(){
        super(new Model(System.currentTimeMillis()));
    }

    public ModelUI(SimState state){
        super(state);
    }

    public static String getName(){
        return "Village generator simulator";
    }

    public void start(){
        super.start();
        setupPortrayals();

        if(DO_PLOT){
            scarcityChart.clearAllSeries();
            demandChart.clearAllSeries();
            disparityChart.clearAllSeries();

            setupCharts();
        }
    }

    public void load(final SimState state){
        super.load(state);
        setupPortrayals();

        if(DO_PLOT){
            setupCharts();
        }
        
    }

    public void setupCharts(){
        int i = 0;
        for(Object x: scarcityChartAttributes){
            TimeSeriesAttributes a = (TimeSeriesAttributes) x;
            final Integer mi = i++;
            ChartUtilities.scheduleSeries(this, a, new sim.util.Valuable(){
                public double doubleValue(){
                    return ((Model)state).scarcity.get(BasicResource.AllBasicResources.get(mi));
                }
            });
        }
        i = 0;
        for(Object x: disparityChartAttributes){
            TimeSeriesAttributes a = (TimeSeriesAttributes) x;
            final Integer mi = i++;
            ChartUtilities.scheduleSeries(this, a, new sim.util.Valuable(){
                public double doubleValue(){
                    return ((Model)state).disparity.get(Resource.AllResources.get(mi));
                }
            });
        }
        i = 0;
        for(Object x: demandChartAttributes){
            TimeSeriesAttributes a = (TimeSeriesAttributes) x;
            final Integer mi = i++;
            ChartUtilities.scheduleSeries(this, a, new sim.util.Valuable(){
                public double doubleValue(){
                    return ((Model)state).demand.get(LuxResource.AllLuxResources.get(mi));
                }
            });
        }
    }

    public void setupPortrayals(){
        Model model = (Model) state;

        // Portrayal for agents
        spacePortrayal.setField(model.space);
        spacePortrayal.setPortrayalForAll(
            new LabelledPortrayal2D(
                new OvalPortrayal2D(){
                    public void draw(Object object, Graphics2D graphics, DrawInfo2D info){
                        Agent agent = (Agent) object;

                        paint = new Color(agent.isDying() ? 255 : 0, 128, 255);

                        super.draw(object, graphics, info);
                    }
                }   
                , 2.0, null, Color.black, false)
        );

        // Portrayal for centroids
        ctrds_sPortrayal.setField(model.ctrds_s);
        ctrds_uPortrayal.setField(model.ctrds_u);
        ctrds_dPortrayal.setField(model.ctrds_d);
        ctrds_pPortrayal.setField(model.ctrds_p);

        Bag ctrds = new Bag();
        ctrds.add(ctrds_sPortrayal);
        ctrds.add(ctrds_uPortrayal);
        ctrds.add(ctrds_dPortrayal);
        ctrds.add(ctrds_pPortrayal);
        Color[] cls = {Color.RED, Color.BLUE, Color.MAGENTA, Color.GREEN};
        int i = 0;
        for(Object x: ctrds){
            ContinuousPortrayal2D y = (ContinuousPortrayal2D) x;
            final Integer mi = i++;
            y.setPortrayalForAll(
                new LabelledPortrayal2D(
                    new OvalPortrayal2D(){
                        public void draw(Object object, Graphics2D graphics, DrawInfo2D info){
                            Resource res = (Resource) object;
    
                            paint = cls[mi];
                            switch(mi){
                                case 0: scale = res.scarcity; break;
                                case 1: scale = Math.log(res.disparity)/2; break;
                                case 2: scale = res.demand; break;
                                case 3: scale = res.productors.size();
                                default: scale = 2.0;
                            }

                            //scale *= 2;
                            super.draw(object, graphics, info);
                        }
                    }   
                    , 2.0, null, Color.black, false)
            );
        }


        // Portrayal for edges
        netPortrayal.setField(new SpatialNetwork2D(model.space, model.village));
        netPortrayal.setPortrayalForAll(new SimpleEdgePortrayal2D());

        display.reset();
        display.setBackdrop(Color.white);

        display.repaint();
    }

    public void init(Controller c){
        super.init(c);

        // Display
        display = new Display2D(600,600, this);
        display.setClipping(false);
        displayFrame =  display.createFrame();
        displayFrame.setTitle("Village Generator");
        c.registerFrame(displayFrame);
        displayFrame.setVisible(true);

        // Portrayals
        display.attach(spacePortrayal, "Space");
        display.attach(netPortrayal, "Village");
        display.attach(ctrds_sPortrayal, "Scarcity Centroids");
        display.attach(ctrds_uPortrayal, "Disparity Centroids");
        display.attach(ctrds_dPortrayal, "Demand Centroids");
        display.attach(ctrds_pPortrayal, "Production Centroids");

        // Inspectors
        display.attach(new SimpleInspector((Object)Resource.AllResources, this), "Resources");
        display.attach(new SimpleInspector((Object)Agent.AllAgents, this), "Agents");

        if(DO_PLOT){

            // Scarcity Chart
            scarcityChart = ChartUtilities.buildTimeSeriesChartGenerator(this, "Scarcity", "Time");
            scarcityChartAttributes.add(ChartUtilities.addSeries(scarcityChart,"Water"));
            scarcityChartAttributes.add(ChartUtilities.addSeries(scarcityChart,"Food"));
    
            // Disparity Chart
            disparityChart = ChartUtilities.buildTimeSeriesChartGenerator(this, "Disparity", "Time");
            disparityChartAttributes.add(ChartUtilities.addSeries(disparityChart,"Water"));
            disparityChartAttributes.add(ChartUtilities.addSeries(disparityChart,"Food"));
            disparityChartAttributes.add(ChartUtilities.addSeries(disparityChart,"Clothes"));
            disparityChartAttributes.add(ChartUtilities.addSeries(disparityChart,"Jewelry"));
            disparityChartAttributes.add(ChartUtilities.addSeries(disparityChart,"Faith"));
    
            // Demand Chart
            demandChart = ChartUtilities.buildTimeSeriesChartGenerator(this, "Demand", "Time");
            demandChartAttributes.add(ChartUtilities.addSeries(demandChart,"Clothes"));
            demandChartAttributes.add(ChartUtilities.addSeries(demandChart,"Jewelry"));
            demandChartAttributes.add(ChartUtilities.addSeries(demandChart,"Faith"));
        }

    }

    public void quit(){
        super.quit();
        if(displayFrame != null) displayFrame.dispose();
        displayFrame = null;
        display = null;
    }

    public Object getSimulationInspectedObject(){return state;}

    public Inspector getInspector(){
        Inspector i = super.getInspector();
        i.setVolatile(true);
        return i;
    }

}