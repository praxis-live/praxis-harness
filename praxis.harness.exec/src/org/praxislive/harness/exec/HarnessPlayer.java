/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2018 Neil C Smith.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License version 3
 * along with this work; if not, see http://www.gnu.org/licenses/
 * 
 *
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 */
package org.praxislive.harness.exec;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.neilcsmith.praxis.core.Argument;
import net.neilcsmith.praxis.core.Call;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.core.ComponentAddress;
import net.neilcsmith.praxis.core.ControlAddress;
import net.neilcsmith.praxis.core.PacketRouter;
import net.neilcsmith.praxis.core.Root;
import net.neilcsmith.praxis.core.interfaces.ServiceUnavailableException;
import net.neilcsmith.praxis.core.info.ControlInfo;
import net.neilcsmith.praxis.core.interfaces.ScriptService;
import net.neilcsmith.praxis.core.interfaces.SystemManagerService;
import net.neilcsmith.praxis.core.types.PReference;
import net.neilcsmith.praxis.core.types.PString;
import net.neilcsmith.praxis.impl.AbstractControl;
import net.neilcsmith.praxis.impl.AbstractRoot;
import net.neilcsmith.praxis.impl.SimpleControl;
import org.openide.LifecycleManager;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
class HarnessPlayer extends AbstractRoot {

    private final static Logger LOG = Logger.getLogger(HarnessPlayer.class.getName());
    private List<String> scripts;
    private ScriptControl scriptControl;
    private Root.Controller controller;

    public HarnessPlayer(List<String> scripts) {
        if (scripts == null) {
            throw new NullPointerException();
        }
        this.scripts = scripts;
        scriptControl = new ScriptControl(scripts);
        registerControl("_script-control", scriptControl);
        registerInterface(SystemManagerService.class);
        registerControl(SystemManagerService.SYSTEM_EXIT, new ExitControl());

    }

    @Override
    protected void activating() {
        scriptControl.nextScript();
    }

    private void exit() {
        LifecycleManager.getDefault().exit();
    }

    private class ScriptControl extends AbstractControl {

        ControlAddress evalControl;
        ControlAddress clearControl;
        Call activeCall;
        Queue<String> scriptQueue;

        ScriptControl(List<String> scripts) {
            scriptQueue = new LinkedList<String>(scripts);
        }

        public void call(Call call, PacketRouter router) throws Exception {
            switch (call.getType()) {
                case RETURN:
                    processReturn(call);
                    break;
                case ERROR:
                    processError(call);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        private void processReturn(Call call) throws Exception {
            if (activeCall != null && call.getMatchID() == activeCall.getMatchID()) {
                activeCall = null;
                nextScript();
            }
        }

        private void processError(Call call) throws Exception {
            if (activeCall != null && call.getMatchID() == activeCall.getMatchID()) {
                activeCall = null;
                CallArguments args = call.getArgs();
                if (args.getSize() > 0) {
                    Argument err = args.get(0);
                    if (err instanceof PReference) {
                        Object o = ((PReference) err).getReference();
                        if (o instanceof Throwable) {
                            LOG.log(Level.SEVERE, "ERROR: ", (Throwable) o);
                        } else {
                            LOG.log(Level.SEVERE, "ERROR: {0}", o.toString());
                        }
                    } else {
                        LOG.log(Level.SEVERE, "ERROR: {0}", err.toString());
                    }
                }
                exit();
            }

        }
        
        private void nextScript() {
            String script = scriptQueue.poll();
            if (script != null) {
                runScript(script);
            }
        }

        private void runScript(String script) {
            try {
                ComponentAddress ss = findService(ScriptService.class);
                evalControl = ControlAddress.create(ss, ScriptService.EVAL);
            } catch (ServiceUnavailableException ex) {
                LOG.log(Level.SEVERE, "", ex);
            }
            activeCall = Call.createCall(evalControl, getAddress(), System.nanoTime(), PString.valueOf(script));
            getPacketRouter().route(activeCall);

        }

        public ControlInfo getInfo() {
            return null;
        }
    }

    private class ExitControl extends SimpleControl {

        private ExitControl() {
            super(SystemManagerService.SYSTEM_EXIT_INFO);
        }

        @Override
        protected CallArguments process(long time, CallArguments args, boolean quiet) throws Exception {
            exit();
            return CallArguments.EMPTY;
        }
    }
}
