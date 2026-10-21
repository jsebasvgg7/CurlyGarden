package com.curlygarden.app.ui.screens.play

data class ThisOrThatPair(
    val a: String,
    val b: String
)

data class KnowMeQuestion(
    val prompt: String,
    val options: List<String>
)

object PlayBank {
    val dailyQuestions: List<String> = emptyList()
    val dailyActions: List<String> = emptyList()
    val thisOrThat: List<ThisOrThatPair> = emptyList()
    val knowMe: List<KnowMeQuestion> = emptyList()
}
