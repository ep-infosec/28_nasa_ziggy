package gov.nasa.ziggy.module;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nasa.ziggy.pipeline.definition.PipelineModule;

/**
 * This class defines execution dependencies for sub-tasks.
 * <p>
 * Each element in the sequence contains one or more sub-tasks. Sub-tasks that are members of the
 * same element will run in parallel.
 * <p>
 * For example, consider the sequence [0][1,5][6] Sub-task 0 will run first, then sub-tasks 1, 2, 3,
 * 4, and 5 will run in parallel, then sub-task 6 will run. Each element of the sequence starts
 * executing only after the previous element is complete. The class also records the unit of work
 * instance for each sub-task, as well as the classes that the task uses for inputs and outputs.
 * <p>
 * Important note: the hashCode() and equals() methods required manual modification because a Class
 * doesn't implement hashCode() or equals(). Consequently the getCanonicalName() method is used to
 * extract something from the Class instance that can be compared via the String hashCode() and
 * equals() methods.
 *
 * @author Todd Klaus
 * @author PT
 */
public class TaskConfigurationManager implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(TaskConfigurationManager.class);
    private static final long serialVersionUID = 20220817L;
    private static final String PERSISTED_FILE_NAME = ".task-configuration.ser";
    public static final String LOCK_FILE_NAME = ".lock";

    private transient File taskDir = null;

    private final List<Set<String>> filesForSubtasks = new ArrayList<>();
    private Class<? extends PipelineInputs> inputsClass;
    private Class<? extends PipelineOutputs> outputsClass;

    private int subtaskCount = 0;

    public TaskConfigurationManager() {
    }

    public TaskConfigurationManager(File taskDir) {
        this.taskDir = taskDir;
    }

    /**
     * Construct the current subtask directory and increment the subtask index. Add the subtask
     * files. Put the .lock file into the subtask directory.
     */
    public void addFilesForSubtask(Set<String> files) {

        File subTaskDirectory = subtaskDirectory(taskDir, subtaskCount);
        filesForSubtasks.add(files);
        try {
            new File(subTaskDirectory, LOCK_FILE_NAME).createNewFile();
        } catch (IOException e) {
            throw new PipelineException(
                "Unable to create lock file in subtask " + subtaskCount + " directory");
        }
        subtaskCount++;
    }

    /**
     * Return the current subtask directory
     *
     * @return
     */
    public File subtaskDirectory() {
        return TaskConfigurationManager.subtaskDirectory(taskDir, subtaskCount);
    }

    public boolean contains(int subTaskNumber) {
        return subtaskCount >= subTaskNumber + 1;
    }

    public int numSubTasks() {
        return subtaskCount;
    }

    public int numInputs() {
        int numInputs = 0;
        numInputs = subtaskCount;
        log.info("numSubTaskInSeq: " + numInputs);
        return numInputs;
    }

    public boolean isEmpty() {
        return subtaskCount == 0;
    }

    /**
     * Checks to make sure that the number of inputs (based on count of sub-task UOWs) and the
     * number of sub-tasks (based on the sub-task pair list) match.
     */
    void validate() {

        int numSubTasks = numSubTasks();
        int numInputs = numInputs();

        // Check that the # of sub-tasks in the list of Pairs equals the number of inputs
        // added to the UOW list. Note that this can be true but the Pairs can still be wrong
        // in one of two ways: a duplicate combined with an omission in the Pairs (i.e.,
        // sub-task 0 never processed, sub-task 1 processed in 2 of the Pairs); an offset of the
        // pairs (i.e., sub-tasks run from 0 to 20 but the Pairs run from 1 to 21).
        if (numInputs != numSubTasks) {
            String message = String.format(
                "Number of sub-tasks(%d) does not match number of inputs (%d)", numSubTasks,
                numInputs);
            log.error(message);
            throw new PipelineException(message);
        }

    }

    @Override
    public String toString() {
        return new String("SINGLE:[" + 0 + "," + (subtaskCount - 1) + "]");
    }

    public void persist() {
        persist(getTaskDir());
    }

    public void persist(File dir) {
        File dest = persistedFile(dir);
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dest))) {
            log.info("Persisting inputs metadata to: " + dest);
            oos.writeObject(this);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to persist to: " + dir + ", caught: " + e,
                e);
        }
    }

    public static TaskConfigurationManager restore(File taskDir) {
        File src = persistedFile(taskDir);
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(src))) {
            log.info("Restoring outputs metadata from: " + src);

            TaskConfigurationManager s = (TaskConfigurationManager) ois.readObject();
            s.taskDir = taskDir;

            return s;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Unable to read persisted object from: " + taskDir + ", caught: " + e, e);
        }
    }

    public static Set<String> restoreAndRetrieveFilesForSubtask(File taskDir, int subtaskIndex) {
        return TaskConfigurationManager.restore(taskDir).filesForSubtask(subtaskIndex);
    }

    public static boolean isPersistedInputsHandlerPresent(File taskDir) {
        return persistedFile(taskDir).exists();
    }

    public static File persistedFile(File taskDir) {
        return new File(taskDir, PERSISTED_FILE_NAME);
    }

    /**
     * Return a collection of all sub-task directories for this InputsHandler
     *
     * @return
     */
    public List<File> allSubTaskDirectories() {
        List<File> subTaskDirs = new LinkedList<>();
        int numSubTasks = numSubTasks();
        for (int subTaskIndex = 0; subTaskIndex < numSubTasks; subTaskIndex++) {
            subTaskDirs.add(subtaskDirectory(taskDir, subTaskIndex));
        }
        return subTaskDirs;
    }

    /**
     * For PI use only. {@link PipelineModule} classes should use subTaskDirectory(), above.
     * <p>
     * Create the sub-task working directory (if necessary) and return the path
     *
     * @param subTaskIndex
     * @return
     */
    public static File subtaskDirectory(File taskWorkingDir, int subTaskIndex) {
        try {
            File subTaskDir = null;

            subTaskDir = new File(taskWorkingDir, "st-" + subTaskIndex);

            // ensure that the directory exists
            if (!subTaskDir.exists()) {
                FileUtils.forceMkdir(subTaskDir);
            }

            return subTaskDir;
        } catch (IOException e) {
            throw new PipelineException("Failed to create sub-task dir: " + e, e);
        }
    }

    public Set<String> filesForSubtask(int subtaskNumber) {
        return filesForSubtasks.get(subtaskNumber);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + subtaskCount;
        result = prime * result + (filesForSubtasks == null ? 0 : filesForSubtasks.hashCode());
        result = prime * result
            + (inputsClass == null ? 0 : inputsClass.getCanonicalName().hashCode());
        result = prime * result
            + (outputsClass == null ? 0 : outputsClass.getCanonicalName().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TaskConfigurationManager other = (TaskConfigurationManager) obj;
        if (subtaskCount != other.subtaskCount) {
            return false;
        }
        if (!Objects.equals(filesForSubtasks, other.filesForSubtasks)) {
            return false;
        }
        if (inputsClass == null) {
            if (other.inputsClass != null) {
                return false;
            }
        } else if (!inputsClass.getCanonicalName().equals(other.inputsClass.getCanonicalName())) {
            return false;
        }
        if (outputsClass == null) {
            if (other.outputsClass != null) {
                return false;
            }
        } else if (!outputsClass.getCanonicalName().equals(other.outputsClass.getCanonicalName())) {
            return false;
        }
        return true;
    }

    public File getTaskDir() {
        return taskDir;
    }

    public int getSubtaskCount() {
        return subtaskCount;
    }

    public void setInputsClass(Class<? extends PipelineInputs> inputsClass) {
        this.inputsClass = inputsClass;
    }

    public Class<? extends PipelineInputs> getInputsClass() {
        return inputsClass;
    }

    public void setOutputsClass(Class<? extends PipelineOutputs> outputsClass) {
        this.outputsClass = outputsClass;
    }

    public Class<? extends PipelineOutputs> getOutputsClass() {
        return outputsClass;
    }

}
