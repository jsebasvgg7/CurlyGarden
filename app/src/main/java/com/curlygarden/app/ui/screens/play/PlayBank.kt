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
    val dailyQuestions: List<String> = listOf(
        "¿Cuál fue tu mejor momento de esta semana?",
        "¿Qué cosa pequeña te hizo sonreír hoy?",
        "¿Qué canción no puedes dejar de escuchar últimamente?",
        "¿Qué aprendiste recientemente que te sorprendió?",
        "¿Cuál es tu recuerdo favorito de tu infancia?",
        "¿Qué lugar te gustaría visitar el próximo año?",
        "¿Qué comida te hace sentir en casa?",
        "¿Qué es lo que más esperas de este mes?",
        "¿Qué hábito nuevo te gustaría empezar?",
        "¿Cómo sería tu día perfecto de descanso?",
        "¿Qué película o serie podrías ver otra vez sin aburrirte?",
        "¿Cuál es el mejor consejo que has recibido?",
        "¿Qué te da energía cuando el día se pone pesado?",
        "¿Qué te gustaría aprender a hacer bien?",
        "¿Qué logro reciente te enorgullece, aunque sea pequeño?",
        "¿Cuál es tu rincón favorito para estar en calma?",
        "¿Qué olor te trae un buen recuerdo?",
        "¿Qué plan te gustaría que hiciéramos pronto?",
        "¿Qué es lo más raro que has comido y te gustó?",
        "¿De qué tema podrías hablar durante horas?",
        "¿Qué te gustaría que el otro supiera de tu día?",
        "¿Qué juego de tu infancia extrañas?",
        "¿Qué te hace reír sin remedio?",
        "¿Cuál es tu forma favorita de pasar una tarde de lluvia?",
        "¿Qué objeto tienes que nunca regalarías?",
        "¿Qué cambiarías de tu rutina si pudieras?",
        "¿Qué le agradecerías a alguien hoy?",
        "¿Cuál es la mejor decisión que has tomado este año?",
        "¿Qué paisaje te da más paz?",
        "¿Qué sueño pequeño te gustaría cumplir este año?"
    )

    val dailyActions: List<String> = listOf(
        "Envíale al otro una foto de algo que te recuerde a esa persona.",
        "Cuéntale al otro algo que aprendiste hoy.",
        "Comparte una canción que te acompañe hoy.",
        "Deja una carta en Hablar con tres cosas que agradeces del otro.",
        "Manda una nota de voz contando cómo va tu día.",
        "Comparte una foto de lo que estás viendo ahora mismo.",
        "Recomiéndale al otro algo para ver, leer o escuchar.",
        "Cuéntale un recuerdo que tengas de los dos.",
        "Dile algo que admires de esa persona.",
        "Comparte tu foto favorita de la semana.",
        "Propón un plan sencillo para el fin de semana.",
        "Deja una carta en Hablar contando algo que te hizo reír hoy.",
        "Comparte una receta o un plato que quieras probar.",
        "Cuéntale una meta pequeña que tengas.",
        "Envía una foto del rincón donde estás hoy.",
        "Haz un dibujo rápido de tu día y compártelo.",
        "Pregúntale al otro cómo se siente hoy y escucha la respuesta.",
        "Comparte un dato curioso que acabas de aprender.",
        "Cuéntale cuál fue lo mejor y lo peor de tu día.",
        "Envía un meme o una imagen que te haga acordar del otro.",
        "Comparte tu lugar favorito del barrio o de la ciudad.",
        "Dile algo que te gustaría hacer con el otro este mes.",
        "Comparte con una foto lo que vas a comer hoy.",
        "Escribe tres palabras que describan tu semana.",
        "Comparte una foto de tu infancia y cuenta de qué se trata.",
        "Cuéntale una anécdota graciosa de tu vida.",
        "Pídele al otro un consejo sobre algo pequeño.",
        "Dale las gracias por algo concreto.",
        "Cuéntale algo que te preocupe o te ilusione esta semana.",
        "Elige una palabra del día y úsala en una carta en Hablar."
    )

    val thisOrThat: List<ThisOrThatPair> = listOf(
        ThisOrThatPair("Playa", "Montaña"),
        ThisOrThatPair("Café", "Té"),
        ThisOrThatPair("Cine en casa", "Salir al cine"),
        ThisOrThatPair("Dulce", "Salado"),
        ThisOrThatPair("Madrugar", "Trasnochar"),
        ThisOrThatPair("Verano", "Invierno"),
        ThisOrThatPair("Perros", "Gatos"),
        ThisOrThatPair("Pizza", "Hamburguesa"),
        ThisOrThatPair("Leer un libro", "Ver una película"),
        ThisOrThatPair("Ciudad", "Campo"),
        ThisOrThatPair("Música en vivo", "Fiesta en casa"),
        ThisOrThatPair("Cocinar", "Pedir domicilio"),
        ThisOrThatPair("Viajar en avión", "Viajar por carretera"),
        ThisOrThatPair("Planear todo", "Improvisar"),
        ThisOrThatPair("Día de lluvia", "Día de sol"),
        ThisOrThatPair("Helado", "Torta"),
        ThisOrThatPair("Mensaje de texto", "Llamada"),
        ThisOrThatPair("Series largas", "Películas"),
        ThisOrThatPair("Aventura", "Descanso"),
        ThisOrThatPair("Mañana", "Noche"),
        ThisOrThatPair("Bailar", "Cantar"),
        ThisOrThatPair("Fotos", "Videos"),
        ThisOrThatPair("Hotel", "Cabaña"),
        ThisOrThatPair("Día en casa", "Día afuera"),
        ThisOrThatPair("Comida casera", "Restaurante"),
        ThisOrThatPair("Jugo natural", "Gaseosa"),
        ThisOrThatPair("Frío", "Calor"),
        ThisOrThatPair("Videojuegos", "Juegos de mesa"),
        ThisOrThatPair("Leer", "Escuchar podcasts"),
        ThisOrThatPair("Lo clásico", "Lo nuevo"),
        ThisOrThatPair("Ahorrar", "Darse un gusto"),
        ThisOrThatPair("Desayuno grande", "Desayuno ligero"),
        ThisOrThatPair("Pasta", "Arroz"),
        ThisOrThatPair("Escribir a mano", "Escribir en el teléfono"),
        ThisOrThatPair("Mar", "Río"),
        ThisOrThatPair("Carro", "Bicicleta"),
        ThisOrThatPair("Cena tranquila", "Picnic"),
        ThisOrThatPair("Chiste malo", "Meme"),
        ThisOrThatPair("Sorpresa", "Plan avisado"),
        ThisOrThatPair("Regalo comprado", "Regalo hecho a mano")
    )

    val knowMe: List<KnowMeQuestion> = listOf(
        KnowMeQuestion(
            "Desayuno favorito",
            listOf("Huevos", "Pan con café", "Frutas", "Avena")
        ),
        KnowMeQuestion(
            "Color favorito",
            listOf("Azul", "Verde", "Morado", "Rojo")
        ),
        KnowMeQuestion(
            "Género de película favorito",
            listOf("Acción", "Comedia", "Terror", "Drama")
        ),
        KnowMeQuestion(
            "Música favorita",
            listOf("Pop", "Rock", "Reguetón", "Salsa")
        ),
        KnowMeQuestion(
            "Lugar ideal para vivir",
            listOf("Ciudad grande", "Pueblo tranquilo", "Cerca del mar", "En la montaña")
        ),
        KnowMeQuestion(
            "Plan ideal de sábado",
            listOf("Salir con amigos", "Maratón de series", "Paseo al aire libre", "Cocinar algo rico")
        ),
        KnowMeQuestion(
            "Postre favorito",
            listOf("Helado", "Torta", "Frutas", "Chocolate")
        ),
        KnowMeQuestion(
            "Superpoder que elegiría",
            listOf("Volar", "Ser invisible", "Leer la mente", "Viajar en el tiempo")
        ),
        KnowMeQuestion(
            "Clima favorito",
            listOf("Calor", "Lluvia", "Frío", "Fresco")
        ),
        KnowMeQuestion(
            "Mascota ideal",
            listOf("Perro", "Gato", "Un pez", "Ninguna")
        ),
        KnowMeQuestion(
            "Hora del día favorita",
            listOf("Amanecer", "Mediodía", "Tarde", "Noche")
        ),
        KnowMeQuestion(
            "Comida favorita",
            listOf("Pizza", "Pasta", "Hamburguesa", "Comida casera")
        ),
        KnowMeQuestion(
            "Forma favorita de relajarse",
            listOf("Dormir", "Escuchar música", "Salir a caminar", "Ver videos")
        ),
        KnowMeQuestion(
            "Viaje soñado",
            listOf("Playa", "Ciudad histórica", "Montaña", "Selva")
        ),
        KnowMeQuestion(
            "Bebida favorita",
            listOf("Café", "Té", "Jugo", "Gaseosa")
        ),
        KnowMeQuestion(
            "Deporte que más disfruta",
            listOf("Fútbol", "Natación", "Correr", "Ninguno")
        ),
        KnowMeQuestion(
            "Tipo de historia favorita",
            listOf("Fantasía", "Misterio", "Romance", "Ciencia ficción")
        ),
        KnowMeQuestion(
            "Cena ideal",
            listOf("Algo ligero", "Comida rápida", "Plato fuerte", "Picar de todo")
        ),
        KnowMeQuestion(
            "Lo que más le da miedo",
            listOf("Las alturas", "Los bichos", "La oscuridad", "Hablar en público")
        ),
        KnowMeQuestion(
            "Forma favorita de comunicarse",
            listOf("Mensaje de texto", "Nota de voz", "Llamada", "En persona")
        ),
        KnowMeQuestion(
            "Estilo de ropa",
            listOf("Cómodo", "Elegante", "Deportivo", "Colorido")
        ),
        KnowMeQuestion(
            "Vacaciones ideales",
            listOf("Aventura", "Descanso total", "Cultura y museos", "Visitar familia")
        ),
        KnowMeQuestion(
            "Mejor momento para concentrarse",
            listOf("Mañana temprano", "Tarde", "Noche", "Madrugada")
        ),
        KnowMeQuestion(
            "Plan de día de lluvia",
            listOf("Dormir", "Ver una película", "Leer", "Cocinar")
        ),
        KnowMeQuestion(
            "Sabor de helado favorito",
            listOf("Chocolate", "Vainilla", "Fresa", "Frutas tropicales")
        ),
        KnowMeQuestion(
            "Lo primero que revisa al despertar",
            listOf("El teléfono", "La hora", "El clima", "Nada")
        ),
        KnowMeQuestion(
            "Regalo ideal",
            listOf("Una experiencia", "Algo hecho a mano", "Algo útil", "Una sorpresa de comida")
        ),
        KnowMeQuestion(
            "Tipo de fiesta favorita",
            listOf("Grande y con música", "Reunión pequeña", "Salida a cenar", "Mejor en casa")
        ),
        KnowMeQuestion(
            "Qué haría con un día libre",
            listOf("Salir de paseo", "Dormir todo el día", "Ver series", "Aprender algo nuevo")
        ),
        KnowMeQuestion(
            "Paisaje favorito",
            listOf("Mar", "Montaña", "Bosque", "Ciudad iluminada")
        )
    )
}
