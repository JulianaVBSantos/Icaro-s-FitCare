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

data class ItemCatalogoExercicio(
    val nome: String,
    val grupo: String,
    val videoUrl: String
)

object CatalogoExerciciosStorage {

    private fun gerarLinkVideoDemonstrativo(nomeExercicio: String): String {
        val query = Uri.encode("execução correta $nomeExercicio academia")
        return "https://youtube.com"
    }

    private fun detectarGrupoMuscular(nome: String): String {
        val n = nome.lowercase()
        return when {
            n.contains("supino") || n.contains("crucifixo") || n.contains("crossover") || n.contains("peitoral") || n.contains("apoio solo") -> "Peito"
            n.contains("puxada") || n.contains("remada") || n.contains("barra fixa") || n.contains("pull") || n.contains("dorsal") -> "Costas"
            n.contains("agachamento") || n.contains("leg press") || n.contains("extensora") || n.contains("flexora") || n.contains("avanço") || n.contains("afundo") || n.contains("passada") || n.contains("hack") || n.contains("squat") || n.contains("stiff") || n.contains("terra") || n.contains("panturrilha") || n.contains("tibial") -> "Pernas"
            n.contains("glúteo") || n.contains("glute") || n.contains("elevação pélvica") || n.contains("abdução") || n.contains("abdutora") || n.contains("adutora") || n.contains("extensão quadril") || n.contains("extensão de quadril") || n.contains("3 apoios") || n.contains("ponte") -> "Glúteos e Quadril"
            n.contains("desenvolvimento") || n.contains("elevação lateral") || n.contains("elevação frontal") || n.contains("ombro") || n.contains("encolhimento") -> "Ombros"
            n.contains("rosca") -> "Bíceps"
            n.contains("tríceps") || n.contains("triceps") || n.contains("paralela") -> "Tríceps"
            n.contains("abdominal") || n.contains("abd ") || n.contains("prancha") || n.contains("core") || n.contains("lombar") || n.contains("perdigueiro") -> "Abdômen e Core"
            n.contains("alongamento") || n.contains("mobilidade") || n.contains("mob ") || n.contains("mob.") || n.contains("liberação") -> "Mobilidade e Alongamento"
            else -> "Cardio e Funcional"
        }
    }

    // Lista completa extraída do arquivo .txt do sistema (limpa e normalizada)
    private val NOMES_EXERCICIOS_SISTEMA = listOf(
        "3 Apoios Perna Estendida",
        "3 Apoios Perna Flexionada",
        "Abdominal Banco Declinado",
        "Abdominal Banco Inclinado",
        "Abdominal Borboleta",
        "Abdominal Bosu Flexionado",
        "Abdominal Bosu Solo",
        "Abdominal Canivete Medicine Ball",
        "Abdominal Completo Máquina Hammer",
        "Abdominal Completo Máquina Nautilus",
        "Abdominal Completo Solo",
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
        "Crossover Curvado Máquina Forward",
        "Crossover Polia Alta",
        "Crossover Polia Baixa",
        "Crossover Polia Média",
        "Crucifixo Banco Inclinado Cross",
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
        "Nórdico Reverso",
        "Panturrilha em Pé Máquina",
        "Panturrilha em Pé Máquina Unilateral",
        "Panturrilha Livre em Pé",
        "Panturrilha Livre em Pé Unilateral",
        "Panturrilha Máquina Cybex",
        "Panturrilha Máquina Matrix",
        "Panturrilha Sentado",
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
        "Ponte Bosu",
        "Ponte Bosu Unilateral",
        "Ponte Solo",
        "Ponte Solo com Mini Band",
        "Ponte Solo Unilateral",
        "Prancha Frontal Ajoelhado",
        "Prancha Isométrica Alta",
        "Prancha Isométrica Banco",
        "Prancha Isométrica Bosu Invertido",
        "Prancha Isométrica Frontal",
        "Prancha Isométrica Lateral",
        "Prancha Sobe Desce",
        "Pular Corda",
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
        "Varredura Solo",
        "Wall Ball"
    )
    fun listarTodosItens(context: Context): List<ItemCatalogoExercicio> {
        val prefs = context.getSharedPreferences("fitcare_catalogo_exercicios", Context.MODE_PRIVATE)
        val json = prefs.getString("personalizados", "[]") ?: "[]"
        val personalizados = try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { idx ->
                arr.optString(idx).takeIf { s -> s.isNotBlank() }
            }
        } catch (e: Exception) {
            emptyList<String>()
        }
        val todosNomes = (personalizados + NOMES_EXERCICIOS_SISTEMA).distinct().sorted()
        return todosNomes.map { nome ->
            ItemCatalogoExercicio(
                nome = nome,
                grupo = detectarGrupoMuscular(nome),
                videoUrl = gerarLinkVideoDemonstrativo(nome)
            )
        }
    }

    fun adicionarPersonalizado(context: Context, nomeExercicio: String) {
        val limpo = nomeExercicio.trim()
        if (limpo.isBlank()) return
        val prefs = context.getSharedPreferences("fitcare_catalogo_exercicios", Context.MODE_PRIVATE)
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
    var listaCatalogo by remember { mutableStateOf(CatalogoExerciciosStorage.listarTodosItens(context)) }
    var novoPersonalizado by remember { mutableStateOf("") }

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
        title = { Text("Catálogo de Exercícios (${filtrados.size})") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = busca,
                    onValueChange = { busca = it },
                    label = { Text("Pesquisar exercício ou grupo...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (filtrados.isEmpty()) {
                        Text(
                            text = "Nenhum exercício encontrado para $busca. Cadastre abaixo:",
                            fontSize = 12.sp
                        )
                    } else {
                        filtrados.forEach { item ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelecionarItem(item) },
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = item.nome,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${item.grupo} • ▶ Vídeo demonstrativo incluído",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()
                Text("Não encontrou? Cadastre e salve no catálogo:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = novoPersonalizado,
                    onValueChange = { novoPersonalizado = it },
                    label = { Text("Nome do novo exercício") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (novoPersonalizado.isNotBlank()) {
                            CatalogoExerciciosStorage.adicionarPersonalizado(context, novoPersonalizado)
                            listaCatalogo = CatalogoExerciciosStorage.listarTodosItens(context)
                            val criado = listaCatalogo.firstOrNull { itemCat ->
                                itemCat.nome.equals(novoPersonalizado.trim(), ignoreCase = true)
                            }
                            if (criado != null) {
                                onSelecionarItem(criado)
                            }
                        }
                    },
                    enabled = novoPersonalizado.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Salvar no catálogo e selecionar")
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