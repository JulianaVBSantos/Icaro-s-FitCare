
package br.com.icaros.fitcare

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding
import br.com.icaros.fitcare.data.UsuarioEntity
import br.com.icaros.fitcare.data.UsuarioRepository
import br.com.icaros.fitcare.data.DispositivoRepository
import br.com.icaros.fitcare.data.DispositivoInfo
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun TelaAdminDashboard(
    usuarioRepository: UsuarioRepository,
    onSair: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val dispositivoRepository = remember {
        DispositivoRepository(context)
    }

    val fundoAdmin = Color(0xFFF4F8FC)
    val cabecalhoTopoAdmin = Color(0xFFEAF5FF)
    val cabecalhoBaseAdmin = Color(0xFFD8ECFF)
    val textoPrincipalAdmin = Color(0xFF173B5E)
    val textoSecundarioAdmin = Color(0xFF526A80)
    val azulPrincipalAdmin = Color(0xFF2878B9)

    var profissionais by remember {
        mutableStateOf<List<UsuarioEntity>>(emptyList())
    }

    var carregando by remember {
        mutableStateOf(true)
    }

    var mensagemErro by remember {
        mutableStateOf<String?>(null)
    }

    var profissionalSelecionado by remember {
        mutableStateOf<UsuarioEntity?>(null)
    }

    var mostrarDialogoExclusao by remember {
        mutableStateOf(false)
    }

    var mostrarDialogoSenha by remember {
        mutableStateOf(false)
    }

    var mostrarDialogoCadastro by remember {
        mutableStateOf(false)
    }

    var novaSenha by remember { mutableStateOf("") }
    var confirmarSenha by remember { mutableStateOf("") }

    var nomeNovoProfessor by remember { mutableStateOf("") }
    var emailNovoProfessor by remember { mutableStateOf("") }
    var senhaNovoProfessor by remember { mutableStateOf("") }
    var confirmarSenhaNovoProfessor by remember { mutableStateOf("") }

    var mensagemCadastro by remember {
        mutableStateOf<String?>(null)
    }

    var cadastrandoProfessor by remember {
        mutableStateOf(false)
    }

    // Controle dos dispositivos
    var dispositivosProfessor by remember {
        mutableStateOf<List<DispositivoInfo>>(emptyList())
    }

    var mostrarDialogoDispositivos by remember {
        mutableStateOf(false)
    }

    var carregandoDispositivos by remember {
        mutableStateOf(false)
    }

    var erroDispositivos by remember {
        mutableStateOf<String?>(null)
    }

    fun carregarProfissionais() {
        scope.launch {
            carregando = true
            mensagemErro = null

            try {
                profissionais =
                    usuarioRepository.listarProfissionaisParaAdmin()
            } catch (e: Exception) {
                mensagemErro = e.message
                    ?: "Não foi possível carregar os profissionais."
            } finally {
                carregando = false
            }
        }
    }

    fun carregarDispositivos(profissional: UsuarioEntity) {
        val uid = profissional.firebaseUid

        if (uid.isNullOrBlank()) {
            erroDispositivos =
                "Este professor ainda não possui UID vinculado ao Firebase."

            dispositivosProfessor = emptyList()
            mostrarDialogoDispositivos = true
            return
        }

        carregandoDispositivos = true
        erroDispositivos = null
        dispositivosProfessor = emptyList()
        mostrarDialogoDispositivos = true

        dispositivoRepository.listarDispositivos(uid) { resultado ->

            resultado
                .onSuccess { lista ->
                    dispositivosProfessor = lista
                    erroDispositivos = null
                }
                .onFailure { erro ->
                    erroDispositivos = erro.message
                        ?: "Não foi possível consultar os dispositivos."
                }

            carregandoDispositivos = false
        }
    }

    DisposableEffect(Unit) {
        carregarProfissionais()

        val listenerProfissionais = FirebaseFirestore.getInstance()
            .collection("usuarios")
            .whereEqualTo("perfil", "PROFESSOR")
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    scope.launch {
                        try {
                            profissionais =
                                usuarioRepository.listarProfissionaisParaAdmin()
                        } catch (_: Exception) {
                        }
                    }
                }
            }

        onDispose {
            listenerProfissionais.remove()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(fundoAdmin)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                cabecalhoTopoAdmin,
                                cabecalhoBaseAdmin
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(top = 4.dp, bottom = 12.dp)
            ) {
                Image(
                    painter = painterResource(
                        id = R.drawable.logo_fitcare
                    ),
                    contentDescription = "Logo Ícaros FitCare",
                    modifier = Modifier
                        .width(160.dp)
                        .height(80.dp)
                        .align(Alignment.Center)
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            nomeNovoProfessor = ""
                            emailNovoProfessor = ""
                            senhaNovoProfessor = ""
                            confirmarSenhaNovoProfessor = ""
                            mensagemCadastro = null
                            mostrarDialogoCadastro = true
                        }
                    ) {
                        Text(
                            text = "+",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Normal,
                            color = azulPrincipalAdmin
                        )
                    }

                    BotaoSairPadrao(onSair = onSair)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Text(
                    text = "Painel administrativo",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textoPrincipalAdmin
                )

                Text(
                    text = "Gerenciamento de contas profissionais",
                    fontSize = 14.sp,
                    color = textoSecundarioAdmin,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Profissionais cadastrados: ${profissionais.size}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textoPrincipalAdmin,
                        modifier = Modifier.weight(1f)
                    )

                    TextButton(
                        onClick = {
                            carregarProfissionais()
                        }
                    ) {
                        Text("ATUALIZAR")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                mensagemErro?.let {
                    Text(
                        text = it,
                        color = Color(0xFFD32F2F),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                when {
                    carregando -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = azulPrincipalAdmin
                            )
                        }
                    }

                    profissionais.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhum profissional cadastrado.",
                                color = textoSecundarioAdmin
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = profissionais,
                                key = { it.id }
                            ) { profissional ->

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp)
                                    ) {
                                        Text(
                                            text = profissional.nome,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textoPrincipalAdmin
                                        )

                                        Text(
                                            text = profissional.email,
                                            fontSize = 13.sp,
                                            color = textoSecundarioAdmin
                                        )

                                        Spacer(
                                            modifier = Modifier.height(8.dp)
                                        )

                                        Text(
                                            text = if (profissional.ativo) {
                                                "ACESSO AUTORIZADO"
                                            } else {
                                                "AGUARDANDO AUTORIZAÇÃO"
                                            },
                                            color = if (profissional.ativo) {
                                                Color(0xFF36844A)
                                            } else {
                                                Color(0xFFB17B00)
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Spacer(
                                            modifier = Modifier.height(10.dp)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement =
                                                Arrangement.spacedBy(6.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    scope.launch {
                                                        try {
                                                            usuarioRepository.alterarStatusProfissional(
                                                                professorId = profissional.id,
                                                                ativo = !profissional.ativo
                                                            )

                                                            carregarProfissionais()
                                                        } catch (e: Exception) {
                                                            mensagemErro =
                                                                e.message
                                                                    ?: "Não foi possível alterar o acesso."
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor =
                                                        if (profissional.ativo) {
                                                            Color(0xFFD32F2F)
                                                        } else {
                                                            Color(0xFF36844A)
                                                        }
                                                ),
                                                contentPadding = PaddingValues(
                                                    horizontal = 6.dp,
                                                    vertical = 8.dp
                                                )
                                            ) {
                                                Text(
                                                    text = if (profissional.ativo) {
                                                        "Bloquear"
                                                    } else {
                                                        "Autorizar"
                                                    },
                                                    fontSize = 12.sp
                                                )
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    profissionalSelecionado =
                                                        profissional

                                                    novaSenha = ""
                                                    confirmarSenha = ""
                                                    mostrarDialogoSenha = true
                                                },
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(
                                                    horizontal = 6.dp,
                                                    vertical = 8.dp
                                                )
                                            ) {
                                                Text(
                                                    "Redefinir senha",
                                                    fontSize = 11.sp
                                                )
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    profissionalSelecionado =
                                                        profissional

                                                    mostrarDialogoExclusao = true
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor =
                                                        Color(0xFFD32F2F)
                                                ),
                                                contentPadding = PaddingValues(
                                                    horizontal = 6.dp,
                                                    vertical = 8.dp
                                                )
                                            ) {
                                                Text(
                                                    "Excluir",
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }

                                        Spacer(
                                            modifier = Modifier.height(8.dp)
                                        )

                                        OutlinedButton(
                                            onClick = {
                                                profissionalSelecionado =
                                                    profissional

                                                carregarDispositivos(profissional)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("VER DISPOSITIVOS")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de cadastro
    if (mostrarDialogoCadastro) {
        AlertDialog(
            onDismissRequest = {
                if (!cadastrandoProfessor) {
                    mostrarDialogoCadastro = false
                }
            },
            title = {
                Text("Adicionar professor")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "O novo professor ficará aguardando autorização.",
                        fontSize = 13.sp,
                        color = textoSecundarioAdmin
                    )

                    OutlinedTextField(
                        value = nomeNovoProfessor,
                        onValueChange = { nomeNovoProfessor = it },
                        label = { Text("Nome completo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = emailNovoProfessor,
                        onValueChange = { emailNovoProfessor = it },
                        label = { Text("E-mail") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = senhaNovoProfessor,
                        onValueChange = { senhaNovoProfessor = it },
                        label = { Text("Senha") },
                        singleLine = true,
                        visualTransformation =
                            PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmarSenhaNovoProfessor,
                        onValueChange = {
                            confirmarSenhaNovoProfessor = it
                        },
                        label = { Text("Confirmar senha") },
                        singleLine = true,
                        visualTransformation =
                            PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    mensagemCadastro?.let {
                        Text(
                            text = it,
                            color = Color(0xFFD32F2F),
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !cadastrandoProfessor,
                    onClick = {
                        when {
                            nomeNovoProfessor.trim().length < 2 -> {
                                mensagemCadastro =
                                    "Informe o nome completo."
                            }

                            emailNovoProfessor.isBlank() -> {
                                mensagemCadastro = "Informe o e-mail."
                            }

                            senhaNovoProfessor.length < 8 -> {
                                mensagemCadastro =
                                    "A senha deve ter pelo menos 8 caracteres."
                            }

                            senhaNovoProfessor != confirmarSenhaNovoProfessor -> {
                                mensagemCadastro =
                                    "As senhas não coincidem."
                            }

                            else -> {
                                scope.launch {
                                    cadastrandoProfessor = true
                                    mensagemCadastro = null

                                    try {
                                        usuarioRepository.cadastrarProfessor(
                                            nome = nomeNovoProfessor.trim(),
                                            email = emailNovoProfessor.trim(),
                                            senha = senhaNovoProfessor
                                        )

                                        mostrarDialogoCadastro = false
                                        carregarProfissionais()
                                    } catch (e: Exception) {
                                        mensagemCadastro = e.message
                                            ?: "Não foi possível cadastrar o professor."
                                    } finally {
                                        cadastrandoProfessor = false
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        if (cadastrandoProfessor) {
                            "CADASTRANDO..."
                        } else {
                            "CADASTRAR"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !cadastrandoProfessor,
                    onClick = {
                        mostrarDialogoCadastro = false
                    }
                ) {
                    Text("CANCELAR")
                }
            }
        )
    }

    // Diálogo de exclusão
    if (mostrarDialogoExclusao) {
        AlertDialog(
            onDismissRequest = {
                mostrarDialogoExclusao = false
            },
            title = {
                Text("Excluir profissional?")
            },
            text = {
                Text(
                    "Deseja realmente excluir a conta de ${profissionalSelecionado?.nome}? Esta ação não poderá ser desfeita."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val profissional = profissionalSelecionado

                        if (profissional != null) {
                            scope.launch {
                                try {
                                    usuarioRepository.excluirContaProfissional(
                                        profissional.id
                                    )

                                    mostrarDialogoExclusao = false
                                    profissionalSelecionado = null
                                    carregarProfissionais()
                                } catch (e: Exception) {
                                    mensagemErro = e.message
                                        ?: "Não foi possível excluir a conta."

                                    mostrarDialogoExclusao = false
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        "EXCLUIR",
                        color = Color(0xFFD32F2F)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoExclusao = false
                    }
                ) {
                    Text("CANCELAR")
                }
            }
        )
    }

    // Diálogo de redefinição de senha
    if (mostrarDialogoSenha) {
        AlertDialog(
            onDismissRequest = {
                mostrarDialogoSenha = false
            },
            title = {
                Text("Redefinir senha")
            },
            text = {
                Column {
                    Text(
                        text = "Profissional: ${profissionalSelecionado?.nome}",
                        fontSize = 13.sp
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    OutlinedTextField(
                        value = novaSenha,
                        onValueChange = { novaSenha = it },
                        label = { Text("Nova senha") },
                        singleLine = true,
                        visualTransformation =
                            PasswordVisualTransformation()
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    OutlinedTextField(
                        value = confirmarSenha,
                        onValueChange = { confirmarSenha = it },
                        label = { Text("Confirmar nova senha") },
                        singleLine = true,
                        visualTransformation =
                            PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (novaSenha.length < 8) {
                            mensagemErro =
                                "A senha deve ter pelo menos 8 caracteres."
                            return@TextButton
                        }

                        if (novaSenha != confirmarSenha) {
                            mensagemErro =
                                "As senhas não coincidem."
                            return@TextButton
                        }

                        val profissional = profissionalSelecionado

                        if (profissional != null) {
                            scope.launch {
                                try {
                                    usuarioRepository.redefinirSenhaProfissional(
                                        professorId = profissional.id,
                                        novaSenha = novaSenha
                                    )

                                    mostrarDialogoSenha = false
                                    profissionalSelecionado = null
                                    novaSenha = ""
                                    confirmarSenha = ""
                                    mensagemErro = null
                                } catch (e: Exception) {
                                    mensagemErro = e.message
                                        ?: "Não foi possível redefinir a senha."
                                }
                            }
                        }
                    }
                ) {
                    Text("SALVAR")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoSenha = false
                    }
                ) {
                    Text("CANCELAR")
                }
            }
        )
    }

    // Diálogo de dispositivos
    if (mostrarDialogoDispositivos) {
        AlertDialog(
            onDismissRequest = {
                mostrarDialogoDispositivos = false
            },
            title = {
                Text(
                    "Dispositivos do professor ${profissionalSelecionado?.nome ?: "professor"}"
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when {
                        carregandoDispositivos -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        erroDispositivos != null -> {
                            Text(
                                text = erroDispositivos!!,
                                color = Color(0xFFD32F2F),
                                fontSize = 13.sp
                            )
                        }

                        dispositivosProfessor.isEmpty() -> {
                            Text(
                                "Nenhum dispositivo registrado para esta conta.",
                                color = textoSecundarioAdmin
                            )
                        }

                        else -> {
                            Text(
                                "Dispositivos encontrados: ${dispositivosProfessor.size}",
                                fontWeight = FontWeight.SemiBold,
                                color = textoPrincipalAdmin
                            )

                            LazyColumn(
                                modifier = Modifier.heightIn(max = 350.dp),
                                verticalArrangement =
                                    Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    items = dispositivosProfessor,
                                    key = { it.id }
                                ) { dispositivo ->

                                    val ativo = dispositivo.estaAtivo()

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor =
                                                Color(0xFFF4F8FC)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement =
                                                Arrangement.spacedBy(5.dp)
                                        ) {
                                            Text(
                                                text = "${dispositivo.manufacturer} ${dispositivo.model}",
                                                fontWeight = FontWeight.Bold,
                                                color = textoPrincipalAdmin
                                            )

                                            Text(
                                                text = if (ativo) {
                                                    "ATIVO RECENTEMENTE"
                                                } else {
                                                    "INATIVO"
                                                },
                                                color = if (ativo) {
                                                    Color(0xFF36844A)
                                                } else {
                                                    Color(0xFFB17B00)
                                                },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )

                                            Text(
                                                text = "Android: ${dispositivo.androidVersion}",
                                                fontSize = 12.sp,
                                                color = textoSecundarioAdmin
                                            )

                                            Text(
                                                text = "Versão do app: ${dispositivo.appVersion}",
                                                fontSize = 12.sp,
                                                color = textoSecundarioAdmin
                                            )

                                            Text(
                                                text = "Última atividade: ${
                                                    dispositivo.lastSeen?.let {
                                                        SimpleDateFormat(
                                                            "dd/MM/yyyy HH:mm:ss",
                                                            Locale("pt", "BR")
                                                        ).format(it)
                                                    } ?: "Não registrada"
                                                }",
                                                fontSize = 12.sp,
                                                color = textoSecundarioAdmin
                                            )

                                            Text(
                                                text = "ID: ${dispositivo.id}",
                                                fontSize = 10.sp,
                                                color = textoSecundarioAdmin
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoDispositivos = false
                    }
                ) {
                    Text("FECHAR")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !carregandoDispositivos,
                    onClick = {
                        profissionalSelecionado?.let {
                            carregarDispositivos(it)
                        }
                    }
                ) {
                    Text("ATUALIZAR")
                }
            }
        )
    }
}