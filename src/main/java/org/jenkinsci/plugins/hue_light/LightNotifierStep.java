/*
 * The MIT License
 *
 * Copyright 2019 A491026.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.hue_light;

import hudson.Extension;
import hudson.model.BallColor;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import net.sf.json.JSONObject;
import nl.q42.jue.Light;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author christian.alfredsson@afconsult.com
 */
public class LightNotifierStep extends Step {
    public final String bridgeUsername;
    public final String bridgeIp;
    private final HashSet<String> lightId;
    
    /* preBuild is not yet used */
    @Nonnull
    private String preBuild = DescriptorImpl.defaultPreBuild;
    @Nonnull
    private String goodBuild = DescriptorImpl.defaultGoodBuild;
    @Nonnull
    private String unstableBuild = DescriptorImpl.defaultUnstableBuild;
    @Nonnull
    private String badBuild = DescriptorImpl.defaultBadBuild;
    
    
    @Nonnull
    public String getPreBuild() {return preBuild;}
    @DataBoundSetter
    public void setPreBuild(String preBuild){
        this.preBuild = preBuild;
    }
    
    @Nonnull
    public String getGoodBuild() {return goodBuild;}
    @DataBoundSetter
    public void setGoodBuild(String goodBuild){
        this.goodBuild = goodBuild;
    }
    
    @Nonnull
    public String getUnstableBuild() {return unstableBuild;}
    @DataBoundSetter
    public void setUnstableBuild(String unstableBuild){
        this.unstableBuild = unstableBuild;
    }
    
    @Nonnull
    public String getBadBuild() {return badBuild;}
    @DataBoundSetter
    public void setBadBuild(String badBuild){
        this.badBuild = badBuild;
    }
    
    public String getBridgeUsername(){return this.bridgeUsername;}
    public String getBridgeIp() {return this.bridgeIp;}
    
    public String getLightId(){
        String lid = "";
            if(this.lightId != null && this.lightId.size() > 0) {
                for(String id : this.lightId) {
                        lid += id + ",";
                }
                lid = lid.substring(0, lid.length() - 1);
            }
        return lid;
    }
   
    
    @DataBoundConstructor
    public LightNotifierStep(
            String bridgeUsername, 
            String bridgeIp, 
            String lightId){
        this.bridgeUsername = bridgeUsername;
        this.bridgeIp = bridgeIp;
        this.lightId = new HashSet<>();
    	
        if(lightId != null) {
    		String[] lightIds = lightId.split(",");
    		for(String id : lightIds) {
    			this.lightId.add(id.trim());
    		}
    	}
    }   
    
    
    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new LightNotifierStepExecution(this.bridgeUsername, this.bridgeIp, this.lightId,this.goodBuild, this.badBuild, this.unstableBuild, context);
    }


    private static class LightNotifierStepExecution extends SynchronousStepExecution<Void> {
        
        //@SuppressFBWarnings(value="SE_TRANSIENT_FIELD_NOT_RESTORED", justification="Only used when starting.")
        private transient final String bridgeUsername;
        private transient final String bridgeIp;
        private transient final HashSet<String> lightId;
        private transient final String goodBuild;
        private transient final String badBuild;
        private transient final String unstableBuild;
        private final LightController lightController;
        private final LightNotifier.DescriptorImpl notifierDescriptor;
        

        LightNotifierStepExecution(
                String bridgeUsername, 
                String bridgeIp, 
                HashSet<String> lightId, 
                String good,
                String bad,
                String ugly,
                StepContext context) throws Exception {
        super(context);
        this.bridgeUsername = bridgeUsername;
        this.bridgeIp = bridgeIp;
        this.lightId = lightId;
        this.goodBuild = good;
        this.badBuild = bad;
        this.unstableBuild = ugly;
        this.notifierDescriptor = new LightNotifier.DescriptorImpl();
        
        PrintStream logger = getContext().get(TaskListener.class).getLogger();
        this.lightController = new LightController(notifierDescriptor, logger,this.bridgeIp, this.bridgeUsername);
        }
        
        @Override
        protected Void run() throws Exception {
            setColor();
            return null;
        }
        
        
        private void setColor() throws Exception {
            BallColor ballcolor = getContext().get(Run.class).getResult().color;
        
            for(String id : this.lightId) {
	        Light light = this.lightController.getLightForId(id);
	
	        switch (ballcolor) {
	            case RED:
	                this.lightController.setColor(light, "Bad Build", ConfigColorToHue(badBuild));
	                break;
	            case YELLOW:
	                this.lightController.setColor(light, "Unstable Build", ConfigColorToHue(unstableBuild));
	                break;
	            case BLUE:
	                this.lightController.setColor(light, "Good Build", ConfigColorToHue(goodBuild));
	                break;
	        }
            }
        }
        
       private Integer ConfigColorToHue(String color) {

            if (color.equalsIgnoreCase("blue")) {
                return Integer.parseInt(notifierDescriptor.getBlue());

            } else if (color.equalsIgnoreCase("green")) {
                return Integer.parseInt(notifierDescriptor.getGreen());

            } else if (color.equalsIgnoreCase("yellow")) {
                return Integer.parseInt(notifierDescriptor.getYellow());

            } else if (color.equalsIgnoreCase("red")) {
                return Integer.parseInt(notifierDescriptor.getRed());

            } else {
                if (DescriptorImpl.isInteger(color))
                    return Integer.parseInt(color);
                else
                    return 0;
            }
        } 
    }

    @Extension
    public static final class DescriptorImpl extends StepDescriptor {
        public static final String defaultPreBuild = "blue";
        public static final String defaultGoodBuild = "green";
        public static final String defaultUnstableBuild = "yellow";
        public static final String defaultBadBuild = "red";
        
        
        public static boolean isInteger(String s) {
            try {
                Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return false;
            }
            return true;
        }
    
        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(TaskListener.class);
        }

        @Override
        public String getFunctionName() {
            return "huelight";
        }

        @Override 
        public String getDisplayName() {
            return "Colorize hue lights";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            return super.configure(req, json); 
        }
    }
}
