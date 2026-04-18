package com.secureexam.browser.domain.model

/**
 * Represents the current state of an exam session.
 */
enum class ExamSessionState {
    IDLE,           // No exam in progress
    CONFIGURING,    // Teacher setting up config
    SECURITY_CHECK, // Running security checks
    READY,          // Ready to start exam
    ACTIVE,         // Exam in progress (kiosk mode)
    PAUSED,         // Exam paused (violation detected)
    SUBMITTED,      // Exam submitted by student
    ENDED           // Session ended by invigilator
}

/**
 * Represents an active exam session.
 */
data class ExamSession(
    val id: String,
    val config: ExamConfig,
    val state: ExamSessionState = ExamSessionState.IDLE,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val violations: List<Violation> = emptyList(),
    val moodleToken: String? = null,
    val studentId: String? = null
) {
    val isActive: Boolean
        get() = state == ExamSessionState.ACTIVE

    val duration: Long
        get() = if (startTime != null && endTime != null) {
            endTime - startTime
        } else if (startTime != null) {
            System.currentTimeMillis() - startTime
        } else {
            0L
        }

    fun withState(newState: ExamSessionState): ExamSession {
        return copy(state = newState)
    }

    fun start(): ExamSession {
        return copy(
            state = ExamSessionState.ACTIVE,
            startTime = System.currentTimeMillis()
        )
    }

    fun end(): ExamSession {
        return copy(
            state = ExamSessionState.ENDED,
            endTime = System.currentTimeMillis()
        )
    }

    fun addViolation(violation: Violation): ExamSession {
        return copy(violations = violations + violation)
    }
}
