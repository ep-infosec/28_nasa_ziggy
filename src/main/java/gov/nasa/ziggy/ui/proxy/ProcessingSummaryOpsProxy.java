package gov.nasa.ziggy.ui.proxy;

import java.util.Map;

import gov.nasa.ziggy.pipeline.definition.PipelineTask.ProcessingSummary;
import gov.nasa.ziggy.pipeline.definition.crud.ProcessingSummaryOperations;
import gov.nasa.ziggy.services.security.Privilege;
import gov.nasa.ziggy.ui.ZiggyGuiConsole;

/**
 * @author Todd Klaus
 */
public class ProcessingSummaryOpsProxy extends CrudProxy {
    public ProcessingSummaryOpsProxy() {
    }

    public ProcessingSummary retrieveByTaskId(final long taskId) {
        verifyPrivileges(Privilege.PIPELINE_MONITOR);
        ProcessingSummary result = ZiggyGuiConsole.crudProxyExecutor
            .executeSynchronousDatabaseTransaction(() -> {
                return new ProcessingSummaryOperations().processingSummaryInternal(taskId);
            });
        return result;
    }

    public Map<Long, ProcessingSummary> retrieveByInstanceId(final long instanceId) {
        return retrieveByInstanceId(instanceId, false);
    }

    public Map<Long, ProcessingSummary> retrieveByInstanceId(final long instanceId,
        boolean silent) {
        verifyPrivileges(Privilege.PIPELINE_MONITOR);
        Map<Long, ProcessingSummary> result = ZiggyGuiConsole.crudProxyExecutor
            .executeSynchronousDatabaseTransaction(() -> {
                return new ProcessingSummaryOperations()
                    .processingSummariesForInstanceInternal(instanceId);
            }, silent);
        return result;
    }

}
