package br.com.icaros.fitcare.data

import android.content.Context
import android.util.Base64
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class UsuarioRepository(
    private val usuarioDao: UsuarioDao,
    private val context: Context
) {

    companion object {
        private const val PERFIL_PROFESSOR = "PROFESSOR"
        private const val PERFIL_ALUNO = "ALUNO"
        private const val PERFIL_ADMIN = "ADMIN"

        private const val UID_ADMIN_AUTORIZADO = "Bcqsue17eVVdJ4Ajc9EhoTQfJ8b2"
        private const val NOME_FIREBASE_APP_SECUNDARIO = "FitCareTeacherProvisioning"

        private const val EMAIL_PROFESSOR_INICIAL = "icaroalvesdasilva2005@gmail.com"
        private const val EMAIL_ADMIN_INICIAL = "julianavieirabsantos@gmail.com"
    }

    // FIREBASE AUTHENTICATION

    private suspend fun autenticarFirebase(
        usuario: UsuarioEntity,
        senha: String
    ): UsuarioEntity {

        val auth = FirebaseAuth.getInstance()

        val firebaseUser = if (usuario.firebaseUid.isNullOrBlank()) {
            try {
                auth.createUserWithEmailAndPassword(
                    usuario.email,
                    senha
                ).await().user
            } catch (e: FirebaseAuthUserCollisionException) {
                auth.signInWithEmailAndPassword(
                    usuario.email,
                    senha
                ).await().user
            }
        } else {
            auth.signInWithEmailAndPassword(
                usuario.email,
                senha
            ).await().user
        } ?: throw IllegalStateException(
            "O Firebase não retornou os dados da conta."
        )

        val uid = firebaseUser.uid

        try {
            if (!usuario.firebaseUid.isNullOrBlank() && usuario.firebaseUid != uid) {
                throw IllegalStateException(
                    "A identificação da conta no Firebase não corresponde ao cadastro local."
                )
            }

            var usuarioAtualizado = usuario

            if (usuario.firebaseUid.isNullOrBlank()) {
                val alterados = usuarioDao.atualizarFirebaseUid(
                    usuarioId = usuario.id,
                    firebaseUid = uid
                )

                check(alterados == 1) {
                    "Não foi possível associar a conta ao Firebase no banco local."
                }

                usuarioAtualizado = usuario.copy(firebaseUid = uid)
            }

            if (usuarioAtualizado.perfil == PERFIL_ALUNO) {
                vincularAlunoAoFirebase(
                    usuario = usuarioAtualizado,
                    uid = uid
                )
            }

            return usuarioAtualizado

        } catch (e: Exception) {
            auth.signOut()
            throw e
        }
    }

    private suspend fun vincularAlunoAoFirebase(
        usuario: UsuarioEntity,
        uid: String
    ) {
        require(usuario.perfil == PERFIL_ALUNO) {
            "Operação permitida somente para alunos."
        }

        require(usuario.email.isNotBlank()) {
            "O aluno não possui e-mail cadastrado."
        }

        require(uid.isNotBlank()) {
            "UID Firebase inválido."
        }

        val emailNormalizado = usuario.email.trim().lowercase()

        val resultado = FirebaseFirestore.getInstance()
            .collection("alunos")
            .whereEqualTo("email", emailNormalizado)
            .get()
            .await()

        val documento = resultado.documents.firstOrNull()
            ?: FirebaseFirestore.getInstance()
                .collection("alunos")
                .whereEqualTo("email", usuario.email.trim())
                .get()
                .await()
                .documents
                .firstOrNull()
            ?: throw IllegalArgumentException(
                "Cadastro remoto do aluno não encontrado. Peça ao professor para sincronizar seus dados."
            )

        val emailRemoto = documento.getString("email")?.trim()?.lowercase()

        require(emailRemoto == emailNormalizado) {
            "O e-mail do cadastro remoto não corresponde ao aluno autenticado."
        }

        val uidExistente = documento.getString("alunoAuthUid")

        require(uidExistente.isNullOrBlank() || uidExistente == uid) {
            "Este cadastro de aluno já está vinculado a outra conta Firebase."
        }

        if (uidExistente.isNullOrBlank()) {
            documento.reference.update(
                "alunoAuthUid",
                uid
            ).await()
        }
    }

    // LOGIN DO ALUNO MULTI-DISPOSITIVO
    private suspend fun entrarAlunoFirebase(
        email: String,
        senha: String
    ): UsuarioEntity {
        val auth = FirebaseAuth.getInstance()

        val firebaseUser = try {
            auth.signInWithEmailAndPassword(email, senha).await().user
        } catch (e: Exception) {
            val usuarioLocal = usuarioDao.buscarPorEmail(email)
            if (
                usuarioLocal != null &&
                usuarioLocal.perfil == PERFIL_ALUNO &&
                usuarioLocal.ativo &&
                verificarSenha(senha, usuarioLocal.senhaHash)
            ) {
                try {
                    auth.createUserWithEmailAndPassword(email, senha).await().user
                } catch (_: Exception) {
                    throw IllegalArgumentException("E-mail ou senha incorretos.")
                }
            } else {
                throw IllegalArgumentException("E-mail ou senha incorretos.")
            }
        } ?: throw IllegalStateException("O Firebase não retornou os dados da conta.")

        val uid = firebaseUser.uid

        try {
            val firestore = FirebaseFirestore.getInstance()
            var resultado = firestore
                .collection("alunos")
                .whereEqualTo("email", email)
                .get()
                .await()

            if (resultado.isEmpty) {
                resultado = firestore
                    .collection("alunos")
                    .whereEqualTo("alunoAuthUid", uid)
                    .get()
                    .await()
            }

            val documentoAluno = resultado.documents.firstOrNull()
                ?: throw IllegalStateException(
                    "Cadastro de aluno não encontrado no servidor. Solicite ao professor que sincronize seu cadastro."
                )

            val uidExistente = documentoAluno.getString("alunoAuthUid")
            if (!uidExistente.isNullOrBlank() && uidExistente != uid) {
                throw IllegalStateException(
                    "Este cadastro já está vinculado a outra conta."
                )
            }

            if (uidExistente.isNullOrBlank()) {
                documentoAluno.reference.update("alunoAuthUid", uid).await()
            }

            val database = FitCareDatabase.getDatabase(context)
            val alunoDao = database.alunoDao()

            val alunoFirebaseId = documentoAluno.getString("firebaseId") ?: documentoAluno.id

            val nomeAluno = documentoAluno.getString("nome")?.takeIf { it.isNotBlank() }
                ?: email.substringBefore("@")

            val alunoLocalExistente = alunoDao.buscarPorFirebaseId(alunoFirebaseId)

            val alunoLocalId = if (alunoLocalExistente != null) {
                alunoDao.atualizar(
                    alunoLocalExistente.copy(
                        nome = nomeAluno,
                        email = email,
                        dataCadastro = documentoAluno.getString("dataCadastro")
                            ?: alunoLocalExistente.dataCadastro,
                        objetivo = documentoAluno.getString("objetivo")
                            ?: alunoLocalExistente.objetivo,
                        status = documentoAluno.getString("status")?.takeIf { it.isNotBlank() }
                            ?: alunoLocalExistente.status,
                        observacoesProfissionais = documentoAluno.getString("observacoesProfissionais")
                            ?: alunoLocalExistente.observacoesProfissionais,
                        condicoes = documentoAluno.getString("condicoes")
                            ?: alunoLocalExistente.condicoes,
                        restricoes = documentoAluno.getString("restricoes")
                            ?: alunoLocalExistente.restricoes
                    )
                )
                alunoLocalExistente.id
            } else {
                alunoDao.inserir(
                    AlunoEntity(
                        nome = nomeAluno,
                        email = email,
                        dataCadastro = documentoAluno.getString("dataCadastro") ?: "",
                        objetivo = documentoAluno.getString("objetivo") ?: "",
                        status = documentoAluno.getString("status")?.takeIf { it.isNotBlank() }
                            ?: "Normal",
                        observacoesProfissionais = documentoAluno.getString("observacoesProfissionais")
                            ?: "",
                        condicoes = documentoAluno.getString("condicoes") ?: "",
                        restricoes = documentoAluno.getString("restricoes") ?: "",
                        professorId = null,
                        firebaseId = alunoFirebaseId
                    )
                )
            }

            val usuarioPorUid = usuarioDao.buscarPorFirebaseUid(uid)
            val usuarioPorEmail = usuarioDao.buscarPorEmail(email)
            val usuarioLocal = usuarioPorUid ?: usuarioPorEmail
            val usuarioFinal = if (usuarioLocal != null) {
                usuarioDao.atualizarSenha(usuarioLocal.id, gerarHashSenha(senha))
                if (usuarioLocal.firebaseUid.isNullOrBlank()) {
                    usuarioDao.atualizarFirebaseUid(usuarioLocal.id, uid)
                }
                usuarioLocal.copy(
                    nome = nomeAluno,
                    email = email,
                    senhaHash = gerarHashSenha(senha),
                    perfil = PERFIL_ALUNO,
                    alunoId = alunoLocalId,
                    ativo = true,
                    firebaseUid = uid
                )
            } else {
                val novoUsuario = UsuarioEntity(
                    nome = nomeAluno,
                    email = email,
                    senhaHash = gerarHashSenha(senha),
                    perfil = PERFIL_ALUNO,
                    alunoId = alunoLocalId,
                    ativo = true,
                    firebaseUid = uid
                )
                val novoId = usuarioDao.inserir(novoUsuario)
                novoUsuario.copy(id = novoId)
            }
            SessaoUsuario.iniciar(usuarioFinal)
            return usuarioFinal
        } catch (e: Exception) {
            auth.signOut()
            throw e
        }
    }
    // LOGIN DO PROFESSOR PELO FIREBASE
    private suspend fun entrarProfessorFirebase(
        email: String,
        senha: String
    ): UsuarioEntity {
        val auth = FirebaseAuth.getInstance()
        val firebaseUser = auth.signInWithEmailAndPassword(
            email,
            senha
        ).await().user ?: throw IllegalStateException(
            "O Firebase não retornou os dados da conta."
        )
        val uid = firebaseUser.uid
        try {
            val documento = FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .get()
                .await()
            if (!documento.exists()) {
                throw IllegalStateException(
                    "Não existe autorização cadastrada para esta conta."
                )
            }
            val perfilRemoto = documento.getString("perfil")
            val ativoRemoto = documento.getBoolean("ativo")
            if (perfilRemoto != PERFIL_PROFESSOR || ativoRemoto != true) {
                throw IllegalStateException(
                    "Esta conta não possui autorização ativa para acessar o perfil de professor."
                )
            }
            val usuarioPorUid = usuarioDao.buscarPorFirebaseUid(uid)
            val usuarioPorEmail = usuarioDao.buscarPorEmail(email)
            if (
                usuarioPorUid != null &&
                usuarioPorEmail != null &&
                usuarioPorUid.id != usuarioPorEmail.id
            ) {
                throw IllegalStateException(
                    "Existem cadastros locais conflitantes para esta conta."
                )
            }
            val usuarioLocal = usuarioPorUid ?: usuarioPorEmail
            val usuarioFinal = if (usuarioLocal != null) {
                if (usuarioLocal.perfil != PERFIL_PROFESSOR) {
                    throw IllegalStateException(
                        "O cadastro local não pertence ao perfil de professor."
                    )
                }
                if (usuarioLocal.email != email) {
                    throw IllegalStateException(
                        "O e-mail do cadastro local não corresponde à conta autenticada."
                    )
                }
                if (!usuarioLocal.firebaseUid.isNullOrBlank() && usuarioLocal.firebaseUid != uid) {
                    throw IllegalStateException(
                        "Este e-mail já está associado a outra conta Firebase."
                    )
                }
                if (!usuarioLocal.ativo) {
                    val alterados = usuarioDao.atualizarStatus(
                        id = usuarioLocal.id,
                        ativo = true
                    )
                    check(alterados == 1) {
                        "Não foi possível atualizar o status local do professor."
                    }
                }
                if (usuarioLocal.firebaseUid.isNullOrBlank()) {
                    val alterados = usuarioDao.atualizarFirebaseUid(
                        usuarioId = usuarioLocal.id,
                        firebaseUid = uid
                    )
                    check(alterados == 1) {
                        "Não foi possível vincular a conta ao Firebase."
                    }
                }
                usuarioLocal.copy(
                    ativo = true,
                    firebaseUid = uid
                )
            } else {
                val novoUsuario = UsuarioEntity(
                    nome = firebaseUser.displayName?.takeIf { it.isNotBlank() }
                        ?: email.substringBefore("@"),
                    email = email,
                    senhaHash = "",
                    perfil = PERFIL_PROFESSOR,
                    ativo = true,
                    firebaseUid = uid
                )
                val novoId = usuarioDao.inserir(novoUsuario)
                novoUsuario.copy(id = novoId)
            }
            SessaoUsuario.iniciar(usuarioFinal)
            return usuarioFinal
        } catch (e: Exception) {
            auth.signOut()
            throw e
        }
    }
    private fun obterFirebaseAppSecundario(): FirebaseApp {
        return try {
            FirebaseApp.getInstance(NOME_FIREBASE_APP_SECUNDARIO)
        } catch (_: IllegalStateException) {
            FirebaseApp.initializeApp(
                context.applicationContext,
                FirebaseApp.getInstance().options,
                NOME_FIREBASE_APP_SECUNDARIO
            ) ?: throw IllegalStateException(
                "Não foi possível inicializar o Firebase Authentication secundário."
            )
        }
    }
    // LOGIN ADMINISTRATIVO
    private suspend fun entrarAdminFirebase(
        email: String,
        senha: String
    ): UsuarioEntity {
        val auth = FirebaseAuth.getInstance()
        val firebaseUser = try {
            auth.signInWithEmailAndPassword(email, senha).await().user
        } catch (e: Exception) {
            auth.signOut()
            throw IllegalArgumentException(
                "Falha na autenticação administrativa: ${e.message ?: "verifique e-mail e senha."}"
            )
        } ?: throw IllegalStateException(
            "O Firebase Authentication não retornou a conta administrativa."
        )
        try {
            require(firebaseUser.uid == UID_ADMIN_AUTORIZADO) {
                "Esta conta não está autorizada como administradora do Ícaros FitCare."
            }
            require(email.equals(EMAIL_ADMIN_INICIAL, ignoreCase = true)) {
                "E-mail administrativo inválido."
            }
            val usuarioPorEmail = usuarioDao.buscarPorEmail(EMAIL_ADMIN_INICIAL)
            val usuarioPorUid = usuarioDao.buscarPorFirebaseUid(UID_ADMIN_AUTORIZADO)
            val usuarioLocal = usuarioPorEmail ?: usuarioPorUid
            val usuarioAutenticado = if (usuarioLocal != null) {
                require(
                    usuarioLocal.perfil == PERFIL_ADMIN && usuarioLocal.ativo
                ) {
                    "Já existe um cadastro local incompatível com a conta administrativa."
                }
                if (usuarioLocal.firebaseUid.isNullOrBlank()) {
                    usuarioDao.atualizarFirebaseUid(
                        usuarioId = usuarioLocal.id,
                        firebaseUid = UID_ADMIN_AUTORIZADO
                    )
                }
                usuarioLocal.copy(
                    email = EMAIL_ADMIN_INICIAL,
                    firebaseUid = UID_ADMIN_AUTORIZADO
                )
            } else {
                val novoAdmin = UsuarioEntity(
                    nome = firebaseUser.displayName?.takeIf { it.isNotBlank() }
                        ?: "Administrador",
                    email = EMAIL_ADMIN_INICIAL,
                    senhaHash = gerarHashSenha(senha),
                    perfil = PERFIL_ADMIN,
                    ativo = true,
                    firebaseUid = UID_ADMIN_AUTORIZADO
                )
                val novoId = usuarioDao.inserir(novoAdmin)
                novoAdmin.copy(id = novoId)
            }
            try {
                registrarConfiguracaoAdmin(UID_ADMIN_AUTORIZADO)
            } catch (_: Exception) {
// Não bloqueia o acesso administrativo caso já esteja configurado
            }
            SessaoUsuario.iniciar(usuarioAutenticado)
            return usuarioAutenticado
        } catch (e: Exception) {
            auth.signOut()
            throw e
        }
    }
    private suspend fun registrarConfiguracaoAdmin(uid: String) {
        require(uid == UID_ADMIN_AUTORIZADO) {
            "UID administrativo não autorizado."
        }
        FirebaseFirestore.getInstance()
            .collection("configuracao")
            .document("admin")
            .set(
                mapOf(
                    "uid" to UID_ADMIN_AUTORIZADO,
                    "configurado" to true
                ),
                SetOptions.merge()
            )
            .await()
    }
    // CRIAÇÃO DO ADMINISTRADOR INICIAL
    suspend fun criarAdminInicial(
        nome: String,
        email: String,
        senha: String,
        emailProfessorInicial: String,
        senhaProfessorInicial: String
    ): UsuarioEntity {
        validarCadastro(nome, email, senha)
        val emailNormalizado = email.trim().lowercase()
        val emailProfessorNormalizado = emailProfessorInicial.trim().lowercase()
        require(emailNormalizado == EMAIL_ADMIN_INICIAL) {
            "O e-mail informado não está autorizado para a conta administrativa."
        }
        require(emailProfessorNormalizado == EMAIL_PROFESSOR_INICIAL) {
            "Informe o e-mail do professor inicial."
        }
        require(senhaProfessorInicial.isNotBlank()) {
            "Informe a senha do professor inicial."
        }
        val professorInicial = usuarioDao.buscarPorEmail(EMAIL_PROFESSOR_INICIAL)
            ?: throw IllegalArgumentException(
                "A conta do professor inicial não foi encontrada."
            )
        require(
            professorInicial.perfil == PERFIL_PROFESSOR && professorInicial.ativo
        ) {
            "A conta do professor inicial precisa estar ativa."
        }
        require(
            verificarSenha(senhaProfessorInicial, professorInicial.senhaHash)
        ) {
            "A autenticação do professor inicial falhou."
        }
        require(
            usuarioDao.buscarPorEmail(EMAIL_ADMIN_INICIAL) == null
        ) {
            "A conta administrativa inicial já foi criada."
        }
        val admin = UsuarioEntity(
            nome = nome.trim(),
            email = EMAIL_ADMIN_INICIAL,
            senhaHash = gerarHashSenha(senha),
            perfil = PERFIL_ADMIN,
            ativo = true
        )
        val id = usuarioDao.inserirAdminInicial(admin)
        val adminCriado = admin.copy(id = id)
        val adminAutenticado = autenticarFirebase(
            usuario = adminCriado,
            senha = senha
        )
        require(adminAutenticado.firebaseUid == UID_ADMIN_AUTORIZADO) {
            "A conta autenticada não corresponde ao UID administrativo autorizado."
        }
        registrarConfiguracaoAdmin(UID_ADMIN_AUTORIZADO)
        SessaoUsuario.iniciar(adminAutenticado)
        return adminAutenticado
    }
    // CADASTRO DE PROFESSOR
    suspend fun cadastrarProfessor(
        nome: String,
        email: String,
        senha: String
    ): UsuarioEntity {
        exigirAdminAtivo()
        validarCadastro(nome, email, senha)
        val emailNormalizado = email.trim().lowercase()
        require(
            FirebaseAuth.getInstance().currentUser?.uid == UID_ADMIN_AUTORIZADO
        ) {
            "A sessão administrativa do Firebase não está autorizada para cadastrar professores."
        }
        require(
            usuarioDao.buscarPorEmail(emailNormalizado) == null
        ) {
            "Este e-mail já está cadastrado."
        }
        val appSecundario = obterFirebaseAppSecundario()
        val authSecundario = FirebaseAuth.getInstance(appSecundario)
        var contaCriadaAgora = false
        val firebaseUser = try {
            authSecundario.createUserWithEmailAndPassword(
                emailNormalizado,
                senha
            ).await().user.also {
                contaCriadaAgora = true
            }
        } catch (e: FirebaseAuthUserCollisionException) {
            authSecundario.signInWithEmailAndPassword(
                emailNormalizado,
                senha
            ).await().user
        } ?: throw IllegalStateException(
            "O Firebase Authentication não retornou a conta do professor."
        )
        val uidProfessor = firebaseUser.uid
        try {
            FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uidProfessor)
                .set(
                    mapOf(
                        "nome" to nome.trim(),
                        "email" to emailNormalizado,
                        "perfil" to PERFIL_PROFESSOR,
                        "ativo" to true,
                        "dataCadastro" to System.currentTimeMillis(),
                        "criadoPor" to UID_ADMIN_AUTORIZADO
                    )
                )
                .await()
            val usuario = UsuarioEntity(
                nome = nome.trim(),
                email = emailNormalizado,
                senhaHash = gerarHashSenha(senha),
                perfil = PERFIL_PROFESSOR,
                ativo = true,
                firebaseUid = uidProfessor
            )
            val id = usuarioDao.inserir(usuario)
            return usuario.copy(id = id)
        } catch (e: Exception) {
            if (contaCriadaAgora) {
                try {
                    authSecundario.currentUser?.delete()?.await()
                } catch (_: Exception) {
                }
            }
            throw e
        } finally {
            authSecundario.signOut()
        }
    }
    // CADASTRO DE ALUNO (COM PROVISIONAMENTO NO FIREBASE AUTH)
    suspend fun cadastrarAluno(
        nome: String,
        email: String,
        senha: String,
        alunoId: Long
    ): UsuarioEntity {
        validarCadastro(nome, email, senha)
        require(alunoId > 0) {
            "É necessário informar um cadastro de aluno válido."
        }
        val professorAtual = exigirProfessorAtivo()
        require(
            usuarioDao.alunoPertenceAoProfessor(alunoId, professorAtual.id)
        ) {
            "Você não tem permissão para criar acesso para este aluno."
        }
        val emailNormalizado = email.trim().lowercase()
        require(
            usuarioDao.buscarPorEmail(emailNormalizado) == null
        ) {
            "Este e-mail já está cadastrado."
        }
        require(
            usuarioDao.buscarPorAlunoId(alunoId) == null
        ) {
            "Este aluno já possui uma conta."
        }
        val appSecundario = obterFirebaseAppSecundario()
        val authSecundario = FirebaseAuth.getInstance(appSecundario)
        var uidAluno: String? = null
        try {
            val userSecundario = try {
                authSecundario.createUserWithEmailAndPassword(
                    emailNormalizado,
                    senha
                ).await().user
            } catch (_: FirebaseAuthUserCollisionException) {
                try {
                    authSecundario.signInWithEmailAndPassword(
                        emailNormalizado,
                        senha
                    ).await().user
                } catch (_: Exception) {
                    null
                }
            }
            uidAluno = userSecundario?.uid
        } finally {
            authSecundario.signOut()
        }
        val database = FitCareDatabase.getDatabase(context)
        val alunoEntity = database.alunoDao().buscarPorIdDireto(alunoId)
        if (alunoEntity != null && !alunoEntity.firebaseId.isNullOrBlank()) {
            try {
                val dadosAtualizacao = mutableMapOf<String, Any>(
                    "email" to emailNormalizado,
                    "nome" to nome.trim()
                )
                if (!uidAluno.isNullOrBlank()) {
                    dadosAtualizacao["alunoAuthUid"] = uidAluno
                }
                FirebaseFirestore.getInstance()
                    .collection("alunos")
                    .document(alunoEntity.firebaseId)
                    .set(dadosAtualizacao, SetOptions.merge())
                    .await()
            } catch (_: Exception) {
// Será sincronizado pelo worker caso esteja offline
            }
        }
        val usuario = UsuarioEntity(
            nome = nome.trim(),
            email = emailNormalizado,
            senhaHash = gerarHashSenha(senha),
            perfil = PERFIL_ALUNO,
            alunoId = alunoId,
            professorId = professorAtual.id,
            firebaseUid = uidAluno
        )
        val id = usuarioDao.inserir(usuario)
        return usuario.copy(id = id)
    }
    // LOGIN
    suspend fun entrar(
        email: String,
        senha: String,
        perfilEsperado: String
    ): UsuarioEntity {
        val emailNormalizado = email.trim().lowercase()
        require(emailNormalizado.isNotBlank()) {
            "Informe seu e-mail."
        }
        require(senha.isNotBlank()) {
            "Informe sua senha."
        }
        require(
            perfilEsperado == PERFIL_PROFESSOR ||
                    perfilEsperado == PERFIL_ALUNO ||
                    perfilEsperado == PERFIL_ADMIN
        ) {
            "Perfil inválido."
        }
        if (perfilEsperado == PERFIL_PROFESSOR) {
            return entrarProfessorFirebase(
                email = emailNormalizado,
                senha = senha
            )
        }
        if (perfilEsperado == PERFIL_ADMIN) {
            return entrarAdminFirebase(
                email = emailNormalizado,
                senha = senha
            )
        }
        return entrarAlunoFirebase(
            email = emailNormalizado,
            senha = senha
        )
    }
    suspend fun autenticar(
        email: String,
        senha: String,
        perfilEsperado: String
    ): UsuarioEntity? {
        return entrar(email, senha, perfilEsperado)
    }
    // BUSCAS
    suspend fun buscarPorId(id: Long): UsuarioEntity? {
        if (id <= 0) return null
        return usuarioDao.buscarPorId(id)
    }
    suspend fun buscarUsuarioAtivoPorId(
        id: Long
    ): UsuarioEntity? {
        if (id <= 0) return null
        val usuario = usuarioDao.buscarPorId(id) ?: return null
        if (!usuario.ativo) return null
        if (
            usuario.perfil !in setOf(
                PERFIL_PROFESSOR,
                PERFIL_ALUNO,
                PERFIL_ADMIN
            )
        ) return null
        if (usuario.perfil == PERFIL_ALUNO) {
            val alunoId = usuario.alunoId ?: return null
            val database = FitCareDatabase.getDatabase(context)
            database.alunoDao().buscarPorIdDireto(alunoId) ?: return null
        }
        if (
            usuario.perfil == PERFIL_ADMIN &&
            !usuario.email.equals(EMAIL_ADMIN_INICIAL, ignoreCase = true)
        ) return null
        return usuario
    }
    // ADMINISTRAÇÃO DE PROFESSORES
    // ADMINISTRAÇÃO DE PROFESSORES
    suspend fun listarProfissionaisParaAdmin(): List<UsuarioEntity> {
        exigirAdminAtivo()
        val firestore = FirebaseFirestore.getInstance()

        val resultado = firestore
            .collection("usuarios")
            .whereEqualTo("perfil", PERFIL_PROFESSOR)
            .get()
            .await()

        val uidsRemotosAtivos = mutableSetOf<String>()
        val emailsRemotosAtivos = mutableSetOf<String>()

        for (documento in resultado.documents) {
            val uid = documento.id
            val nome = documento.getString("nome")?.trim().orEmpty()
            val email = documento.getString("email")?.trim()?.lowercase().orEmpty()
            val ativo = documento.getBoolean("ativo") ?: false

            if (uid.isBlank() || nome.isBlank() || email.isBlank()) {
                continue
            }

            uidsRemotosAtivos.add(uid)
            emailsRemotosAtivos.add(email)

            val usuarioPorUid = usuarioDao.buscarPorFirebaseUid(uid)
            val usuarioPorEmail = usuarioDao.buscarPorEmail(email)

            require(
                usuarioPorUid == null ||
                        usuarioPorEmail == null ||
                        usuarioPorUid.id == usuarioPorEmail.id
            ) {
                "Existem cadastros locais conflitantes para o professor $email."
            }

            val usuarioLocal = usuarioPorUid ?: usuarioPorEmail

            if (usuarioLocal != null) {
                require(usuarioLocal.perfil == PERFIL_PROFESSOR) {
                    "O e-mail $email já pertence a outro perfil."
                }
                require(
                    usuarioLocal.firebaseUid.isNullOrBlank() ||
                            usuarioLocal.firebaseUid == uid
                ) {
                    "O professor $email está vinculado a outra conta Firebase."
                }

                val atualizado = usuarioDao.atualizarProfessorComDadosFirebase(
                    usuarioId = usuarioLocal.id,
                    nome = nome,
                    email = email,
                    ativo = ativo,
                    firebaseUid = uid
                )
                check(atualizado == 1) {
                    "Não foi possível atualizar o professor $email no banco local."
                }
            } else {
                val novoProfessor = UsuarioEntity(
                    nome = nome,
                    email = email,
                    senhaHash = "",
                    perfil = PERFIL_PROFESSOR,
                    ativo = ativo,
                    firebaseUid = uid
                )
                usuarioDao.inserir(novoProfessor)
            }
        }

        // Remove do banco local professores que foram excluídos no Firestore por outro dispositivo
        val locaisAtuais = usuarioDao.listarProfissionaisParaAdmin()
        for (local in locaisAtuais) {
            if (local.email.equals(EMAIL_PROFESSOR_INICIAL, ignoreCase = true)) {
                continue
            }
            val existeNoServidor =
                (!local.firebaseUid.isNullOrBlank() && local.firebaseUid in uidsRemotosAtivos) ||
                        (local.email.trim().lowercase() in emailsRemotosAtivos)

            if (!existeNoServidor && !usuarioDao.professorPossuiAlunos(local.id)) {
                usuarioDao.excluirProfessor(local.id)
            }
        }

        return usuarioDao.listarProfissionaisParaAdmin()
    }

    suspend fun alterarStatusProfissional(
        professorId: Long,
        ativo: Boolean
    ) {
        exigirAdminAtivo()
        require(professorId > 0) {
            "Professor inválido."
        }
        val professor = usuarioDao.buscarPorId(professorId)
            ?: throw IllegalArgumentException("Professor não encontrado.")
        require(professor.perfil == PERFIL_PROFESSOR) {
            "A conta informada não pertence a um professor."
        }
        require(
            professor.email != EMAIL_PROFESSOR_INICIAL || ativo
        ) {
            "A conta do professor inicial não pode ser bloqueada."
        }
        professor.firebaseUid?.takeIf { it.isNotBlank() }?.let { uid ->
            FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .update("ativo", ativo)
                .await()
        }
        val alterados = usuarioDao.atualizarStatus(
            id = professorId,
            ativo = ativo
        )
        check(alterados == 1) {
            "Não foi possível atualizar a autorização do professor."
        }
    }
    suspend fun redefinirSenhaProfissional(
        professorId: Long,
        novaSenha: String
    ) {
        exigirAdminAtivo()
        require(professorId > 0) {
            "Professor inválido."
        }
        require(novaSenha.length >= 8) {
            "A senha deve ter pelo menos 8 caracteres."
        }
        val professor = usuarioDao.buscarPorId(professorId)
            ?: throw IllegalArgumentException("Professor não encontrado.")
        require(professor.perfil == PERFIL_PROFESSOR) {
            "A conta informada não pertence a um professor."
        }
        val alterados = usuarioDao.atualizarSenha(
            id = professorId,
            senhaHash = gerarHashSenha(novaSenha)
        )
        check(alterados == 1) {
            "Não foi possível redefinir a senha."
        }
    }
    suspend fun excluirContaProfissional(
        professorId: Long
    ) {
        exigirAdminAtivo()
        require(professorId > 0) {
            "Professor inválido."
        }
        val professor = usuarioDao.buscarPorId(professorId)
            ?: throw IllegalArgumentException("Professor não encontrado.")
        require(professor.perfil == PERFIL_PROFESSOR) {
            "Somente contas profissionais podem ser excluídas."
        }
        require(professor.email != EMAIL_PROFESSOR_INICIAL) {
            "A conta do professor inicial não pode ser excluída."
        }
        require(!usuarioDao.professorPossuiAlunos(professorId)) {
            "Transfira os alunos deste professor antes de excluir sua conta."
        }
        professor.firebaseUid?.takeIf { it.isNotBlank() }?.let { uid ->
            FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .delete()
                .await()
        }
        val excluidos = usuarioDao.excluirProfessor(professorId)
        check(excluidos == 1) {
            "Não foi possível excluir a conta profissional."
        }
    }
    // VÍNCULO ALUNO / PROFESSOR
    suspend fun vincularAlunoAoProfessor(
        alunoUsuarioId: Long,
        professorUsuarioId: Long
    ) {
        exigirAdminAtivo()
        require(alunoUsuarioId > 0 && professorUsuarioId > 0) {
            "Informe contas válidas."
        }
        val aluno = usuarioDao.buscarPorId(alunoUsuarioId)
            ?: throw IllegalArgumentException("Aluno não encontrado.")
        val professor = usuarioDao.buscarPorId(professorUsuarioId)
            ?: throw IllegalArgumentException("Professor não encontrado.")
        require(aluno.perfil == PERFIL_ALUNO) {
            "O usuário informado não é um aluno."
        }
        require(professor.perfil == PERFIL_PROFESSOR) {
            "O usuário informado não é um professor."
        }
        require(aluno.ativo && professor.ativo) {
            "Não é possível vincular contas inativas."
        }
        val alunoId = aluno.alunoId
            ?: throw IllegalArgumentException("A conta não possui cadastro de aluno vinculado.")
        val alterados = usuarioDao.vincularProfessorCompleto(
            usuarioId = alunoUsuarioId,
            alunoId = alunoId,
            professorId = professorUsuarioId
        )
        check(alterados) {
            "Não foi possível atualizar o vínculo do aluno."
        }
    }
    suspend fun alunoPossuiAcesso(alunoId: Long): Boolean {
        require(alunoId > 0) {
            "Aluno inválido."
        }
        val professor = exigirProfessorAtivo()
        require(
            usuarioDao.alunoPertenceAoProfessor(alunoId, professor.id)
        ) {
            "Você não tem permissão para gerenciar o acesso deste aluno."
        }
        val usuarioAluno = usuarioDao.buscarPorAlunoId(alunoId)
        if (usuarioAluno != null) {
            return true
        }
// Verifica também no Firestore se o aluno já possui vínculo remoto
        val database = FitCareDatabase.getDatabase(context)
        val aluno = database.alunoDao().buscarPorIdDireto(alunoId) ?: return false
        val firebaseId = aluno.firebaseId ?: return false
        return try {
            val doc = FirebaseFirestore.getInstance()
                .collection("alunos")
                .document(firebaseId)
                .get()
                .await()
            val uidRemoto = doc.getString("alunoAuthUid")
            if (!uidRemoto.isNullOrBlank()) {
                usuarioDao.inserir(
                    UsuarioEntity(
                        nome = aluno.nome,
                        email = aluno.email.trim().lowercase(),
                        senhaHash = "",
                        perfil = PERFIL_ALUNO,
                        alunoId = aluno.id,
                        professorId = professor.id,
                        firebaseUid = uidRemoto
                    )
                )
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
    suspend fun redefinirSenhaDoAluno(
        alunoId: Long,
        novaSenha: String
    ) {
        val professor = exigirProfessorAtivo()
        require(novaSenha.length >= 8) {
            "A senha deve ter pelo menos 8 caracteres."
        }
        require(
            usuarioDao.alunoPertenceAoProfessor(alunoId, professor.id)
        ) {
            "Você não tem permissão para alterar a senha deste aluno."
        }
        val database = FitCareDatabase.getDatabase(context)
        val aluno = database.alunoDao().buscarPorIdDireto(alunoId)
            ?: throw IllegalStateException("Cadastro do aluno não encontrado.")
        val emailNormalizado = aluno.email.trim().lowercase()
        var usuarioAluno = usuarioDao.buscarPorAlunoId(alunoId)
        if (usuarioAluno == null) {
            val idNovo = usuarioDao.inserir(
                UsuarioEntity(
                    nome = aluno.nome,
                    email = emailNormalizado,
                    senhaHash = gerarHashSenha(novaSenha),
                    perfil = PERFIL_ALUNO,
                    alunoId = alunoId,
                    professorId = professor.id
                )
            )
            usuarioAluno = usuarioDao.buscarPorId(idNovo)
        } else {
            usuarioDao.atualizarSenha(
                id = usuarioAluno.id,
                senhaHash = gerarHashSenha(novaSenha)
            )
        }
        val appSecundario = obterFirebaseAppSecundario()
        val authSecundario = FirebaseAuth.getInstance(appSecundario)
        try {
            val criado = try {
                authSecundario.createUserWithEmailAndPassword(
                    emailNormalizado,
                    novaSenha
                ).await().user
            } catch (_: FirebaseAuthUserCollisionException) {
                null
            }
            if (criado != null) {
                usuarioDao.atualizarFirebaseUid(usuarioAluno!!.id, criado.uid)
                if (!aluno.firebaseId.isNullOrBlank()) {
                    FirebaseFirestore.getInstance()
                        .collection("alunos")
                        .document(aluno.firebaseId)
                        .update("alunoAuthUid", criado.uid)
                        .await()
                }
            } else {
                authSecundario.sendPasswordResetEmail(emailNormalizado).await()
            }
        } catch (_: Exception) {
// Mantém atualização local se estiver offline
        } finally {
            authSecundario.signOut()
        }
    }
    // VALIDAÇÃO DE SESSÃO
    private suspend fun exigirProfessorAtivo(): UsuarioEntity {
        val sessao = SessaoUsuario.usuarioAtual
            ?: throw IllegalStateException("Nenhum professor está conectado.")
        require(sessao.perfil == PERFIL_PROFESSOR) {
            "Operação permitida somente para professores."
        }
        val professor = usuarioDao.buscarPorId(sessao.id)
            ?: throw IllegalStateException("Conta profissional não encontrada.")
        require(
            professor.perfil == PERFIL_PROFESSOR && professor.ativo
        ) {
            "Esta conta profissional está inativa."
        }
        return professor
    }
    private suspend fun exigirAdminAtivo(): UsuarioEntity {
        val sessao = SessaoUsuario.usuarioAtual
            ?: throw IllegalStateException("Nenhum administrador está conectado.")
        require(sessao.perfil == PERFIL_ADMIN) {
            "Operação permitida somente ao administrador."
        }
        val admin = usuarioDao.buscarPorId(sessao.id)
            ?: throw IllegalStateException("Conta administrativa não encontrada.")
        require(
            admin.perfil == PERFIL_ADMIN &&
                    admin.ativo &&
                    admin.email.equals(EMAIL_ADMIN_INICIAL, ignoreCase = true)
        ) {
            "A conta administrativa está inativa ou inválida."
        }
        return admin
    }
    // ENCERRAR SESSÃO
    fun sair() {
        FirebaseAuth.getInstance().signOut()
        SessaoUsuario.encerrar()
    }
    // VALIDAÇÃO DE CADASTRO
    private fun validarCadastro(
        nome: String,
        email: String,
        senha: String
    ) {
        require(nome.trim().length >= 2) {
            "Informe o nome completo."
        }
        require(
            android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
        ) {
            "Informe um e-mail válido."
        }
        require(senha.length >= 8) {
            "A senha deve possuir pelo menos 8 caracteres."
        }
    }
    // HASH PBKDF2
    private fun gerarHashSenha(senha: String): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val hash = derivarSenha(senha, salt)
        val saltBase64 = Base64.encodeToString(
            salt,
            Base64.NO_WRAP
        )
        val hashBase64 = Base64.encodeToString(
            hash,
            Base64.NO_WRAP
        )
        return "PBKDF2_SHA1_120000:$saltBase64:$hashBase64"
    }
    private fun verificarSenha(
        senha: String,
        senhaHash: String
    ): Boolean {
        return try {
            val partes = senhaHash.split(":")
            if (
                partes.size != 3 ||
                partes.get(0) != "PBKDF2_SHA1_120000"
            ) {
                return false
            }

            val salt = Base64.decode(
                partes.get(1),
                Base64.NO_WRAP
            )

            val hashOriginal = Base64.decode(
                partes.get(2),
                Base64.NO_WRAP
            )
            val hashInformado = derivarSenha(senha, salt)
            MessageDigest.isEqual(
                hashOriginal,
                hashInformado
            )
        } catch (_: Exception) {
            false
        }
    }
    private fun derivarSenha(
        senha: String,
        salt: ByteArray
    ): ByteArray {
        val spec = PBEKeySpec(
            senha.toCharArray(),
            salt,
            120000,
            256
        )
        return try {
            SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA1")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }
    // ADMINISTRADOR
    suspend fun existeAdministrador(): Boolean {
        if (usuarioDao.contarAdmins() > 0) {
            return true
        }
        return try {
            val documento = FirebaseFirestore.getInstance()
                .collection("configuracao")
                .document("admin")
                .get()
                .await()
            documento.exists()
        } catch (_: Exception) {
            true
        }
    }
}