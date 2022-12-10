/*
 * Copyright © 2022 United States Government as represented by the Administrator of the National
 * Aeronautics and Space Administration. All Rights Reserved.
 *
 * NASA acknowledges the SETI Institute’s primary role in authoring and producing Ziggy, a Pipeline
 * Management System for Data Analysis Pipelines, under Cooperative Agreement Nos. NNX14AH97A,
 * 80NSSC18M0068 & 80NSSC21M0079.
 *
 * This file is available under the terms of the NASA Open Source Agreement (NOSA). You should have
 * received a copy of this agreement with the Ziggy source code; see the file LICENSE.pdf.
 *
 * Disclaimers
 *
 * No Warranty: THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY KIND, EITHER
 * EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT LIMITED TO, ANY WARRANTY THAT THE SUBJECT
 * SOFTWARE WILL CONFORM TO SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL BE
 * ERROR FREE, OR ANY WARRANTY THAT DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT
 * SOFTWARE. THIS AGREEMENT DOES NOT, IN ANY MANNER, CONSTITUTE AN ENDORSEMENT BY GOVERNMENT AGENCY
 * OR ANY PRIOR RECIPIENT OF ANY RESULTS, RESULTING DESIGNS, HARDWARE, SOFTWARE PRODUCTS OR ANY
 * OTHER APPLICATIONS RESULTING FROM USE OF THE SUBJECT SOFTWARE. FURTHER, GOVERNMENT AGENCY
 * DISCLAIMS ALL WARRANTIES AND LIABILITIES REGARDING THIRD-PARTY SOFTWARE, IF PRESENT IN THE
 * ORIGINAL SOFTWARE, AND DISTRIBUTES IT "AS IS."
 *
 * Waiver and Indemnity: RECIPIENT AGREES TO WAIVE ANY AND ALL CLAIMS AGAINST THE UNITED STATES
 * GOVERNMENT, ITS CONTRACTORS AND SUBCONTRACTORS, AS WELL AS ANY PRIOR RECIPIENT. IF RECIPIENT'S
 * USE OF THE SUBJECT SOFTWARE RESULTS IN ANY LIABILITIES, DEMANDS, DAMAGES, EXPENSES OR LOSSES
 * ARISING FROM SUCH USE, INCLUDING ANY DAMAGES FROM PRODUCTS BASED ON, OR RESULTING FROM,
 * RECIPIENT'S USE OF THE SUBJECT SOFTWARE, RECIPIENT SHALL INDEMNIFY AND HOLD HARMLESS THE UNITED
 * STATES GOVERNMENT, ITS CONTRACTORS AND SUBCONTRACTORS, AS WELL AS ANY PRIOR RECIPIENT, TO THE
 * EXTENT PERMITTED BY LAW. RECIPIENT'S SOLE REMEDY FOR ANY SUCH MATTER SHALL BE THE IMMEDIATE,
 * UNILATERAL TERMINATION OF THIS AGREEMENT.
 */

package gov.nasa.ziggy.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import gov.nasa.ziggy.metrics.report.PerformanceReport;
import gov.nasa.ziggy.models.ModelRegistryOperations;
import gov.nasa.ziggy.pipeline.PipelineExecutor;
import gov.nasa.ziggy.pipeline.PipelineOperations;
import gov.nasa.ziggy.pipeline.definition.PipelineDefinition;
import gov.nasa.ziggy.pipeline.definition.PipelineDefinitionNode;
import gov.nasa.ziggy.pipeline.definition.PipelineInstance;
import gov.nasa.ziggy.pipeline.definition.PipelineModule.RunMode;
import gov.nasa.ziggy.pipeline.definition.PipelineTask;
import gov.nasa.ziggy.pipeline.definition.PipelineTask.ProcessingSummary;
import gov.nasa.ziggy.pipeline.definition.crud.PipelineDefinitionCrud;
import gov.nasa.ziggy.pipeline.definition.crud.PipelineInstanceCrud;
import gov.nasa.ziggy.pipeline.definition.crud.PipelineInstanceNodeCrud;
import gov.nasa.ziggy.pipeline.definition.crud.PipelineTaskCrud;
import gov.nasa.ziggy.pipeline.definition.crud.ProcessingSummaryOperations;
import gov.nasa.ziggy.services.alert.AlertLog;
import gov.nasa.ziggy.services.alert.AlertLogCrud;
import gov.nasa.ziggy.services.config.DirectoryProperties;
import gov.nasa.ziggy.services.config.ZiggyConfiguration;
import gov.nasa.ziggy.services.database.DatabaseTransaction;
import gov.nasa.ziggy.services.database.DatabaseTransactionFactory;
import gov.nasa.ziggy.services.messages.WorkerFireTriggerRequest;
import gov.nasa.ziggy.services.messaging.MessageHandler;
import gov.nasa.ziggy.services.messaging.UiCommunicator;
import gov.nasa.ziggy.ui.messaging.ConsoleMessageDispatcher;
import gov.nasa.ziggy.ui.proxy.PipelineExecutorProxy;
import gov.nasa.ziggy.util.TasksStates;
import gov.nasa.ziggy.util.ZiggyShutdownHook;
import gov.nasa.ziggy.util.dispmod.AlertLogDisplayModel;
import gov.nasa.ziggy.util.dispmod.InstancesDisplayModel;
import gov.nasa.ziggy.util.dispmod.PipelineStatsDisplayModel;
import gov.nasa.ziggy.util.dispmod.TaskMetricsDisplayModel;
import gov.nasa.ziggy.util.dispmod.TaskSummaryDisplayModel;
import gov.nasa.ziggy.util.dispmod.TasksDisplayModel;

/**
 * Top-level front end for Ziggy control. {@link ZiggyConsole} can launch a new instance of
 * {@link ZiggyGuiConsole}, the console GUI, or accept command-line options that can be used to
 * control the pipeline.
 *
 * @author Todd Klaus
 * @author PT
 */

public class ZiggyConsole {
    private static final int DEFAULT_STATUS_WAIT_MILLIS = 10000;

    public ZiggyConsole() {
    }

    public void processCommand(String[] args) throws Exception {
        String command = args[0];
        if (command.equals("instance") || command.equals("i")) {
            processInstanceCommand(args);
        } else if (command.equals("task") || command.equals("t")) {
            processTaskCommand(args);
        } else if (command.equals("worker") || command.equals("w")) {
            processWorkerCommand(args);
        } else if (command.equals("reports") || command.equals("r")) {
            processReportsCommand(args);
        } else if (command.equals("fire") || command.equals("f")) {
            fireTriggerCommand(args);
        } else if (command.equals("show") || command.equals("s")) {
            if (args.length <= 2) {
                System.err.println("Too few arguments for show command");
                usage();
                System.exit(-1);
            }
            if (args[1].equals("trigger")) {
                processShowTriggerCommand(Arrays.copyOfRange(args, 2, args.length));
            } else {
                System.err.println("Unknown show command option '" + args[1] + "'");
                usage();
                System.exit(-1);
            }
        } else if (command.equals("Restart") || command.equals("R")) {
            processRestartCommand(args);
        } else {
            System.err.println("Unknown command: " + printCommandLine(args));
            usage();
            System.exit(-1);
        }
    }

    private void processInstanceCommand(String[] args) throws Exception {
        PipelineInstanceCrud pipelineInstanceCrud = new PipelineInstanceCrud();

        if (args.length == 1) {
            // i[nstance] : display status of all pipeline instances
            List<PipelineInstance> instances = pipelineInstanceCrud.retrieveAll();
            InstancesDisplayModel instancesDisplayModel = new InstancesDisplayModel(instances);

            instancesDisplayModel.print(System.out, "Pipeline Instances");
        } else {
            if (args[1].equals("c")) {
                List<PipelineInstance> activeInstances = pipelineInstanceCrud.retrieveAllActive();
                System.out.println("Cancelling Active Instances:");
                for (PipelineInstance instance : activeInstances) {
                    System.out.println(" " + instance.getName());
                }
                pipelineInstanceCrud.cancelAllActive();
            } else {
                long id = -1;
                try {
                    id = Long.parseLong(args[1]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid ID: " + args[1]);
                    usage();
                    System.exit(-1);
                }

                PipelineInstance instance = pipelineInstanceCrud.retrieve(id);

                if (instance == null) {
                    System.err.println("No instance found with ID = " + id);
                    System.exit(-1);
                }

                InstancesDisplayModel instancesDisplayModel = new InstancesDisplayModel(instance);
                instancesDisplayModel.print(System.out, "Instance Summary");
                System.out.println();

                if (args.length == 2) {
                    // i[nstance] ID : display status and task count summary of
                    // specified pipeline instance

                    PipelineTaskCrud pipelineTaskCrud = new PipelineTaskCrud();
                    List<PipelineTask> tasks = pipelineTaskCrud.retrieveTasksForInstance(instance);

                    Map<Long, ProcessingSummary> taskAttrs = new ProcessingSummaryOperations()
                        .processingSummaries(tasks);
                    displayTaskSummary(tasks, taskAttrs);
                } else {
                    String subCommand = args[2];

                    if (subCommand.equals("full") || subCommand.equals("f")) {
                        // i[nstance] ID f[ull]: display status of all tasks for
                        // specified pipeline instance

                        PipelineTaskCrud pipelineTaskCrud = new PipelineTaskCrud();
                        List<PipelineTask> tasks = pipelineTaskCrud
                            .retrieveTasksForInstance(instance);
                        Map<Long, ProcessingSummary> taskAttrs = new ProcessingSummaryOperations()
                            .processingSummaries(tasks);

                        displayTaskSummary(tasks, taskAttrs);
                        TasksDisplayModel tasksDisplayModel = new TasksDisplayModel(tasks,
                            taskAttrs);
                        tasksDisplayModel.print(System.out, "Pipeline Tasks");
                    } else if (subCommand.equals("reset")) {
                        if (args.length < 4) {
                            System.err.println("The reset command requires an additional arg: "
                                + printCommandLine(args));
                            usage();
                            System.exit(-1);
                        }

                        String taskType = args[3];

                        if (taskType.equals("s")) {
                            resetPipelineInstance(instance, false, null);
                        } else if (taskType.equals("a")) {
                            resetPipelineInstance(instance, true, null);
                        } else if (taskType.matches(".*\\d.*")) {
                            // If the arg contains a digit, then assume it's a list of taskIds
                            resetPipelineInstance(instance, true, taskType);
                        } else {
                            System.err.println("Unknown reset arg: " + printCommandLine(args));
                            usage();
                            System.exit(-1);
                        }
                    } else if (subCommand.equals("report") || subCommand.equals("r")) {
                        // i[nstance] ID r[eport]: display report for specified
                        // pipeline instance

                        PipelineOperations ops = new PipelineOperations();
                        String report = ops.generatePedigreeReport(instance);
                        System.out.println(report);
                        PerformanceReport perfReport = new PerformanceReport(instance.getId(),
                            DirectoryProperties.taskDataDir().toFile(), null);
                        perfReport.generateReport();
                    } else if (subCommand.equals("alerts") || subCommand.equals("a")) {
                        // i[nstance] ID a[lerts]: display alerts for specified
                        // pipeline instance

                        AlertLogCrud alertLogCrud = new AlertLogCrud();
                        List<AlertLog> alerts = alertLogCrud
                            .retrieveForPipelineInstance(instance.getId());
                        AlertLogDisplayModel alertLogDisplayModel = new AlertLogDisplayModel(
                            alerts);
                        alertLogDisplayModel.print(System.out, "Alerts");
                    } else if (subCommand.equals("statistics") || subCommand.equals("s")) {
                        // i[nstance] ID s[statistics]: display processing time statistics for
                        // specified pipeline instance

                        PipelineTaskCrud pipelineTaskCrud = new PipelineTaskCrud();
                        List<PipelineTask> tasks = pipelineTaskCrud
                            .retrieveTasksForInstance(instance);

                        Map<Long, ProcessingSummary> taskAttrs = new ProcessingSummaryOperations()
                            .processingSummaries(tasks);

                        TasksStates tasksStates = displayTaskSummary(tasks, taskAttrs);
                        List<String> orderedModuleNames = tasksStates.getModuleNames();

                        PipelineStatsDisplayModel pipelineStatsDisplayModel = new PipelineStatsDisplayModel(
                            tasks, orderedModuleNames);
                        pipelineStatsDisplayModel.print(System.out, "Processing Time Statistics");

                        TaskMetricsDisplayModel taskMetricsDisplayModel = new TaskMetricsDisplayModel(
                            tasks, orderedModuleNames);
                        taskMetricsDisplayModel.print(System.out,
                            "Processing Time Breakdown (completed tasks only)");
                    } else if (subCommand.equals("errors") || subCommand.equals("e")) {
                        // i[nstance] ID e[rrors]: display status and worker logs for all failed
                        // tasks for specified pipeline instance

                        PipelineTaskCrud pipelineTaskCrud = new PipelineTaskCrud();
                        List<PipelineTask> tasks = pipelineTaskCrud.retrieveAll(instance,
                            PipelineTask.State.ERROR);

                        Map<Long, ProcessingSummary> taskAttrs = new ProcessingSummaryOperations()
                            .processingSummaries(tasks);

                        for (PipelineTask task : tasks) {
                            TasksDisplayModel tasksDisplayModel = new TasksDisplayModel(task,
                                taskAttrs.get(task.getId()));
                            tasksDisplayModel.print(System.out, "Task Summary");

                            System.out.println();
                            System.out.println("Worker log: ");

//                            System.out.println(WorkerTaskLogRequest.requestTaskLog(task));
                        }
                    } else {
                        System.err
                            .println("Unknown instance subcommand: " + printCommandLine(args));
                        usage();
                        System.exit(-1);
                    }
                }
            }
        }
    }

    /**
     * @param instance
     * @param allStalledTasks If true, reset SUBMITTED and PROCESSING tasks, else just SUBMITTED
     * tasks
     */
    private void resetPipelineInstance(PipelineInstance instance, boolean allStalledTasks,
        String taskIds) {
        long instanceId = instance.getId();

        /*
         * Set the pipeline task state to ERROR for any tasks assigned to this worker that are in
         * the PROCESSING state. This condition indicates that the previous instance of the worker
         * process on this host died abnormally
         */
        DatabaseTransactionFactory.performTransaction(() -> {
            PipelineTaskCrud pipelineTaskCrud = new PipelineTaskCrud();
            pipelineTaskCrud.resetTaskStates(instanceId, allStalledTasks, taskIds);
            return null;
        });

        /*
         * Update the pipeline instance state for the instances associated with the stale tasks from
         * above since that change may result in a change to the instances
         */
        DatabaseTransactionFactory.performTransaction(() -> {
            PipelineExecutor pe = new PipelineExecutor();

            // handle the corner case in which the task counts in the instance nodes
            // have gotten out of sync with the task counts in the tasks themselves

            new PipelineInstanceNodeCrud().updateTaskCountsFromTasks(instanceId);
            pe.updateInstanceState(instance);
            return null;
        });

    }

    private TasksStates displayTaskSummary(List<PipelineTask> tasks,
        Map<Long, ProcessingSummary> taskAttrs) throws Exception {
        TaskSummaryDisplayModel taskSummaryDisplayModel = new TaskSummaryDisplayModel(
            new TasksStates(tasks, taskAttrs));
        taskSummaryDisplayModel.print(System.out, "Instance Task Summary");
        return taskSummaryDisplayModel.getTaskStates();
    }

    private void processTaskCommand(String[] args) throws Exception {
        if (args.length == 2 || args.length == 3 || args.length == 4) {
            PipelineTaskCrud pipelineTaskCrud = new PipelineTaskCrud();
            long id = -1;
            try {
                id = Long.parseLong(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid ID: " + args[1]);
                usage();
                System.exit(-1);
            }

            PipelineTask task = pipelineTaskCrud.retrieve(id);

            ProcessingSummary taskAttr = new ProcessingSummaryOperations().processingSummary(id);

            if (task == null) {
                System.err.println("No task found with ID = " + id);
                System.exit(-1);
            }

            TasksDisplayModel tasksDisplayModel = new TasksDisplayModel(task, taskAttr);
            tasksDisplayModel.print(System.out, "Task Summary");
            System.out.println();

            if (args.length >= 3) {
                String subCommand = args[2];

                if (subCommand.equals("log") || subCommand.equals("l")) {
                    // t[ask] ID l[og] : display status and log (fetched from worker) for selected
                    // task

                    System.out.println("Requesting log from worker...");

//                    System.out.println(WorkerTaskLogRequest.requestTaskLog(task));
                } else {
                    System.err.println("Unknown task subcommand: " + printCommandLine(args));
                    usage();
                    System.exit(-1);
                }
            }
        } else {
            System.err.println("Too many arguments: " + printCommandLine(args));
            usage();
            System.exit(-1);
        }
    }

    private String printCommandLine(String[] args) {
        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            sb.append(arg + " ");
        }
        return sb.toString();
    }

    private void processWorkerCommand(String[] args) {
        int waitTimeMillis = DEFAULT_STATUS_WAIT_MILLIS;

        if (args.length == 2) {
            try {
                waitTimeMillis = Integer.parseInt(args[1]) * 1000;
            } catch (NumberFormatException e) {
                System.err.println("Invalid Wait time: " + args[1]);
                usage();
                System.exit(-1);
            }
        }

        try {
            Thread.sleep(waitTimeMillis);
        } catch (InterruptedException e) {
        }

        System.exit(0);
    }

    private void processReportsCommand(String[] args) {
        if (args.length == 2) {
            String reportType = args[1];
            PipelineOperations pipelineOps = new PipelineOperations();
            if (reportType.equals("i") || reportType.equals("instance")) {
                PipelineInstanceCrud instanceCrud = new PipelineInstanceCrud();
                List<PipelineInstance> instances = instanceCrud.retrieveAll();

                System.out.println("***** Pipeline Instance Reports *****");

                for (PipelineInstance instance : instances) {
                    String instanceReport = pipelineOps.generatePedigreeReport(instance);
                    System.out.println(instanceReport);
                    System.out.println();
                    System.out.println();
                }
            } else if (reportType.equals("t") || reportType.equals("trigger")) {
                List<PipelineDefinition> pipelines = new PipelineDefinitionCrud().retrieveAll();

                System.out.println("***** Trigger Reports *****");

                for (PipelineDefinition pipeline : pipelines) {
                    String triggerReport = pipelineOps.generateTriggerReport(pipeline);
                    System.out.println(triggerReport);
                    System.out.println();
                    System.out.println();
                }
            } else if (reportType.equals("d") || reportType.equals("data-model-registry")) {
                System.out.println("Data Model Registry");
                System.out.println();
                ModelRegistryOperations modelMetadataOps = new ModelRegistryOperations();
                System.out.println(modelMetadataOps.report());
            } else {
                System.err.println("Unrecognized report type: " + reportType);
                usage();
                System.exit(-1);
            }
        } else {
            System.err.println("Report type not specified (instance or trigger)");
            usage();
            System.exit(-1);
        }

        System.exit(0);
    }

    private void fireTriggerCommand(String[] args) {
        if (args.length < 3) {
            System.err.println("Too few arguments for firing trigger");
            usage();
            System.exit(-1);
        } else {
            String triggerName = args[1];
            String instanceName = args[2];

            String startNodeName = null;
            String endNodeName = null;
            if (args.length >= 5) {
                startNodeName = args[3];
                endNodeName = args[4];
            }

            fireTrigger(triggerName, instanceName, startNodeName, endNodeName);
        }

        System.exit(0);
    }

    private void fireTrigger(final String pipelineName, String instanceName, String startNodeName,
        String endNodeName) {

        System.out.println("Launching " + pipelineName);

        PipelineOperations pipelineOps = new PipelineOperations();
        pipelineOps.sendTriggerMessage(new WorkerFireTriggerRequest(pipelineName, instanceName,
            startNodeName, endNodeName, 1, 0));
        System.out.println("Done launching " + pipelineName);
    }

    private void processShowTriggerCommand(String[] args) {
        if (args.length != 1) {
            usage();
            System.exit(-1);
        }

        String triggerName = args[0];

        DatabaseTransactionFactory.performTransaction(new DatabaseTransaction<Void>() {

            @Override
            public void catchBlock(Throwable e) {
                System.out.println("Unable to fire trigger, caught e = " + e);
            }

            @Override
            public Void transaction() throws Exception {
                PipelineDefinitionCrud pipelineDefinitionCrud = new PipelineDefinitionCrud();
                PipelineDefinition pipelineDefinition = pipelineDefinitionCrud
                    .retrieveLatestVersionForName(triggerName);
                if (pipelineDefinition == null) {
                    System.err.println("Trigger '" + triggerName + "' not found");
                    System.exit(-1);
                }

                System.out.println("Nodes for trigger '" + triggerName + "':");
                int index = 1;
                for (PipelineDefinitionNode node : pipelineDefinition.getRootNodes()) {
                    index += showNode(node, index);
                }
                return null;
            }
        });
    }

    private int showNode(PipelineDefinitionNode node, int index) {
        int count = 1;
        System.out.println(String.format("%6d: %s", index, node.getModuleName().getName()));
        for (PipelineDefinitionNode sibling : node.getNextNodes()) {
            count += showNode(sibling, index + count);
        }

        return count;
    }

    private void processRestartCommand(String[] args) {
        if (args.length < 2) {
            System.err.println("No task IDs specified");
            usage();
            System.exit(-1);
        }

        Collection<Long> taskIDs = new ArrayList<>();
        for (int i = 1; i < args.length; ++i) {
            try {
                long taskID = Long.parseLong(args[i]);
                taskIDs.add(taskID);
            } catch (NumberFormatException ex) {
                System.err.println("Task ID is not an integer: " + args[i]);
                usage();
                System.exit(-1);
            }
        }

        DatabaseTransactionFactory.performTransaction(new DatabaseTransaction<Void>() {
            @Override
            public void catchBlock(Throwable e) {
                System.out.println("Unable to restart tasks, caught e = " + e);
            }

            @Override
            public Void transaction() throws Exception {
                List<PipelineTask> tasks = new PipelineTaskCrud().retrieveAll(taskIDs);
                for (long taskID : taskIDs) {
                    boolean found = false;
                    for (PipelineTask task : tasks) {
                        if (task.getId() == taskID) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        throw new IllegalArgumentException("Task not found with ID " + taskID);
                    }
                }

                PipelineExecutorProxy pipelineExecutor = new PipelineExecutorProxy();
                System.out.println("Restarting " + tasks.size() + " tasks");
                pipelineExecutor.restartTasks(tasks, RunMode.RESTART_FROM_BEGINNING);
                return null;
            }
        });
    }

    private static void usage() {
        System.out.println("picli COMMAND ARGS");
        System.out.println("  Examples:");
        System.out.println("    i[nstance] : display status of all pipeline instances");
        System.out.println(
            "    i[nstance] ID : display status and task count summary of specified pipeline instance");
        System.out.println(
            "    i[nstance] ID f[ull]: display status of all tasks for specified pipeline instance");
        System.out
            .println("    i[nstance] ID r[eport]: display report for specified pipeline instance");
        System.out
            .println("    i[nstance] ID a[lerts]: display alerts for specified pipeline instance");
        System.out.println(
            "    i[nstance] ID s[statistics]: display processing time statistics for specified pipeline instance");
        System.out.println(
            "    i[nstance] ID e[rrors]: display status and worker logs for all failed tasks for specified pipeline instance");
        System.out.println(
            "    i[nstance] ID reset s: (reset submitted): Puts all SUBMITTED tasks in the specified pipeline instance in the ERROR state so that they can be restarted");
        System.out.println(
            "    i[nstance] ID reset a: (reset all): Puts all SUBMITTED or PROCESSING tasks in the specified pipeline instance in the ERROR state so that they can be restarted");
        System.out.println(
            "    i[nstance] ID reset task_IDs: (reset task_IDs): Puts all SUBMITTED or PROCESSING tasks in the specified pipeline instance and on the supplied list of task_IDs in the ERROR state so that they can be restarted. task_IDs is a comma-separated list of task IDs with no spaces (For example, 10000 or 100,101,105)");
        System.out
            .println("    i[nstance] c[ancel]: Puts all running instances in the STOPPED state");
        System.out.println("    t[ask] ID : display status for selected task");
        System.out.println(
            "    t[ask] ID l[og] : display status and log (fetched from worker) for selected task");
        System.out.println(
            "    t[ask] ID c[opy] DESTINATION_PATH : display status and log (fetched from worker) for selected task");
        System.out.println("    w[orker] : display worker health status (10 second wait)");
        System.out.println("    w[orker] WAIT : display worker health status (WAIT seconds wait)");
        System.out.println(
            "    r[eports] d[ata-model-registry] : current state of the data model registry");
        System.out.println(
            "    r[eports] i[instance] : dump all pipeline instance reports (WARNING: could be big & slow!)");
        System.out.println(
            "    r[eports] t[rigger] : dump all trigger reports (WARNING: could be big & slow!)");
        System.out.println(
            "    f[ire] TRIGGER_NAME INSTANCE_NAME [startnode stopnode] : Fire the specified trigger and assign INSTANCE_NAME as the name of the new pipeline instance");
        System.out.println("    s[how] trigger TRIGGER_NAME : show nodes in a trigger");
        System.out.println("    R[estart] task_IDs: restarts specified tasks");
    }

    public static void main(String[] args) throws Exception {

        if (args.length > 0) { // CLI control
            if (args.length < 1) {
                usage();
                System.exit(-1);
            }

            ZiggyConsole cli = new ZiggyConsole();

            MessageHandler messageHandler = new MessageHandler(
                new ConsoleMessageDispatcher(null, null, false));
            int rmiPort = ZiggyConfiguration.getInstance()
                .getInt(MessageHandler.RMI_REGISTRY_PORT_PROP,
                    MessageHandler.RMI_REGISTRY_PORT_PROP_DEFAULT);
            UiCommunicator.initializeInstance(messageHandler, rmiPort);
            ZiggyShutdownHook.addShutdownHook(() -> {
                UiCommunicator.reset();
            });

            cli.processCommand(args);

            System.exit(0);
        } else { // GUI control
            ZiggyGuiConsole.launch();
        }
    }
}
