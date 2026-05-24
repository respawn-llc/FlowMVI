package pro.respawn.flowmvi.api

/**
 * A DSL marker for the FlowMVI DSL.
 * Defines a contract for calling the DSL functions.
 */
@DslMarker
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPEALIAS,
)
public annotation class FlowMVIDSL
