
package br.com.icaros.fitcare.data

object SessaoUsuario {

    var usuarioAtual: UsuarioEntity? = null
        private set

    fun iniciar(usuario: UsuarioEntity) {
        usuarioAtual = usuario
    }

    fun encerrar() {
        usuarioAtual = null
    }

    fun ehProfessor(): Boolean {
        return usuarioAtual?.perfil == "PROFESSOR"
    }

    fun ehAluno(): Boolean {
        return usuarioAtual?.perfil == "ALUNO"
    }

    fun ehAdmin(): Boolean {
        return usuarioAtual?.perfil == "ADMIN"
    }

    fun usuarioId(): Long? {
        return usuarioAtual?.id
    }

    fun professorId(): Long? {
        return if (ehProfessor()) usuarioAtual?.id else null
    }

    fun exigirProfessorId(): Long {
        return professorId()
            ?: throw IllegalStateException(
                "Acesso permitido somente para um professor autenticado."
            )
    }

    fun alunoId(): Long? {
        return if (ehAluno()) usuarioAtual?.alunoId else null
    }

    fun exigirAlunoId(): Long {
        return alunoId()
            ?: throw IllegalStateException(
                "Acesso permitido somente para um aluno autenticado."
            )
    }

    fun exigirAdmin() {
        check(ehAdmin()) {
            "Acesso permitido somente ao administrador."
        }
    }
}