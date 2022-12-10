package gov.nasa.ziggy.services.security;

/**
 * List of privileges.
 *
 * @author Todd Klaus
 */
public enum Privilege {
    // PI privileges

    /** Launch and manage pipeline instances */
    PIPELINE_OPERATIONS("Pipeline Ops"),

    /** View pipeline instance/task data, parameters, cluster status */
    PIPELINE_MONITOR("Pipeline Mon"),

    /**
     * Create and modify Pipeline configurations (pipelines, modules, parameter sets, triggers,
     * etc.)
     */
    PIPELINE_CONFIG("Pipeline Config"),

    /** Create and modify users, reset passwords */
    USER_ADMIN("User Admin"),

    // TODO Delete MR privileges?

    // MR privileges

    // Role: reports
    MR_PERM_REPORT_ALERTS("mr.alerts"),
    MR_PERM_REPORT_BAD_PIXELS("mr.bad-pixels"),
    MR_PERM_REPORT_CONFIG_MAP("mr.config-map"),
    MR_PERM_REPORT_DATA_COMPRESSION("mr.data-compression"),
    MR_PERM_REPORT_DATA_GAP("mr.data-gap"),
    MR_PERM_REPORT_DR_SUMMARY("mr.dr-summary"),
    MR_PERM_REPORT_FC("mr.fc"),
    MR_PERM_REPORT_GENERIC_REPORT("mr.generic-report"),
    MR_PERM_REPORT_HUFFMAN_TABLES("mr.huffman-tables"),
    MR_PERM_REPORT_PI_PROCESSING("mr.pipeline-processing"),
    MR_PERM_REPORT_PI_INSTANCE_DETAIL("mr.pipeline-instance-detail"),
    MR_PERM_REPORT_REQUANT_TABLES("mr.requantization-tables"),
    MR_PERM_REPORT_TAD_CCD_MODULE_OUTPUT("mr.tad-ccd-module-output"),
    MR_PERM_REPORT_TAD_SUMMARY("mr.tad-summary"),

    /* OpenEdit standard permissions. */

    // Role: editors
    MR_BLOG("oe.blog"),
    MR_EDIT("oe.edit"),
    MR_EDIT_FTPUPLOAD("oe.edit.ftpUpload"),
    MR_EDIT_UPLOAD("oe.edit.upload"),

    // Role: intranet
    MR_FILEMANAGER("oe.filemanager"),
    MR_INTRANET("oe.intranet"),

    // Role: administrators
    MR_ADMINISTRATION("oe.administration"),
    MR_EDIT_APPROVES("oe.edit.approves"),
    MR_EDIT_APPROVE_LEVEL1("oe.edit.approve.level1"),
    MR_EDIT_APPROVE_LEVEL2("oe.edit.approve.level2"),
    MR_EDIT_APPROVE_LEVEL3("oe.edit.approve.level3"),
    MR_EDIT_DIRECTEDIT("oe.edit.directedit"),
    MR_EDIT_DRAFTMODE("oe.edit.draftmode"),
    MR_EDIT_EDITOR_ADVANCED("oe.edit.editor.advanced"),
    MR_EDIT_EDITSLANGUAGES("oe.edit.editslanguages"),
    MR_EDIT_LINKS("oe.edit.links"),
    MR_EDIT_MANAGENOTIFICATIONS("oe.edit.managenotifications"),
    MR_EDIT_NOTIFY("oe.edit.notify"),
    MR_EDIT_RECENTEDITS("oe.edit.recentedits"),
    MR_EDIT_SETTINGS_ADVANCED("oe.edit.settings.advanced"),
    MR_EDIT_UPDATE("oe.edit.update"),
    MR_ERROR_NOTIFY("oe.error.notify"),
    MR_USERMANAGER("oe.usermanager");

    private String displayName;

    Privilege(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
