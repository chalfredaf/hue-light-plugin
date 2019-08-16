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
import hudson.util.ListBoxModel;
import java.io.PrintStream;
import java.util.Arrays;
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
    
    @Nonnull
    private String result = DescriptorImpl.currentResult;
    @Nonnull
    private String notifierType = DescriptorImpl.notifierResult;
    @Nonnull
    private String preBuild = DescriptorImpl.defaultPreBuild;
    @Nonnull
    private String goodBuild = DescriptorImpl.defaultGoodBuild;
    @Nonnull
    private String unstableBuild = DescriptorImpl.defaultUnstableBuild;
    @Nonnull
    private String badBuild = DescriptorImpl.defaultBadBuild;
   
    public String getNotifierType(){return notifierType;}
    @DataBoundSetter
    public void setNotifierType(String notifierType){
        this.notifierType = notifierType;
    }
    
    public String getResult(){return this.result;}
    @DataBoundSetter
    public void setResult(String r){
        this.result = r;
    }
    
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
        return new LightNotifierStepExecution(
                this.bridgeUsername, 
                this.bridgeIp, 
                this.lightId,
                this.goodBuild, 
                this.badBuild, 
                this.unstableBuild, 
                this.preBuild, 
                this.notifierType,
                this.result,
                context);
    }


    private static class LightNotifierStepExecution extends SynchronousStepExecution<Void> {
        
        private static final long serialVersionUID = 1L;
                
        private transient final String bridgeUsername;
        private transient final String bridgeIp;
        private transient final HashSet<String> lightId;
        private transient final String goodBuild;
        private transient final String badBuild;
        private transient final String unstableBuild;
        private transient final String preBuild;
        private transient final String result;
        private transient final String notifierType;
        private final LightController lightController;
        private final LightNotifier.DescriptorImpl notifierDescriptor;
        private final PrintStream logger;        

        LightNotifierStepExecution(
                String bridgeUsername, 
                String bridgeIp, 
                HashSet<String> lightId, 
                String good,
                String bad,
                String unstable,
                String preBuild,
                String notifierType,
                String result,
                StepContext context) throws Exception {
        super(context);
        this.bridgeUsername = bridgeUsername;
        this.bridgeIp = bridgeIp;
        this.lightId = lightId;
        this.goodBuild = good;
        this.badBuild = bad;
        this.unstableBuild = unstable;
        this.preBuild = preBuild;
        this.notifierDescriptor = new LightNotifier.DescriptorImpl();
        this.logger = getContext().get(TaskListener.class).getLogger();
        this.lightController = new LightController(notifierDescriptor, this.logger,this.bridgeIp, this.bridgeUsername);
        this.notifierType = notifierType;
        this.result = result;
        }
        
        @Override
        protected Void run() throws Exception {
            switch(notifierType){
                case DescriptorImpl.notifierPreBuild:
                    setPreBuild();
                    break;
                case DescriptorImpl.notifierResult:
                    setResult();
                    break;
            }
            return null;
        }
        
        private void setPreBuild() throws Exception {
            for (String id : this.lightId){
                Light light =  this.lightController.getLightForId(id);
                this.lightController.setPulseBreathe(light, "Build Starting", ConfigColorToHue(this.preBuild));
            }
        }
        
        private void setResult(){
            logger.println("Input Param: " + result);
                    
            for(String id : this.lightId) {
                Light light = this.lightController.getLightForId(id);

                switch(result) {
                    case "FAILURE":
                        this.lightController.setColor(light, "Bad Build", ConfigColorToHue(badBuild));
                        break;
                    case "UNSTABLE":
                        this.lightController.setColor(light, "Unstable Build", ConfigColorToHue(unstableBuild));
                        break;
                    case "SUCCESS":
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
        public static final String notifierPreBuild = "Prebuild";
        public static final String notifierResult = "Build Result";
        public static final String currentResult = "${currentBuild.currentResult}";
        
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
        
        private ListBoxModel defaultList(){
            ListBoxModel items = new ListBoxModel();
            Arrays.asList(defaultPreBuild, defaultGoodBuild, defaultUnstableBuild, defaultBadBuild)
                    .forEach( (i) -> {
                        items.add(i);
                    } );
            return items;
        }
        
        public ListBoxModel doFillPreBuildItems() {
            
            return defaultList();
        }
        
        public ListBoxModel doFillGoodBuildItems() {
            return defaultList();
        }
        
        public ListBoxModel doFillUnstableBuildItems(){
            return defaultList();
        }
        
        public ListBoxModel doFillBadBuildItems(){
            return defaultList();
        }
        
        public ListBoxModel doFillNotifierTypeItems(){
            ListBoxModel items = new ListBoxModel();
            Arrays.asList(notifierPreBuild, notifierResult)
                    .forEach((i) -> {
                        items.add(i);
                    } );
            return items;
        }
    }
}
