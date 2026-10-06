package com.kernelbreach.core.content

/** Severity of a content validation finding. */
enum class Severity {
    /** Breaks import. Fatal in debug builds; the module is skipped in release. */
    ERROR,

    /** Content is importable but suspect (e.g. plan/file title drift). */
    WARNING,

    /** Informational (e.g. a module has no authored content yet). */
    INFO,
}

/** One validation finding, scoped to a module (or the whole curriculum). */
data class ValidationIssue(
    val severity: Severity,
    /** Module code, or "curriculum" / a path key for cross-file issues. */
    val scope: String,
    val message: String,
) {
    override fun toString(): String = "[$severity] $scope: $message"
}

/** The outcome of validating + assembling the bundled content. */
data class ImportResult(
    val curriculum: com.kernelbreach.core.model.Curriculum,
    val issues: List<ValidationIssue>,
    val contentHash: String,
) {
    val errors: List<ValidationIssue> get() = issues.filter { it.severity == Severity.ERROR }
    val warnings: List<ValidationIssue> get() = issues.filter { it.severity == Severity.WARNING }
    val hasErrors: Boolean get() = errors.isNotEmpty()

    /** Module codes that produced at least one ERROR and were skipped. */
    val failedModuleCodes: Set<String> get() = errors.map { it.scope }.toSet()
}
