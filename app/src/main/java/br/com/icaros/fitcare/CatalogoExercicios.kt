package br.com.icaros.fitcare

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape

data class ItemCatalogoExercicio(
    val nome: String,
    val grupo: String,
    val videoUrl: String
)

object CatalogoExerciciosStorage {

    private val SHORTS_IDS_MAP = mapOf(
        "abdominal completo com joelhos estendidos" to "Q4cd3l9tAxE",
        "abdm completo com joelhos estendidos" to "Q4cd3l9tAxE",
        "cadeira flexora" to "siiTqRvLQgA",
        "passadas halter" to "yNldoM28Dp0",
        "elevação pélvica máquina hammer" to "3Qu8iCxQOC0",
        "prancha com elevação de perna" to "rChIiS9dBHc",
        "isometria em l" to "biXVs0YRvVk",
        "escada" to "7ToIwnOIF0Q",
        "elíptico" to "UfEB5fG871c",
        "burpee com agachamento" to "PaqnXQ6lN2A",
        "polichinelo com prancha" to "1w2PQQzlQk0",
        "pulo lateral" to "C-Pos61X6NU",
        "burpee over" to "S7FAo7HG8g4",
        "prancha com rotação de quadril" to "VpWu34kmU1Q",
        "v-up + remador" to "CjMLPbEYmPI",
        "cadeira flexora unilateral" to "_nTNhwk69ZU",
        "prancha com base no cotovelo" to "0nyqBKMAFwQ",
        "prancha lateral" to "dWAzUEzl2ss",
        "corrida com joelho alto" to "jv_YU1DXfws",
        "polichinelo" to "rsVqgClekSA",
        "polichinelo adaptado" to "2-7eJlLrhJM",
        "prancha com mão no ombro" to "PwxK6KH8g9o",
        "corrida no lugar com joelho baixo" to "MVFFkMhYiAo",
        "elevação de quadril unilateral" to "RxiZe7cM7Go",
        "elevação de quadril" to "EuR8khoZScw",
        "agachamento unilateral com apoio" to "Y_VBJuKG8TI",
        "prancha mão no pé" to "BhxW4VtCoHo",
        "burpee" to "-9NB8pLgf7M",
        "mountain climbers" to "xCaLAO5shTw",
        "agachamento com salto" to "FLCHGa3FXWs",
        "crucifixo curvado" to "mUST8bUbkmc",
        "rosca barra w" to "F1RKQCaDELY",
        "rosca direta barra w" to "F1RKQCaDELY",
        "abdominal convencional com mão na frente" to "OajnonOVcbU",
        "panturrilha unilateral" to "X0T4Ug-Ii0w",
        "panturrilha livre em pé unilateral" to "X0T4Ug-Ii0w",
        "deadlift unilateral alternado" to "II44g1jkZaQ"
    )

    private fun gerarLinkVideoDemonstrativo(context: Context, nomeExercicio: String): String {
        val chave = nomeExercicio.trim().lowercase()
        val prefsVideos =
            context.getSharedPreferences("fitcare_catalogo_videos_prof", Context.MODE_PRIVATE)
        val linkSalvoProfessor = prefsVideos.getString(chave, null)
        if (linkSalvoProfessor != null && !linkSalvoProfessor.contains("%3A") && linkSalvoProfessor.length > 22) {
            return linkSalvoProfessor
        }
        val idShort = SHORTS_IDS_MAP[chave]
        if (!idShort.isNullOrBlank()) {
            return "https:" + "//www.youtube.com/shorts/" + idShort
        }
        return ""
    }

    fun salvarVideoExercicio(context: Context, nomeExercicio: String, videoUrl: String) {
        val chave = nomeExercicio.trim().lowercase()
        if (chave.isBlank()) return
        val prefsVideos =
            context.getSharedPreferences("fitcare_catalogo_videos_prof", Context.MODE_PRIVATE)
        prefsVideos.edit().putString(chave, videoUrl.trim()).apply()
    }

    val GRUPOS_MUSCULARES = listOf(
        "Peito",
        "Costas",
        "Pernas",
        "Glúteos e Quadril",
        "Ombros",
        "Bíceps",
        "Tríceps",
        "Abdômen e Core",
        "Mobilidade e Alongamento",
        "Cardio e Funcional"
    )

    fun editarExercicioCompleto(
        context: Context,
        nomeOriginal: String,
        novoNome: String,
        novaCategoria: String,
        novoVideoUrl: String
    ) {
        val origLimpo = nomeOriginal.trim()
        val novoLimpo = novoNome.trim()
        if (novoLimpo.isBlank()) return

        val prefs =
            context.getSharedPreferences("fitcare_catalogo_exercicios", Context.MODE_PRIVATE)
        val jsonPers = prefs.getString("personalizados", "[]") ?: "[]"
        val personalizados = try {
            val arr = JSONArray(jsonPers)
            (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
                .toMutableSet()
        } catch (_: Exception) {
            mutableSetOf()
        }

        val jsonOcultos = prefs.getString("ocultos", "[]") ?: "[]"
        val ocultos = try {
            val arr = JSONArray(jsonOcultos)
            (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
                .toMutableSet()
        } catch (_: Exception) {
            mutableSetOf()
        }

        if (!origLimpo.equals(novoLimpo, ignoreCase = true)) {
            personalizados.removeAll { it.equals(origLimpo, ignoreCase = true) }
            ocultos.add(origLimpo.lowercase())
            personalizados.add(novoLimpo)
        } else if (NOMES_EXERCICIOS_SISTEMA.none { it.equals(novoLimpo, ignoreCase = true) }) {
            personalizados.add(novoLimpo)
        }

        prefs.edit()
            .putString("personalizados", JSONArray(personalizados.toList()).toString())
            .putString("ocultos", JSONArray(ocultos.toList()).toString())
            .apply()

        val prefsCat =
            context.getSharedPreferences("fitcare_catalogo_categorias_prof", Context.MODE_PRIVATE)
        if (novaCategoria.isNotBlank()) {
            prefsCat.edit().putString(novoLimpo.lowercase(), novaCategoria.trim()).apply()
        }

        salvarVideoExercicio(context, novoLimpo, novoVideoUrl)
    }


    private fun detectarGrupoMuscular(nome: String): String {
        val n = nome.lowercase()
        return when {
            n.contains("supino") || n.contains("crucifixo") || n.contains("crossover") || n.contains(
                "peitoral"
            ) || n.contains("apoio solo") -> "Peito"

            n.contains("puxada") || n.contains("remada") || n.contains("barra fixa") || n.contains("pull") || n.contains(
                "dorsal"
            ) -> "Costas"

            n.contains("agachamento") || n.contains("leg press") || n.contains("extensora") || n.contains(
                "flexora"
            ) || n.contains("avanço") || n.contains("afundo") || n.contains("passada") || n.contains(
                "hack"
            ) || n.contains("squat") || n.contains("stiff") || n.contains("terra") || n.contains("deadlift") || n.contains(
                "panturrilha"
            ) || n.contains("tibial") -> "Pernas"

            n.contains("glúteo") || n.contains("glute") || n.contains("elevação pélvica") || n.contains(
                "elevação de quadril"
            ) || n.contains("abdução") || n.contains("abdutora") || n.contains("adutora") || n.contains(
                "extensão quadril"
            ) || n.contains("extensão de quadril") || n.contains("3 apoios") || n.contains("ponte") -> "Glúteos e Quadril"

            n.contains("desenvolvimento") || n.contains("elevação lateral") || n.contains("elevação frontal") || n.contains(
                "ombro"
            ) || n.contains("encolhimento") -> "Ombros"

            n.contains("rosca") -> "Bíceps"
            n.contains("tríceps") || n.contains("triceps") || n.contains("paralela") -> "Tríceps"
            n.contains("abdominal") || n.contains("abd ") || n.contains("abdm") || n.contains("prancha") || n.contains(
                "isometria em l"
            ) || n.contains("v-up") || n.contains("core") || n.contains("lombar") || n.contains("perdigueiro") -> "Abdômen e Core"

            n.contains("alongamento") || n.contains("mobilidade") || n.contains("mob ") || n.contains(
                "mob."
            ) || n.contains("liberação") -> "Mobilidade e Alongamento"

            else -> "Cardio e Funcional"
        }
    }

    private val NOMES_EXERCICIOS_SISTEMA = listOf(
        "3 Apoios Perna Estendida",
        "3 Apoios Perna Flexionada",
        "Abdominal Banco Declinado",
        "Abdominal Banco Inclinado",
        "Abdominal Borboleta",
        "Abdominal Bosu Flexionado",
        "Abdominal Bosu Solo",
        "Abdominal Canivete Medicine Ball",
        "Abdominal Completo com Joelhos Estendidos",
        "Abdominal Completo Máquina Hammer",
        "Abdominal Completo Máquina Nautilus",
        "Abdominal Completo Solo",
        "Abdominal Convencional com Mão na Frente",
        "Abdominal Curto Solo",
        "Abdominal Curto Solo Anilha",
        "Abdominal Curto Solo Barra",
        "Abdominal Escalador",
        "Abdominal Infra Banco Inclinado",
        "Abdominal Infra Suspenso",
        "Abdominal Inverso Solo Estendido",
        "Abdominal Inverso Solo Flexionado",
        "Abdominal Lateral Solo",
        "Abdominal Medicine Ball",
        "Abdominal Oblíquo Máquina Hammer",
        "Abdominal Oblíquo Solo",
        "Abdominal Remador Banco",
        "Abdominal Remador Solo",
        "Abdominal Rodinha",
        "Abdominal Supra Máquina Nautilus",
        "Abdominal Supra no Banco Declinado",
        "Abdução Deitado Caneleira",
        "Abdução Deitado Mini Band",
        "Abdução em Pé Elástico",
        "Abdução em Pé Máquina Booty Builder",
        "Abdução Pulley",
        "Afundo no Espaldar",
        "Agachamento 2 Halteres",
        "Agachamento Balance Disc",
        "Agachamento Barra Frontal",
        "Agachamento Barra Hexagonal",
        "Agachamento Bosu Invertido",
        "Agachamento Búlgaro",
        "Agachamento Búlgaro Máquina",
        "Agachamento com Desenvolvimento Halter",
        "Agachamento com Desenvolvimento Kettlebell",
        "Agachamento com Halter",
        "Agachamento com Salto",
        "Agachamento Isométrico",
        "Agachamento Livre",
        "Agachamento Livre Barra",
        "Agachamento Pêndulo Realleader",
        "Agachamento Peso Corporal",
        "Agachamento Pliométrico",
        "Agachamento Senta e Levanta",
        "Agachamento Smith",
        "Agachamento Sumô Belt Squat",
        "Agachamento Sumô Halter",
        "Agachamento Sumô Isométrico",
        "Agachamento Sumô Kettlebell",
        "Agachamento Sumô Smith",
        "Agachamento Taça Halter",
        "Agachamento TRX",
        "Agachamento Unilateral com Apoio",
        "Air Dyne",
        "Alongamento Adutor Banco",
        "Alongamento Anterior Máquina",
        "Alongamento Cross Toe Touch",
        "Alongamento de Quadríceps em Pé",
        "Alongamento Glúteo Unilateral Máquina",
        "Alongamento Lateral Espaldar Dinâmico",
        "Alongamento Lateral Espaldar Isométrico",
        "Alongamento Lombar Máquina",
        "Alongamento Maomé",
        "Alongamento Maomé Unilateral",
        "Alongamento Maomé Unilateral Espaldar",
        "Alongamento Neural Banco",
        "Alongamento Neural Máquina Precor",
        "Alongamento Panturrilha Bilateral",
        "Alongamento Peitoral Bilateral Espaldar",
        "Alongamento Peitoral Unilateral Braço Estendido",
        "Alongamento Peitoral Unilateral Braço Fletido",
        "Alongamento Piriforme",
        "Alongamento Posterior Banco Declinado",
        "Alongamento Posterior Banco Inclinado",
        "Alongamento Posterior Dinâmico Banco",
        "Alongamento Posteriores Máquina Technogym",
        "Alongamento Posterior Máquina Precor",
        "Alongamento Yoga",
        "Apoio Bosu Invertido",
        "Apoio Solo Aberto",
        "Apoio Solo Ajoelhado",
        "Apoio Solo Fechado",
        "Apoio Solo Suporte Fixo",
        "Apoio Solo Suporte Giratório",
        "Auto Liberação Antebraço (Bolinha)",
        "Auto Liberação de Dorsal Foam Roller",
        "Auto Liberação de Fáscia Plantar",
        "Auto Liberação Escápulas (Bolinha)",
        "Auto Liberação Glúteo Médio Rolo",
        "Auto Liberação Glúteo Rolo",
        "Auto Liberação Panturrilha Rolo",
        "Auto Liberação Panturrilha Stick",
        "Auto Liberação Posterior de Coxa",
        "Auto Liberação Quadríceps",
        "Auto Liberação Quadríceps com Stick",
        "Auto Liberação Tensor da Fáscia Lata",
        "Avanço Alternando",
        "Avanço Bosu",
        "Avanço com Halter 2 Steps",
        "Avanço com Halteres",
        "Avanço Livre Barra",
        "Avanço Livre Halter",
        "Avanço Livre Peso Corporal",
        "Avanço no Balance Disc",
        "Avanço Reverso",
        "Avanço Senta e Levanta Banco / Caixote",
        "Avanço Smith",
        "Avanço Smith 2 Steps",
        "Avanço Smith Step Atrás",
        "Avanço Smith Step Frente",
        "Avanço Step Atrás",
        "Avanço Step Frente",
        "Avanço TRX",
        "Banco Romano Dinâmico",
        "Banco Romano Isométrico",
        "Banco Romano Máquina",
        "Barra Fixa Graviton Neutra",
        "Barra Fixa Graviton Pronada",
        "Barra Fixa Graviton Supinada",
        "Barra Fixa Neutra",
        "Barra Fixa Pronada",
        "Barra Fixa Supinada",
        "Belt Squat Hammer",
        "Bike Cycling",
        "Bike Horizontal",
        "Burpee",
        "Burpee com Agachamento",
        "Burpee Over",
        "Cadeira Abdutora",
        "Cadeira Abdutora Inclinada Atrás",
        "Cadeira Abdutora Inclinada Frente",
        "Cadeira Abdutora Máquina Forward",
        "Cadeira Adutora",
        "Cadeira Extensora",
        "Cadeira Extensora Articulada Realleader",
        "Cadeira Extensora Articulada Unilateral Realleader",
        "Cadeira Extensora Isométrica",
        "Cadeira Extensora Unilateral",
        "Cadeira Flexora",
        "Cadeira Flexora Unilateral",
        "Corrida com Joelho Alto",
        "Corrida no Lugar com Joelho Baixo",
        "Crossover Curvado Máquina Forward",
        "Crossover Polia Alta",
        "Crossover Polia Baixa",
        "Crossover Polia Média",
        "Crucifixo Banco Inclinado Cross",
        "Crucifixo Curvado",
        "Crucifixo Declinado Máquina Forward",
        "Crucifixo Inclinado Articulado Realleader",
        "Crucifixo Inclinado Halter",
        "Crucifixo Inverso Banco Inclinado",
        "Crucifixo Inverso Cross",
        "Crucifixo Inverso em Pé",
        "Crucifixo Inverso Máquina Neutra",
        "Crucifixo Inverso Máquina Pronada",
        "Crucifixo Máquina",
        "Crucifixo Reto Cross",
        "Crucifixo Reto Halter",
        "Deadlift Unilateral Alternado",
        "Depressão Escapular no Pulley",
        "Desenvolvimento Arnold Completo",
        "Desenvolvimento Arnold Parcial",
        "Desenvolvimento Articulado Neutro Realleader",
        "Desenvolvimento Articulado Pronado Realleader",
        "Desenvolvimento Barra em Pé",
        "Desenvolvimento Barra Sentado",
        "Desenvolvimento com Kettlebell",
        "Desenvolvimento Halter Neutro em Pé",
        "Desenvolvimento Halter Neutro Sentado",
        "Desenvolvimento Halter Pronado em Pé",
        "Desenvolvimento Halter Pronado Sentado",
        "Desenvolvimento Máquina Technogym Neutra",
        "Desenvolvimento Máquina Technogym Pronada",
        "Desenvolvimento Nautilus Neutra",
        "Desenvolvimento Nautilus Pronada",
        "Desenvolvimento Pronado Máquina Forward",
        "Dorsiflexão com Elástico",
        "Educativo Stiff com Bastão",
        "Elevação de Quadril",
        "Elevação de Quadril Unilateral",
        "Elevação Frontal Alternada",
        "Elevação Frontal Anilha",
        "Elevação Frontal Barra Pronada",
        "Elevação Frontal Barra Supinada",
        "Elevação Frontal Cross Barra",
        "Elevação Frontal Cross Corda",
        "Elevação Frontal Halter Neutro",
        "Elevação Frontal Halter Pronado",
        "Elevação Frontal Halter Supinada",
        "Elevação Lateral Cross",
        "Elevação Lateral de Pé Máquina Forward",
        "Elevação Lateral Halter",
        "Elevação Lateral Máquina Forward",
        "Elevação Lateral Máquina Nautilus",
        "Elevação Lateral Unilateral Cross",
        "Elevação Pélvica Barra Livre",
        "Elevação Pélvica Guiada",
        "Elevação Pélvica Máquina Booty Builder",
        "Elevação Pélvica Máquina Hammer",
        "Elevação Pélvica Máquina Nautilus",
        "Elíptico",
        "Encolhimento Ombros Anilhas",
        "Encolhimento Ombros Barra Frente",
        "Encolhimento Ombros Halter",
        "Encolhimento Ombros Pulley Barra Frente",
        "Encolhimento Ombros Pulley Duplo",
        "Equilíbrio no Balance Disc",
        "Escada",
        "Escorpião",
        "Estabilidade de Joelho Avião",
        "Esteira",
        "Extensão de Quadril Máquina Glute Builder",
        "Extensão Punho Barra Banco",
        "Extensão Punho Halter Banco",
        "Extensão Punho Pulley Frente",
        "Extensão Punhos Invertido",
        "Extensão Punho Unilateral Halter Joelho",
        "Extensão Quadril 45°",
        "Extensão Quadril Caneleira",
        "Extensão Quadril Elástico",
        "Extensão Quadril Máquina Forward",
        "Extensão Quadril Polia Máquina Booty Builder",
        "Extensão Quadril Pulley",
        "Extensor Lombar Nautilus",
        "Fazendeiro com Kettlebell",
        "Flexão Joelho Bola",
        "Flexão Joelhos em Pé Caneleira",
        "Flexão Nórdica",
        "Flexão Plantar com Elástico",
        "Flexão Punho Barra Joelho",
        "Flexão Punho Halter Joelho",
        "Flexão Punho Pulley Atrás",
        "Flexão Punho Pulley Frente",
        "Flexor em Pé Articulado",
        "Flexor em Pé Pulley",
        "Glute Press",
        "Good Morning (Somente Bastão)",
        "Grande Remada Barra",
        "Grande Remada Cross",
        "Grande Remada Kettlebell",
        "Hack Cybex",
        "Hack Hammer",
        "Hack Linear Shua",
        "Isometria Core Pulley",
        "Isometria em L",
        "Jump Box",
        "Leg Press 45°",
        "Leg Press 45° Unilateral",
        "Leg Press Horizontal",
        "Leg Press Horizontal Unilateral",
        "Levantamento Olímpico",
        "Levantamento Terra Barra",
        "Levantamento Terra Barra Sumô",
        "Levantamento Terra Hexagonal",
        "Lombar Nautilus",
        "Lombar Superman Solo Dinâmico",
        "Lombar Superman Solo Isométrico",
        "Meio Burpee",
        "Mesa Flexora",
        "Mesa Flexora Unilateral",
        "Mobilidade Abdução de Quadril 2 Apoios",
        "Mobilidade Abdução de Quadril 2 Apoios (Foam Roller)",
        "Mobilidade Abdução Ombro com Bastão",
        "Mobilidade Agachamento com Bastão",
        "Mobilidade Back Line",
        "Mobilidade Circundução Ombros Alternada Bastão / Elástico",
        "Mobilidade Cócoras com Extensão de Joelhos",
        "Mobilidade Flexão Quadril em Pé Caneleira",
        "Mobilidade Gato Bravo",
        "Mobilidade de Quadril 90° / 90°",
        "Mobilidade de Quadril em Cócoras",
        "Mobilidade de Quadril Sapinho",
        "Mobilidade de Tronco em J",
        "Mobilidade Escorpião Invertido",
        "Mobilidade Extensão de Punhos",
        "Mobilidade Flexão de Quadril com Caixote",
        "Mobilidade Flexão Punho Invertido",
        "Mobilidade Flexão Punhos",
        "Mobilidade Flexão Quadril Elástico Unilateral Dinâmico",
        "Mobilidade Ombro em L",
        "Mobilidade Ombros Foam Roller",
        "Mobilidade Pêndulo",
        "Mobilidade Quadril Anterior Solo",
        "Mobilidade Quadril Anterior Solo Bola Feijão",
        "Mobilidade Rotação de Ombro Elástico",
        "Mobilidade Rotação de Quadril com Joelhos Flexionados",
        "Mobilidade Rotação de Quadril Cruzado com Elástico",
        "Mobilidade Rotação de Tronco em J",
        "Mobilidade Rotação de Tronco em Pé",
        "Mobilidade Rotação de Tronco Solo",
        "Mobilidade Rotação Ombro Bastão",
        "Mobilidade Rotação Ombro Deitado",
        "Mobilidade Sacrifício Banco Isométrico",
        "Mobilidade Sacrifício no Banco",
        "Mobilidade Sacrifício Solo",
        "Mobilidade Sanson com Rotação de Tronco",
        "Mobilidade Sanson Dinâmico",
        "Mobilidade Sanson Estático",
        "Mobilidade Toe Touch",
        "Mobilidade Toque nas Escápulas",
        "Mobilidade Torácica no Rolo",
        "Mobilidade Torácica no Rolo Estática",
        "Mobilidade Tornozelo Ajoelhado",
        "Mobilidade Tornozelo Ajoelhado com Kettlebell",
        "Mobilidade Tornozelo em Pé",
        "Mobilidade Varredura Bola Feijão",
        "Mountain Climbers",
        "Nórdico Reverso",
        "Panturrilha em Pé Máquina",
        "Panturrilha em Pé Máquina Unilateral",
        "Panturrilha Livre em Pé",
        "Panturrilha Livre em Pé Unilateral",
        "Panturrilha Máquina Cybex",
        "Panturrilha Máquina Matrix",
        "Panturrilha Sentado",
        "Panturrilha Unilateral",
        "Paralela Máquina Matrix",
        "Paralela no Graviton",
        "Passada Lateral com Mini Band",
        "Passadas Anilha",
        "Passadas Barra",
        "Passadas Halter",
        "Passadas Peso Corporal",
        "Perdigueiro",
        "Perdigueiro Invertido",
        "Perdigueiro Invertido com Bola",
        "Perdigueiro Unilateral",
        "Polichinelo",
        "Polichinelo Adaptado",
        "Polichinelo com Prancha",
        "Ponte Bosu",
        "Ponte Bosu Unilateral",
        "Ponte Solo",
        "Ponte Solo com Mini Band",
        "Ponte Solo Unilateral",
        "Prancha com Base no Cotovelo",
        "Prancha com Elevação de Perna",
        "Prancha com Mão no Ombro",
        "Prancha com Rotação de Quadril",
        "Prancha Frontal Ajoelhado",
        "Prancha Isométrica Alta",
        "Prancha Isométrica Banco",
        "Prancha Isométrica Bosu Invertido",
        "Prancha Isométrica Frontal",
        "Prancha Isométrica Lateral",
        "Prancha Lateral",
        "Prancha Mão no Pé",
        "Prancha Sobe Desce",
        "Pular Corda",
        "Pulo Lateral",
        "Pull Down Barra Pulley",
        "Pull Down Corda Pulley",
        "Pullover Barra",
        "Pullover Halter",
        "Puxada Articulada Forward",
        "Puxada Articulada Hammer",
        "Puxada Articulada Hammer Unilateral",
        "Puxada Articulada Nautilus Neutra",
        "Puxada Articulada Nautilus Pronada",
        "Puxada Articulada Nautilus Supinada",
        "Puxada Marsafit 01",
        "Puxada Marsafit 02",
        "Puxada Marsafit Pronada",
        "Puxada Marsafit Supinada",
        "Puxada Pulley Pronada",
        "Puxada Pulley Romana",
        "Puxada Pulley Supinada",
        "Puxada Pulley Triângulo",
        "Puxada Pulley Unilateral Neutra",
        "Puxada Pulley Unilateral Pronada",
        "Puxada Pulley Unilateral Supinada",
        "Puxada X",
        "Remada Alta Barra Romana Máquina Forward",
        "Remada Alta Marsafit Máquina Forward",
        "Remada Alta Marsafit Supinada Máquina Forward",
        "Remada Alta Pronada Aberta Máquina Forward",
        "Remada Alta Supinada Máquina Forward",
        "Remada Alta Triângulo Máquina Forward",
        "Remada Articulada Hammer Neutra",
        "Remada Articulada Hammer Pronada",
        "Remada Articulada Hammer Unilateral Neutro",
        "Remada Articulada Neutra Realleader",
        "Remada Articulada Pronada Realleader",
        "Remada Articulada Technogym Neutra",
        "Remada Articulada Technogym Neutra Unilateral",
        "Remada Baixa Pronada Aberta",
        "Remada Baixa Supinada",
        "Remada Baixa Triângulo",
        "Remada Barra Romana",
        "Remada Cavalinho Neutra Aberta",
        "Remada Cavalinho Neutra Fechada",
        "Remada Cavalinho Pronada",
        "Remada Cavalo Neutra",
        "Remada Cavalo Pronada",
        "Remada Cavalo Supinada",
        "Remada Curvada Barra Pronada",
        "Remada Curvada Barra Supinada",
        "Remada Curvada com Barra",
        "Remada Curvada com Halter",
        "Remada Curvada com Kettlebell Neutra",
        "Remada Curvada Halter Neutra",
        "Remada Curvada Halter Pronada",
        "Remada Curvada Halter Supinada",
        "Remada Máquina Supinada Realleader",
        "Remada Nautilus Articulada Neutra",
        "Remada Nautilus Articulada Pronada",
        "Remada Nautilus Articulada Supinada",
        "Remada TRX Neutra",
        "Remada TRX Pronada",
        "Remada TRX Supinada",
        "Remada Unilateral com Halter",
        "Remo Indoor",
        "Retração Escapular em T",
        "Retração Escapular em Y",
        "Rosca 21",
        "Rosca Alta Máquina",
        "Rosca Alternada",
        "Rosca Alternada Banco Inclinado",
        "Rosca Alternada Banco Inclinado com Isometria",
        "Rosca Alternada com Isometria",
        "Rosca Articulada Máquina",
        "Rosca Articulada Máquina Alternada",
        "Rosca Barra W",
        "Rosca Concentrada",
        "Rosca Direta Barra Reta",
        "Rosca Direta Barra W",
        "Rosca Direta Halter",
        "Rosca Direta na Barra 360°",
        "Rosca Halter Banco Inclinado",
        "Rosca Inversa Alternada",
        "Rosca Inversa Barra Reta",
        "Rosca Inversa Barra W",
        "Rosca Inversa na Barra 360°",
        "Rosca Inversa Pulley",
        "Rosca Martelo Alternada",
        "Rosca Martelo Barra 360°",
        "Rosca Martelo Barra H",
        "Rosca Martelo Halter",
        "Rosca Pulley Barra",
        "Rosca Pulley Barra W",
        "Rosca Pulley Corda",
        "Rosca Pulley Unilateral",
        "Rosca Scott Livre Barra Reta",
        "Rosca Scott Livre Barra W",
        "Rosca Scott Livre Halter",
        "Rosca Scott Livre Halter Unilateral",
        "Rosca Scott Máquina",
        "Rosca TRX",
        "Rosca Zottman Barra 360°",
        "Rosca Zottman com Halteres",
        "Rotação Core Pulley",
        "Rotação de Ombro Externa Supinada",
        "Rotação de Tronco com Bastão em Pé",
        "Rotação Externa de Ombro Polia",
        "Rotação Interna de Ombro Polia",
        "Rotação Tronco Solo",
        "Senta Levanta",
        "Sobe e Desce",
        "Squat Press",
        "Squat Press Unilateral",
        "Step Up",
        "Stiff Barra",
        "Stiff Belt Squat Hammer",
        "Stiff Halter",
        "Stiff Kettlebell",
        "Stiff Unilateral",
        "Superman Bola Dinâmico",
        "Superman Bola Isométrico",
        "Supino Declinado Articulado",
        "Supino Declinado com Halteres",
        "Supino Declinado Máquina Forward",
        "Supino Inclinado Articulado",
        "Supino Inclinado Barra Pronta",
        "Supino Inclinado Halter",
        "Supino Inclinado Hammer",
        "Supino Inclinado Livre",
        "Supino Inclinado Máquina Forward",
        "Supino Inclinado Máquina Nautilus Neutro",
        "Supino Inclinado Máquina Nautilus Pronado",
        "Supino no Crossover",
        "Supino Pegada Fechada",
        "Supino Reto Articulado Realleader",
        "Supino Reto Barra Pronta",
        "Supino Reto Halter",
        "Supino Reto Livre",
        "Supino Reto Máquina Forward",
        "Supino Reto Máquina Neutra",
        "Supino Reto Máquina Pronado",
        "Supino Reto Pronado Hammer",
        "Suspensão na Barra - Descompressão",
        "Swing Kettlebell",
        "Tibial Anterior Bilateral Máquina Hammer",
        "Tibial Anterior Sentado",
        "Tibial Anterior Unilateral Máquina Hammer",
        "Toque nos Ombros",
        "Transport",
        "Tríceps Coice Halter",
        "Tríceps Coice Halter Unilateral",
        "Tríceps Francês com Barra W em Pé",
        "Tríceps Francês com Barra W Sentado",
        "Tríceps Francês Corda Polia Média",
        "Tríceps Francês Halter em Pé",
        "Tríceps Francês Halter Sentado",
        "Tríceps Francês Máquina Nautilus",
        "Tríceps Francês Polia Média Barra",
        "Tríceps Francês Unilateral em Pé",
        "Tríceps Francês Unilateral na Polia",
        "Tríceps Francês Unilateral Pulley",
        "Tríceps Francês Unilateral Sentado",
        "Tríceps Pulley Barra Reta",
        "Tríceps Pulley Barra V",
        "Tríceps Pulley Coice",
        "Tríceps Pulley Corda",
        "Tríceps Pulley Supinado",
        "Tríceps Pulley Unilateral",
        "Tríceps Pulley Unilateral Pronado",
        "Tríceps Pulley Unilateral Supinado",
        "Tríceps Supinado Barra Reta",
        "Tríceps Supinado Barra W",
        "Tríceps Testa Banco Pulley",
        "Tríceps Testa Barra H",
        "Tríceps Testa Barra Reta",
        "Tríceps Testa Barra W",
        "Tríceps Testa Halter",
        "Tríceps Testa Máquina",
        "Tríceps Testa Pulley Barra",
        "Tríceps Testa Pulley Corda",
        "Tríceps Testa Unilateral",
        "V-up + Remador",
        "Varredura Solo",
        "Wall Ball"
    )

    fun listarTodosItens(context: Context): List<ItemCatalogoExercicio> {
        val prefs =
            context.getSharedPreferences("fitcare_catalogo_exercicios", Context.MODE_PRIVATE)
        val prefsCat =
            context.getSharedPreferences("fitcare_catalogo_categorias_prof", Context.MODE_PRIVATE)
        val json = prefs.getString("personalizados", "[]") ?: "[]"
        val personalizados = try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { idx ->
                arr.optString(idx).takeIf { s -> s.isNotBlank() }
            }
        } catch (e: Exception) {
            emptyList<String>()
        }
        val jsonOcultos = prefs.getString("ocultos", "[]") ?: "[]"
        val ocultos = try {
            val arr = JSONArray(jsonOcultos)
            (0 until arr.length()).mapNotNull { idx ->
                arr.optString(idx).takeIf { s -> s.isNotBlank() }?.lowercase()
            }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
        val todosNomes = (personalizados + NOMES_EXERCICIOS_SISTEMA)
            .distinctBy { it.trim().lowercase() }
            .filter { it.trim().lowercase() !in ocultos }
            .sorted()
        return todosNomes.map { nome ->
            val catSalva = prefsCat.getString(nome.trim().lowercase(), null)
            ItemCatalogoExercicio(
                nome = nome,
                grupo = catSalva?.takeIf { it.isNotBlank() } ?: detectarGrupoMuscular(nome),
                videoUrl = gerarLinkVideoDemonstrativo(context, nome)
            )
        }
    }

    fun adicionarPersonalizado(context: Context, nomeExercicio: String) {
        val limpo = nomeExercicio.trim()
        if (limpo.isBlank()) return
        val prefs =
            context.getSharedPreferences("fitcare_catalogo_exercicios", Context.MODE_PRIVATE)
        val json = prefs.getString("personalizados", "[]") ?: "[]"
        val atuais = try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { idx ->
                arr.optString(idx).takeIf { s -> s.isNotBlank() }
            }.toMutableSet()
        } catch (e: Exception) {
            mutableSetOf<String>()
        }
        atuais.add(limpo)
        val novoArr = JSONArray()
        atuais.forEach { novoArr.put(it) }
        prefs.edit().putString("personalizados", novoArr.toString()).apply()
    }
}

@Composable
fun DialogoCatalogoExercicios(
    context: Context,
    onDismiss: () -> Unit,
    onSelecionarItem: (ItemCatalogoExercicio) -> Unit
) {
    var busca by remember { mutableStateOf("") }
    var grupoFiltro by remember { mutableStateOf("Todos") }
    var listaCatalogo by remember {
        mutableStateOf(
            CatalogoExerciciosStorage.listarTodosItens(
                context
            )
        )
    }
    var novoPersonalizado by remember { mutableStateOf("") }

    val grupos = remember { listOf("Todos") + CatalogoExerciciosStorage.GRUPOS_MUSCULARES }

    val filtrados = remember(busca, grupoFiltro, listaCatalogo) {
        val termo = busca.trim()
        listaCatalogo.filter { item ->
            val okGrupo = grupoFiltro == "Todos" || item.grupo == grupoFiltro
            val okTexto = termo.isBlank() ||
                    item.nome.contains(termo, ignoreCase = true) ||
                    item.grupo.contains(termo, ignoreCase = true)
            okGrupo && okTexto
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Atividades cadastradas no sistema (${filtrados.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = busca,
                    onValueChange = { busca = it },
                    placeholder = { Text("Buscar atividade por nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(grupos.size) { idx ->
                        val grp = grupos[idx]
                        FilterChip(
                            selected = grupoFiltro == grp,
                            onClick = { grupoFiltro = grp },
                            label = { Text(grp, fontSize = 11.sp) }
                        )
                    }
                }

                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (filtrados.isEmpty()) {
                        item {
                            Text(
                                text = "Nenhum exercício encontrado para \"$busca\". Cadastre abaixo:",
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        items(filtrados.size) { index ->
                            val item = filtrados[index]
                            val temVideo = item.videoUrl.isNotBlank()
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (temVideo) {
                                        CardPreviewVideoExercicio(
                                            tituloExercicio = item.nome,
                                            videoUrl = item.videoUrl,
                                            videoTipo = if (item.videoUrl.startsWith("/")) "LOCAL" else "LINK",
                                            modifier = Modifier.size(46.dp),
                                            onAbrirPlayer = { onSelecionarItem(item) }
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surface,
                                                    RoundedCornerShape(8.dp)
                                                ),
                                            contentAlignment = androidx.compose.ui.Alignment.Center
                                        ) {
                                            Text("❚█═█❚", fontSize = 9.sp)
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.nome,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2
                                        )
                                        Text(
                                            text = if (temVideo) item.grupo else "${item.grupo} • Sem vídeo",
                                            fontSize = 10.sp,
                                            color = if (temVideo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                    }
                                    Button(
                                        onClick = { onSelecionarItem(item) },
                                        contentPadding = PaddingValues(
                                            horizontal = 10.dp,
                                            vertical = 4.dp
                                        ),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(
                                            "+ Adicionar",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = novoPersonalizado,
                        onValueChange = { novoPersonalizado = it },
                        label = { Text("Novo exercício") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (novoPersonalizado.isNotBlank()) {
                                CatalogoExerciciosStorage.adicionarPersonalizado(
                                    context,
                                    novoPersonalizado
                                )
                                listaCatalogo = CatalogoExerciciosStorage.listarTodosItens(context)
                                val criado = listaCatalogo.firstOrNull {
                                    it.nome.equals(novoPersonalizado.trim(), ignoreCase = true)
                                }
                                if (criado != null) onSelecionarItem(criado)
                            }
                        },
                        enabled = novoPersonalizado.isNotBlank()
                    ) {
                        Text("Salvar")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

@Composable
fun TelaGerenciarExercicios(
    onVoltar: () -> Unit,
    onInicio: () -> Unit,
    onAlunos: () -> Unit,
    onTreinos: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var busca by remember { mutableStateOf("") }
    var filtroVideo by remember { mutableStateOf("Todos") }
    var listaCatalogo by remember {
        mutableStateOf(
            CatalogoExerciciosStorage.listarTodosItens(
                context
            )
        )
    }
    var exercicioEditando by remember { mutableStateOf<ItemCatalogoExercicio?>(null) }
    var urlVideoInput by remember { mutableStateOf("") }
    var categoriaEditandoInput by remember { mutableStateOf("") }
    var nomeEditandoInput by remember { mutableStateOf("") }
    var erroUrl by remember { mutableStateOf("") }
    var novoNomeExercicio by remember { mutableStateOf("") }
    var mostrarDialogoNovoExercicio by remember { mutableStateOf(false) }
    var videoEmReproducao by remember { mutableStateOf<Pair<String, String>?>(null) }

    val seletorVideoCatalogo = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val pastaVideos = java.io.File(context.filesDir, "videos_catalogo")
                if (!pastaVideos.exists()) pastaVideos.mkdirs()
                val destino = java.io.File(pastaVideos, "${System.currentTimeMillis()}_video.mp4")
                context.contentResolver.openInputStream(uri)?.use { entrada ->
                    destino.outputStream().use { saida -> entrada.copyTo(saida) }
                }
                urlVideoInput = destino.absolutePath
                erroUrl = ""
            } catch (e: Exception) {
                erroUrl = "Não foi possível importar o vídeo: ${e.message ?: "erro"}"
            }
        }
    }

    var mostrarAvisoVideoLocal by remember { mutableStateOf(false) }

    val filtrados = remember(busca, filtroVideo, listaCatalogo) {
        val termo = busca.trim()
        listaCatalogo.filter { item ->
            val okFiltro = when (filtroVideo) {
                "Com vídeo" -> item.videoUrl.isNotBlank()
                "Sem vídeo" -> item.videoUrl.isBlank()
                else -> true
            }
            val okTexto = termo.isBlank() ||
                    item.nome.contains(termo, ignoreCase = true) ||
                    item.grupo.contains(termo, ignoreCase = true)
            okFiltro && okTexto
        }
    }

    val totalComVideo = remember(listaCatalogo) { listaCatalogo.count { it.videoUrl.isNotBlank() } }
    val totalSemVideo = remember(listaCatalogo) { listaCatalogo.count { it.videoUrl.isBlank() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (temaEscuroAtivoState) androidx.compose.ui.graphics.Color(0xFF0F1722) else androidx.compose.ui.graphics.Color(
                    0xFFF5F9FC
                )
            )
            .statusBarsPadding()
    ) {
        CabecalhoPadrao(
            titulo = "Gerenciar exercícios",
            onVoltar = onVoltar
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Todos (${listaCatalogo.size})" to "Todos",
                    "Com vídeo ($totalComVideo)" to "Com vídeo",
                    "Sem vídeo ($totalSemVideo)" to "Sem vídeo"
                ).forEach { (rotulo, chave) ->
                    FilterChip(
                        selected = filtroVideo == chave,
                        onClick = { filtroVideo = chave },
                        label = { Text(rotulo, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = busca,
                    onValueChange = { busca = it },
                    label = { Text("Buscar exercício ou grupo...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        novoNomeExercicio = ""
                        mostrarDialogoNovoExercicio = true
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)
                ) {
                    Text("+ Novo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtrados.size) { index ->
                    val item = filtrados[index]
                    val temVideo = item.videoUrl.isNotBlank()
                    val tipoVid = if (item.videoUrl.startsWith("/")) "LOCAL" else "LINK"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (temaEscuroAtivoState) androidx.compose.ui.graphics.Color(
                                0xFF172331
                            ) else androidx.compose.ui.graphics.Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.nome,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = item.grupo,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (temVideo) {
                                        if (tipoVid == "LOCAL") "▶ Vídeo importado do celular" else "▶ Vídeo demonstrativo cadastrado"
                                    } else {
                                        "⚠ Esse exercício ainda não tem vídeo demonstrativo"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (temVideo) {
                                        androidx.compose.ui.graphics.Color(0xFF2E7D32)
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        exercicioEditando = item
                                        nomeEditandoInput = item.nome
                                        categoriaEditandoInput = item.grupo
                                        urlVideoInput = item.videoUrl
                                        erroUrl = ""
                                    },
                                    contentPadding = PaddingValues(
                                        horizontal = 14.dp,
                                        vertical = 4.dp
                                    ),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Editar exercício", fontSize = 12.sp)
                                }
                            }
                            if (temVideo) {
                                CardPreviewVideoExercicio(
                                    tituloExercicio = item.nome,
                                    videoUrl = item.videoUrl,
                                    videoTipo = tipoVid,
                                    modifier = Modifier
                                        .width(88.dp)
                                        .height(115.dp),
                                    onAbrirPlayer = {
                                        videoEmReproducao = item.nome to item.videoUrl
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        DashboardBottomBar(
            telaAtual = AppScreen.GerenciarExercicios,
            onInicio = onInicio,
            onAlunos = onAlunos,
            onTreinos = onTreinos,
            onExercicios = {}
        )
    }

    videoEmReproducao?.let { (titulo, url) ->
        DialogoPlayerVideoExercicio(
            tituloExercicio = titulo,
            videoUrl = url,
            videoTipo = if (url.startsWith("/")) "LOCAL" else "LINK",
            onFechar = { videoEmReproducao = null }
        )
    }
    exercicioEditando?.let { exAlvo ->
        AlertDialog(
            onDismissRequest = { exercicioEditando = null },
            title = { Text("Editar exercício", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = nomeEditandoInput,
                        onValueChange = {
                            nomeEditandoInput = it
                            erroUrl = ""
                        },
                        label = { Text("Nome do exercício") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Parte do corpo / Categoria:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(CatalogoExerciciosStorage.GRUPOS_MUSCULARES.size) { gIdx ->
                            val grp = CatalogoExerciciosStorage.GRUPOS_MUSCULARES[gIdx]
                            FilterChip(
                                selected = categoriaEditandoInput == grp,
                                onClick = { categoriaEditandoInput = grp },
                                label = { Text(grp, fontSize = 11.sp) }
                            )
                        }
                    }
                    Text(
                        text = "Vídeo demonstrativo (Link YouTube ou celular):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = if (urlVideoInput.startsWith("/")) "Vídeo importado do celular" else urlVideoInput,
                        onValueChange = {
                            urlVideoInput = it
                            erroUrl = ""
                        },
                        readOnly = urlVideoInput.startsWith("/"),
                        label = { Text("Link do vídeo (YouTube)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = { mostrarAvisoVideoLocal = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Importar vídeo do próprio telefone")
                    }
                    Text(
                        text = "Obs.: Vídeos importados do celular ficam salvos apenas neste aparelho. Para sincronizar com o aluno, utilize um link do YouTube (pode ser vídeo Não Listado).",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    if (urlVideoInput.isNotBlank()) {
                        TextButton(
                            onClick = { urlVideoInput = "" },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Remover vídeo atual",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }
                    }
                    if (erroUrl.isNotBlank()) {
                        Text(
                            text = erroUrl,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nomeEditandoInput.isBlank()) {
                            erroUrl = "Informe o nome do exercício."
                            return@TextButton
                        }
                        val limpa = urlVideoInput.trim()
                        if (limpa.isNotBlank() && !limpa.startsWith("/") && !limpa.startsWith(
                                "http://",
                                true
                            ) && !limpa.startsWith("https://", true)
                        ) {
                            erroUrl = "Informe um link válido iniciado com http:// ou https://"
                            return@TextButton
                        }
                        CatalogoExerciciosStorage.editarExercicioCompleto(
                            context = context,
                            nomeOriginal = exAlvo.nome,
                            novoNome = nomeEditandoInput,
                            novaCategoria = categoriaEditandoInput,
                            novoVideoUrl = limpa
                        )
                        listaCatalogo = CatalogoExerciciosStorage.listarTodosItens(context)
                        exercicioEditando = null
                    }
                ) {
                    Text("Salvar alterações", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { exercicioEditando = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (mostrarAvisoVideoLocal) {
        AlertDialog(
            onDismissRequest = { mostrarAvisoVideoLocal = false },
            title = {
                Text(
                    text = "Aviso sobre vídeo local",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "No momento, vídeos importados diretamente da galeria ficam salvos apenas neste aparelho (para demonstração presencial) e não são enviados para o celular do aluno.\n\nPara que o vídeo apareça sincronizado no aplicativo do aluno, utilize a opção de link do YouTube.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarAvisoVideoLocal = false
                        seletorVideoCatalogo.launch(arrayOf("video/*"))
                    }
                ) {
                    Text("Entendi, importar mesmo assim", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarAvisoVideoLocal = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (mostrarDialogoNovoExercicio) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoNovoExercicio = false },
            title = { Text("Novo exercício no catálogo") },
            text = {
                OutlinedTextField(
                    value = novoNomeExercicio,
                    onValueChange = { novoNomeExercicio = it },
                    label = { Text("Nome do exercício") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (novoNomeExercicio.isNotBlank()) {
                            CatalogoExerciciosStorage.adicionarPersonalizado(
                                context,
                                novoNomeExercicio
                            )
                            listaCatalogo = CatalogoExerciciosStorage.listarTodosItens(context)
                            mostrarDialogoNovoExercicio = false
                        }
                    }
                ) {
                    Text("Cadastrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoNovoExercicio = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}