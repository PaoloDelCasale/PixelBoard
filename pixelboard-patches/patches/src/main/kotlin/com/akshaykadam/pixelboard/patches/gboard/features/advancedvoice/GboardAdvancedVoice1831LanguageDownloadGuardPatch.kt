package com.akshaykadam.pixelboard.patches.gboard.features.advancedvoice

import com.akshaykadam.pixelboard.patches.shared.ExternalLabel
import com.akshaykadam.pixelboard.patches.shared.MutableMethod
import com.akshaykadam.pixelboard.patches.shared.addInstructionsWithLabels
import com.akshaykadam.pixelboard.patches.shared.bytecodePatch
import com.akshaykadam.pixelboard.patches.shared.Constants.COMPATIBILITY_GBOARD
import com.akshaykadam.pixelboard.patches.gboard.shared.GboardMethodTarget
import com.akshaykadam.pixelboard.patches.gboard.shared.findMutableMethodOrNull
import com.akshaykadam.pixelboard.patches.gboard.shared.gboardPatchesExtensionCarrierPatch
import com.akshaykadam.pixelboard.patches.gboard.shared.isMethodReference
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeAbiCatalog
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import com.akshaykadam.pixelboard.patches.gboard.shared.runtimeabi.RuntimeCallId
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private object GboardLanguageDownloadGuardTargets {
    /** LanguageDownloadQueue.enqueue(languageTag, source, durationConsumer) in 18.3.1 release. */
    val languageDownloadEnqueue = GboardMethodTarget(
        classType = "Lsnm;",
        name = "c",
        parameterTypes = listOf(
            "Ljava/lang/String;",
            "Lsoe;",
            "Ljava/util/function/Consumer;",
        ),
        returnType = "V",
    )
}

private const val ENQUEUE_PARAMETER_REGISTERS = 4 // this + 3 arguments
private const val GUARD_CONTINUE_LABEL = "pixelboard_language_download_continue"

internal val gboardAdvancedVoice1831LanguageDownloadGuardPatch = bytecodePatch(
    description = "Back off repeated language pack download requests from the eligibility " +
        "checker, which otherwise retry every few milliseconds when the speech service " +
        "refuses silent downloads.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch)

    execute {
        val method = findMutableMethodOrNull(GboardLanguageDownloadGuardTargets.languageDownloadEnqueue)
            ?: return@execute
        if (!method.looksLikeLanguageDownloadEnqueue()) {
            return@execute
        }
        method.applyLanguageDownloadGuard()
    }
}

private fun MutableMethod.looksLikeLanguageDownloadEnqueue(): Boolean {
    val instructions = implementation?.instructions ?: return false
    return instructions.any { instruction ->
        ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string ==
            "languageTag"
    }
}

private fun MutableMethod.applyLanguageDownloadGuard() {
    val call = RuntimeCallId.ADVANCED_VOICE_RUNTIME_BEFORE_LANGUAGE_DOWNLOAD
    val reference = RuntimeAbiCatalog.abi(call).reference
    val implementation = implementation ?: error("No language download enqueue implementation")
    val existing = implementation.instructions.count { it.isMethodReference(reference) }
    if (existing != 0) {
        check(existing == 1) { "Duplicate language download guards" }
        return
    }
    check(implementation.registerCount - ENQUEUE_PARAMETER_REGISTERS >= 1) {
        "Language download enqueue has no free local register for the guard"
    }
    val continuation = implementation.instructions.firstOrNull() ?: return
    addInstructionsWithLabels(
        0,
        """
            ${RuntimeCallEmitter.invoke(call, "p1, p2")}
            move-result v0
            if-eqz v0, :$GUARD_CONTINUE_LABEL
            return-void
        """.trimIndent(),
        ExternalLabel(GUARD_CONTINUE_LABEL, continuation),
    )
}
